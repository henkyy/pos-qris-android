export type Product = { id: string; business_id: string; category_id?: string | null; base_unit_id?: string | null; sku?: string | null; barcode?: string | null; name: string; short_name?: string | null; is_active?: boolean; min_stock?: number | null }
export type Category = { id: string; business_id: string; name: string; is_active?: boolean }
export type Unit = { id: string; business_id: string; name: string; code?: string | null }
export type ProductPrice = { id: string; product_id: string; price_list_id: string; unit_id?: string | null; min_qty?: number; price: number }
