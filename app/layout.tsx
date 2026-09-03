import type { Metadata } from 'next'
import { Manrope } from 'next/font/google'
import './globals.css'

const manrope = Manrope({ subsets: ['latin'], display: 'swap' })

export const metadata: Metadata = {
  title: 'POS QRIS',
  description: 'Point of Sale berbasis web untuk HP dan tablet',
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="id"><body className={manrope.className}>{children}</body></html>
}
