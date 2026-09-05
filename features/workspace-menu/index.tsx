'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import { getActiveWorkspace } from '../../lib/business-context'
import styles from './workspace-menu.module.css'

type Row = Record<string, any>
export type WorkspaceMenuId =
  | 'Pergerakan Stok' | 'Stok Opname' | 'Penyesuaian Stok' | 'Purchase Order' | 'Penerimaan Barang' | 'Hutang'
  | 'Riwayat Transaksi' | 'Produk Terlaris' | 'Laporan Penjualan' | 'Laporan Stok' | 'Laporan Pembelian'
  | 'Laporan Piutang' | 'Laporan Hutang' | 'Pengeluaran' | 'Laba & Margin' | 'Cabang / Outlet'
  | 'Karyawan & Akses' | 'Shift Kasir' | 'Metode Pembayaran' | 'Audit Log' | 'Bisnis' | 'Outlet'
  | 'Satuan' | 'Kategori' | 'Harga' | 'QRIS' | 'Pengaturan Sistem'

type Props = { menu: WorkspaceMenuId }

type MenuMeta = { title: string; description: string; icon: string; table?: string }

const config: Record<WorkspaceMenuId, MenuMeta> = {
  'Pergerakan Stok': { title: 'Pergerakan Stok', description: 'Audit seluruh masuk, keluar, transfer, opname, dan penyesuaian stok pada outlet aktif.', icon: '↕', table: 'stock_movements' },
  'Stok Opname': { title: 'Stok Opname', description: 'Catat hasil penghitungan fisik dan rekonsiliasi persediaan outlet.', icon: '▦', table: 'stock_adjustments' },
  'Penyesuaian Stok': { title: 'Penyesuaian Stok', description: 'Koreksi stok dengan alasan yang terdokumentasi dan jejak audit.', icon: '±', table: 'stock_adjustments' },
  'Purchase Order': { title: 'Purchase Order', description: 'Buat, konfirmasi, dan pantau pesanan pembelian ke supplier.', icon: 'PO', table: 'purchase_orders' },
  'Penerimaan Barang': { title: 'Penerimaan Barang', description: 'Terima barang berdasarkan PO, perbarui persediaan, dan bentuk hutang bila tempo.', icon: 'GR', table: 'goods_receipts' },
  'Hutang': { title: 'Hutang', description: 'Pantau kewajiban pembelian, jatuh tempo, pembayaran parsial, dan status lunas.', icon: 'Rp', table: 'payables' },
  'Riwayat Transaksi': { title: 'Riwayat Transaksi', description: 'Telusuri penjualan outlet aktif berdasarkan nomor transaksi, pelanggan, dan tanggal.', icon: '▤', table: 'sales' },
  'Produk Terlaris': { title: 'Produk Terlaris', description: 'Peringkat produk berdasarkan kuantitas dan nilai penjualan.', icon: '★', table: 'sale_items' },
  'Laporan Penjualan': { title: 'Laporan Penjualan', description: 'Ringkasan omzet, transaksi, diskon, HPP, dan margin outlet aktif.', icon: '▤', table: 'sales' },
  'Laporan Stok': { title: 'Laporan Stok', description: 'Nilai persediaan, stok kritis, stok negatif, dan ketersediaan outlet.', icon: '▥', table: 'stock_balances' },
  'Laporan Pembelian': { title: 'Laporan Pembelian', description: 'Analisis PO, penerimaan barang, pembelian, dan supplier.', icon: '⇩', table: 'goods_receipts' },
  'Laporan Piutang': { title: 'Laporan Piutang', description: 'Ringkasan piutang pelanggan, jatuh tempo, dan outstanding.', icon: 'AR', table: 'receivables' },
  'Laporan Hutang': { title: 'Laporan Hutang', description: 'Ringkasan hutang supplier dan kewajiban yang akan jatuh tempo.', icon: 'AP', table: 'payables' },
  'Pengeluaran': { title: 'Pengeluaran', description: 'Catat dan pantau biaya operasional outlet untuk laporan laba rugi.', icon: '−' },
  'Laba & Margin': { title: 'Laba & Margin', description: 'Pantau omzet, HPP, laba kotor, pengeluaran, dan margin.', icon: '↗', table: 'sales' },
  'Cabang / Outlet': { title: 'Cabang / Outlet', description: 'Kelola outlet yang tersedia dalam bisnis dan status operasionalnya.', icon: '⌂', table: 'branches' },
  'Karyawan & Akses': { title: 'Karyawan & Akses', description: 'Kelola anggota bisnis, peran, dan akses outlet.', icon: '♙', table: 'business_users' },
  'Shift Kasir': { title: 'Shift Kasir', description: 'Pantau pembukaan, penutupan, kas awal, dan kas akhir shift.', icon: '◷', table: 'cashier_shifts' },
  'Metode Pembayaran': { title: 'Metode Pembayaran', description: 'Atur metode pembayaran aktif untuk kasir outlet.', icon: '◉', table: 'payment_methods' },
  'Audit Log': { title: 'Audit Log', description: 'Telusuri aktivitas perubahan data dan tindakan penting pengguna.', icon: '⌁', table: 'audit_logs' },
  'Bisnis': { title: 'Bisnis', description: 'Identitas, informasi, dan konfigurasi dasar bisnis.', icon: 'B', table: 'businesses' },
  'Outlet': { title: 'Outlet', description: 'Konfigurasi outlet aktif, kode, dan status operasional.', icon: '⌂', table: 'branches' },
  'Satuan': { title: 'Satuan', description: 'Kelola satuan produk dan presisi kuantitas.', icon: 'U', table: 'units' },
  'Kategori': { title: 'Kategori', description: 'Kelola kategori produk dan struktur kategori.', icon: 'C', table: 'categories' },
  'Harga': { title: 'Harga', description: 'Kelola price list dan versi harga yang berlaku.', icon: 'Rp', table: 'product_prices' },
  'QRIS': { title: 'QRIS', description: 'Konfigurasi kanal QRIS dan parameter pembayaran.', icon: 'Q', table: 'qris_configurations' },
  'Pengaturan Sistem': { title: 'Pengaturan Sistem', description: 'Pengaturan aplikasi dan kontrol operasional tingkat sistem.', icon: '⚙' },
}

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(Math.round(n || 0))
const fmt = (n: number) => new Intl.NumberFormat('id-ID').format(Math.round(n || 0))

