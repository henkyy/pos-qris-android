create or replace function public.purchase_order_confirm(p_purchase_order_id uuid)
returns table(purchase_order_id uuid, status text)
language plpgsql security definer set search_path = public
as $$
declare v_business uuid; v_status text;
begin
  select business_id,status::text into v_business,v_status from purchase_orders where id=p_purchase_order_id for update;
  if v_business is null then raise exception 'Purchase order tidak ditemukan.'; end if;
  if not exists (select 1 from business_users where business_id=v_business and user_id=auth.uid() and is_active) then raise exception 'Akses bisnis ditolak.'; end if;
  if v_status <> 'DRAFT' then raise exception 'Hanya PO DRAFT yang dapat dikonfirmasi.'; end if;
  update purchase_orders set status='CONFIRMED',updated_at=now() where id=p_purchase_order_id;
  return query select id,status::text from purchase_orders where id=p_purchase_order_id;
end $$;
grant execute on function public.purchase_order_confirm(uuid) to anon,authenticated;

create or replace function public.purchase_order_receive(p_purchase_order_id uuid,p_location_id uuid,p_supplier_invoice_no text default null,p_supplier_invoice_date date default null,p_due_date date default null,p_payment_mode text default 'PAID')
returns table(goods_receipt_id uuid, receipt_no text, payable_id uuid, purchase_order_status text)
language plpgsql security definer set search_path = public
as $$
declare v_po purchase_orders%rowtype; v_gr uuid; v_receipt_no text; v_payable uuid; v_item purchase_order_items%rowtype; v_remaining numeric; v_due date; v_mode text := upper(coalesce(p_payment_mode,'PAID'));
begin
  select * into v_po from purchase_orders where id=p_purchase_order_id for update;
  if v_po.id is null then raise exception 'Purchase order tidak ditemukan.'; end if;
  if not exists (select 1 from business_users where business_id=v_po.business_id and user_id=auth.uid() and is_active) then raise exception 'Akses bisnis ditolak.'; end if;
  if v_po.status::text not in ('CONFIRMED','OPEN') then raise exception 'PO harus berstatus CONFIRMED sebelum diterima.'; end if;
  if not exists (select 1 from locations where id=p_location_id and branch_id=v_po.branch_id and is_active) then raise exception 'Lokasi penerimaan tidak valid untuk branch ini.'; end if;
  if v_mode not in ('PAID','CREDIT') then raise exception 'Mode pembayaran harus PAID atau CREDIT.'; end if;
  if v_mode='CREDIT' then v_due:=coalesce(p_due_date,current_date); if v_due<current_date then raise exception 'Jatuh tempo tidak boleh sebelum hari ini.'; end if; else v_due:=null; end if;
  v_receipt_no:='GR-'||to_char(clock_timestamp(),'YYYYMMDDHH24MISSMS');
  insert into goods_receipts(business_id,branch_id,location_id,supplier_id,purchase_order_id,receipt_no,receipt_date,supplier_invoice_no,supplier_invoice_date,due_date,status,subtotal,discount_amount,tax_amount,other_cost,total_amount,notes,created_by)
  values(v_po.business_id,v_po.branch_id,p_location_id,v_po.supplier_id,v_po.id,v_receipt_no,now(),nullif(trim(p_supplier_invoice_no),''),p_supplier_invoice_date,v_due,'COMPLETED',v_po.subtotal,v_po.discount_amount,v_po.tax_amount,v_po.other_cost,v_po.total_amount,v_po.notes,auth.uid()) returning id into v_gr;
  for v_item in select * from purchase_order_items where purchase_order_id=v_po.id for update loop
    v_remaining:=v_item.qty-v_item.received_qty;
    if v_remaining>0 then
      insert into goods_receipt_items(goods_receipt_id,product_id,unit_id,qty,unit_cost,discount_amount,tax_amount,line_total,batch_no,expiry_date) values(v_gr,v_item.product_id,v_item.unit_id,v_remaining,v_item.unit_cost,0,0,round(v_remaining*v_item.unit_cost),null,null);
      insert into stock_movements(business_id,branch_id,location_id,product_id,movement_type,qty_base,unit_cost,reference_type,reference_id,reference_item_id,reason,created_by) values(v_po.business_id,v_po.branch_id,p_location_id,v_item.product_id,'PURCHASE_RECEIPT',v_remaining,v_item.unit_cost,'GOODS_RECEIPT',v_gr,v_item.id,'Penerimaan pembelian',auth.uid());
      update purchase_order_items set received_qty=qty where id=v_item.id;
    end if;
  end loop;
  update purchase_orders set status='COMPLETED',updated_at=now() where id=v_po.id;
  if v_mode='CREDIT' then
    insert into payables(business_id,branch_id,supplier_id,goods_receipt_id,invoice_no,invoice_date,due_date,original_amount,paid_amount,outstanding_amount,status) values(v_po.business_id,v_po.branch_id,v_po.supplier_id,v_gr,coalesce(nullif(trim(p_supplier_invoice_no),''),v_receipt_no),coalesce(p_supplier_invoice_date,current_date),v_due,v_po.total_amount,0,v_po.total_amount,'OPEN') returning id into v_payable;
  else
    insert into payables(business_id,branch_id,supplier_id,goods_receipt_id,invoice_no,invoice_date,due_date,original_amount,paid_amount,outstanding_amount,status) values(v_po.business_id,v_po.branch_id,v_po.supplier_id,v_gr,coalesce(nullif(trim(p_supplier_invoice_no),''),v_receipt_no),coalesce(p_supplier_invoice_date,current_date),current_date,v_po.total_amount,v_po.total_amount,0,'PAID') returning id into v_payable;
    insert into payable_payments(payable_id,amount,paid_at,reference,notes) values(v_payable,v_po.total_amount,now(),v_receipt_no,'Pembayaran pembelian saat penerimaan');
  end if;
  return query select v_gr,v_receipt_no,v_payable,'COMPLETED'::text;
end $$;
grant execute on function public.purchase_order_receive(uuid,uuid,text,date,date,text) to anon,authenticated;

create or replace function public.payable_register_payment(p_payable_id uuid,p_amount bigint,p_reference text default null,p_notes text default null)
returns table(payable_id uuid, paid_amount bigint, outstanding_amount bigint, status text)
language plpgsql security definer set search_path = public
as $$
declare v_payable payables%rowtype; v_new_paid bigint; v_out bigint; v_status text;
begin
  if p_amount<=0 then raise exception 'Jumlah pembayaran harus lebih dari 0.'; end if;
  select * into v_payable from payables where id=p_payable_id for update;
  if v_payable.id is null then raise exception 'Hutang tidak ditemukan.'; end if;
  if not exists (select 1 from business_users where business_id=v_payable.business_id and user_id=auth.uid() and is_active) then raise exception 'Akses bisnis ditolak.'; end if;
  if p_amount>v_payable.outstanding_amount then raise exception 'Pembayaran melebihi saldo hutang.'; end if;
  v_new_paid:=v_payable.paid_amount+p_amount; v_out:=v_payable.original_amount-v_new_paid; v_status:=case when v_out=0 then 'PAID' else 'OPEN' end;
  insert into payable_payments(payable_id,amount,paid_at,reference,notes) values(p_payable_id,p_amount,now(),nullif(trim(p_reference),''),nullif(trim(p_notes),''));
  update payables set paid_amount=v_new_paid,outstanding_amount=v_out,status=v_status where id=p_payable_id;
  return query select p_payable_id,v_new_paid,v_out,v_status;
end $$;
grant execute on function public.payable_register_payment(uuid,bigint,text,text) to anon,authenticated;
