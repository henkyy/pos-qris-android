do $$
declare
  fn text;
  replaced text;
begin
  select pg_get_functiondef(p.oid) into fn
  from pg_proc p
  join pg_namespace n on n.oid=p.pronamespace
  where n.nspname='public'
    and p.proname='checkout_sale_multi_payment'
    and pg_get_function_identity_arguments(p.oid)='p_branch_id uuid, p_location_id uuid, p_customer_id uuid, p_items jsonb, p_payments jsonb, p_idempotency_key text';
  if fn is null then raise exception 'checkout_sale_multi_payment tidak ditemukan'; end if;
  replaced := regexp_replace(fn, E'if v_qris_confirmed then\\s+raise exception ''Konfirmasi QRIS harus berasal dari proses verifikasi pembayaran'';\\s+end if;', 'v_qris_confirmed:=false;', 1, 0, 'n');
  if replaced = fn then raise exception 'Pola konfirmasi QRIS tidak ditemukan'; end if;
  execute replaced;
end $$;
