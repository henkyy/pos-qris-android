'use client'

import { useEffect, useState } from 'react'
import AuthGate from '../components/AuthGate'
import PasswordRecovery from '../components/PasswordRecovery'

export default function HomePage() {
  const [recovery, setRecovery] = useState(false)

  useEffect(() => {
    const detect = () => setRecovery(window.location.hash.includes('type=recovery'))
    detect()
    window.addEventListener('hashchange', detect)
    return () => window.removeEventListener('hashchange', detect)
  }, [])

  if (recovery) return <PasswordRecovery />
  return <AuthGate />
}
