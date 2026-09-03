'use client'

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' | 'ghost' }

export default function Button({ variant = 'primary', className = '', ...props }: Props) {
  return <button {...props} className={`ui-button ui-button-${variant} ${className}`} />
}
