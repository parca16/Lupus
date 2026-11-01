import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Input } from '../components/ui/Input'
import { useSession } from '../session/useSession'

type Mode = 'create' | 'join'

const NICKNAME_MIN = 3
const NICKNAME_MAX = 16
const CODE_LENGTH = 6

export function HomeScreen() {
  const { createRoom, joinRoom, status, error, roomCode, clearError } = useSession()
  const navigate = useNavigate()

  const [mode, setMode] = useState<Mode>('create')
  const [nickname, setNickname] = useState('')
  const [code, setCode] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const busy = status === 'connecting'

  useEffect(() => {
    if (roomCode) {
      navigate('/lobby')
    }
  }, [roomCode, navigate])

  function cambiarModo(next: Mode) {
    setMode(next)
    setLocalError(null)
    clearError()
  }

  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearError()

    const apodo = nickname.trim()
    if (apodo.length < NICKNAME_MIN || apodo.length > NICKNAME_MAX) {
      setLocalError(`El apodo debe tener entre ${NICKNAME_MIN} y ${NICKNAME_MAX} caracteres.`)
      return
    }

    if (mode === 'join') {
      const limpio = code.trim().toUpperCase()
      if (!/^[A-Z0-9]{6}$/.test(limpio)) {
        setLocalError('El código de sala tiene 6 caracteres.')
        return
      }
      setLocalError(null)
      joinRoom(limpio, apodo)
      return
    }

    setLocalError(null)
    createRoom(apodo)
  }

  const mensajeError = localError ?? error

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2 text-center">
        <h2 className="text-3xl font-semibold tracking-tight">Entra en la partida</h2>
        <p className="text-sm text-parchment/60">
          Elige un apodo, crea una sala privada y comparte el código con tus amigos.
        </p>
      </div>

      <Card>
        <div className="mb-6 grid grid-cols-2 gap-2 rounded-xl bg-night-950/60 p-1">
          <button
            type="button"
            onClick={() => cambiarModo('create')}
            className={[
              'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
              mode === 'create' ? 'bg-night-700 text-parchment' : 'text-parchment/60 hover:text-parchment',
            ].join(' ')}
            aria-pressed={mode === 'create'}
          >
            Crear sala
          </button>
          <button
            type="button"
            onClick={() => cambiarModo('join')}
            className={[
              'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
              mode === 'join' ? 'bg-night-700 text-parchment' : 'text-parchment/60 hover:text-parchment',
            ].join(' ')}
            aria-pressed={mode === 'join'}
          >
            Unirse
          </button>
        </div>

        <form className="flex flex-col gap-4" onSubmit={enviar}>
          <Input
            label="Tu apodo"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            maxLength={NICKNAME_MAX}
            placeholder="Cómo te verán los demás"
            autoComplete="nickname"
            autoFocus
          />

          {mode === 'join' ? (
            <Input
              label="Código de sala"
              value={code}
              onChange={(event) => setCode(event.target.value.toUpperCase())}
              maxLength={CODE_LENGTH}
              placeholder="ABC234"
              autoComplete="off"
              spellCheck={false}
              className="font-mono tracking-[0.3em] uppercase"
              hint="Te lo habrá pasado quien creó la sala."
            />
          ) : null}

          {mensajeError ? (
            <p role="alert" className="rounded-lg border border-blood/50 bg-blood/10 px-3 py-2 text-sm">
              {mensajeError}
            </p>
          ) : null}

          <Button type="submit" fullWidth disabled={busy}>
            {busy ? 'Conectando…' : mode === 'create' ? 'Crear sala y entrar' : 'Entrar en la sala'}
          </Button>
        </form>

        <p className="mt-4 text-center text-xs text-parchment/40">
          Entre 5 y 16 jugadores por sala. El servidor reparte los roles.
        </p>
      </Card>
    </div>
  )
}
