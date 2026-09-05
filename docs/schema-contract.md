# Posjay schema contract

The application and Supabase schema are treated as one contract. UI labels may differ from database field names, but every data field used by the application must exist in the database with the expected type and semantics.

## Master data

- `products` references `categories`, `brands`, and `units`.
- `units.is_active` controls whether a unit can be selected for new product, purchase, and sales operations.
- Historical transaction rows keep their unit reference even if the unit is later inactive.
- `locations.is_active`, `products.is_active`, `categories.is_active`, `brands.is_active`, `customers.is_active`, and `suppliers.is_active` follow the same active-master principle where supported by the UI.

## Transactions

- Sales are stored in `sales` and `sale_items`.
- Payment state belongs to `payments.status` and uses the `payment_status` enum. The sale itself keeps its document lifecycle in `sales.status`.
- Receivables are stored in `receivables` and `receivable_payments`.
- Purchases flow through `purchase_orders`, `goods_receipts`, `payables`, and their item/payment tables.
- Inventory changes are represented by `stock_movements` and materialized in `stock_balances`.

## Payment semantics

- Cash can complete offline and may create a cash movement.
- Receivable/credit is recorded against the customer ledger.
- QRIS remains pending until provider/server-side verification exists.
- Transfer can be recorded as pending until settlement is confirmed.

## Reset policy

A development reset may clear business operational data while retaining Supabase Auth identities. The reset must leave a usable workspace with an active business, branch, location, owner membership, default retail price list, base unit, and core payment methods.
