-- Keep one open-ended active price for each product/unit/quantity tier.
create unique index if not exists product_prices_active_key_uq
  on public.product_prices (price_list_id, product_id, unit_id, min_qty)
  where valid_until is null;

-- Existing businesses must have a usable default price list so product setup and checkout
-- never silently operate with an empty price-list reference.
insert into public.price_lists (id, business_id, code, name, price_type, currency_code, is_default, is_active)
select gen_random_uuid(), b.id, 'DEFAULT', 'Harga Jual', 'SELLING', 'IDR', true, true
from public.businesses b
where b.is_active = true
  and not exists (
    select 1 from public.price_lists pl
    where pl.business_id = b.id and pl.is_default = true and pl.is_active = true
  );

create or replace function public.product_price_active(
  p_product_id uuid,
  p_price_list_id uuid,
  p_unit_id uuid,
  p_qty numeric default 1
)
returns table (
  id uuid,
  price numeric,
  discount_percent numeric,
  valid_from timestamptz,
  valid_until timestamptz
)
language sql
stable
security invoker
set search_path = public
as $$
  select pp.id, pp.price, pp.discount_percent, pp.valid_from, pp.valid_until
  from public.product_prices pp
  where pp.product_id = p_product_id
    and pp.price_list_id = p_price_list_id
    and pp.unit_id = p_unit_id
    and pp.min_qty <= coalesce(p_qty, 1)
    and (pp.valid_from is null or pp.valid_from <= now())
    and (pp.valid_until is null or pp.valid_until >= now())
  order by pp.min_qty desc, pp.valid_from desc nulls last, pp.id desc
  limit 1;
$$;

grant execute on function public.product_price_active(uuid, uuid, uuid, numeric) to authenticated;
