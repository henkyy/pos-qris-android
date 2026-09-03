create or replace function public.checkout_sale_multi_payment_v2(
  p_branch_id uuid,
  p_location_id uuid,
  p_customer_id uuid,
  p_items jsonb,
  p_payments jsonb,
  p_discount_amount bigint default 0,
  p_idempotency_key text default null
)
returns table(sale_id uuid, sale_no text, subtotal_amount bigint, discount_amount bigint, total_amount bigint, paid_amount bigint, change_amount bigint, sale_status text)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_business_id uuid;
  v_sale_id uuid := gen_random_uuid();
  v_sale_no text := 'TRX-' || to_char(clock_timestamp(), 'YYMMDDHH24MISSMS');
  v_subtotal bigint := 0;
  v_discount bigint := greatest(coalesce(p_discount_amount,0),0);
  v_total bigint := 0;
  v_paid bigint := 0;
  v_change bigint := 0;
  v_hpp bigint := 0;
  v_item jsonb;
  v_payment jsonb;
  v_idx bigint;
  v_method record;
  v_product record;
  v_price bigint;
  v_conversion numeric;
  v_payment_status public.payment_status;
  v_amount bigint;
  v_cash_received bigint;
  v_reference text;
  v_qris_configuration_id uuid;
  v_existing_sale uuid;
  v_existing_sale_no text;
  v_existing_subtotal bigint;
  v_existing_discount bigint;
  v_existing_total bigint;
  v_existing_paid bigint;
  v_existing_change bigint;
  v_existing_status text;
