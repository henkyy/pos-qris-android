'use client'

type Props = React.InputHTMLAttributes<HTMLInputElement> & { label?: string }
export default function Input({ label, ...props }: Props) {
  return <label className="ui-field">{label && <span>{label}</span>}<input {...props} /></label>
}
