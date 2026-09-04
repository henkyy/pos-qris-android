# Workspace Regression Checklist

Scope: business → branch → stock location, reports, and offline POS.

## Required scenarios

1. Switch between accessible outlets and verify Sales, Purchase, Inventory, Dashboard, and Reports reload in the selected outlet scope.
2. Create an offline cash/receivable sale in outlet A, switch to outlet B, and verify the pending transaction is not counted or displayed as outlet B data.
3. Restore connectivity while outlet B is active and verify the queued sale is still posted using its stored outlet A scope.
4. Verify a failed sync leaves the pending sale in IndexedDB.
5. Verify successful sync removes the pending sale only after the checkout RPC succeeds.
6. Verify a branch without an active stock location still loads Dashboard and Reports without fabricating stock data.
7. Verify inventory/report stock queries use the selected location and do not depend on RLS as the primary application filter.
8. Verify completed sales, receivables, payables, payments, purchase orders, and goods receipts remain business + branch scoped.

## Data safety rule

Do not create test transactions in production merely to validate cross-outlet isolation. Use a dedicated test fixture or a non-production environment for two-outlet transactional regression.
