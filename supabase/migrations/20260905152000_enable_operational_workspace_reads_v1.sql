create policy sales_returns_member_read on public.sales_returns for select to authenticated using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy stock_transfers_member_read on public.stock_transfers for select to authenticated using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy cashier_shifts_member_read on public.cashier_shifts for select to authenticated using (
  business_id in (select private.current_business_ids())
  and branch_id in (select private.current_branch_ids())
);

create policy cash_movements_member_read on public.cash_movements for select to authenticated using (
  exists (
    select 1 from public.cashier_shifts s
    where s.id = cash_movements.shift_id
      and s.business_id in (select private.current_business_ids())
      and s.branch_id in (select private.current_branch_ids())
  )
);
