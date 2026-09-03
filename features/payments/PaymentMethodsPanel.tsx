'use client'

import { PAYMENT_METHODS } from '../../lib/payments'

const icons: Record<string, string> = { CASH: 'Rp', RECEIVABLE: 'AR', QRIS: 'QR', TRANSFER: 'TR' }

export default function PaymentMethodsPanel() {
  return <section className="module-card payment-methods-card">
    <div className="module-card-head">
      <div><strong>Metode pembayaran POS</strong><span>Empat metode inti dengan perilaku online/offline yang berbeda.</span></div>
    </div>
    <div className="payment-method-grid">
      {PAYMENT_METHODS.map(method => <article className="payment-method-item" key={method.code}>
        <div className="payment-method-icon">{icons[method.code]}</div>
        <div><strong>{method.name}</strong><p>{method.description}</p><small>{method.offline ? 'Bisa offline' : 'Memerlukan koneksi / verifikasi provider'}</small></div>
      </article>)}
    </div>
  </section>
}
