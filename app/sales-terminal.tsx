'use client'

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { requireSupabase } from '../lib/supabase'
import { enqueueOfflineSale, getCatalogCache, getPendingOfflineSales, removeOfflineSale, saveCatalogCache, type OfflineSale, type OfflinePaymentCode } from '../lib/offline-pos'
import { normalizePaymentCode, type PaymentCode } from '../lib/payments'
import styles from './sales.module.css'

type Row = Record<string, any>
type CartItem = { product: Row; qty: number }
type HeldOrder = { id: string; name: string; items: CartItem[]; note: string; createdAt: string }
type Toast = { id: number; type: 'success' | 'error' | 'info'; title: string; text: string }
type ViewMode = 'retail' | 'distributor' | 'fnb'

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(Math.max(0, Math.round(n || 0)))
const number = (n: number) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 }).format(Math.max(0, Math.round(n || 0)))
const text = (v: unknown) => String(v ?? '')
const modes: { id: ViewMode; label: string; icon: string }[] = [
  { id: 'retail', label: 'Retail', icon: '▦' },
  { id: 'distributor', label: 'Distributor', icon: '▤' },
  { id: 'fnb', label: 'F&B', icon: '♨' },
]

const paymentLabels: Record<PaymentCode, string> = { CASH: 'Tunai', RECEIVABLE: 'Piutang', QRIS: 'QRIS', TRANSFER: 'Transfer' }

