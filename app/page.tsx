'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../lib/supabase'

type Row = Record<string, any>
type CartItem = { product: Row; qty: number }

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)
const number = (n: number) => new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 }).format(n)
const text = (v: unknown) => String(v ?? '')

const navItems = [
  ['Dashboard', '⌂'], ['Penjualan', '＋'], ['Produk', '▦'], ['Stok', '◈'],
  ['Pelanggan', '♙'], ['Supplier', '◇'], ['Pembelian', '↓'], ['Laporan', '▤'], ['Pengaturan', '⚙'],
]

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
  const [paying, setPaying] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Row | null>(null)

  const categoryName = (id: string) => text(categories.find(x => x.id === id)?.name || 'Tanpa kategori')
  const unitName = (id: string) => text(units.find(x => x.id === id)?.name || 'Satuan')
  const priceFor = (product: Row, qty: number) => Number(prices.filter(x => x.product_id === product.id && x.unit_id === product.base_unit_id && Number(x.min_qty) <= qty).sort((a, b) => Number(b.min_qty) - Number(a.min_qty))[0]?.price || 0)
  const stockFor = (productId: string) => Number(stock.find(x => x.product_id === productId)?.qty_base || 0)
  const total = cart.reduce((sum, item) => sum + priceFor(item.product, item.qty) * item.qty, 0)
  const received = Number(cash || 0)
  const change = received - total
  const lowStock = products.filter(p => stockFor(p.id) <= 5).length
  const todaySales = recentSales.reduce((sum, sale) => sum + Number(sale.total_amount || 0), 0)

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    return products.filter(p => {
      const matchesSearch = !q || [p.name, p.short_name, p.sku, p.barcode, categoryName(p.category_id)].some(v => text(v).toLowerCase().includes(q))
      const matchesCategory = !categoryFilter || p.category_id === categoryFilter
      return matchesSearch && matchesCategory
    })
  }, [products, search, categories, categoryFilter])

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
        // units pada database lama tidak memiliki kolom is_active.
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

  useEffect(() => { load() }, [])

  function add(product: Row) {
    setMessage(''); setError('')
    if (stockFor(product.id) <= 0) { setError(`${product.name} sedang habis.`); return }
    setCart(prev => prev.some(x => x.product.id === product.id) ? prev.map(x => x.product.id === product.id ? { ...x, qty: Math.min(x.qty + 1, stockFor(product.id)) } : x) : [...prev, { product, qty: 1 }])
  }
  function changeQty(id: string, delta: number) {
    setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty: Math.min(stockFor(id), Math.max(0, x.qty + delta)) } : x).filter(x => x.qty > 0))
  }

  async function checkout() {
    if (!branchId || !locationId || !cashMethodId || received < total || !cart.length) return
    setSaving(true); setError(''); setMessage('')
    try {
      const db = requireSupabase()
      const payload = {
        p_branch_id: branchId, p_location_id: locationId, p_customer_id: null,
        p_idempotency_key: crypto.randomUUID(),
        p_items: cart.map(({ product, qty }) => ({ product_id: product.id, unit_id: product.base_unit_id, qty, unit_price: priceFor(product, qty) })),
        p_payments: [{ payment_method_id: cashMethodId, amount: total, cash_received: received, provider: 'CASH' }],
      }
      const { data, error: ce } = await db.rpc('checkout_sale_multi_payment', payload)
      if (ce) throw ce
      const result = Array.isArray(data) ? data[0] : data
      if (!result) throw new Error('Transaksi tidak mengembalikan nomor transaksi.')
      setMessage(`Transaksi ${result.sale_no} berhasil. Total ${money(Number(result.total_amount))}, kembalian ${money(Number(result.change_amount || 0))}.`)
      setCart([]); setCash(''); setPaying(false); await load()
    } catch (e: any) { setError(e.message || 'Checkout gagal.') }
    finally { setSaving(false) }
  }

  async function saveProduct(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setSaving(true); setError(''); setMessage('')
    const form = new FormData(e.currentTarget)
    const values = { business_id: businessId, name: text(form.get('name')).trim(), short_name: text(form.get('short_name')).trim() || null, sku: text(form.get('sku')).trim(), barcode: text(form.get('barcode')).trim() || null, category_id: text(form.get('category_id')) || null, base_unit_id: text(form.get('base_unit_id')), current_cost: Number(form.get('current_cost') || 0), is_active: true }
    try {
      const db = requireSupabase()
      const query = editing ? db.from('products').update(values).eq('id', editing.id) : db.from('products').insert(values)
      const { error: pe } = await query
      if (pe) throw pe
      setMessage(editing ? 'Produk diperbarui.' : 'Produk ditambahkan.')
      setEditing(null); await load()
    } catch (e: any) { setError(e.message || 'Produk gagal disimpan.') }
    finally { setSaving(false) }
  }

  const dashboard = <>
    <div className="hero"><div><span className="eyebrow">OPERASIONAL HARI INI</span><h2>Ringkasan toko</h2><p>Pantau penjualan, produk, dan stok dari satu layar.</p></div><button className="secondary" onClick={load} disabled={loading}>↻ Refresh data</button></div>
    <div className="stat-grid">
      <div className="stat"><span>Penjualan terbaru</span><strong>{money(todaySales)}</strong><small>{recentSales.length} transaksi terakhir</small></div>
      <div className="stat"><span>Produk aktif</span><strong>{number(products.length)}</strong><small>Katalog tersedia</small></div>
      <div className="stat"><span>Stok menipis</span><strong>{number(lowStock)}</strong><small>≤ 5 unit</small></div>
      <div className="stat"><span>Keranjang</span><strong>{number(cart.reduce((s, x) => s + x.qty, 0))}</strong><small>Item sedang diproses</small></div>
    </div>
    <div className="dashboard-grid"><section className="panel"><div className="panel-title"><div><h3>Transaksi terakhir</h3><span className="muted">Aktivitas penjualan terbaru</span></div><button className="link-button" onClick={() => setPage('Laporan')}>Lihat semua</button></div>{recentSales.length ? recentSales.map(s => <div className="sale-row" key={s.id}><div><strong>{s.sale_no}</strong><span>{new Date(s.created_at).toLocaleString('id-ID')}</span></div><div><b>{money(Number(s.total_amount || 0))}</b><em className={s.status === 'COMPLETED' ? 'status good' : 'status'}>{s.status}</em></div></div>) : <div className="empty">Belum ada transaksi.</div>}</section><section className="panel"><div className="panel-title"><div><h3>Stok perlu perhatian</h3><span className="muted">Produk dengan stok ≤ 5</span></div><button className="link-button" onClick={() => setPage('Stok')}>Kelola stok</button></div>{products.filter(p => stockFor(p.id) <= 5).slice(0, 6).map(p => <div className="stock-row" key={p.id}><div><strong>{p.name}</strong><span>{p.sku}</span></div><b className={stockFor(p.id) <= 0 ? 'danger-text' : ''}>{number(stockFor(p.id))} {unitName(p.base_unit_id)}</b></div>)}{lowStock === 0 && <div className="empty">Semua stok aman.</div>}</section></div>
  </>

  return <div className="app">
    <aside className="sidebar"><div className="brand"><span className="brand-mark">Q</span><div><b>POS QRIS</b><small>Point of Sale</small></div></div><div className="nav">{navItems.map(([item, icon]) => <button key={item} className={page === item ? 'active' : ''} onClick={() => setPage(item)}><span className="nav-icon">{icon}</span>{item}</button>)}</div><div className="sidebar-footer"><span className="online-dot" /> Supabase connected</div></aside>
    <main className="main">
      <header className="header"><div><span className="breadcrumb">TOKO MAJU JAYA / POS</span><h1>{page}</h1></div><div className="header-actions"><span className="branch-chip">● Cabang Utama</span><button className="avatar">TJ</button></div></header>
      {error && <div className="alert error"><b>Terjadi masalah</b><span>{error}</span><button onClick={() => setError('')}>×</button></div>}{message && <div className="alert ok"><b>Berhasil</b><span>{message}</span><button onClick={() => setMessage('')}>×</button></div>}
      {page === 'Dashboard' && dashboard}
      {page === 'Penjualan' && <>
        <div className="sales-toolbar"><div className="search-box"><span>⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Cari produk, SKU, barcode..." /><kbd>Ctrl K</kbd></div><select className="filter" value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}><option value="">Semua kategori</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
        <div className="layout"><section className="panel catalog-panel"><div className="section-heading"><div><h2>Produk</h2><span className="muted">{shown.length} item tersedia</span></div><span className="live-pill"><i /> LIVE</span></div>{loading ? <div className="empty">Memuat katalog...</div> : shown.length === 0 ? <div className="empty">Produk tidak ditemukan.</div> : <div className="catalog">{shown.map(p => { const s = stockFor(p.id); return <button className="product" key={p.id} onClick={() => add(p)} disabled={s <= 0}><div className="product-art">{text(p.name).slice(0, 1).toUpperCase()}</div><div className="product-info"><strong>{p.name}</strong><span className="muted">{p.sku} · {unitName(p.base_unit_id)}</span><div className="product-bottom"><b>{money(priceFor(p, 1))}</b><span className={s <= 5 ? 'stock-low' : 'stock-ok'}>{s <= 0 ? 'Habis' : `Stok ${number(s)}`}</span></div></div></button> })}</div>}</section>
          <section className="panel cart"><div className="section-heading"><div><h2>Pesanan</h2><span className="muted">{cart.reduce((s, x) => s + x.qty, 0)} item</span></div>{cart.length > 0 && <button className="clear" onClick={() => setCart([])}>Kosongkan</button>}</div><div className="cart-list">{cart.length === 0 ? <div className="cart-empty"><div className="cart-empty-icon">＋</div><strong>Keranjang kosong</strong><span>Pilih produk di sebelah kiri untuk memulai transaksi.</span></div> : cart.map(({ product, qty }) => <div className="cart-row" key={product.id}><div className="mini-art">{text(product.name).slice(0, 1).toUpperCase()}</div><div className="cart-product"><strong>{product.name}</strong><span>{money(priceFor(product, qty))} / {unitName(product.base_unit_id)}</span><div className="qty"><button onClick={() => changeQty(product.id, -1)}>−</button><span>{qty}</span><button onClick={() => changeQty(product.id, 1)}>+</button></div></div><b className="line-total">{money(qty * priceFor(product, qty))}</b></div>)}</div><div className="checkout"><div className="summary-line"><span>Subtotal</span><b>{money(total)}</b></div><div className="summary-line muted"><span>Pajak</span><span>Rp 0</span></div><div className="grand"><span>Total</span><strong>{money(total)}</strong></div><button className="primary checkout-button" disabled={!cart.length || total <= 0} onClick={() => { setCash(String(total)); setPaying(true) }}>Bayar Sekarang <span>→</span></button></div></section>
        </div>
      </>}
      {page === 'Produk' && <section className="products-page"><section className="panel"><div className="section-heading"><div><h2>{editing ? 'Edit produk' : 'Tambah produk'}</h2><span className="muted">Kelola katalog dan informasi harga modal.</span></div></div><form onSubmit={saveProduct}><div className="form-grid"><label>Nama produk<input name="name" defaultValue={editing?.name || ''} required placeholder="Contoh: Es Teh Manis" /></label><label>Nama singkat<input name="short_name" defaultValue={editing?.short_name || ''} placeholder="Nama di kasir" /></label><label>SKU<input name="sku" defaultValue={editing?.sku || ''} required placeholder="SKU-001" /></label><label>Barcode<input name="barcode" defaultValue={editing?.barcode || ''} placeholder="Scan / ketik barcode" /></label><label>Kategori<select name="category_id" defaultValue={editing?.category_id || ''}><option value="">Tanpa kategori</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label>Satuan<select name="base_unit_id" defaultValue={editing?.base_unit_id || ''} required><option value="">Pilih satuan</option>{units.map(u => <option key={u.id} value={u.id}>{u.name}</option>)}</select></label><label>Harga modal<input name="current_cost" type="number" min="0" defaultValue={editing?.current_cost || 0} /></label></div><button className="primary" disabled={saving}>{saving ? 'Menyimpan...' : 'Simpan produk'}</button>{editing && <button type="button" className="secondary cancel-edit" onClick={() => setEditing(null)}>Batal edit</button>}</form></section><section className="panel table-wrap"><div className="section-heading"><div><h2>Daftar produk</h2><span className="muted">{products.length} produk aktif</span></div></div><table><thead><tr><th>Produk</th><th>SKU</th><th>Barcode</th><th>Kategori</th><th>Satuan</th><th>Harga</th><th></th></tr></thead><tbody>{products.map(p => <tr key={p.id}><td><strong>{p.name}</strong></td><td>{p.sku}</td><td>{p.barcode || '-'}</td><td>{categoryName(p.category_id)}</td><td>{unitName(p.base_unit_id)}</td><td><b>{money(priceFor(p, 1))}</b></td><td><button className="secondary" onClick={() => setEditing(p)}>Edit</button></td></tr>)}</tbody></table></section></section>}
      {page === 'Stok' && <section className="panel"><div className="section-heading"><div><h2>Stok barang</h2><span className="muted">Pantau persediaan per produk.</span></div></div><div className="stock-table">{products.map(p => { const s = stockFor(p.id); return <div className="stock-card" key={p.id}><div className="mini-art">{text(p.name).slice(0,1).toUpperCase()}</div><div><strong>{p.name}</strong><span>{p.sku} · {categoryName(p.category_id)}</span></div><b className={s <= 5 ? 'danger-text' : 'stock-value'}>{number(s)} {unitName(p.base_unit_id)}</b></div> })}</div></section>}
      {['Pelanggan','Supplier','Pembelian','Laporan','Pengaturan'].includes(page) && <section className="panel module-placeholder"><div className="module-icon">{navItems.find(x => x[0] === page)?.[1]}</div><h2>Modul {page}</h2><p>Struktur navigasi sudah siap. Modul berikutnya akan dibangun di atas database Supabase yang sama, tanpa membuat tabel pengganti sembarangan.</p></section>}
    </main>
    {paying && <div className="modal-backdrop"><div className="payment-modal"><div className="modal-header"><div><span className="eyebrow">CHECKOUT</span><h2>Pembayaran tunai</h2></div><button className="modal-close" onClick={() => setPaying(false)}>×</button></div><div className="pay-total"><span>Total pembayaran</span><strong>{money(total)}</strong></div><div className="pay-items">{cart.map(({product,qty}) => <div key={product.id}><span>{qty} × {product.name}</span><b>{money(qty * priceFor(product,qty))}</b></div>)}</div><label className="cash-field">Uang diterima<input value={cash} onChange={e => setCash(e.target.value.replace(/\D/g,''))} inputMode="numeric" autoFocus placeholder="0" /></label><div className={change >= 0 ? 'change-box' : 'change-box short'}><span>{change >= 0 ? 'Kembalian' : 'Uang masih kurang'}</span><strong>{money(Math.abs(change))}</strong></div><button className="primary" disabled={saving || received < total} onClick={checkout}>{saving ? 'Memproses transaksi...' : 'Konfirmasi transaksi'}</button><button className="secondary modal-cancel" disabled={saving} onClick={() => setPaying(false)}>Batal</button></div></div>}
    <nav className="mobile-nav">{[['Penjualan','＋'],['Produk','▦'],['Stok','◈']].map(([item, icon]) => <button key={item} className={page === item ? 'active' : ''} onClick={() => setPage(item)}><span>{icon}</span>{item}</button>)}</nav>
  </div>
}
