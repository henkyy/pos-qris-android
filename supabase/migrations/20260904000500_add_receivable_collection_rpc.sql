-- Register customer receivable collections with partial/full settlement.
create or replace function public.receivable_register_payment(
  p_receivable_id uuid,
  p_amount bigint,
  p_payment_id uuid default null
)
returns table(receivable_id uuid, paid_amount bigint, outstanding_amount bigint, status text)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_receivable public.receivables%rowtype;
  v_new_paid bigint;
  v_new_outstanding bigint;
  v_status text;
begin
  select * into v_receivable
  from public.receivables r
  where r.id=p_receivable_id
  for update;
  if not found then raise exception 'Piutang tidak ditemukan'; end if;
  if not exists (select 1 from private.current_business_ids() x where x=v_receivable.business_id) then raise exception 'Akses bisnis ditolak'; end if;
  if p_amount is null or p_amount<=0 then raise exception 'Nominal pembayaran harus lebih dari 0'; end if;
  if p_amount>v_receivable.outstanding_amount then raise exception 'Pembayaran melebihi saldo piutang'; end if;
  if p_payment_id is not null and not exists(select 1 from public.payments p where p.id=p_payment_id and p.business_id=v_receivable.business_id and p.status='PAID') then raise exception 'Pembayaran tidak valid'; end if;
  v_new_paid:=v_receivable.paid_amount+p_amount;
  v_new_outstanding:=v_receivable.original_amount-v_new_paid;
  v_status:=case when v_new_outstanding=0 then 'PAID' else 'OPEN' end;
  insert into public.receivable_payments(id,receivable_id,payment_id,amount,paid_at) values(gen_random_uuid(),p_receivable_id,p_payment_id,p_amount,now());
  update public.receivables set paid_amount=v_new_paid,outstanding_amount=v_new_outstanding,status=v_status where id=p_receivable_id;
  return query select p_receivable_id,v_new_paid,v_new_outstanding,v_status;
end;
$$;
revoke execute on function public.receivable_register_payment(uuid,bigint,uuid) from public,anon;
grant execute on function public.receivable_register_payment(uuid,bigint,uuid) to authenticated,service_role;
