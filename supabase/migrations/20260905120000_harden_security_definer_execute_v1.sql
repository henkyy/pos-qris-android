-- Defense in depth: sensitive SECURITY DEFINER RPCs must never be callable anonymously.
revoke execute on function public.purchase_order_receive(uuid, uuid, text, date, date, text) from anon, public;
revoke execute on function public.validate_inventory_scope() from anon, public;
revoke execute on function public.validate_stock_balance_scope() from anon, public;
revoke execute on function public.bootstrap_first_business(text, text) from anon, public;
grant execute on function public.bootstrap_first_business(text, text) to authenticated;
