'use client'

import { useState } from 'react'
import SalesTerminal from './sales-terminal'
import styles from './app-shell.module.css'

type MenuId = 'Dashboard' | 'Penjualan' | 'Pesanan' | 'Produk' | 'Stok' | 'Pelanggan' | 'Supplier' | 'Pembelian' | 'Piutang' | 'Laporan' | 'Pembayaran' | 'Pengaturan'

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

function Placeholder({ menu }: { menu: Menu }) {
  return (
    <div className={styles.placeholder}>
      <div className={styles.placeholderGlow} />
      <div className={styles.placeholderCard}>
        <div className={styles.placeholderIcon}>{menu.icon}</div>
        <div className={styles.eyebrow}>{menu.group.toUpperCase()}</div>
        <h2 className={styles.title}>{menu.id}</h2>
        <p className={styles.text}>{menu.desc}. Modul ini tetap menjadi bagian dari aplikasi dan siap diaktifkan tanpa mengganggu alur Penjualan.</p>
        <div className={styles.comingSoon}><span /> Modul sedang disiapkan</div>
      </div>
    </div>
  )
}

export default function HomePage() {
  const [active, setActive] = useState<MenuId>('Penjualan')
  const [menuOpen, setMenuOpen] = useState(false)

  const selectMenu = (id: MenuId) => {
    setActive(id)
    setMenuOpen(false)
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <div className={styles.brandMark}>P</div>
          <div>
            <div className={styles.brandTitle}>POS QRIS</div>
            <div className={styles.brandSub}>Toko Maju Jaya</div>
          </div>
        </div>
        <div className={styles.storeCard}>
          <div className={styles.storeDot} />
          <div><strong>Toko Utama</strong><span>Cabang aktif</span></div>
          <span className={styles.chevron}>⌄</span>
        </div>
        <nav className={styles.nav}>
          {['Utama', 'Transaksi', 'Master', 'Analitik', 'Sistem'].map(group => (
            <div className={styles.navGroup} key={group}>
              <div className={styles.navLabel}>{group}</div>
              {menus.filter(menu => menu.group === group).map(menu => (
                <button key={menu.id} className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`} onClick={() => selectMenu(menu.id)}>
                  <span className={styles.icon}>{menu.icon}</span>
                  <span className={styles.navText}>{menu.id}</span>
                  {active === menu.id && <span className={styles.activeMark} />}
                </button>
              ))}
            </div>
          ))}
        </nav>
        <div className={styles.sidebarFooter}>
          <div className={styles.userAvatar}>A</div>
          <div className={styles.userMeta}><strong>Admin</strong><span>Administrator</span></div>
          <button className={styles.moreButton} aria-label="Opsi pengguna">•••</button>
        </div>
      </aside>

      <main className={styles.main}>
        {active === 'Penjualan' ? <SalesTerminal /> : <Placeholder menu={menus.find(x => x.id === active)!} />}
      </main>

      {menuOpen && (
        <div className={styles.mobileMenuBackdrop} onClick={() => setMenuOpen(false)}>
          <div className={styles.mobileMenu} onClick={e => e.stopPropagation()}>
            <div className={styles.mobileMenuHead}><div><strong>Menu POS</strong><span>Semua fitur aplikasi</span></div><button onClick={() => setMenuOpen(false)}>×</button></div>
            <div className={styles.mobileMenuGrid}>
              {menus.map(menu => (
                <button key={menu.id} className={active === menu.id ? styles.mobileMenuActive : ''} onClick={() => selectMenu(menu.id)}>
                  <span>{menu.icon}</span><strong>{menu.id}</strong><small>{menu.desc}</small>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      <nav className={styles.mobileBar}>
        {mobileMenus.map(menu => (
          <button key={menu.id} className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`} onClick={() => selectMenu(menu.id)}>
            <span className={styles.mobileIcon}>{menu.icon}</span><span>{menu.id}</span>
          </button>
        ))}
        <button className={styles.mobileButton} onClick={() => setMenuOpen(true)}><span className={styles.mobileIcon}>•••</span><span>Lainnya</span></button>
      </nav>
    </div>
  )
}
