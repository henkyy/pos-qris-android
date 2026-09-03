-- Ensure all four canonical POS payment methods exist for every business.
-- This is idempotent because existing methods are preserved.
insert into public.payment_methods(id, business_id, code, name, method_type, is_active)
select gen_random_uuid(), b.id, x.code, x.name, x.method_type, true
from public.businesses b
cross join (values
  ('CASH', 'Tunai', 'CASH'),
  ('RECEIVABLE', 'Piutang', 'RECEIVABLE'),
  ('QRIS', 'QRIS', 'QRIS'),
  ('TRANSFER', 'Transfer Bank', 'BANK_TRANSFER')
) as x(code, name, method_type)
where not exists (
  select 1
  from public.payment_methods pm
  where pm.business_id = b.id
    and pm.code = x.code
);
