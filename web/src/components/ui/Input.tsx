import type { InputHTMLAttributes } from 'react'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  hint?: string
}

export function Input({ label, hint, id, className = '', ...props }: InputProps) {
  const inputId = id ?? `input-${label.toLowerCase().replace(/\s+/g, '-')}`

  return (
    <div className="flex flex-col gap-1.5 text-sm">
      <label className="font-medium text-parchment/80" htmlFor={inputId}>
        {label}
      </label>
      <input
        id={inputId}
        className={[
          'rounded-lg border border-night-700 bg-night-900 px-3 py-2 text-parchment',
          'placeholder:text-parchment/30',
          'focus:border-blood focus:outline-none focus:ring-2 focus:ring-blood/30',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
        {...props}
      />
      {hint ? <span className="text-xs text-parchment/50">{hint}</span> : null}
    </div>
  )
}
