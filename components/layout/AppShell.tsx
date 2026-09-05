'use client'

import { useEffect, useState } from 'react'
import SalesTerminal from '../../features/sales'
import DashboardPage from '../../features/dashboard'
import OrdersPage from '../../features/orders'
import ProductsPage from '../../features/products'
import InventoryPage from '../../features/inventory'
import CustomersPage from '../../features/customers'
import SuppliersPage from '../../features/suppliers'
import PurchasesPage from '../../features/purchases'
import ReceivablesPage from '../../features/receivables'
import ReportsPage from '../../features/reports'
import PaymentsPage from '../../features/payments'
import SettingsPage from '../../features/settings'
import WorkspaceMenuPage, { type WorkspaceMenuId } from '../../features/workspace-menu'
import { getAccessibleBranches, getActiveWorkspace, setStoredBranchId } from '../../lib/business-context'
import styles from '../../app/app-shell.module.css'

type MenuId = 'Dashboard' | 'Kasir' | 'Penjualan' | 'Pesanan' | 'Pembayaran' | 'Produk' | 'Stok' | 'Pergerakan Stok' | 'Stok Opname' | 'Penyesuaian Stok' | 'Purchase Order' | 'Penerimaan Barang' | 'Hutang' | 'Supplier' | 'Pelanggan' | 'Piutang' | 'Riwayat Transaksi' | 'Laporan Penjualan' | 'Produk Terlaris' | 'Laporan Stok' | 'Laporan Pembelian' | 'Laporan Piutang' | 'Laporan Hutang' | 'Pengeluaran' | 'Laba & Margin' | 'Cabang / Outlet' | 'Karyawan & Akses' | 'Shift Kasir' | 'Metode Pembayaran' | 'Audit Log' | 'Bisnis' | 'Outlet' | 'Satuan' | 'Kategori' | 'Harga' | 'QRIS' | 'Pengaturan Sistem'
type ViewMode = 'retail' | 'distributor' | 'fnb'
type Branch = Record<string, any>
type GroupId = 'TRANSAKSI' | 'INVENTORI' | 'PEMBELIAN' | 'PELANGGAN' | 'LAPORAN' | 'MANAJEMEN' | 'PENGATURAN'
type Menu = { id: MenuId; label: string; icon: string; group: GroupId; description: string }

