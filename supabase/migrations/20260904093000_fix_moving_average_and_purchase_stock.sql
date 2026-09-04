create or replace function public.ubah_stok_atomic(
  p_location_id uuid,
  p_product_id uuid,
  p_quantity numeric,
  p_reference_type text,
  p_reference_id uuid default null,
  p_note text default null
)
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
  v_new numeric;
  v_business_id uuid;
  v_branch_id uuid;
  v_product_business_id uuid;
  v_movement_type stock_movement_type;
begin
  if auth.uid() is null then raise exception 'Autentikasi diperlukan'; end if;
  if p_quantity = 0 then raise exception 'Perubahan stok tidak boleh nol'; end if;

  select b.business_id, b.id
    into v_business_id, v_branch_id
  from locations l
  join branches b on b.id = l.branch_id
  where l.id = p_location_id
    and b.is_active = true;

  if v_branch_id is null then raise exception 'Lokasi tidak dapat diakses'; end if;
  if not exists (select 1 from private.current_business_ids() x where x = v_business_id) then
    raise exception 'Akses bisnis ditolak';
  end if;
  if not exists (select 1 from private.current_branch_ids() x where x = v_branch_id) then
    raise exception 'Akses cabang ditolak';
  end if;

  select business_id into v_product_business_id from products where id = p_product_id and is_active = true;
  if v_product_business_id is null or v_product_business_id <> v_business_id then
    raise exception 'Produk tidak valid untuk bisnis ini';
  end if;

  v_movement_type := case upper(coalesce(p_reference_type,''))
    when 'SALE' then 'SALE'::stock_movement_type
    when 'SALE_RETURN' then 'SALE_RETURN'::stock_movement_type
    when 'PURCHASE_RETURN' then 'PURCHASE_RETURN'::stock_movement_type
    when 'TRANSFER_IN' then 'TRANSFER_IN'::stock_movement_type
    when 'TRANSFER_OUT' then 'TRANSFER_OUT'::stock_movement_type
    when 'ADJUSTMENT_IN' then 'ADJUSTMENT_IN'::stock_movement_type
    when 'ADJUSTMENT_OUT' then 'ADJUSTMENT_OUT'::stock_movement_type
    when 'OPENING' then 'OPENING'::stock_movement_type
    when 'STOCK_OPNAME' then 'STOCK_OPNAME'::stock_movement_type
    when 'PURCHASE_RECEIPT', 'GOODS_RECEIPT' then 'PURCHASE_RECEIPT'::stock_movement_type
    else raise exception 'Jenis referensi stok tidak didukung: %', p_reference_type
  end;

  insert into stock_balances(location_id, product_id, qty_base)
  values (p_location_id, p_product_id, 0)
  on conflict(location_id, product_id) do nothing;

  update stock_balances
     set qty_base = qty_base + p_quantity,
         updated_at = now()
   where location_id = p_location_id
     and product_id = p_product_id
  returning qty_base into v_new;

  if v_new < 0 then raise exception 'Stok tidak mencukupi'; end if;

  insert into stock_movements(
    id, business_id, branch_id, location_id, product_id, movement_type,
    qty_base, unit_cost, reference_type, reference_id, reason, created_by
  )
  select
    gen_random_uuid(), v_business_id, v_branch_id, p_location_id, p_product_id,
    v_movement_type, p_quantity,
    coalesce((select current_cost from products where id = p_product_id), 0),
    p_reference_type, p_reference_id, p_note, auth.uid();

  return v_new;
end;
$$;

grant execute on function public.ubah_stok_atomic(uuid,uuid,numeric,text,uuid,text) to authenticated;
revoke execute on function public.ubah_stok_atomic(uuid,uuid,numeric,text,uuid,text) from anon;

