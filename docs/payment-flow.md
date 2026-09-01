# Payment Flow

## QRIS checkout
1. Cashier selects items and confirms the order.
2. Backend validates prices, discounts, tax/charges, stock rules, and total.
3. Backend creates an immutable order snapshot and a payment transaction with a unique idempotency key.
4. QRIS core generates a dynamic QR payload from the merchant payload and validated amount.
5. Android displays the QR and polls the backend for the payment state.
6. Provider callback or server-side status inquiry is authenticated and normalized by the provider adapter.
7. Backend applies an idempotent payment event.
8. Only a verified successful provider result changes the payment and order to `PAID`.
9. Inventory and financial postings are committed exactly once.
10. Android receives the authoritative status and enables receipt printing.

## Never trust the client
The client must not be able to set `PAID`, change a settled amount, submit a provider credential, or finalize stock movements by itself.

## Amount integrity
The payment amount is derived from the server-side order total. The Android client may display it, but must not be the authoritative source for settlement.

## Idempotency
Provider event identifiers and internal idempotency keys must be unique. Replayed callbacks must produce the same final state without duplicate payment, stock, or ledger postings.

## Expiry and cancellation
A pending QRIS payment may expire or be cancelled according to provider capabilities and business rules. A late success must be reconciled explicitly rather than silently attaching money to a different order.

## Reconciliation
The backend must support periodic reconciliation against provider status where the provider exposes inquiry APIs. Exceptions must be visible to an operator.
