# QRIS ERP/POS Master Blueprint

> Living specification untuk pengembangan QRIS ERP/POS. Legacy POS reference menjadi referensi functional requirement dan workflow, bukan source code yang di-porting mentah.

## 1. Prinsip Utama

- QRIS adalah web SaaS ERP/POS berbasis Next.js + Supabase + Vercel.
- Business, branch, dan location menjadi scope data sesuai access model.
- Master data tidak boleh mengubah histori transaksi.
- Completed transaction tidak di-hard-delete.
- Stock berubah melalui stock movement, bukan edit angka bebas.
- HPP transaksi disimpan sebagai snapshot.
- Harga historis harus dipertahankan melalui versioning/end-date.
- Offline/network state berbeda dari receivable/credit.
- Write lintas tabel yang kritis harus atomic melalui database transaction/RPC.
- Operator harus dapat membuat master yang diperlukan secara inline tanpa kehilangan draft transaksi.
- UI harus menjelaskan konsekuensi aksi sensitif.

## 2. Referensi Legacy POS

Audit source legacy POS menemukan modul/fungsi utama: POS/Kasir, Held Sales, Sales, Void, Purchases, Products, Stock, Stock Adjustment, Cash Sessions, Cash Movements, Expenses, Receivables, Customer Payments, Reports, Users/Roles, Audit Logs, Settings, Receipt/Print.

Referensi ini digunakan untuk menangkap functional requirement dan workflow operator. Implementasi QRIS tetap mengikuti arsitektur multi-tenant dan schema QRIS.

## 3. Mapping Legacy POS → QRIS

| Legacy POS | QRIS | Arah |
|---|---|---|
| POS/Kasir | Sales Terminal | Sudah ada, refinement |
| Held Sales | Sales Terminal | P1 |
| Sales history | Sales | Sudah ada |
| Void sale | Sales lifecycle | Audit/refinement |
| Sales return | Returns | P1 |
| Purchases | Purchase Workspace | Sudah ada |
| Purchase receive | Goods Receipt | Sudah ada |
| Purchase return | Purchase Return | P1 |
| Products | Product Catalog | Sudah ada |
| Product units | Product Catalog | Sudah ada |
| Categories | Master Data | Sudah ada |
| Units | Master Data | Sudah ada |
| Suppliers | Suppliers | Sudah ada |
| Customers | Customers | Sudah ada |
| Stock | Inventory Workspace | Sudah ada |
| Stock adjustment | Inventory | Sudah ada |
| Stock opname | Inventory | P1 |
| Stock transfer | Inventory | P1 |
| Cash session | Cashier Shift | Struktur sudah ada |
| Cash movement | Cash | Struktur sudah ada |
| Expenses | Expenses | Audit workflow |
| Customer payment | Receivables | Struktur sudah ada |
| Supplier payment | Payables | Struktur sudah ada |
| Sales/Purchase/Product/Stock reports | Report Center | Sudah ada/refinement |
| Audit logs | Audit | Sudah ada |
| Backup | Data management | Jangan porting tanpa audit |

## 4. Arsitektur yang Dipertahankan

Legacy POS berorientasi single-store. QRIS harus tetap multi-tenant:

```text
business
├── branches
│   └── locations
├── products
├── customers
├── suppliers
├── sales
├── purchases
└── financial records

business_users
user_branch_access
roles
permissions
```

Jangan menyalin schema legacy 1:1.

## 5. Master Data

### Products

Mendukung SKU, barcode, nama, kategori, base unit, multi-unit, conversion to base, purchase/sales unit, current HPP, last purchase cost, cost method, minimum stock, reorder point, active/inactive.

Edit master tidak boleh mengubah nama/unit/harga/HPP snapshot transaksi lama.

### Product Units

Konsep multi-unit: `1 Dus = 24 PCS`. QRIS memakai `product_units`, `conversion_to_base`, `is_purchase_unit`, `is_sales_unit`. Jangan menghapus histori unit yang sudah direferensikan transaksi.

### Prices

QRIS menggunakan `price_lists`, `product_prices`, `valid_from`, `valid_until`, `min_qty`. Perubahan harga harus membuat versi baru/end-date, bukan menimpa histori yang telah dipakai.

### Customers/Suppliers

Customer mendukung penjualan tunai/kredit, histori, dan pembayaran piutang. Supplier mendukung PO, goods receipt, payable, dan supplier payment.

Inline creation wajib mempertahankan draft transaksi dan langsung memilih record baru setelah berhasil.

