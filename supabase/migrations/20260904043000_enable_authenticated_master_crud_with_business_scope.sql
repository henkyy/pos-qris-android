-- Master data may be maintained by authenticated business members.
-- Historical transaction tables remain immutable and are not granted generic CRUD.

do $$
begin
  drop policy if exists public_product_access on public.products;
  drop policy if exists public_customer_access on public.customers;
  drop policy if exists public_supplier_access on public.suppliers;
  drop policy if exists public_category_access on public.categories;
  drop policy if exists public_unit_access on public.units;

  grant select, insert, update, delete on table public.products to authenticated;
  drop policy if exists products_member_insert on public.products;
  drop policy if exists products_member_update on public.products;
  drop policy if exists products_member_delete on public.products;
  create policy products_member_insert on public.products for insert to authenticated with check (business_id in (select private.current_business_ids()));
  create policy products_member_update on public.products for update to authenticated using (business_id in (select private.current_business_ids())) with check (business_id in (select private.current_business_ids()));
  create policy products_member_delete on public.products for delete to authenticated using (business_id in (select private.current_business_ids()));

  grant select, insert, update, delete on table public.customers to authenticated;
  drop policy if exists customers_member_insert on public.customers;
  drop policy if exists customers_member_update on public.customers;
  drop policy if exists customers_member_delete on public.customers;
  create policy customers_member_insert on public.customers for insert to authenticated with check (business_id in (select private.current_business_ids()));
  create policy customers_member_update on public.customers for update to authenticated using (business_id in (select private.current_business_ids())) with check (business_id in (select private.current_business_ids()));
  create policy customers_member_delete on public.customers for delete to authenticated using (business_id in (select private.current_business_ids()));

  grant select, insert, update, delete on table public.suppliers to authenticated;
  drop policy if exists suppliers_member_insert on public.suppliers;
  drop policy if exists suppliers_member_update on public.suppliers;
  drop policy if exists suppliers_member_delete on public.suppliers;
  create policy suppliers_member_insert on public.suppliers for insert to authenticated with check (business_id in (select private.current_business_ids()));
  create policy suppliers_member_update on public.suppliers for update to authenticated using (business_id in (select private.current_business_ids())) with check (business_id in (select private.current_business_ids()));
  create policy suppliers_member_delete on public.suppliers for delete to authenticated using (business_id in (select private.current_business_ids()));

  grant select, insert, update, delete on table public.categories to authenticated;
  drop policy if exists categories_member_insert on public.categories;
  drop policy if exists categories_member_update on public.categories;
  drop policy if exists categories_member_delete on public.categories;
  create policy categories_member_insert on public.categories for insert to authenticated with check (business_id in (select private.current_business_ids()));
  create policy categories_member_update on public.categories for update to authenticated using (business_id in (select private.current_business_ids())) with check (business_id in (select private.current_business_ids()));
  create policy categories_member_delete on public.categories for delete to authenticated using (business_id in (select private.current_business_ids()));

  grant select, insert, update, delete on table public.units to authenticated;
  drop policy if exists units_member_insert on public.units;
  drop policy if exists units_member_update on public.units;
  drop policy if exists units_member_delete on public.units;
  create policy units_member_insert on public.units for insert to authenticated with check (business_id in (select private.current_business_ids()));
  create policy units_member_update on public.units for update to authenticated using (business_id in (select private.current_business_ids())) with check (business_id in (select private.current_business_ids()));
  create policy units_member_delete on public.units for delete to authenticated using (business_id in (select private.current_business_ids()));
end $$;
