export type SaleStatus = 'draft' | 'paid' | 'void' | 'refunded'
export type Sale = { id: string; sale_no?: string; branch_id?: string; customer_id?: string | null; total?: number; status?: SaleStatus; created_at?: string }
export type SaleItem = { id: string; sale_id: string; product_id: string; qty: number; unit_price: number }
