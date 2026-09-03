export default function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="ui-empty"><strong>{title}</strong><span>{text}</span></div>
}
