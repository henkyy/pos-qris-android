'use client'

import { useState } from 'react'
import SalesTerminal from './sales-terminal'
import styles from './app-shell.module.css'

type MenuId = 'Dashboard' | 'Penjualan' | 'Pesanan' | 'Produk' | 'Stok' | 'Pelanggan' | 'Supplier' | 'Pembelian' | 'Piutang' | 'Laporan' | 'Pembayaran' | 'Pengaturan'

const menus: { id: MenuId; icon: string }[] = [
  { id: 'Dashboard', icon: '⌂' },
  { id: 'Penjualan', icon: '▣' },
  { id: 'Pesanan', icon: '▤' },
  { id: 'Produk', icon: '□' },
  { id: 'Stok', icon: '▥' },
  { id: 'Pelanggan', icon: '♙' },
  { id: 'Supplier', icon: '⇄' },
  { id: 'Pembelian', icon: '⇩' },
  { id: 'Piutang', icon: 'Rp' },
  { id: 'Laporan', icon: '▥' },
  { id: 'Pembayaran', icon: '◉' },
  { id: 'Pengaturan', icon: '⚙' },
]

function Placeholder({ menu }: { menu: MenuId }) {
  return (
    <div className={styles.placeholder}>
      <div className={styles.placeholderCard}>
        <div className={styles.eyebrow}>POS QRIS</div>
        <h2 className={styles.title}>{menu}</h2>
        <p className={styles.text}>
          Menu ini tetap menjadi bagian dari aplikasi. Implementasi Penjualan sedang menjadi fokus,
          jadi halaman {menu} belum diaktifkan kembali pada web shell ini.
        </p>
      </div>
    </div>
  )
}

export default function HomePage() {
  const [active, setActive] = useState<MenuId>('Penjualan')
  const mobileMenus = menus.filter(x => ['Dashboard', 'Penjualan', 'Produk', 'Stok'].includes(x.id))

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <div className={styles.brandTitle}>POS QRIS</div>
          <div className={styles.brandSub}>Toko Maju Jaya</div>
        </div>
        <nav className={styles.nav}>
          {menus.map(menu => (
            <button
              key={menu.id}
              className={`${styles.navButton} ${active === menu.id ? styles.navActive : ''}`}
              onClick={() => setActive(menu.id)}
            >
              <span className={styles.icon}>{menu.icon}</span>
              <span>{menu.id}</span>
            </button>
          ))}
        </nav>
        <div className={styles.user}>
          <div className={styles.userName}>Admin</div>
          <div className={styles.userSub}>Toko Utama</div>
        </div>
      </aside>

      <main className={styles.main}>
        {active === 'Penjualan' ? <SalesTerminal /> : <Placeholder menu={active} />}
      </main>

      <nav className={styles.mobileBar}>
        {mobileMenus.map(menu => (
          <button
            key={menu.id}
            className={`${styles.mobileButton} ${active === menu.id ? styles.mobileActive : ''}`}
            onClick={() => setActive(menu.id)}
          >
            <span className={styles.mobileIcon}>{menu.icon}</span>
            <span>{menu.id}</span>
          </button>
        ))}
      </nav>
    </div>
  )
}
