alter policy branch_access_self_read on public.user_branch_access
using (
  (user_id = (select auth.uid()))
  or exists (
    select 1
    from public.business_users bu
    join public.roles r on r.id = bu.role_id
    where bu.business_id = (
      select b.business_id from public.branches b where b.id = user_branch_access.branch_id
    )
      and bu.user_id = (select auth.uid())
      and bu.is_active = true
      and r.code = 'OWNER'
  )
);
