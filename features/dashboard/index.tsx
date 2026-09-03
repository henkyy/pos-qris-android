'use client'
import { useEffect, useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import EmptyState from '../../components/ui/EmptyState'

export default function DashboardPage() {
  const [counts, setCounts] = useState({ products: 0, customers: 0, suppliers: 0 })
  const [loading, setLoading] = useState(true)
  useEffect(() => { (async () => { try { const db = requireSupabase(); const [{ count: products }, { count: customers }, { count: suppliers }] = await Promise.all([db.from('products').select('*', { count: 'exact', head: true }), db.from('customers').select('*', { count: 'exact', head: true }), db.from('suppliers').select('*', { count: 'exact', head: true })]); setCounts({ products: products || 0, customers: customers || 0, suppliers: suppliers || 0 }) } finally { setLoading(false) } })() }, [])
  return <div className="module-page"><div className="module-hero"><div><span className="eyebrow">UTAMA · DASHBOARD</span><h1>Ringkasan bisnis</h1><p>Dashboard operasional menjadi titik masuk untuk KPI, aktivitas dan alert bisnis.</p></div></div><div className="stat-grid"><div className="stat"><span>Produk</span><strong>{loading ? '…' : counts.products}</strong><small>Katalog aktif</small></div><div className="stat"><span>Pelanggan</span><strong>{loading ? '…' : counts.customers}</strong><small>Master pelanggan</small></div><div className="stat"><span>Supplier</span><strong>{loading ? '…' : counts.suppliers}</strong><small>Master supplier</small></div><div className="stat"><span>Penjualan</span><strong>Live</strong><small>Core POS tetap di Penjualan</small></div></div><section className="module-card"><EmptyState title="Fondasi dashboard aktif" text="KPI penjualan, stok, piutang dan pembayaran akan mengambil data dari modul masing-masing tanpa membuat logic baru di AppShell." /></section></div>
}
