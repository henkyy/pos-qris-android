-- Functional smoke-test findings:
-- 1) RECEIVABLE was missing from the live payment_methods data.
-- 2) The public checkout wrapper delegated to a SECURITY DEFINER private function
--    without enforcing authenticated business/branch membership at the wrapper boundary.
-- 3) The existing receivable checkout function was not present in the live database,
--    and the historical version did not create a receivable ledger row.

create or replace function public.checkout_sale_receivable_v2(
  p_branch_id uuid,
  p_location_id uuid,
  p_customer_id uuid,
  p_items jsonb,
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
  v_sale_no text := 'AR-' || to_char(clock_timestamp(),'YYMMDDHH24MISSMS');
  v_subtotal bigint := 0;
  v_discount bigint := greatest(coalesce(p_discount_amount,0),0);
  v_total bigint := 0;
  v_hpp bigint := 0;
  v_item jsonb;
  v_product record;
  v_price bigint;
  v_conversion numeric;
  v_customer_term integer := 0;
  v_existing_sale uuid;
  v_existing_sale_no text;
  v_existing_subtotal bigint;
  v_existing_discount bigint;
  v_existing_total bigint;
  v_existing_paid bigint;
  v_existing_change bigint;
  v_existing_status text;
begin
  select br.business_id into v_business_id
  from public.branches br
  where br.id=p_branch_id and br.is_active=true;
  if v_business_id is null then raise exception 'Cabang tidak valid atau tidak aktif'; end if;
  if not exists (select 1 from private.current_business_ids() x where x.business_id=v_business_id) then raise exception 'Akses bisnis ditolak'; end if;
  if not exists (select 1 from private.current_branch_ids() x where x.branch_id=p_branch_id) then raise exception 'Akses cabang ditolak'; end if;
  if not exists(select 1 from public.locations l where l.id=p_location_id and l.branch_id=p_branch_id and l.is_active=true) then raise exception 'Lokasi stok tidak valid atau tidak aktif'; end if;
  if p_customer_id is null then raise exception 'Pelanggan wajib dipilih untuk transaksi piutang'; end if;
  select c.payment_term_days into v_customer_term
  from public.customers c
  where c.id=p_customer_id and c.business_id=v_business_id and c.is_active=true;
  if not found then raise exception 'Pelanggan tidak valid'; end if;

  if p_idempotency_key is not null then
    select s.id,s.sale_no,s.subtotal,s.discount_amount,s.total_amount,s.paid_amount,s.change_amount,s.status::text
      into v_existing_sale,v_existing_sale_no,v_existing_subtotal,v_existing_discount,v_existing_total,v_existing_paid,v_existing_change,v_existing_status
    from public.sales s
    where s.business_id=v_business_id and s.customer_id=p_customer_id and s.notes='POS_RECEIVABLE:'||p_idempotency_key
    limit 1;
    if v_existing_sale is not null then
      return query select v_existing_sale,v_existing_sale_no,v_existing_subtotal,v_existing_discount,v_existing_total,v_existing_paid,v_existing_change,v_existing_status;
      return;
    end if;
  end if;

  if jsonb_typeof(p_items)<>'array' or jsonb_array_length(p_items)=0 then raise exception 'Item transaksi kosong'; end if;

  for v_item in select * from jsonb_array_elements(p_items) loop
    if coalesce((v_item->>'qty')::numeric,0)<=0 then raise exception 'Qty harus lebih dari 0'; end if;
    select p.sku,p.name,p.current_cost into v_product
    from public.products p
    where p.id=(v_item->>'product_id')::uuid and p.business_id=v_business_id and p.is_active=true;
    if not found then raise exception 'Produk tidak valid'; end if;
    select pu.conversion_to_base into v_conversion
    from public.product_units pu
    join public.units u on u.id=pu.unit_id
    where pu.product_id=(v_item->>'product_id')::uuid and pu.unit_id=(v_item->>'unit_id')::uuid and pu.is_sales_unit=true and u.business_id=v_business_id
    limit 1;
    if v_conversion is null then raise exception 'Satuan penjualan tidak valid untuk produk'; end if;
    select pp.price into v_price
    from public.product_prices pp
    join public.price_lists pl on pl.id=pp.price_list_id
    where pl.business_id=v_business_id and pl.is_default=true and pl.is_active=true
      and pp.product_id=(v_item->>'product_id')::uuid and pp.unit_id=(v_item->>'unit_id')::uuid
      and pp.min_qty<=(v_item->>'qty')::numeric
      and (pp.valid_from is null or pp.valid_from<=now())
      and (pp.valid_until is null or pp.valid_until>=now())
    order by pp.min_qty desc,pp.valid_from desc nulls last limit 1;
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
  values(v_sale_id,v_business_id,p_branch_id,p_location_id,p_customer_id,v_sale_no,'OPEN'::public.document_status,v_subtotal,v_discount,0,0,0,v_total,0,0,v_hpp,v_total-v_hpp,'POS_RECEIVABLE:'||coalesce(p_idempotency_key,v_sale_id::text));

  for v_item in select * from jsonb_array_elements(p_items) loop
    select p.sku,p.name,p.current_cost into v_product from public.products p where p.id=(v_item->>'product_id')::uuid and p.business_id=v_business_id and p.is_active=true;
    select pu.conversion_to_base into v_conversion from public.product_units pu where pu.product_id=(v_item->>'product_id')::uuid and pu.unit_id=(v_item->>'unit_id')::uuid and pu.is_sales_unit=true limit 1;
    select pp.price into v_price from public.product_prices pp join public.price_lists pl on pl.id=pp.price_list_id where pl.business_id=v_business_id and pl.is_default=true and pl.is_active=true and pp.product_id=(v_item->>'product_id')::uuid and pp.unit_id=(v_item->>'unit_id')::uuid and pp.min_qty<=(v_item->>'qty')::numeric and (pp.valid_from is null or pp.valid_from<=now()) and (pp.valid_until is null or pp.valid_until>=now()) order by pp.min_qty desc,pp.valid_from desc nulls last limit 1;
    insert into public.sale_items(id,sale_id,product_id,unit_id,product_sku_snapshot,product_name_snapshot,qty,conversion_to_base,unit_price,discount_amount,tax_amount,line_total,hpp_unit,hpp_total)
    values(gen_random_uuid(),v_sale_id,(v_item->>'product_id')::uuid,(v_item->>'unit_id')::uuid,v_product.sku,v_product.name,(v_item->>'qty')::numeric,v_conversion,v_price,0,0,((v_item->>'qty')::numeric*v_price)::bigint,coalesce(v_product.current_cost,0),((v_item->>'qty')::numeric*coalesce(v_product.current_cost,0))::bigint);
    perform public.ubah_stok_atomic(p_location_id,(v_item->>'product_id')::uuid,-((v_item->>'qty')::numeric),'SALE',v_sale_id,'Penjualan POS piutang');
  end loop;

  insert into public.receivables(id,business_id,branch_id,customer_id,sale_id,invoice_no,invoice_date,due_date,original_amount,paid_amount,outstanding_amount,status,notes)
  values(gen_random_uuid(),v_business_id,p_branch_id,p_customer_id,v_sale_id,v_sale_no,now(),current_date+greatest(coalesce(v_customer_term,0),0),v_total,0,v_total,'OPEN','POS_RECEIVABLE');

  return query select v_sale_id,v_sale_no,v_subtotal,v_discount,v_total,0,0,'OPEN'::text;
end;
$$;

revoke execute on function public.checkout_sale_receivable_v2(uuid,uuid,uuid,jsonb,bigint,text) from public,anon;
grant execute on function public.checkout_sale_receivable_v2(uuid,uuid,uuid,jsonb,bigint,text) to authenticated,service_role;

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
security invoker
set search_path = ''
as $$
declare
  v_business_id uuid;
  v_method_id uuid;
  v_method_code text;
begin
  select br.business_id into v_business_id from public.branches br where br.id=p_branch_id and br.is_active=true;
  if v_business_id is null then raise exception 'Cabang tidak valid atau tidak aktif'; end if;
  if not exists (select 1 from private.current_business_ids() x where x.business_id=v_business_id) then raise exception 'Akses bisnis ditolak'; end if;
  if not exists (select 1 from private.current_branch_ids() x where x.branch_id=p_branch_id) then raise exception 'Akses cabang ditolak'; end if;
  if jsonb_typeof(p_payments)<>'array' or jsonb_array_length(p_payments)=0 then raise exception 'Pembayaran kosong'; end if;
  if jsonb_array_length(p_payments)=1 then
    v_method_id := (p_payments->0->>'payment_method_id')::uuid;
    select upper(pm.code) into v_method_code from public.payment_methods pm where pm.id=v_method_id and pm.business_id=v_business_id and pm.is_active=true;
    if v_method_code='RECEIVABLE' then
      return query select * from public.checkout_sale_receivable_v2(p_branch_id,p_location_id,p_customer_id,p_items,p_discount_amount,p_idempotency_key);
      return;
    end if;
  end if;
  return query select * from private.checkout_sale_multi_payment_v2(p_branch_id,p_location_id,p_customer_id,p_items,p_payments,p_discount_amount,p_idempotency_key);
end;
$$;

revoke execute on function public.checkout_sale_multi_payment_v2(uuid,uuid,uuid,jsonb,jsonb,bigint,text) from public,anon;
grant execute on function public.checkout_sale_multi_payment_v2(uuid,uuid,uuid,jsonb,jsonb,bigint,text) to authenticated,service_role;
