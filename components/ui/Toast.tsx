export default function Toast({ message, tone = 'info' }: { message: string; tone?: 'info' | 'success' | 'error' }) {
  return <div className={`ui-toast ui-toast-${tone}`}>{message}</div>
}
