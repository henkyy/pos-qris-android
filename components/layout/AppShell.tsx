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
import { getAccessibleBranches, getActiveWorkspace, setStoredBranchId } from '../../lib/business-context'
import styles from '../../app/app-shell.module.css'

type MenuId = 'Dashboard' | 'Penjualan' | 'Pesanan' | 'Produk' | 'Stok' | 'Pelanggan' | 'Supplier' | 'Pembelian' | 'Piutang' | 'Laporan' | 'Pembayaran' | 'Pengaturan'
type ViewMode = 'retail' | 'distributor' | 'fnb'
type Menu = { id: MenuId; icon: string; group: string; desc: string }
type Branch = Record<string, any>

const menus: Menu[] = [
  { id: 'Dashboard', icon: '⌂', group: 'Utama', desc: 'Ringkasan bisnis' },
  { id: 'Penjualan', icon: '▣', group: 'Transaksi', desc: 'Kasir & pembayaran' },
  { id: 'Pesanan', icon: '▤', group: 'Transaksi', desc: 'Pesanan berjalan' },
  { id: 'Pembelian', icon: '⇩', group: 'Transaksi', desc: 'Pembelian barang' },
  { id: 'Piutang', icon: 'Rp', group: 'Transaksi', desc: 'Tagihan pelanggan' },
  { id: 'Produk', icon: '□', group: 'Master', desc: 'Produk & harga' },
  { id: 'Stok', icon: '▥', group: 'Master', desc: 'Persediaan barang' },
  { id: 'Pelanggan', icon: '♙', group: 'Master', desc: 'Data pelanggan' },
  { id: 'Supplier', icon: '⇄', group: 'Master', desc: 'Data supplier' },
  { id: 'Laporan', icon: '▥', group: 'Analitik', desc: 'Penjualan & bisnis' },
  { id: 'Pembayaran', icon: '◉', group: 'Analitik', desc: 'Metode & transaksi' },
  { id: 'Pengaturan', icon: '⚙', group: 'Sistem', desc: 'Konfigurasi aplikasi' },
]
const mobileMenus = menus.filter(x => ['Dashboard', 'Penjualan', 'Laporan'].includes(x.id))

function renderFeature(active: MenuId, mode: ViewMode, workspaceVersion: number) {
  const key = `${workspaceVersion}-${mode}`
  switch (active) {
    case 'Dashboard': return <DashboardPage key={key} />
    case 'Penjualan': return <SalesTerminal key={key} />
    case 'Pesanan': return <OrdersPage key={key} />
    case 'Produk': return <ProductsPage key={key} />
    case 'Stok': return <InventoryPage key={key} />
    case 'Pelanggan': return <CustomersPage key={key} />
    case 'Supplier': return <SuppliersPage key={key} />
    case 'Pembelian': return <PurchasesPage key={key} />
    case 'Piutang': return <ReceivablesPage key={key} />
    case 'Laporan': return <ReportsPage key={key} />
    case 'Pembayaran': return <PaymentsPage key={key} />
    case 'Pengaturan': return <SettingsPage key={key} />
  }
}

