create or replace function public.purchase_order_create(
  p_business_id uuid,
  p_branch_id uuid,
  p_supplier_id uuid,
  p_order_no text,
  p_order_date timestamptz,
  p_expected_date date,
  p_discount_amount bigint,
  p_tax_amount bigint,
  p_other_cost bigint,
  p_total_amount bigint,
  p_notes text,
  p_items jsonb
)
returns table(purchase_order_id uuid, order_no text, status text)
language plpgsql
security definer
set search_path = public, private
as $$
declare
  v_order_id uuid;
  v_item jsonb;
  v_subtotal bigint := 0;
  v_discount bigint := greatest(coalesce(p_discount_amount, 0), 0);
  v_tax bigint := greatest(coalesce(p_tax_amount, 0), 0);
  v_other bigint := greatest(coalesce(p_other_cost, 0), 0);
  v_total bigint;
  v_qty numeric;
  v_unit_cost bigint;
  v_line_total bigint;
  v_product_id uuid;
  v_unit_id uuid;
begin
  if auth.uid() is null then raise exception 'Sesi login tidak valid.'; end if;
  if not exists (select 1 from public.business_users bu where bu.business_id=p_business_id and bu.user_id=auth.uid() and bu.is_active=true) then raise exception 'Akses business ditolak.'; end if;
  if not exists (select 1 from public.user_branch_access uba where uba.branch_id=p_branch_id and uba.user_id=auth.uid()) then raise exception 'Akses branch ditolak.'; end if;
  if not exists (select 1 from public.branches b where b.id=p_branch_id and b.business_id=p_business_id and b.is_active=true) then raise exception 'Branch tidak valid untuk business ini.'; end if;
  if not exists (select 1 from public.suppliers s where s.id=p_supplier_id and s.business_id=p_business_id and s.is_active=true) then raise exception 'Supplier tidak valid untuk business ini.'; end if;
  if coalesce(trim(p_order_no),'')='' then raise exception 'Nomor PO wajib diisi.'; end if;
  if p_items is null or jsonb_typeof(p_items)<>'array' or jsonb_array_length(p_items)=0 then raise exception 'PO minimal memiliki satu item.'; end if;

  for v_item in select * from jsonb_array_elements(p_items) loop
    v_product_id := (v_item->>'product_id')::uuid;
    v_unit_id := (v_item->>'unit_id')::uuid;
    v_qty := (v_item->>'qty')::numeric;
    v_unit_cost := greatest(round(coalesce((v_item->>'unit_cost')::numeric,0)),0)::bigint;
    if v_qty is null or v_qty<=0 then raise exception 'Qty item harus lebih dari 0.'; end if;
    if not exists (select 1 from public.products p where p.id=v_product_id and p.business_id=p_business_id and p.is_active=true) then raise exception 'Produk tidak valid untuk business ini.'; end if;
    if not exists (select 1 from public.product_units pu where pu.product_id=v_product_id and pu.unit_id=v_unit_id) then raise exception 'Unit produk tidak valid.'; end if;
    v_line_total := greatest(round(v_qty*v_unit_cost),0)::bigint;
    v_subtotal := v_subtotal + v_line_total;
  end loop;

  v_total := greatest(v_subtotal-v_discount+v_tax+v_other,0);

  insert into public.purchase_orders (
    business_id, branch_id, supplier_id, order_no, order_date, expected_date,
    status, subtotal, discount_amount, tax_amount, other_cost, total_amount, notes, created_by
  ) values (
    p_business_id, p_branch_id, p_supplier_id, p_order_no, coalesce(p_order_date,now()), p_expected_date,
    'DRAFT'::public.document_status, v_subtotal, v_discount, v_tax, v_other, v_total, nullif(p_notes,''), auth.uid()
  ) returning id into v_order_id;

  for v_item in select * from jsonb_array_elements(p_items) loop
    v_product_id := (v_item->>'product_id')::uuid;
    v_unit_id := (v_item->>'unit_id')::uuid;
    v_qty := (v_item->>'qty')::numeric;
    v_unit_cost := greatest(round(coalesce((v_item->>'unit_cost')::numeric,0)),0)::bigint;
    insert into public.purchase_order_items (
      purchase_order_id, product_id, unit_id, qty, unit_cost, discount_amount, tax_amount, line_total, received_qty
    ) values (v_order_id,v_product_id,v_unit_id,v_qty,v_unit_cost,0,0,greatest(round(v_qty*v_unit_cost),0)::bigint,0);
  end loop;

  return query select v_order_id,p_order_no,'DRAFT';
end;
$$;

revoke all on function public.purchase_order_create(uuid,uuid,uuid,text,timestamptz,date,bigint,bigint,bigint,bigint,text,jsonb) from public, anon;
grant execute on function public.purchase_order_create(uuid,uuid,uuid,text,timestamptz,date,bigint,bigint,bigint,bigint,text,jsonb) to authenticated;
