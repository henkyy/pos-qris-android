create schema if not exists private;

alter function public.checkout_sale_multi_payment(uuid, uuid, uuid, jsonb, jsonb, text) set schema private;

create or replace function public.checkout_sale_multi_payment(
  p_branch_id uuid,
  p_location_id uuid,
  p_customer_id uuid,
  p_items jsonb,
  p_payments jsonb,
  p_idempotency_key text default null
)
returns table(
  sale_id uuid,
  sale_no text,
  total_amount bigint,
  paid_amount bigint,
  change_amount bigint,
  sale_status text
)
language sql
security invoker
set search_path = ''
as $$
  select * from private.checkout_sale_multi_payment(
    p_branch_id,
    p_location_id,
    p_customer_id,
    p_items,
    p_payments,
    p_idempotency_key
  );
$$;

revoke execute on function private.checkout_sale_multi_payment(uuid, uuid, uuid, jsonb, jsonb, text) from public, anon, authenticated;
grant execute on function public.checkout_sale_multi_payment(uuid, uuid, uuid, jsonb, jsonb, text) to anon, authenticated, service_role;
