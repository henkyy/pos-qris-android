'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import { getActiveWorkspace } from '../../lib/business-context'
import styles from './workspace-menu.module.css'

type Row = Record<string, unknown>
export type WorkspaceMenuId =
  | 'Pergerakan Stok' | 'Stok Opname' | 'Penyesuaian Stok' | 'Purchase Order' | 'Penerimaan Barang' | 'Hutang'
  | 'Riwayat Transaksi' | 'Produk Terlaris' | 'Laporan Penjualan' | 'Laporan Stok' | 'Laporan Pembelian'
  | 'Laporan Piutang' | 'Laporan Hutang' | 'Pengeluaran' | 'Laba & Margin' | 'Cabang / Outlet'
  | 'Karyawan & Akses' | 'Shift Kasir' | 'Metode Pembayaran' | 'Audit Log' | 'Bisnis' | 'Outlet'
  | 'Satuan' | 'Kategori' | 'Harga' | 'QRIS' | 'Pengaturan Sistem'

type Props = { menu: WorkspaceMenuId }
type Meta = { title: string; description: string; icon: string; eyebrow: string }

const META: Record<WorkspaceMenuId, Meta> = {
  'Pergerakan Stok': { title: 'Pergerakan Stok', description: 'Audit stok masuk, keluar, transfer, dan koreksi pada outlet aktif.', icon: '↕', eyebrow: 'INVENTORI' },
  'Stok Opname': { title: 'Stok Opname', description: 'Dokumen rekonsiliasi hasil hitung fisik terhadap saldo sistem.', icon: '▦', eyebrow: 'INVENTORI' },
  'Penyesuaian Stok': { title: 'Penyesuaian Stok', description: 'Koreksi persediaan yang selalu meninggalkan jejak alasan dan pergerakan.', icon: '±', eyebrow: 'INVENTORI' },
  'Purchase Order': { title: 'Purchase Order', description: 'Pesanan pembelian supplier dari draft sampai konfirmasi.', icon: 'PO', eyebrow: 'PEMBELIAN' },
  'Penerimaan Barang': { title: 'Penerimaan Barang', description: 'Penerimaan barang yang memperbarui stok dan dapat membentuk hutang.', icon: 'GR', eyebrow: 'PEMBELIAN' },
  'Hutang': { title: 'Hutang', description: 'Kewajiban supplier, jatuh tempo, pembayaran parsial, dan pelunasan.', icon: 'Rp', eyebrow: 'KEUANGAN' },
  'Riwayat Transaksi': { title: 'Riwayat Transaksi', description: 'Riwayat penjualan outlet aktif dengan status dan nilai transaksi.', icon: '▤', eyebrow: 'TRANSAKSI' },
  'Produk Terlaris': { title: 'Produk Terlaris', description: 'Peringkat produk berdasarkan kuantitas dan nilai penjualan yang valid.', icon: '★', eyebrow: 'LAPORAN' },
  'Laporan Penjualan': { title: 'Laporan Penjualan', description: 'Omzet, transaksi, diskon, HPP, dan laba kotor outlet aktif.', icon: '▤', eyebrow: 'LAPORAN' },
  'Laporan Stok': { title: 'Laporan Stok', description: 'Saldo stok per lokasi, nilai persediaan, stok kritis, dan negatif.', icon: '▥', eyebrow: 'LAPORAN' },
  'Laporan Pembelian': { title: 'Laporan Pembelian', description: 'Ringkasan penerimaan barang dan nilai pembelian supplier.', icon: '⇩', eyebrow: 'LAPORAN' },
  'Laporan Piutang': { title: 'Laporan Piutang', description: 'Outstanding pelanggan, jatuh tempo, pembayaran, dan status.', icon: 'AR', eyebrow: 'LAPORAN' },
  'Laporan Hutang': { title: 'Laporan Hutang', description: 'Outstanding supplier, jatuh tempo, pembayaran, dan status.', icon: 'AP', eyebrow: 'LAPORAN' },
  'Pengeluaran': { title: 'Pengeluaran', description: 'Biaya operasional outlet yang menjadi komponen laporan laba rugi.', icon: '−', eyebrow: 'KEUANGAN' },
  'Laba & Margin': { title: 'Laba & Margin', description: 'Laba kotor dihitung dari penjualan dikurangi HPP transaksi.', icon: '↗', eyebrow: 'LAPORAN' },
  'Cabang / Outlet': { title: 'Cabang / Outlet', description: 'Daftar outlet bisnis dan status operasionalnya.', icon: '⌂', eyebrow: 'MANAJEMEN' },
  'Karyawan & Akses': { title: 'Karyawan & Akses', description: 'Keanggotaan bisnis, peran, dan akses outlet.', icon: '♙', eyebrow: 'MANAJEMEN' },
  'Shift Kasir': { title: 'Shift Kasir', description: 'Pembukaan, penutupan, kas awal, kas akhir, dan selisih.', icon: '◷', eyebrow: 'MANAJEMEN' },
  'Metode Pembayaran': { title: 'Metode Pembayaran', description: 'Metode pembayaran yang tersedia untuk proses kasir.', icon: '◉', eyebrow: 'MANAJEMEN' },
  'Audit Log': { title: 'Audit Log', description: 'Jejak perubahan dan tindakan penting pengguna.', icon: '⌁', eyebrow: 'MANAJEMEN' },
  'Bisnis': { title: 'Bisnis', description: 'Identitas dan konfigurasi dasar bisnis aktif.', icon: 'B', eyebrow: 'PENGATURAN' },
  'Outlet': { title: 'Outlet', description: 'Konfigurasi outlet aktif dan status operasional.', icon: '⌂', eyebrow: 'PENGATURAN' },
  'Satuan': { title: 'Satuan', description: 'Satuan yang digunakan produk beserta presisi kuantitasnya.', icon: 'U', eyebrow: 'PENGATURAN' },
  'Kategori': { title: 'Kategori', description: 'Pengelompokan produk untuk operasional dan laporan.', icon: 'C', eyebrow: 'PENGATURAN' },
  'Harga': { title: 'Harga', description: 'Price list dan versi harga yang sedang berlaku.', icon: 'Rp', eyebrow: 'PENGATURAN' },
  'QRIS': { title: 'QRIS', description: 'Konfigurasi QRIS untuk outlet aktif.', icon: 'Q', eyebrow: 'PENGATURAN' },
  'Pengaturan Sistem': { title: 'Pengaturan Sistem', description: 'Kontrol sistem aplikasi dan reset workspace.', icon: '⚙', eyebrow: 'PENGATURAN' },
}

