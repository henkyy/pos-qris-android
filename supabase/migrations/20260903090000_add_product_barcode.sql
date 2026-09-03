alter table public.products add column if not exists barcode text;
create unique index if not exists uq_products_business_barcode
  on public.products(business_id, barcode)
  where barcode is not null and barcode <> '';
