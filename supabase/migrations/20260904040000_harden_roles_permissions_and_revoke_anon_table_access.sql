-- Authorization catalog: roles are business-scoped, permissions are global,
-- role_permissions is scoped through the role's business.

grant select on table public.roles to authenticated;
grant select on table public.permissions to authenticated;
grant select on table public.role_permissions to authenticated;

alter table public.roles enable row level security;
alter table public.permissions enable row level security;
alter table public.role_permissions enable row level security;

drop policy if exists roles_member_read on public.roles;
create policy roles_member_read on public.roles
for select to authenticated
using (business_id in (select private.current_business_ids()));

drop policy if exists permissions_catalog_read on public.permissions;
create policy permissions_catalog_read on public.permissions
for select to authenticated
using (true);

drop policy if exists role_permissions_member_read on public.role_permissions;
create policy role_permissions_member_read on public.role_permissions
for select to authenticated
using (
  role_id in (
    select r.id from public.roles r
    where r.business_id in (select private.current_business_ids())
  )
);

-- Role administration is owner-only.
grant insert, update, delete on table public.roles to authenticated;
grant insert, update, delete on table public.role_permissions to authenticated;

drop policy if exists roles_owner_insert on public.roles;
create policy roles_owner_insert on public.roles
for insert to authenticated
with check (
  business_id in (select private.current_business_ids())
  and private.is_business_owner(business_id)
);

drop policy if exists roles_owner_update on public.roles;
create policy roles_owner_update on public.roles
for update to authenticated
using (
  business_id in (select private.current_business_ids())
  and private.is_business_owner(business_id)
)
with check (
  business_id in (select private.current_business_ids())
  and private.is_business_owner(business_id)
);

drop policy if exists roles_owner_delete on public.roles;
create policy roles_owner_delete on public.roles
for delete to authenticated
using (
  business_id in (select private.current_business_ids())
  and private.is_business_owner(business_id)
  and is_system = false
);

drop policy if exists role_permissions_owner_insert on public.role_permissions;
create policy role_permissions_owner_insert on public.role_permissions
for insert to authenticated
with check (
  role_id in (
    select r.id from public.roles r
    where r.business_id in (select private.current_business_ids())
  )
  and private.is_business_owner((select r.business_id from public.roles r where r.id = role_id))
);

drop policy if exists role_permissions_owner_delete on public.role_permissions;
create policy role_permissions_owner_delete on public.role_permissions
for delete to authenticated
using (
  role_id in (
    select r.id from public.roles r
    where r.business_id in (select private.current_business_ids())
  )
  and private.is_business_owner((select r.business_id from public.roles r where r.id = role_permissions.role_id))
);

-- The web app authenticates before business data loads. Anonymous clients must
-- not have blanket CRUD access to public business tables.
do $$
declare
  r record;
begin
  for r in
    select quote_ident(schemaname) as schema_name, quote_ident(tablename) as table_name
    from pg_tables
    where schemaname = 'public'
  loop
    execute format('revoke all on table %s.%s from anon', r.schema_name, r.table_name);
  end loop;
end $$;
