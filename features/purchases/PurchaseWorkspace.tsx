'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { requireSupabase } from '../../lib/supabase'
import styles from './PurchaseWorkspace.module.css'

type Supplier = { id: string; name: string; code?: string; payment_term_days?: number }
type Product = { id: string; name: string; sku: string; base_unit_id: string; current_cost: number; last_purchase_cost: number }
type CartItem = Product & { qty: number; unitCost: number }
type PurchaseRow = { id: string; order_no: string; supplier_id: string; order_date: string; expected_date: string | null; status: string; subtotal: number; discount_amount: number; tax_amount: number; other_cost: number; total_amount: number; notes: string | null; supplier?: { name: string } | null }

const money = (value: number) => `Rp ${Math.round(Number(value || 0)).toLocaleString('id-ID')}`
const today = () => new Date().toISOString().slice(0, 10)

export default function PurchaseWorkspace() {
  const [businessId, setBusinessId] = useState('')
  const [branchId, setBranchId] = useState('')
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [rows, setRows] = useState<PurchaseRow[]>([])
  const [cart, setCart] = useState<CartItem[]>([])
  const [supplierId, setSupplierId] = useState('')
  const [orderDate, setOrderDate] = useState(today())
  const [expectedDate, setExpectedDate] = useState('')
  const [paymentTerm, setPaymentTerm] = useState<'PAID' | 'CREDIT'>('PAID')
  const [dueDate, setDueDate] = useState('')
  const [discount, setDiscount] = useState('0')
  const [tax, setTax] = useState('0')
  const [otherCost, setOtherCost] = useState('0')
  const [notes, setNotes] = useState('')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<'new' | 'history'>('new')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const db = requireSupabase()
      const { data: businesses, error: be } = await db.from('businesses').select('id').eq('code', 'TOKO_MAJU_JAYA').limit(1)
      if (be) throw be
      const bid = businesses?.[0]?.id
      if (!bid) throw new Error('Business aktif tidak ditemukan.')
      setBusinessId(bid)
      const [{ data: branches, error: bre } , { data: sups, error: se }, { data: prods, error: pe }, { data: purchases, error: qe }] = await Promise.all([
        db.from('branches').select('id').eq('business_id', bid).limit(1),
        db.from('suppliers').select('id,name,code,payment_term_days').eq('business_id', bid).eq('is_active', true).order('name'),
        db.from('products').select('id,name,sku,base_unit_id,current_cost,last_purchase_cost').eq('business_id', bid).eq('is_active', true).order('name').limit(500),
        db.from('purchase_orders').select('id,order_no,supplier_id,order_date,expected_date,status,subtotal,discount_amount,tax_amount,other_cost,total_amount,notes,suppliers(name)').eq('business_id', bid).order('order_date', { ascending: false }).limit(100)
      ])
      if (bre) throw bre; if (se) throw se; if (pe) throw pe; if (qe) throw qe
      const brid = branches?.[0]?.id
      if (!brid) throw new Error('Branch aktif tidak ditemukan.')
      setBranchId(brid); setSuppliers((sups || []) as Supplier[]); setProducts((prods || []) as Product[]); setRows((purchases || []) as PurchaseRow[])
    } catch (e: any) { setError(e?.message || 'Gagal memuat modul pembelian.') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + item.qty * item.unitCost, 0), [cart])
  const discountAmount = Math.max(0, Math.min(subtotal, Number(discount) || 0))
  const taxAmount = Math.max(0, Number(tax) || 0)
  const otherAmount = Math.max(0, Number(otherCost) || 0)
  const total = Math.max(0, subtotal - discountAmount + taxAmount + otherAmount)
  const visibleProducts = products.filter(p => `${p.name} ${p.sku}`.toLowerCase().includes(search.toLowerCase())).slice(0, 20)
  const supplier = suppliers.find(s => s.id === supplierId)

  function addProduct(product: Product) {
    setCart(prev => prev.some(x => x.id === product.id) ? prev.map(x => x.id === product.id ? { ...x, qty: x.qty + 1 } : x) : [...prev, { ...product, qty: 1, unitCost: Number(product.last_purchase_cost || product.current_cost || 0) }])
  }
  function updateQty(id: string, qty: number) { setCart(prev => prev.map(x => x.id === id ? { ...x, qty: Math.max(0, qty) } : x).filter(x => x.qty > 0)) }
  function updateCost(id: string, unitCost: number) { setCart(prev => prev.map(x => x.id === id ? { ...x, unitCost: Math.max(0, unitCost) } : x)) }
  function resetForm() { setCart([]); setSupplierId(''); setOrderDate(today()); setExpectedDate(''); setPaymentTerm('PAID'); setDueDate(''); setDiscount('0'); setTax('0'); setOtherCost('0'); setNotes(''); setSearch('') }

  async function savePurchase() {
    setError('')
    if (!businessId || !branchId) return setError('Business atau branch aktif belum tersedia.')
    if (!supplierId) return setError('Supplier wajib dipilih.')
    if (!cart.length) return setError('Tambahkan minimal satu barang.')
    if (paymentTerm === 'CREDIT' && !dueDate) return setError('Jatuh tempo wajib diisi untuk pembelian tempo.')
    setSaving(true)
    try {
      const db = requireSupabase()
      const orderNo = `PO-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`
      const { data: order, error: oe } = await db.from('purchase_orders').insert({ business_id: businessId, branch_id: branchId, supplier_id: supplierId, order_no: orderNo, order_date: new Date(`${orderDate}T00:00:00`).toISOString(), expected_date: expectedDate || null, status: 'DRAFT', subtotal: Math.round(subtotal), discount_amount: Math.round(discountAmount), tax_amount: Math.round(taxAmount), other_cost: Math.round(otherAmount), total_amount: Math.round(total), notes: notes || null }).select('id').single()
      if (oe) throw oe
      const { error: ie } = await db.from('purchase_order_items').insert(cart.map(item => ({ purchase_order_id: order.id, product_id: item.id, unit_id: item.base_unit_id, qty: item.qty, unit_cost: Math.round(item.unitCost), discount_amount: 0, tax_amount: 0, line_total: Math.round(item.qty * item.unitCost), received_qty: 0 })))
      if (ie) throw ie
      resetForm(); await load(); setActiveTab('history')
    } catch (e: any) { setError(e?.message || 'Gagal menyimpan pembelian.') }
    finally { setSaving(false) }
  }

  return <div className={styles.page}>
    <div className={styles.hero}><div><span>TRANSAKSI · PEMBELIAN</span><h1>Pembelian</h1><p>Kelola pesanan supplier, biaya pembelian, penerimaan, dan alur pembayaran tunai atau tempo.</p></div><div className={styles.heroStats}><div><small>Draft</small><strong>{rows.filter(r => r.status === 'DRAFT').length}</strong></div><div><small>Total order</small><strong>{rows.length}</strong></div></div></div>
    {error && <div className={styles.alert}>{error}</div>}
    <div className={styles.tabs}><button className={activeTab === 'new' ? styles.activeTab : ''} onClick={() => setActiveTab('new')}>+ Pembelian baru</button><button className={activeTab === 'history' ? styles.activeTab : ''} onClick={() => setActiveTab('history')}>Riwayat pembelian <b>{rows.length}</b></button></div>

    {activeTab === 'new' ? <div className={styles.workspace}>
      <section className={styles.products}><div className={styles.cardHead}><div><strong>Pilih barang</strong><span>Cari produk berdasarkan nama atau SKU</span></div></div><input className={styles.search} value={search} onChange={e => setSearch(e.target.value)} placeholder="Cari barang..." />{loading ? <div className={styles.empty}>Memuat katalog...</div> : <div className={styles.productList}>{visibleProducts.map(product => <button className={styles.product} key={product.id} onClick={() => addProduct(product)}><div className={styles.art}>{product.name.slice(0,1).toUpperCase()}</div><div><strong>{product.name}</strong><span>{product.sku}</span></div><b>{money(Number(product.last_purchase_cost || product.current_cost || 0))}</b></button>)}</div>}</section>
      <section className={styles.order}><div className={styles.cardHead}><div><strong>Detail pembelian</strong><span>Supplier, item, biaya dan termin pembayaran</span></div><button className={styles.ghost} onClick={resetForm}>Reset</button></div>
        <div className={styles.formGrid}><label>Supplier<select value={supplierId} onChange={e => { setSupplierId(e.target.value); const s = suppliers.find(x => x.id === e.target.value); if (s?.payment_term_days && s.payment_term_days > 0) { setPaymentTerm('CREDIT'); const d = new Date(); d.setDate(d.getDate() + s.payment_term_days); setDueDate(d.toISOString().slice(0,10)) } }}><option value="">Pilih supplier</option>{suppliers.map(s => <option key={s.id} value={s.id}>{s.name}{s.code ? ` · ${s.code}` : ''}</option>)}</select></label><label>Tanggal<input type="date" value={orderDate} onChange={e => setOrderDate(e.target.value)} /></label><label>Estimasi datang<input type="date" value={expectedDate} onChange={e => setExpectedDate(e.target.value)} /></label></div>
        <div className={styles.term}><button className={paymentTerm === 'PAID' ? styles.termActive : ''} onClick={() => setPaymentTerm('PAID')}><strong>Lunas</strong><span>Dibayar saat transaksi pembelian.</span></button><button className={paymentTerm === 'CREDIT' ? styles.termActive : ''} onClick={() => setPaymentTerm('CREDIT')}><strong>Tempo / Hutang</strong><span>Masuk ke hutang supplier dan ditagihkan sesuai jatuh tempo.</span></button></div>{paymentTerm === 'CREDIT' && <label>Jatuh tempo<input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} /><small>{supplier?.payment_term_days ? `Termin supplier ${supplier.payment_term_days} hari.` : 'Tentukan tanggal jatuh tempo.'}</small></label>}
        <div className={styles.cart}>{cart.length === 0 ? <div className={styles.empty}>Belum ada barang. Pilih barang dari panel kiri.</div> : cart.map(item => <div className={styles.cartRow} key={item.id}><div><strong>{item.name}</strong><span>{item.sku}</span></div><input type="number" min="1" value={item.qty} onChange={e => updateQty(item.id, Number(e.target.value))} /><input type="number" min="0" value={item.unitCost} onChange={e => updateCost(item.id, Number(e.target.value))} /><b>{money(item.qty * item.unitCost)}</b><button onClick={() => updateQty(item.id, 0)}>×</button></div>)}</div>
        <div className={styles.costGrid}><label>Diskon<input type="number" min="0" value={discount} onChange={e => setDiscount(e.target.value)} /></label><label>Pajak<input type="number" min="0" value={tax} onChange={e => setTax(e.target.value)} /></label><label>Biaya lain<input type="number" min="0" value={otherCost} onChange={e => setOtherCost(e.target.value)} /></label></div><label>Catatan<textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Catatan supplier, nomor dokumen, atau informasi penerimaan..." /></label>
        <div className={styles.summary}><div><span>Subtotal</span><b>{money(subtotal)}</b></div><div><span>Diskon</span><b>-{money(discountAmount)}</b></div><div><span>Pajak + biaya lain</span><b>{money(taxAmount + otherAmount)}</b></div><div className={styles.total}><span>Total pembelian</span><strong>{money(total)}</strong></div>{paymentTerm === 'CREDIT' && <div className={styles.creditNote}><span>Termin</span><strong>Hutang supplier · jatuh tempo {dueDate || '-'}</strong></div>}</div>
        <button className={styles.save} disabled={saving || !supplierId || !cart.length} onClick={savePurchase}>{saving ? 'Menyimpan...' : `Simpan pembelian ${money(total)}`}</button><small className={styles.footnote}>{paymentTerm === 'CREDIT' ? 'Dokumen disimpan sebagai DRAFT. Hutang resmi sebaiknya dibentuk saat barang diterima/invoice dikonfirmasi.' : 'Dokumen disimpan sebagai DRAFT dan belum mengubah stok.'}</small>
      </section>
    </div> : <section className={styles.history}><div className={styles.cardHead}><div><strong>Riwayat pembelian</strong><span>{rows.length} dokumen</span></div><button className={styles.ghost} onClick={load}>Muat ulang</button></div>{rows.length === 0 ? <div className={styles.empty}>Belum ada pembelian.</div> : <div className={styles.table}>{rows.map(row => <div className={styles.tableRow} key={row.id}><div><strong>{row.order_no}</strong><span>{row.supplier?.name || row.supplier_id} · {new Date(row.order_date).toLocaleDateString('id-ID')}</span></div><span className={styles.badge}>{row.status}</span><div><small>Total</small><strong>{money(row.total_amount)}</strong></div><button onClick={() => { setActiveTab('new'); setSupplierId(row.supplier_id) }}>Buka</button></div>)}</div>}</section>}
    </div>
}
