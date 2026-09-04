'use client'

import { useEffect, useMemo, useState } from 'react'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './reports.module.css'

type Sale = { id: string; sale_no: string; sale_date: string; total_amount: number; hpp_amount: number; margin_amount: number; status: string }
type SaleItem = { sale_id: string; product_id: string; product_name_snapshot: string; qty: number; conversion_to_base: number; unit_price: number; line_total: number; hpp_unit: number; hpp_total: number }
type Product = { id: string; name: string; sku: string; current_cost: number; reorder_point: number; min_stock: number; cost_method: string }
type Purchase = { order_no: string; order_date: string; total_amount: number; status: string }
type GoodsReceipt = { receipt_no: string; receipt_date: string; total_amount: number; status: string }
type Receivable = { invoice_no: string; outstanding_amount: number; original_amount: number; paid_amount: number; due_date: string; status: string }
type Payment = { amount: number; status: string; payment_method_id: string; paid_at: string | null }
type PaymentMethod = { id: string; name: string; code: string }
type Stock = { product_id: string; location_id: string; qty_base: number; reserved_qty: number }

type ReportKey = 'overview' | 'sales' | 'purchases' | 'inventory' | 'receivables' | 'payments' | 'hpp'

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n || 0)
const compact = (n: number) => n >= 1_000_000 ? `Rp ${(n / 1_000_000).toFixed(1)} jt` : n >= 1_000 ? `Rp ${(n / 1_000).toFixed(0)} rb` : money(n)
const date = (v: string) => new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(v))
const completedSale = (s: string) => s === 'COMPLETED'
const postedReceipt = (s: string) => ['CONFIRMED', 'COMPLETED', 'POSTED'].includes(s)
const reportCards: Array<[ReportKey, string, string, string]> = [
  ['sales', 'Penjualan', 'sales + sale_items', 'Omzet, transaksi, item, HPP dan margin'],
  ['purchases', 'Pembelian', 'goods_receipts + purchase_orders', 'Barang diterima, nilai pembelian dan PO'],
  ['inventory', 'Persediaan', 'stock_balances + products', 'Saldo, reserved, nilai stok dan stok kritis'],
  ['receivables', 'Piutang', 'receivables', 'Outstanding, jatuh tempo dan aging'],
  ['payments', 'Pembayaran', 'payments + payment_methods', 'Dana masuk dan komposisi metode'],
  ['hpp', 'HPP & Margin', 'sale_items + products', 'Audit HPP transaksi dan metode costing'],
]

