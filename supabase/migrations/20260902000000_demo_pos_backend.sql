-- Demo backend hardening for the no-login test mode.
-- Production must replace these anon policies with authenticated business membership policies.

create or replace function public.demo_complete_sale(
  p_branch_id uuid,
  p_location_id uuid,
  p_customer_id uuid,
  p_payment_method_id uuid,
  p_items jsonb,
  p_idempotency_key text default null
) returns table(sale_id uuid, sale_no text, total_amount bigint)
language plpgsql security definer set search_path = public as $$
declare
  v_business_id uuid;
  v_sale_id uuid := gen_random_uuid();
  v_sale_no text := 'TRX-' || to_char(clock_timestamp(),'YYMMDDHH24MISSMS');
  v_total bigint;
  v_hpp bigint;
  v_item jsonb;
  v_product_id uuid;
  v_unit_id uuid;
  v_qty numeric;
  v_price bigint;
begin
  select b.id into v_business_id
  from businesses b join branches br on br.business_id=b.id
  where br.id=p_branch_id and br.code='MAIN' and b.code='DEMO' and b.is_active=true limit 1;
  if v_business_id is null then raise exception 'Demo business/branch not found'; end if;
  if p_items is null or jsonb_array_length(p_items)=0 then raise exception 'Items cannot be empty'; end if;
  select sum((x->>'qty')::numeric * (x->>'price')::bigint), sum((x->>'qty')::numeric * coalesce((x->>'hpp')::bigint,0))
    into v_total,v_hpp from jsonb_array_elements(p_items) x;
  if v_total <= 0 then raise exception 'Total must be positive'; end if;
  insert into sales(id,business_id,branch_id,location_id,customer_id,sale_no,status,subtotal,total_amount,paid_amount,hpp_amount,margin_amount,notes)
  values(v_sale_id,v_business_id,p_branch_id,p_location_id,p_customer_id,v_sale_no,'COMPLETED',v_total,v_total,v_total,v_hpp,v_total-v_hpp,'POS QRIS demo');
  for v_item in select * from jsonb_array_elements(p_items) loop
    v_product_id := (v_item->>'product_id')::uuid;
    v_unit_id := (v_item->>'unit_id')::uuid;
    v_qty := (v_item->>'qty')::numeric;
    v_price := (v_item->>'price')::bigint;
    if v_qty <= 0 then raise exception 'Invalid quantity'; end if;
    insert into sale_items(id,sale_id,product_id,unit_id,product_sku_snapshot,product_name_snapshot,qty,conversion_to_base,unit_price,line_total,hpp_unit,hpp_total)
    select gen_random_uuid(),v_sale_id,p.id,v_unit_id,p.sku,p.name,v_qty,1,v_price,v_qty*v_price,p.current_cost,v_qty*p.current_cost
    from products p where p.id=v_product_id and p.business_id=v_business_id and p.is_active=true;
    if not found then raise exception 'Product not found'; end if;
    update stock_balances set qty_base=qty_base-v_qty, updated_at=now() where location_id=p_location_id and product_id=v_product_id;
    if not found then raise exception 'Stock balance not found'; end if;
    if (select qty_base from stock_balances where location_id=p_location_id and product_id=v_product_id) < 0 then raise exception 'Insufficient stock'; end if;
    insert into stock_movements(id,business_id,branch_id,location_id,product_id,movement_type,qty_base,unit_cost,reference_type,reference_id,reason)
    select gen_random_uuid(),v_business_id,p_branch_id,p_location_id,v_product_id,'SALE',-v_qty,(select current_cost from products where id=v_product_id),'SALE',v_sale_id,'Penjualan POS';
  end loop;
  insert into payments(id,business_id,branch_id,sale_id,payment_method_id,payment_no,amount,currency_code,status,provider,external_transaction_id,idempotency_key,reconciliation_status,paid_at,verified_at)
  values(gen_random_uuid(),v_business_id,p_branch_id,v_sale_id,p_payment_method_id,'PAY-'||right(v_sale_no,12),v_total,'IDR','PAID','DEMO','DEMO-'||replace(v_sale_id::text,'-',''),coalesce(p_idempotency_key,'demo-'||v_sale_id::text),'UNRECONCILED',now(),now());
  return query select v_sale_id,v_sale_no,v_total;
end; $$;
revoke all on function public.demo_complete_sale(uuid,uuid,uuid,uuid,jsonb,text) from public;
grant execute on function public.demo_complete_sale(uuid,uuid,uuid,uuid,jsonb,text) to anon, authenticated;

do $$ begin
  execute 'drop policy if exists demo_access on suppliers';
  execute 'create policy demo_access on suppliers for all to anon using (business_id=(select id from businesses where code=''DEMO'' limit 1)) with check (business_id=(select id from businesses where code=''DEMO'' limit 1))';
  execute 'drop policy if exists demo_access on purchase_orders';
  execute 'create policy demo_access on purchase_orders for all to anon using (business_id=(select id from businesses where code=''DEMO'' limit 1)) with check (business_id=(select id from businesses where code=''DEMO'' limit 1))';
  execute 'drop policy if exists demo_access on purchase_order_items';
  execute 'create policy demo_access on purchase_order_items for all to anon using (purchase_order_id in (select id from purchase_orders where business_id=(select id from businesses where code=''DEMO'' limit 1))) with check (purchase_order_id in (select id from purchase_orders where business_id=(select id from businesses where code=''DEMO'' limit 1)))';
  execute 'drop policy if exists demo_access on goods_receipts';
  execute 'create policy demo_access on goods_receipts for all to anon using (business_id=(select id from businesses where code=''DEMO'' limit 1)) with check (business_id=(select id from businesses where code=''DEMO'' limit 1))';
  execute 'drop policy if exists demo_access on goods_receipt_items';
  execute 'create policy demo_access on goods_receipt_items for all to anon using (goods_receipt_id in (select id from goods_receipts where business_id=(select id from businesses where code=''DEMO'' limit 1))) with check (goods_receipt_id in (select id from goods_receipts where business_id=(select id from businesses where code=''DEMO'' limit 1)))';
  execute 'drop policy if exists demo_access on stock_movements';
  execute 'create policy demo_access on stock_movements for all to anon using (business_id=(select id from businesses where code=''DEMO'' limit 1)) with check (business_id=(select id from businesses where code=''DEMO'' limit 1))';
end $$;

insert into suppliers(id,business_id,code,name,phone,is_active)
select gen_random_uuid(),b.id,'SUP001','PT Sumber Pangan','08120000001',true from businesses b where b.code='DEMO'
union all select gen_random_uuid(),b.id,'SUP002','CV Minuman Nusantara','08120000002',true from businesses b where b.code='DEMO'
union all select gen_random_uuid(),b.id,'SUP003','UD Snack Jaya','08120000003',true from businesses b where b.code='DEMO'
  and not exists (select 1 from suppliers s where s.business_id=b.id and s.code in ('SUP001','SUP002','SUP003'));
