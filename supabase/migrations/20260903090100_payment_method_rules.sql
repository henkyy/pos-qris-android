-- Payment rules for the four supported POS methods.
-- Cash settles immediately. Receivable and Transfer remain open/pending.
-- QRIS is pending until trusted provider verification settles the payment.

create or replace function public.validate_pos_payment_method(
  p_business_id uuid,
  p_branch_id uuid,
  p_method_id uuid,
  p_amount bigint,
  p_total bigint,
  p_cash_received bigint default null,
  p_reference text default null
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_method record;
  v_qris_configuration_id uuid;
begin
  select * into v_method
  from public.payment_methods pm
  where pm.id = p_method_id
    and pm.business_id = p_business_id
    and pm.is_active = true;

  if not found then raise exception 'Metode pembayaran tidak valid'; end if;
  if p_amount is null or p_amount <= 0 then raise exception 'Nominal pembayaran harus lebih dari 0'; end if;
  if p_amount <> p_total then raise exception 'Nominal pembayaran harus sama dengan total transaksi'; end if;

  if upper(v_method.method_type) = 'CASH' or v_method.code = 'CASH' then
    if coalesce(p_cash_received, 0) < p_amount then raise exception 'Uang tunai kurang dari total pembayaran'; end if;
    return 'PAID';
  end if;

  if upper(v_method.method_type) in ('RECEIVABLE','PIUTANG') or v_method.code in ('RECEIVABLE','PIUTANG','AR') then
    return 'PENDING';
  end if;

  if upper(v_method.method_type) in ('BANK_TRANSFER','TRANSFER') or v_method.code = 'TRANSFER' then
    if nullif(trim(p_reference), '') is null then raise exception 'Nomor referensi transfer wajib diisi'; end if;
    return 'PENDING';
  end if;

  if upper(v_method.method_type) = 'QRIS' or v_method.code = 'QRIS' then
    select qc.id into v_qris_configuration_id
    from public.qris_configurations qc
    where qc.business_id = p_business_id
      and qc.branch_id = p_branch_id
      and qc.is_active = true
    limit 1;
    if v_qris_configuration_id is null then raise exception 'QRIS belum dikonfigurasi untuk cabang ini'; end if;
    return 'PENDING';
  end if;

  raise exception 'Metode pembayaran belum didukung: %', v_method.code;
end;
$$;

revoke execute on function public.validate_pos_payment_method(uuid,uuid,uuid,bigint,bigint,bigint,text) from public, anon, authenticated;
grant execute on function public.validate_pos_payment_method(uuid,uuid,uuid,bigint,bigint,bigint,text) to anon, authenticated, service_role;
