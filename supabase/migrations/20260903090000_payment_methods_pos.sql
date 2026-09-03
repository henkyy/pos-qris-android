-- POS payment methods used by the four supported checkout paths.
-- CASH and RECEIVABLE are usable offline at the client boundary.
-- QRIS remains pending until a trusted provider verification path settles it.
-- TRANSFER is recorded pending until reconciliation/verification.

insert into public.payment_methods(id,business_id,code,name,method_type,is_active)
select gen_random_uuid(), b.id, 'RECEIVABLE', 'Piutang', 'RECEIVABLE', true
from public.businesses b
where not exists (
  select 1 from public.payment_methods pm
  where pm.business_id=b.id and pm.code='RECEIVABLE'
);

-- Keep the existing seeded methods intact. This migration only adds the missing POS method.
