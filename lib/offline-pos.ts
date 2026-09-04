'use client'

export type OfflinePaymentCode = 'CASH' | 'RECEIVABLE' | 'QRIS' | 'TRANSFER'

export type OfflineCartItem = {
  product: Record<string, any>
  qty: number
  unit_price: number
}

export type OfflineSale = {
  id: string
  businessId: string
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
  businessId: string
  branchId: string
  locationId: string
  cashMethodId: string
  paymentMethods?: Record<string, any>[]
  customers?: Record<string, any>[]
  cachedAt: string
}

export type OfflineScope = {
  businessId?: string
  branchId?: string
  locationId?: string
}

const DB_NAME = 'qris-pos-local'
const DB_VERSION = 3
const SALES_STORE = 'pending-sales'
const CATALOG_STORE = 'catalog'

function catalogKey(scope: { businessId: string; branchId: string; locationId: string }) {
  return `catalog:${scope.businessId}:${scope.branchId}:${scope.locationId}`
}

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
    tx.objectStore(CATALOG_STORE).put({ ...cache, id: catalogKey(cache), cachedAt: new Date().toISOString() })
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

export async function getCatalogCache(scope?: OfflineScope): Promise<CatalogCache | null> {
  if (scope && !scope.businessId && !scope.branchId && !scope.locationId) return null
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const store = db.transaction(CATALOG_STORE, 'readonly').objectStore(CATALOG_STORE)
    const request = scope?.businessId && scope.branchId && scope.locationId
      ? store.get(catalogKey({ businessId: scope.businessId, branchId: scope.branchId, locationId: scope.locationId }))
      : store.getAll()
    request.onsuccess = () => {
      db.close()
      if (scope?.businessId || scope?.branchId || scope?.locationId) {
        const rows = Array.isArray(request.result) ? request.result : request.result ? [request.result] : []
        const match = rows
          .filter((row: any) => Boolean(row.businessId))
          .filter((row: any) => !scope.businessId || row.businessId === scope.businessId)
          .filter((row: any) => !scope.branchId || row.branchId === scope.branchId)
          .filter((row: any) => !scope.locationId || row.locationId === scope.locationId)
          .sort((a: any, b: any) => String(b.cachedAt || '').localeCompare(String(a.cachedAt || '')))[0]
        resolve(match || null)
      } else {
        const rows = Array.isArray(request.result) ? request.result : []
        resolve(rows.sort((a: any, b: any) => String(b.cachedAt || '').localeCompare(String(a.cachedAt || '')))[0] || null)
      }
    }
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

export async function getPendingOfflineSales(scope?: OfflineScope): Promise<OfflineSale[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const request = db.transaction(SALES_STORE, 'readonly').objectStore(SALES_STORE).getAll()
    request.onsuccess = () => {
      db.close()
      const rows = (request.result || [])
        .filter((sale: any) => !scope?.businessId || sale.businessId === scope.businessId)
        .filter((sale: any) => !scope?.branchId || sale.branchId === scope.branchId)
        .filter((sale: any) => !scope?.locationId || sale.locationId === scope.locationId)
        .map((sale: any) => ({
          ...sale,
          paymentCode: sale.paymentCode || 'CASH',
          reference: sale.reference || '',
          provider: sale.provider ?? null,
        }))
        .sort((a: OfflineSale, b: OfflineSale) => a.createdAt.localeCompare(b.createdAt))
      resolve(rows)
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

export async function countPendingOfflineSales(scope?: OfflineScope) {
  const sales = await getPendingOfflineSales(scope)
  return sales.length
}
