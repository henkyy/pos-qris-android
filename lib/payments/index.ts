'use client'

export { PAYMENT_METHODS, getPaymentMethod } from './paymentMethods'
export type { PaymentMethodCode, PaymentMethodDefinition, PaymentMethodAvailability } from './paymentMethods'

export type CheckoutPaymentInput = {
  payment_method_id: string
  code: import('./paymentMethods').PaymentMethodCode
  amount: number
  cash_received?: number
  reference?: string
  provider?: string | null
  status?: 'PAID' | 'PENDING'
}

export function isOfflinePayment(code: import('./paymentMethods').PaymentMethodCode) {
  return code === 'CASH' || code === 'RECEIVABLE'
}

export function normalizePaymentCode(code: string): import('./paymentMethods').PaymentMethodCode {
  if (code === 'PIUTANG' || code === 'RECEIVABLE' || code === 'AR') return 'RECEIVABLE'
  if (code === 'QRIS') return 'QRIS'
  if (code === 'TRANSFER' || code === 'BANK_TRANSFER') return 'TRANSFER'
  return 'CASH'
}

export function validatePayment(input: CheckoutPaymentInput, total: number) {
  if (!input.payment_method_id) return 'Metode pembayaran belum dipilih.'
  if (input.amount <= 0) return 'Nominal pembayaran harus lebih dari 0.'
  if (input.code === 'CASH' && (input.cash_received || 0) < input.amount) return 'Uang tunai kurang dari total pembayaran.'
  if (input.code === 'TRANSFER' && !input.reference?.trim()) return 'Nomor referensi transfer wajib diisi.'
  if (input.code === 'RECEIVABLE' && input.amount !== total) return 'Piutang harus mencakup seluruh total transaksi.'
  return null
}