create or replace function public.purchase_order_receive(
  p_purchase_order_id uuid,
  p_location_id uuid,
  p_supplier_invoice_no text default null,
  p_supplier_invoice_date date default null,
  p_due_date date default null,
  p_payment_mode text default 'PAID'
)
returns table(goods_receipt_id uuid, receipt_no text, payable_id uuid, purchase_order_status text)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_po purchase_orders%rowtype;
  v_gr uuid;
  v_receipt_no text;
  v_payable uuid;
  v_item purchase_order_items%rowtype;
  v_remaining numeric;
  v_due date;
  v_mode text := upper(coalesce(p_payment_mode,'PAID'));
  v_product products%rowtype;
  v_conversion numeric;
  v_old_qty numeric;
  v_old_cost bigint;
  v_received_base numeric;
  v_received_cost bigint;
  v_new_cost bigint;
  v_total_received_cost bigint;
begin
  if auth.uid() is null then raise exception 'Autentikasi diperlukan'; end if;

  select * into v_po
  from purchase_orders
  where id = p_purchase_order_id
  for update;

  if v_po.id is null then raise exception 'Purchase order tidak ditemukan.'; end if;
  if not exists (select 1 from private.current_business_ids() x where x = v_po.business_id) then raise exception 'Akses bisnis ditolak.'; end if;
  if not exists (select 1 from private.current_branch_ids() x where x = v_po.branch_id) then raise exception 'Akses cabang ditolak.'; end if;
  if v_po.status::text not in ('CONFIRMED','OPEN') then raise exception 'PO harus berstatus CONFIRMED sebelum diterima.'; end if;
  if not exists (select 1 from locations where id = p_location_id and branch_id = v_po.branch_id and is_active) then raise exception 'Lokasi penerimaan tidak valid untuk branch ini.'; end if;
  if v_mode not in ('PAID','CREDIT') then raise exception 'Mode pembayaran harus PAID atau CREDIT.'; end if;
  if v_mode = 'CREDIT' then
    v_due := coalesce(p_due_date, current_date);
    if v_due < current_date then raise exception 'Jatuh tempo tidak boleh sebelum hari ini.'; end if;
  else
    v_due := null;
  end if;

  v_receipt_no := 'GR-' || to_char(clock_timestamp(),'YYYYMMDDHH24MISSMS');

  insert into goods_receipts(
    business_id, branch_id, location_id, supplier_id, purchase_order_id,
    receipt_no, receipt_date, supplier_invoice_no, supplier_invoice_date,
    due_date, status, subtotal, discount_amount, tax_amount, other_cost,
    total_amount, notes, created_by
  ) values (
    v_po.business_id, v_po.branch_id, p_location_id, v_po.supplier_id, v_po.id,
    v_receipt_no, now(), nullif(trim(p_supplier_invoice_no),''), p_supplier_invoice_date,
    v_due, 'COMPLETED', v_po.subtotal, v_po.discount_amount, v_po.tax_amount,
    v_po.other_cost, v_po.total_amount, v_po.notes, auth.uid()
  ) returning id into v_gr;

  for v_item in
    select * from purchase_order_items
    where purchase_order_id = v_po.id
    for update
  loop
    v_remaining := v_item.qty - v_item.received_qty;
    if v_remaining > 0 then
      select * into v_product
      from products
      where id = v_item.product_id
        and business_id = v_po.business_id
        and is_active = true
      for update;
      if v_product.id is null then raise exception 'Produk pembelian tidak valid.'; end if;

      select pu.conversion_to_base into v_conversion
      from product_units pu
      where pu.product_id = v_item.product_id
        and pu.unit_id = v_item.unit_id
        and pu.is_purchase_unit = true
      limit 1;
      if v_conversion is null then
        select case when v_item.unit_id = v_product.base_unit_id then 1::numeric else null::numeric end into v_conversion;
      end if;
      if v_conversion is null then raise exception 'Satuan pembelian tidak memiliki konversi ke unit dasar untuk produk %.', v_product.name; end if;

      v_received_base := v_remaining * v_conversion;
      v_received_cost := round(v_remaining * v_item.unit_cost);

      select coalesce(sum(sb.qty_base),0) into v_old_qty
      from stock_balances sb
      join locations l on l.id = sb.location_id
      join branches br on br.id = l.branch_id
      where sb.product_id = v_item.product_id
        and br.business_id = v_po.business_id;

      v_old_cost := coalesce(v_product.current_cost,0);
      v_total_received_cost := v_received_cost;
      if v_received_base > 0 then
        if v_old_qty > 0 and v_old_cost > 0 then
          v_new_cost := round(((v_old_qty * v_old_cost) + v_total_received_cost) / (v_old_qty + v_received_base));
        else
          v_new_cost := round(v_total_received_cost / v_received_base);
        end if;
      else
        raise exception 'Qty penerimaan tidak valid.';
      end if;

      insert into goods_receipt_items(
        goods_receipt_id, product_id, unit_id, qty, unit_cost,
        discount_amount, tax_amount, line_total, batch_no, expiry_date
      ) values (
        v_gr, v_item.product_id, v_item.unit_id, v_remaining, v_item.unit_cost,
        0, 0, round(v_remaining * v_item.unit_cost), null, null
      );

      insert into stock_movements(
        business_id, branch_id, location_id, product_id, movement_type,
        qty_base, unit_cost, reference_type, reference_id, reference_item_id,
        reason, created_by
      ) values (
        v_po.business_id, v_po.branch_id, p_location_id, v_item.product_id,
        'PURCHASE_RECEIPT', v_received_base, round(v_total_received_cost / v_received_base),
        'GOODS_RECEIPT', v_gr, v_item.id, 'Penerimaan pembelian', auth.uid()
      );

      insert into stock_balances(location_id, product_id, qty_base)
      values (p_location_id, v_item.product_id, 0)
      on conflict(location_id, product_id) do nothing;

      update stock_balances
         set qty_base = qty_base + v_received_base,
             updated_at = now()
       where location_id = p_location_id
         and product_id = v_item.product_id;

      update products
         set current_cost = v_new_cost,
             last_purchase_cost = round(v_total_received_cost / v_received_base),
             updated_at = now()
       where id = v_item.product_id;

      update purchase_order_items set received_qty = qty where id = v_item.id;
    end if;
  end loop;

  update purchase_orders set status='COMPLETED', updated_at=now() where id=v_po.id;

  if v_mode='CREDIT' then
    insert into payables(
      business_id,branch_id,supplier_id,goods_receipt_id,invoice_no,invoice_date,
      due_date,original_amount,paid_amount,outstanding_amount,status
    ) values (
      v_po.business_id,v_po.branch_id,v_po.supplier_id,v_gr,
      coalesce(nullif(trim(p_supplier_invoice_no),''),v_receipt_no),
      coalesce(p_supplier_invoice_date,current_date),v_due,
      v_po.total_amount,0,v_po.total_amount,'OPEN'
    ) returning id into v_payable;
  else
    insert into payables(
      business_id,branch_id,supplier_id,goods_receipt_id,invoice_no,invoice_date,
      due_date,original_amount,paid_amount,outstanding_amount,status
    ) values (
      v_po.business_id,v_po.branch_id,v_po.supplier_id,v_gr,
      coalesce(nullif(trim(p_supplier_invoice_no),''),v_receipt_no),
      coalesce(p_supplier_invoice_date,current_date),current_date,
      v_po.total_amount,v_po.total_amount,0,'PAID'
    ) returning id into v_payable;
    insert into payable_payments(payable_id,amount,paid_at,reference,notes)
    values(v_payable,v_po.total_amount,now(),v_receipt_no,'Pembayaran pembelian saat penerimaan');
  end if;

  return query select v_gr,v_receipt_no,v_payable,'COMPLETED'::text;
end;
$$;

grant execute on function public.purchase_order_receive(uuid,uuid,text,date,date,text) to authenticated;
revoke execute on function public.purchase_order_receive(uuid,uuid,text,date,date,text) from anon;
