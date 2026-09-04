'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import Input from '../../components/ui/Input'
import Modal from '../../components/ui/Modal'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './EntityPage.module.css'

type Field = {
  key: string
  label: string
  required?: boolean
  type?: 'text' | 'number' | 'select'
  source?: string
  placeholder?: string
  help?: string
}
export type EntityConfig = {
  title: string
  eyebrow: string
  description: string
  table: string
  columns: string[]
  columnLabels?: Record<string, string>
  fields?: Field[]
  businessScoped?: boolean
  readOnly?: boolean
  softDelete?: boolean
  workflow?: string[]
  usageNote?: string
}

type Option = { id: string; name: string }
type InlineField = { key: string; label: string; type?: 'text' | 'number'; required?: boolean; placeholder?: string }

const currencyKeys = new Set(['credit_limit', 'total_amount', 'amount', 'original_amount', 'paid_amount', 'outstanding_amount'])
const foreignLabels: Record<string, string> = {
  product_id: 'products', location_id: 'locations', customer_id: 'customers', supplier_id: 'suppliers',
  category_id: 'categories', payment_method_id: 'payment_methods', unit_id: 'units'
}
const inlineSources = new Set(['customers', 'suppliers', 'categories', 'units'])

function formatNumber(value: unknown) {
  const n = Number(value)
  return Number.isFinite(n) ? new Intl.NumberFormat('id-ID').format(n) : String(value ?? '-')
}
function formatCell(key: string, value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (key === 'is_active') return value ? 'Aktif' : 'Nonaktif'
  if (currencyKeys.has(key)) return `Rp ${formatNumber(value)}`
  if (key === 'qty_base') return formatNumber(value)
  if (key === 'status') return String(value).replace(/_/g, ' ')
  if (key.endsWith('_at') || key.endsWith('_date')) {
    const date = new Date(String(value))
    if (!Number.isNaN(date.getTime())) return date.toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' })
  }
  return String(value)
}
function cellTone(key: string, value: unknown) {
  if (key === 'is_active') return value ? styles.success : styles.muted
  if (key === 'status') {
    const status = String(value).toLowerCase()
    if (['completed', 'paid', 'active', 'confirmed', 'received'].includes(status)) return styles.success
    if (['pending', 'draft', 'open', 'partial'].includes(status)) return styles.warning
    if (['cancelled', 'failed', 'overdue'].includes(status)) return styles.danger
  }
  return ''
}
function makeCode(name: string) {
  const code = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '').slice(0, 12)
  return code || 'DATA'
}
function inlineFields(source: string): InlineField[] {
  if (source === 'customers') return [
    { key: 'name', label: 'Nama pelanggan', required: true, placeholder: 'Contoh: Budi' },
    { key: 'code', label: 'Kode pelanggan', required: true, placeholder: 'Contoh: BUDI-01' },
    { key: 'phone', label: 'Telepon', placeholder: 'Opsional' },
  ]
  if (source === 'suppliers') return [
    { key: 'name', label: 'Nama supplier', required: true, placeholder: 'Contoh: PT Sumber Jaya' },
    { key: 'code', label: 'Kode supplier', required: true, placeholder: 'Contoh: SUP-01' },
    { key: 'phone', label: 'Telepon', placeholder: 'Opsional' },
  ]
  if (source === 'categories') return [
    { key: 'name', label: 'Nama kategori', required: true, placeholder: 'Contoh: Minuman' },
    { key: 'code', label: 'Kode kategori', required: true, placeholder: 'Contoh: MINUM' },
  ]
  return [
    { key: 'name', label: 'Nama satuan', required: true, placeholder: 'Contoh: Kilogram' },
    { key: 'code', label: 'Kode satuan', required: true, placeholder: 'Contoh: KG' },
    { key: 'symbol', label: 'Simbol', placeholder: 'Contoh: kg' },
    { key: 'decimal_places', label: 'Jumlah desimal', type: 'number', placeholder: '0' },
  ]
}

