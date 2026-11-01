import { useContext } from 'react'
import { SessionContext } from './context'
import type { SessionValue } from './context'

export function useSession(): SessionValue {
  const value = useContext(SessionContext)
  if (!value) {
    throw new Error('useSession debe usarse dentro de SessionProvider')
  }
  return value
}