## 6. Sales

Workflow:

```text
Draft Cart → Customer optional → Product → Pricing → Discount/Tax → Payment → COMPLETED
```

Payment: CASH, QRIS, TRANSFER, RECEIVABLE.

RECEIVABLE berarti kewajiban finansial, bukan offline.

## 7. Held Sales

Referensi legacy POS memiliki konsep held sales.

Target QRIS:

```text
Cart → Hold → Saved transaction → Resume → Checkout
```

Held sale tidak mengurangi stok sebelum checkout. Draft yang sedang berjalan tidak boleh hilang.

## 8. Void / Return

Completed sale bukan CRUD delete. Gunakan VOID/RETURN/REFUND dan reversal yang memperhitungkan stock, cash, payment, receivable, HPP, margin, laporan, dan audit.

## 9. Purchases

Workflow:

```text
Draft PO → Confirm → Goods Receipt → Stock increases → HPP update → Payable → Payment
```

PO adalah komitmen pembelian. Goods Receipt berarti barang benar-benar diterima. Stock naik pada receipt, bukan saat PO dibuat.

## 10. HPP / Moving Average

Untuk Moving Average:

```text
new average cost =
(old stock qty × old average cost + received qty × landed unit cost)
/ new stock qty
```

Sale menyimpan `hpp_unit` dan `hpp_total` sebagai snapshot.

Landed-cost allocation untuk discount/transport/biaya langsung masih harus diverifikasi sebelum dianggap lengkap.

## 11. Inventory

Model:

```text
Stock Movement → Stock Balance → Available Stock
```

Movement dapat berasal dari opening stock, purchase receipt, sale, adjustment, damage, return, transfer, dan stock opname.

Setiap perubahan harus dapat ditelusuri ke movement type, reference, waktu, user, lokasi, quantity, conversion, dan reason bila adjustment.

## 12. Stock Opname & Transfer

Stock opname:

```text
System Stock → Physical Count → Variance → Adjustment Movement → Updated Balance
```

Transfer:

```text
Location A → Transfer → Location B
```

Harus ada movement keluar dan masuk yang saling berkorelasi.

## 13. Receivables & Payables

Receivable QRIS: `receivables`, `receivable_payments`, `payments` dengan original_amount, paid_amount, outstanding_amount, due_date, status.

Lifecycle: FORMED → OUTSTANDING/DUE → PARTIAL → PAID.

Payable mengikuti prinsip yang sama. Pembayaran tidak menghapus histori kewajiban.

## 14. Payment

QRIS memiliki payments, payment_events, payment_methods, idempotency, external transaction reference, QR reference, verification state, reconciliation state.

Payment verified diperlakukan immutable. Koreksi melalui reversal/reconciliation, bukan edit sembarangan.

## 15. Cashier Shift & Expense

Workflow shift:

```text
Open Shift → Opening Cash → Cash Sales → Expenses → Deposit/Withdrawal → Expected Cash → Actual Cash → Variance → Close
```

Expense harus jelas hubungan tanggal, kategori, nominal, user, cash movement, dan shift bila relevan.

Net profit:

```text
Sales - HPP = Gross Profit
Gross Profit - Operating Expense = Net Profit
```

## 16. Report Center

Target menu:

```text
Reports
├── Sales
├── Purchases
├── Products
├── Inventory
├── HPP & Margin
├── Receivables
├── Payables
├── Expenses
├── Cash / Shift
└── Audit
```

Purchase report harus membedakan PO, Goods Receipt, dan Payable serta menunjukkan korelasi bila relevan.

## 17. Audit

Aksi sensitif yang perlu diaudit: void, adjustment, payment correction, master activation/deactivation, permission changes, reset, dan sensitive settings.

Minimal: actor, action, module, reference, timestamp, description; before/after bila diperlukan.

## 18. Reset / Demo Data

QRIS memiliki safe reset dengan dua mode.

**Transactions:** sales, sale items, payments/events, receivables/payments, payables/payments, purchase orders, goods receipts, stock movements/balances, returns, adjustments, transfers, cashier shifts/cash movements dan data transaksi terkait.

**Full:** transaction reset + master demo data seperti products, product units/prices, customers, categories, brands, price lists, payment methods, QRIS configurations, locations, units, suppliers, dan audit logs.

Tetap pertahankan business, branch, account/login, membership, roles, permissions. Reset dibatasi user berotorisasi dan membutuhkan konfirmasi eksplisit.

