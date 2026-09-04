grant select on table public.price_lists to authenticated;
grant select on table public.product_prices to authenticated;

alter table public.price_lists enable row level security;
alter table public.product_prices enable row level security;

drop policy if exists price_lists_member_read on public.price_lists;
create policy price_lists_member_read on public.price_lists
for select to authenticated
using (business_id in (select private.current_business_ids()) and is_active = true);

drop policy if exists product_prices_member_read on public.product_prices;
create policy product_prices_member_read on public.product_prices
for select to authenticated
using (
  price_list_id in (
    select pl.id from public.price_lists pl
    where pl.business_id in (select private.current_business_ids())
      and pl.is_active = true
  )
);
