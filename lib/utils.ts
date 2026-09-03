export const formatIdr = (value: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(Math.round(value || 0))
export const formatNumber = (value: number) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 }).format(Math.round(value || 0))
export const safeText = (value: unknown) => String(value ?? '')
