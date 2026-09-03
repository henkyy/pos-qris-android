-- Recreate purchase_order_confirm because PostgreSQL does not allow changing OUT parameter names/types with CREATE OR REPLACE.
drop function if exists public.purchase_order_confirm(uuid);

create function public.purchase_order_confirm(p_purchase_order_id uuid)
returns table(purchase_order_id uuid,status text)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_business uuid;
  v_status text;
begin
  select po.business_id,po.status::text into v_business,v_status
  from public.purchase_orders po
  where po.id=p_purchase_order_id
  for update;
  if v_business is null then raise exception 'Purchase order tidak ditemukan.'; end if;
  if not exists (select 1 from public.business_users bu where bu.business_id=v_business and bu.user_id=auth.uid() and bu.is_active) then raise exception 'Akses bisnis ditolak.'; end if;
  if v_status <> 'DRAFT' then raise exception 'Hanya PO DRAFT yang dapat dikonfirmasi.'; end if;
  update public.purchase_orders po set status='CONFIRMED'::public.document_status,updated_at=now() where po.id=p_purchase_order_id;
  return query select p_purchase_order_id,'CONFIRMED'::text;
end;
$$;

grant execute on function public.purchase_order_confirm(uuid) to authenticated,service_role;
