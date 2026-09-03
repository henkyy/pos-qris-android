'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { requireSupabase } from '../lib/supabase'

type Row = Record<string, any>
type CartItem = { product: Row; qty: number }
type HeldOrder = { id: string; name: string; items: CartItem[]; cash: string; createdAt: string }

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)
const number = (n: number) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 }).format(n)
const text = (v: unknown) => String(v ?? '')
const navItems = [['Dashboard', '⌂'], ['Penjualan', '＋'], ['Produk', '▦'], ['Stok', '◈'], ['Pelanggan', '♙'], ['Supplier', '◇'], ['Pembelian', '↓'], ['Laporan', '▤'], ['Pengaturan', '⚙']]

export default function HomePage() {
  const [page, setPage] = useState('Penjualan')
  const [products, setProducts] = useState<Row[]>([])
  const [categories, setCategories] = useState<Row[]>([])
  const [units, setUnits] = useState<Row[]>([])
  const [prices, setPrices] = useState<Row[]>([])
  const [stock, setStock] = useState<Row[]>([])
  const [recentSales, setRecentSales] = useState<Row[]>([])
  const [businessId, setBusinessId] = useState('')
  const [branchId, setBranchId] = useState('')
  const [locationId, setLocationId] = useState('')
  const [cashMethodId, setCashMethodId] = useState('')
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [cash, setCash] = useState('')
  const [note, setNote] = useState('')
  const [heldOrders, setHeldOrders] = useState<HeldOrder[]>([])
  const [showHeld, setShowHeld] = useState(false)
  const [showPayment, setShowPayment] = useState(false)
  const [lastSale, setLastSale] = useState<Row | null>(null)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const searchRef = useRef<HTMLInputElement>(null)

  const categoryName = (id: string) => text(categories.find(x => x.id === id)?.name || 'Tanpa kategori')
  const unitName = (id: string) => text(units.find(x => x.id === id)?.name || 'Satuan')
  const priceFor = (product: Row, qty: number) => Number(prices.filter(x => x.product_id === product.id && x.unit_id === product.base_unit_id && Number(x.min_qty) <= qty).sort((a, b) => Number(b.min_qty) - Number(a.min_qty))[0]?.price || 0)
  const stockFor = (productId: string) => Number(stock.find(x => x.product_id === productId)?.qty_base || 0)
  const totalQty = cart.reduce((sum, item) => sum + item.qty, 0)
  const subtotal = cart.reduce((sum, item) => sum + priceFor(item.product, item.qty) * item.qty, 0)
  const received = Number(cash || 0)
  const change = received - subtotal

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return products.filter(p => {
      const haystack = [p.name, p.short_name, p.sku, p.barcode, categoryName(p.category_id)].map(text).join(' ').toLowerCase()
      return (!q || haystack.includes(q)) && (!categoryFilter || p.category_id === categoryFilter)
    })
  }, [products, search, categoryFilter, categories])

  async function load() {
    setLoading(true); setError('')
    try {
      const db = requireSupabase()
      const { data: businesses, error: be } = await db.from('businesses').select('*').eq('code', 'TOKO_MAJU_JAYA').limit(1)
      if (be) throw be
      const business = businesses?.[0]
      if (!business) throw new Error('Business TOKO_MAJU_JAYA tidak ditemukan.')
      setBusinessId(business.id)
      const [{ data: ps, error: pe }, { data: cs, error: ce }, { data: us, error: ue }, { data: bs, error: se }, { data: branches, error: bre }] = await Promise.all([
        db.from('products').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('categories').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
        db.from('units').select('*').eq('business_id', business.id).order('name'),
        db.from('stock_balances').select('*'),
        db.from('branches').select('*').eq('business_id', business.id).eq('is_active', true).limit(1),
      ])
      if (pe || ce || ue || se || bre) throw pe || ce || ue || se || bre
      setProducts(ps || []); setCategories(cs || []); setUnits(us || []); setStock(bs || [])
      const branch = branches?.[0]
      if (!branch) throw new Error('Cabang aktif tidak ditemukan.')
      setBranchId(branch.id)
      const { data: locations, error: le } = await db.from('locations').select('*').eq('branch_id', branch.id).eq('is_active', true).limit(1)
      if (le) throw le
      setLocationId(locations?.[0]?.id || '')
      const { data: pls, error: ple } = await db.from('price_lists').select('*').eq('business_id', business.id).eq('is_default', true).eq('is_active', true).limit(1)
      if (ple) throw ple
      const pl = pls?.[0]
      const { data: prs, error: pre } = pl ? await db.from('product_prices').select('*').eq('price_list_id', pl.id) : { data: [], error: null }
      if (pre) throw pre
      setPrices(prs || [])
      const { data: methods, error: me } = await db.from('payment_methods').select('*').eq('business_id', business.id).eq('code', 'CASH').eq('is_active', true).limit(1)
      if (me) throw me
      setCashMethodId(methods?.[0]?.id || '')
      const { data: sales, error: saleError } = await db.from('sales').select('id,sale_no,status,total_amount,paid_amount,created_at').eq('business_id', business.id).order('created_at', { ascending: false }).limit(8)
      if (!saleError) setRecentSales(sales || [])
    } catch (e: any) { setError(e.message || 'Gagal memuat data.') }
    finally { setLoading(false) }
  }

  useEffect(() => {
    load()
    const saved = window.localStorage.getItem('qris-held-orders')
    if (saved) { try { setHeldOrders(JSON.parse(saved)) } catch {} }
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); searchRef.current?.focus() }
      if (e.key === 'Escape') { setShowPayment(false); setShowHeld(false) }
      if (e.key === 'F4' && cart.length) { e.preventDefault(); setShowPayment(true) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [cart.length])

  function add(product: Row) {
    setMessage(''); setError('')
    const available = stockFor(product.id)
    if (available <= 0) return setError(`${product.name} sedang habis.`)
    setCart(prev => prev.some(x => x.product.id === product.id) ? prev.map(x => x.product.id === product.id ? { ...x, qty: Math.min(x.qty + 1, available) } : x) : [...prev, { product, qty: 1 }])
  }
  function changeQty(id: string, delta: number) { setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty: Math.min(stockFor(id), Math.max(0, x.qty + delta)) } : x).filter(x => x.qty > 0)) }
  function setQty(id: string, raw: string) { const qty = Math.max(0, Math.min(stockFor(id), Number(raw) || 0)); setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty } : x).filter(x => x.qty > 0)) }
  function clearCart() { setCart([]); setCash(''); setNote(''); setShowPayment(false) }
  function holdOrder() {
    if (!cart.length) return
    const order: HeldOrder = { id: crypto.randomUUID(), name: `Pesanan ${heldOrders.length + 1}`, items: cart, cash, createdAt: new Date().toISOString() }
    const next = [order, ...heldOrders]; setHeldOrders(next); window.localStorage.setItem('qris-held-orders', JSON.stringify(next)); clearCart(); setMessage('Pesanan ditahan. Bisa dilanjutkan dari tombol Ditahan.')
  }
  function resumeOrder(order: HeldOrder) { const next = heldOrders.filter(x => x.id !== order.id); setCart(order.items); setCash(order.cash); setHeldOrders(next); setShowHeld(false); window.localStorage.setItem('qris-held-orders', JSON.stringify(next)) }

  async function checkout() {
    if (!branchId || !locationId || !cashMethodId || received < subtotal || !cart.length) return
    setSaving(true); setError(''); setMessage('')
    try {
      const db = requireSupabase()
      const payload = { p_branch_id: branchId, p_location_id: locationId, p_customer_id: null, p_idempotency_key: crypto.randomUUID(), p_items: cart.map(({ product, qty }) => ({ product_id: product.id, unit_id: product.base_unit_id, qty, unit_price: priceFor(product, qty) })), p_payments: [{ payment_method_id: cashMethodId, amount: subtotal, cash_received: received, provider: 'CASH' }] }
      const { data, error: ce } = await db.rpc('checkout_sale_multi_payment', payload)
      if (ce) throw ce
      const result = Array.isArray(data) ? data[0] : data
      if (!result) throw new Error('Transaksi tidak mengembalikan nomor transaksi.')
      setLastSale({ ...result, note, items: cart, received })
      setMessage(`Transaksi ${result.sale_no} berhasil. Kembalian ${money(Number(result.change_amount || change))}.`)
      clearCart(); await load()
    } catch (e: any) { setError(e.message || 'Checkout gagal.') }
    finally { setSaving(false) }
  }

  function printReceipt() {
    if (!lastSale) return
    const rows = lastSale.items.map((x: CartItem) => `<tr><td>${text(x.product.name)}</td><td>${x.qty}</td><td>${money(priceFor(x.product, x.qty))}</td><td>${money(priceFor(x.product, x.qty) * x.qty)}</td></tr>`).join('')
    const html = `<!doctype html><html><head><title>${text(lastSale.sale_no)}</title><style>body{font:12px monospace;width:280px;margin:16px auto}h2{text-align:center;margin:0 0 8px}table{width:100%;border-collapse:collapse}td{padding:3px 0;border-bottom:1px dashed #aaa}td:nth-child(n+2){text-align:right}.total{font-size:16px;font-weight:bold;margin-top:10px}.center{text-align:center;color:#666}</style></head><body><h2>TOKO MAJU JAYA</h2><div class="center">${text(lastSale.sale_no)}<br>${new Date().toLocaleString('id-ID')}</div><hr><table>${rows}</table><div class="total">TOTAL ${money(Number(lastSale.total_amount || subtotal))}</div><div>Bayar ${money(Number(lastSale.received || 0))}</div><div>Kembali ${money(Number(lastSale.change_amount || 0))}</div>${lastSale.note ? `<p>Catatan: ${text(lastSale.note)}</p>` : ''}<hr><div class="center">Terima kasih</div><script>window.print()</script></body></html>`
    const win = window.open('', '_blank', 'width=380,height=650')
    if (!win) return setError('Popup diblokir browser. Izinkan popup untuk mencetak struk.')
    win.document.write(html); win.document.close()
  }

  const quickCash = [subtotal, Math.ceil(subtotal / 10000) * 10000, Math.ceil(subtotal / 50000) * 50000, Math.ceil(subtotal / 100000) * 100000].filter((v, i, a) => v > 0 && a.indexOf(v) === i)
  const dashboard = <><div className="hero"><div><span className="eyebrow">OPERASIONAL HARI INI</span><h2>Ringkasan toko</h2><p>Pantau penjualan, produk, dan stok dari satu layar.</p></div><button className="secondary" onClick={load}>↻ Refresh data</button></div><div className="stat-grid"><div className="stat"><span>Penjualan terbaru</span><strong>{money(recentSales.reduce((s, x) => s + Number(x.total_amount || 0), 0))}</strong><small>{recentSales.length} transaksi terakhir</small></div><div className="stat"><span>Produk aktif</span><strong>{number(products.length)}</strong><small>Katalog tersedia</small></div><div className="stat"><span>Stok menipis</span><strong>{number(products.filter(p => stockFor(p.id) <= 5).length)}</strong><small>≤ 5 unit</small></div><div className="stat"><span>Keranjang</span><strong>{number(totalQty)}</strong><small>Item sedang diproses</small></div></div></>

  return <div className="app"><aside className="sidebar"><div className="brand"><span className="brand-mark">Q</span><div><b>POS QRIS</b><small>Point of Sale</small></div></div><div className="nav">{navItems.map(([item, icon]) => <button key={item} className={page === item ? 'active' : ''} onClick={() => setPage(item)}><span className="nav-icon">{icon}</span>{item}</button>)}</div><div className="sidebar-footer"><span className="online-dot" /> Supabase connected</div></aside>
    <main className="main"><header className="header"><div><span className="breadcrumb">TOKO MAJU JAYA / POS</span><h1>{page}</h1></div><div className="header-actions"><button className="branch-chip">● Cabang Utama</button><button className="avatar">TJ</button></div></header>
      {error && <div className="alert error"><b>Terjadi masalah</b><span>{error}</span><button onClick={() => setError('')}>×</button></div>}{message && <div className="alert ok"><b>Berhasil</b><span>{message}</span><button onClick={() => setMessage('')}>×</button></div>}
      {page === 'Dashboard' && dashboard}
      {page !== 'Dashboard' && page !== 'Penjualan' && <div className="panel placeholder"><h2>{page}</h2><p>Modul ini kita kerjakan setelah alur penjualan stabil.</p></div>}
      {page === 'Penjualan' && <><div className="sales-toolbar"><div className="search-box"><span>⌕</span><input ref={searchRef} value={search} onChange={e => setSearch(e.target.value)} placeholder="Cari produk, SKU, barcode..." autoComplete="off" /><kbd>Ctrl K</kbd></div><select className="filter" value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}><option value="">Semua kategori</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><button className="toolbar-btn" onClick={() => setShowHeld(true)}>Ditahan <b>{heldOrders.length}</b></button></div>
        <div className="sales-layout"><section className="panel catalog-panel"><div className="section-heading"><div><h2>Produk</h2><span className="muted">{shown.length} produk · klik untuk menambahkan</span></div><span className="live-pill"><i /> LIVE</span></div>{loading ? <div className="empty">Memuat katalog...</div> : shown.length === 0 ? <div className="empty">Produk tidak ditemukan.</div> : <div className="catalog">{shown.map(p => { const s = stockFor(p.id); return <button className="product" key={p.id} onClick={() => add(p)} disabled={s <= 0}><div className="product-art">{text(p.name).slice(0, 1).toUpperCase()}</div><div className="product-info"><strong>{p.name}</strong><span className="muted">{p.sku || 'Tanpa SKU'} · {unitName(p.base_unit_id)}</span><div className="product-bottom"><b>{money(priceFor(p, 1))}</b><span className={s <= 5 ? 'stock-low' : 'stock-ok'}>{s <= 0 ? 'Habis' : `Stok ${number(s)}`}</span></div></div></button> })}</div>}</section>
          <section className="panel cart"><div className="section-heading"><div><h2>Pesanan</h2><span className="muted">{totalQty} item</span></div>{cart.length > 0 && <button className="clear" onClick={clearCart}>Kosongkan</button>}</div><div className="cart-list">{cart.length === 0 ? <div className="cart-empty"><div className="cart-empty-icon">＋</div><strong>Belum ada pesanan</strong><span>Pilih produk dari katalog untuk memulai transaksi.</span></div> : cart.map(item => <div className="cart-row" key={item.product.id}><div className="mini-art">{text(item.product.name).slice(0, 1).toUpperCase()}</div><div className="cart-product"><strong>{item.product.name}</strong><span>{money(priceFor(item.product, item.qty))} / {unitName(item.product.base_unit_id)}</span><div className="qty"><button onClick={() => changeQty(item.product.id, -1)}>−</button><input value={item.qty} onChange={e => setQty(item.product.id, e.target.value)} inputMode="numeric" aria-label={`Jumlah ${item.product.name}`} /><button onClick={() => changeQty(item.product.id, 1)}>+</button></div></div><b>{money(priceFor(item.product, item.qty) * item.qty)}</b></div>)}</div><div className="cart-tools"><button className="secondary" onClick={holdOrder} disabled={!cart.length}>Tahan pesanan</button><button className="secondary" onClick={() => setNote(note ? '' : ' ')} disabled={!cart.length}>{note.trim() ? 'Hapus catatan' : 'Tambah catatan'}</button></div>{note !== '' && <input className="note-input" value={note} onChange={e => setNote(e.target.value)} placeholder="Catatan pesanan, contoh: tanpa es" autoFocus />}
            <div className="checkout-box"><div className="summary-line"><span>Subtotal</span><b>{money(subtotal)}</b></div><div className="summary-line total-line"><span>Total</span><strong>{money(subtotal)}</strong></div><button className="primary" disabled={!cart.length || !branchId || !locationId || !cashMethodId} onClick={() => setShowPayment(true)}>Bayar <span>{money(subtotal)}</span></button><small className="shortcut">F4 untuk pembayaran · Stok dan harga divalidasi server</small></div></section></div></>}

      {showPayment && <div className="modal-backdrop" onMouseDown={() => setShowPayment(false)}><div className="modal payment-modal" onMouseDown={e => e.stopPropagation()}><div className="modal-head"><div><span className="eyebrow">PEMBAYARAN</span><h2>Konfirmasi tunai</h2></div><button onClick={() => setShowPayment(false)}>×</button></div><div className="payment-total"><span>Total tagihan</span><strong>{money(subtotal)}</strong></div><label className="field-label">Uang diterima<input autoFocus value={cash} onChange={e => setCash(e.target.value.replace(/\D/g, ''))} inputMode="numeric" placeholder="0" /></label><div className="quick-cash">{quickCash.map(v => <button key={v} className="secondary" onClick={() => setCash(String(v))}>{money(v)}</button>)}</div><div className={`change ${received >= subtotal ? 'change-ok' : 'change-warn'}`}><span>{received >= subtotal ? 'Kembalian' : `Kurang ${money(subtotal - received)}`}</span><strong>{received >= subtotal ? money(change) : money(0)}</strong></div><button className="primary" disabled={saving || received < subtotal || !cart.length} onClick={checkout}>{saving ? 'Memproses...' : `Selesaikan ${money(subtotal)}`}</button><small className="modal-hint">Transaksi memakai checkout server-side dan idempotency key untuk mencegah duplikasi.</small></div></div>}
      {showHeld && <div className="modal-backdrop" onMouseDown={() => setShowHeld(false)}><div className="modal" onMouseDown={e => e.stopPropagation()}><div className="modal-head"><div><span className="eyebrow">ANTRIAN</span><h2>Pesanan ditahan</h2></div><button onClick={() => setShowHeld(false)}>×</button></div>{heldOrders.length === 0 ? <div className="empty">Tidak ada pesanan yang ditahan.</div> : <div className="held-list">{heldOrders.map(o => <div className="held-row" key={o.id}><div><strong>{o.name}</strong><span>{o.items.reduce((s, x) => s + x.qty, 0)} item · {new Date(o.createdAt).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' })}</span></div><button className="secondary" onClick={() => resumeOrder(o)}>Lanjutkan</button></div>)}</div>}</div></div>}
      {lastSale && <div className="success-dock"><div><b>Transaksi {lastSale.sale_no} selesai</b><span>{money(Number(lastSale.total_amount || 0))} · siap dicetak</span></div><button className="secondary" onClick={printReceipt}>Cetak struk</button><button className="clear" onClick={() => setLastSale(null)}>×</button></div>}
    </main></div>
}
