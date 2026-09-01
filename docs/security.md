# Security Baseline

## Secrets
Never commit provider API keys, webhook secrets, Supabase service-role keys, signing keys, passwords, or merchant credentials. Android builds contain public configuration only.

## Authorization
Use least-privilege roles. Privileged operations such as voids, stock adjustments, price overrides, refunds, and manual reconciliation require explicit permissions and audit records.

## Payments
- Server computes the payable amount.
- Server owns payment state transitions.
- Provider callbacks must be authenticated according to provider documentation.
- Payment events must be idempotent.
- Client confirmation is never sufficient evidence of payment.

## Database
Row Level Security should be enabled for exposed application tables. Backend privileged operations should use narrowly scoped server-side credentials and must not be embedded in the Android app.

## Inventory
Stock is derived from an append-only or controlled stock movement ledger. Direct client-side balance updates are prohibited.

## Logs
Do not log full payment credentials, secrets, authentication tokens, or unnecessary personal data. Operational logs should use internal IDs where possible.

## Release
CI should run static checks and tests. Release signing keys remain outside the repository and should be injected by the build/release system.
