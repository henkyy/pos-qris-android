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

type MenuId = 'Dashboard' | 'Penjualan' | 'Pesanan' | 'Pembayaran' | 'Produk' | 'Stok' | 'Pembelian' | 'Supplier' | 'Pelanggan' | 'Piutang' | 'Laporan' | 'Pengaturan'
type ViewMode = 'retail' | 'distributor' | 'fnb'
type Menu = { id: MenuId; label: string; icon: string; group: string; desc: string }
type Branch = Record<string, any>

const menus: Menu[] = [
  { id: 'Dashboard', label: 'Dashboard', icon: '⌂', group: 'DASHBOARD', desc: 'Ringkasan bisnis' },
  { id: 'Penjualan', label: 'Kasir', icon: '▣', group: 'TRANSAKSI', desc: 'Transaksi penjualan' },
  { id: 'Penjualan', label: 'Penjualan', icon: '▤', group: 'TRANSAKSI', desc: 'Riwayat penjualan' },
  { id: 'Pesanan', label: 'Pesanan', icon: '◫', group: 'TRANSAKSI', desc: 'Pesanan berjalan' },
  { id: 'Pembayaran', label: 'Pembayaran', icon: '◉', group: 'TRANSAKSI', desc: 'Status pembayaran' },
  { id: 'Produk', label: 'Produk', icon: '□', group: 'INVENTORI', desc: 'Produk & harga' },
  { id: 'Stok', label: 'Stok', icon: '▥', group: 'INVENTORI', desc: 'Persediaan' },
  { id: 'Stok', label: 'Pergerakan Stok', icon: '↕', group: 'INVENTORI', desc: 'Riwayat mutasi stok' },
  { id: 'Stok', label: 'Stok Opname', icon: '☷', group: 'INVENTORI', desc: 'Pemeriksaan fisik stok' },
  { id: 'Stok', label: 'Penyesuaian Stok', icon: '±', group: 'INVENTORI', desc: 'Koreksi persediaan' },
  { id: 'Pembelian', label: 'Purchase Order', icon: '⇩', group: 'PEMBELIAN', desc: 'Pesanan pembelian' },
  { id: 'Pembelian', label: 'Penerimaan Barang', icon: '▾', group: 'PEMBELIAN', desc: 'Penerimaan barang' },
  { id: 'Piutang', label: 'Hutang', icon: 'Rp', group: 'PEMBELIAN', desc: 'Tagihan supplier' },
  { id: 'Supplier', label: 'Supplier', icon: '⇄', group: 'PEMBELIAN', desc: 'Pemasok barang' },
  { id: 'Pelanggan', label: 'Pelanggan', icon: '♙', group: 'PELANGGAN', desc: 'Data pelanggan' },
  { id: 'Piutang', label: 'Piutang', icon: 'Rp', group: 'PELANGGAN', desc: 'Tagihan pelanggan' },
  { id: 'Pelanggan', label: 'Riwayat Transaksi', icon: '↺', group: 'PELANGGAN', desc: 'Riwayat transaksi pelanggan' },
  { id: 'Laporan', label: 'Penjualan', icon: '▤', group: 'LAPORAN', desc: 'Laporan penjualan' },
  { id: 'Laporan', label: 'Produk Terlaris', icon: '★', group: 'LAPORAN', desc: 'Performa produk' },
  { id: 'Laporan', label: 'Stok', icon: '▥', group: 'LAPORAN', desc: 'Laporan persediaan' },
  { id: 'Laporan', label: 'Pembelian', icon: '⇩', group: 'LAPORAN', desc: 'Laporan pembelian' },
  { id: 'Laporan', label: 'Piutang', icon: 'Rp', group: 'LAPORAN', desc: 'Laporan piutang' },
  { id: 'Laporan', label: 'Hutang', icon: 'Rp', group: 'LAPORAN', desc: 'Laporan hutang' },
  { id: 'Laporan', label: 'Pengeluaran', icon: '−', group: 'LAPORAN', desc: 'Laporan pengeluaran' },
  { id: 'Laporan', label: 'Laba & Margin', icon: '%', group: 'LAPORAN', desc: 'Profitabilitas bisnis' },
  { id: 'Pengaturan', label: 'Cabang / Outlet', icon: '⌂', group: 'MANAJEMEN', desc: 'Workspace outlet aktif' },
  { id: 'Pengaturan', label: 'Karyawan & Akses', icon: '♙', group: 'MANAJEMEN', desc: 'Segera tersedia', disabled: true } as Menu & { disabled?: boolean },
  { id: 'Pengaturan', label: 'Shift Kasir', icon: '◷', group: 'MANAJEMEN', desc: 'Segera tersedia', disabled: true } as Menu & { disabled?: boolean },
  { id: 'Pembayaran', label: 'Metode Pembayaran', icon: '◉', group: 'MANAJEMEN', desc: 'Metode pembayaran aktif' },
  { id: 'Pengaturan', label: 'Audit Log', icon: '≡', group: 'MANAJEMEN', desc: 'Segera tersedia', disabled: true } as Menu & { disabled?: boolean },
  { id: 'Pengaturan', label: 'Bisnis', icon: '◆', group: 'PENGATURAN', desc: 'Profil bisnis' },
  { id: 'Pengaturan', label: 'Outlet', icon: '⌂', group: 'PENGATURAN', desc: 'Konfigurasi outlet' },
  { id: 'Pengaturan', label: 'Satuan', icon: '◌', group: 'PENGATURAN', desc: 'Satuan produk' },
  { id: 'Pengaturan', label: 'Kategori', icon: '▦', group: 'PENGATURAN', desc: 'Kategori produk' },
  { id: 'Produk', label: 'Harga', icon: 'Rp', group: 'PENGATURAN', desc: 'Harga jual produk' },
  { id: 'Pengaturan', label: 'QRIS', icon: '▣', group: 'PENGATURAN', desc: 'Konfigurasi QRIS' },
  { id: 'Pengaturan', label: 'Pengaturan Sistem', icon: '⚙', group: 'PENGATURAN', desc: 'Konfigurasi aplikasi' },
]

