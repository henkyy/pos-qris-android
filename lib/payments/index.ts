'use client'

export type PaymentCode = 'CASH' | 'QRIS' | 'TRANSFER' | 'RECEIVABLE'

export type CheckoutPaymentInput = {
  payment_method_id: string
  code: PaymentCode
  amount: number
  cash_received?: number
  reference?: string
  provider?: string | null
  status?: 'PAID' | 'PENDING'
}

export const PAYMENT_METHODS: Array<{ code: PaymentCode; name: string; offline: boolean; description: string }> = [
  { code: 'CASH', name: 'Tunai', offline: true, description: 'Bayar langsung dan bisa diproses tanpa koneksi.' },
  { code: 'PIUTANG' as PaymentCode, name: 'Piutang', offline: true, description: 'Catat sebagai piutang pelanggan dan sinkronkan saat online.' },
  { code: 'QRIS', name: 'QRIS', offline: false, description: 'Menunggu konfirmasi dari integrasi/provider QRIS.' },
  { code: 'TRANSFER', name: 'Transfer', offline: false, description: 'Catat transfer sebagai pending sampai diverifikasi.' },
]

export function isOfflinePayment(code: PaymentCode) {
  return code === 'CASH' || code === 'RECEIVABLE'
}

export function normalizePaymentCode(code: string): PaymentCode {
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