export default function SalesTerminal() {
  const [products, setProducts] = useState<Row[]>([])
  const [categories, setCategories] = useState<Row[]>([])
  const [units, setUnits] = useState<Row[]>([])
  const [prices, setPrices] = useState<Row[]>([])
  const [stock, setStock] = useState<Row[]>([])
  const [customers, setCustomers] = useState<Row[]>([])
  const [paymentMethods, setPaymentMethods] = useState<Row[]>([])
  const [branchId, setBranchId] = useState('')
  const [locationId, setLocationId] = useState('')
  const [paymentMethodId, setPaymentMethodId] = useState('')
  const [paymentCode, setPaymentCode] = useState<PaymentCode>('CASH')
  const [customerId, setCustomerId] = useState('')
  const [reference, setReference] = useState('')
  const [provider, setProvider] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [cash, setCash] = useState('')
  const [note, setNote] = useState('')
  const [discount, setDiscount] = useState('')
  const [discountMode, setDiscountMode] = useState<'percent' | 'amount'>('percent')
  const [heldOrders, setHeldOrders] = useState<HeldOrder[]>([])
  const [showHeld, setShowHeld] = useState(false)
  const [showPayment, setShowPayment] = useState(false)
  const [showReceipt, setShowReceipt] = useState(false)
  const [lastSale, setLastSale] = useState<Row | null>(null)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)
  const [toasts, setToasts] = useState<Toast[]>([])
  const [mode, setMode] = useState<ViewMode>('retail')
  const [online, setOnline] = useState(true)
  const [pendingCount, setPendingCount] = useState(0)
  const searchRef = useRef<HTMLInputElement>(null)
  const catalogRef = useRef<HTMLDivElement>(null)
  const toastId = useRef(0)

  const toast = useCallback((type: Toast['type'], title: string, value: string) => {
    const id = ++toastId.current
    setToasts(prev => [...prev, { id, type, title, text: value }])
    window.setTimeout(() => setToasts(prev => prev.filter(x => x.id !== id)), 3600)
  }, [])
  const dismissToast = useCallback((id: number) => setToasts(prev => prev.filter(x => x.id !== id)), [])
  const categoryName = useCallback((id: string) => text(categories.find(x => x.id === id)?.name || 'Tanpa kategori'), [categories])
  const unitName = useCallback((id: string) => text(units.find(x => x.id === id)?.name || 'Satuan'), [units])
  const priceFor = useCallback((product: Row, qty: number) => Number(prices.filter(x => x.product_id === product.id && x.unit_id === product.base_unit_id && Number(x.min_qty) <= qty).sort((a, b) => Number(b.min_qty) - Number(a.min_qty))[0]?.price || 0), [prices])
  const stockFor = useCallback((productId: string) => Number(stock.find(x => x.product_id === productId)?.qty_base || 0), [stock])
  const totalQty = cart.reduce((sum, item) => sum + item.qty, 0)
  const subtotal = cart.reduce((sum, item) => sum + priceFor(item.product, item.qty) * item.qty, 0)
  const discountValue = discountMode === 'percent' ? Math.min(subtotal, subtotal * Math.max(0, Math.min(100, Number(discount) || 0)) / 100) : Math.min(subtotal, Math.max(0, Number(discount) || 0))
  const total = Math.max(0, subtotal - discountValue)
  const received = Number(cash || 0)
  const change = received - total
  const selectedMethod = paymentMethods.find(x => x.id === paymentMethodId)
  const selectedCode = normalizePaymentCode(text(selectedMethod?.code || selectedMethod?.method_type || paymentCode))
  const needsCash = selectedCode === 'CASH'
  const needsCustomer = selectedCode === 'RECEIVABLE'
  const needsReference = selectedCode === 'TRANSFER'
  const methodIsOffline = selectedCode === 'CASH' || selectedCode === 'RECEIVABLE'
  const modeLabel = modes.find(x => x.id === mode)?.label || 'Retail'
  const modeDescription = mode === 'distributor' ? 'Penjualan grosir · kuantitas & harga bertingkat' : mode === 'fnb' ? 'Kasir F&B · pesanan cepat & catatan' : 'Kasir retail · transaksi cepat'

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return products.filter(p => {
      const haystack = [p.name, p.short_name, p.sku, p.barcode, categoryName(p.category_id)].map(text).join(' ').toLowerCase()
      return (!q || haystack.includes(q)) && (!categoryFilter || p.category_id === categoryFilter)
    })
  }, [products, search, categoryFilter, categoryName])

  const refreshPending = useCallback(async () => {
    try { setPendingCount((await getPendingOfflineSales()).length) } catch {}
  }, [])

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      if (!navigator.onLine) throw new Error('OFFLINE_MODE')
      const db = requireSupabase()
      const { data: businesses, error: be } = await db.from('businesses').select('*').eq('code', 'TOKO_MAJU_JAYA').limit(1)
      if (be) throw be
      const business = businesses?.[0]
      if (!business) throw new Error('Business TOKO_MAJU_JAYA tidak ditemukan.')
      const [{ data: ps, error: pe }, { data: cs, error: ce }, { data: us, error: ue }, { data: bs, error: se }, { data: branches, error: bre }, { data: cus, error: cue }, { data: methods, error: me }] = await Promise.all([
        db.from('products').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('categories').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('units').select('*').eq('business_id', business.id).order('name'),
        db.from('stock_balances').select('*'),
        db.from('branches').select('*').eq('business_id', business.id).eq('is_active', true).limit(1),
        db.from('customers').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('payment_methods').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
      ])
      if (pe || ce || ue || se || bre || cue || me) throw pe || ce || ue || se || bre || cue || me
      const branch = branches?.[0]
      if (!branch) throw new Error('Cabang aktif tidak ditemukan.')
      const { data: locations, error: le } = await db.from('locations').select('*').eq('branch_id', branch.id).eq('is_active', true).limit(1)
      if (le) throw le
      const locationIdNext = locations?.[0]?.id || ''
      const { data: pls, error: ple } = await db.from('price_lists').select('*').eq('business_id', business.id).eq('is_default', true).eq('is_active', true).limit(1)
      if (ple) throw ple
      const pl = pls?.[0]
      const { data: prs, error: pre } = pl ? await db.from('product_prices').select('*').eq('price_list_id', pl.id) : { data: [], error: null }
      if (pre) throw pre
      const usable = (methods || []).filter(m => ['CASH','RECEIVABLE','PIUTANG','AR','QRIS','TRANSFER','BANK_TRANSFER'].includes(text(m.code).toUpperCase()) || ['CASH','RECEIVABLE','PIUTANG','QRIS','TRANSFER','BANK_TRANSFER'].includes(text(m.method_type).toUpperCase()))
      const cash = usable.find(m => normalizePaymentCode(text(m.code || m.method_type)) === 'CASH')
      const defaultMethod = cash || usable[0]
      setProducts(ps || []); setCategories(cs || []); setUnits(us || []); setStock(bs || []); setCustomers(cus || []); setPaymentMethods(usable); setBranchId(branch.id); setLocationId(locationIdNext); setPrices(prs || []); setPaymentMethodId(defaultMethod?.id || ''); setPaymentCode(normalizePaymentCode(text(defaultMethod?.code || defaultMethod?.method_type || 'CASH'))); setOnline(true)
      await saveCatalogCache({ products: ps || [], categories: cs || [], units: us || [], prices: prs || [], stock: bs || [], branchId: branch.id, locationId: locationIdNext, cashMethodId: cash?.id || '', paymentMethods: usable, customers: cus || [] } as any)
    } catch (e: any) {
      const cached = await getCatalogCache().catch(() => null)
      if (cached) {
        setProducts(cached.products || []); setCategories(cached.categories || []); setUnits(cached.units || []); setPrices(cached.prices || []); setStock(cached.stock || []); setCustomers((cached as any).customers || []); setPaymentMethods((cached as any).paymentMethods || []); setBranchId(cached.branchId || ''); setLocationId(cached.locationId || '')
        const cash = ((cached as any).paymentMethods || []).find((m: Row) => normalizePaymentCode(text(m.code || m.method_type)) === 'CASH')
        setPaymentMethodId(cash?.id || cached.cashMethodId || ''); setPaymentCode('CASH'); setOnline(false)
        if (e?.message !== 'OFFLINE_MODE') toast('info', 'Mode offline', 'Server tidak dapat dihubungi. Katalog, stok, pelanggan, dan metode tersimpan lokal tetap digunakan.')
      } else toast('error', 'Gagal memuat data', e.message || 'Periksa koneksi Supabase.')
    } finally { if (!silent) setLoading(false) }
  }, [toast])

  const syncPending = useCallback(async () => {
    if (!navigator.onLine) return
    const pending = await getPendingOfflineSales()
    if (!pending.length) { setPendingCount(0); return }
    let synced = 0
    for (const sale of pending) {
      try {
        const db = requireSupabase()
        const { error } = await db.rpc('checkout_sale_multi_payment_v2', {
          p_branch_id: sale.branchId,
          p_location_id: sale.locationId,
          p_customer_id: sale.customerId,
          p_items: sale.items.map(item => ({ product_id: item.product.id, unit_id: item.product.base_unit_id, qty: item.qty, unit_price: item.unit_price })),
          p_payments: [{ payment_method_id: sale.paymentMethodId, amount: Math.round(sale.total), cash_received: sale.cashReceived, reference: sale.reference || null, provider: sale.provider || null }],
          p_discount_amount: Math.round(sale.discountAmount),
          p_idempotency_key: sale.idempotencyKey,
        })
        if (error) throw error
        await removeOfflineSale(sale.id)
        synced++
      } catch (e: any) {
        toast('error', 'Sinkronisasi tertunda', e.message || 'Data offline belum bisa dikirim ke server.')
        break
      }
    }
    await refreshPending()
    if (synced) { await load(true); toast('success', 'Data tersinkron', `${synced} transaksi offline berhasil dikirim ke Supabase.`) }
  }, [load, refreshPending, toast])

  useEffect(() => {
    setOnline(navigator.onLine); load(); refreshPending()
    try { const savedHeld = window.localStorage.getItem('qris-held-orders'); if (savedHeld) setHeldOrders(JSON.parse(savedHeld)); const savedMode = window.localStorage.getItem('qris-view-mode') as ViewMode | null; if (savedMode && modes.some(x => x.id === savedMode)) setMode(savedMode) } catch {}
    const onOffline = () => { setOnline(false); toast('info', 'Koneksi offline', 'Tunai dan piutang dapat disimpan lokal. QRIS dan transfer menunggu koneksi.') }
    const onOnline = async () => { setOnline(true); await syncPending(); await load(true) }
    window.addEventListener('offline', onOffline); window.addEventListener('online', onOnline)
    return () => { window.removeEventListener('offline', onOffline); window.removeEventListener('online', onOnline) }
  }, [load, refreshPending, syncPending, toast])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); searchRef.current?.focus() }; if (e.key === 'Escape') { setShowPayment(false); setShowHeld(false); setShowReceipt(false) }; if (e.key === 'F4' && cart.length) { e.preventDefault(); setShowPayment(true) }; if (e.key === 'F7' && cart.length) { e.preventDefault(); holdOrder() } }
    window.addEventListener('keydown', onKey); return () => window.removeEventListener('keydown', onKey)
  }, [cart.length])

  function add(product: Row) { const scroll = catalogRef.current?.scrollTop || 0; const available = stockFor(product.id); if (available <= 0) return toast('error', 'Stok habis', `${product.name} tidak memiliki stok tersedia.`); setCart(prev => prev.some(x => x.product.id === product.id) ? prev.map(x => x.product.id === product.id ? { ...x, qty: Math.min(x.qty + 1, available) } : x) : [...prev, { product, qty: 1 }]); requestAnimationFrame(() => { if (catalogRef.current) catalogRef.current.scrollTop = scroll }) }
  function changeQty(id: string, delta: number) { setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty: Math.min(stockFor(id), Math.max(0, x.qty + delta)) } : x).filter(x => x.qty > 0)) }
  function setQty(id: string, raw: string) { const qty = Math.max(0, Math.min(stockFor(id), Number(raw) || 0)); setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty } : x).filter(x => x.qty > 0)) }
  function clearCart() { setCart([]); setCash(''); setReference(''); setCustomerId(''); setNote(''); setDiscount(''); setShowPayment(false) }
  function holdOrder() { if (!cart.length) return; const order: HeldOrder = { id: crypto.randomUUID(), name: `Pesanan ${heldOrders.length + 1}`, items: cart, note, createdAt: new Date().toISOString() }; const next = [order, ...heldOrders]; setHeldOrders(next); window.localStorage.setItem('qris-held-orders', JSON.stringify(next)); clearCart(); toast('success', 'Pesanan ditahan', 'Pesanan bisa dilanjutkan dari daftar pesanan ditahan.') }
  function resumeOrder(order: HeldOrder) { setCart(order.items); setNote(order.note); const next = heldOrders.filter(x => x.id !== order.id); setHeldOrders(next); setShowHeld(false); window.localStorage.setItem('qris-held-orders', JSON.stringify(next)); toast('info', 'Pesanan dilanjutkan', `${order.name} kembali ke kasir.`) }

  function localSaleFromCart(idempotencyKey?: string): OfflineSale { return { id: crypto.randomUUID(), branchId, locationId, customerId: customerId || null, items: cart.map(({ product, qty }) => ({ product, qty, unit_price: priceFor(product, qty) })), paymentMethodId, paymentCode: selectedCode as OfflinePaymentCode, total: Math.round(total), cashReceived: needsCash ? received : 0, reference: needsReference ? reference.trim() : '', provider, discountAmount: Math.round(discountValue), note, idempotencyKey: idempotencyKey || crypto.randomUUID(), createdAt: new Date().toISOString() } }

  function validateCheckout() {
    if (!branchId || !locationId || !paymentMethodId || !cart.length) return 'Data checkout belum lengkap.'
    if (!methodIsOffline && !online) return `${paymentLabels[selectedCode]} tidak tersedia saat offline.`
    if (needsCash && received < total) return `Uang diterima harus minimal ${money(total)}.`
    if (needsCustomer && !customerId) return 'Pelanggan wajib dipilih untuk transaksi piutang.'
    if (needsReference && !reference.trim()) return 'Nomor referensi transfer wajib diisi.'
    return null
  }

  async function checkoutOffline(idempotencyKey?: string) {
    const validation = validateCheckout(); if (validation) return toast('error', 'Pembayaran belum lengkap', validation)
    if (!methodIsOffline) return toast('error', 'Tidak tersedia offline', `${paymentLabels[selectedCode]} memerlukan koneksi atau verifikasi provider.`)
    try {
      const sale = localSaleFromCart(idempotencyKey); await enqueueOfflineSale(sale)
      const nextStock = stock.map(row => cart.some(item => item.product.id === row.product_id) ? { ...row, qty_base: Number(row.qty_base || 0) - (cart.find(item => item.product.id === row.product_id)?.qty || 0) } : row)
      setStock(nextStock); await saveCatalogCache({ products, categories, units, prices, stock: nextStock, branchId, locationId, cashMethodId: paymentMethods.find(m => normalizePaymentCode(text(m.code || m.method_type)) === 'CASH')?.id || '', paymentMethods, customers } as any)
      setLastSale({ sale_no: `OFF-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`, items: cart, received: needsCash ? received : 0, subtotal, discountAmount: Math.round(discountValue), total, note, offline: true, paymentCode: selectedCode, customerId })
      await refreshPending(); setShowPayment(false); setShowReceipt(true); clearCart(); toast('success', `${paymentLabels[selectedCode]} tersimpan offline`, 'Transaksi dan pengurangan stok disimpan di perangkat dan akan disinkronkan saat online.')
    } catch (e: any) { toast('error', 'Gagal menyimpan offline', e.message || 'Browser tidak mengizinkan penyimpanan lokal.') }
  }

  async function checkout() {
    const validation = validateCheckout(); if (validation) return toast('error', 'Pembayaran belum lengkap', validation)
    if (!online) return checkoutOffline()
    setSaving(true); const idempotencyKey = crypto.randomUUID()
    try {
      const db = requireSupabase()
      const payload = { p_branch_id: branchId, p_location_id: locationId, p_customer_id: customerId || null, p_items: cart.map(({ product, qty }) => ({ product_id: product.id, unit_id: product.base_unit_id, qty, unit_price: priceFor(product, qty) })), p_payments: [{ payment_method_id: paymentMethodId, amount: Math.round(total), cash_received: needsCash ? received : 0, reference: needsReference ? reference.trim() : null, provider }], p_discount_amount: Math.round(discountValue), p_idempotency_key: idempotencyKey }
      const { data, error: ce } = await db.rpc('checkout_sale_multi_payment_v2', payload); if (ce) throw ce
      const result = Array.isArray(data) ? data[0] : data; if (!result) throw new Error('Transaksi tidak mengembalikan nomor transaksi.')
      setLastSale({ ...result, note, items: cart, received: needsCash ? received : 0, subtotal, discountAmount: Math.round(discountValue), total, paymentCode: selectedCode, customerId }); setShowPayment(false); setShowReceipt(true); clearCart(); await load(true)
      toast('success', selectedCode === 'CASH' ? 'Transaksi berhasil' : `${paymentLabels[selectedCode]} tercatat`, `${result.sale_no} · ${money(total)}${selectedCode === 'CASH' ? '' : ' · status menunggu verifikasi'}`)
    } catch (e: any) { const message = e?.message || 'Transaksi tidak dapat diproses.'; if (/failed to fetch|networkerror|network request|fetch failed|offline|connection/i.test(message)) await checkoutOffline(idempotencyKey); else toast('error', 'Checkout gagal', message) } finally { setSaving(false) }
  }

  function printReceipt() { if (!lastSale) return; const rows = lastSale.items.map((x: CartItem) => `<tr><td>${text(x.product.name)}</td><td>${x.qty}</td><td>${money(priceFor(x.product,x.qty))}</td><td>${money(priceFor(x.product,x.qty)*x.qty)}</td></tr>`).join(''); const html = `<!doctype html><html><head><title>${text(lastSale.sale_no)}</title><style>body{font:12px monospace;width:280px;margin:16px auto;color:#111}h2{text-align:center;margin:0 0 8px}table{width:100%;border-collapse:collapse}td{padding:4px 0;border-bottom:1px dashed #bbb}td:nth-child(n+2){text-align:right}.line{display:flex;justify-content:space-between;padding:3px 0}.total{font-size:16px;font-weight:bold;border-top:1px solid #111;margin-top:6px;padding-top:7px}.center{text-align:center;color:#555}</style></head><body><h2>TOKO MAJU JAYA</h2><div class="center">${text(lastSale.sale_no)}<br>${new Date().toLocaleString('id-ID')}</div><hr><table>${rows}</table><div class="line"><span>Subtotal</span><span>${money(lastSale.subtotal)}</span></div><div class="line"><span>Diskon</span><span>-${money(lastSale.discountAmount)}</span></div><div class="line total"><span>TOTAL</span><span>${money(lastSale.total)}</span></div><div class="line"><span>Metode</span><span>${paymentLabels[normalizePaymentCode(text(lastSale.paymentCode || 'CASH'))]}</span></div>${lastSale.received > 0 ? `<div class="line"><span>Bayar</span><span>${money(lastSale.received)}</span></div><div class="line"><span>Kembali</span><span>${money(lastSale.received-lastSale.total)}</span></div>` : ''}${lastSale.note ? `<p>Catatan: ${text(lastSale.note)}</p>` : ''}<hr><div class="center">${lastSale.offline ? 'Tersimpan offline · akan disinkronkan' : 'Terima kasih'}</div><script>window.print()</script></body></html>`; const win = window.open('', '_blank', 'width=380,height=650'); if (!win) return toast('error', 'Cetak gagal', 'Popup diblokir browser. Izinkan popup untuk mencetak struk.'); win.document.write(html); win.document.close() }

  const quickCash = [total, Math.ceil(total / 10000) * 10000, Math.ceil(total / 50000) * 50000, Math.ceil(total / 100000) * 100000].filter((v, i, a) => v > 0 && a.indexOf(v) === i)
  const paymentOptions = paymentMethods.map(method => ({ ...method, code: normalizePaymentCode(text(method.code || method.method_type)) })).filter((m, i, a) => a.findIndex(x => x.code === m.code) === i)

  return <div className={`${styles.root} ${styles[`${mode}Mode`] || ''}`}>
    <header className={styles.topbar}><div><span className={styles.eyebrow}>TOKO MAJU JAYA · POINT OF SALE</span><h1>Penjualan</h1><p className={styles.modeDescription}>{modeDescription}</p></div><div className={styles.headerActions}><div className={`${styles.connectionPill} ${online ? styles.connectionOnline : styles.connectionOffline}`}><span>●</span>{online ? 'Online' : 'Offline'}{pendingCount > 0 && <b>{pendingCount} tertunda</b>}</div><div className={styles.modeBadge}>{modeLabel}</div><div className={styles.branch}>● Cabang Utama</div><div className={styles.avatar}>TJ</div></div></header>
    <div className={styles.toolbar}><div className={styles.search}><span>⌕</span><input ref={searchRef} value={search} onChange={e => setSearch(e.target.value)} placeholder={mode === 'distributor' ? 'Cari SKU, barcode, nama produk...' : 'Scan barcode atau cari produk, SKU...'} autoComplete="off" /><kbd>Ctrl K</kbd></div><select value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}><option value="">Semua kategori</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><button onClick={() => setShowHeld(true)}>{mode === 'distributor' ? 'Draft pesanan' : 'Ditahan'} <b>{heldOrders.length}</b></button></div>
    <div className={styles.layout}><section className={styles.catalogPanel}><div className={styles.sectionHead}><div><h2>{mode === 'distributor' ? 'Daftar Barang' : mode === 'fnb' ? 'Menu Produk' : 'Daftar Produk'}</h2><span>{mode === 'distributor' ? 'Pilih barang lalu atur kuantitas untuk penjualan grosir' : mode === 'fnb' ? 'Pilih menu untuk masuk ke pesanan' : `${shown.length} produk · klik kartu untuk menambah`}</span></div><strong>{online ? '● LIVE' : '● LOCAL'}</strong></div>{loading ? <div className={styles.empty}>Memuat katalog...</div> : shown.length === 0 ? <div className={styles.empty}>Produk tidak ditemukan.</div> : <div className={styles.catalog} ref={catalogRef}>{shown.map(p => { const s = stockFor(p.id); return <button className={styles.product} key={p.id} onClick={() => add(p)} disabled={s <= 0}><div className={styles.productArt}>{text(p.name).slice(0,1).toUpperCase()}</div><div className={styles.productInfo}><div className={styles.productTop}><span>{categoryName(p.category_id)}</span><small>{s <= 0 ? 'Habis' : `Stok ${number(s)}`}</small></div><strong>{p.name}</strong><em>{p.sku || 'Tanpa SKU'} · {unitName(p.base_unit_id)}</em><div className={styles.productBottom}><b>{money(priceFor(p,1))}</b><i>＋</i></div></div></button> })}</div>}</section>
      <section className={styles.cart}><div className={styles.sectionHead}><div><h2>{mode === 'distributor' ? 'Detail Penjualan' : 'Pesanan'}</h2><span>{totalQty} item · {cart.length} produk</span></div>{cart.length > 0 && <button className={styles.clear} onClick={clearCart}>Kosongkan</button>}</div><div className={styles.cartList}>{cart.length === 0 ? <div className={styles.cartEmpty}><div>＋</div><strong>Belum ada pesanan</strong><span>Pilih produk dari katalog untuk memulai transaksi.</span></div> : cart.map(item => <div className={styles.cartRow} key={item.product.id}><div className={styles.miniArt}>{text(item.product.name).slice(0,1).toUpperCase()}</div><div className={styles.cartProduct}><strong>{item.product.name}</strong><span>{money(priceFor(item.product,item.qty))} / {unitName(item.product.base_unit_id)}</span><div className={styles.qty}><button onClick={() => changeQty(item.product.id,-1)}>−</button><input value={item.qty} onChange={e => setQty(item.product.id,e.target.value)} inputMode="numeric" /><button onClick={() => changeQty(item.product.id,1)}>+</button></div></div><b>{money(priceFor(item.product,item.qty)*item.qty)}</b></div>)}</div><div className={styles.cartTools}><button onClick={holdOrder} disabled={!cart.length}>{mode === 'distributor' ? 'Simpan draft' : 'Tahan'} <kbd>F7</kbd></button><button onClick={() => setNote(note ? '' : ' ')} disabled={!cart.length}>Catatan</button></div>{note !== '' && <input className={styles.note} value={note} onChange={e => setNote(e.target.value)} placeholder="Catatan transaksi" autoFocus />}
        <div className={styles.discount}><div><strong>Diskon transaksi</strong><span>Potongan dari subtotal</span></div><div className={styles.discountControl}><div><button className={discountMode === 'percent' ? styles.active : ''} onClick={() => setDiscountMode('percent')}>%</button><button className={discountMode === 'amount' ? styles.active : ''} onClick={() => setDiscountMode('amount')}>Rp</button></div><input value={discount} onChange={e => setDiscount(e.target.value.replace(/[^0-9.]/g,''))} inputMode="decimal" placeholder="0" /></div></div>
        <div className={styles.checkout}><div><span>Subtotal</span><b>{money(subtotal)}</b></div>{discountValue > 0 && <div className={styles.discountLine}><span>Diskon {discountMode === 'percent' ? `(${discount}%)` : ''}</span><b>-{money(discountValue)}</b></div>}<div className={styles.totalLine}><span>Total</span><strong>{money(total)}</strong></div><button className={styles.pay} disabled={!cart.length || !branchId || !locationId || !paymentMethodId} onClick={() => setShowPayment(true)}>Bayar <b>{money(total)}</b></button><small>{online ? 'F4 untuk pembayaran · harga & stok divalidasi server' : 'F4 untuk pembayaran · Tunai dan Piutang dapat disimpan lokal'}</small></div></section></div>

    {showPayment && <div className={styles.backdrop} onMouseDown={() => setShowPayment(false)}><div className={styles.modalWide} onMouseDown={e => e.stopPropagation()}><div className={styles.modalHead}><div><span className={styles.eyebrow}>CHECKOUT · {online ? 'ONLINE' : 'OFFLINE'}</span><h2>Pembayaran & preview struk</h2><p>{online ? 'Pilih metode. Status pembayaran mengikuti aturan server.' : 'Offline hanya mendukung Tunai dan Piutang.'}</p></div><button onClick={() => setShowPayment(false)}>×</button></div><div className={styles.paymentGrid}><div><div className={styles.paymentTotal}><span>Total tagihan</span><strong>{money(total)}</strong></div><div style={{display:'grid',gridTemplateColumns:'repeat(2,minmax(0,1fr))',gap:8,marginBottom:16}}>{paymentOptions.map(method => { const disabled = !online && method.code !== 'CASH' && method.code !== 'RECEIVABLE'; return <button key={method.code} type="button" disabled={disabled} onClick={() => { setPaymentMethodId(method.id); setPaymentCode(method.code); if (method.code !== 'CASH') setCash(''); if (method.code !== 'TRANSFER') setReference('') }} className={paymentCode === method.code ? styles.active : ''}>{paymentLabels[method.code]}{disabled ? ' · online' : method.code === 'RECEIVABLE' ? ' · offline' : ''}</button> })}</div>{needsCustomer && <label>Pelanggan<select value={customerId} onChange={e => setCustomerId(e.target.value)}><option value="">Pilih pelanggan</option>{customers.map(c => <option key={c.id} value={c.id}>{c.name}{c.phone ? ` · ${c.phone}` : ''}</option>)}</select></label>}{needsReference && <label>Referensi transfer<input value={reference} onChange={e => setReference(e.target.value)} placeholder="Nomor referensi / berita transfer" /></label>}{selectedCode === 'QRIS' && <div className={styles.change}><span>QRIS</span><strong>{online ? 'Menunggu konfirmasi provider' : 'Tidak tersedia offline'}</strong></div>}{needsCash && <><label>Uang diterima<input autoFocus value={cash} onChange={e => setCash(e.target.value.replace(/\D/g,''))} inputMode="numeric" placeholder="0" /></label><div className={styles.quickCash}>{quickCash.map(v => <button key={v} onClick={() => setCash(String(v))}>{money(v)}</button>)}</div><div className={`${styles.change} ${received >= total ? styles.good : styles.warn}`}><span>{received >= total ? 'Kembalian' : `Kurang ${money(total-received)}`}</span><strong>{received >= total ? money(change) : money(0)}</strong></div></>}{needsCustomer && <div className={styles.change}><span>Piutang</span><strong>Dicatat sebagai tagihan pelanggan</strong></div>}{needsReference && <div className={styles.change}><span>Transfer</span><strong>Pending sampai diverifikasi</strong></div>}<button className={styles.pay} disabled={saving || !!validateCheckout()} onClick={checkout}>{saving ? 'Memproses...' : online ? `Selesaikan ${money(total)}` : `Simpan ${paymentLabels[selectedCode]} ${money(total)}`}</button></div><ReceiptPreview sale={{sale_no:'DRAFT',items:cart,subtotal,discountAmount:Math.round(discountValue),total,received:needsCash?received:0,paymentCode:selectedCode}} priceFor={priceFor}/></div></div></div>}
    {showHeld && <div className={styles.backdrop} onMouseDown={() => setShowHeld(false)}><div className={styles.modal} onMouseDown={e => e.stopPropagation()}><div className={styles.modalHead}><div><span className={styles.eyebrow}>ANTRIAN</span><h2>Pesanan ditahan</h2></div><button onClick={() => setShowHeld(false)}>×</button></div>{heldOrders.length === 0 ? <div className={styles.empty}>Tidak ada pesanan yang ditahan.</div> : <div className={styles.heldList}>{heldOrders.map(o => <div className={styles.held} key={o.id}><div><strong>{o.name}</strong><span>{o.items.reduce((s,x)=>s+x.qty,0)} item · {new Date(o.createdAt).toLocaleTimeString('id-ID',{hour:'2-digit',minute:'2-digit'})}</span></div><button onClick={() => resumeOrder(o)}>Lanjutkan</button></div>)}</div>}</div></div>}
    {showReceipt && lastSale && <div className={styles.backdrop} onMouseDown={() => setShowReceipt(false)}><div className={styles.receiptModal} onMouseDown={e => e.stopPropagation()}><div className={styles.modalHead}><div><span className={styles.eyebrow}>{lastSale.offline ? 'TERSIMPAN LOKAL' : 'TRANSAKSI TERCATAT'}</span><h2>{lastSale.sale_no}</h2><p>{lastSale.offline ? 'Transaksi tersimpan di perangkat dan akan dikirim saat online.' : lastSale.paymentCode === 'CASH' ? 'Pembayaran tunai selesai dan stok sudah diproses.' : 'Pembayaran dicatat dan menunggu status final/verifikasi.'}</p></div><button onClick={() => setShowReceipt(false)}>×</button></div><ReceiptPreview sale={lastSale} priceFor={priceFor}/><div className={styles.receiptActions}><button onClick={() => setShowReceipt(false)}>Transaksi baru</button><button className={styles.pay} onClick={printReceipt}>Cetak struk</button></div></div></div>}
    <div className={styles.toastStack}>{toasts.map(t => <div className={`${styles.toast} ${styles[t.type]}`} key={t.id}><div className={styles.toastIcon}>{t.type==='success'?'✓':t.type==='error'?'!':'i'}</div><div><b>{t.title}</b><span>{t.text}</span></div><button onClick={() => dismissToast(t.id)}>×</button></div>)}</div>
  </div>
}

