'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import { getActiveWorkspace } from '../../lib/business-context'
import styles from './operations.module.css'

type Row = Record<string, unknown>
export type OperationsMenuId = 'Retur Penjualan' | 'Transfer Stok' | 'Pembayaran Piutang' | 'Pembayaran Hutang' | 'Kas' | 'Held Sales'

type Props = { menu: OperationsMenuId }

type Meta = { title: string; description: string; eyebrow: string }

const META: Record<OperationsMenuId, Meta> = {
  'Retur Penjualan': { title: 'Retur Penjualan', description: 'Pantau retur dan refund penjualan tanpa menghapus transaksi asal.', eyebrow: 'TRANSAKSI' },
  'Transfer Stok': { title: 'Transfer Stok', description: 'Pantau perpindahan stok antar lokasi dalam outlet aktif.', eyebrow: 'INVENTORI' },
  'Pembayaran Piutang': { title: 'Pembayaran Piutang', description: 'Riwayat penerimaan pembayaran untuk tagihan pelanggan.', eyebrow: 'PELANGGAN' },
  'Pembayaran Hutang': { title: 'Pembayaran Hutang', description: 'Riwayat pembayaran kewajiban kepada supplier.', eyebrow: 'PEMBELIAN' },
  'Kas': { title: 'Kas', description: 'Pergerakan kas yang terkait dengan shift kasir dan transaksi operasional.', eyebrow: 'KEUANGAN' },
  'Held Sales': { title: 'Held Sales', description: 'Transaksi yang ditahan sementara di terminal kasir perangkat ini.', eyebrow: 'TRANSAKSI' },
}

const money = (value: unknown) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(Math.round(Number(value) || 0))
const number = (value: unknown) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 2 }).format(Number(value) || 0)
const date = (value: unknown) => value ? new Intl.DateTimeFormat('id-ID', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(String(value))) : '-'
const text = (value: unknown) => String(value ?? '-').replaceAll('_', ' ')

const COLUMNS: Record<OperationsMenuId, { key: string; label: string; money?: boolean; qty?: boolean; date?: boolean }[]> = {
  'Retur Penjualan': [
    { key: 'return_no', label: 'No. Retur' }, { key: 'return_date', label: 'Tanggal', date: true },
    { key: 'original_sale_id', label: 'Transaksi Asal' }, { key: 'status', label: 'Status' }, { key: 'total_amount', label: 'Nilai', money: true },
  ],
  'Transfer Stok': [
    { key: 'transfer_no', label: 'No. Transfer' }, { key: 'transfer_date', label: 'Tanggal', date: true },
    { key: 'from_location_name', label: 'Dari' }, { key: 'to_location_name', label: 'Ke' }, { key: 'status', label: 'Status' },
  ],
  'Pembayaran Piutang': [
    { key: 'invoice_no', label: 'Invoice' }, { key: 'paid_at', label: 'Tanggal', date: true }, { key: 'amount', label: 'Jumlah', money: true }, { key: 'receivable_status', label: 'Status' },
  ],
  'Pembayaran Hutang': [
    { key: 'invoice_no', label: 'Invoice' }, { key: 'paid_at', label: 'Tanggal', date: true }, { key: 'amount', label: 'Jumlah', money: true }, { key: 'payable_status', label: 'Status' },
  ],
  'Kas': [
    { key: 'created_at', label: 'Tanggal', date: true }, { key: 'terminal_name', label: 'Shift' }, { key: 'movement_type', label: 'Jenis' }, { key: 'amount', label: 'Jumlah', money: true }, { key: 'reason', label: 'Keterangan' },
  ],
  'Held Sales': [
    { key: 'name', label: 'Nama' }, { key: 'items_count', label: 'Item', qty: true }, { key: 'qty', label: 'Qty', qty: true }, { key: 'note', label: 'Catatan' }, { key: 'createdAt', label: 'Ditahan', date: true },
  ],
}

function Table({ menu, rows }: { menu: OperationsMenuId; rows: Row[] }) {
  const columns = COLUMNS[menu]
  return <div className={styles.tableWrap}>
    <table>
      <thead><tr>{columns.map(column => <th key={column.key}>{column.label}</th>)}</tr></thead>
      <tbody>
        {rows.length ? rows.map((row, index) => <tr key={String(row.id ?? row.return_no ?? row.transfer_no ?? index)}>{columns.map(column => {
          const value = row[column.key]
          return <td key={column.key} className={column.money || column.qty ? styles.numeric : ''}>{column.money ? money(value) : column.qty ? number(value) : column.date ? date(value) : text(value)}</td>
        })}</tr>) : <tr><td colSpan={columns.length} className={styles.empty}>Belum ada data pada outlet aktif.</td></tr>}
      </tbody>
    </table>
  </div>
}

