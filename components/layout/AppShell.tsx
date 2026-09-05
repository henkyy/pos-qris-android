'use client'

import { useEffect, useMemo, useState } from 'react'
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
type Menu = { id: MenuId; label: string; icon: string; group: string; desc: string }
type Branch = Record<string, any>
type NavGroup = { id: string; label: string; menus: Menu[] }

const menus: Menu[] = [
  { id: 'Dashboard', label: 'Dashboard', icon: '⌂', group: 'DASHBOARD', desc: 'Ringkasan bisnis outlet aktif' },
  { id: 'Kasir', label: 'Kasir', icon: '▣', group: 'TRANSAKSI', desc: 'Buat dan proses transaksi penjualan' },
  { id: 'Penjualan', label: 'Penjualan', icon: '▤', group: 'TRANSAKSI', desc: 'Riwayat dan detail penjualan' },
  { id: 'Pesanan', label: 'Pesanan', icon: '◫', group: 'TRANSAKSI', desc: 'Pantau status pesanan penjualan' },
  { id: 'Pembayaran', label: 'Pembayaran', icon: '◉', group: 'TRANSAKSI', desc: 'Pantau pembayaran dan metode pembayaran' },
  { id: 'Produk', label: 'Produk', icon: '□', group: 'INVENTORI', desc: 'Kelola katalog, satuan dan harga produk' },
  { id: 'Stok', label: 'Stok', icon: '▥', group: 'INVENTORI', desc: 'Pantau saldo stok outlet aktif' },
  { id: 'Pergerakan Stok', label: 'Pergerakan Stok', icon: '↕', group: 'INVENTORI', desc: 'Audit pergerakan masuk dan keluar' },
  { id: 'Stok Opname', label: 'Stok Opname', icon: '▦', group: 'INVENTORI', desc: 'Rekonsiliasi stok fisik' },
  { id: 'Penyesuaian Stok', label: 'Penyesuaian Stok', icon: '±', group: 'INVENTORI', desc: 'Koreksi stok dengan alasan' },
  { id: 'Purchase Order', label: 'Purchase Order', icon: 'PO', group: 'PEMBELIAN', desc: 'Pesanan pembelian ke supplier' },
  { id: 'Penerimaan Barang', label: 'Penerimaan Barang', icon: 'GR', group: 'PEMBELIAN', desc: 'Penerimaan barang dan pembentukan stok' },
  { id: 'Hutang', label: 'Hutang', icon: 'Rp', group: 'PEMBELIAN', desc: 'Kewajiban dan pembayaran supplier' },
  { id: 'Supplier', label: 'Supplier', icon: '⇄', group: 'PEMBELIAN', desc: 'Kelola pemasok dan termin pembayaran' },
  { id: 'Pelanggan', label: 'Pelanggan', icon: '♙', group: 'PELANGGAN', desc: 'Kelola pelanggan dan relasi transaksi' },
  { id: 'Piutang', label: 'Piutang', icon: 'Rp', group: 'PELANGGAN', desc: 'Pantau tagihan pelanggan' },
  { id: 'Riwayat Transaksi', label: 'Riwayat Transaksi', icon: '▤', group: 'PELANGGAN', desc: 'Telusuri transaksi pelanggan dan outlet' },
  { id: 'Laporan Penjualan', label: 'Penjualan', icon: '▤', group: 'LAPORAN', desc: 'Analisis omzet dan transaksi' },
  { id: 'Produk Terlaris', label: 'Produk Terlaris', icon: '★', group: 'LAPORAN', desc: 'Peringkat produk berdasarkan penjualan' },
  { id: 'Laporan Stok', label: 'Stok', icon: '▥', group: 'LAPORAN', desc: 'Analisis persediaan dan stok kritis' },
  { id: 'Laporan Pembelian', label: 'Pembelian', icon: '⇩', group: 'LAPORAN', desc: 'Analisis pembelian dan penerimaan' },
  { id: 'Laporan Piutang', label: 'Piutang', icon: 'AR', group: 'LAPORAN', desc: 'Analisis outstanding piutang' },
  { id: 'Laporan Hutang', label: 'Hutang', icon: 'AP', group: 'LAPORAN', desc: 'Analisis outstanding hutang' },
  { id: 'Pengeluaran', label: 'Pengeluaran', icon: '−', group: 'LAPORAN', desc: 'Analisis biaya operasional' },
  { id: 'Laba & Margin', label: 'Laba & Margin', icon: '↗', group: 'LAPORAN', desc: 'Analisis laba kotor dan margin' },
  { id: 'Cabang / Outlet', label: 'Cabang / Outlet', icon: '⌂', group: 'MANAJEMEN', desc: 'Kelola outlet dalam bisnis' },
  { id: 'Karyawan & Akses', label: 'Karyawan & Akses', icon: '♙', group: 'MANAJEMEN', desc: 'Kelola anggota, peran, dan akses outlet' },
  { id: 'Shift Kasir', label: 'Shift Kasir', icon: '◷', group: 'MANAJEMEN', desc: 'Kelola siklus shift kasir' },
  { id: 'Metode Pembayaran', label: 'Metode Pembayaran', icon: '◉', group: 'MANAJEMEN', desc: 'Kelola metode pembayaran' },
  { id: 'Audit Log', label: 'Audit Log', icon: '⌁', group: 'MANAJEMEN', desc: 'Jejak aktivitas pengguna' },
  { id: 'Bisnis', label: 'Bisnis', icon: 'B', group: 'PENGATURAN', desc: 'Identitas dan konfigurasi bisnis' },
  { id: 'Outlet', label: 'Outlet', icon: '⌂', group: 'PENGATURAN', desc: 'Konfigurasi outlet' },
  { id: 'Satuan', label: 'Satuan', icon: 'U', group: 'PENGATURAN', desc: 'Kelola satuan produk' },
  { id: 'Kategori', label: 'Kategori', icon: 'C', group: 'PENGATURAN', desc: 'Kelola kategori produk' },
  { id: 'Harga', label: 'Harga', icon: 'Rp', group: 'PENGATURAN', desc: 'Kelola price list dan harga berlaku' },
  { id: 'QRIS', label: 'QRIS', icon: 'Q', group: 'PENGATURAN', desc: 'Konfigurasi kanal QRIS' },
  { id: 'Pengaturan Sistem', label: 'Pengaturan Sistem', icon: '⚙', group: 'PENGATURAN', desc: 'Konfigurasi aplikasi' },
]

