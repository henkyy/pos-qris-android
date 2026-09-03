export type PaymentMethodCode = 'CASH' | 'RECEIVABLE' | 'QRIS' | 'TRANSFER'
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED'

export type PaymentMethod = {
  id: string
  business_id: string
  code: PaymentMethodCode | string
  name: string
  method_type?: string
  provider?: string | null
  is_active?: boolean
}

export type Payment = {
  id: string
  payment_no?: string
  sale_id?: string | null
  payment_method_id?: string
  amount: number
  status?: PaymentStatus | string
  provider?: string | null
  external_transaction_id?: string | null
  reference?: string | null
  created_at?: string
}
