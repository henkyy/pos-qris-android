insert into public.product_units (id, product_id, unit_id, conversion_to_base, is_purchase_unit, is_sales_unit)
select gen_random_uuid(), p.id, p.base_unit_id, 1, true, true
from public.products p
where p.is_active = true
  and p.base_unit_id is not null
  and not exists (
    select 1 from public.product_units pu
    where pu.product_id = p.id and pu.unit_id = p.base_unit_id
  );