export default function AppShell() {
  const [active, setActive] = useState<MenuId>('Penjualan')
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [salesMode, setSalesMode] = useState<ViewMode>('retail')
  const [businessName, setBusinessName] = useState('')
  const [branches, setBranches] = useState<Branch[]>([])
  const [activeBranchId, setActiveBranchId] = useState('')
  const [workspaceVersion, setWorkspaceVersion] = useState(0)

  useEffect(() => {
    try {
      setSidebarCollapsed(localStorage.getItem('qris-sidebar-collapsed') === '1')
      const saved = localStorage.getItem('qris-view-mode') as ViewMode | null
      if (saved === 'retail' || saved === 'distributor' || saved === 'fnb') setSalesMode(saved)
    } catch {}

    const loadWorkspace = async () => {
      try {
        const workspace = await getActiveWorkspace()
        setBusinessName(String(workspace.business?.name || workspace.business?.business_name || ''))
        const list = await getAccessibleBranches(workspace.business.id)
        setBranches(list)
        setActiveBranchId(workspace.branch.id)
      } catch {}
    }

    void loadWorkspace()

    const onModeChanged = (event: Event) => {
      const next = (event as CustomEvent<ViewMode>).detail
      if (['retail', 'distributor', 'fnb'].includes(next)) setSalesMode(next)
    }
    window.addEventListener('qris-mode-changed', onModeChanged)
    return () => window.removeEventListener('qris-mode-changed', onModeChanged)
  }, [])

  function toggleSidebar() {
    setSidebarCollapsed(prev => {
      const next = !prev
      localStorage.setItem('qris-sidebar-collapsed', next ? '1' : '0')
      return next
    })
  }

  const selectMenu = (id: MenuId) => {
    setActive(id)
    setMenuOpen(false)
  }

  const selectBranch = (branchId: string) => {
    const branch = branches.find(x => x.id === branchId)
    if (!branch || branch.id === activeBranchId) return
    setStoredBranchId(branch.id)
    setActiveBranchId(branch.id)
    setWorkspaceVersion(v => v + 1)
    window.dispatchEvent(new CustomEvent('qris-workspace-changed', { detail: { branchId: branch.id } }))
  }

  const activeBranch = branches.find(x => x.id === activeBranchId)
  const activeBranchName = String(activeBranch?.name || activeBranch?.code || 'Cabang aktif')

  return <div className={`${styles.shell} ${sidebarCollapsed ? styles.sidebarIsCollapsed : ''}`}>
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandMark}>P</div>
        <div className={styles.brandCopy}>
          <div className={styles.brandTitle}>POS QRIS</div>
          <div className={styles.brandSub}>{businessName || 'Bisnis'}</div>
        </div>
        <button className={styles.collapseButton} onClick={toggleSidebar} aria-label="Sembunyikan menu kiri">{sidebarCollapsed ? '›' : '‹'}</button>
      </div>

      <div className={styles.storeCard}>
        <div className={styles.storeDot} />
        <div className={styles.storeCopy}>
          <strong>{activeBranchName}</strong>
          <span>{branches.length > 1 ? `${branches.length} outlet terakses` : 'Outlet aktif'}</span>
        </div>
        {branches.length > 1 ? (
          <select
            aria-label="Pilih outlet aktif"
            value={activeBranchId}
            onChange={e => selectBranch(e.target.value)}
            style={{ maxWidth: 34, border: 0, background: 'transparent', color: 'inherit', cursor: 'pointer' }}
          >
            {branches.map(branch => <option key={branch.id} value={branch.id}>{String(branch.name || branch.code || branch.id)}</option>)}
          </select>
        ) : <span className={styles.chevron}>•</span>}
      </div>

      <nav className={styles.nav}>{['Utama', 'Transaksi', 'Master', 'Analitik', 'Sistem'].map(group => <div className={styles.navGroup} key={group}><div className={styles.navLabel}>{group}</div>{menus.filter(menu => menu.group === group).map(menu => <button key={menu.id} title={sidebarCollapsed ? menu.id : undefined} className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.icon}>{menu.icon}</span><span className={styles.navText}>{menu.id}</span>{active === menu.id && <span className={styles.activeMark} />}</button>)}</div>)}</nav>
      <div className={styles.sidebarFooter}><div className={styles.userAvatar}>O</div><div className={styles.userMeta}><strong>Owner</strong><span>Administrator</span></div><button className={styles.moreButton} aria-label="Opsi pengguna">•••</button></div>
    </aside>
    <main className={styles.main}>{renderFeature(active, salesMode, workspaceVersion)}</main>
    {menuOpen && <div className={styles.mobileMenuBackdrop} onClick={() => setMenuOpen(false)}><div className={styles.mobileMenu} onClick={e => e.stopPropagation()}><div className={styles.mobileMenuHead}><div><strong>Menu POS</strong><span>Semua fitur aplikasi</span></div><button onClick={() => setMenuOpen(false)}>×</button></div><div className={styles.mobileMenuGrid}>{menus.map(menu => <button key={menu.id} className={active === menu.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(menu.id)}><span>{menu.icon}</span><strong>{menu.id}</strong><small>{menu.desc}</small></button>)}</div></div></div>}
    <nav className={styles.mobileBar}>{mobileMenus.map(menu => <button key={menu.id} className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.mobileIcon}>{menu.icon}</span><span>{menu.id}</span></button>)}<button className={styles.mobileButton} onClick={() => setMenuOpen(true)}><span className={styles.mobileIcon}>•••</span><span>Lainnya</span></button></nav>
  </div>
}