const mobileMenus = menus.filter((x, index) => index === 0 || ['Kasir', 'Stok', 'Laporan'].includes(x.label)).filter((x, index, all) => all.findIndex(y => y.id === x.id) === index)
const groups = ['DASHBOARD', 'TRANSAKSI', 'INVENTORI', 'PEMBELIAN', 'PELANGGAN', 'LAPORAN', 'MANAJEMEN', 'PENGATURAN']

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
  const activeBranchName = String(activeBranch?.name || activeBranch?.code || 'Outlet aktif')

  return <div className={`${styles.shell} ${sidebarCollapsed ? styles.sidebarIsCollapsed : ''}`}>
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.brandMark}>Q</div>
        <div className={styles.brandCopy}>
          <div className={styles.brandTitle}>QRIS POS</div>
          <div className={styles.brandSub}>{businessName || 'Bisnis'}</div>
        </div>
        <button className={styles.collapseButton} onClick={toggleSidebar} aria-label={sidebarCollapsed ? 'Tampilkan menu kiri' : 'Sembunyikan menu kiri'}>{sidebarCollapsed ? '›' : '‹'}</button>
      </div>

      <div className={styles.workspaceCard}>
        <div className={styles.workspaceIcon}>⌂</div>
        <div className={styles.workspaceCopy}>
          <span>OUTLET AKTIF</span>
          <strong>{activeBranchName}</strong>
        </div>
        {branches.length > 1 && (
          <select aria-label="Pilih outlet aktif" value={activeBranchId} onChange={e => selectBranch(e.target.value)}>
            {branches.map(branch => <option key={branch.id} value={branch.id}>{String(branch.name || branch.code || branch.id)}</option>)}
          </select>
        )}
      </div>

      <nav className={styles.nav} aria-label="Navigasi utama">
        {groups.map(group => (
          <div className={styles.navGroup} key={group}>
            <div className={styles.navLabel}>{group}</div>
            {menus.filter(menu => menu.group === group).map((menu, index) => {
              const disabled = 'disabled' in menu && Boolean((menu as Menu & { disabled?: boolean }).disabled)
              return <button key={`${group}-${menu.label}-${index}`} title={sidebarCollapsed ? menu.label : disabled ? 'Belum tersedia' : undefined} disabled={disabled} className={`${styles.navButton} ${active === menu.id && !disabled ? styles.navActive : ''} ${disabled ? styles.navDisabled : ''}`} onClick={() => !disabled && selectMenu(menu.id)}>
                <span className={styles.icon}>{menu.icon}</span>
                <span className={styles.navText}>{menu.label}</span>
                {disabled ? <span className={styles.navSoon}>Segera</span> : active === menu.id && <span className={styles.activeMark} />}
              </button>
            })}
          </div>
        ))}
      </nav>

      <div className={styles.sidebarFooter}>
        <div className={styles.userAvatar}>O</div>
        <div className={styles.userMeta}><strong>Owner</strong><span>Administrator</span></div>
        <button className={styles.moreButton} aria-label="Opsi pengguna">•••</button>
      </div>
    </aside>

    <main className={styles.main}>{renderFeature(active, salesMode, workspaceVersion)}</main>

    {menuOpen && <div className={styles.mobileMenuBackdrop} onClick={() => setMenuOpen(false)}>
      <div className={styles.mobileMenu} onClick={e => e.stopPropagation()}>
        <div className={styles.mobileMenuHead}><div><strong>Menu</strong><span>Semua fitur aplikasi</span></div><button aria-label="Tutup menu" onClick={() => setMenuOpen(false)}>×</button></div>
        <div className={styles.mobileMenuGroupList}>
          {groups.filter(x => x !== 'DASHBOARD').map(group => (
            <section key={group}><h3>{group}</h3><div className={styles.mobileMenuGrid}>{menus.filter(menu => menu.group === group).map((menu, index) => {
              const disabled = 'disabled' in menu && Boolean((menu as Menu & { disabled?: boolean }).disabled)
              return <button key={`${group}-${menu.label}-${index}`} disabled={disabled} className={active === menu.id && !disabled ? styles.mobileMenuActive : ''} onClick={() => !disabled && selectMenu(menu.id)}><span>{menu.icon}</span><strong>{menu.label}</strong><small>{disabled ? 'Segera tersedia' : menu.desc}</small></button>
            })}</div></section>
          ))}
        </div>
      </div>
    </div>}

    <nav className={styles.mobileBar} aria-label="Navigasi cepat">
      {mobileMenus.map(menu => <button key={menu.id} className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.mobileIcon}>{menu.icon}</span><span>{menu.label}</span></button>)}
      <button className={styles.mobileButton} onClick={() => setMenuOpen(true)}><span className={styles.mobileIcon}>•••</span><span>Lainnya</span></button>
    </nav>
  </div>
}
