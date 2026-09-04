'use client'

import { useEffect, useMemo, useState } from 'react'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './reports.module.css'

type Sale = { id: string; sale_no: string; sale_date: string; total_amount: number; hpp_amount: number; margin_amount: number; status: string }
type SaleItem = { sale_id: string; product_id: string; product_name_snapshot: string; qty: number; line_total: number }
type Product = { id: string; name: string; sku: string; current_cost: number; reorder_point: number; min_stock: number }
type Purchase = { order_no: string; order_date: string; total_amount: number; status: string }
type Receivable = { invoice_no: string; outstanding_amount: number; due_date: string; status: string }
type Payment = { amount: number; status: string; payment_method_id: string; paid_at: string | null }
type PaymentMethod = { id: string; name: string; code: string }
type Stock = { product_id: string; location_id: string; qty_base: number; reserved_qty: number }

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n || 0)
const compact = (n: number) => n >= 1_000_000 ? `Rp ${(n / 1_000_000).toFixed(1)} jt` : n >= 1_000 ? `Rp ${(n / 1_000).toFixed(0)} rb` : money(n)
const date = (v: string) => new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'short' }).format(new Date(v))
const completedSale = (s: string) => s === 'COMPLETED'
const completedPurchase = (s: string) => ['CONFIRMED', 'COMPLETED'].includes(s)

