import { createContext } from 'react'
import type { Player } from '../protocol'
import type { SocketStatus } from '../ws/socket'

export interface SessionValue {
  status: SocketStatus
  connectionId: string | null
  roomCode: string | null
  playerId: string | null
  players: Player[]
  error: string | null
  notice: string | null
  createRoom: (nickname: string) => void
  joinRoom: (roomCode: string, nickname: string) => void
  kickPlayer: (playerId: string) => void
  leave: () => void
  clearError: () => void
}

export const SessionContext = createContext<SessionValue | null>(null)
