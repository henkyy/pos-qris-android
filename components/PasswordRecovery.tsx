'use client'

import { FormEvent, useEffect, useState } from 'react'
import { requireSupabase } from '../lib/supabase'

const inputStyle = { width: '100%', border: '1px solid #dfe5ee', borderRadius: 10, padding: 11, outline: 'none', boxSizing: 'border-box' as const }
const buttonStyle = { width: '100%', border: 0, borderRadius: 10, padding: 12, background: '#315efb', color: '#fff', fontWeight: 800 }

export default function PasswordRecovery() {
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [valid, setValid] = useState(false)

  useEffect(() => {
    const db = requireSupabase()
    let mounted = true
    const { data: listener } = db.auth.onAuthStateChange((event) => {
      if (mounted && event === 'PASSWORD_RECOVERY') setValid(true)
    })
    if (window.location.hash.includes('type=recovery')) setValid(true)
    db.auth.getSession().then(({ data }) => {
      if (mounted && data.session && window.location.hash.includes('access_token=')) setValid(true)
    })
    return () => {
      mounted = false
      listener.subscription.unsubscribe()
    }
  }, [])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setMessage('')
    if (!valid) return setError('Link reset password tidak valid atau sudah kedaluwarsa.')
    if (password.length < 8) return setError('Password minimal 8 karakter.')
    if (password !== confirmPassword) return setError('Konfirmasi password tidak sama.')
    setBusy(true)
    try {
      const db = requireSupabase()
      const { error: updateError } = await db.auth.updateUser({ password })
      if (updateError) throw updateError
      setMessage('Password berhasil diperbarui. Silakan kembali ke halaman login.')
      setPassword('')
      setConfirmPassword('')
      setTimeout(() => { window.location.href = '/' }, 1200)
    } catch (e: any) {
      setError(e?.message || 'Gagal memperbarui password.')
    } finally {
      setBusy(false)
    }
  }

  return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 20, background: '#f6f8fc' }}>
    <form onSubmit={submit} style={{ width: 'min(430px,100%)', background: '#fff', border: '1px solid #e7ebf2', borderRadius: 18, padding: 24, boxShadow: '0 18px 50px #17203314' }}>
      <div style={{ width: 42, height: 42, display: 'grid', placeItems: 'center', borderRadius: 12, background: '#315efb', color: '#fff', fontWeight: 900, marginBottom: 14 }}>P</div>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>Buat password baru</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Masukkan password baru untuk akun POS QRIS Anda.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      {message && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#176b3a', background: '#eefaf3', border: '1px solid #ccefd9', fontSize: 12 }}>{message}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Password Baru<input type="password" autoComplete="new-password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} style={inputStyle} /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Konfirmasi Password<input type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} style={inputStyle} /></label>
      <button type="submit" disabled={busy || !valid} style={{ ...buttonStyle, opacity: busy || !valid ? .6 : 1 }}>{busy ? 'Menyimpan...' : 'Simpan Password Baru'}</button>
    </form>
  </main>
}
