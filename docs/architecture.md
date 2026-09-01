# POS QRIS Android — Architecture

## Goal
Production-oriented Android POS for retail and distribution businesses, with server-authoritative transactions and QRIS payment verification.

## Principles
- Android is a client, not the authority for payment state.
- Never store provider secrets in the APK.
- Money is stored as integer minor units (IDR has no fractional rupiah in the business model).
- Every payment operation is idempotent.
- Inventory mutations are auditable and tied to business documents.
- QR generation and QR validation are isolated from payment settlement.

## High-level components

```text
Android POS
  ├─ Authentication / roles
  ├─ Product & barcode catalog
  ├─ Cart / checkout
  ├─ Cashier shift
  ├─ QRIS display
  └─ Receipt / local print integration
          │ HTTPS
          ▼
Backend / Supabase Edge Functions
  ├─ Orders
  ├─ Payments
  ├─ Provider adapter
  ├─ Webhook verification
  ├─ Inventory posting
  └─ Audit logging
          │
          ▼
PostgreSQL / Supabase

External payment provider
  └─ QRIS payment + status notification/inquiry
```

## Business domains
1. Master data: stores, users, roles, products, categories, units, customers, suppliers.
2. Procurement: purchase orders, receiving, supplier invoices, purchase returns.
3. Sales: quotations, sales orders, invoices, POS sales, sales returns.
4. Inventory: stock ledger, locations, transfers, adjustments, batches where required.
5. Finance: payments, receivables, payables, cash movements, shifts.
6. Reporting: sales, margin, stock, purchasing, receivables/payables, cashier reports.

## Payment state machine

```text
CREATED → PENDING → PAID
                 ├→ FAILED
                 ├→ EXPIRED
                 └→ CANCELLED
```

Only a trusted backend verification path may transition a QRIS payment to `PAID`.

## Retail + distributor requirements
- Multiple stores/branches.
- Multiple stock locations per store where needed.
- Barcode/SKU support.
- Multiple selling units and conversion factors.
- Purchase and sales pricing.
- Customer-specific pricing for distribution.
- Supplier/customer credit terms.
- Partial payments and outstanding balances.
- Stock transfers between locations.
- Stock adjustment with reason and audit trail.
- Cost/HPP tracking using a documented costing method.
- Returns linked to original documents when possible.
- Cashier shifts and cash reconciliation.

## Security boundaries
The Android app may contain public configuration only. Provider credentials, webhook secrets, privileged database keys, and signing secrets belong on the server/managed secret store.

## Implementation order
1. Database model and RLS.
2. Domain contracts and validation.
3. QRIS core package.
4. Payment provider adapter and verification boundary.
5. Backend APIs/functions.
6. Android shell/auth/navigation.
7. Product/catalog and inventory.
8. Checkout and payment UI.
9. Reporting and operational controls.
10. Automated tests and CI.