const menuItems: Menu[] = [
  { id: 'Kasir', label: 'Kasir', icon: '▣', group: 'TRANSAKSI', description: 'Buat dan proses transaksi penjualan' },
  { id: 'Penjualan', label: 'Penjualan', icon: '▤', group: 'TRANSAKSI', description: 'Riwayat dan detail penjualan' },
  { id: 'Pesanan', label: 'Pesanan', icon: '◫', group: 'TRANSAKSI', description: 'Pantau status pesanan' },
  { id: 'Pembayaran', label: 'Pembayaran', icon: '◉', group: 'TRANSAKSI', description: 'Pantau pembayaran' },
  { id: 'Produk', label: 'Produk', icon: '□', group: 'INVENTORI', description: 'Kelola katalog produk' },
  { id: 'Stok', label: 'Stok', icon: '▥', group: 'INVENTORI', description: 'Pantau saldo stok' },
  { id: 'Pergerakan Stok', label: 'Pergerakan Stok', icon: '↕', group: 'INVENTORI', description: 'Riwayat pergerakan stok' },
  { id: 'Stok Opname', label: 'Stok Opname', icon: '▦', group: 'INVENTORI', description: 'Rekonsiliasi stok fisik' },
  { id: 'Penyesuaian Stok', label: 'Penyesuaian Stok', icon: '±', group: 'INVENTORI', description: 'Koreksi stok terdokumentasi' },
  { id: 'Purchase Order', label: 'Purchase Order', icon: 'PO', group: 'PEMBELIAN', description: 'Pesanan pembelian supplier' },
  { id: 'Penerimaan Barang', label: 'Penerimaan Barang', icon: 'GR', group: 'PEMBELIAN', description: 'Terima barang dan bentuk stok' },
  { id: 'Hutang', label: 'Hutang', icon: 'Rp', group: 'PEMBELIAN', description: 'Hutang supplier dan pembayaran' },
  { id: 'Supplier', label: 'Supplier', icon: '⇄', group: 'PEMBELIAN', description: 'Kelola pemasok' },
  { id: 'Pelanggan', label: 'Pelanggan', icon: '♙', group: 'PELANGGAN', description: 'Kelola pelanggan' },
  { id: 'Piutang', label: 'Piutang', icon: 'Rp', group: 'PELANGGAN', description: 'Pantau tagihan pelanggan' },
  { id: 'Riwayat Transaksi', label: 'Riwayat Transaksi', icon: '▤', group: 'PELANGGAN', description: 'Telusuri transaksi pelanggan' },
  { id: 'Laporan Penjualan', label: 'Laporan Penjualan', icon: '▤', group: 'LAPORAN', description: 'Analisis omzet dan transaksi' },
  { id: 'Produk Terlaris', label: 'Produk Terlaris', icon: '★', group: 'LAPORAN', description: 'Produk dengan penjualan tertinggi' },
  { id: 'Laporan Stok', label: 'Laporan Stok', icon: '▥', group: 'LAPORAN', description: 'Analisis persediaan' },
  { id: 'Laporan Pembelian', label: 'Laporan Pembelian', icon: '⇩', group: 'LAPORAN', description: 'Analisis pembelian' },
  { id: 'Laporan Piutang', label: 'Laporan Piutang', icon: 'AR', group: 'LAPORAN', description: 'Analisis piutang' },
  { id: 'Laporan Hutang', label: 'Laporan Hutang', icon: 'AP', group: 'LAPORAN', description: 'Analisis hutang' },
  { id: 'Pengeluaran', label: 'Pengeluaran', icon: '−', group: 'LAPORAN', description: 'Analisis biaya operasional' },
  { id: 'Laba & Margin', label: 'Laba & Margin', icon: '↗', group: 'LAPORAN', description: 'Analisis laba dan margin' },
  { id: 'Cabang / Outlet', label: 'Cabang / Outlet', icon: '⌂', group: 'MANAJEMEN', description: 'Kelola outlet dalam bisnis' },
  { id: 'Karyawan & Akses', label: 'Karyawan & Akses', icon: '♙', group: 'MANAJEMEN', description: 'Kelola anggota dan akses outlet' },
  { id: 'Shift Kasir', label: 'Shift Kasir', icon: '◷', group: 'MANAJEMEN', description: 'Kelola shift kasir' },
  { id: 'Metode Pembayaran', label: 'Metode Pembayaran', icon: '◉', group: 'MANAJEMEN', description: 'Kelola metode pembayaran' },
  { id: 'Audit Log', label: 'Audit Log', icon: '⌁', group: 'MANAJEMEN', description: 'Jejak aktivitas pengguna' },
  { id: 'Bisnis', label: 'Bisnis', icon: 'B', group: 'PENGATURAN', description: 'Identitas dan konfigurasi bisnis' },
  { id: 'Outlet', label: 'Outlet', icon: '⌂', group: 'PENGATURAN', description: 'Konfigurasi outlet' },
  { id: 'Satuan', label: 'Satuan', icon: 'U', group: 'PENGATURAN', description: 'Kelola satuan produk' },
  { id: 'Kategori', label: 'Kategori', icon: 'C', group: 'PENGATURAN', description: 'Kelola kategori produk' },
  { id: 'Harga', label: 'Harga', icon: 'Rp', group: 'PENGATURAN', description: 'Kelola price list dan harga berlaku' },
  { id: 'QRIS', label: 'QRIS', icon: 'Q', group: 'PENGATURAN', description: 'Konfigurasi kanal QRIS' },
  { id: 'Pengaturan Sistem', label: 'Pengaturan Sistem', icon: '⚙', group: 'PENGATURAN', description: 'Konfigurasi sistem aplikasi' },
]

const groups: { id: GroupId; items: Menu[] }[] = ['TRANSAKSI', 'INVENTORI', 'PEMBELIAN', 'PELANGGAN', 'LAPORAN', 'MANAJEMEN', 'PENGATURAN'].map(id => ({
  id,
  items: menuItems.filter(item => item.group === id),
}))

const dashboardMenu: Menu = { id: 'Dashboard', label: 'Dashboard', icon: '⌂', group: 'TRANSAKSI', description: 'Ringkasan bisnis' }
const mobileItems = [dashboardMenu, menuItems.find(item => item.id === 'Kasir'), menuItems.find(item => item.id === 'Stok'), menuItems.find(item => item.id === 'Laporan Penjualan')].filter((item): item is Menu => Boolean(item))

