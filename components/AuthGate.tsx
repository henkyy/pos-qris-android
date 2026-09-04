'use client'

import { FormEvent, useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import AppShell from './layout/AppShell'
import { requireSupabase } from '../lib/supabase'

type Mode = 'login' | 'register' | 'forgot' | 'reset' | 'setup'

const cardStyle = { width: 'min(430px,100%)', background: '#fff', border: '1px solid #e7ebf2', borderRadius: 18, padding: 24, boxShadow: '0 18px 50px #17203314' }
const inputStyle = { width: '100%', border: '1px solid #dfe5ee', borderRadius: 10, padding: 11, outline: 'none', boxSizing: 'border-box' as const }
const buttonStyle = { width: '100%', border: 0, borderRadius: 10, padding: 12, background: '#315efb', color: '#fff', fontWeight: 800 }

export default function AuthGate() {
  const [session, setSession] = useState<Session | null>(null)
  const [hasBusiness, setHasBusiness] = useState<boolean | null>(null)
  const [checking, setChecking] = useState(true)
  const [mode, setMode] = useState<Mode>('login')
  const [fullName, setFullName] = useState('')
  const [businessName, setBusinessName] = useState('')
  const [branchName, setBranchName] = useState('Cabang Utama')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const db = requireSupabase()
    let mounted = true

    const check = async (nextSession: Session | null) => {
      if (!mounted) return
      setSession(nextSession)
      if (!nextSession) {
        setHasBusiness(null)
        setChecking(false)
        return
      }
      const { data, error: membershipError } = await db
        .from('business_users')
        .select('business_id')
        .eq('user_id', nextSession.user.id)
        .eq('is_active', true)
        .limit(1)
      if (!mounted) return
      if (membershipError) {
        setError(membershipError.message)
        setHasBusiness(false)
      } else {
        setHasBusiness(Boolean(data?.length))
      }
      setChecking(false)
    }

    db.auth.getSession().then(({ data, error: sessionError }) => {
      if (sessionError && mounted) setError(sessionError.message)
      void check(data.session)
    })

    const { data: listener } = db.auth.onAuthStateChange((_event, nextSession) => {
      void check(nextSession)
    })

    return () => {
      mounted = false
      listener.subscription.unsubscribe()
    }
  }, [])

  function clearFeedback() {
    setError('')
    setMessage('')
  }

  function switchMode(next: Mode) {
    clearFeedback()
    setMode(next)
    setPassword('')
    setConfirmPassword('')
  }

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    setBusy(true)
    try {
      const db = requireSupabase()
      const { data, error: signInError } = await db.auth.signInWithPassword({ email: email.trim(), password })
      if (signInError) throw signInError
      if (!data.session) throw new Error('Login berhasil tetapi session Supabase belum tersedia.')
      setSession(data.session)
    } catch (e: any) {
      setError(e?.message || 'Login gagal.')
    } finally {
      setBusy(false)
    }
  }

  async function register(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    if (password.length < 8) return setError('Password minimal 8 karakter.')
    if (password !== confirmPassword) return setError('Konfirmasi password tidak sama.')
    if (!businessName.trim()) return setError('Nama bisnis wajib diisi.')
    setBusy(true)
    try {
      const db = requireSupabase()
      const { data, error: signUpError } = await db.auth.signUp({
        email: email.trim(),
        password,
        options: { data: { full_name: fullName.trim() } },
      })
      if (signUpError) throw signUpError
      if (!data.user) throw new Error('Akun gagal dibuat.')
      if (!data.session) {
        setMessage('Akun berhasil dibuat. Cek email untuk verifikasi, lalu login kembali untuk menyelesaikan setup bisnis.')
        setMode('login')
        return
      }
      const { error: bootstrapError } = await db.rpc('bootstrap_first_business', {
        p_business_name: businessName.trim(),
        p_branch_name: branchName.trim() || 'Cabang Utama',
      })
      if (bootstrapError) throw bootstrapError
      setSession(data.session)
      setHasBusiness(true)
    } catch (e: any) {
      setError(e?.message || 'Pendaftaran gagal.')
    } finally {
      setBusy(false)
    }
  }

  async function forgotPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    setBusy(true)
    try {
      const db = requireSupabase()
      const { error: resetError } = await db.auth.resetPasswordForEmail(email.trim(), {
        redirectTo: window.location.origin,
      })
      if (resetError) throw resetError
      setMessage('Jika email terdaftar, link reset password telah dikirim. Periksa inbox dan folder spam.')
    } catch (e: any) {
      setError(e?.message || 'Gagal mengirim link reset password.')
    } finally {
      setBusy(false)
    }
  }

  async function updatePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    if (password.length < 8) return setError('Password minimal 8 karakter.')
    if (password !== confirmPassword) return setError('Konfirmasi password tidak sama.')
    setBusy(true)
    try {
      const db = requireSupabase()
      const { error: updateError } = await db.auth.updateUser({ password })
      if (updateError) throw updateError
      setMessage('Password berhasil diperbarui. Anda sudah dapat menggunakan password baru.')
      setMode('login')
      setPassword('')
      setConfirmPassword('')
    } catch (e: any) {
      setError(e?.message || 'Gagal memperbarui password.')
    } finally {
      setBusy(false)
    }
  }

  async function setupBusiness(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    if (!businessName.trim()) return setError('Nama bisnis wajib diisi.')
    setBusy(true)
    try {
      const db = requireSupabase()
      const { error: bootstrapError } = await db.rpc('bootstrap_first_business', {
        p_business_name: businessName.trim(),
        p_branch_name: branchName.trim() || 'Cabang Utama',
      })
      if (bootstrapError) throw bootstrapError
      setHasBusiness(true)
    } catch (e: any) {
      setError(e?.message || 'Setup bisnis gagal.')
    } finally {
      setBusy(false)
    }
  }

  if (checking) {
    return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: '#f6f8fc', color: '#536176' }}>Memeriksa sesi login...</main>
  }

  if (session && hasBusiness === false) {
    return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 20, background: '#f6f8fc' }}><form onSubmit={setupBusiness} style={cardStyle}>
      <div style={{ width: 42, height: 42, display: 'grid', placeItems: 'center', borderRadius: 12, background: '#315efb', color: '#fff', fontWeight: 900, marginBottom: 14 }}>P</div>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>Siapkan bisnis Anda</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Satu akun dapat memiliki akses bisnis dan cabang melalui membership yang aman.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Nama Bisnis<input required value={businessName} onChange={e => setBusinessName(e.target.value)} style={inputStyle} placeholder="Toko Maju Jaya" /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Cabang Utama<input value={branchName} onChange={e => setBranchName(e.target.value)} style={inputStyle} /></label>
      <button type="submit" disabled={busy} style={buttonStyle}>{busy ? 'Menyiapkan...' : 'Mulai Menggunakan POS'}</button>
    </form></main>
  }

  if (session && hasBusiness) return <AppShell />

  return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 20, background: '#f6f8fc' }}>
    {mode === 'login' && <form onSubmit={signIn} style={cardStyle}>
      <div style={{ width: 42, height: 42, display: 'grid', placeItems: 'center', borderRadius: 12, background: '#315efb', color: '#fff', fontWeight: 900, marginBottom: 14 }}>P</div>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>POS QRIS</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Masuk untuk mengakses data bisnis dan transaksi.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      {message && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#176b3a', background: '#eefaf3', border: '1px solid #ccefd9', fontSize: 12 }}>{message}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Email<input type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} style={inputStyle} /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 8, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Password<input type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} style={inputStyle} /></label>
      <div style={{ textAlign: 'right', marginBottom: 16 }}><button type="button" onClick={() => switchMode('forgot')} style={{ border: 0, background: 'transparent', padding: 0, color: '#315efb', fontSize: 12, cursor: 'pointer' }}>Lupa password?</button></div>
      <button type="submit" disabled={busy} style={buttonStyle}>{busy ? 'Memproses...' : 'Masuk'}</button>
      <p style={{ textAlign: 'center', color: '#718096', fontSize: 12, margin: '18px 0 0' }}>Belum punya akun? <button type="button" onClick={() => switchMode('register')} style={{ border: 0, background: 'transparent', padding: 0, color: '#315efb', fontWeight: 800, cursor: 'pointer' }}>Daftar</button></p>
    </form>}

    {mode === 'register' && <form onSubmit={register} style={cardStyle}>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>Buat akun</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Akun baru langsung disiapkan untuk bisnis pertama Anda.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      {message && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#176b3a', background: '#eefaf3', border: '1px solid #ccefd9', fontSize: 12 }}>{message}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Nama Lengkap<input required value={fullName} onChange={e => setFullName(e.target.value)} style={inputStyle} placeholder="Henky Lola" /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Email<input type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} style={inputStyle} /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Password<input type="password" autoComplete="new-password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} style={inputStyle} placeholder="Minimal 8 karakter" /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Konfirmasi Password<input type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} style={inputStyle} /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Nama Bisnis<input required value={businessName} onChange={e => setBusinessName(e.target.value)} style={inputStyle} placeholder="Toko Maju Jaya" /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Cabang Utama<input value={branchName} onChange={e => setBranchName(e.target.value)} style={inputStyle} /></label>
      <button type="submit" disabled={busy} style={buttonStyle}>{busy ? 'Membuat akun...' : 'Daftar & Siapkan POS'}</button>
      <p style={{ textAlign: 'center', color: '#718096', fontSize: 12, margin: '18px 0 0' }}><button type="button" onClick={() => switchMode('login')} style={{ border: 0, background: 'transparent', padding: 0, color: '#315efb', fontWeight: 800, cursor: 'pointer' }}>Kembali ke login</button></p>
    </form>}

    {mode === 'forgot' && <form onSubmit={forgotPassword} style={cardStyle}>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>Lupa password?</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Masukkan email akun. Kami akan mengirim link untuk membuat password baru.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      {message && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#176b3a', background: '#eefaf3', border: '1px solid #ccefd9', fontSize: 12 }}>{message}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Email<input type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} style={inputStyle} /></label>
      <button type="submit" disabled={busy} style={buttonStyle}>{busy ? 'Mengirim...' : 'Kirim Link Reset'}</button>
      <p style={{ textAlign: 'center', margin: '18px 0 0' }}><button type="button" onClick={() => switchMode('login')} style={{ border: 0, background: 'transparent', color: '#315efb', fontWeight: 800, cursor: 'pointer' }}>Kembali ke login</button></p>
    </form>}

    {mode === 'reset' && <form onSubmit={updatePassword} style={cardStyle}>
      <h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>Buat password baru</h1>
      <p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Gunakan minimal 8 karakter dan jangan gunakan password yang sama dengan layanan lain.</p>
      {error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}
      <label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Password Baru<input type="password" autoComplete="new-password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} style={inputStyle} /></label>
      <label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Konfirmasi Password<input type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} style={inputStyle} /></label>
      <button type="submit" disabled={busy} style={buttonStyle}>{busy ? 'Menyimpan...' : 'Simpan Password Baru'}</button>
    </form>}
  </main>
}