export default function EntityPage({ config }: { config: EntityConfig }) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([])
  const [labels, setLabels] = useState<Record<string, Record<string, string>>>({})
  const [options, setOptions] = useState<Record<string, Option[]>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Record<string, unknown> | null>(null)
  const [form, setForm] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [inlineSource, setInlineSource] = useState('')
  const [inlineForm, setInlineForm] = useState<Record<string, string>>({})
  const [inlineSaving, setInlineSaving] = useState(false)
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>('all')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const db = requireSupabase()
      const workspace = config.businessScoped ? await getActiveWorkspace() : null
      let query = db.from(config.table).select('*').limit(200)
      if (workspace) query = query.eq('business_id', workspace.business.id)
      const { data, error: qe } = await query
      if (qe) throw qe
      const nextRows = (data || []) as Record<string, unknown>[]
      const nextLabels: Record<string, Record<string, string>> = {}
      await Promise.all(config.columns.filter(key => foreignLabels[key]).map(async key => {
        const ids = [...new Set(nextRows.map(row => row[key]).filter(Boolean).map(String))]
        if (!ids.length) return
        const { data: lookup } = await db.from(foreignLabels[key]).select('id,name').in('id', ids)
        nextLabels[key] = Object.fromEntries((lookup || []).map((item: any) => [item.id, item.name]))
      }))

      const sources = [...new Set((config.fields || []).map(field => field.source).filter(Boolean))] as string[]
      const nextOptions: Record<string, Option[]> = {}
      await Promise.all(sources.map(async source => {
        let optionQuery = db.from(source).select('id,name').limit(500)
        if (source !== 'units') optionQuery = optionQuery.eq('is_active', true)
        if (workspace) optionQuery = optionQuery.eq('business_id', workspace.business.id)
        const { data: lookup, error: lookupError } = await optionQuery
        if (lookupError) throw lookupError
        nextOptions[source] = (lookup || []) as Option[]
      }))

      setRows(nextRows)
      setLabels(nextLabels)
      setOptions(nextOptions)
    } catch (e: any) {
      setError(e?.message || 'Gagal memuat data.')
    } finally {
      setLoading(false)
    }
  }, [config])

  useEffect(() => { load() }, [load])

  const displayRows = useMemo(() => rows.map(row => {
    const copy = { ...row }
    for (const key of config.columns) if (foreignLabels[key] && labels[key]?.[String(row[key])]) copy[key] = labels[key][String(row[key])]
    return copy
  }), [rows, labels, config.columns])

  const filteredRows = useMemo(() => {
    const needle = search.trim().toLowerCase()
    return displayRows.filter(row => {
      const matchesSearch = !needle || config.columns.some(key => String(row[key] ?? '').toLowerCase().includes(needle))
      const matchesActive = activeFilter === 'all' || (activeFilter === 'active' ? row.is_active !== false : row.is_active === false)
      return matchesSearch && matchesActive
    })
  }, [displayRows, search, activeFilter, config.columns])

  const activeCount = rows.filter(row => row.is_active !== false).length
  const inactiveCount = rows.filter(row => row.is_active === false).length
  const attentionCount = rows.filter(row => ['overdue', 'pending', 'partial', 'draft'].includes(String(row.status).toLowerCase())).length

  function startCreate() {
    setEditing(null)
    setForm({})
    setError('')
    setOpen(true)
  }
  function startEdit(row: Record<string, unknown>) {
    const original = rows.find(item => item.id === row.id) || row
    setEditing(original)
    setForm(Object.fromEntries((config.fields || []).map(f => [f.key, String(original[f.key] ?? '')])))
    setError('')
    setOpen(true)
  }
  function startInline(source: string) {
    setInlineSource(source)
    setInlineForm({ code: '' })
    setError('')
  }
  function closeInline() {
    if (!inlineSaving) {
      setInlineSource('')
      setInlineForm({})
    }
  }
  function updateInline(key: string, value: string) {
    setInlineForm(prev => {
      const next = { ...prev, [key]: value }
      if (key === 'name' && !prev.code) next.code = makeCode(value)
      return next
    })
  }

  async function createInline() {
    const fields = inlineFields(inlineSource)
    for (const field of fields) {
      if (field.required && !inlineForm[field.key]?.trim()) {
        setError(`${field.label} wajib diisi.`)
        return
      }
      if (field.type === 'number' && inlineForm[field.key] && Number.isNaN(Number(inlineForm[field.key]))) {
        setError(`${field.label} harus berupa angka.`)
        return
      }
    }
    setInlineSaving(true)
    setError('')
    try {
      const db = requireSupabase()
      const workspace = await getActiveWorkspace()
      const payload: Record<string, unknown> = { business_id: workspace.business.id, is_active: true }
      for (const field of fields) {
        if (field.type === 'number') payload[field.key] = Number(inlineForm[field.key] || 0)
        else payload[field.key] = inlineForm[field.key]?.trim() || null
      }
      const { data, error: insertError } = await db.from(inlineSource).insert(payload).select('id,name').single()
      if (insertError) throw insertError
      const option = data as Option
      setOptions(prev => ({ ...prev, [inlineSource]: [option, ...(prev[inlineSource] || [])] }))
      setForm(prev => ({ ...prev, [config.fields?.find(field => field.source === inlineSource)?.key || '']: option.id }))
      closeInline()
    } catch (e: any) {
      setError(e?.message || `Gagal membuat ${inlineSource}.`)
    } finally {
      setInlineSaving(false)
    }
  }

  async function save() {
    if (!config.fields?.length) return
    for (const field of config.fields) {
      if (field.required && !form[field.key]?.trim()) {
        setError(`${field.label} wajib diisi.`)
        return
      }
      if (field.type === 'number' && form[field.key] && Number.isNaN(Number(form[field.key]))) {
        setError(`${field.label} harus berupa angka.`)
        return
      }
    }
    setSaving(true)
    setError('')
    try {
      const db = requireSupabase()
      const payload: Record<string, unknown> = {}
      for (const field of config.fields) {
        if (field.type === 'number') payload[field.key] = Number(form[field.key] || 0)
        else payload[field.key] = form[field.key] || null
      }
      if (config.businessScoped && !editing) {
        const workspace = await getActiveWorkspace()
        payload.business_id = workspace.business.id
      }
      const result = editing?.id
        ? await db.from(config.table).update(payload).eq('id', editing.id)
        : await db.from(config.table).insert(payload)
      if (result.error) throw result.error
      setOpen(false)
      await load()
    } catch (e: any) {
      setError(e?.message || 'Gagal menyimpan data.')
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(row: Record<string, unknown>) {
    if (!row.id || !config.softDelete) return
    const currentlyActive = row.is_active !== false
    const action = currentlyActive ? 'menonaktifkan' : 'mengaktifkan'
    if (!window.confirm(`Yakin ingin ${action} ${String(row.name || row.sku || 'data')}? Data transaksi historis tetap aman.`)) return
    setError('')
    try {
      const db = requireSupabase()
      const result = await db.from(config.table).update({ is_active: !currentlyActive }).eq('id', row.id)
      if (result.error) throw result.error
      await load()
    } catch (e: any) {
      setError(e?.message || `Gagal ${action} data.`)
    }
  }

  const currentInlineFields = inlineSource ? inlineFields(inlineSource) : []
  const currentInlineLabel = inlineSource === 'customers' ? 'pelanggan' : inlineSource === 'suppliers' ? 'supplier' : inlineSource === 'categories' ? 'kategori' : 'satuan'

  return <div className="module-page">
    <div className={styles.heroRow}>
      <div className={styles.heroCopy}><span className="eyebrow">{config.eyebrow}</span><h1>{config.title}</h1><p>{config.description}</p></div>
      {!config.readOnly && config.fields?.length ? <Button onClick={startCreate}>+ Tambah {config.title}</Button> : null}
    </div>

    {config.workflow?.length ? <div className={styles.workflow}>
      {config.workflow.map((step, index) => <div className={styles.workflowStep} key={step}><span>{index + 1}</span><strong>{step}</strong>{index < config.workflow!.length - 1 ? <i>›</i> : null}</div>)}
    </div> : null}

    {config.usageNote ? <div className={styles.usageNote}><strong>Catatan proses:</strong> {config.usageNote}</div> : null}
    {error && <div className="module-alert">{error}</div>}

    <div className={styles.metrics}>
      <div className={styles.metric}><span>Total data</span><strong>{loading ? '…' : rows.length}</strong><small>Record dalam workspace aktif</small></div>
      <div className={styles.metric}><span>Aktif</span><strong>{loading ? '…' : activeCount}</strong><small>Siap digunakan</small></div>
      {config.softDelete ? <div className={styles.metric}><span>Nonaktif</span><strong>{loading ? '…' : inactiveCount}</strong><small>Tidak dipakai transaksi baru</small></div> : <div className={styles.metric}><span>Perlu perhatian</span><strong>{loading ? '…' : attentionCount}</strong><small>Status perlu ditindaklanjuti</small></div>}
    </div>

    <section className="module-card">
      <div className={styles.toolbar}>
        <div><strong>Daftar {config.title}</strong><span>{loading ? 'Memuat data…' : `${filteredRows.length} dari ${rows.length} data`}</span></div>
        <div className={styles.actions}>
          <label className={styles.search}><span>⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder={`Cari ${config.title.toLowerCase()}…`} /></label>
          {config.softDelete ? <select className={styles.filter} value={activeFilter} onChange={e => setActiveFilter(e.target.value as typeof activeFilter)}><option value="all">Semua status</option><option value="active">Aktif</option><option value="inactive">Nonaktif</option></select> : null}
          <Button variant="secondary" onClick={load} disabled={loading}>Muat ulang</Button>
        </div>
      </div>

      {loading ? <EmptyState title="Memuat data" text="Mengambil data dari workspace aktif." /> : filteredRows.length ? <div className="ui-table-wrap"><table className="ui-table"><thead><tr>{config.columns.map(c => <th key={c}>{config.columnLabels?.[c] || c.replace(/_/g, ' ')}</th>)}{!config.readOnly && config.fields?.length ? <th>Aksi</th> : null}</tr></thead><tbody>{filteredRows.map((row, i) => <tr key={String(row.id ?? i)}>{config.columns.map(c => <td key={c}><span className={cellTone(c, row[c])}>{formatCell(c, row[c])}</span></td>)}{!config.readOnly && config.fields?.length ? <td><div className="table-actions"><button className="table-edit" onClick={() => startEdit(row)}>Edit</button>{config.softDelete ? <button className="table-edit" onClick={() => toggleActive(rows.find(item => item.id === row.id) || row)}>{row.is_active === false ? 'Aktifkan' : 'Nonaktifkan'}</button> : null}</div></td> : null}</tr>)}</tbody></table></div> : <EmptyState title={search ? 'Data tidak ditemukan' : 'Belum ada data'} text={search ? 'Coba kata kunci atau filter status lain.' : 'Belum ada record yang tersedia untuk modul ini.'} />}
    </section>

    {config.fields?.length ? <Modal open={open} title={editing ? `Edit ${config.title}` : `Tambah ${config.title}`} onClose={() => setOpen(false)}>
      <div className={styles.formIntro}><strong>{editing ? 'Perbarui data dengan hati-hati.' : `Tambah ${config.title.toLowerCase()} baru.`}</strong><span>Data master yang sudah dipakai transaksi tidak dihapus dari histori. Gunakan Nonaktifkan jika tidak ingin dipakai lagi.</span></div>
      <div className="module-form">
        {config.fields.map(field => field.type === 'select' ? <div key={field.key} className={styles.selectWithAction}><label className="ui-field"><span>{field.label}{field.required ? ' *' : ''}</span><select value={form[field.key] || ''} onChange={e => setForm(prev => ({ ...prev, [field.key]: e.target.value }))}><option value="">Pilih {field.label.toLowerCase()}</option>{(options[field.source || ''] || []).map(option => <option key={option.id} value={option.id}>{option.name}</option>)}</select>{field.help ? <small>{field.help}</small> : null}</label>{field.source && inlineSources.has(field.source) ? <button type="button" className={styles.inlineLink} onClick={() => startInline(field.source!)}>+ Buat baru</button> : null}</div> : <div key={field.key}><Input label={`${field.label}${field.required ? ' *' : ''}`} type={field.type === 'number' ? 'number' : 'text'} value={form[field.key] || ''} placeholder={field.placeholder} onChange={e => setForm(prev => ({ ...prev, [field.key]: e.target.value }))} />{field.help ? <small className={styles.fieldHelp}>{field.help}</small> : null}</div>)}
        <div className={styles.formActions}><Button variant="secondary" onClick={() => setOpen(false)} disabled={saving || inlineSaving}>Batal</Button><Button onClick={save} disabled={saving || inlineSaving}>{saving ? 'Menyimpan…' : editing ? 'Simpan perubahan' : 'Simpan data'}</Button></div>
      </div>

      {inlineSource ? <div className={styles.inlinePanel}>
        <div className={styles.inlineHeader}><div><strong>Buat {currentInlineLabel} tanpa meninggalkan form</strong><span>Data baru akan langsung dipilih pada field {currentInlineLabel}.</span></div><button type="button" className={styles.inlineClose} onClick={closeInline} disabled={inlineSaving}>Tutup</button></div>
        <div className={styles.inlineGrid}>
          {currentInlineFields.map(field => <Input key={field.key} label={`${field.label}${field.required ? ' *' : ''}`} type={field.type === 'number' ? 'number' : 'text'} value={inlineForm[field.key] || ''} placeholder={field.placeholder} onChange={e => updateInline(field.key, e.target.value)} />)}
        </div>
        <div className={styles.inlineActions}><Button variant="secondary" onClick={closeInline} disabled={inlineSaving}>Batal</Button><Button onClick={createInline} disabled={inlineSaving}>{inlineSaving ? 'Membuat…' : `Buat ${currentInlineLabel}`}</Button></div>
      </div> : null}
    </Modal> : null}
  </div>
}
