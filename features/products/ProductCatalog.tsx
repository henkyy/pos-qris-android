'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../../components/ui/Button'
import Modal from '../../components/ui/Modal'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './ProductWorkspace.module.css'

type Opt = { id: string; name: string; symbol?: string | null }
type Unit = {
  id?: string
  unit_id: string
  conversion_to_base: number
  is_purchase_unit: boolean
  is_sales_unit: boolean
}
type Price = {
  id?: string
  unit_id: string
  min_qty: number
  price: number
  discount_percent: number
  valid_from?: string | null
  valid_until?: string | null
}
type Product = {
  id: string
  name: string
  sku: string
  barcode: string | null
  category_id: string | null
  base_unit_id: string
  current_cost: number
  last_purchase_cost: number
  min_stock: number
  reorder_point: number
  is_active: boolean
  category?: string
  base_unit?: string
  units: Unit[]
  prices: Price[]
}
type MasterKind = 'category' | 'unit'

const rp = (v: number) => `Rp ${new Intl.NumberFormat('id-ID').format(Number(v) || 0)}`
const normalize = (v: string) => v.trim().toLocaleLowerCase('id-ID')
const makeCode = (v: string) => v.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_|_$/g, '').slice(0, 30)

export default function ProductCatalog() {
  const db = useMemo(() => requireSupabase(), [])
  const [rows, setRows] = useState<Product[]>([])
  const [cats, setCats] = useState<Opt[]>([])
  const [units, setUnits] = useState<Opt[]>([])
  const [priceList, setPriceList] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [f, setF] = useState<any>({})
  const [us, setUs] = useState<Unit[]>([])
  const [ps, setPs] = useState<Price[]>([])
  const [masterKind, setMasterKind] = useState<MasterKind | null>(null)
  const [masterUnitIndex, setMasterUnitIndex] = useState<number | null>(null)
  const [masterName, setMasterName] = useState('')
  const [masterSymbol, setMasterSymbol] = useState('')
  const [masterDecimals, setMasterDecimals] = useState('0')
  const [masterSaving, setMasterSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const w = await getActiveWorkspace()
      const [{ data: p, error: pe }, { data: c, error: ce }, { data: u, error: ue }, { data: pl, error: ple }] = await Promise.all([
        db.from('products').select('*').eq('business_id', w.business.id).order('name'),
        db.from('categories').select('id,name').eq('business_id', w.business.id).eq('is_active', true).order('name'),
        db.from('units').select('id,name,symbol').eq('business_id', w.business.id).eq('is_active', true).order('name'),
        db.from('price_lists').select('id').eq('business_id', w.business.id).eq('is_default', true).eq('is_active', true).limit(1),
      ])
      if (pe) throw pe
      if (ce) throw ce
      if (ue) throw ue
      if (ple) throw ple

      const lid = pl?.[0]?.id || ''
      setPriceList(lid)
      const ids = (p || []).map((x: any) => x.id)
      const [{ data: pu, error: pue }, { data: pp, error: ppe }] = await Promise.all([
        ids.length ? db.from('product_units').select('*').in('product_id', ids) : Promise.resolve({ data: [], error: null }),
        ids.length && lid ? db.from('product_prices').select('*').eq('price_list_id', lid).in('product_id', ids).is('valid_until', null) : Promise.resolve({ data: [], error: null }),
      ])
      if (pue) throw pue
      if (ppe) throw ppe

      const um = new Map((u || []).map((x: any) => [x.id, x]))
      const cm = new Map((c || []).map((x: any) => [x.id, x.name]))
      const ub = new Map<string, Unit[]>()
      const pb = new Map<string, Price[]>()
      for (const x of pu || []) {
        const a = ub.get(x.product_id) || []
        a.push(x)
        ub.set(x.product_id, a)
      }
      for (const x of pp || []) {
        const a = pb.get(x.product_id) || []
        a.push(x)
        pb.set(x.product_id, a)
      }
      setRows((p || []).map((x: any) => ({
        ...x,
        category: cm.get(x.category_id) || '-',
        base_unit: um.get(x.base_unit_id)?.name || '-',
        units: ub.get(x.id) || [],
        prices: pb.get(x.id) || [],
      })))
      setCats(c || [])
      setUnits((u || []).map((x: any) => ({ id: x.id, name: x.name, symbol: x.symbol })))
    } catch (e: any) {
      setError(e?.message || 'Gagal memuat produk.')
    } finally {
      setLoading(false)
    }
  }, [db])

  useEffect(() => { load() }, [load])

  const filtered = rows.filter(x => `${x.name} ${x.sku} ${x.barcode || ''} ${x.category}`.toLowerCase().includes(q.toLowerCase()))

  function create() {
    setEditing(null)
    setF({ name: '', sku: '', barcode: '', category_id: '', base_unit_id: '', current_cost: '0', min_stock: '0', reorder_point: '0' })
    setUs([])
    setPs([])
    setMasterKind(null)
    setError('')
    setOpen(true)
  }

  function edit(p: Product) {
    setEditing(p)
    setF({ name: p.name, sku: p.sku, barcode: p.barcode || '', category_id: p.category_id || '', base_unit_id: p.base_unit_id, current_cost: String(p.current_cost), min_stock: String(p.min_stock), reorder_point: String(p.reorder_point) })
    setUs(p.units.map(x => ({ ...x })))
    setPs(p.prices.map(x => ({ ...x })))
    setMasterKind(null)
    setError('')
    setOpen(true)
  }

  function openMasterCreator(kind: MasterKind, unitIndex: number | null = null, initialName = '') {
    setMasterKind(kind)
    setMasterUnitIndex(unitIndex)
    setMasterName(initialName)
    setMasterSymbol('')
    setMasterDecimals('0')
    setError('')
  }

  function closeMasterCreator() {
    setMasterKind(null)
    setMasterUnitIndex(null)
    setMasterName('')
    setMasterSymbol('')
    setMasterDecimals('0')
  }

  async function saveInlineMaster() {
    const name = masterName.trim()
    if (!masterKind || !name) {
      setError(`Nama ${masterKind === 'category' ? 'kategori' : 'satuan'} wajib diisi.`)
      return
    }

    const list = masterKind === 'category' ? cats : units
    const existing = list.find(x => normalize(x.name) === normalize(name))
    if (existing) {
      if (masterKind === 'category') setF((v: any) => ({ ...v, category_id: existing.id }))
      else if (masterUnitIndex !== null) setUs(a => a.map((x, i) => i === masterUnitIndex ? { ...x, unit_id: existing.id } : x))
      closeMasterCreator()
      return
    }

    setMasterSaving(true)
    setError('')
    try {
      const w = await getActiveWorkspace()
      const code = makeCode(name)
      if (!code) throw new Error('Nama menghasilkan kode yang tidak valid. Gunakan nama yang mengandung huruf atau angka.')
      if (masterKind === 'category') {
        const r = await db.from('categories').insert({ business_id: w.business.id, name, code, is_active: true }).select('id,name').single()
        if (r.error) throw r.error
        setCats(a => [...a, r.data].sort((x, y) => x.name.localeCompare(y.name)))
        setF((v: any) => ({ ...v, category_id: r.data.id }))
      } else {
        const decimalPlaces = Math.max(0, Math.min(6, Number(masterDecimals || 0)))
        const r = await db.from('units').insert({ business_id: w.business.id, name, code, symbol: masterSymbol.trim() || null, decimal_places: decimalPlaces, is_active: true }).select('id,name,symbol').single()
        if (r.error) throw r.error
        setUnits(a => [...a, r.data].sort((x, y) => x.name.localeCompare(y.name)))
        if (masterUnitIndex !== null) setUs(a => a.map((x, i) => i === masterUnitIndex ? { ...x, unit_id: r.data.id } : x))
        else setF((v: any) => ({ ...v, base_unit_id: r.data.id }))
      }
      closeMasterCreator()
    } catch (e: any) {
      setError(e?.message || 'Gagal membuat master data. Pastikan kode belum digunakan.')
    } finally {
      setMasterSaving(false)
    }
  }

  function addUnit() {
    if (us.length < 3) setUs(a => [...a, { unit_id: '', conversion_to_base: 1, is_purchase_unit: false, is_sales_unit: false }])
  }

  function addPrice() {
    setPs(a => [...a, { unit_id: f.base_unit_id || '', min_qty: 1, price: 0, discount_percent: 0 }])
  }

  function setBaseUnit(unitId: string) {
    setF((v: any) => ({ ...v, base_unit_id: unitId }))
    setUs(a => a.map(x => x.unit_id === unitId ? { ...x, conversion_to_base: 1 } : x))
  }

  async function save() {
    if (!f.name?.trim() || !f.sku?.trim() || !f.base_unit_id) {
      setError('Nama, SKU dan satuan dasar wajib diisi.')
      return
    }

    const list = us.filter(x => x.unit_id).map(x => ({ ...x, conversion_to_base: x.unit_id === f.base_unit_id ? 1 : Number(x.conversion_to_base) }))
    if (!list.some(x => x.unit_id === f.base_unit_id)) list.unshift({ unit_id: f.base_unit_id, conversion_to_base: 1, is_purchase_unit: true, is_sales_unit: true })
    if (list.some(x => Number(x.conversion_to_base) <= 0) || new Set(list.map(x => x.unit_id)).size !== list.length) {
      setError('Satuan harus unik dan konversinya harus lebih dari 0.')
      return
    }

    const prices = ps.filter(x => x.unit_id && Number(x.price) >= 0).map(x => ({ ...x, min_qty: Math.max(1, Number(x.min_qty || 1)), price: Math.round(Number(x.price || 0)), discount_percent: Number(x.discount_percent || 0) }))
    if (prices.some(x => x.discount_percent < 0 || x.discount_percent > 100)) {
      setError('Diskon harga harus berada di antara 0% sampai 100%.')
      return
    }
    if (new Set(prices.map(x => `${x.unit_id}:${x.min_qty}`)).size !== prices.length) {
      setError('Harga duplikat untuk satuan dan minimum qty yang sama. Gabungkan menjadi satu baris.')
      return
    }

    setSaving(true)
    setError('')
    try {
      const w = await getActiveWorkspace()
      let id = editing?.id
      const payload = {
        name: f.name.trim(),
        sku: f.sku.trim(),
        barcode: f.barcode?.trim() || null,
        category_id: f.category_id || null,
        base_unit_id: f.base_unit_id,
        product_type: 'GOODS',
        track_batch: false,
        track_expiry: false,
        min_stock: Number(f.min_stock || 0),
        reorder_point: Number(f.reorder_point || 0),
        cost_method: 'MOVING_AVERAGE',
        current_cost: editing ? editing.current_cost : Math.max(0, Number(f.current_cost || 0)),
        last_purchase_cost: editing?.last_purchase_cost || 0,
        is_active: editing?.is_active ?? true,
      }

      if (id) {
        const r = await db.from('products').update(payload).eq('id', id).eq('business_id', w.business.id)
        if (r.error) throw r.error
      } else {
        const r = await db.from('products').insert({ ...payload, business_id: w.business.id }).select('id').single()
        if (r.error) throw r.error
        id = r.data.id
      }
      if (!id) throw new Error('ID produk tidak ditemukan.')

      for (const x of list) {
        const r = await db.from('product_units').upsert({ product_id: id, unit_id: x.unit_id, conversion_to_base: Number(x.conversion_to_base), is_purchase_unit: !!x.is_purchase_unit, is_sales_unit: !!x.is_sales_unit }, { onConflict: 'product_id,unit_id' })
        if (r.error) throw r.error
      }

      const now = new Date().toISOString()
      const oldPrices = editing?.prices || []
      for (const x of prices) {
        const old = x.id ? oldPrices.find(p => p.id === x.id) : undefined
        const changed = !!old && (old.unit_id !== x.unit_id || Number(old.min_qty) !== Number(x.min_qty) || Number(old.price) !== Number(x.price) || Number(old.discount_percent) !== Number(x.discount_percent))
        if (old?.id && !changed) continue

        if (old?.id && changed) {
          const close = await db.from('product_prices').update({ valid_until: now }).eq('id', old.id).is('valid_until', null)
          if (close.error) throw close.error
        }

        const insert = await db.from('product_prices').insert({ price_list_id: priceList, product_id: id, unit_id: x.unit_id, min_qty: Number(x.min_qty), price: Number(x.price), discount_percent: Number(x.discount_percent), valid_from: now, valid_until: null })
        if (insert.error) throw insert.error
      }

      setOpen(false)
      await load()
    } catch (e: any) {
      setError(e?.message || 'Gagal menyimpan produk.')
    } finally {
      setSaving(false)
    }
  }

  const unitName = (id: string) => units.find(x => x.id === id)?.name || 'Satuan'

  return (
    <div className="module-page">
      <div className={styles.hero}>
        <div>
          <span className="eyebrow">MASTER · PRODUK</span>
          <h1>Produk</h1>
          <p>Identitas, kategori, HPP, harga jual, satuan dan konversi dikelola dalam satu tempat.</p>
        </div>
        <Button onClick={create}>+ Tambah Produk</Button>
      </div>

      <div className={styles.summary}>
        <div><span>Total produk</span><strong>{rows.length}</strong></div>
        <div><span>Harga terisi</span><strong>{rows.filter(x => x.prices.length).length}</strong></div>
        <div><span>Multi-satuan</span><strong>{rows.filter(x => x.units.length > 1).length}</strong></div>
        <div><span>Produk aktif</span><strong>{rows.filter(x => x.is_active).length}</strong></div>
      </div>

      {error && <div className="module-alert">{error}</div>}

      <section className="module-card">
        <div className={styles.toolbar}>
          <div><strong>Daftar Produk</strong><span>{loading ? 'Memuat…' : `${filtered.length} produk`}</span></div>
          <input value={q} onChange={e => setQ(e.target.value)} placeholder="Cari nama, SKU, barcode…" />
          <Button variant="secondary" onClick={load}>Muat ulang</Button>
        </div>
        {loading ? <div className={styles.empty}>Memuat katalog…</div> : <div className="ui-table-wrap">
          <table className="ui-table">
            <thead><tr><th>Produk</th><th>Kategori</th><th>HPP</th><th>Harga jual</th><th>Satuan</th><th>Status</th><th>Aksi</th></tr></thead>
            <tbody>{filtered.map(p => <tr key={p.id}>
              <td><strong>{p.name}</strong><small>{p.sku}{p.barcode ? ` · ${p.barcode}` : ''}</small></td>
              <td>{p.category}</td>
              <td>{rp(p.current_cost)}</td>
              <td>{p.prices.length ? p.prices.map(x => `${unitName(x.unit_id)}: ${rp(x.price)}`).join(' · ') : <span className={styles.missing}>Belum diatur</span>}</td>
              <td>{p.base_unit} · {p.units.length} unit</td>
              <td>{p.is_active ? 'Aktif' : 'Nonaktif'}</td>
              <td><button className="table-edit" onClick={() => edit(p)}>Edit</button></td>
            </tr>)}</tbody>
          </table>
        </div>}
      </section>

      <Modal open={open} title={editing ? `Edit Produk · ${editing.name}` : 'Tambah Produk'} onClose={() => setOpen(false)}>
        <div className={styles.form}>
          <div className={styles.section}>
            <h3>Identitas</h3>
            <div className={styles.grid}>
              <label>Nama *<input value={f.name || ''} onChange={e => setF({ ...f, name: e.target.value })} /></label>
              <label>SKU *<input value={f.sku || ''} onChange={e => setF({ ...f, sku: e.target.value })} /></label>
              <label>Barcode<input value={f.barcode || ''} onChange={e => setF({ ...f, barcode: e.target.value })} /></label>
              <div>
                <label>Kategori</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <select style={{ flex: 1 }} value={f.category_id || ''} onChange={e => setF({ ...f, category_id: e.target.value })}>
                    <option value="">Tanpa kategori</option>
                    {cats.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
                  </select>
                  <Button variant="secondary" onClick={() => openMasterCreator('category')}>+ Baru</Button>
                </div>
              </div>
            </div>
            {masterKind === 'category' && <div className="module-card" style={{ marginTop: 12, padding: 12 }}>
              <strong>Tambah kategori tanpa keluar dari Produk</strong>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <input autoFocus style={{ flex: 1 }} value={masterName} onChange={e => setMasterName(e.target.value)} placeholder="Nama kategori, misalnya Minuman" />
                <Button onClick={saveInlineMaster} disabled={masterSaving}>{masterSaving ? 'Menyimpan…' : 'Tambah'}</Button>
                <Button variant="secondary" onClick={closeMasterCreator} disabled={masterSaving}>Batal</Button>
              </div>
            </div>}
          </div>

          <div className={styles.section}>
            <h3>HPP & stok</h3>
            <p>HPP awal hanya diisi saat membuat produk. Setelah transaksi pembelian, Moving Average memperbarui HPP melalui penerimaan barang.</p>
            <div className={styles.grid}>
              <label>HPP / unit{editing ? <input type="number" value={f.current_cost} readOnly /> : <input type="number" min="0" value={f.current_cost} onChange={e => setF({ ...f, current_cost: e.target.value })} />}</label>
              <label>Minimum stok<input type="number" min="0" value={f.min_stock} onChange={e => setF({ ...f, min_stock: e.target.value })} /></label>
              <label>Titik pesan ulang<input type="number" min="0" value={f.reorder_point} onChange={e => setF({ ...f, reorder_point: e.target.value })} /></label>
              <div className={styles.readonly}><span>Metode</span><strong>Moving Average</strong></div>
            </div>
          </div>

          <div className={styles.section}>
            <div className={styles.sectionHead}>
              <div><h3>Satuan & konversi</h3><p>Satuan adalah master global. Konversi adalah milik produk, misalnya BOX = 12 PCS. Maksimal 3 unit pada UI.</p></div>
              <Button variant="secondary" onClick={addUnit} disabled={us.length >= 3}>+ Satuan</Button>
            </div>
            <div style={{ display: 'grid', gap: 8, marginBottom: 10 }}>
              <div>
                <label>Satuan dasar *</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <select style={{ flex: 1 }} value={f.base_unit_id || ''} onChange={e => setBaseUnit(e.target.value)}>
                    <option value="">Pilih satuan dasar</option>
                    {units.map(x => <option key={x.id} value={x.id}>{x.name}{x.symbol ? ` (${x.symbol})` : ''}</option>)}
                  </select>
                  <Button variant="secondary" onClick={() => openMasterCreator('unit')}>+ Baru</Button>
                </div>
              </div>
            </div>
            {masterKind === 'unit' && masterUnitIndex === null && <div className="module-card" style={{ marginBottom: 12, padding: 12 }}>
              <strong>Tambah satuan tanpa keluar dari Produk</strong>
              <div className={styles.grid} style={{ marginTop: 8 }}>
                <label>Nama *<input autoFocus value={masterName} onChange={e => setMasterName(e.target.value)} placeholder="Contoh: Dus" /></label>
                <label>Simbol<input value={masterSymbol} onChange={e => setMasterSymbol(e.target.value)} placeholder="Contoh: dus" /></label>
                <label>Desimal<input type="number" min="0" max="6" value={masterDecimals} onChange={e => setMasterDecimals(e.target.value)} /></label>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
                <Button variant="secondary" onClick={closeMasterCreator} disabled={masterSaving}>Batal</Button>
                <Button onClick={saveInlineMaster} disabled={masterSaving}>{masterSaving ? 'Menyimpan…' : 'Tambah Satuan'}</Button>
              </div>
            </div>}
            {us.map((u, i) => <div key={u.id || i}>
              <div className={styles.unitRow}>
                <div style={{ flex: 1 }}>
                  <select style={{ width: '100%' }} value={u.unit_id} onChange={e => setUs(a => a.map((x, j) => j === i ? { ...x, unit_id: e.target.value, conversion_to_base: e.target.value === f.base_unit_id ? 1 : x.conversion_to_base } : x))}>
                    <option value="">Pilih satuan</option>
                    {units.map(x => <option key={x.id} value={x.id}>{x.name}{x.symbol ? ` (${x.symbol})` : ''}</option>)}
                  </select>
                </div>
                <input type="number" min="0.000001" step="any" value={u.unit_id === f.base_unit_id ? 1 : u.conversion_to_base} disabled={u.unit_id === f.base_unit_id} onChange={e => setUs(a => a.map((x, j) => j === i ? { ...x, conversion_to_base: Number(e.target.value) } : x))} />
                <label className={styles.check}><input type="checkbox" checked={u.is_purchase_unit} onChange={e => setUs(a => a.map((x, j) => j === i ? { ...x, is_purchase_unit: e.target.checked } : x))} /> Beli</label>
                <label className={styles.check}><input type="checkbox" checked={u.is_sales_unit} onChange={e => setUs(a => a.map((x, j) => j === i ? { ...x, is_sales_unit: e.target.checked } : x))} /> Jual</label>
                <Button variant="secondary" onClick={() => openMasterCreator('unit', i)}>+ Baru</Button>
              </div>
              {masterKind === 'unit' && masterUnitIndex === i && <div className="module-card" style={{ marginTop: 8, padding: 12 }}>
                <strong>Tambah satuan untuk baris ini</strong>
                <div className={styles.grid} style={{ marginTop: 8 }}>
                  <label>Nama *<input autoFocus value={masterName} onChange={e => setMasterName(e.target.value)} placeholder="Contoh: Karton" /></label>
                  <label>Simbol<input value={masterSymbol} onChange={e => setMasterSymbol(e.target.value)} placeholder="Contoh: ctn" /></label>
                  <label>Desimal<input type="number" min="0" max="6" value={masterDecimals} onChange={e => setMasterDecimals(e.target.value)} /></label>
                </div>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
                  <Button variant="secondary" onClick={closeMasterCreator} disabled={masterSaving}>Batal</Button>
                  <Button onClick={saveInlineMaster} disabled={masterSaving}>{masterSaving ? 'Menyimpan…' : 'Tambah Satuan'}</Button>
                </div>
              </div>}
            </div>)}
          </div>

          <div className={styles.section}>
            <div className={styles.sectionHead}>
              <div><h3>Harga jual</h3><p>Harga baru membuat versi baru. Harga lama ditutup dengan valid_until agar histori transaksi tetap dapat ditelusuri.</p></div>
              <Button variant="secondary" onClick={addPrice} disabled={!f.base_unit_id}>+ Harga</Button>
            </div>
            {ps.map((x, i) => <div className={styles.priceRow} key={x.id || i}>
              <select value={x.unit_id} onChange={e => setPs(a => a.map((z, j) => j === i ? { ...z, unit_id: e.target.value } : z))}>
                <option value="">Pilih satuan</option>
                {us.filter(u => u.unit_id).map(u => <option key={u.unit_id} value={u.unit_id}>{unitName(u.unit_id)}</option>)}
              </select>
              <input type="number" min="0" value={x.price} onChange={e => setPs(a => a.map((z, j) => j === i ? { ...z, price: Number(e.target.value) } : z))} placeholder="Harga jual" />
              <input type="number" min="1" value={x.min_qty} onChange={e => setPs(a => a.map((z, j) => j === i ? { ...z, min_qty: Number(e.target.value) } : z))} placeholder="Min qty" />
            </div>)}
            {!ps.length && <div className={styles.hint}>Belum ada harga. Tambahkan harga sebelum produk digunakan di kasir.</div>}
          </div>

          <div className={styles.actions}>
            <Button variant="secondary" onClick={() => setOpen(false)} disabled={saving || masterSaving}>Batal</Button>
            <Button onClick={save} disabled={saving || masterSaving}>{saving ? 'Menyimpan…' : 'Simpan Produk'}</Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