begin
  select br.business_id into v_business_id from public.branches br where br.id=p_branch_id and br.is_active=true;
  if v_business_id is null then raise exception 'Cabang tidak valid atau tidak aktif'; end if;
  if not exists(select 1 from public.locations l where l.id=p_location_id and l.branch_id=p_branch_id and l.is_active=true) then raise exception 'Lokasi stok tidak valid atau tidak aktif'; end if;
  if p_customer_id is not null and not exists(select 1 from public.customers c where c.id=p_customer_id and c.business_id=v_business_id and c.is_active=true) then raise exception 'Pelanggan tidak valid'; end if;

  if p_idempotency_key is not null then
    select pay.sale_id,s.sale_no,s.subtotal,s.discount_amount,s.total_amount,s.paid_amount,s.change_amount,s.status::text
      into v_existing_sale,v_existing_sale_no,v_existing_subtotal,v_existing_discount,v_existing_total,v_existing_paid,v_existing_change,v_existing_status
    from public.payments pay join public.sales s on s.id=pay.sale_id
    where pay.idempotency_key=p_idempotency_key||':0' limit 1;
    if v_existing_sale is not null then
      return query select v_existing_sale,v_existing_sale_no,v_existing_subtotal,v_existing_discount,v_existing_total,v_existing_paid,v_existing_change,v_existing_status;
      return;
    end if;
  end if;

  if jsonb_typeof(p_items)<>'array' or jsonb_array_length(p_items)=0 then raise exception 'Item transaksi kosong'; end if;
  if jsonb_typeof(p_payments)<>'array' or jsonb_array_length(p_payments)=0 then raise exception 'Pembayaran kosong'; end if;

  for v_item in select * from jsonb_array_elements(p_items) loop
    if coalesce((v_item->>'qty')::numeric,0)<=0 then raise exception 'Qty harus lebih dari 0'; end if;
    if coalesce((v_item->>'unit_price')::bigint,-1)<0 then raise exception 'Harga jual tidak valid'; end if;
    select p.sku,p.name,p.current_cost into v_product from public.products p where p.id=(v_item->>'product_id')::uuid and p.business_id=v_business_id and p.is_active=true;
    if not found then raise exception 'Produk tidak valid'; end if;
    select pu.conversion_to_base into v_conversion from public.product_units pu join public.units u on u.id=pu.unit_id where pu.product_id=(v_item->>'product_id')::uuid and pu.unit_id=(v_item->>'unit_id')::uuid and pu.is_sales_unit=true and u.business_id=v_business_id limit 1;
    if v_conversion is null then raise exception 'Satuan penjualan tidak valid untuk produk'; end if;
    select pp.price into v_price from public.product_prices pp join public.price_lists pl on pl.id=pp.price_list_id where pl.business_id=v_business_id and pl.is_default=true and pl.is_active=true and pp.product_id=(v_item->>'product_id')::uuid and pp.unit_id=(v_item->>'unit_id')::uuid and pp.min_qty<=(v_item->>'qty')::numeric and (pp.valid_from is null or pp.valid_from<=now()) and (pp.valid_until is null or pp.valid_until>=now()) order by pp.min_qty desc,pp.valid_from desc nulls last limit 1;
    if v_price is null then raise exception 'Harga jual belum dikonfigurasi untuk produk'; end if;
    if (v_item->>'unit_price')::bigint<>v_price then raise exception 'Harga produk berubah. Silakan muat ulang daftar produk'; end if;
    v_subtotal:=v_subtotal+((v_item->>'qty')::numeric*v_price)::bigint;
    v_hpp:=v_hpp+((v_item->>'qty')::numeric*coalesce(v_product.current_cost,0))::bigint;
  end loop;

  if v_subtotal<=0 then raise exception 'Total transaksi tidak valid'; end if;
  if v_discount>v_subtotal then raise exception 'Diskon tidak boleh melebihi subtotal'; end if;
  v_total:=v_subtotal-v_discount;
  if v_total<=0 then raise exception 'Total setelah diskon harus lebih dari 0'; end if;

  insert into public.sales(id,business_id,branch_id,location_id,customer_id,sale_no,status,subtotal,discount_amount,tax_amount,service_charge,rounding_amount,total_amount,paid_amount,change_amount,hpp_amount,margin_amount,notes)
  values(v_sale_id,v_business_id,p_branch_id,p_location_id,p_customer_id,v_sale_no,'OPEN'::public.document_status,v_subtotal,v_discount,0,0,0,v_total,0,0,v_hpp,v_total-v_hpp,'POS');

  for v_item in select * from jsonb_array_elements(p_items) loop
    select p.sku,p.name,p.current_cost into v_product from public.products p where p.id=(v_item->>'product_id')::uuid and p.business_id=v_business_id and p.is_active=true;
    select pu.conversion_to_base into v_conversion from public.product_units pu where pu.product_id=(v_item->>'product_id')::uuid and pu.unit_id=(v_item->>'unit_id')::uuid and pu.is_sales_unit=true limit 1;
    select pp.price into v_price from public.product_prices pp join public.price_lists pl on pl.id=pp.price_list_id where pl.business_id=v_business_id and pl.is_default=true and pl.is_active=true and pp.product_id=(v_item->>'product_id')::uuid and pp.unit_id=(v_item->>'unit_id')::uuid and pp.min_qty<=(v_item->>'qty')::numeric and (pp.valid_from is null or pp.valid_from<=now()) and (pp.valid_until is null or pp.valid_until>=now()) order by pp.min_qty desc,pp.valid_from desc nulls last limit 1;
    insert into public.sale_items(id,sale_id,product_id,unit_id,product_sku_snapshot,product_name_snapshot,qty,conversion_to_base,unit_price,discount_amount,tax_amount,line_total,hpp_unit,hpp_total)
    values(gen_random_uuid(),v_sale_id,(v_item->>'product_id')::uuid,(v_item->>'unit_id')::uuid,v_product.sku,v_product.name,(v_item->>'qty')::numeric,v_conversion,v_price,0,0,((v_item->>'qty')::numeric*v_price)::bigint,coalesce(v_product.current_cost,0),((v_item->>'qty')::numeric*coalesce(v_product.current_cost,0))::bigint);
  end loop;

  for v_payment,v_idx in select item,ordinality from jsonb_array_elements(p_payments) with ordinality as t(item,ordinality) loop
    select pm.* into v_method from public.payment_methods pm where pm.id=(v_payment->>'payment_method_id')::uuid and pm.business_id=v_business_id and pm.is_active=true;
    if not found then raise exception 'Metode pembayaran tidak valid'; end if;
    v_amount:=(v_payment->>'amount')::bigint;
    if v_amount is null or v_amount<=0 then raise exception 'Nominal pembayaran harus lebih dari 0'; end if;
    if v_amount<>v_total then raise exception 'Nominal pembayaran harus sama dengan total transaksi'; end if;
    v_cash_received:=coalesce((v_payment->>'cash_received')::bigint,v_amount);
    v_reference:=nullif(trim(v_payment->>'reference'),'');
    v_qris_configuration_id:=null;

    if upper(v_method.method_type)='CASH' or v_method.code='CASH' then
      if v_cash_received<v_amount then raise exception 'Uang tunai kurang dari nominal pembayaran'; end if;
      v_payment_status:='PAID'::public.payment_status;
      v_change:=v_change+(v_cash_received-v_amount);
    elsif upper(v_method.method_type) in ('RECEIVABLE','PIUTANG') or v_method.code in ('RECEIVABLE','PIUTANG','AR') then
      if p_customer_id is null then raise exception 'Pelanggan wajib dipilih untuk transaksi piutang'; end if;
      v_payment_status:='PENDING'::public.payment_status;
    elsif upper(v_method.method_type) in ('BANK_TRANSFER','TRANSFER') or v_method.code='TRANSFER' then
      if v_reference is null then raise exception 'Nomor referensi transfer wajib diisi'; end if;
      v_payment_status:='PENDING'::public.payment_status;
    elsif upper(v_method.method_type)='QRIS' or v_method.code='QRIS' then
      select qc.id into v_qris_configuration_id from public.qris_configurations qc where qc.business_id=v_business_id and qc.branch_id=p_branch_id and qc.is_active=true limit 1;
      if v_qris_configuration_id is null then raise exception 'QRIS belum dikonfigurasi untuk cabang ini'; end if;
      v_payment_status:='PENDING'::public.payment_status;
    else
      raise exception 'Metode pembayaran belum didukung: %',v_method.code;
    end if;

    insert into public.payments(id,business_id,branch_id,sale_id,payment_method_id,qris_configuration_id,payment_no,amount,currency_code,status,provider,external_transaction_id,idempotency_key,qr_reference,paid_at,verified_at,reconciliation_status,metadata)
    values(gen_random_uuid(),v_business_id,p_branch_id,v_sale_id,v_method.id,v_qris_configuration_id,'PAY-'||to_char(clock_timestamp(),'YYMMDDHH24MISSMS')||'-'||v_idx,v_amount,'IDR',v_payment_status,null,null,case when p_idempotency_key is null then null else p_idempotency_key||':'||(v_idx-1)::text end,null,case when v_payment_status='PAID' then now() else null end,case when v_payment_status='PAID' then now() else null end,case when v_payment_status='PAID' then 'MANUAL_VERIFIED' else 'UNRECONCILED' end,jsonb_build_object('reference',v_reference,'cash_received',v_cash_received,'qris_confirmed',false));
    if v_payment_status='PAID' then v_paid:=v_paid+v_amount; end if;
  end loop;

  if v_paid>=v_total then
    update public.sales set status='COMPLETED'::public.document_status,paid_amount=v_total,change_amount=v_change,updated_at=now() where id=v_sale_id;
    for v_item in select * from jsonb_array_elements(p_items) loop
      perform public.ubah_stok_atomic(p_location_id,(v_item->>'product_id')::uuid,-((v_item->>'qty')::numeric),'SALE',v_sale_id,'Penjualan POS');
    end loop;
  else
    update public.sales set status='OPEN'::public.document_status,paid_amount=v_paid,change_amount=v_change,updated_at=now() where id=v_sale_id;
  end if;

  return query select v_sale_id,v_sale_no,v_subtotal,v_discount,v_total,v_paid,v_change,(select s.status::text from public.sales s where s.id=v_sale_id);
end;
$$;

revoke execute on function public.checkout_sale_multi_payment_v2(uuid,uuid,uuid,jsonb,jsonb,bigint,text) from public, anon, authenticated;
grant execute on function public.checkout_sale_multi_payment_v2(uuid,uuid,uuid,jsonb,jsonb,bigint,text) to anon, authenticated, service_role;