export default function WorkspaceMenuPage({ menu }: Props) {
  const meta = config[menu]
  const [businessId, setBusinessId] = useState('')
  const [branchId, setBranchId] = useState('')
  const [branchName, setBranchName] = useState('')
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { business, branch } = await getActiveWorkspace()
      setBusinessId(business.id)
      setBranchId(branch.id)
      setBranchName(String(branch.name || branch.code || 'Outlet aktif'))
      const db = requireSupabase()

      if (!meta.table) {
        setRows([])
        return
      }

      let query = db.from(meta.table).select('*').limit(100)
      const branchScoped = ['sales', 'purchase_orders', 'goods_receipts', 'receivables', 'payables', 'payments', 'stock_movements', 'stock_adjustments', 'cashier_shifts', 'audit_logs']
      const businessScoped = ['branches', 'products', 'units', 'categories', 'payment_methods', 'expenses', 'business_users', 'price_lists', 'businesses']

      if (branchScoped.includes(meta.table)) {
        query = query.eq('business_id', business.id).eq('branch_id', branch.id)
      } else if (businessScoped.includes(meta.table)) {
        query = query.eq('business_id', business.id)
      } else if (meta.table === 'qris_configurations') {
        query = query.eq('business_id', business.id).eq('branch_id', branch.id)
      } else if (meta.table === 'stock_balances') {
        const { data: locations, error: locationError } = await db.from('locations').select('id').eq('branch_id', branch.id).eq('is_active', true).order('name').limit(1)
        if (locationError) throw locationError
        const locationId = locations?.[0]?.id
        if (!locationId) {
          setRows([])
          return
        }
        query = query.eq('location_id', locationId)
      } else if (meta.table === 'product_prices') {
        const { data: priceLists, error: priceListError } = await db.from('price_lists').select('id').eq('business_id', business.id).eq('is_active', true)
        if (priceListError) throw priceListError
        const priceListIds = (priceLists || []).map(item => item.id).filter(Boolean)
        if (!priceListIds.length) {
          setRows([])
          return
        }
        query = query.in('price_list_id', priceListIds)
      }

      const { data, error: queryError } = await query
      if (queryError) throw queryError
      setRows(data || [])
    } catch (e: any) {
      setError(e?.message || 'Data belum dapat dimuat.')
    } finally {
      setLoading(false)
    }
  }, [meta.table])

  useEffect(() => { void load() }, [load])

  const stats = useMemo(() => {
    const total = rows.length
    const amount = rows.reduce((sum, row) => sum + Number(row.total_amount || row.total || row.outstanding_amount || 0), 0)
    const active = rows.filter(row => row.is_active !== false && !['VOID', 'CANCELLED'].includes(String(row.status || '').toUpperCase())).length
    return { total, amount, active }
  }, [rows])

  const columns = useMemo(() => {
    if (!rows.length) return []
    const preferred = ['sale_no', 'order_no', 'receipt_no', 'invoice_no', 'name', 'code', 'status', 'due_date', 'total_amount', 'outstanding_amount', 'qty_base', 'created_at']
    return preferred.filter(key => key in rows[0]).slice(0, 5)
  }, [rows])

  const isDataConnected = Boolean(meta.table)

  return <section className={styles.page}>
    <header className={styles.header}>
      <div className={styles.titleRow}><div className={styles.icon}>{meta.icon}</div><div><div className={styles.eyebrow}>{menu === 'Pengaturan Sistem' || ['Bisnis', 'Outlet', 'Satuan', 'Kategori', 'Harga', 'QRIS'].includes(menu) ? 'PENGATURAN' : 'MODUL OPERASIONAL'}</div><h1>{meta.title}</h1><p>{meta.description}</p></div></div>
      <button className={styles.refresh} onClick={() => void load()} disabled={loading}>↻ Perbarui</button>
    </header>

    <div className={styles.scope}><span>Outlet aktif</span><strong>{branchName || 'Memuat...'}</strong><span>·</span><span>Business</span><strong>{businessId ? 'Terhubung' : 'Memuat...'}</strong></div>

    {isDataConnected && <div className={styles.cards}>
      <article><span>Data termuat</span><strong>{fmt(stats.total)}</strong><small>Baris pada workspace aktif</small></article>
      <article><span>Aktif / terbuka</span><strong>{fmt(stats.active)}</strong><small>Belum nonaktif/void</small></article>
      <article><span>Nilai terukur</span><strong>{money(stats.amount)}</strong><small>Agregat dari data tersedia</small></article>
    </div>}

    {error && <div className={styles.alert}><strong>Data belum tersedia</strong><span>{error}</span></div>}

    <div className={styles.panel}>
      <div className={styles.panelHead}><div><h2>{meta.title}</h2><p>{isDataConnected ? 'Data dibatasi ke business dan outlet aktif sesuai struktur tabel.' : 'Modul ini belum memiliki tabel operasional khusus pada schema saat ini.'}</p></div><span className={styles.badge}>{loading ? 'Memuat' : isDataConnected ? `${fmt(rows.length)} data` : 'Belum terhubung'}</span></div>
      {loading ? <div className={styles.empty}>Memuat data workspace...</div> : !isDataConnected ? <div className={styles.empty}><strong>Belum ada workflow backend</strong><span>UI tidak membuat tabel atau data palsu. Modul akan dihubungkan setelah workflow bisnisnya tersedia.</span></div> : rows.length === 0 ? <div className={styles.empty}><strong>Belum ada data</strong><span>Modul sudah terhubung ke workspace aktif. Setelah data dibuat, daftar akan muncul di sini.</span></div> : <div className={styles.tableWrap}><table><thead><tr>{columns.map(col => <th key={col}>{col.replaceAll('_', ' ')}</th>)}</tr></thead><tbody>{rows.map((row, i) => <tr key={row.id || i}>{columns.map(col => <td key={col}>{col.includes('amount') || col.includes('total') ? money(Number(row[col] || 0)) : col === 'created_at' || col === 'due_date' ? String(row[col] || '').slice(0, 10) : String(row[col] ?? '-')}</td>)}</tr>)}</tbody></table></div>}
    </div>
  </section>
}
