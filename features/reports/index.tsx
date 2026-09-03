export default function ReportsPage() {
  const reports = ['Penjualan', 'Produk terlaris', 'Stok', 'Pembelian', 'Piutang', 'Pembayaran']
  return <div className="module-page"><div className="module-hero"><div><span className="eyebrow">ANALITIK · LAPORAN</span><h1>Laporan</h1><p>Lapisan analitik dipisahkan dari transaksi agar query laporan tidak mengotori alur kasir.</p></div></div><div className="report-grid">{reports.map(name => <section className="module-card report-card" key={name}><strong>Laporan {name}</strong><span>Siap menjadi query/report service khusus modul {name.toLowerCase()}.</span><small>Fondasi</small></section>)}</div></div>
}
