import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'POS QRIS',
  description: 'Point of Sale berbasis web untuk HP dan tablet',
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="id"><body>{children}</body></html>
}
