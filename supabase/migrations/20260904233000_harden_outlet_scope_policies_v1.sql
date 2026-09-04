-- Enforce outlet scope at the database boundary for financial and transaction reads.
-- Receivables/payables previously exposed business-wide rows; payments/sales already use branch scope.

drop policy if exists receivables_member_read on public.receivables;
create policy receivables_member_read on public.receivables
for select to authenticated
using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

drop policy if exists payables_member_read on public.payables;
create policy payables_member_read on public.payables
for select to authenticated
using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

drop policy if exists receivable_payments_member_read on public.receivable_payments;
create policy receivable_payments_member_read on public.receivable_payments
for select to authenticated
using (
  exists (
    select 1 from public.receivables r
    where r.id = receivable_payments.receivable_id
      and r.business_id in (select private.current_business_ids())
      and r.branch_id in (select private.current_branch_ids())
  )
);

drop policy if exists payable_payments_member_read on public.payable_payments;
create policy payable_payments_member_read on public.payable_payments
for select to authenticated
using (
  exists (
    select 1 from public.payables p
    where p.id = payable_payments.payable_id
      and p.business_id in (select private.current_business_ids())
      and p.branch_id in (select private.current_branch_ids())
  )
);

drop policy if exists purchase_order_items_member_read on public.purchase_order_items;
create policy purchase_order_items_member_read on public.purchase_order_items
for select to authenticated
using (
  exists (
    select 1 from public.purchase_orders po
    where po.id = purchase_order_items.purchase_order_id
      and po.business_id in (select private.current_business_ids())
      and po.branch_id in (select private.current_branch_ids())
  )
);

drop policy if exists goods_receipt_items_member_read on public.goods_receipt_items;
create policy goods_receipt_items_member_read on public.goods_receipt_items
for select to authenticated
using (
  exists (
    select 1 from public.goods_receipts gr
    where gr.id = goods_receipt_items.goods_receipt_id
      and gr.business_id in (select private.current_business_ids())
      and gr.branch_id in (select private.current_branch_ids())
  )
);

drop policy if exists sale_items_branch_read on public.sale_items;
create policy sale_items_branch_read on public.sale_items
for select to authenticated
using (
  exists (
    select 1 from public.sales s
    where s.id = sale_items.sale_id
      and s.business_id in (select private.current_business_ids())
      and s.branch_id in (select private.current_branch_ids())
  )
);

create or replace function public.validate_inventory_scope()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare
  v_business_id uuid;
  v_branch_id uuid;
begin
  select b.business_id, b.id into v_business_id, v_branch_id
  from public.locations l
  join public.branches b on b.id = l.branch_id
  where l.id = new.location_id;
  if v_branch_id is null then raise exception 'Lokasi stok tidak valid.'; end if;
  if new.branch_id <> v_branch_id then raise exception 'Lokasi tidak berada pada cabang transaksi.'; end if;
  if new.business_id <> v_business_id then raise exception 'Lokasi tidak berada pada bisnis transaksi.'; end if;
  return new;
end;
$$;

drop trigger if exists trg_validate_stock_movement_scope on public.stock_movements;
create trigger trg_validate_stock_movement_scope
before insert or update on public.stock_movements
for each row execute function public.validate_inventory_scope();

create or replace function public.validate_stock_balance_scope()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
  if not exists (
    select 1 from public.locations l
    join public.branches b on b.id = l.branch_id
    where l.id = new.location_id and l.is_active and b.is_active
      and l.branch_id in (select private.current_branch_ids())
      and b.business_id in (select private.current_business_ids())
  ) then raise exception 'Lokasi stok tidak berada dalam outlet aktif yang dapat diakses.'; end if;
  return new;
end;
$$;

drop trigger if exists trg_validate_stock_balance_scope on public.stock_balances;
create trigger trg_validate_stock_balance_scope
before insert or update on public.stock_balances
for each row execute function public.validate_stock_balance_scope();

grant execute on function public.validate_inventory_scope() to authenticated;
grant execute on function public.validate_stock_balance_scope() to authenticated;
