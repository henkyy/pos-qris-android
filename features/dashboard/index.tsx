'use client'

import { useEffect, useMemo, useState } from 'react'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './dashboard.module.css'

type Sale = { sale_no: string; sale_date: string; total_amount: number; status: string; customer_id: string | null }
type Purchase = { order_no: string; order_date: string; total_amount: number; status: string; supplier_id: string | null }
type Receivable = { invoice_no: string; customer_id: string; outstanding_amount: number; due_date: string; status: string }
type Stock = { product_id: string; location_id: string; qty_base: number; reserved_qty: number }
type Product = { id: string; name: string; sku: string; reorder_point: number; min_stock: number }
type Payment = { amount: number; status: string; payment_method_id: string; paid_at: string | null }

type DashboardData = {
  sales: Sale[]
  purchases: Purchase[]
  receivables: Receivable[]
  stock: Stock[]
  products: Product[]
  payments: Payment[]
  customerCount: number
  supplierCount: number
}

const money = (value: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(value || 0)
const compactMoney = (value: number) => value >= 1_000_000 ? `Rp ${(value / 1_000_000).toFixed(1)} jt` : value >= 1_000 ? `Rp ${(value / 1_000).toFixed(0)} rb` : money(value)
const dateLabel = (value: string) => new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'short' }).format(new Date(value))
const isCompleted = (status: string) => ['COMPLETED', 'CONFIRMED'].includes(status)

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const db = requireSupabase()
        const { business, branch } = await getActiveWorkspace()
        const { data: locations, error: locationError } = await db.from('locations').select('id,name,code').eq('branch_id', branch.id).eq('is_active', true).order('name').limit(1)
        if (locationError) throw locationError
        const activeLocationId = locations?.[0]?.id
        if (!activeLocationId) throw new Error(`Lokasi stok aktif untuk cabang ${String(branch.name || branch.code || branch.id)} tidak ditemukan.`)
        const since = new Date()
        since.setDate(since.getDate() - 30)

        const [salesRes, purchasesRes, receivablesRes, stockRes, productsRes, paymentsRes, customersRes, suppliersRes] = await Promise.all([
          db.from('sales').select('sale_no,sale_date,total_amount,status,customer_id').eq('business_id', business.id).eq('branch_id', branch.id).gte('sale_date', since.toISOString()).order('sale_date', { ascending: false }).limit(500),
          db.from('purchase_orders').select('order_no,order_date,total_amount,status,supplier_id').eq('business_id', business.id).eq('branch_id', branch.id).gte('order_date', since.toISOString()).order('order_date', { ascending: false }).limit(300),
          db.from('receivables').select('invoice_no,customer_id,outstanding_amount,due_date,status').eq('business_id', business.id).eq('branch_id', branch.id).gt('outstanding_amount', 0).order('due_date', { ascending: true }).limit(200),
          db.from('stock_balances').select('product_id,location_id,qty_base,reserved_qty').eq('location_id', activeLocationId).limit(1000),
          db.from('products').select('id,name,sku,reorder_point,min_stock').eq('business_id', business.id).eq('is_active', true).limit(1000),
          db.from('payments').select('amount,status,payment_method_id,paid_at').eq('business_id', business.id).eq('branch_id', branch.id).gte('created_at', since.toISOString()).limit(500),
          db.from('customers').select('id', { count: 'exact', head: true }).eq('business_id', business.id).eq('is_active', true),
          db.from('suppliers').select('id', { count: 'exact', head: true }).eq('business_id', business.id).eq('is_active', true),
        ])
        const firstError = [salesRes, purchasesRes, receivablesRes, stockRes, productsRes, paymentsRes, customersRes, suppliersRes].find(result => result.error)?.error
        if (firstError) throw firstError
        if (!cancelled) setData({ sales: salesRes.data || [], purchases: purchasesRes.data || [], receivables: receivablesRes.data || [], stock: stockRes.data || [], products: productsRes.data || [], payments: paymentsRes.data || [], customerCount: customersRes.count || 0, supplierCount: suppliersRes.count || 0 })
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Dashboard gagal memuat data.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [])

  const metrics = useMemo(() => {
    if (!data) return null
    const completedSales = data.sales.filter(x => isCompleted(x.status))
    const salesTotal = completedSales.reduce((sum, x) => sum + Number(x.total_amount || 0), 0)
    const salesCount = completedSales.length
    const avgSale = salesCount ? salesTotal / salesCount : 0
    const outstanding = data.receivables.reduce((sum, x) => sum + Number(x.outstanding_amount || 0), 0)
    const overdue = data.receivables.filter(x => new Date(x.due_date) < new Date()).reduce((sum, x) => sum + Number(x.outstanding_amount || 0), 0)
    const purchaseTotal = data.purchases.filter(x => isCompleted(x.status)).reduce((sum, x) => sum + Number(x.total_amount || 0), 0)
    const paid = data.payments.filter(x => x.status === 'PAID').reduce((sum, x) => sum + Number(x.amount || 0), 0)
    const productMap = new Map(data.products.map(x => [x.id, x]))
    const lowStock = data.stock.filter(x => {
      const p = productMap.get(x.product_id)
      const available = Number(x.qty_base || 0) - Number(x.reserved_qty || 0)
      return p && available <= Math.max(Number(p.reorder_point || 0), Number(p.min_stock || 0))
    })
    const byDay = Array.from({ length: 7 }, (_, i) => {
      const day = new Date(); day.setHours(0, 0, 0, 0); day.setDate(day.getDate() - (6 - i))
      const key = day.toISOString().slice(0, 10)
      const total = completedSales.filter(x => x.sale_date.slice(0, 10) === key).reduce((sum, x) => sum + Number(x.total_amount || 0), 0)
      return { label: new Intl.DateTimeFormat('id-ID', { weekday: 'short' }).format(day), total }
    })
    return { salesTotal, salesCount, avgSale, outstanding, overdue, purchaseTotal, paid, lowStock, byDay }
  }, [data])

  const productMap = useMemo(() => new Map((data?.products || []).map(x => [x.id, x])), [data])
  const recentSales = (data?.sales || []).filter(x => isCompleted(x.status)).slice(0, 6)
  const maxBar = Math.max(...(metrics?.byDay.map(x => x.total) || [1]), 1)

  return <main className={styles.page}>
    <header className={styles.header}>
      <div><span className={styles.eyebrow}>UTAMA · DASHBOARD</span><h1>Ringkasan bisnis</h1><p>Panel operasional lintas modul: penjualan, pembelian, persediaan, piutang, pembayaran dan master data.</p></div>
      <span className={styles.period}>30 hari terakhir</span>
    </header>

    {error ? <div className={styles.error}>{error}</div> : null}
    {loading ? <div className={styles.loading}>Mengambil ringkasan dari database...</div> : null}

    {metrics && data ? <>
      <section className={styles.kpis}>
        <div className={styles.kpi}><span className={styles.kpiLabel}>Penjualan selesai</span><strong className={styles.kpiValue}>{money(metrics.salesTotal)}</strong><span className={styles.kpiMeta}>{metrics.salesCount} transaksi · rata-rata {money(metrics.avgSale)}</span></div>
        <div className={styles.kpi}><span className={styles.kpiLabel}>Piutang berjalan</span><strong className={styles.kpiValue}>{money(metrics.outstanding)}</strong><span className={styles.kpiMeta}>{metrics.overdue > 0 ? `${money(metrics.overdue)} sudah jatuh tempo` : 'Tidak ada piutang lewat jatuh tempo'}</span></div>
        <div className={styles.kpi}><span className={styles.kpiLabel}>Pembelian selesai</span><strong className={styles.kpiValue}>{money(metrics.purchaseTotal)}</strong><span className={styles.kpiMeta}>{data.purchases.filter(x => isCompleted(x.status)).length} PO selesai dalam periode</span></div>
        <div className={styles.kpi}><span className={styles.kpiLabel}>Pembayaran diterima</span><strong className={styles.kpiValue}>{money(metrics.paid)}</strong><span className={styles.kpiMeta}>{data.customerCount} pelanggan · {data.supplierCount} supplier aktif</span></div>
      </section>

      <section className={styles.grid}>
        <article className={`${styles.card} ${styles.cardWide}`}><div className={styles.cardHeader}><div><h2>Tren penjualan</h2><p>Total transaksi selesai per hari, 7 hari terakhir</p></div></div><div className={styles.chart}>{metrics.byDay.map(day => <div className={styles.barWrap} key={day.label}><span className={styles.barValue}>{compactMoney(day.total)}</span><div className={styles.bar} style={{ height: `${Math.max(4, (day.total / maxBar) * 150)}px` }} /><span className={styles.barLabel}>{day.label}</span></div>)}</div></article>
        <article className={styles.card}><div className={styles.cardHeader}><div><h2>Alert operasional</h2><p>Hal yang perlu ditindaklanjuti</p></div></div><div className={styles.alertList}>
          <div className={styles.alert}><div><strong>Stok perlu perhatian</strong><span>Produk di bawah reorder point / minimum</span></div><span className={styles.alertValue}>{metrics.lowStock.length} item</span></div>
          <div className={styles.alert}><div><strong>Piutang jatuh tempo</strong><span>Saldo yang sudah melewati due date</span></div><span className={styles.alertValue}>{money(metrics.overdue)}</span></div>
          <div className={styles.alert}><div><strong>Pembayaran belum selesai</strong><span>Transaksi payment selain PAID</span></div><span className={styles.alertValue}>{data.payments.filter(x => x.status !== 'PAID').length}</span></div>
        </div></article>

        <article className={styles.card}><div className={styles.cardHeader}><div><h2>Stok kritis</h2><p>Saldo tersedia setelah reservasi</p></div></div>{metrics.lowStock.length ? <table className={styles.table}><thead><tr><th>Produk</th><th>Stok</th></tr></thead><tbody>{metrics.lowStock.slice(0, 6).map(row => <tr key={`${row.product_id}-${row.location_id}`}><td>{productMap.get(row.product_id)?.name || row.product_id.slice(0, 8)}</td><td>{Number(row.qty_base || 0) - Number(row.reserved_qty || 0)}</td></tr>)}</tbody></table> : <p className={styles.muted}>Tidak ada stok kritis.</p>}</article>
        <article className={styles.card}><div className={styles.cardHeader}><div><h2>Piutang terdekat</h2><p>Prioritas berdasarkan jatuh tempo</p></div></div>{data.receivables.length ? <table className={styles.table}><thead><tr><th>Invoice</th><th>Tempo</th><th>Sisa</th></tr></thead><tbody>{data.receivables.slice(0, 6).map(row => <tr key={row.invoice_no}><td>{row.invoice_no}</td><td>{dateLabel(row.due_date)}</td><td>{money(row.outstanding_amount)}</td></tr>)}</tbody></table> : <p className={styles.muted}>Tidak ada piutang berjalan.</p>}</article>
        <article className={styles.card}><div className={styles.cardHeader}><div><h2>Transaksi terbaru</h2><p>Penjualan selesai terbaru</p></div></div>{recentSales.length ? <table className={styles.table}><thead><tr><th>Transaksi</th><th>Tanggal</th><th>Total</th></tr></thead><tbody>{recentSales.map(row => <tr key={row.sale_no}><td>{row.sale_no}</td><td>{dateLabel(row.sale_date)}</td><td>{money(row.total_amount)}</td></tr>)}</tbody></table> : <p className={styles.muted}>Belum ada penjualan selesai.</p>}</article>
      </section>

      <section className={styles.summaryGrid}>
        <div className={styles.summary}><span>Produk aktif</span><strong>{data.products.length}</strong></div>
        <div className={styles.summary}><span>Pelanggan aktif</span><strong>{data.customerCount}</strong></div>
        <div className={styles.summary}><span>Supplier aktif</span><strong>{data.supplierCount}</strong></div>
      </section>
    </> : null}
  </main>
}
