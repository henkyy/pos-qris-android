-- Purchase workflow permissions and authenticated RLS.
-- Draft PO creation remains the only direct transaction write exposed to the client;
-- confirmation, receipt, stock and payable mutations stay behind secured RPCs.

grant insert on table public.purchase_orders, public.purchase_order_items to authenticated;
revoke update, delete on table public.purchase_orders, public.purchase_order_items from authenticated;

drop policy if exists public_purchase_order_access on public.purchase_orders;
drop policy if exists public_purchase_order_item_access on public.purchase_order_items;
drop policy if exists public_goods_receipt_access on public.goods_receipts;
drop policy if exists public_goods_receipt_item_access on public.goods_receipt_items;

create policy purchase_orders_member_read on public.purchase_orders
for select to authenticated
using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy purchase_orders_member_insert on public.purchase_orders
for insert to authenticated
with check (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy purchase_order_items_member_read on public.purchase_order_items
for select to authenticated
using (
  purchase_order_id in (
    select po.id from public.purchase_orders po
    where po.business_id in (select private.current_business_ids())
      and po.branch_id in (select private.current_branch_ids())
  )
);

create policy purchase_order_items_member_insert on public.purchase_order_items
for insert to authenticated
with check (
  purchase_order_id in (
    select po.id from public.purchase_orders po
    where po.business_id in (select private.current_business_ids())
      and po.branch_id in (select private.current_branch_ids())
      and po.status = 'DRAFT'::public.document_status
  )
);

create policy goods_receipts_member_read on public.goods_receipts
for select to authenticated
using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy goods_receipt_items_member_read on public.goods_receipt_items
for select to authenticated
using (
  goods_receipt_id in (
    select gr.id from public.goods_receipts gr
    where gr.business_id in (select private.current_business_ids())
      and gr.branch_id in (select private.current_branch_ids())
  )
);

revoke all on public.purchase_orders, public.purchase_order_items, public.goods_receipts, public.goods_receipt_items from anon;

-- Remove legacy anonymous access left by the original demo policies.
drop policy if exists public_business_access on public.businesses;
drop policy if exists public_branch_access on public.branches;
drop policy if exists public_payment_method_access on public.payment_methods;
drop policy if exists public_payment_read on public.payments;
drop policy if exists public_price_list_access on public.price_lists;
drop policy if exists public_product_price_access on public.product_prices;
drop policy if exists public_sale_read on public.sales;
drop policy if exists public_sale_item_read on public.sale_items;
drop policy if exists public_stock_balance_read on public.stock_balances;
drop policy if exists public_stock_movement_read on public.stock_movements;

revoke all on public.businesses, public.branches, public.payment_methods, public.payments, public.price_lists, public.product_prices, public.sales, public.sale_items, public.stock_balances, public.stock_movements from anon;