const navGroups: NavGroup[] = ['TRANSAKSI','INVENTORI','PEMBELIAN','PELANGGAN','LAPORAN','MANAJEMEN','PENGATURAN'].map(id => ({ id, label: id, menus: menus.filter(menu => menu.group === id) }))
const mobileMenus = menus.filter(x => ['Dashboard','Kasir','Stok','Laporan Penjualan'].includes(x.id))

function renderFeature(active: MenuId, mode: ViewMode, workspaceVersion: number) {
  const key = `${workspaceVersion}-${mode}-${active}`
  switch (active) {
    case 'Dashboard': return <DashboardPage key={key} />
    case 'Kasir': return <SalesTerminal key={key} />
    case 'Penjualan': return <WorkspaceMenuPage key={key} menu="Riwayat Transaksi" />
    case 'Pesanan': return <OrdersPage key={key} />
    case 'Produk': return <ProductsPage key={key} />
    case 'Stok': return <InventoryPage key={key} />
    case 'Pelanggan': return <CustomersPage key={key} />
    case 'Supplier': return <SuppliersPage key={key} />
    case 'Pembelian': return <PurchasesPage key={key} />
    case 'Piutang': return <ReceivablesPage key={key} />
    case 'Laporan': return <ReportsPage key={key} />
    case 'Pembayaran': return <PaymentsPage key={key} />
    case 'Pengaturan Sistem': return <SettingsPage key={key} />
    default: return <WorkspaceMenuPage key={key} menu={active as WorkspaceMenuId} />
  }
}