## 19. UI/UX Principles

### Operator-first

Operator tidak seharusnya meninggalkan transaksi hanya untuk membuat customer/supplier yang belum ada.

### Inline creation

```text
Operational form → + Master Baru → Save → langsung selected
```

Draft, cart, filter, discount, dan state penting harus tetap utuh.

### Empty state actionable

Empty state harus menawarkan aksi yang relevan, bukan hanya pesan kosong.

### Consequence-aware

Aksi sensitif harus menjelaskan dampaknya terhadap stock, payment, receivable/payable, histori, dan laporan.

### States

Workflow penting wajib memiliki loading, validation error, database/permission error, success, retry/recovery, dan responsive/mobile behavior.

## 20. Information Architecture Target

```text
Dashboard
Sales
├── Sales Terminal
├── Held Sales
└── Sales History
Purchases
├── Purchase Orders
├── Goods Receipts
├── Purchase Returns
└── Suppliers
Inventory
├── Stock Overview
├── Stock Movements
├── Stock Adjustment
├── Stock Opname
└── Stock Transfer
Customers
├── Customers
└── Receivables
Products
├── Product Catalog
├── Categories
├── Units
└── Price Lists
Finance
├── Receivables
├── Payables
├── Payments
├── Cash / Shift
└── Expenses
Reports
├── Sales
├── Purchases
├── Inventory
├── HPP & Margin
├── Receivables
├── Payables
├── Expenses
└── Cash
Settings
├── Business
├── Branch
├── Payment
├── Users & Roles
├── Audit
└── Data Reset
```

Menu final dapat berubah mengikuti kebutuhan nyata, tetapi pengelompokan harus tetap mudah dipahami operator.

## 21. Database / RPC Rules

Operasi lintas tabel yang kritis harus atomic, khususnya checkout sale, purchase receive, receivable payment, payable payment, stock adjustment, stock transfer, dan void/return.

Security boundary tidak boleh hanya berupa filter frontend. RLS dan business/branch membership tetap wajib.

## 22. Current QRIS Strengths

- Next.js/Supabase/Vercel
- business/branch access model
- RLS
- transaction-oriented schema
- stock movements/balances
- product units
- price lists/product prices
- transaction snapshots
- Moving Average HPP
- receivables/payables
- payment events
- cashier shifts/cash movements
- Report Center
- safe reset RPC

## 23. Priority Backlog

### P0

- Audit atomicity semua transaction RPC.
- Audit RLS workflow.
- Verifikasi purchase receive → stock + HPP + payable.
- Verifikasi checkout sale → payment + stock + HPP + receivable.
- Pastikan master edit tidak mengubah histori.
- Hilangkan hardcoded business/branch selection.
- Pastikan UI tidak stale setelah reset.

### P1

- Held Sales.
- Sales Return/Refund.
- Purchase Return.
- Stock Opname.
- Stock Transfer.
- Supplier Payment UX.
- Customer Payment UX.
- Expense workflow.
- Cash/Shift workflow.
- HPP & Margin detail.
- Price history UI.
- Inline master creation pada workflow operasional.

### P2

- Multi-business selector.
- Multi-branch selector.
- Import/export.
- Advanced reporting.
- Advanced audit/reconciliation.
- Additional ERP workflows.

## 24. Definition of Done

Fitur selesai jika UI, data, lifecycle transaksi, RLS, permission, error handling, loading/empty states, responsive behavior, histori, related tables, report terkait, audit bila perlu, dan production build semuanya benar.

Tidak boleh ada hardcoded business/branch. Build Vercel harus berhasil dan behavior production harus diverifikasi.

## 25. Development Protocol

```text
/debug
→ Understand existing implementation
→ Check DB/schema/RLS/RPC
→ Identify side effects
→ Design UX flow
→ Implement
→ Build
→ Deploy
→ Verify production
```

Jangan menambah tabel/field hanya karena UI membutuhkannya sebelum schema existing diperiksa. Jangan hard-delete histori demi kebutuhan master CRUD.

## 26. Source of Truth

Prioritas ketika terjadi konflik:

1. Integritas data dan lifecycle transaksi.
2. Security/RLS.
3. Business requirement.
4. Existing QRIS schema.
5. Functional requirement legacy POS.
6. UI convention.
7. Cosmetic preference.

Dokumen ini wajib diperbarui ketika keputusan arsitektur, workflow, atau UI/UX penting berubah.