export default function ReportsPage() {
  const [period, setPeriod] = useState(30)
  const [selected, setSelected] = useState<ReportKey>('overview')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [data, setData] = useState<{ sales: Sale[]; items: SaleItem[]; products: Product[]; purchases: Purchase[]; receipts: GoodsReceipt[]; receivables: Receivable[]; payments: Payment[]; methods: PaymentMethod[]; stock: Stock[] } | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true); setError('')
      try {
        const db = requireSupabase(); const { business, branch } = await getActiveWorkspace()
        const { data: locations, error: locationError } = await db.from('locations').select('id,name,code').eq('branch_id', branch.id).eq('is_active', true).order('name').limit(1)
        if (locationError) throw locationError
        const activeLocationId = locations?.[0]?.id
        if (!activeLocationId) throw new Error(`Lokasi stok aktif untuk cabang ${String(branch.name || branch.code || branch.id)} tidak ditemukan.`)
        const since = new Date() since.setHours(0, 0, 0, 0); since.setDate(since.getDate() - (period - 1))
        const [sales, products, purchases, receipts, receivables, payments, methods, stock] = await Promise.all([
          db.from('sales').select('id,sale_no,sale_date,total_amount,hpp_amount,margin_amount,status').eq('business_id', business.id).eq('branch_id', branch.id).gte('sale_date', since.toISOString()).order('sale_date', { ascending: true }).limit(2000),
          db.from('products').select('id,name,sku,current_cost,reorder_point,min_stock,cost_method').eq('business_id', business.id).limit(2000),
          db.from('purchase_orders').select('order_no,order_date,total_amount,status').eq('business_id', business.id).eq('branch_id', branch.id).gte('order_date', since.toISOString()).limit(1000),
          db.from('goods_receipts').select('receipt_no,receipt_date,total_amount,status').eq('business_id', business.id).eq('branch_id', branch.id).gte('receipt_date', since.toISOString()).limit(1000),
          db.from('receivables').select('invoice_no,outstanding_amount,original_amount,paid_amount,due_date,status').eq('business_id', business.id).eq('branch_id', branch.id).gt('outstanding_amount', 0).limit(1000),
          db.from('payments').select('amount,status,payment_method_id,paid_at').eq('business_id', business.id).eq('branch_id', branch.id).gte('created_at', since.toISOString()).limit(2000),
          db.from('payment_methods').select('id,name,code').eq('business_id', business.id).eq('is_active', true),
          db.from('stock_balances').select('product_id,location_id,qty_base,reserved_qty').eq('location_id', activeLocationId).limit(3000),
        ])
        const ids = (sales.data || []).map(x => x.id)
        let itemsData: SaleItem[] = []
        if (ids.length) {
          const items = await db.from('sale_items').select('sale_id,product_id,product_name_snapshot,qty,conversion_to_base,unit_price,line_total,hpp_unit,hpp_total').in('sale_id', ids)
          if (items.error) throw items.error
          itemsData = items.data || []
        }
        const firstError = [sales, products, purchases, receipts, receivables, payments, methods, stock].find(x => x.error)?.error
        if (firstError) throw firstError
        if (!cancelled) setData({ sales: sales.data || [], items: itemsData, products: products.data || [], purchases: purchases.data || [], receipts: receipts.data || [], receivables: receivables.data || [], payments: payments.data || [], methods: methods.data || [], stock: stock.data || [] })
      } catch (e: any) { if (!cancelled) setError(e?.message || 'Laporan gagal dimuat.') }
      finally { if (!cancelled) setLoading(false) }
    })()
    return () => { cancelled = true }
  }, [period])

  const report = useMemo(() => {
    if (!data) return null
    const sales = data.sales.filter(x => completedSale(x.status))
    const receipts = data.receipts.filter(x => postedReceipt(x.status))
    const salesTotal = sales.reduce((a, x) => a + Number(x.total_amount || 0), 0)
    const hppFromItems = data.items.filter(x => sales.some(s => s.id === x.sale_id)).reduce((a, x) => a + Number(x.hpp_total || 0), 0)
    const hppFromHeader = sales.reduce((a, x) => a + Number(x.hpp_amount || 0), 0)
    const marginFromItems = data.items.filter(x => sales.some(s => s.id === x.sale_id)).reduce((a, x) => a + Number(x.line_total || 0) - Number(x.hpp_total || 0), 0)
    const margin = marginFromItems || sales.reduce((a, x) => a + Number(x.margin_amount || 0), 0)
    const purchaseReceived = receipts.reduce((a, x) => a + Number(x.total_amount || 0), 0)
    const purchaseOrders = data.purchases.filter(x => ['CONFIRMED', 'COMPLETED'].includes(x.status)).reduce((a, x) => a + Number(x.total_amount || 0), 0)
    const paid = data.payments.filter(x => x.status === 'PAID').reduce((a, x) => a + Number(x.amount || 0), 0)
    const today = new Date(); today.setHours(0, 0, 0, 0)
    const overdue = data.receivables.filter(x => new Date(x.due_date) < today).reduce((a, x) => a + Number(x.outstanding_amount || 0), 0)
    const productMap = new Map(data.products.map(x => [x.id, x]))
    const saleSet = new Set(sales.map(x => x.id))
    const top = new Map<string, { name: string; qty: number; value: number; hpp: number }>()
    data.items.forEach(x => { if (!saleSet.has(x.sale_id)) return; const current = top.get(x.product_id) || { name: x.product_name_snapshot, qty: 0, value: 0, hpp: 0 }; current.qty += Number(x.qty || 0); current.value += Number(x.line_total || 0); current.hpp += Number(x.hpp_total || 0); top.set(x.product_id, current) })
    const topProducts = [...top.entries()].sort((a, b) => b[1].value - a[1].value).slice(0, 10)
    const methodMap = new Map(data.methods.map(x => [x.id, x.name]))
    const paymentMix = new Map<string, number>()
    data.payments.filter(x => x.status === 'PAID').forEach(x => { const name = methodMap.get(x.payment_method_id) || 'Metode lain'; paymentMix.set(name, (paymentMix.get(name) || 0) + Number(x.amount || 0)) })
    const trend = Array.from({ length: Math.min(period, 14) }, (_, i) => { const day = new Date(); day.setHours(0, 0, 0, 0); day.setDate(day.getDate() - (Math.min(period, 14) - 1 - i)); const key = day.toISOString().slice(0, 10); return { label: new Intl.DateTimeFormat('id-ID', { day: '2-digit', month: 'short' }).format(day), value: sales.filter(x => x.sale_date.slice(0, 10) === key).reduce((a, x) => a + Number(x.total_amount || 0), 0) } })
    const inventoryValue = data.stock.reduce((a, x) => a + Math.max(0, Number(x.qty_base || 0) - Number(x.reserved_qty || 0)) * Number(productMap.get(x.product_id)?.current_cost || 0), 0)
    const lowStock = data.stock.filter(x => { const p = productMap.get(x.product_id); const available = Number(x.qty_base || 0) - Number(x.reserved_qty || 0); return p && available <= Math.max(Number(p.reorder_point || 0), Number(p.min_stock || 0)) }).length
    const aging = { current: 0, d1_30: 0, d31_60: 0, d61: 0 }
    data.receivables.forEach(x => { const days = Math.max(0, Math.floor((Date.now() - new Date(x.due_date).getTime()) / 86400000)); const value = Number(x.outstanding_amount || 0); if (days === 0) aging.current += value; else if (days <= 30) aging.d1_30 += value; else if (days <= 60) aging.d31_60 += value; else aging.d61 += value })
    return { sales, salesTotal, hpp: hppFromItems || hppFromHeader, hppFromItems, hppFromHeader, hppDelta: hppFromItems - hppFromHeader, margin, marginPct: salesTotal ? (margin / salesTotal) * 100 : 0, purchaseReceived, purchaseOrders, paid, overdue, topProducts, paymentMix: [...paymentMix.entries()].sort((a, b) => b[1] - a[1]), trend, maxTrend: Math.max(...trend.map(x => x.value), 1), inventoryValue, lowStock, salesCount: sales.length, purchaseCount: receipts.length, aging, productMap }
  }, [data, period])

  const detail = () => {
    if (!report || !data) return null
    if (selected === 'sales') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Detail penjualan</h2><p>Hanya transaksi COMPLETED. HPP diambil dari snapshot item saat transaksi.</p></div></div><div className={styles.table}><div className={styles.tr}><b>Invoice</b><b>Tanggal</b><b>Total</b><b>HPP</b><b>Margin</b></div>{report.sales.slice().reverse().slice(0, 30).map(s => <div className={styles.tr} key={s.id}><span>{s.sale_no}</span><span>{date(s.sale_date)}</span><span>{money(s.total_amount)}</span><span>{money(s.hpp_amount)}</span><span>{money(s.margin_amount)}</span></div>)}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Korelasi</h2><p>Jejak perhitungan</p></div></div><div className={styles.finance}><div><span>Penjualan</span><strong>{money(report.salesTotal)}</strong></div><div><span>HPP dari sale_items</span><strong>{money(report.hppFromItems)}</strong></div><div><span>Gross margin</span><strong>{money(report.margin)}</strong></div></div></article></section>
    if (selected === 'purchases') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Pembelian & penerimaan</h2><p>PO adalah komitmen pembelian. Barang benar-benar masuk saat Goods Receipt diposting.</p></div></div><div className={styles.table}><div className={styles.tr}><b>Receipt</b><b>Tanggal</b><b>Status</b><b>Total diterima</b></div>{data.receipts.length ? data.receipts.slice().reverse().slice(0, 30).map(x => <div className={styles.tr} key={x.receipt_no}><span>{x.receipt_no}</span><span>{date(x.receipt_date)}</span><span>{x.status}</span><span>{money(x.total_amount)}</span></div>) : <div className={styles.emptyRow}>Belum ada Goods Receipt pada periode ini.</div>}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Hubungan PO → stok</h2><p>Dipisahkan agar laporan tidak menghitung barang yang belum diterima sebagai stok.</p></div></div><div className={styles.finance}><div><span>Nilai PO terkonfirmasi</span><strong>{money(report.purchaseOrders)}</strong></div><div><span>Nilai barang diterima</span><strong>{money(report.purchaseReceived)}</strong></div></div></article></section>
    if (selected === 'inventory') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Persediaan</h2><p>Saldo berasal dari stock_balances, nilai menggunakan current_cost.</p></div></div><div className={styles.table}><div className={styles.tr}><b>SKU</b><b>Produk</b><b>Qty</b><b>Reserved</b><b>Nilai tersedia</b></div>{data.stock.slice(0, 40).map(x => { const p = report.productMap.get(x.product_id); const available = Math.max(0, Number(x.qty_base || 0) - Number(x.reserved_qty || 0)); return <div className={styles.tr} key={`${x.location_id}-${x.product_id}`}><span>{p?.sku || '-'}</span><span>{p?.name || 'Produk'}</span><span>{Number(x.qty_base || 0)}</span><span>{Number(x.reserved_qty || 0)}</span><span>{money(available * Number(p?.current_cost || 0))}</span></div> })}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Kontrol stok</h2><p>Prioritas replenishment</p></div></div><div className={styles.finance}><div><span>Nilai stok tersedia</span><strong>{money(report.inventoryValue)}</strong></div><div><span>Stok kritis</span><strong>{report.lowStock} baris</strong></div></div></article></section>
    if (selected === 'receivables') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Piutang outstanding</h2><p>Piutang adalah kewajiban pelanggan, bukan status offline.</p></div></div><div className={styles.table}><div className={styles.tr}><b>Invoice</b><b>Jatuh tempo</b><b>Original</b><b>Terbayar</b><b>Outstanding</b></div>{data.receivables.slice().sort((a,b) => a.due_date.localeCompare(b.due_date)).map(x => <div className={styles.tr} key={x.invoice_no}><span>{x.invoice_no}</span><span>{date(x.due_date)}</span><span>{money(x.original_amount)}</span><span>{money(x.paid_amount)}</span><span>{money(x.outstanding_amount)}</span></div>)}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Aging piutang</h2><p>Berdasarkan tanggal jatuh tempo</p></div></div><div className={styles.finance}><div><span>Belum jatuh tempo</span><strong>{money(report.aging.current)}</strong></div><div><span>1–30 hari</span><strong>{money(report.aging.d1_30)}</strong></div><div><span>31–60 hari</span><strong>{money(report.aging.d31_60)}</strong></div><div><span>&gt;60 hari</span><strong>{money(report.aging.d61)}</strong></div></div></article></section>
    if (selected === 'payments') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Pembayaran berhasil</h2><p>Hanya payment berstatus PAID.</p></div></div><div className={styles.table}><div className={styles.tr}><b>Waktu</b><b>Metode</b><b>Status</b><b>Nominal</b></div>{data.payments.filter(x => x.status === 'PAID').slice().reverse().slice(0, 40).map((x, i) => <div className={styles.tr} key={`${x.payment_method_id}-${x.paid_at}-${i}`}><span>{x.paid_at ? date(x.paid_at) : '-'}</span><span>{data.methods.find(m => m.id === x.payment_method_id)?.name || 'Metode lain'}</span><span>{x.status}</span><span>{money(x.amount)}</span></div>)}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Komposisi</h2><p>Rekonsiliasi dana masuk</p></div></div><div className={styles.mix}>{report.paymentMix.map(([name, value]) => <div className={styles.mixRow} key={name}><span>{name}</span><strong>{money(value)}</strong></div>)}</div></article></section>
    if (selected === 'hpp') return <section className={styles.detailGrid}><article className={styles.cardWide}><div className={styles.cardHead}><div><h2>Audit HPP per item</h2><p>Snapshot hpp_unit dan hpp_total menjaga histori transaksi tetap stabil.</p></div></div><div className={styles.table}><div className={styles.tr}><b>Produk</b><b>Qty</b><b>Harga jual</b><b>HPP/unit</b><b>HPP total</b></div>{data.items.filter(x => report.sales.some(s => s.id === x.sale_id)).slice(0, 50).map((x, i) => <div className={styles.tr} key={`${x.sale_id}-${x.product_id}-${i}`}><span>{x.product_name_snapshot}</span><span>{Number(x.qty || 0)}</span><span>{money(x.unit_price)}</span><span>{money(x.hpp_unit)}</span><span>{money(x.hpp_total)}</span></div>)}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Metode costing</h2><p>Konfigurasi master produk saat ini</p></div></div><div className={styles.hppBox}><strong>MOVING_AVERAGE</strong><p>HPP pembelian memperbarui biaya rata-rata bergerak. Saat penjualan diposting, biaya pada saat itu disimpan ke sale_items.hpp_unit dan sale_items.hpp_total.</p><div className={styles.formula}>HPP baru = ((stok lama × HPP lama) + (qty masuk × biaya masuk)) ÷ stok setelah penerimaan</div><small>Perubahan harga master produk setelah transaksi tidak boleh mengubah HPP historis.</small></div></article></section>
    return null
  }

  return <main className={styles.page}>
    <header className={styles.header}><div><span className={styles.eyebrow}>ANALITIK · REPORT CENTER</span><h1>Laporan bisnis</h1><p>Pilih laporan yang ingin dianalisis. Setiap angka ditelusurkan ke transaksi dan tabel sumbernya, bukan sekadar saling menempel seperti dashboard yang sedang berusaha terlihat pintar.</p></div><div className={styles.periods}>{[7, 30, 90].map(x => <button key={x} className={period === x ? styles.periodActive : ''} onClick={() => setPeriod(x)}>{x} hari</button>)}</div></header>
    {error ? <div className={styles.error}>{error}</div> : null}
    {loading ? <div className={styles.loading}>Menghitung laporan dari transaksi dan tabel terkait...</div> : null}
    {report ? <>
      <section className={styles.reportNav}><button className={selected === 'overview' ? styles.reportNavActive : ''} onClick={() => setSelected('overview')}><strong>Ringkasan</strong><span>KPI lintas modul</span></button>{reportCards.map(([key, name, source, detailText]) => <button key={key} className={selected === key ? styles.reportNavActive : ''} onClick={() => setSelected(key)}><strong>{name}</strong><span>{detailText}</span><small>{source}</small></button>)}</section>
      {selected === 'overview' ? <>
        <section className={styles.kpis}><div className={styles.kpi}><span>Penjualan bersih</span><strong>{money(report.salesTotal)}</strong><small>{report.salesCount} transaksi COMPLETED</small></div><div className={styles.kpi}><span>HPP penjualan</span><strong>{money(report.hpp)}</strong><small>Berbasis sale_items snapshot</small></div><div className={styles.kpi}><span>Gross margin</span><strong>{money(report.margin)}</strong><small>{report.marginPct.toFixed(1)}% dari penjualan</small></div><div className={styles.kpi}><span>Barang diterima</span><strong>{money(report.purchaseReceived)}</strong><small>{report.purchaseCount} Goods Receipt</small></div></section>
        <section className={styles.primaryGrid}><article className={`${styles.card} ${styles.chartCard}`}><div className={styles.cardHead}><div><h2>Tren penjualan</h2><p>Hanya transaksi COMPLETED</p></div><b>{compact(report.salesTotal)}</b></div><div className={styles.chart}>{report.trend.map(x => <div className={styles.barWrap} key={x.label}><span>{compact(x.value)}</span><div className={styles.bar} style={{ height: `${Math.max(5, x.value / report.maxTrend * 155)}px` }} /><small>{x.label}</small></div>)}</div></article><article className={styles.card}><div className={styles.cardHead}><div><h2>Arus pembayaran</h2><p>Payment berstatus PAID</p></div></div><div className={styles.bigNumber}>{money(report.paid)}</div><div className={styles.mix}>{report.paymentMix.length ? report.paymentMix.map(([name, value]) => <div className={styles.mixRow} key={name}><span>{name}</span><strong>{money(value)}</strong></div>) : <span className={styles.muted}>Belum ada pembayaran berhasil.</span>}</div></article></section>
        <section className={styles.secondaryGrid}><article className={styles.card}><div className={styles.cardHead}><div><h2>Produk terlaris</h2><p>Berdasarkan nilai penjualan item</p></div></div>{report.topProducts.length ? <div className={styles.rankList}>{report.topProducts.map(([id, x], i) => <div className={styles.rank} key={id}><b>{i + 1}</b><div><strong>{x.name}</strong><small>{x.qty} unit · HPP {money(x.hpp)}</small></div><span>{money(x.value)}</span></div>)}</div> : <p className={styles.muted}>Belum ada item penjualan.</p>}</article><article className={styles.card}><div className={styles.cardHead}><div><h2>Posisi bisnis</h2><p>Angka yang perlu dipantau</p></div></div><div className={styles.finance}><div><span>Piutang overdue</span><strong>{money(report.overdue)}</strong></div><div><span>Nilai stok tersedia</span><strong>{money(report.inventoryValue)}</strong></div><div><span>Stok kritis</span><strong>{report.lowStock} baris</strong></div><div><span>Selisih HPP header vs item</span><strong>{money(report.hppDelta)}</strong></div></div></article></section>
        <section className={styles.audit}><div><span className={styles.eyebrow}>DATA LINEAGE</span><h2>Korelasi laporan</h2><p>Penjualan → sale_items → HPP/margin. Pembelian → Goods Receipt → stok. Piutang → receivables → pembayaran. Persediaan → stock_balances → current cost.</p></div><div className={styles.auditGrid}>{reportCards.map(([key, name, source, detailText]) => <button key={key} onClick={() => setSelected(key)} className={styles.reportCard}><strong>{name}</strong><small>{source}</small><span>{detailText}</span></button>)}</div></section>
      </> : detail()}
    </> : null}
  </main>
}