const money = (value: unknown) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(Math.round(Number(value) || 0))
const number = (value: unknown) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 2 }).format(Number(value) || 0)
const date = (value: unknown) => value ? new Intl.DateTimeFormat('id-ID', { dateStyle: 'medium' }).format(new Date(String(value))) : '-'
const text = (value: unknown) => String(value ?? '-').replaceAll('_', ' ')
const status = (value: unknown) => String(value ?? '-').replaceAll('_', ' ')

function Table({ columns, rows }: { columns: { key: string; label: string; money?: boolean; qty?: boolean; date?: boolean; status?: boolean }[]; rows: Row[] }) {
  return <div className={styles.tableWrap}><table><thead><tr>{columns.map(column => <th key={column.key}>{column.label}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={String(row.id ?? index)}>{columns.map(column => { const value = row[column.key]; return <td key={column.key} className={column.money || column.qty ? styles.numeric : ''}>{column.money ? money(value) : column.qty ? number(value) : column.date ? date(value) : column.status ? <span className={styles.status}>{status(value)}</span> : text(value)}</td> })}</tr>)}</tbody></table></div>
}

export default function WorkspaceMenuPage({ menu }: Props) {
  const meta = META[menu]
  const [businessId, setBusinessId] = useState('')
  const [branchId, setBranchId] = useState('')
  const [branchName, setBranchName] = useState('')
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const workspace = await getActiveWorkspace(); const business = workspace.business; const branch = workspace.branch
      setBusinessId(business.id); setBranchId(branch.id); setBranchName(String(branch.name || branch.code || 'Outlet aktif'))
      const db = requireSupabase()
      let next: Row[] = []

      if (menu === 'Produk Terlaris') {
        const { data: sales, error: salesError } = await db.from('sales').select('id').eq('business_id', business.id).eq('branch_id', branch.id).neq('status', 'VOID').neq('status', 'CANCELLED').limit(2000); if (salesError) throw salesError
        const ids = (sales || []).map(item => item.id).filter(Boolean); if (ids.length) { const { data, error: itemError } = await db.from('sale_items').select('product_id,product_name_snapshot,qty,line_total,hpp_total').in('sale_id', ids).limit(5000); if (itemError) throw itemError; const grouped = new Map<string, Row>(); for (const item of data || []) { const key = String(item.product_id); const current = grouped.get(key) || { product_id: key, product_name: item.product_name_snapshot, qty: 0, sales_amount: 0, hpp_amount: 0 }; current.qty = Number(current.qty) + Number(item.qty || 0); current.sales_amount = Number(current.sales_amount) + Number(item.line_total || 0); current.hpp_amount = Number(current.hpp_amount) + Number(item.hpp_total || 0); grouped.set(key, current) } next = [...grouped.values()].sort((a,b) => Number(b.qty)-Number(a.qty)).slice(0,100) }
      } else if (menu === 'Laporan Penjualan' || menu === 'Laba & Margin' || menu === 'Riwayat Transaksi') {
        const { data, error: e } = await db.from('sales').select('id,sale_no,sale_date,status,customer_id,subtotal,discount_amount,tax_amount,total_amount,paid_amount,hpp_amount,margin_amount').eq('business_id', business.id).eq('branch_id', branch.id).order('sale_date', { ascending: false }).limit(1000); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Laporan Stok') {
        const { data: locations, error: le } = await db.from('locations').select('id,name').eq('branch_id', branch.id).eq('is_active', true).order('name').limit(20); if (le) throw le; const locationIds = (locations || []).map(item => item.id); if (locationIds.length) { const { data, error: e } = await db.from('stock_balances').select('location_id,product_id,qty_base,reserved_qty,updated_at').in('location_id', locationIds).limit(3000); if (e) throw e; const locationMap = new Map((locations || []).map(item => [String(item.id), String(item.name)])); next = (data || []).map(item => ({ ...item, location_name: locationMap.get(String(item.location_id)) || '-' })) as Row[] }
      } else if (menu === 'Pergerakan Stok') {
        const { data, error: e } = await db.from('stock_movements').select('id,product_id,movement_type,qty_base,unit_cost,batch_no,expiry_date,reference_type,reason,created_at').eq('business_id', business.id).eq('branch_id', branch.id).order('created_at', { ascending: false }).limit(1000); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Stok Opname' || menu === 'Penyesuaian Stok') {
        const { data, error: e } = await db.from('stock_adjustments').select('id,adjustment_no,adjustment_date,status,reason,created_by,approved_by').eq('business_id', business.id).eq('branch_id', branch.id).order('adjustment_date', { ascending: false }).limit(500); if (e) throw e; next = ((data || []) as Row[]).filter(row => menu === 'Penyesuaian Stok' || /opname|stock take|physical/i.test(String(row.reason || '')))
      } else if (menu === 'Purchase Order') {
        const { data, error: e } = await db.from('purchase_orders').select('id,order_no,order_date,expected_date,status,supplier_id,subtotal,discount_amount,tax_amount,total_amount').eq('business_id', business.id).eq('branch_id', branch.id).order('order_date', { ascending: false }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Penerimaan Barang' || menu === 'Laporan Pembelian') {
        const { data, error: e } = await db.from('goods_receipts').select('id,receipt_no,receipt_date,supplier_id,purchase_order_id,total_amount,status,due_date').eq('business_id', business.id).eq('branch_id', branch.id).order('receipt_date', { ascending: false }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Hutang' || menu === 'Laporan Hutang') {
        const { data, error: e } = await db.from('payables').select('id,invoice_no,invoice_date,due_date,original_amount,paid_amount,outstanding_amount,status,supplier_id').eq('business_id', business.id).eq('branch_id', branch.id).order('due_date', { ascending: true }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Laporan Piutang') {
        const { data, error: e } = await db.from('receivables').select('id,invoice_no,invoice_date,due_date,original_amount,paid_amount,outstanding_amount,status,customer_id').eq('business_id', business.id).eq('branch_id', branch.id).order('due_date', { ascending: true }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Pengeluaran') {
        const { data, error: e } = await db.from('expenses').select('id,expense_no,expense_date,category,description,amount,status').eq('business_id', business.id).eq('branch_id', branch.id).order('expense_date', { ascending: false }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Cabang / Outlet' || menu === 'Outlet') {
        const { data, error: e } = await db.from('branches').select('id,code,name,is_active').eq('business_id', business.id).order('name').limit(100); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Karyawan & Akses') {
        const { data, error: e } = await db.from('business_users').select('id,user_id,role_id,is_active,created_at').eq('business_id', business.id).order('created_at').limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Shift Kasir') {
        const { data, error: e } = await db.from('cashier_shifts').select('id,terminal_name,opened_at,opening_cash,closed_at,expected_cash,actual_cash,variance,status,cashier_id').eq('business_id', business.id).eq('branch_id', branch.id).order('opened_at', { ascending: false }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Metode Pembayaran') {
        const { data, error: e } = await db.from('payment_methods').select('id,code,name,method_type,is_active').eq('business_id', business.id).order('name').limit(100); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Audit Log') {
        const { data, error: e } = await db.from('audit_logs').select('id,action,entity_type,entity_id,reason,actor_user_id,created_at').eq('business_id', business.id).eq('branch_id', branch.id).order('created_at', { ascending: false }).limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Bisnis') {
        const { data, error: e } = await db.from('businesses').select('id,name').eq('id', business.id).limit(1); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Satuan') {
        const { data, error: e } = await db.from('units').select('id,name,code,symbol,decimal_places,is_active').eq('business_id', business.id).order('name').limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Kategori') {
        const { data, error: e } = await db.from('categories').select('id,name,code,is_active,parent_id').eq('business_id', business.id).order('name').limit(500); if (e) throw e; next = (data || []) as Row[]
      } else if (menu === 'Harga') {
        const { data: lists, error: le } = await db.from('price_lists').select('id,name,code,is_default').eq('business_id', business.id).eq('is_active', true).order('name'); if (le) throw le; const ids = (lists || []).map(item => item.id); if (ids.length) { const { data, error: e } = await db.from('product_prices').select('id,price_list_id,product_id,unit_id,min_qty,price,discount_percent,valid_from,valid_until').in('price_list_id', ids).or('valid_until.is.null,valid_until.gte.now()').order('valid_from', { ascending: false }).limit(1000); if (e) throw e; const listMap = new Map((lists || []).map(item => [String(item.id), String(item.name)])); next = (data || []).map(item => ({ ...item, price_list_name: listMap.get(String(item.price_list_id)) || '-' })) as Row[] }
      } else if (menu === 'QRIS') {
        const { data, error: e } = await db.from('qris_configurations').select('id,provider,mode,display_name,merchant_name,merchant_identifier,is_active,nmid,outlet_code,is_static').eq('business_id', business.id).eq('branch_id', branch.id).order('created_at', { ascending: false }).limit(50); if (e) throw e; next = (data || []) as Row[]
      }
      setRows(next)
    } catch (e: unknown) { setError(e instanceof Error ? e.message : 'Data belum dapat dimuat.') } finally { setLoading(false) }
  }, [menu])

  useEffect(() => { void load(); const handler = () => void load(); window.addEventListener('qris-workspace-changed', handler); window.addEventListener('qris-data-reset', handler); return () => { window.removeEventListener('qris-workspace-changed', handler); window.removeEventListener('qris-data-reset', handler) } }, [load])

  const stats = useMemo(() => {
    const total = rows.length
    const amount = rows.reduce((sum, row) => sum + Number(row.total_amount ?? row.outstanding_amount ?? row.amount ?? row.sales_amount ?? 0), 0)
    const active = rows.filter(row => row.is_active !== false && !['VOID', 'CANCELLED'].includes(String(row.status || '').toUpperCase())).length
    return { total, amount, active }
  }, [rows])

  const columns = useMemo(() => {
    const cols: Record<WorkspaceMenuId, { key: string; label: string; money?: boolean; qty?: boolean; date?: boolean; status?: boolean }[]> = {
      'Pergerakan Stok': [{key:'movement_type',label:'Jenis'},{key:'product_id',label:'Produk'},{key:'qty_base',label:'Qty',qty:true},{key:'unit_cost',label:'HPP',money:true},{key:'created_at',label:'Waktu',date:true}],
      'Stok Opname': [{key:'adjustment_no',label:'No. Dokumen'},{key:'adjustment_date',label:'Tanggal',date:true},{key:'status',label:'Status',status:true},{key:'reason',label:'Alasan'},{key:'created_by',label:'Dibuat Oleh'}],
      'Penyesuaian Stok': [{key:'adjustment_no',label:'No. Dokumen'},{key:'adjustment_date',label:'Tanggal',date:true},{key:'status',label:'Status',status:true},{key:'reason',label:'Alasan'},{key:'created_by',label:'Dibuat Oleh'}],
      'Purchase Order': [{key:'order_no',label:'No. PO'},{key:'order_date',label:'Tanggal',date:true},{key:'expected_date',label:'Estimasi',date:true},{key:'status',label:'Status',status:true},{key:'total_amount',label:'Total',money:true}],
      'Penerimaan Barang': [{key:'receipt_no',label:'No. Penerimaan'},{key:'receipt_date',label:'Tanggal',date:true},{key:'supplier_id',label:'Supplier'},{key:'status',label:'Status',status:true},{key:'total_amount',label:'Total',money:true}],
      'Hutang': [{key:'invoice_no',label:'Invoice'},{key:'invoice_date',label:'Tanggal',date:true},{key:'due_date',label:'Jatuh Tempo',date:true},{key:'outstanding_amount',label:'Sisa',money:true},{key:'status',label:'Status',status:true}],
      'Riwayat Transaksi': [{key:'sale_no',label:'No. Transaksi'},{key:'sale_date',label:'Tanggal',date:true},{key:'status',label:'Status',status:true},{key:'total_amount',label:'Total',money:true},{key:'paid_amount',label:'Dibayar',money:true}],
      'Produk Terlaris': [{key:'product_name',label:'Produk'},{key:'qty',label:'Terjual',qty:true},{key:'sales_amount',label:'Penjualan',money:true},{key:'hpp_amount',label:'HPP',money:true}],
      'Laporan Penjualan': [{key:'sale_no',label:'No. Transaksi'},{key:'sale_date',label:'Tanggal',date:true},{key:'total_amount',label:'Omzet',money:true},{key:'hpp_amount',label:'HPP',money:true},{key:'margin_amount',label:'Laba Kotor',money:true}],
      'Laporan Stok': [{key:'location_name',label:'Lokasi'},{key:'product_id',label:'Produk'},{key:'qty_base',label:'Stok',qty:true},{key:'reserved_qty',label:'Reserved',qty:true},{key:'updated_at',label:'Diperbarui',date:true}],
      'Laporan Pembelian': [{key:'receipt_no',label:'No. Penerimaan'},{key:'receipt_date',label:'Tanggal',date:true},{key:'supplier_id',label:'Supplier'},{key:'total_amount',label:'Total',money:true},{key:'status',label:'Status',status:true}],
      'Laporan Piutang': [{key:'invoice_no',label:'Invoice'},{key:'invoice_date',label:'Tanggal',date:true},{key:'due_date',label:'Jatuh Tempo',date:true},{key:'outstanding_amount',label:'Sisa',money:true},{key:'status',label:'Status',status:true}],
      'Laporan Hutang': [{key:'invoice_no',label:'Invoice'},{key:'invoice_date',label:'Tanggal',date:true},{key:'due_date',label:'Jatuh Tempo',date:true},{key:'outstanding_amount',label:'Sisa',money:true},{key:'status',label:'Status',status:true}],
      'Pengeluaran': [{key:'expense_no',label:'No. Pengeluaran'},{key:'expense_date',label:'Tanggal',date:true},{key:'category',label:'Kategori'},{key:'description',label:'Keterangan'},{key:'amount',label:'Jumlah',money:true}],
      'Laba & Margin': [{key:'sale_no',label:'No. Transaksi'},{key:'sale_date',label:'Tanggal',date:true},{key:'total_amount',label:'Omzet',money:true},{key:'hpp_amount',label:'HPP',money:true},{key:'margin_amount',label:'Laba Kotor',money:true}],
      'Cabang / Outlet': [{key:'code',label:'Kode'},{key:'name',label:'Nama Outlet'},{key:'is_active',label:'Status'}],
      'Karyawan & Akses': [{key:'user_id',label:'User'},{key:'role_id',label:'Role'},{key:'is_active',label:'Status'},{key:'created_at',label:'Bergabung',date:true}],
      'Shift Kasir': [{key:'terminal_name',label:'Terminal'},{key:'opened_at',label:'Buka',date:true},{key:'opening_cash',label:'Kas Awal',money:true},{key:'actual_cash',label:'Kas Aktual',money:true},{key:'variance',label:'Selisih',money:true}],
      'Metode Pembayaran': [{key:'code',label:'Kode'},{key:'name',label:'Nama'},{key:'method_type',label:'Tipe'},{key:'is_active',label:'Status'}],
      'Audit Log': [{key:'created_at',label:'Waktu',date:true},{key:'action',label:'Aksi'},{key:'entity_type',label:'Entitas'},{key:'entity_id',label:'ID'},{key:'reason',label:'Alasan'}],
      'Bisnis': [{key:'name',label:'Nama Bisnis'},{key:'id',label:'ID'}],
      'Outlet': [{key:'code',label:'Kode'},{key:'name',label:'Nama Outlet'},{key:'is_active',label:'Status'}],
      'Satuan': [{key:'name',label:'Nama'},{key:'code',label:'Kode'},{key:'symbol',label:'Simbol'},{key:'decimal_places',label:'Desimal',qty:true},{key:'is_active',label:'Status'}],
      'Kategori': [{key:'name',label:'Nama'},{key:'code',label:'Kode'},{key:'parent_id',label:'Parent'},{key:'is_active',label:'Status'}],
      'Harga': [{key:'price_list_name',label:'Price List'},{key:'product_id',label:'Produk'},{key:'unit_id',label:'Satuan'},{key:'min_qty',label:'Min. Qty',qty:true},{key:'price',label:'Harga',money:true}],
      'QRIS': [{key:'display_name',label:'Konfigurasi'},{key:'provider',label:'Provider'},{key:'mode',label:'Mode'},{key:'nmid',label:'NMID'},{key:'is_active',label:'Status'}],
      'Pengaturan Sistem': [],
    }
    return cols[menu]
  }, [menu])

  return <section className={styles.page}>
    <header className={styles.header}><div className={styles.titleRow}><div className={styles.icon}>{meta.icon}</div><div><div className={styles.eyebrow}>{meta.eyebrow}</div><h1>{meta.title}</h1><p>{meta.description}</p></div></div><button className={styles.refresh} onClick={() => void load()} disabled={loading}>{loading ? 'Memuat…' : '↻ Perbarui'}</button></header>
    <div className={styles.scope}><span>Outlet</span><strong>{branchName || 'Memuat…'}</strong><span>•</span><span>Business</span><strong>{businessId ? 'Terhubung' : 'Memuat…'}</strong></div>
    <div className={styles.cards}><article><span>Data</span><strong>{number(stats.total)}</strong><small>Baris pada workspace aktif</small></article><article><span>Aktif / terbuka</span><strong>{number(stats.active)}</strong><small>Status operasional</small></article><article><span>Nilai</span><strong>{money(stats.amount)}</strong><small>Agregat data yang dimuat</small></article></div>
    {error && <div className={styles.alert}><strong>Gagal memuat data</strong><span>{error}</span></div>}
    <div className={styles.panel}><div className={styles.panelHead}><div><h2>{meta.title}</h2><p>Read-only untuk ledger dan histori. Perubahan transaksi dilakukan melalui workflow masing-masing.</p></div><span className={styles.badge}>{loading ? 'Memuat' : `${number(rows.length)} data`}</span></div>{loading ? <div className={styles.empty}>Memuat workspace…</div> : rows.length === 0 ? <div className={styles.empty}><strong>Belum ada data</strong><span>Tidak ada data yang cocok dengan outlet aktif.</span></div> : <Table columns={columns} rows={rows} />}</div>
  </section>
}
