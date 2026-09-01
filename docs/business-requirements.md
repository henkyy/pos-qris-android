# Business Requirements — Retail & Distribution POS

## Roles
- Owner/admin: configuration, pricing, reports, financial visibility.
- Manager: inventory, purchasing, sales oversight, approvals.
- Cashier: checkout, payments, shift operations, receipts.
- Warehouse: receiving, picking, transfers, adjustments.
- Sales: customer management, quotations, orders, receivables.

## Core master data
- Business and branches.
- Users, roles, permissions.
- Products, SKU, barcode, brand, category.
- Units of measure and conversion factors.
- Selling and purchasing price lists.
- Customers and customer groups.
- Suppliers.
- Tax/charge configuration.
- Payment methods.

## Sales
- POS walk-in sales.
- Customer account sales.
- Quotations and sales orders.
- Invoices.
- Discounts with approval limits.
- Customer-specific pricing.
- Returns and refunds according to payment method.
- Cash, QRIS and other configured payment methods.

## Purchasing
- Purchase orders.
- Goods receiving.
- Supplier invoices.
- Purchase returns.
- Supplier payable tracking.

## Inventory
- Stock by branch/location.
- Stock ledger rather than editable balance-only records.
- Transfers.
- Receiving and dispatch.
- Adjustments with reason and authorization.
- Negative stock policy configurable, default disabled.
- Optional batch/expiry tracking for applicable products.
- Unit conversions posted explicitly.

## Distribution
- Customer groups and tiers.
- Wholesale price lists.
- Minimum order quantities.
- Credit limits and payment terms.
- Partial payments and receivables.
- Delivery/order status.
- Salesperson assignment.

## Cashier operations
- Open shift with opening cash.
- Cash in/out with reason.
- Sales by payment method.
- Reprint receipts subject to permission.
- Close shift and reconcile expected vs actual cash.
- Cannot delete completed sales; use void/return workflows with authorization.

## Reporting
- Daily sales.
- Sales by product/category/customer/cashier.
- Gross margin/HPP report.
- Stock on hand and stock movement.
- Low-stock report.
- Purchase report.
- Receivables and payables aging.
- Cashier shift reconciliation.
- Payment reconciliation and exceptions.

## Auditability
Every material mutation must record actor, timestamp, branch, document, before/after where appropriate, and reason for privileged actions.
