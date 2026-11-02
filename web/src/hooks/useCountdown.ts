import { useEffect, useState } from 'react'

function secondsLeft(deadline: number): number {
  if (!deadline) {
    return 0
  }
  return Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
}

/** Segundos restantes hasta un plazo absoluto (epoch ms). */
export function useCountdown(deadline: number): number {
  const [remaining, setRemaining] = useState(() => secondsLeft(deadline))

  useEffect(() => {
    const id = window.setInterval(() => setRemaining(secondsLeft(deadline)), 500)
    return () => window.clearInterval(id)
  }, [deadline])

  return remaining
}

export function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}
