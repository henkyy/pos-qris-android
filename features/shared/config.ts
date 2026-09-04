import type { EntityConfig } from './EntityPage'

export const entityConfigs: Record<string, EntityConfig> = {
  products: {
    title: 'Produk', eyebrow: 'MASTER · PRODUK', description: 'Kelola katalog yang dipakai kasir. Perubahan master tidak mengubah histori transaksi.', table: 'products',
    columns: ['name', 'sku', 'barcode', 'is_active'], columnLabels: { name: 'Nama Produk', sku: 'SKU', barcode: 'Barcode', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    workflow: ['Buat produk', 'Atur kategori & satuan', 'Atur harga', 'Siap dijual'],
    usageNote: 'Edit untuk koreksi master. Jika produk sudah pernah ditransaksikan, gunakan Nonaktifkan, bukan hapus data historis.',
    fields: [
      { key: 'name', label: 'Nama produk', required: true, placeholder: 'Contoh: Nasi Goreng' },
      { key: 'sku', label: 'SKU', required: true, placeholder: 'Contoh: FD-001' },
      { key: 'barcode', label: 'Barcode', placeholder: 'Opsional' },
      { key: 'base_unit_id', label: 'Satuan dasar', required: true, type: 'select', source: 'units', help: 'Satuan dasar menjadi acuan stok produk.' },
      { key: 'category_id', label: 'Kategori', type: 'select', source: 'categories' },
    ],
  },
  customers: {
    title: 'Pelanggan', eyebrow: 'MASTER · PELANGGAN', description: 'Kelola identitas pelanggan, limit kredit dan status yang dipakai pada penjualan.', table: 'customers',
    columns: ['name', 'phone', 'email', 'credit_limit', 'is_active'], columnLabels: { name: 'Nama Pelanggan', phone: 'Telepon', email: 'Email', credit_limit: 'Limit Kredit', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    workflow: ['Buat pelanggan', 'Atur limit kredit', 'Pakai di penjualan', 'Pantau piutang'],
    usageNote: 'Menonaktifkan pelanggan mencegah transaksi baru memakai pelanggan tersebut, tetapi histori penjualan dan piutang tetap tersimpan.',
    fields: [
      { key: 'name', label: 'Nama pelanggan', required: true },
      { key: 'code', label: 'Kode pelanggan', required: true },
      { key: 'phone', label: 'Telepon' }, { key: 'email', label: 'Email' }, { key: 'credit_limit', label: 'Limit kredit', type: 'number', help: 'Gunakan 0 jika pelanggan tidak boleh memiliki saldo kredit.' },
    ],
  },
  suppliers: {
    title: 'Supplier', eyebrow: 'MASTER · SUPPLIER', description: 'Kelola pemasok, kontak dan termin yang menjadi dasar proses pembelian serta hutang.', table: 'suppliers',
    columns: ['name', 'code', 'phone', 'email', 'is_active'], columnLabels: { name: 'Nama Supplier', code: 'Kode', phone: 'Telepon', email: 'Email', is_active: 'Status' },
    businessScoped: true, softDelete: true,
    workflow: ['Buat supplier', 'Atur termin', 'Buat PO', 'Terima & bayar'],
    usageNote: 'Termin pembayaran digunakan sebagai dasar jatuh tempo hutang. Menonaktifkan supplier tidak menghapus histori pembelian.',
    fields: [
      { key: 'name', label: 'Nama supplier', required: true }, { key: 'code', label: 'Kode supplier', required: true },
      { key: 'phone', label: 'Telepon' }, { key: 'email', label: 'Email' }, { key: 'payment_term_days', label: 'Tempo pembayaran (hari)', type: 'number', help: 'Contoh: 30 untuk tempo 30 hari.' },
    ],
  },
  categories: {
    title: 'Kategori', eyebrow: 'MASTER · PRODUK', description: 'Atur pengelompokan produk untuk kasir, pencarian dan laporan.', table: 'categories', columns: ['name', 'code', 'is_active'], columnLabels: { name: 'Nama Kategori', code: 'Kode', is_active: 'Status' },
    businessScoped: true, softDelete: true, workflow: ['Buat kategori', 'Hubungkan produk', 'Gunakan di kasir', 'Analisis laporan'], usageNote: 'Nonaktifkan kategori jika tidak ingin dipakai untuk produk baru. Produk dan transaksi lama tetap aman.',
    fields: [{ key: 'name', label: 'Nama kategori', required: true }, { key: 'code', label: 'Kode kategori', required: true }],
  },
  units: {
    title: 'Satuan', eyebrow: 'MASTER · PRODUK', description: 'Kelola satuan dasar dan aturan angka desimal untuk produk.', table: 'units', columns: ['name', 'code', 'symbol', 'decimal_places'], columnLabels: { name: 'Nama Satuan', code: 'Kode', symbol: 'Simbol', decimal_places: 'Desimal' },
    businessScoped: true, softDelete: true, workflow: ['Buat satuan', 'Hubungkan ke produk', 'Gunakan di transaksi'], usageNote: 'Jangan menonaktifkan satuan yang masih dibutuhkan produk aktif. Perubahan satuan transaksi historis tidak dilakukan dari master.',
    fields: [{ key: 'name', label: 'Nama satuan', required: true }, { key: 'code', label: 'Kode satuan', required: true }, { key: 'symbol', label: 'Simbol' }, { key: 'decimal_places', label: 'Jumlah desimal', type: 'number' }],
  },
  stock: { title: 'Stok', eyebrow: 'PERSEDIAAN', description: 'Pantau posisi stok per lokasi. Koreksi stok dilakukan melalui adjustment, penerimaan atau transfer agar histori tetap terlacak.', table: 'stock_balances', columns: ['product_id', 'location_id', 'qty_base'], columnLabels: { product_id: 'Produk', location_id: 'Lokasi', qty_base: 'Stok' }, readOnly: true, workflow: ['Pantau stok', 'Stock opname', 'Adjustment / transfer', 'Replenishment'], usageNote: 'Saldo stok bukan data yang diedit bebas. Setiap koreksi harus melalui transaksi persediaan agar laporan dan histori tetap konsisten.' },
  orders: { title: 'Pesanan', eyebrow: 'TRANSAKSI · PESANAN', description: 'Pusat pemantauan pesanan dan status penjualan dari dibuat sampai selesai.', table: 'sales', columns: ['sale_no', 'customer_id', 'total_amount', 'status', 'sale_date'], columnLabels: { sale_no: 'No. Pesanan', customer_id: 'Pelanggan', total_amount: 'Total', status: 'Status', sale_date: 'Tanggal' }, businessScoped: true, readOnly: true, workflow: ['Pesanan dibuat', 'Diproses', 'Dibayar', 'Selesai'], usageNote: 'Pesanan yang sudah menjadi transaksi selesai tidak dihapus. Koreksi dilakukan melalui pembatalan, void atau refund sesuai status.' },
  purchases: { title: 'Pembelian', eyebrow: 'TRANSAKSI · PEMBELIAN', description: 'Kelola alur PO, penerimaan barang dan kewajiban pembayaran supplier.', table: 'purchase_orders', columns: ['order_no', 'supplier_id', 'total_amount', 'status', 'order_date'], columnLabels: { order_no: 'No. PO', supplier_id: 'Supplier', total_amount: 'Total', status: 'Status', order_date: 'Tanggal' }, businessScoped: true, readOnly: true, workflow: ['Draft PO', 'Konfirmasi', 'Terima barang', 'Bayar hutang'], usageNote: 'PO yang sudah diterima tidak boleh dihapus karena penerimaan telah memengaruhi stok dan dapat membentuk hutang supplier.' },
  receivables: { title: 'Piutang', eyebrow: 'KEUANGAN · PIUTANG', description: 'Pantau tagihan pelanggan, jatuh tempo, pembayaran sebagian dan pelunasan.', table: 'receivables', columns: ['invoice_no', 'customer_id', 'original_amount', 'outstanding_amount', 'due_date', 'status'], columnLabels: { invoice_no: 'Invoice', customer_id: 'Pelanggan', original_amount: 'Nilai Awal', outstanding_amount: 'Sisa', due_date: 'Jatuh Tempo', status: 'Status' }, businessScoped: true, readOnly: true, workflow: ['Piutang terbentuk', 'Jatuh tempo', 'Bayar sebagian', 'Lunas'], usageNote: 'Piutang tidak dihapus untuk mengoreksi kesalahan. Gunakan pembayaran atau reversal sesuai transaksi keuangan.' },
  payments: { title: 'Pembayaran', eyebrow: 'KEUANGAN · PEMBAYARAN', description: 'Pantau pembayaran, status verifikasi provider dan rekonsiliasi.', table: 'payments', columns: ['payment_no', 'amount', 'provider', 'status', 'paid_at'], columnLabels: { payment_no: 'No. Pembayaran', amount: 'Jumlah', provider: 'Provider', status: 'Status', paid_at: 'Dibayar' }, businessScoped: true, readOnly: true, workflow: ['Dibuat', 'Menunggu / diproses', 'Terverifikasi', 'Direkonsiliasi'], usageNote: 'Pembayaran terverifikasi bersifat immutable. Koreksi dilakukan melalui reversal atau proses rekonsiliasi, bukan delete.' },
}
