export type PaymentMethodCode = 'CASH' | 'RECEIVABLE' | 'QRIS' | 'TRANSFER'

export type PaymentMethodAvailability = 'OFFLINE' | 'PENDING' | 'ONLINE'

export type PaymentMethodDefinition = {
  code: PaymentMethodCode
  name: string
  description: string
  offline: boolean
  label: string
  availability: PaymentMethodAvailability
  requiresReference: boolean
  requiresProviderVerification: boolean
}

export const PAYMENT_METHODS: PaymentMethodDefinition[] = [
  {
    code: 'CASH',
    name: 'Tunai',
    description: 'Bayar langsung dan bisa diproses tanpa koneksi.',
    offline: true,
    label: 'Tunai',
    availability: 'OFFLINE',
    requiresReference: false,
    requiresProviderVerification: false,
  },
  {
    code: 'RECEIVABLE',
    name: 'Piutang',
    description: 'Catat sebagai piutang pelanggan dan sinkronkan saat online.',
    offline: true,
    label: 'Piutang',
    availability: 'OFFLINE',
    requiresReference: false,
    requiresProviderVerification: false,
  },
  {
    code: 'QRIS',
    name: 'QRIS',
    description: 'Menunggu konfirmasi dari integrasi/provider QRIS.',
    offline: false,
    label: 'QRIS',
    availability: 'ONLINE',
    requiresReference: false,
    requiresProviderVerification: true,
  },
  {
    code: 'TRANSFER',
    name: 'Transfer',
    description: 'Catat transfer sebagai pending sampai diverifikasi.',
    offline: false,
    label: 'Transfer',
    availability: 'PENDING',
    requiresReference: true,
    requiresProviderVerification: false,
  },
]

export const getPaymentMethod = (code: PaymentMethodCode) => PAYMENT_METHODS.find(method => method.code === code)
