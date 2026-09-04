import type { EntityConfig } from './EntityPage'

export const entityConfigs: Record<string, EntityConfig> = {
  products: {
    title: 'Produk', eyebrow: 'MASTER · PRODUK', description: 'Katalog, SKU, barcode, harga, kategori dan gambar produk.', table: 'products',
    columns: ['name', 'sku', 'barcode', 'is_active'], columnLabels: { name: 'Nama Produk', sku: 'SKU', barcode: 'Barcode', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    fields: [
      { key: 'name', label: 'Nama produk', required: true },
      { key: 'sku', label: 'SKU', required: true },
      { key: 'barcode', label: 'Barcode' },
      { key: 'base_unit_id', label: 'Satuan dasar', required: true, type: 'select', source: 'units' },
      { key: 'category_id', label: 'Kategori', type: 'select', source: 'categories' },
    ],
  },
  customers: {
    title: 'Pelanggan', eyebrow: 'MASTER · PELANGGAN', description: 'Data pelanggan, kontak, limit kredit dan saldo piutang.', table: 'customers',
    columns: ['name', 'phone', 'email', 'credit_limit', 'is_active'], columnLabels: { name: 'Nama Pelanggan', phone: 'Telepon', email: 'Email', credit_limit: 'Limit Kredit', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    fields: [
      { key: 'name', label: 'Nama pelanggan', required: true },
      { key: 'code', label: 'Kode pelanggan', required: true },
      { key: 'phone', label: 'Telepon' }, { key: 'email', label: 'Email' }, { key: 'credit_limit', label: 'Limit kredit', type: 'number' },
    ],
  },
  suppliers: {
    title: 'Supplier', eyebrow: 'MASTER · SUPPLIER', description: 'Data pemasok dan riwayat hubungan pembelian.', table: 'suppliers',
    columns: ['name', 'code', 'phone', 'email', 'is_active'], columnLabels: { name: 'Nama Supplier', code: 'Kode', phone: 'Telepon', email: 'Email', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    fields: [
      { key: 'name', label: 'Nama supplier', required: true }, { key: 'code', label: 'Kode supplier', required: true },
      { key: 'phone', label: 'Telepon' }, { key: 'email', label: 'Email' }, { key: 'payment_term_days', label: 'Tempo pembayaran (hari)', type: 'number' },
    ],
  },
  categories: {
    title: 'Kategori', eyebrow: 'MASTER · PRODUK', description: 'Pengelompokan katalog untuk kasir dan laporan.', table: 'categories',
    columns: ['name', 'code', 'is_active'], columnLabels: { name: 'Nama Kategori', code: 'Kode', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    fields: [{ key: 'name', label: 'Nama kategori', required: true }, { key: 'code', label: 'Kode kategori', required: true }],
  },
  units: {
    title: 'Satuan', eyebrow: 'MASTER · PRODUK', description: 'Satuan dasar dan satuan penjualan produk.', table: 'units',
    columns: ['name', 'code', 'symbol', 'decimal_places'], columnLabels: { name: 'Nama Satuan', code: 'Kode', symbol: 'Simbol', decimal_places: 'Desimal' },
    businessScoped: true,
    fields: [{ key: 'name', label: 'Nama satuan', required: true }, { key: 'code', label: 'Kode satuan', required: true }, { key: 'symbol', label: 'Simbol' }, { key: 'decimal_places', label: 'Jumlah desimal', type: 'number' }],
  },
  stock: { title: 'Stok', eyebrow: 'MASTER · PERSEDIAAN', description: 'Posisi stok per lokasi untuk membantu pemantauan persediaan dan keputusan replenishment.', table: 'stock_balances', columns: ['product_id', 'location_id', 'qty_base'], columnLabels: { product_id: 'Produk', location_id: 'Lokasi', qty_base: 'Stok' }, readOnly: true },
  orders: { title: 'Pesanan', eyebrow: 'TRANSAKSI · PESANAN', description: 'Pusat pesanan berjalan, held order dan status fulfillment.', table: 'sales', columns: ['sale_no', 'customer_id', 'total_amount', 'status', 'sale_date'], columnLabels: { sale_no: 'No. Pesanan', customer_id: 'Pelanggan', total_amount: 'Total', status: 'Status', sale_date: 'Tanggal' }, businessScoped: true, readOnly: true },
  purchases: { title: 'Pembelian', eyebrow: 'TRANSAKSI · PEMBELIAN', description: 'Daftar purchase order, penerimaan barang dan hubungan supplier.', table: 'purchase_orders', columns: ['order_no', 'supplier_id', 'total_amount', 'status', 'order_date'], columnLabels: { order_no: 'No. PO', supplier_id: 'Supplier', total_amount: 'Total', status: 'Status', order_date: 'Tanggal' }, businessScoped: true, readOnly: true },
  receivables: { title: 'Piutang', eyebrow: 'KEUANGAN · PIUTANG', description: 'Tagihan pelanggan, jatuh tempo, pembayaran dan riwayat pelunasan.', table: 'receivables', columns: ['invoice_no', 'customer_id', 'original_amount', 'outstanding_amount', 'due_date', 'status'], columnLabels: { invoice_no: 'Invoice', customer_id: 'Pelanggan', original_amount: 'Nilai Awal', outstanding_amount: 'Sisa', due_date: 'Jatuh Tempo', status: 'Status' }, businessScoped: true, readOnly: true },
  payments: { title: 'Pembayaran', eyebrow: 'KEUANGAN · PEMBAYARAN', description: 'Transaksi pembayaran, provider, status verifikasi dan rekonsiliasi.', table: 'payments', columns: ['payment_no', 'amount', 'provider', 'status', 'paid_at'], columnLabels: { payment_no: 'No. Pembayaran', amount: 'Jumlah', provider: 'Provider', status: 'Status', paid_at: 'Dibayar' }, businessScoped: true, readOnly: true },
}
