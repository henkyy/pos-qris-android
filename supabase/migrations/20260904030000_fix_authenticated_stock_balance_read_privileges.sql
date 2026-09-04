begin;

grant select on table public.stock_balances to authenticated;

alter table public.stock_balances enable row level security;

drop policy if exists stock_balances_branch_read on public.stock_balances;

create policy stock_balances_branch_read
on public.stock_balances
for select
to authenticated
using (
  location_id in (
    select l.id
    from public.locations l
    where l.branch_id in (select private.current_branch_ids())
      and l.is_active = true
  )
);

commit;