function renderFeature(active: MenuId, mode: ViewMode, version: number) {
  const key = `${version}-${mode}-${active}`
  switch (active) {
    case 'Dashboard': return <DashboardPage key={key} />
    case 'Kasir': return <SalesTerminal key={key} />
    case 'Penjualan':
    case 'Riwayat Transaksi': return <WorkspaceMenuPage key={key} menu="Riwayat Transaksi" />
    case 'Pesanan': return <OrdersPage key={key} />
    case 'Produk': return <ProductsPage key={key} />
    case 'Stok': return <InventoryPage key={key} />
    case 'Pelanggan': return <CustomersPage key={key} />
    case 'Supplier': return <SuppliersPage key={key} />
    case 'Pembayaran': return <PaymentsPage key={key} />
    case 'Purchase Order':
    case 'Penerimaan Barang': return <PurchasesPage key={key} />
    case 'Piutang': return <ReceivablesPage key={key} />
    case 'Laporan Penjualan': return <ReportsPage key={key} />
    case 'Pengaturan Sistem': return <SettingsPage key={key} />
    default: return <WorkspaceMenuPage key={key} menu={active as WorkspaceMenuId} />
  }
}

export default function AppShell() {
  const [active, setActive] = useState<MenuId>('Dashboard')
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [mode, setMode] = useState<ViewMode>('retail')
  const [businessName, setBusinessName] = useState('')
  const [branches, setBranches] = useState<Branch[]>([])
  const [branchId, setBranchId] = useState('')
  const [version, setVersion] = useState(0)
  const [open, setOpen] = useState<Record<GroupId, boolean>>({ TRANSAKSI: true, INVENTORI: true, PEMBELIAN: false, PELANGGAN: false, LAPORAN: false, MANAJEMEN: false, PENGATURAN: false })

  useEffect(() => {
    try {
      setCollapsed(localStorage.getItem('qris-sidebar-collapsed') === '1')
      const savedMode = localStorage.getItem('qris-view-mode')
      if (savedMode === 'retail' || savedMode === 'distributor' || savedMode === 'fnb') setMode(savedMode)
      const savedGroups = localStorage.getItem('qris-nav-groups')
      if (savedGroups) {
        const parsed = JSON.parse(savedGroups)
        if (parsed && typeof parsed === 'object') setOpen(current => ({ ...current, ...parsed }))
      }
    } catch {}

    let cancelled = false
    void (async () => {
      try {
        const workspace = await getActiveWorkspace()
        const accessible = await getAccessibleBranches(workspace.business.id)
        if (cancelled) return
        setBusinessName(String(workspace.business?.name || workspace.business?.business_name || 'Bisnis'))
        setBranches(accessible)
        setBranchId(workspace.branch.id)
      } catch {}
    })()

    const onMode = (event: Event) => {
      const next = (event as CustomEvent<ViewMode>).detail
      if (next === 'retail' || next === 'distributor' || next === 'fnb') setMode(next)
    }
    window.addEventListener('qris-mode-changed', onMode)
    return () => {
      cancelled = true
      window.removeEventListener('qris-mode-changed', onMode)
    }
  }, [])

  useEffect(() => {
    const item = menuItems.find(menu => menu.id === active)
    if (item) setOpen(current => current[item.group] ? current : { ...current, [item.group]: true })
  }, [active])

  const selectMenu = (id: MenuId) => {
    setActive(id)
    setMobileOpen(false)
  }

  const toggleGroup = (id: GroupId) => setOpen(current => {
    const next = { ...current, [id]: !current[id] }
    try { localStorage.setItem('qris-nav-groups', JSON.stringify(next)) } catch {}
    return next
  })

  const toggleSidebar = () => setCollapsed(current => {
    const next = !current
    try { localStorage.setItem('qris-sidebar-collapsed', next ? '1' : '0') } catch {}
    return next
  })

  const selectBranch = (id: string) => {
    const branch = branches.find(item => item.id === id)
    if (!branch || branch.id === branchId) return
    setStoredBranchId(branch.id)
    setBranchId(branch.id)
    setVersion(current => current + 1)
    window.dispatchEvent(new CustomEvent('qris-workspace-changed', { detail: { branchId: branch.id } }))
  }

  const activeBranch = branches.find(branch => branch.id === branchId)
  const activeBranchName = String(activeBranch?.name || activeBranch?.code || 'Outlet Aktif')

  return <div className={`${styles.shell} ${collapsed ? styles.sidebarIsCollapsed : ''}`}>
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandMark}>Q</div>
        <div className={styles.brandCopy}>
          <div className={styles.brandTitle}>QRIS POS</div>
          <div className={styles.brandSub}>{businessName || 'Toko / Business'}</div>
        </div>
        <button type="button" className={styles.collapseButton} onClick={toggleSidebar} aria-label={collapsed ? 'Tampilkan menu' : 'Sembunyikan menu'}>{collapsed ? '›' : '‹'}</button>
      </div>

      <div className={styles.workspaceCard}>
        <div className={styles.workspaceIcon}>●</div>
        <div className={styles.workspaceCopy}>
          <span>OUTLET AKTIF</span>
          <strong>{activeBranchName}</strong>
        </div>
        <select aria-label="Pilih outlet aktif" value={branchId} onChange={event => selectBranch(event.target.value)} disabled={branches.length <= 1}>
          {branches.length ? branches.map(branch => <option key={branch.id} value={branch.id}>{String(branch.name || branch.code || branch.id)}</option>) : <option value="">Tidak ada outlet</option>}
        </select>
      </div>

      <nav className={styles.nav} aria-label="Navigasi utama">
        <button type="button" className={`${styles.navButton} ${active === 'Dashboard' ? styles.navActive : ''}`} onClick={() => selectMenu('Dashboard')}>
          <span className={styles.icon}>⌂</span><span className={styles.navText}>Dashboard</span>{active === 'Dashboard' && <span className={styles.activeMark} />}
        </button>
        {groups.map(group => {
          const expanded = Boolean(open[group.id])
          const activeGroup = group.items.some(item => item.id === active)
          return <div className={styles.navGroup} key={group.id}>
            <button type="button" className={`${styles.navGroupButton} ${activeGroup ? styles.navGroupButtonActive : ''}`} onClick={() => toggleGroup(group.id)} aria-expanded={expanded}>
              <span className={styles.navLabel}>{group.id}</span><span className={styles.navGroupChevron}>{expanded ? '⌄' : '›'}</span>
            </button>
            {expanded && <div className={styles.navGroupChildren}>
              {group.items.map(item => <button type="button" key={item.id} className={`${styles.navButton} ${active === item.id ? styles.navActive : ''}`} onClick={() => selectMenu(item.id)} title={collapsed ? item.description : undefined}>
                <span className={styles.icon}>{item.icon}</span><span className={styles.navText}>{item.label}</span>{active === item.id && <span className={styles.activeMark} />}
              </button>)}
            </div>}
          </div>
        })}
      </nav>

      <div className={styles.sidebarFooter}>
        <div className={styles.userAvatar}>Q</div>
        <div className={styles.userMeta}><strong>Workspace</strong><span>Outlet aktif</span></div>
        <button type="button" className={styles.moreButton} aria-label="Menu pengguna">•••</button>
      </div>
    </aside>

    <main className={styles.main}>{renderFeature(active, mode, version)}</main>

    {mobileOpen && <div className={styles.mobileMenuBackdrop} role="presentation" onClick={() => setMobileOpen(false)}>
      <section className={styles.mobileMenu} role="dialog" aria-modal="true" aria-label="Menu utama" onClick={event => event.stopPropagation()}>
        <div className={styles.mobileMenuHead}><div><strong>QRIS POS</strong><span>{activeBranchName}</span></div><button type="button" onClick={() => setMobileOpen(false)} aria-label="Tutup">×</button></div>
        <div className={styles.mobileMenuGroupList}>
          <section><h3>DASHBOARD</h3><div className={styles.mobileMenuGrid}><button type="button" className={active === 'Dashboard' ? styles.mobileMenuActive : ''} onClick={() => selectMenu('Dashboard')}><span>⌂</span><strong>Dashboard</strong><small>Ringkasan bisnis</small></button></div></section>
          {groups.map(group => <section key={group.id}><h3>{group.id}</h3><div className={styles.mobileMenuGrid}>{group.items.map(item => <button type="button" key={item.id} className={active === item.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(item.id)}><span>{item.icon}</span><strong>{item.label}</strong><small>{item.description}</small></button>)}</div></section>)}
        </div>
      </section>
    </div>}

    <div className={styles.mobileBar}>
      {mobileItems.map(item => <button type="button" key={item.id} className={`${styles.mobileButton} ${active === item.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(item.id)}><span className={styles.mobileIcon}>{item.icon}</span><span>{item.label}</span></button>)}
      <button type="button" className={styles.mobileButton} onClick={() => setMobileOpen(true)}><span className={styles.mobileIcon}>☰</span><span>Lainnya</span></button>
    </div>
  </div>
}