export default function AppShell() {
  const [active, setActive] = useState<MenuId>('Dashboard')
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [salesMode, setSalesMode] = useState<ViewMode>('retail')
  const [businessName, setBusinessName] = useState('')
  const [branches, setBranches] = useState<Branch[]>([])
  const [activeBranchId, setActiveBranchId] = useState('')
  const [workspaceVersion, setWorkspaceVersion] = useState(0)
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({ TRANSAKSI:true, INVENTORI:true, PEMBELIAN:false, PELANGGAN:false, LAPORAN:false, MANAJEMEN:false, PENGATURAN:false })

  useEffect(() => {
    try {
      setSidebarCollapsed(localStorage.getItem('qris-sidebar-collapsed') === '1')
      const saved = localStorage.getItem('qris-view-mode') as ViewMode | null
      if (saved === 'retail' || saved === 'distributor' || saved === 'fnb') setSalesMode(saved)
      const savedGroups = localStorage.getItem('qris-nav-groups')
      if (savedGroups) { const parsed = JSON.parse(savedGroups); if (parsed && typeof parsed === 'object') setOpenGroups(current => ({ ...current, ...parsed })) }
    } catch {}
    void (async () => { try { const workspace = await getActiveWorkspace(); setBusinessName(String(workspace.business?.name || workspace.business?.business_name || '')); const list = await getAccessibleBranches(workspace.business.id); setBranches(list); setActiveBranchId(workspace.branch.id) } catch {} })()
    const onModeChanged = (event: Event) => { const next = (event as CustomEvent<ViewMode>).detail; if (['retail','distributor','fnb'].includes(next)) setSalesMode(next) }
    window.addEventListener('qris-mode-changed', onModeChanged)
    return () => window.removeEventListener('qris-mode-changed', onModeChanged)
  }, [])

  useEffect(() => { const group = menus.find(menu => menu.id === active)?.group; if (!group) return; setOpenGroups(current => current[group] ? current : { ...current, [group]: true }) }, [active])
  function toggleSidebar() { setSidebarCollapsed(prev => { const next = !prev; localStorage.setItem('qris-sidebar-collapsed', next ? '1' : '0'); return next }) }
  function toggleGroup(groupId: string) { setOpenGroups(current => { const next = { ...current, [groupId]: !current[groupId] }; try { localStorage.setItem('qris-nav-groups', JSON.stringify(next)) } catch {}; return next }) }
  const selectMenu = (id: MenuId) => { setActive(id); setMenuOpen(false) }
  const selectBranch = (branchId: string) => { const branch = branches.find(x => x.id === branchId); if (!branch || branch.id === activeBranchId) return; setStoredBranchId(branch.id); setActiveBranchId(branch.id); setWorkspaceVersion(v => v + 1); window.dispatchEvent(new CustomEvent('qris-workspace-changed', { detail: { branchId: branch.id } })) }
  const activeBranch = branches.find(x => x.id === activeBranchId)
  const activeBranchName = String(activeBranch?.name || activeBranch?.code || 'Outlet aktif')
  const activeGroup = useMemo(() => menus.find(menu => menu.id === active)?.group || 'DASHBOARD', [active])

  return <div className={`${styles.shell} ${sidebarCollapsed ? styles.sidebarIsCollapsed : ''}`}>
    <aside className={styles.sidebar}>
      <div className={styles.brand}><div className={styles.brandMark}>Q</div><div className={styles.brandCopy}><div className={styles.brandTitle}>QRIS POS</div><div className={styles.brandSub}>{businessName || 'Bisnis'}</div></div><button className={styles.collapseButton} onClick={toggleSidebar} aria-label={sidebarCollapsed ? 'Tampilkan menu kiri' : 'Sembunyikan menu kiri'}>{sidebarCollapsed ? '›' : '‹'}</button></div>
      <div className={styles.workspaceCard}><div className={styles.workspaceIcon}>⌂</div><div className={styles.workspaceCopy}><span>OUTLET AKTIF</span><strong>{activeBranchName}</strong></div>{branches.length > 1 && <select aria-label="Pilih outlet aktif" value={activeBranchId} onChange={e => selectBranch(e.target.value)}>{branches.map(branch => <option key={branch.id} value={branch.id}>{String(branch.name || branch.code || branch.id)}</option>)}</select>}</div>
      <nav className={styles.nav} aria-label="Navigasi utama">
        <button className={`${styles.navButton} ${active === 'Dashboard' ? styles.navActive : ''}`} onClick={() => selectMenu('Dashboard')} title={sidebarCollapsed ? 'Dashboard' : undefined}><span className={styles.icon}>⌂</span><span className={styles.navText}>Dashboard</span>{active === 'Dashboard' && <span className={styles.activeMark} />}</button>
        {navGroups.filter(group => group.id !== 'PENGATURAN').map(group => { const expanded = Boolean(openGroups[group.id]); const hasActive = activeGroup === group.id; return <div className={styles.navGroup} key={group.id}><button className={`${styles.navGroupButton} ${hasActive ? styles.navGroupButtonActive : ''}`} onClick={() => toggleGroup(group.id)} aria-expanded={expanded} title={sidebarCollapsed ? group.label : undefined}><span className={styles.navLabel}>{group.label}</span><span className={styles.navGroupChevron}>{expanded ? '⌄' : '›'}</span></button>{expanded && <div className={styles.navGroupChildren}>{group.menus.map(menu => <button key={menu.id} title={sidebarCollapsed ? menu.label : undefined} className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.icon}>{menu.icon}</span><span className={styles.navText}>{menu.label}</span>{active === menu.id && <span className={styles.activeMark} />}</button>)}</div>}</div> })}
        <div className={styles.navGroup}><button className={`${styles.navGroupButton} ${activeGroup === 'PENGATURAN' ? styles.navGroupButtonActive : ''}`} onClick={() => toggleGroup('PENGATURAN')} aria-expanded={Boolean(openGroups.PENGATURAN)}><span className={styles.navLabel}>PENGATURAN</span><span className={styles.navGroupChevron}>{openGroups.PENGATURAN ? '⌄' : '›'}</span></button>{openGroups.PENGATURAN && <div className={styles.navGroupChildren}>{menus.filter(menu => menu.group === 'PENGATURAN').map(menu => <button key={menu.id} className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.icon}>{menu.icon}</span><span className={styles.navText}>{menu.label}</span>{active === menu.id && <span className={styles.activeMark} />}</button>)}</div>}</div>
      </nav>
      <div className={styles.sidebarFooter}><div className={styles.userAvatar}>O</div><div className={styles.userMeta}><strong>Owner</strong><span>Administrator</span></div><button className={styles.moreButton} aria-label="Opsi pengguna">•••</button></div>
    </aside>
    <main className={styles.main}>{renderFeature(active, salesMode, workspaceVersion)}</main>
    {menuOpen && <div className={styles.mobileMenuBackdrop} onClick={() => setMenuOpen(false)}><div className={styles.mobileMenu} onClick={e => e.stopPropagation()}><div className={styles.mobileMenuHead}><div><strong>Menu</strong><span>Semua fitur aplikasi</span></div><button aria-label="Tutup menu" onClick={() => setMenuOpen(false)}>×</button></div><div className={styles.mobileMenuGroupList}><section><h3>DASHBOARD</h3><div className={styles.mobileMenuGrid}>{menus.filter(menu => menu.id === 'Dashboard').map(menu => <button key={menu.id} className={active === menu.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(menu.id)}><span>{menu.icon}</span><strong>{menu.label}</strong><small>{menu.desc}</small></button>)}</div></section>{navGroups.map(group => <section key={group.id}><h3>{group.label}</h3><div className={styles.mobileMenuGrid}>{group.menus.map(menu => <button key={menu.id} className={active === menu.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(menu.id)}><span>{menu.icon}</span><strong>{menu.label}</strong><small>{menu.desc}</small></button>)}</div></section>)}</div></div></div>}
    <nav className={styles.mobileBar} aria-label="Navigasi cepat">{mobileMenus.map(menu => <button key={menu.id} className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.mobileIcon}>{menu.icon}</span><span>{menu.label}</span></button>)}<button className={styles.mobileButton} onClick={() => setMenuOpen(true)}><span className={styles.mobileIcon}>•••</span><span>Lainnya</span></button></nav>
  </div>
}
