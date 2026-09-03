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
import styles from '../../app/app-shell.module.css'

type MenuId = 'Dashboard' | 'Penjualan' | 'Pesanan' | 'Produk' | 'Stok' | 'Pelanggan' | 'Supplier' | 'Pembelian' | 'Piutang' | 'Laporan' | 'Pembayaran' | 'Pengaturan'
type ViewMode = 'retail' | 'distributor' | 'fnb'
type Menu = { id: MenuId; icon: string; group: string; desc: string }

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

function renderFeature(active: MenuId, mode: ViewMode) {
  switch (active) {
    case 'Dashboard': return <DashboardPage />
    case 'Penjualan': return <SalesTerminal key={mode} />
    case 'Pesanan': return <OrdersPage />
    case 'Produk': return <ProductsPage />
    case 'Stok': return <InventoryPage />
    case 'Pelanggan': return <CustomersPage />
    case 'Supplier': return <SuppliersPage />
    case 'Pembelian': return <PurchasesPage />
    case 'Piutang': return <ReceivablesPage />
    case 'Laporan': return <ReportsPage />
    case 'Pembayaran': return <PaymentsPage />
    case 'Pengaturan': return <SettingsPage />
  }
}

export default function AppShell() {
  const [active, setActive] = useState<MenuId>('Penjualan')
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [salesMode, setSalesMode] = useState<ViewMode>('retail')

  useEffect(() => {
    try {
      setSidebarCollapsed(localStorage.getItem('qris-sidebar-collapsed') === '1')
      const saved = localStorage.getItem('qris-view-mode') as ViewMode | null
      if (saved === 'retail' || saved === 'distributor' || saved === 'fnb') setSalesMode(saved)
    } catch {}
    const onModeChanged = (event: Event) => { const next = (event as CustomEvent<ViewMode>).detail; if (['retail', 'distributor', 'fnb'].includes(next)) setSalesMode(next) }
    window.addEventListener('qris-mode-changed', onModeChanged)
    return () => window.removeEventListener('qris-mode-changed', onModeChanged)
  }, [])

  function toggleSidebar() { setSidebarCollapsed(prev => { const next = !prev; localStorage.setItem('qris-sidebar-collapsed', next ? '1' : '0'); return next }) }
  const selectMenu = (id: MenuId) => { setActive(id); setMenuOpen(false) }

  return <div className={`${styles.shell} ${sidebarCollapsed ? styles.sidebarIsCollapsed : ''}`}>
    <aside className={styles.sidebar}>
      <div className={styles.brand}><div className={styles.brandMark}>P</div><div className={styles.brandCopy}><div className={styles.brandTitle}>POS QRIS</div><div className={styles.brandSub}>Toko Maju Jaya</div></div><button className={styles.collapseButton} onClick={toggleSidebar} aria-label="Sembunyikan menu kiri">{sidebarCollapsed ? '›' : '‹'}</button></div>
      <div className={styles.storeCard}><div className={styles.storeDot} /><div className={styles.storeCopy}><strong>Toko Utama</strong><span>Cabang aktif</span></div><span className={styles.chevron}>⌄</span></div>
      <nav className={styles.nav}>{['Utama', 'Transaksi', 'Master', 'Analitik', 'Sistem'].map(group => <div className={styles.navGroup} key={group}><div className={styles.navLabel}>{group}</div>{menus.filter(menu => menu.group === group).map(menu => <button key={menu.id} title={sidebarCollapsed ? menu.id : undefined} className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.icon}>{menu.icon}</span><span className={styles.navText}>{menu.id}</span>{active === menu.id && <span className={styles.activeMark} />}</button>)}</div>)}</nav>
      <div className={styles.sidebarFooter}><div className={styles.userAvatar}>O</div><div className={styles.userMeta}><strong>Owner</strong><span>Administrator</span></div><button className={styles.moreButton} aria-label="Opsi pengguna">•••</button></div>
    </aside>
    <main className={styles.main}>{renderFeature(active, salesMode)}</main>
    {menuOpen && <div className={styles.mobileMenuBackdrop} onClick={() => setMenuOpen(false)}><div className={styles.mobileMenu} onClick={e => e.stopPropagation()}><div className={styles.mobileMenuHead}><div><strong>Menu POS</strong><span>Semua fitur aplikasi</span></div><button onClick={() => setMenuOpen(false)}>×</button></div><div className={styles.mobileMenuGrid}>{menus.map(menu => <button key={menu.id} className={active === menu.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(menu.id)}><span>{menu.icon}</span><strong>{menu.id}</strong><small>{menu.desc}</small></button>)}</div></div></div>}
    <nav className={styles.mobileBar}>{mobileMenus.map(menu => <button key={menu.id} className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(menu.id)}><span className={styles.mobileIcon}>{menu.icon}</span><span>{menu.id}</span></button>)}<button className={styles.mobileButton} onClick={() => setMenuOpen(true)}><span className={styles.mobileIcon}>•••</span><span>Lainnya</span></button></nav>
  </div>
}
