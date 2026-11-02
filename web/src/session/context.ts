import { createContext } from 'react'
import type {
  GameDawnMessage,
  GameOverMessage,
  GameSettings,
  GameStateMessage,
  Player,
} from '../protocol'
import type { SocketStatus } from '../ws/socket'

export interface GameActionOptions {
  kind?: string
  targetId?: string
  secondTargetId?: string
}

export interface SessionValue {
  status: SocketStatus
  connectionId: string | null
  roomCode: string | null
  playerId: string | null
  players: Player[]
  settings: GameSettings | null
  game: GameStateMessage | null
  dawn: GameDawnMessage | null
  gameOver: GameOverMessage | null
  starting: boolean
  restoring: boolean
  error: string | null
  notice: string | null
  createRoom: (nickname: string) => void
  joinRoom: (roomCode: string, nickname: string) => void
  kickPlayer: (playerId: string) => void
  updateSettings: (settings: GameSettings) => void
  startGame: () => void
  gameAction: (action: string, options?: GameActionOptions) => void
  leave: () => void
  clearError: () => void
}

export const SessionContext = createContext<SessionValue | null>(null)
