'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../../components/ui/Button'
import EmptyState from '../../components/ui/EmptyState'
import Input from '../../components/ui/Input'
import Modal from '../../components/ui/Modal'
import Table from '../../components/ui/Table'
import { getActiveWorkspace } from '../../lib/business-context'
import { requireSupabase } from '../../lib/supabase'

type Field = { key: string; label: string; required?: boolean; type?: 'text' | 'number' }
export type EntityConfig = { title: string; eyebrow: string; description: string; table: string; columns: string[]; fields?: Field[]; businessScoped?: boolean; readOnly?: boolean }

export default function EntityPage({ config }: { config: EntityConfig }) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Record<string, unknown> | null>(null)
  const [form, setForm] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

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
      setRows((data || []) as Record<string, unknown>[])
    } catch (e: any) {
      setError(e?.message || 'Gagal memuat data.')
    } finally {
      setLoading(false)
    }
  }, [config])

  useEffect(() => { load() }, [load])
  const displayColumns = useMemo(() => config.columns, [config.columns])
  function startCreate() { setEditing(null); setForm({}); setOpen(true) }
  function startEdit(row: Record<string, unknown>) { setEditing(row); setForm(Object.fromEntries((config.fields || []).map(f => [f.key, String(row[f.key] ?? '')]))); setOpen(true) }

  async function save() {
    if (!config.fields?.length) return
    for (const field of config.fields) if (field.required && !form[field.key]?.trim()) return setError(`${field.label} wajib diisi.`)
    setSaving(true)
    setError('')
    try {
      const db = requireSupabase()
      const payload: Record<string, unknown> = {}
      for (const field of config.fields) payload[field.key] = field.type === 'number' ? Number(form[field.key] || 0) : form[field.key] || null
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

  return <div className="module-page"><div className="module-hero"><div><span className="eyebrow">{config.eyebrow}</span><h1>{config.title}</h1><p>{config.description}</p></div>{!config.readOnly && config.fields?.length ? <Button onClick={startCreate}>+ Tambah</Button> : null}</div>{error && <div className="module-alert">{error}</div>}<section className="module-card"><div className="module-card-head"><div><strong>Daftar {config.title}</strong><span>{loading ? 'Memuat data…' : `${rows.length} data`}</span></div><Button variant="secondary" onClick={load} disabled={loading}>Muat ulang</Button></div>{loading ? <EmptyState title="Memuat data" text="Mengambil data dari Supabase." /> : rows.length ? <Table columns={displayColumns} rows={rows} onEdit={!config.readOnly && config.fields?.length ? startEdit : undefined} /> : <EmptyState title="Belum ada data" text="Belum ada record yang tersedia untuk modul ini." />}</section>{config.fields?.length ? <Modal open={open} title={editing ? `Edit ${config.title}` : `Tambah ${config.title}`} onClose={() => setOpen(false)}><div className="module-form">{config.fields.map(field => <Input key={field.key} label={field.label} type={field.type === 'number' ? 'number' : 'text'} value={form[field.key] || ''} onChange={e => setForm(prev => ({ ...prev, [field.key]: e.target.value }))} />)}<Button onClick={save} disabled={saving}>{saving ? 'Menyimpan…' : 'Simpan'}</Button></div></Modal> : null}</div>
}
