import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { Player } from '../protocol'
import { LupusSocket } from '../ws/socket'
import type { SocketStatus } from '../ws/socket'
import { SessionContext } from './context'
import type { SessionValue } from './context'

type PendingAction =
  | { type: 'create_room'; nickname: string }
  | { type: 'join_room'; nickname: string; roomCode: string }

const ERROR_MESSAGES: Record<string, string> = {
  invalid_nickname: 'El apodo debe tener entre 3 y 16 caracteres.',
  room_not_found: 'No existe ninguna sala con ese código.',
  room_full: 'La sala está completa (16 jugadores).',
  nickname_taken: 'Ese apodo ya está en uso en esa sala.',
  invalid_message: 'El servidor no entendió el mensaje.',
  unknown_type: 'Ese mensaje no lo entiende el servidor.',
  unsupported_version: 'Tu cliente y el servidor no hablan la misma versión. Recarga la página.',
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SocketStatus>('idle')
  const [connectionId, setConnectionId] = useState<string | null>(null)
  const [roomCode, setRoomCode] = useState<string | null>(null)
  const [playerId, setPlayerId] = useState<string | null>(null)
  const [players, setPlayers] = useState<Player[]>([])
  const [error, setError] = useState<string | null>(null)

  const socketRef = useRef<LupusSocket | null>(null)
  const pendingRef = useRef<PendingAction | null>(null)

  useEffect(() => () => socketRef.current?.close(), [])

  const start = useCallback((action: PendingAction) => {
    socketRef.current?.close()
    setRoomCode(null)
    setPlayerId(null)
    setPlayers([])
    setError(null)
    pendingRef.current = action

    const socket = new LupusSocket({
      onStatusChange: (next) => {
        setStatus(next)

        const pending = pendingRef.current
        if (next === 'open' && pending) {
          pendingRef.current = null
          if (pending.type === 'create_room') {
            socket.send('create_room', { nickname: pending.nickname })
          } else {
            socket.send('join_room', { roomCode: pending.roomCode, nickname: pending.nickname })
          }
        }
      },
      onMessage: (message) => {
        switch (message.type) {
          case 'welcome':
            setConnectionId(message.connectionId)
            break
          case 'room_state':
            setRoomCode(message.roomCode)
            setPlayerId(message.playerId)
            setPlayers(message.players)
            break
          case 'room_updated':
            setPlayers(message.players)
            break
          case 'error':
            setError(ERROR_MESSAGES[message.code] ?? message.message)
            break
          default:
            break
        }
      },
    })

    socketRef.current = socket
    socket.connect(action.nickname)
  }, [])

  const createRoom = useCallback(
    (nickname: string) => {
      start({ type: 'create_room', nickname })
    },
    [start],
  )

  const joinRoom = useCallback(
    (roomCode: string, nickname: string) => {
      start({ type: 'join_room', roomCode, nickname })
    },
    [start],
  )

  const leave = useCallback(() => {
    socketRef.current?.close()
    socketRef.current = null
    pendingRef.current = null
    setStatus('idle')
    setConnectionId(null)
    setRoomCode(null)
    setPlayerId(null)
    setPlayers([])
    setError(null)
  }, [])

  const clearError = useCallback(() => setError(null), [])

  const value = useMemo<SessionValue>(
    () => ({
      status,
      connectionId,
      roomCode,
      playerId,
      players,
      error,
      createRoom,
      joinRoom,
      leave,
      clearError,
    }),
    [status, connectionId, roomCode, playerId, players, error, createRoom, joinRoom, leave, clearError],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
