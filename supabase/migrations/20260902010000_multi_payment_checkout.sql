-- Multi-payment demo checkout: Cash + Transfer + QRIS.
-- QRIS stays PENDING unless explicitly confirmed by the cashier/provider flow.
create index if not exists idx_payments_sale_status on public.payments(sale_id,status);
create index if not exists idx_payments_idempotency on public.payments(idempotency_key) where idempotency_key is not null;

insert into public.payment_methods(id,business_id,code,name,method_type,is_active)
select gen_random_uuid(), b.id, 'TRANSFER', 'Transfer Bank', 'BANK_TRANSFER', true
from public.businesses b
where b.code='DEMO'
  and not exists (select 1 from public.payment_methods pm where pm.business_id=b.id and pm.code='TRANSFER');

create or replace function public.demo_checkout_multi_payment(
  p_branch_id uuid,p_location_id uuid,p_customer_id uuid,p_items jsonb,p_payments jsonb,p_idempotency_key text default null
)
returns table(sale_id uuid,sale_no text,total_amount bigint,paid_amount bigint,change_amount bigint,sale_status text)
language plpgsql security definer set search_path=public
as $$
declare v_business_id uuid; v_sale_id uuid:=gen_random_uuid(); v_sale_no text:='TRX-'||to_char(clock_timestamp(),'YYMMDDHH24MISSMS'); v_total bigint:=0; v_paid bigint:=0; v_change bigint:=0; v_hpp bigint:=0; v_item jsonb; v_payment jsonb; v_idx bigint; v_method record; v_payment_id uuid; v_payment_status public.payment_status; v_amount bigint; v_cash_received bigint; v_reference text; v_qris_confirmed boolean; v_existing_sale uuid; v_existing_sale_no text; v_existing_total bigint; v_existing_paid bigint; v_existing_change bigint; v_existing_status text;
begin
  select b.business_id into v_business_id from public.branches b where b.id=p_branch_id and b.code='MAIN';
  if v_business_id is null then raise exception 'DEMO branch tidak valid'; end if;
  if not exists(select 1 from public.locations l where l.id=p_location_id and l.branch_id=p_branch_id and l.code='STORE') then raise exception 'DEMO location tidak valid'; end if;
  if p_idempotency_key is not null then
    select p.sale_id,s.sale_no,s.total_amount,s.paid_amount,s.change_amount,s.status::text into v_existing_sale,v_existing_sale_no,v_existing_total,v_existing_paid,v_existing_change,v_existing_status
    from public.payments p join public.sales s on s.id=p.sale_id where p.idempotency_key=p_idempotency_key||':0' limit 1;
    if v_existing_sale is not null then return query select v_existing_sale,v_existing_sale_no,v_existing_total,v_existing_paid,v_existing_change,v_existing_status; return; end if;
  end if;
  if jsonb_typeof(p_items)<>'array' or jsonb_array_length(p_items)=0 then raise exception 'Item transaksi kosong'; end if;
  if jsonb_typeof(p_payments)<>'array' or jsonb_array_length(p_payments)=0 then raise exception 'Pembayaran kosong'; end if;
  for v_item in select * from jsonb_array_elements(p_items) loop
    if coalesce((v_item->>'qty')::numeric,0)<=0 then raise exception 'Qty harus lebih dari 0'; end if;
    v_total:=v_total+((v_item->>'qty')::numeric*(v_item->>'unit_price')::bigint)::bigint;
    v_hpp:=v_hpp+((v_item->>'qty')::numeric*coalesce((v_item->>'hpp_unit')::bigint,0))::bigint;
  end loop;
  if v_total<=0 then raise exception 'Total transaksi tidak valid'; end if;
  insert into public.sales(id,business_id,branch_id,location_id,customer_id,sale_no,status,subtotal,discount_amount,tax_amount,service_charge,rounding_amount,total_amount,paid_amount,change_amount,hpp_amount,margin_amount,notes)
  values(v_sale_id,v_business_id,p_branch_id,p_location_id,p_customer_id,v_sale_no,'OPEN'::public.document_status,v_total,0,0,0,0,v_total,0,0,v_hpp,v_total-v_hpp,'Multi-payment POS');
  for v_item in select * from jsonb_array_elements(p_items) loop
    insert into public.sale_items(id,sale_id,product_id,unit_id,product_sku_snapshot,product_name_snapshot,qty,conversion_to_base,unit_price,discount_amount,tax_amount,line_total,hpp_unit,hpp_total)
    values(gen_random_uuid(),v_sale_id,(v_item->>'product_id')::uuid,(v_item->>'unit_id')::uuid,v_item->>'sku',v_item->>'name',(v_item->>'qty')::numeric,coalesce((v_item->>'conversion_to_base')::numeric,1),(v_item->>'unit_price')::bigint,0,0,((v_item->>'qty')::numeric*(v_item->>'unit_price')::bigint)::bigint,coalesce((v_item->>'hpp_unit')::bigint,0),((v_item->>'qty')::numeric*coalesce((v_item->>'hpp_unit')::bigint,0))::bigint);
  end loop;
  for v_payment,v_idx in select item,ordinality from jsonb_array_elements(p_payments) with ordinality as t(item,ordinality) loop
    select pm.* into v_method from public.payment_methods pm where pm.id=(v_payment->>'payment_method_id')::uuid and pm.business_id=v_business_id and pm.is_active=true;
    if not found then raise exception 'Metode pembayaran tidak valid'; end if;
    v_amount:=(v_payment->>'amount')::bigint; if v_amount<=0 then raise exception 'Nominal pembayaran harus lebih dari 0'; end if;
    v_cash_received:=coalesce((v_payment->>'cash_received')::bigint,v_amount); v_reference:=nullif(trim(v_payment->>'reference'),''); v_qris_confirmed:=coalesce((v_payment->>'qris_confirmed')::boolean,false);
    if v_method.code='CASH' or upper(v_method.method_type)='CASH' then
      if v_cash_received<v_amount then raise exception 'Uang tunai kurang dari nominal pembayaran'; end if; v_payment_status:='PAID'::public.payment_status; v_change:=v_change+(v_cash_received-v_amount);
    elsif v_method.code='QRIS' or upper(v_method.method_type)='QRIS' then
      v_payment_status:=case when v_qris_confirmed then 'PAID'::public.payment_status else 'PENDING'::public.payment_status end;
    elsif v_method.code='TRANSFER' or upper(v_method.method_type) in ('BANK_TRANSFER','TRANSFER') then
      if v_reference is null then raise exception 'Nomor referensi transfer wajib diisi'; end if; v_payment_status:='PAID'::public.payment_status;
    else raise exception 'Metode pembayaran belum didukung: %',v_method.code; end if;
    v_payment_id:=gen_random_uuid();
    insert into public.payments(id,business_id,branch_id,sale_id,payment_method_id,qris_configuration_id,payment_no,amount,currency_code,status,provider,external_transaction_id,idempotency_key,qr_reference,paid_at,verified_at,reconciliation_status,metadata)
    values(v_payment_id,v_business_id,p_branch_id,v_sale_id,v_method.id,case when v_method.code='QRIS' then (select qc.id from public.qris_configurations qc where qc.business_id=v_business_id and qc.is_active=true limit 1) else null end,'PAY-'||to_char(clock_timestamp(),'YYMMDDHH24MISSMS')||'-'||v_idx,v_amount,'IDR',v_payment_status,'DEMO',null,case when p_idempotency_key is null then null else p_idempotency_key||':'||(v_idx-1)::text end,null,case when v_payment_status='PAID' then now() else null end,case when v_payment_status='PAID' then now() else null end,case when v_payment_status='PAID' then 'MANUAL_VERIFIED' else 'UNRECONCILED' end,jsonb_build_object('reference',v_reference,'cash_received',v_cash_received,'qris_confirmed',v_qris_confirmed));
    if v_payment_status='PAID' then v_paid:=v_paid+v_amount; end if;
  end loop;
  if v_paid>=v_total then
    update public.sales set status='COMPLETED'::public.document_status,paid_amount=v_total,change_amount=v_change,updated_at=now() where id=v_sale_id;
    for v_item in select * from jsonb_array_elements(p_items) loop perform public.ubah_stok_atomic(p_location_id,(v_item->>'product_id')::uuid,-((v_item->>'qty')::numeric),'SALE',v_sale_id,'Multi-payment sale'); end loop;
  else update public.sales set status='OPEN'::public.document_status,paid_amount=v_paid,change_amount=v_change,updated_at=now() where id=v_sale_id; end if;
  return query select v_sale_id,v_sale_no,v_total,v_paid,v_change,(select s.status::text from public.sales s where s.id=v_sale_id);
end; $$;
grant execute on function public.demo_checkout_multi_payment(uuid,uuid,uuid,jsonb,jsonb,text) to anon,authenticated;