-- Posjay schema contract: unit availability is part of the application contract.
-- New sales/purchase/product flows may select only active units, while historical rows
-- continue to reference units that may later be deactivated.

alter table public.units
  add column if not exists is_active boolean not null default true;

create index if not exists units_business_active_name_idx
  on public.units (business_id, is_active, name);

comment on column public.units.is_active is
  'Controls whether the unit can be selected for new product, purchase, and sales operations. Historical references remain valid.';