function ReceiptPreview({ sale, priceFor }: { sale: any; priceFor: (product: Row, qty: number) => number }) {
  const code = normalizePaymentCode(text(sale.paymentCode || 'CASH'))
  return <div className={styles.receiptPreview}><div className={styles.receiptPaper}><div className={styles.receiptCenter}><strong>TOKO MAJU JAYA</strong><span>POS QRIS · Cabang Utama</span><small>{sale.sale_no} · {new Date().toLocaleString('id-ID')}</small></div><div className={styles.rule}/>{(sale.items || []).map((x: CartItem, i: number) => <div className={styles.receiptItem} key={`${x.product.id}-${i}`}><div><b>{x.product.name}</b><span>{x.qty} × {money(priceFor(x.product,x.qty))}</span></div><strong>{money(priceFor(x.product,x.qty)*x.qty)}</strong></div>)}<div className={styles.rule}/><div className={styles.receiptLine}><span>Subtotal</span><b>{money(sale.subtotal)}</b></div><div className={styles.receiptLine}><span>Diskon</span><b>-{money(sale.discountAmount || 0)}</b></div><div className={styles.grand}><span>TOTAL</span><b>{money(sale.total)}</b></div><div className={styles.receiptLine}><span>Metode</span><b>{paymentLabels[code]}</b></div>{sale.received > 0 && <><div className={styles.receiptLine}><span>Tunai</span><b>{money(sale.received)}</b></div><div className={styles.receiptLine}><span>Kembalian</span><b>{money(Math.max(0,sale.received-sale.total))}</b></div></>}{sale.note && <div className={styles.receiptNote}>Catatan: {sale.note.trim()}</div>}<div className={styles.receiptCenter}><small>{sale.offline ? 'Tersimpan lokal · menunggu sinkronisasi' : code === 'CASH' ? 'Terima kasih atas kunjungan Anda.' : 'Menunggu verifikasi pembayaran.'}</small></div></div></div>
}
