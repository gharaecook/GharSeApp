import { forwardRef, useEffect, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'
import { ApiError } from '../lib/api'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'green'
const variants: Record<Variant, string> = {
  primary: 'bg-brand-500 text-white hover:bg-brand-600 shadow-sm',
  secondary: 'bg-white text-brand-700 border border-brand-200 hover:bg-brand-50',
  ghost: 'text-ink-700 hover:bg-cream-200',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  green: 'bg-leaf-600 text-white hover:bg-leaf-700',
}

export function Button({ variant = 'primary', loading, className = '', children, disabled, ...rest }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
    >
      {loading && <Spinner small />}
      {children}
    </button>
  )
}

export function Spinner({ small }: { small?: boolean }) {
  return <span role="status" aria-label="Loading" className={`inline-block animate-spin rounded-full border-2 border-current border-t-transparent ${small ? 'h-4 w-4' : 'h-8 w-8 text-brand-500'}`} />
}

interface FieldProps { label?: string; error?: string; hint?: string }

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & FieldProps>(
  ({ label, error, hint, className = '', ...rest }, ref) => (
    <div className={className}>
      {label && <label className="label" htmlFor={rest.id ?? rest.name}>{label}</label>}
      <input ref={ref} id={rest.id ?? rest.name} {...rest} className="input" aria-invalid={!!error} />
      {hint && !error && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
      {error && <p className="mt-1 text-xs font-semibold text-red-600">{error}</p>}
    </div>
  ),
)
Input.displayName = 'Input'

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement> & FieldProps>(
  ({ label, error, hint, className = '', ...rest }, ref) => (
    <div className={className}>
      {label && <label className="label" htmlFor={rest.id ?? rest.name}>{label}</label>}
      <textarea ref={ref} id={rest.id ?? rest.name} rows={4} {...rest} className="input" aria-invalid={!!error} />
      {hint && !error && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
      {error && <p className="mt-1 text-xs font-semibold text-red-600">{error}</p>}
    </div>
  ),
)
Textarea.displayName = 'Textarea'

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement> & FieldProps>(
  ({ label, error, className = '', children, ...rest }, ref) => (
    <div className={className}>
      {label && <label className="label" htmlFor={rest.id ?? rest.name}>{label}</label>}
      <select ref={ref} id={rest.id ?? rest.name} {...rest} className="input" aria-invalid={!!error}>{children}</select>
      {error && <p className="mt-1 text-xs font-semibold text-red-600">{error}</p>}
    </div>
  ),
)
Select.displayName = 'Select'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`card ${className}`}>{children}</div>
}

const badgeTones = {
  neutral: 'bg-cream-200 text-ink-700',
  brand: 'bg-brand-100 text-brand-800',
  green: 'bg-leaf-100 text-leaf-700',
  red: 'bg-red-100 text-red-700',
  amber: 'bg-amber-100 text-amber-800',
}
export function Badge({ tone = 'neutral', children }: { tone?: keyof typeof badgeTones; children: ReactNode }) {
  return <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-bold ${badgeTones[tone]}`}>{children}</span>
}

export function Rating({ value, count, size = 'sm' }: { value: number; count?: number; size?: 'sm' | 'lg' }) {
  if (!count && !value) return <span className="text-xs font-semibold text-ink-500">New</span>
  return (
    <span className={`inline-flex items-center gap-1 font-bold ${size === 'lg' ? 'text-lg' : 'text-sm'}`} aria-label={`Rated ${Number(value).toFixed(1)} out of 5`}>
      <span className="text-amber-500">★</span>
      {Number(value).toFixed(1)}
      {count !== undefined && <span className="font-semibold text-ink-500">({count})</span>}
    </span>
  )
}

export function StarInput({ value, onChange, label }: { value: number; onChange: (v: number) => void; label: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-sm font-semibold text-ink-700">{label}</span>
      <div className="flex" role="radiogroup" aria-label={label}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} type="button" role="radio" aria-checked={value === n} aria-label={`${n} star${n > 1 ? 's' : ''}`}
            onClick={() => onChange(n)} className={`px-0.5 text-2xl ${n <= value ? 'text-amber-500' : 'text-cream-300'}`}>★</button>
        ))}
      </div>
    </div>
  )
}

const palette = ['from-brand-300 to-brand-500', 'from-leaf-500 to-leaf-700', 'from-amber-300 to-brand-400', 'from-rose-300 to-brand-500', 'from-orange-300 to-amber-500']
export function Avatar({ name, src, size = 56 }: { name: string; src?: string; size?: number }) {
  const initials = name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()
  const tone = palette[[...name].reduce((a, c) => a + c.charCodeAt(0), 0) % palette.length]
  if (src) return <img src={src} alt={name} width={size} height={size} className="rounded-full object-cover" style={{ width: size, height: size }} />
  return (
    <div aria-label={name} role="img" style={{ width: size, height: size, fontSize: size * 0.38 }}
      className={`flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br font-extrabold text-white ${tone}`}>
      {initials}
    </div>
  )
}

export function Modal({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink-900/50 p-4 sm:items-center" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-label={title} className="card w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <h2 className="mb-3 text-xl">{title}</h2>
        {children}
      </div>
    </div>
  )
}

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return <div className="flex flex-col items-center gap-3 py-16 text-ink-500"><Spinner /><p className="text-sm font-semibold">{label}</p></div>
}

export function EmptyState({ emoji = '🍲', title, message, action }: { emoji?: string; title: string; message?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2 py-14 text-center">
      <div className="text-5xl" aria-hidden>{emoji}</div>
      <h3 className="text-lg">{title}</h3>
      {message && <p className="max-w-sm text-sm text-ink-500">{message}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
  return (
    <div role="alert" className="flex flex-col items-center gap-2 py-14 text-center">
      <div className="text-5xl" aria-hidden>🫕</div>
      <h3 className="text-lg">We couldn't load that</h3>
      <p className="max-w-sm text-sm text-ink-500">{message}</p>
      {onRetry && <Button variant="secondary" onClick={onRetry}>Try again</Button>}
    </div>
  )
}

export function FormError({ error }: { error: unknown }) {
  if (!error) return null
  const msg = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
  const fields = error instanceof ApiError ? error.fieldErrors : []
  return (
    <div role="alert" className="rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-700">
      {msg}
      {fields.length > 0 && <ul className="mt-1 list-disc pl-5 font-normal">{fields.map((f) => <li key={f.field}>{f.field}: {f.message}</li>)}</ul>}
    </div>
  )
}

export function PageTitle({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl sm:text-3xl">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-ink-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}
