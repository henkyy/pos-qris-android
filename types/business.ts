export type Business = { id: string; code?: string; name: string; is_active?: boolean }
export type Branch = { id: string; business_id: string; code?: string; name: string; is_active?: boolean }
export type Location = { id: string; branch_id: string; code?: string; name: string; is_active?: boolean }
