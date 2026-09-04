create or replace function public.bootstrap_first_business(p_business_name text, p_branch_name text default 'Cabang Utama')
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_business uuid;
  v_branch uuid;
  v_role uuid;
  v_code text;
  v_branch_code text := 'MAIN';
begin
  if v_user is null then raise exception 'Autentikasi diperlukan'; end if;
  if nullif(trim(p_business_name), '') is null then raise exception 'Nama bisnis wajib diisi'; end if;
  if exists (select 1 from public.business_users where user_id = v_user and is_active) then
    raise exception 'Pengguna sudah memiliki bisnis aktif';
  end if;

  v_role := (select id from public.roles where name = 'Owner / Super User' limit 1);
  if v_role is null then raise exception 'Role Owner / Super User belum tersedia'; end if;

  v_code := upper(regexp_replace(trim(p_business_name), '[^a-zA-Z0-9]+', '-', 'g'));
  v_code := left(trim(both '-' from v_code), 36);
  if v_code = '' then v_code := 'BUSINESS'; end if;
  v_code := v_code || '-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));

  insert into public.businesses(code,name,email)
  values(v_code, trim(p_business_name), (select email from auth.users where id=v_user))
  returning id into v_business;

  insert into public.branches(business_id,code,name)
  values(v_business,v_branch_code,coalesce(nullif(trim(p_branch_name), ''),'Cabang Utama'))
  returning id into v_branch;

  insert into public.profiles(id,full_name,is_active)
  values(v_user,coalesce((select raw_user_meta_data->>'full_name' from auth.users where id=v_user),''),true)
  on conflict (id) do update set is_active=true, updated_at=now();

  insert into public.business_users(business_id,user_id,role_id,is_active)
  values(v_business,v_user,v_role,true);

  insert into public.user_branch_access(user_id,branch_id)
  values(v_user,v_branch);

  return v_business;
end;
$$;

revoke all on function public.bootstrap_first_business(text,text) from public, anon;
grant execute on function public.bootstrap_first_business(text,text) to authenticated, service_role;
