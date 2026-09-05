create table if not exists public.expenses (
  id uuid primary key default gen_random_uuid(),
  business_id uuid not null references public.businesses(id) on delete cascade,
  branch_id uuid not null references public.branches(id) on delete restrict,
  expense_no text not null,
  expense_date timestamptz not null default now(),
  category text not null,
  description text,
  amount numeric(18,2) not null check (amount > 0),
  status text not null default 'POSTED' check (status in ('DRAFT','POSTED','VOID')),
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (business_id, expense_no)
);

create index if not exists expenses_business_branch_date_idx on public.expenses (business_id, branch_id, expense_date desc);
alter table public.expenses enable row level security;
revoke all on public.expenses from anon;
grant select, insert, update on public.expenses to authenticated;

drop policy if exists expenses_select on public.expenses;
drop policy if exists expenses_insert on public.expenses;
drop policy if exists expenses_update on public.expenses;

create policy expenses_select on public.expenses for select to authenticated using (
  exists (select 1 from public.business_users bu where bu.business_id=expenses.business_id and bu.user_id=(select auth.uid()) and bu.is_active=true)
  and exists (select 1 from public.branches b where b.id=expenses.branch_id and b.business_id=expenses.business_id)
);

create policy expenses_insert on public.expenses for insert to authenticated with check (
  exists (select 1 from public.business_users bu where bu.business_id=expenses.business_id and bu.user_id=(select auth.uid()) and bu.is_active=true)
  and exists (select 1 from public.user_branch_access uba where uba.branch_id=expenses.branch_id and uba.user_id=(select auth.uid()))
  and exists (select 1 from public.branches b where b.id=expenses.branch_id and b.business_id=expenses.business_id and b.is_active=true)
  and expenses.created_by=(select auth.uid())
);

create policy expenses_update on public.expenses for update to authenticated using (
  exists (select 1 from public.business_users bu join public.roles r on r.id=bu.role_id where bu.business_id=expenses.business_id and bu.user_id=(select auth.uid()) and bu.is_active=true and r.code in ('OWNER','ADMIN'))
) with check (
  exists (select 1 from public.business_users bu join public.roles r on r.id=bu.role_id where bu.business_id=expenses.business_id and bu.user_id=(select auth.uid()) and bu.is_active=true and r.code in ('OWNER','ADMIN'))
);

create or replace function public.expenses_touch_updated_at() returns trigger language plpgsql as $$
begin new.updated_at=now(); return new; end;
$$;

drop trigger if exists expenses_touch_updated_at on public.expenses;
create trigger expenses_touch_updated_at before update on public.expenses for each row execute function public.expenses_touch_updated_at();