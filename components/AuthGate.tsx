'use client'

import { FormEvent, useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import AppShell from './layout/AppShell'
import { requireSupabase } from '../lib/supabase'

export default function AuthGate() {
  const [session, setSession] = useState<Session | null>(null)
  const [checking, setChecking] = useState(true)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const db = requireSupabase()
    let mounted = true

    db.auth.getSession().then(({ data, error: sessionError }) => {
      if (!mounted) return
      if (sessionError) setError(sessionError.message)
      setSession(data.session)
      setChecking(false)
    })

    const { data: listener } = db.auth.onAuthStateChange((_event, nextSession) => {
      if (!mounted) return
      setSession(nextSession)
      setChecking(false)
      if (nextSession) setError('')
    })

    return () => {
      mounted = false
      listener.subscription.unsubscribe()
    }
  }, [])

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
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

  if (checking) {
    return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: '#f6f8fc', color: '#536176' }}>Memeriksa sesi login...</main>
  }

  if (!session) {
    return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 20, background: '#f6f8fc' }}><form onSubmit={signIn} style={{ width: 'min(410px,100%)', background: '#fff', border: '1px solid #e7ebf2', borderRadius: 18, padding: 24, boxShadow: '0 18px 50px #17203314' }}><div style={{ width: 42, height: 42, display: 'grid', placeItems: 'center', borderRadius: 12, background: '#315efb', color: '#fff', fontWeight: 900, marginBottom: 14 }}>P</div><h1 style={{ margin: 0, fontSize: 25, color: '#172033' }}>POS QRIS</h1><p style={{ margin: '6px 0 20px', color: '#718096', fontSize: 13 }}>Masuk untuk mengakses data bisnis dan transaksi.</p>{error && <div style={{ padding: '10px 12px', borderRadius: 10, marginBottom: 12, color: '#a42a2a', background: '#fff1f1', border: '1px solid #ffdada', fontSize: 12 }}>{error}</div>}<label style={{ display: 'grid', gap: 6, marginBottom: 12, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Email<input type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} style={{ width: '100%', border: '1px solid #dfe5ee', borderRadius: 10, padding: 11, outline: 'none' }} /></label><label style={{ display: 'grid', gap: 6, marginBottom: 16, color: '#58657a', fontSize: 11, fontWeight: 700 }}>Password<input type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} style={{ width: '100%', border: '1px solid #dfe5ee', borderRadius: 10, padding: 11, outline: 'none' }} /></label><button type="submit" disabled={busy} style={{ width: '100%', border: 0, borderRadius: 10, padding: 12, background: '#315efb', color: '#fff', fontWeight: 800 }}>{busy ? 'Memproses...' : 'Masuk'}</button></form></main>
  }

  return <AppShell />
}
