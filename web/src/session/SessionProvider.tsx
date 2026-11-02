import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { GameSettings, Player } from '../protocol'
import { LupusSocket } from '../ws/socket'
import type { SocketStatus } from '../ws/socket'
import { SessionContext } from './context'
import type { SessionValue } from './context'
import { clearSession, loadSession, saveSession } from './storage'

type PendingAction =
  | { type: 'create_room'; nickname: string }
  | { type: 'join_room'; nickname: string; roomCode: string }
  | { type: 'resume_room'; roomCode: string; playerId: string; nickname: string }

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
  not_host: 'Solo el host puede hacer eso.',
  invalid_message: 'El servidor no entendió el mensaje.',
  unknown_type: 'Ese mensaje no lo entiende el servidor.',
  unsupported_version: 'Tu cliente y el servidor no hablan la misma versión. Recarga la página.',
}

const RESUME_ERRORS = new Set(['seat_not_found', 'room_not_found', 'already_connected'])

export function SessionProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SocketStatus>('idle')
  const [connectionId, setConnectionId] = useState<string | null>(null)
  const [roomCode, setRoomCode] = useState<string | null>(null)
  const [playerId, setPlayerId] = useState<string | null>(null)
  const [players, setPlayers] = useState<Player[]>([])
  const [settings, setSettings] = useState<GameSettings | null>(null)
  const [starting, setStarting] = useState(false)
  const [restoring, setRestoring] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const socketRef = useRef<LupusSocket | null>(null)
  const pendingRef = useRef<PendingAction | null>(null)
  const seatedRef = useRef<SeatedSession | null>(null)
  const nicknameRef = useRef('')
  const resumingRef = useRef(false)

  const limpiarSala = useCallback(() => {
    seatedRef.current = null
    resumingRef.current = false
    setRoomCode(null)
    setPlayerId(null)
    setPlayers([])
    setSettings(null)
    setStarting(false)
    setRestoring(false)
  }, [])

  const start = useCallback(
    (action: PendingAction) => {
      socketRef.current?.close()
      limpiarSala()
      setError(null)
      setNotice(null)
      pendingRef.current = action
      nicknameRef.current = action.nickname

      if (action.type === 'resume_room') {
        resumingRef.current = true
        setRestoring(true)
        seatedRef.current = {
          roomCode: action.roomCode,
          playerId: action.playerId,
          nickname: action.nickname,
        }
      }

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
            } else if (pending.type === 'join_room') {
              socket.send('join_room', { roomCode: pending.roomCode, nickname: pending.nickname })
            } else {
              socket.send('resume_room', { roomCode: pending.roomCode, playerId: pending.playerId })
            }
            return
          }

          // Tras una reconexion, recuperamos el asiento con la identidad guardada (#26).
          const seated = seatedRef.current
          if (seated) {
            resumingRef.current = true
            setRestoring(true)
            socket.send('resume_room', { roomCode: seated.roomCode, playerId: seated.playerId })
          }
        },
        onMessage: (message) => {
          switch (message.type) {
            case 'welcome':
              setConnectionId(message.connectionId)
              break
            case 'room_state': {
              const seated: SeatedSession = {
                roomCode: message.roomCode,
                playerId: message.playerId,
                nickname: nicknameRef.current,
              }
              setRoomCode(message.roomCode)
              setPlayerId(message.playerId)
              setPlayers(message.players)
              seatedRef.current = seated
              saveSession(seated)
              resumingRef.current = false
              setRestoring(false)
              setNotice(null)
              break
            }
            case 'room_updated':
              setPlayers(message.players)
              break
            case 'room_settings':
              setSettings({
                wolves: message.wolves,
                roles: message.roles,
                lives: message.lives,
                president: message.president,
                debateSeconds: message.debateSeconds,
                roleSeconds: message.roleSeconds,
                voteSeconds: message.voteSeconds,
                showVoteDetail: message.showVoteDetail,
              })
              break
            case 'game_starting':
              setStarting(true)
              break
            case 'error': {
              const recuperando = resumingRef.current

              if (recuperando && RESUME_ERRORS.has(message.code)) {
                clearSession()
                limpiarSala()
                setNotice(
                  message.code === 'already_connected'
                    ? 'Esta sala ya está abierta en otra pestaña.'
                    : 'Tu sala ya no está disponible.',
                )
                break
              }

              setError(ERROR_MESSAGES[message.code] ?? message.message)
              break
            }
            case 'room_closed':
              clearSession()
              limpiarSala()
              setNotice('La sala se ha cerrado porque el host se ha ido.')
              break
            case 'kicked':
              clearSession()
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

  // Al cargar la pagina, si hay una sesion guardada, intentamos recuperar el asiento.
  const autoResumeRef = useRef(false)
  useEffect(() => {
    if (autoResumeRef.current) {
      return
    }
    autoResumeRef.current = true

    const stored = loadSession()
    if (stored) {
      // oxlint-disable-next-line react/set-state-in-effect -- sincroniza React con localStorage al montar
      start({ type: 'resume_room', ...stored })
    }
  }, [start])

  useEffect(() => () => socketRef.current?.close(), [])

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

  const updateSettings = useCallback((next: GameSettings) => {
    socketRef.current?.send('update_settings', next)
  }, [])

  const startGame = useCallback(() => {
    socketRef.current?.send('start_game', {})
  }, [])

  const leave = useCallback(() => {
    const socket = socketRef.current
    if (socket) {
      socket.send('leave_room', {})
      socket.close()
    }
    socketRef.current = null
    pendingRef.current = null
    clearSession()
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
      settings,
      starting,
      restoring,
      error,
      notice,
      createRoom,
      joinRoom,
      kickPlayer,
      updateSettings,
      startGame,
      leave,
      clearError,
    }),
    [
      status,
      connectionId,
      roomCode,
      playerId,
      players,
      settings,
      starting,
      restoring,
      error,
      notice,
      createRoom,
      joinRoom,
      kickPlayer,
      updateSettings,
      startGame,
      leave,
      clearError,
    ],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
