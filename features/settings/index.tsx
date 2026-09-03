'use client'
import { useState } from 'react'

const modes = ['Retail', 'Distributor', 'F&B']
export default function SettingsPage() {
  const [mode, setMode] = useState(() => typeof window === 'undefined' ? 'Retail' : ({ retail: 'Retail', distributor: 'Distributor', fnb: 'F&B' }[window.localStorage.getItem('qris-view-mode') || 'retail'] || 'Retail'))
  function save(next: string) { const id = next === 'Retail' ? 'retail' : next === 'Distributor' ? 'distributor' : 'fnb'; setMode(next); localStorage.setItem('qris-view-mode', id); window.dispatchEvent(new CustomEvent('qris-mode-changed', { detail: id })) }
  return <div className="module-page"><div className="module-hero"><div><span className="eyebrow">SISTEM · OWNER</span><h1>Pengaturan</h1><p>Konfigurasi bisnis, cabang, user, role, permission dan mode operasional.</p></div></div><div className="settings-grid">{['Bisnis', 'Cabang', 'User', 'Role & Permission'].map(x => <section className="module-card setting-card" key={x}><strong>{x}</strong><span>Fondasi konfigurasi {x.toLowerCase()} dipisahkan dari transaksi.</span></section>)}</div><section className="module-card"><div className="module-card-head"><div><strong>Mode operasional Penjualan</strong><span>Mengubah pengalaman kasir tanpa memindahkan logic checkout.</span></div></div><div className="mode-grid">{modes.map(x => <button key={x} className={mode === x ? 'mode-active' : ''} onClick={() => save(x)}>{x}</button>)}</div></section></div>
}
