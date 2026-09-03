'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../lib/supabase'

type Row = Record<string, any>
type CartItem = { product: Row; qty: number }

const money = (n: number) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n)
const text = (v: unknown) => String(v ?? '')

export default function HomePage() {
  const [page, setPage] = useState('Penjualan')
  const [products, setProducts] = useState<Row[]>([])
  const [categories, setCategories] = useState<Row[]>([])
  const [units, setUnits] = useState<Row[]>([])
  const [prices, setPrices] = useState<Row[]>([])
  const [stock, setStock] = useState<Row[]>([])
  const [businessId, setBusinessId] = useState('')
  const [branchId, setBranchId] = useState('')
  const [locationId, setLocationId] = useState('')
  const [cashMethodId, setCashMethodId] = useState('')
  const [search, setSearch] = useState('')
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
  const priceFor = (product: Row, qty: number) => Number(prices.filter(x => x.product_id === product.id && x.unit_id === product.base_unit_id && Number(x.min_qty) <= qty).sort((a,b) => Number(b.min_qty)-Number(a.min_qty))[0]?.price || 0)
  const stockFor = (productId: string) => Number(stock.find(x => x.product_id === productId)?.qty_base || 0)
  const total = cart.reduce((sum, item) => sum + priceFor(item.product, item.qty) * item.qty, 0)
  const received = Number(cash || 0)
  const change = received - total
  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return products
    return products.filter(p => [p.name, p.short_name, p.sku, p.barcode, categoryName(p.category_id)].some(v => text(v).toLowerCase().includes(q)))
  }, [products, search, categories])

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
        db.from('units').select('*').eq('business_id', business.id).eq('is_active', true).order('name'),
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
    } catch (e: any) { setError(e.message || 'Gagal memuat data.') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  function add(product: Row) {
    setMessage(''); setError('')
    setCart(prev => prev.some(x => x.product.id === product.id) ? prev.map(x => x.product.id === product.id ? { ...x, qty: x.qty + 1 } : x) : [...prev, { product, qty: 1 }])
  }
  function changeQty(id: string, delta: number) {
    setCart(prev => prev.map(x => x.product.id === id ? { ...x, qty: Math.max(0, x.qty + delta) } : x).filter(x => x.qty > 0))
  }

  async function checkout() {
    if (!branchId || !locationId || !cashMethodId || received < total) return
    setSaving(true); setError(''); setMessage('')
    try {
      const db = requireSupabase()
      const payload = {
        p_branch_id: branchId,
        p_location_id: locationId,
        p_customer_id: null,
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

  return <div className="app">
    <aside className="sidebar"><div className="brand">POS QRIS</div><div className="nav">{['Dashboard','Penjualan','Produk','Stok','Pelanggan','Supplier','Pembelian','Laporan','Pengaturan'].map(item => <button key={item} className={page === item ? 'active' : ''} onClick={() => setPage(item)}>{item}</button>)}</div></aside>
    <main className="main">
      <div className="header"><div><h1>{page}</h1><div className="muted">POS web untuk HP dan tablet</div></div><button className="secondary" onClick={load} disabled={loading}>Refresh</button></div>
      {error && <div className="error">{error}</div>}{message && <div className="ok">{message}</div>}
      {page === 'Penjualan' && <>
        <input className="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Cari nama, SKU, barcode, atau kategori..." />
        <div className="layout"><section className="panel catalog">{loading ? <div className="empty">Memuat katalog...</div> : shown.length === 0 ? <div className="empty">Produk tidak ditemukan.</div> : shown.map(p => <button className="product" key={p.id} onClick={() => add(p)}><strong>{p.name}</strong><span className="muted">SKU {p.sku}</span><br/><span className="tag">{categoryName(p.category_id)}</span><span className="muted"> · {unitName(p.base_unit_id)}</span><div className="price">{money(priceFor(p, 1))}</div><span className="muted">Stok {stockFor(p.id)}</span></button>)}</section>
          <section className="panel cart"><h2 style={{marginTop:0}}>Keranjang</h2><div className="cart-list">{cart.length === 0 ? <div className="empty">Belum ada barang.</div> : cart.map(({ product, qty }) => <div className="cart-row" key={product.id}><div><strong>{product.name}</strong><div className="muted">{qty} × {money(priceFor(product, qty))}</div></div><div className="qty"><button onClick={() => changeQty(product.id, -1)}>−</button><span>{qty}</span><button onClick={() => changeQty(product.id, 1)}>+</button></div></div>)}</div><div className="total"><div className="muted">Total</div><strong>{money(total)}</strong><button className="primary" disabled={!cart.length || total <= 0} onClick={() => { setCash(String(total)); setPaying(true) }}>Bayar</button></div></section>
        </div>
      </>}
      {page === 'Produk' && <section className="products-page"><section className="panel"><h2 style={{marginTop:0}}>{editing ? 'Edit produk' : 'Tambah produk'}</h2><form onSubmit={saveProduct}><div className="form-grid"><label>Nama<input name="name" defaultValue={editing?.name || ''} required /></label><label>Nama singkat<input name="short_name" defaultValue={editing?.short_name || ''} /></label><label>SKU<input name="sku" defaultValue={editing?.sku || ''} required /></label><label>Barcode<input name="barcode" defaultValue={editing?.barcode || ''} /></label><label>Kategori<select name="category_id" defaultValue={editing?.category_id || ''}><option value="">Tanpa kategori</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label>Satuan<select name="base_unit_id" defaultValue={editing?.base_unit_id || ''} required><option value="">Pilih satuan</option>{units.map(u => <option key={u.id} value={u.id}>{u.name}</option>)}</select></label><label>Harga modal<input name="current_cost" type="number" min="0" defaultValue={editing?.current_cost || 0} /></label></div><button className="primary" disabled={saving}>{saving ? 'Menyimpan...' : 'Simpan produk'}</button>{editing && <button type="button" className="secondary" style={{marginTop:8}} onClick={() => setEditing(null)}>Batal edit</button>}</form></section><section className="panel table-wrap"><table><thead><tr><th>Produk</th><th>SKU</th><th>Barcode</th><th>Kategori</th><th>Satuan</th><th>Harga</th><th></th></tr></thead><tbody>{products.map(p => <tr key={p.id}><td>{p.name}</td><td>{p.sku}</td><td>{p.barcode || '-'}</td><td>{categoryName(p.category_id)}</td><td>{unitName(p.base_unit_id)}</td><td>{money(priceFor(p,1))}</td><td><button className="secondary" onClick={() => setEditing(p)}>Edit</button></td></tr>)}</tbody></table></section></section>}
      {['Dashboard','Stok','Pelanggan','Supplier','Pembelian','Laporan','Pengaturan'].includes(page) && <section className="panel"><h2>Modul {page}</h2><p className="muted">Fondasi web sudah disiapkan. Modul ini akan menggunakan Supabase yang sama, tanpa kembali ke Android native.</p></section>}
    </main>
    {paying && <div style={{position:'fixed',inset:0,background:'#071a3355',display:'grid',placeItems:'center',padding:16,zIndex:30}}><div className="panel" style={{width:'min(460px,100%)',maxHeight:'90vh',overflow:'auto'}}><h2 style={{marginTop:0}}>Pembayaran Tunai</h2><p className="muted">Total {money(total)}</p>{cart.map(({product,qty}) => <p key={product.id} className="muted">{qty} × {product.name} = {money(qty*priceFor(product,qty))}</p>)}<label className="form-grid" style={{display:'grid'}}>Uang diterima<input value={cash} onChange={e => setCash(e.target.value.replace(/\D/g,''))} inputMode="numeric" autoFocus /></label><h3>{change >= 0 ? `Kembalian ${money(change)}` : `Kurang ${money(-change)}`}</h3><button className="primary" disabled={saving || received < total} onClick={checkout}>{saving ? 'Memproses...' : 'Konfirmasi transaksi'}</button><button className="secondary" style={{width:'100%',marginTop:8}} disabled={saving} onClick={() => setPaying(false)}>Batal</button></div></div>}
    <nav className="mobile-nav">{['Penjualan','Produk','Stok'].map(item => <button key={item} className={page === item ? 'active' : ''} onClick={() => setPage(item)}>{item}</button>)}</nav>
  </div>
}
