import type { ReactNode } from 'react'

export interface CardProps {
  children: ReactNode
  className?: string
}

export function Card({ children, className = '' }: CardProps) {
  return (
    <section
      className={[
        'rounded-2xl border border-night-700/80 bg-night-900/80 p-6 shadow-xl shadow-black/30',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {children}
    </section>
  )
}
