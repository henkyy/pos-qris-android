export type StockBalance = { id: string; product_id: string; location_id?: string; qty_base: number; min_stock?: number }
export type StockMovement = { id: string; product_id: string; location_id?: string; qty: number; movement_type?: string; created_at?: string }
