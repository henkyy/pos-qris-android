'use client'

export type OfflinePaymentCode = 'CASH' | 'RECEIVABLE' | 'QRIS' | 'TRANSFER'

export type OfflineCartItem = {
  product: Record<string, any>
  qty: number
  unit_price: number
}

export type OfflineSale = {
  id: string
  branchId: string
  locationId: string
  customerId: string | null
  items: OfflineCartItem[]
  paymentMethodId: string
  paymentCode?: OfflinePaymentCode
  total: number
  cashReceived: number
  reference?: string
  provider?: string | null
  discountAmount: number
  note: string
  idempotencyKey: string
  createdAt: string
}

type CatalogCache = {
  products: Record<string, any>[]
  categories: Record<string, any>[]
  units: Record<string, any>[]
  prices: Record<string, any>[]
  stock: Record<string, any>[]
  branchId: string
  locationId: string
  cashMethodId: string
  paymentMethods?: Record<string, any>[]
  customers?: Record<string, any>[]
  cachedAt: string
}

const DB_NAME = 'qris-pos-local'
const DB_VERSION = 2
const SALES_STORE = 'pending-sales'
const CATALOG_STORE = 'catalog'
const CATALOG_KEY = 'active'

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !('indexedDB' in window)) {
      reject(new Error('Penyimpanan lokal browser tidak tersedia.'))
      return
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(SALES_STORE)) db.createObjectStore(SALES_STORE, { keyPath: 'id' })
      if (!db.objectStoreNames.contains(CATALOG_STORE)) db.createObjectStore(CATALOG_STORE, { keyPath: 'id' })
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || new Error('Gagal membuka penyimpanan lokal.'))
  })
}

export async function saveCatalogCache(cache: Omit<CatalogCache, 'cachedAt'>) {
  const db = await openDb()
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction(CATALOG_STORE, 'readwrite')
    tx.objectStore(CATALOG_STORE).put({ ...cache, id: CATALOG_KEY, cachedAt: new Date().toISOString() })
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

export async function getCatalogCache(): Promise<CatalogCache | null> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const request = db.transaction(CATALOG_STORE, 'readonly').objectStore(CATALOG_STORE).get(CATALOG_KEY)
    request.onsuccess = () => { db.close(); resolve(request.result || null) }
    request.onerror = () => { db.close(); reject(request.error) }
  })
}

export async function enqueueOfflineSale(sale: OfflineSale) {
  const db = await openDb()
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction(SALES_STORE, 'readwrite')
    tx.objectStore(SALES_STORE).put(sale)
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

export async function getPendingOfflineSales(): Promise<OfflineSale[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const request = db.transaction(SALES_STORE, 'readonly').objectStore(SALES_STORE).getAll()
    request.onsuccess = () => {
      db.close()
      resolve((request.result || []).map((sale: any) => ({
        ...sale,
        paymentCode: sale.paymentCode || 'CASH',
        reference: sale.reference || '',
        provider: sale.provider ?? null,
      })).sort((a: OfflineSale, b: OfflineSale) => a.createdAt.localeCompare(b.createdAt)))
    }
    request.onerror = () => { db.close(); reject(request.error) }
  })
}

export async function removeOfflineSale(id: string) {
  const db = await openDb()
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction(SALES_STORE, 'readwrite')
    tx.objectStore(SALES_STORE).delete(id)
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

export async function countPendingOfflineSales() {
  const sales = await getPendingOfflineSales()
  return sales.length
}
