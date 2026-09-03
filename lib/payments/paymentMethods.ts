export type PaymentMethodCode = 'CASH' | 'QRIS' | 'TRANSFER'

export type PaymentMethodAvailability = 'OFFLINE' | 'PENDING' | 'ONLINE'

export type PaymentMethodDefinition = {
  code: PaymentMethodCode
  label: string
  availability: PaymentMethodAvailability
  requiresReference: boolean
  requiresProviderVerification: boolean
}

export const PAYMENT_METHODS: PaymentMethodDefinition[] = [
  {
    code: 'CASH',
    label: 'Tunai',
    availability: 'OFFLINE',
    requiresReference: false,
    requiresProviderVerification: false,
  },
  {
    code: 'QRIS',
    label: 'QRIS',
    availability: 'ONLINE',
    requiresReference: false,
    requiresProviderVerification: true,
  },
  {
    code: 'TRANSFER',
    label: 'Transfer',
    availability: 'PENDING',
    requiresReference: true,
    requiresProviderVerification: false,
  },
]

export const getPaymentMethod = (code: PaymentMethodCode) =>
  PAYMENT_METHODS.find(method => method.code === code)