export default function OperationsPage({ menu }: Props) {
  const meta = META[menu]
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [branchName, setBranchName] = useState('Outlet aktif')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      if (menu === 'Held Sales') {
        let held: any[] = []
        try { held = JSON.parse(window.localStorage.getItem('qris-held-orders') || '[]') } catch { held = [] }
        setRows(held.map(order => ({ ...order, items_count: Array.isArray(order.items) ? order.items.length : 0, qty: Array.isArray(order.items) ? order.items.reduce((sum: number, item: any) => sum + Number(item.qty || 0), 0) : 0 })))
        setLoading(false)
        return
      }

      const workspace = await getActiveWorkspace()
      const { business, branch } = workspace
      setBranchName(String(branch.name || branch.code || 'Outlet aktif'))
      const db = requireSupabase()
      let next: Row[] = []

      if (menu === 'Retur Penjualan') {
        const { data, error: e } = await db.from('sales_returns').select('id,return_no,return_date,original_sale_id,status,total_amount,reason').eq('business_id', business.id).eq('branch_id', branch.id).order('return_date', { ascending: false }).limit(500)
        if (e) throw e
        next = data || []
      } else if (menu === 'Transfer Stok') {
        const { data, error: e } = await db.from('stock_transfers').select('id,transfer_no,transfer_date,from_location_id,to_location_id,status,notes').eq('business_id', business.id).eq('branch_id', branch.id).order('transfer_date', { ascending: false }).limit(500)
        if (e) throw e
        const locationIds = [...new Set((data || []).flatMap(item => [item.from_location_id, item.to_location_id]).filter(Boolean))]
        const locations = locationIds.length ? await db.from('locations').select('id,name').in('id', locationIds) : { data: [], error: null }
        if (locations.error) throw locations.error
        const map = new Map((locations.data || []).map(item => [String(item.id), String(item.name)]))
        next = (data || []).map(item => ({ ...item, from_location_name: map.get(String(item.from_location_id)) || '-', to_location_name: map.get(String(item.to_location_id)) || '-' }))
      } else if (menu === 'Pembayaran Piutang') {
        const { data, error: e } = await db.from('receivable_payments').select('id,receivable_id,payment_id,amount,paid_at').order('paid_at', { ascending: false }).limit(500)
        if (e) throw e
        const ids = [...new Set((data || []).map(item => item.receivable_id).filter(Boolean))]
        const receivables = ids.length ? await db.from('receivables').select('id,invoice_no,status,business_id,branch_id').in('id', ids) : { data: [], error: null }
        if (receivables.error) throw receivables.error
        const map = new Map((receivables.data || []).filter(item => item.business_id === business.id && item.branch_id === branch.id).map(item => [String(item.id), item]))
        next = (data || []).filter(item => map.has(String(item.receivable_id))).map(item => ({ ...item, invoice_no: map.get(String(item.receivable_id))?.invoice_no, receivable_status: map.get(String(item.receivable_id))?.status }))
      } else if (menu === 'Pembayaran Hutang') {
        const { data, error: e } = await db.from('payable_payments').select('id,payable_id,amount,paid_at,reference,notes').order('paid_at', { ascending: false }).limit(500)
        if (e) throw e
        const ids = [...new Set((data || []).map(item => item.payable_id).filter(Boolean))]
        const payables = ids.length ? await db.from('payables').select('id,invoice_no,status,business_id,branch_id').in('id', ids) : { data: [], error: null }
        if (payables.error) throw payables.error
        const map = new Map((payables.data || []).filter(item => item.business_id === business.id && item.branch_id === branch.id).map(item => [String(item.id), item]))
        next = (data || []).filter(item => map.has(String(item.payable_id))).map(item => ({ ...item, invoice_no: map.get(String(item.payable_id))?.invoice_no, payable_status: map.get(String(item.payable_id))?.status }))
      } else if (menu === 'Kas') {
        const { data: shifts, error: se } = await db.from('cashier_shifts').select('id,terminal_name').eq('business_id', business.id).eq('branch_id', branch.id).limit(500)
        if (se) throw se
        const shiftIds = (shifts || []).map(item => item.id).filter(Boolean)
        if (shiftIds.length) {
          const { data, error: e } = await db.from('cash_movements').select('id,shift_id,movement_type,amount,reference_type,reference_id,reason,created_at').in('shift_id', shiftIds).order('created_at', { ascending: false }).limit(1000)
          if (e) throw e
          const map = new Map((shifts || []).map(item => [String(item.id), String(item.terminal_name || item.id)]))
          next = (data || []).map(item => ({ ...item, terminal_name: map.get(String(item.shift_id)) || 'Shift' }))
        }
      }
      setRows(next)
    } catch (e: any) {
      setError(e?.message || 'Gagal memuat data.')
      setRows([])
    } finally { setLoading(false) }
  }, [menu])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    if (menu !== 'Held Sales') return
    const refresh = () => { void load() }
    window.addEventListener('storage', refresh)
    window.addEventListener('qris-held-orders-changed', refresh)
    return () => { window.removeEventListener('storage', refresh); window.removeEventListener('qris-held-orders-changed', refresh) }
  }, [menu, load])

  const summary = useMemo(() => ({ count: rows.length, amount: rows.reduce((sum, row) => sum + Number(row.amount || row.total_amount || 0), 0) }), [rows])

  return <div className="module-page">
    <header className={styles.header}>
      <div><span className={styles.eyebrow}>{meta.eyebrow}</span><h1>{meta.title}</h1><p>{meta.description}</p></div>
      <div className={styles.context}><strong>{branchName}</strong><span>{summary.count} data</span>{summary.amount > 0 && <span>{money(summary.amount)}</span>}</div>
    </header>
    {error && <div className={styles.error} role="alert">{error}</div>}
    {loading ? <div className={styles.loading}>Memuat data…</div> : <Table menu={menu} rows={rows} />}
  </div>
}
