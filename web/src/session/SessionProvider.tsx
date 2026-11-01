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

interface SeatedSession {
  roomCode: string
  playerId: string
  nickname: string
}

const ERROR_MESSAGES: Record<string, string> = {
  invalid_nickname: 'El apodo debe tener entre 3 y 16 caracteres.',
  room_not_found: 'No existe ninguna sala con ese código.',
  room_full: 'La sala está completa (16 jugadores).',
  nickname_taken: 'Ese apodo ya está en uso en esa sala.',
  not_host: 'Solo el host puede expulsar jugadores.',
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
  const [notice, setNotice] = useState<string | null>(null)

  const socketRef = useRef<LupusSocket | null>(null)
  const pendingRef = useRef<PendingAction | null>(null)
  const seatedRef = useRef<SeatedSession | null>(null)
  const nicknameRef = useRef('')

  useEffect(() => () => socketRef.current?.close(), [])

  const limpiarSala = useCallback(() => {
    seatedRef.current = null
    setRoomCode(null)
    setPlayerId(null)
    setPlayers([])
  }, [])

  const start = useCallback(
    (action: PendingAction) => {
      socketRef.current?.close()
      limpiarSala()
      setError(null)
      setNotice(null)
      pendingRef.current = action
      nicknameRef.current = action.nickname

      const socket = new LupusSocket({
        onStatusChange: (next) => {
          setStatus(next)
          if (next !== 'open') {
            return
          }

          const pending = pendingRef.current
          if (pending) {
            pendingRef.current = null
            if (pending.type === 'create_room') {
              socket.send('create_room', { nickname: pending.nickname })
            } else {
              socket.send('join_room', { roomCode: pending.roomCode, nickname: pending.nickname })
            }
            return
          }

          // Tras una reconexion, intentamos recuperar el asiento con el mismo apodo (#33).
          const seated = seatedRef.current
          if (seated) {
            socket.send('join_room', { roomCode: seated.roomCode, nickname: seated.nickname })
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
              seatedRef.current = {
                roomCode: message.roomCode,
                playerId: message.playerId,
                nickname: nicknameRef.current,
              }
              break
            case 'room_updated':
              setPlayers(message.players)
              break
            case 'error':
              setError(ERROR_MESSAGES[message.code] ?? message.message)
              break
            case 'room_closed':
              limpiarSala()
              setNotice('La sala se ha cerrado porque el host se ha ido.')
              break
            case 'kicked':
              limpiarSala()
              setNotice('El host te ha expulsado de la sala.')
              break
            default:
              break
          }
        },
      })

      socketRef.current = socket
      socket.connect(action.nickname)
    },
    [limpiarSala],
  )

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

  const kickPlayer = useCallback((targetPlayerId: string) => {
    socketRef.current?.send('kick_player', { playerId: targetPlayerId })
  }, [])

  const leave = useCallback(() => {
    const socket = socketRef.current
    if (socket) {
      socket.send('leave_room', {})
      socket.close()
    }
    socketRef.current = null
    pendingRef.current = null
    limpiarSala()
    setStatus('idle')
    setConnectionId(null)
    setError(null)
    setNotice(null)
  }, [limpiarSala])

  const clearError = useCallback(() => setError(null), [])

  const value = useMemo<SessionValue>(
    () => ({
      status,
      connectionId,
      roomCode,
      playerId,
      players,
      error,
      notice,
      createRoom,
      joinRoom,
      kickPlayer,
      leave,
      clearError,
    }),
    [
      status,
      connectionId,
      roomCode,
      playerId,
      players,
      error,
      notice,
      createRoom,
      joinRoom,
      kickPlayer,
      leave,
      clearError,
    ],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
