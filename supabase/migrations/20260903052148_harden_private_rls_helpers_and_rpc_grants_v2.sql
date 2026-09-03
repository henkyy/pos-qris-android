create schema if not exists private;

create or replace function private.current_business_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select bu.business_id
  from public.business_users bu
  where bu.user_id = (select auth.uid())
    and bu.is_active = true
$$;

create or replace function private.current_branch_ids()
returns setof uuid
language sql
stable
security definer
set search_path = ''
as $$
  select uba.branch_id
  from public.user_branch_access uba
  where uba.user_id = (select auth.uid())
$$;

create or replace function private.is_business_owner(p_business_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.business_users bu
    join public.roles r on r.id = bu.role_id
    where bu.business_id = p_business_id
      and bu.user_id = (select auth.uid())
      and bu.is_active = true
      and r.code = 'OWNER'
  )
$$;

revoke all on function private.current_business_ids() from public, anon, authenticated;
revoke all on function private.current_branch_ids() from public, anon, authenticated;
revoke all on function private.is_business_owner(uuid) from public, anon, authenticated;
grant usage on schema private to authenticated;
grant execute on function private.current_business_ids() to authenticated;
grant execute on function private.current_branch_ids() to authenticated;
grant execute on function private.is_business_owner(uuid) to authenticated;

do $$
declare
  p record;
  new_using text;
  new_check text;
  alter_sql text;
begin
  for p in
    select pp.schemaname, pp.tablename, pp.policyname, pp.cmd as policy_cmd, pp.qual, pp.with_check
    from pg_policies pp
    where pp.schemaname = 'public'
      and (
        coalesce(pp.qual, '') ilike '%current_business_ids%'
        or coalesce(pp.qual, '') ilike '%current_branch_ids%'
        or coalesce(pp.qual, '') ilike '%is_business_owner%'
        or coalesce(pp.with_check, '') ilike '%current_business_ids%'
        or coalesce(pp.with_check, '') ilike '%current_branch_ids%'
        or coalesce(pp.with_check, '') ilike '%is_business_owner%'
      )
  loop
    new_using := p.qual;
    new_check := p.with_check;
    if new_using is not null then
      new_using := replace(new_using, 'current_business_ids()', 'private.current_business_ids()');
      new_using := replace(new_using, 'current_branch_ids()', 'private.current_branch_ids()');
      new_using := replace(new_using, 'is_business_owner(', 'private.is_business_owner(');
    end if;
    if new_check is not null then
      new_check := replace(new_check, 'current_business_ids()', 'private.current_business_ids()');
      new_check := replace(new_check, 'current_branch_ids()', 'private.current_branch_ids()');
      new_check := replace(new_check, 'is_business_owner(', 'private.is_business_owner(');
    end if;
    if p.tablename in ('business_users', 'user_branch_access') then
      if new_using is not null then new_using := replace(new_using, 'auth.uid()', '(select auth.uid())'); end if;
      if new_check is not null then new_check := replace(new_check, 'auth.uid()', '(select auth.uid())'); end if;
    end if;
    alter_sql := format('alter policy %I on %I', p.policyname, p.tablename);
    if new_using is not null then alter_sql := alter_sql || format(' using (%s)', new_using); end if;
    if new_check is not null then alter_sql := alter_sql || format(' with check (%s)', new_check); end if;
    execute alter_sql;
  end loop;
end $$;

drop policy if exists qris_member_read_active on public.qris_configurations;

revoke execute on function public.complete_paid_sale(uuid) from public, anon, authenticated;
revoke execute on function public.confirm_static_qris_payment(uuid, text) from public, anon, authenticated;
revoke execute on function public.create_payment_for_sale(uuid, uuid, text) from public, anon, authenticated;
revoke execute on function public.create_pending_sale(uuid, uuid, jsonb, numeric, numeric, text) from public, anon, authenticated;
revoke execute on function public.current_branch_ids() from public, anon, authenticated;
revoke execute on function public.current_business_ids() from public, anon, authenticated;
revoke execute on function public.get_payment_status(uuid) from public, anon, authenticated;
revoke execute on function public.is_business_owner(uuid) from public, anon, authenticated;
revoke execute on function public.mark_payment_paid(uuid, text) from public, anon, authenticated;
revoke execute on function public.record_payment(uuid, uuid, numeric, text, text) from public, anon, authenticated;
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;
revoke execute on function public.tambah_idempotency_key(text, text, text) from public, anon, authenticated;
revoke execute on function public.ubah_stok_atomic(uuid, uuid, numeric, text, uuid, text) from public, anon, authenticated;
revoke execute on function public.validate_qris_configuration(uuid) from public, anon, authenticated;

alter function public.hitung_total_penjualan(numeric, numeric, numeric) set search_path = '';

revoke execute on function public.checkout_sale_multi_payment(uuid, uuid, uuid, jsonb, jsonb, text) from public;
grant execute on function public.checkout_sale_multi_payment(uuid, uuid, uuid, jsonb, jsonb, text) to anon, authenticated;

drop index if exists public.uq_qris_config_branch;

alter default privileges in schema public revoke execute on functions from public;
alter default privileges in schema public revoke execute on functions from anon, authenticated;