export default function ReportsPage() {
  const [period, setPeriod] = useState(30)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [data, setData] = useState<{ sales: Sale[]; items: SaleItem[]; products: Product[]; purchases: Purchase[]; receivables: Receivable[]; payments: Payment[]; methods: PaymentMethod[]; stock: Stock[] } | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true); setError('')
      try {
        const db = requireSupabase(); const { business, branch } = await getActiveWorkspace()
        const since = new Date(); since.setHours(0, 0, 0, 0); since.setDate(since.getDate() - (period - 1))
        const [sales, products, purchases, receivables, payments, methods, stock] = await Promise.all([
          db.from('sales').select('id,sale_no,sale_date,total_amount,hpp_amount,margin_amount,status').eq('business_id', business.id).eq('branch_id', branch.id).gte('sale_date', since.toISOString()).order('sale_date', { ascending: true }).limit(2000),
          db.from('products').select('id,name,sku,current_cost,reorder_point,min_stock').eq('business_id', business.id).eq('is_active', true).limit(2000),
          db.from('purchase_orders').select('order_no,order_date,total_amount,status').eq('business_id', business.id).eq('branch_id', branch.id).gte('order_date', since.toISOString()).limit(1000),
          db.from('receivables').select('invoice_no,outstanding_amount,due_date,status').eq('business_id', business.id).eq('branch_id', branch.id).gt('outstanding_amount', 0).limit(1000),
          db.from('payments').select('amount,status,payment_method_id,paid_at').eq('business_id', business.id).eq('branch_id', branch.id).gte('created_at', since.toISOString()).limit(2000),
          db.from('payment_methods').select('id,name,code').eq('business_id', business.id).eq('is_active', true),
          db.from('stock_balances').select('product_id,location_id,qty_base,reserved_qty').limit(3000),
        ])
        const ids = (sales.data || []).map(x => x.id)
        let itemsData: SaleItem[] = []
        if (ids.length) {
          const items = await db.from('sale_items').select('sale_id,product_id,product_name_snapshot,qty,line_total').in('sale_id', ids)
          if (items.error) throw items.error
          itemsData = items.data || []
        }
        const firstError = [sales, products, purchases, receivables, payments, methods, stock].find(x => x.error)?.error
        if (firstError) throw firstError
        if (!cancelled) setData({ sales: sales.data || [], items: itemsData, products: products.data || [], purchases: purchases.data || [], receivables: receivables.data || [], payments: payments.data || [], methods: methods.data || [], stock: stock.data || [] })
      } catch (e: any) { if (!cancelled) setError(e?.message || 'Laporan gagal dimuat.') }
      finally { if (!cancelled) setLoading(false) }
    })()
    return () => { cancelled = true }
  }, [period])

  const report = useMemo(() => {
    if (!data) return null
    const sales = data.sales.filter(x => completedSale(x.status))
    const purchases = data.purchases.filter(x => completedPurchase(x.status))
    const salesTotal = sales.reduce((a, x) => a + Number(x.total_amount || 0), 0)
    const hpp = sales.reduce((a, x) => a + Number(x.hpp_amount || 0), 0)
    const margin = sales.reduce((a, x) => a + Number(x.margin_amount || 0), 0)
    const purchaseTotal = purchases.reduce((a, x) => a + Number(x.total_amount || 0), 0)
    const paid = data.payments.filter(x => x.status === 'PAID').reduce((a, x) => a + Number(x.amount || 0), 0)
    const overdue = data.receivables.filter(x => new Date(x.due_date) < new Date()).reduce((a, x) => a + Number(x.outstanding_amount || 0), 0)
    const productMap = new Map(data.products.map(x => [x.id, x]))
    const top = new Map<string, { name: string; qty: number; value: number }>()
    data.items.forEach(x => { if (!sales.some(s => s.id === x.sale_id)) return; const current = top.get(x.product_id) || { name: x.product_name_snapshot, qty: 0, value: 0 }; current.qty += Number(x.qty || 0); current.value += Number(x.line_total || 0); top.set(x.product_id, current) })
    const topProducts = [...top.entries()].sort((a, b) => b[1].value - a[1].value).slice(0, 7)
    const methodMap = new Map(data.methods.map(x => [x.id, x.name]))
    const paymentMix = new Map<string, number>()
    data.payments.filter(x => x.status === 'PAID').forEach(x => paymentMix.set(methodMap.get(x.payment_method_id) || 'Metode lain', (paymentMix.get(methodMap.get(x.payment_method_id) || 'Metode lain') || 0) + Number(x.amount || 0)))
    const trend = Array.from({ length: Math.min(period, 14) }, (_, i) => { const day = new Date(); day.setHours(0, 0, 0, 0); day.setDate(day.getDate() - (Math.min(period, 14) - 1 - i)); const key = day.toISOString().slice(0, 10); return { label: new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'short' }).format(day), value: sales.filter(x => x.sale_date.slice(0, 10) === key).reduce((a, x) => a + Number(x.total_amount || 0), 0) } })
    const inventoryValue = data.stock.reduce((a, x) => a + Math.max(0, Number(x.qty_base || 0) - Number(x.reserved_qty || 0)) * Number(productMap.get(x.product_id)?.current_cost || 0), 0)
    const lowStock = data.stock.filter(x => { const p = productMap.get(x.product_id); const available = Number(x.qty_base || 0) - Number(x.reserved_qty || 0); return p && available <= Math.max(Number(p.reorder_point || 0), Number(p.min_stock || 0)) }).length
    return { salesTotal, hpp, margin, marginPct: salesTotal ? (margin / salesTotal) * 100 : 0, purchaseTotal, paid, overdue, topProducts, paymentMix: [...paymentMix.entries()].sort((a, b) => b[1] - a[1]), trend, maxTrend: Math.max(...trend.map(x => x.value), 1), inventoryValue, lowStock, salesCount: sales.length, purchaseCount: purchases.length }
  }, [data, period])

  return <main className={styles.page}>
    <header className={styles.header}><div><span className={styles.eyebrow}>ANALITIK · LAPORAN</span><h1>Laporan bisnis</h1><p>Ringkasan yang menghubungkan penjualan, HPP, margin, pembelian, stok, piutang dan pembayaran dalam satu periode.</p></div><div className={styles.periods}>{[7, 30, 90].map(x => <button key={x} className={period === x ? styles.periodActive : ''} onClick={() => setPeriod(x)}>{x} hari</button>)}</div></header>
    {error ? <div className={styles.error}>{error}</div> : null}
    {loading ? <div className={styles.loading}>Menghitung laporan dari transaksi dan tabel terkait...</div> : null}
    {report ? <>
      <section className={styles.kpis}>
        <div className={styles.kpi}><span>Penjualan bersih</span><strong>{money(report.salesTotal)}</strong><small>{report.salesCount} transaksi selesai</small></div>
        <div className={styles.kpi}><span>Gross margin</span><strong>{money(report.margin)}</strong><small>{report.marginPct.toFixed(1)}% dari penjualan</small></div>
        <div className={styles.kpi}><span>HPP penjualan</span><strong>{money(report.hpp)}</strong><small>Diambil dari snapshot HPP transaksi</small></div>
        <div className={styles.kpi}><span>Pembelian selesai</span><strong>{money(report.purchaseTotal)}</strong><small>{report.purchaseCount} PO dalam periode</small></div>
      </section>

      <section className={styles.primaryGrid}>
        <article className={`${styles.card} ${styles.chartCard}`}><div className={styles.cardHead}><div><h2>Tren penjualan</h2><p>Nilai penjualan dengan status selesai</p></div><b>{compact(report.salesTotal)}</b></div><div className={styles.chart}>{report.trend.map(x => <div className={styles.barWrap} key={x.label}><span>{compact(x.value)}</span><div className={styles.bar} style={{ height: `${Math.max(5, x.value / report.maxTrend * 155)}px` }} /><small>{x.label}</small></div>)}</div></article>
        <article className={styles.card}><div className={styles.cardHead}><div><h2>Arus pembayaran</h2><p>Payment berstatus PAID</p></div></div><div className={styles.bigNumber}>{money(report.paid)}</div><div className={styles.mix}>{report.paymentMix.length ? report.paymentMix.map(([name, value]) => <div className={styles.mixRow} key={name}><span>{name}</span><strong>{money(value)}</strong></div>) : <span className={styles.muted}>Belum ada pembayaran berhasil.</span>}</div></article>
      </section>

      <section className={styles.secondaryGrid}>
        <article className={styles.card}><div className={styles.cardHead}><div><h2>Produk terlaris</h2><p>Berdasarkan nilai penjualan item</p></div></div>{report.topProducts.length ? <div className={styles.rankList}>{report.topProducts.map(([id, x], i) => <div className={styles.rank} key={id}><b>{i + 1}</b><div><strong>{x.name}</strong><small>{x.qty} unit terjual</small></div><span>{money(x.value)}</span></div>)}</div> : <p className={styles.muted}>Belum ada item penjualan.</p>}</article>
        <article className={styles.card}><div className={styles.cardHead}><div><h2>Posisi keuangan</h2><p>Saldo yang perlu dipantau</p></div></div><div className={styles.finance}><div><span>Piutang overdue</span><strong>{money(report.overdue)}</strong></div><div><span>Nilai stok tersedia</span><strong>{money(report.inventoryValue)}</strong></div><div><span>Stok kritis</span><strong>{report.lowStock} item</strong></div></div></article>
      </section>

      <section className={styles.reportLinks}><div><span className={styles.eyebrow}>DETAIL</span><h2>Jenis laporan</h2><p>Setiap laporan menggunakan sumber transaksi yang relevan, bukan mengubah data operasional.</p></div><div className={styles.reportCards}>{[['Penjualan','sales + sale_items','Omzet, HPP, margin, transaksi'],['Produk terlaris','sale_items + products','Qty dan nilai item terjual'],['Stok','stock_balances + products','Saldo, reservasi, nilai persediaan'],['Pembelian','purchase_orders','Nilai dan jumlah PO selesai'],['Piutang','receivables','Outstanding dan overdue'],['Pembayaran','payments + payment_methods','Nilai PAID dan komposisi metode']].map(([name, source, detail]) => <div className={styles.reportCard} key={name}><strong>{name}</strong><small>{source}</small><span>{detail}</span></div>)}</div></section>
    </> : null}
  </main>
}
