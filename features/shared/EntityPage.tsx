'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import Input from '../../components/ui/Input'
import Modal from '../../components/ui/Modal'
import Table from '../../components/ui/Table'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'
import styles from './EntityPage.module.css'

type Field = { key: string; label: string; required?: boolean; type?: 'text' | 'number' }
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
}

const currencyKeys = new Set(['credit_limit', 'total_amount', 'amount', 'original_amount', 'paid_amount', 'outstanding_amount'])
const foreignLabels: Record<string, string> = { product_id: 'products', location_id: 'locations', customer_id: 'customers', supplier_id: 'suppliers', category_id: 'categories', payment_method_id: 'payment_methods', unit_id: 'units' }

function formatNumber(value: unknown) { const n = Number(value); return Number.isFinite(n) ? new Intl.NumberFormat('id-ID').format(n) : String(value ?? '-') }
function formatCell(key: string, value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (key === 'is_active') return value ? 'Aktif' : 'Nonaktif'
  if (currencyKeys.has(key) || key === 'qty_base') return formatNumber(value)
  if (key === 'status') return String(value).replace(/_/g, ' ')
  if (key.endsWith('_at') || key.endsWith('_date')) { const date = new Date(String(value)); if (!Number.isNaN(date.getTime())) return date.toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' }) }
  return String(value)
}
function cellTone(key: string, value: unknown) {
  if (key === 'is_active') return value ? styles.success : styles.muted
  if (key === 'status') { const status = String(value).toLowerCase(); if (['completed', 'paid', 'active', 'confirmed', 'received'].includes(status)) return styles.success; if (['pending', 'draft', 'open', 'partial'].includes(status)) return styles.warning; if (['cancelled', 'failed', 'overdue'].includes(status)) return styles.danger }
  return ''
}

export default function EntityPage({ config }: { config: EntityConfig }) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([])
  const [labels, setLabels] = useState<Record<string, Record<string, string>>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Record<string, unknown> | null>(null)
  const [form, setForm] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [search, setSearch] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const db = requireSupabase(); const workspace = config.businessScoped ? await getActiveWorkspace() : null
      let query = db.from(config.table).select('*').limit(200); if (workspace) query = query.eq('business_id', workspace.business.id)
      const { data, error: qe } = await query; if (qe) throw qe
      const nextRows = (data || []) as Record<string, unknown>[]; const nextLabels: Record<string, Record<string, string>> = {}
      await Promise.all(config.columns.filter(key => foreignLabels[key]).map(async key => {
        const ids = [...new Set(nextRows.map(row => row[key]).filter(Boolean).map(String))]; if (!ids.length) return
        const { data: lookup } = await db.from(foreignLabels[key]).select('id,name').in('id', ids)
        nextLabels[key] = Object.fromEntries((lookup || []).map((item: any) => [item.id, item.name]))
      }))
      setRows(nextRows); setLabels(nextLabels)
    } catch (e: any) { setError(e?.message || 'Gagal memuat data.') } finally { setLoading(false) }
  }, [config])

  useEffect(() => { load() }, [load])
  const displayRows = useMemo(() => rows.map(row => { const copy = { ...row }; for (const key of config.columns) if (foreignLabels[key] && labels[key]?.[String(row[key])]) copy[key] = labels[key][String(row[key])]; return copy }), [rows, labels, config.columns])
  const filteredRows = useMemo(() => { const needle = search.trim().toLowerCase(); return needle ? displayRows.filter(row => config.columns.some(key => String(row[key] ?? '').toLowerCase().includes(needle))) : displayRows }, [displayRows, search, config.columns])
  const activeCount = rows.filter(row => row.is_active !== false).length
  const attentionCount = rows.filter(row => ['overdue', 'pending', 'partial', 'draft'].includes(String(row.status).toLowerCase())).length

  function startCreate() { setEditing(null); setForm({}); setOpen(true) }
  function startEdit(row: Record<string, unknown>) { const original = rows.find(item => item.id === row.id) || row; setEditing(original); setForm(Object.fromEntries((config.fields || []).map(f => [f.key, String(original[f.key] ?? '')]))); setOpen(true) }
  async function save() {
    if (!config.fields?.length) return
    for (const field of config.fields) if (field.required && !form[field.key]?.trim()) return setError(`${field.label} wajib diisi.`)
    setSaving(true); setError('')
    try {
      const db = requireSupabase(); const payload: Record<string, unknown> = {}
      for (const field of config.fields) payload[field.key] = field.type === 'number' ? Number(form[field.key] || 0) : form[field.key] || null
      if (config.businessScoped && !editing) { const workspace = await getActiveWorkspace(); payload.business_id = workspace.business.id }
      const result = editing?.id ? await db.from(config.table).update(payload).eq('id', editing.id) : await db.from(config.table).insert(payload)
      if (result.error) throw result.error
      setOpen(false); await load()
    } catch (e: any) { setError(e?.message || 'Gagal menyimpan data.') } finally { setSaving(false) }
  }

  return <div className="module-page">
    <div className="module-hero"><div><span className="eyebrow">{config.eyebrow}</span><h1>{config.title}</h1><p>{config.description}</p></div>{!config.readOnly && config.fields?.length ? <Button onClick={startCreate}>+ Tambah {config.title}</Button> : null}</div>
    {error && <div className="module-alert">{error}</div>}
    <div className={styles.metrics}><div className={styles.metric}><span>Total data</span><strong>{loading ? '…' : rows.length}</strong><small>Record tersedia</small></div><div className={styles.metric}><span>Aktif</span><strong>{loading ? '…' : activeCount}</strong><small>Siap digunakan</small></div><div className={styles.metric}><span>Perlu perhatian</span><strong>{loading ? '…' : attentionCount}</strong><small>Status perlu ditindaklanjuti</small></div></div>
    <section className="module-card"><div className={styles.toolbar}><div><strong>Daftar {config.title}</strong><span>{loading ? 'Memuat data…' : `${filteredRows.length} dari ${rows.length} data`}</span></div><div className={styles.actions}><label className={styles.search}><span>⌕</span><input value={search} onChange={e => setSearch(e.target.value)} placeholder={`Cari ${config.title.toLowerCase()}…`} /></label><Button variant="secondary" onClick={load} disabled={loading}>Muat ulang</Button></div></div>{loading ? <EmptyState title="Memuat data" text="Mengambil data dari Supabase." /> : filteredRows.length ? <div className="ui-table-wrap"><table className="ui-table"><thead><tr>{config.columns.map(c => <th key={c}>{config.columnLabels?.[c] || c.replace(/_/g, ' ')}</th>)}{!config.readOnly && config.fields?.length ? <th>Aksi</th> : null}</tr></thead><tbody>{filteredRows.map((row, i) => <tr key={String(row.id ?? i)}>{config.columns.map(c => <td key={c}><span className={cellTone(c, row[c])}>{formatCell(c, row[c])}</span></td>)}{!config.readOnly && config.fields?.length ? <td><button className="table-edit" onClick={() => startEdit(row)}>Edit</button></td> : null}</tr>)}</tbody></table></div> : <EmptyState title={search ? 'Data tidak ditemukan' : 'Belum ada data'} text={search ? 'Coba kata kunci lain.' : 'Belum ada record yang tersedia untuk modul ini.'} />}</section>
    {config.fields?.length ? <Modal open={open} title={editing ? `Edit ${config.title}` : `Tambah ${config.title}`} onClose={() => setOpen(false)}><div className="module-form">{config.fields.map(field => <Input key={field.key} label={field.label} type={field.type === 'number' ? 'number' : 'text'} value={form[field.key] || ''} onChange={e => setForm(prev => ({ ...prev, [field.key]: e.target.value }))} />)}<Button onClick={save} disabled={saving}>{saving ? 'Menyimpan…' : 'Simpan'}</Button></div></Modal> : null}
  </div>
}
