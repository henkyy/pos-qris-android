'use client'

export default function Modal({ open, title, children, onClose }: { open: boolean; title: string; children: React.ReactNode; onClose: () => void }) {
  if (!open) return null
  return <div className="ui-modal-backdrop" onClick={onClose}><section className="ui-modal" onClick={e => e.stopPropagation()}><header><strong>{title}</strong><button onClick={onClose} aria-label="Tutup">×</button></header><div>{children}</div></section></div>
}
