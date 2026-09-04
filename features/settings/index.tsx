'use client'
import { useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import { getActiveWorkspace } from '../../lib/business-context'
import styles from './settings.module.css'

const modes = ['Retail', 'Distributor', 'F&B']

type ResetMode = 'transactions' | 'full'

export default function SettingsPage() {
  const [mode, setMode] = useState(() => typeof window === 'undefined' ? 'Retail' : ({ retail: 'Retail', distributor: 'Distributor', fnb: 'F&B' }[window.localStorage.getItem('qris-view-mode') || 'retail'] || 'Retail'))
  const [resetMode, setResetMode] = useState<ResetMode | null>(null)
  const [confirmText, setConfirmText] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  function save(next: string) {
    const id = next === 'Retail' ? 'retail' : next === 'Distributor' ? 'distributor' : 'fnb'
    setMode(next)
    localStorage.setItem('qris-view-mode', id)
    window.dispatchEvent(new CustomEvent('qris-mode-changed', { detail: id }))
  }

  function openReset(next: ResetMode) { setMessage(''); setConfirmText(''); setResetMode(next) }
  function closeReset() { if (busy) return; setResetMode(null); setConfirmText('') }

  async function executeReset() {
    if (!resetMode || confirmText !== 'RESET') return
    setBusy(true); setMessage('')
    try {
      const db = requireSupabase()
      const { business } = await getActiveWorkspace()
      const { error } = await db.rpc('reset_business_data', { p_business_id: business.id, p_mode: resetMode })
      if (error) throw error
      closeReset()
      setMessage(resetMode === 'full' ? 'Reset penuh selesai. Data transaksi dan master bisnis sudah dibersihkan.' : 'Reset transaksi selesai. Master bisnis tetap dipertahankan.')
      window.dispatchEvent(new CustomEvent('qris-data-reset', { detail: resetMode }))
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Reset gagal. Tidak ada perubahan yang dinyatakan berhasil.')
    } finally { setBusy(false) }
  }

  const isFull = resetMode === 'full'
  return <div className="module-page">
    <div className="module-hero"><div><span className="eyebrow">SISTEM · OWNER</span><h1>Pengaturan</h1><p>Konfigurasi bisnis, cabang, user, role, permission dan mode operasional.</p></div></div>
    <div className="settings-grid">{['Bisnis', 'Cabang', 'User', 'Role & Permission'].map(x => <section className="module-card setting-card" key={x}><strong>{x}</strong><span>Fondasi konfigurasi {x.toLowerCase()} dipisahkan dari transaksi.</span></section>)}</div>
    <section className="module-card"><div className="module-card-head"><div><strong>Mode operasional Penjualan</strong><span>Mengubah pengalaman kasir tanpa memindahkan logic checkout.</span></div></div><div className="mode-grid">{modes.map(x => <button key={x} className={mode === x ? 'mode-active' : ''} onClick={() => save(x)}>{x}</button>)}</div></section>
    <section className={`module-card ${styles.resetCard}`}>
      <div className="module-card-head"><div><strong>Reset data pengujian</strong><span>Untuk membersihkan data demo sebelum input data awal. Akun login, bisnis, cabang, role dan permission tidak dihapus.</span></div></div>
      <div className={styles.resetActions}><button className={styles.resetSecondary} onClick={() => openReset('transactions')}>Reset transaksi</button><button className={styles.resetDanger} onClick={() => openReset('full')}>Reset data full</button></div>
      {message && <div className={styles.resetMessage}>{message}</div>}
    </section>
    {resetMode && <div className={styles.resetOverlay} role="dialog" aria-modal="true" aria-labelledby="reset-title"><div className={styles.resetDialog}>
      <span className="eyebrow">TINDAKAN DESTRUKTIF</span><h2 id="reset-title">{isFull ? 'Reset data full?' : 'Reset semua transaksi?'}</h2>
      <p>{isFull ? 'Produk, pelanggan, supplier, kategori, satuan, harga, lokasi, metode pembayaran, konfigurasi QRIS dan seluruh transaksi bisnis akan dihapus. Struktur bisnis dan akses akun tetap ada.' : 'Penjualan, pembelian, pembayaran, piutang, hutang, penerimaan barang, retur, stock adjustment, transfer stok, saldo stok dan shift kasir akan dihapus. Master tetap ada.'}</p>
      <p className={styles.resetWarning}>Tindakan ini tidak dapat dibatalkan. Ketik <strong>RESET</strong> untuk melanjutkan.</p>
      <input autoFocus value={confirmText} onChange={e => setConfirmText(e.target.value.toUpperCase())} placeholder="Ketik RESET" disabled={busy} />
      <div className={styles.resetDialogActions}><button onClick={closeReset} disabled={busy}>Batal</button><button className={styles.resetDanger} onClick={executeReset} disabled={busy || confirmText !== 'RESET'}>{busy ? 'Memproses…' : isFull ? 'Hapus semua data' : 'Reset transaksi'}</button></div>
    </div></div>}
  </div>
}
