export type CheckoutLine = { product_id: string; unit_id: string; qty: number; unit_price: number }
export type CheckoutPayment = { payment_method_id: string; amount: number; cash_received?: number; provider?: string }
export const checkoutRpcName = 'checkout_sale_multi_payment_v2'
