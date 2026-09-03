export type PaymentMethod = { id: string; business_id: string; code: string; name: string; provider?: string | null; is_active?: boolean }
export type Payment = { id: string; payment_no?: string; sale_id?: string | null; payment_method_id?: string; amount: number; status?: string; provider?: string | null; created_at?: string }
