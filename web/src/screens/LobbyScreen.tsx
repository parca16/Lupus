import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { useSession } from '../session/useSession'
import type { SocketStatus } from '../ws/socket'

const STATUS: Record<SocketStatus, { label: string; dot: string }> = {
  idle: { label: 'Sin conexión', dot: 'bg-parchment/30' },
  connecting: { label: 'Conectando…', dot: 'bg-amber-400' },
  open: { label: 'Conectado', dot: 'bg-emerald-400' },
  closed: { label: 'Desconectado', dot: 'bg-blood' },
}

const MIN_PLAYERS = 5
const MAX_PLAYERS = 16

export function LobbyScreen() {
  const { roomCode, players, status, playerId, leave } = useSession()
  const navigate = useNavigate()
  const [copied, setCopied] = useState(false)

  if (!roomCode) {
    return (
      <Card className="text-center">
        <h2 className="mb-2 text-xl font-semibold">No hay ninguna sala activa</h2>
        <p className="mb-6 text-sm text-parchment/60">
          Crea una sala o entra con un código para empezar.
        </p>
        <Link to="/">
          <Button>Volver al inicio</Button>
        </Link>
      </Card>
    )
  }

  async function copiarCodigo() {
    try {
      await navigator.clipboard.writeText(roomCode ?? '')
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      setCopied(false)
    }
  }

  function salir() {
    leave()
    navigate('/')
  }

  const estado = STATUS[status]
  const conectados = players.filter((player) => player.connected).length

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-parchment/50">Código de sala</p>
            <div className="mt-1 flex items-center gap-3">
              <span className="font-mono text-3xl font-semibold tracking-[0.3em]">{roomCode}</span>
              <Button variant="secondary" onClick={copiarCodigo}>
                {copied ? '¡Copiado!' : 'Copiar'}
              </Button>
            </div>
          </div>

          <span className="inline-flex items-center gap-2 rounded-full border border-night-700 px-3 py-1 text-xs text-parchment/70">
            <span className={`h-2 w-2 rounded-full ${estado.dot}`} aria-hidden="true" />
            {estado.label}
          </span>
        </div>
      </Card>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">
            Jugadores <span className="text-parchment/50">({conectados}/{MAX_PLAYERS})</span>
          </h2>
          <Button variant="ghost" onClick={salir}>
            Salir
          </Button>
        </div>

        <ul className="flex flex-col gap-2">
          {players.map((player) => (
            <li
              key={player.id}
              className={[
                'flex items-center justify-between rounded-lg border px-3 py-2 text-sm',
                player.connected
                  ? 'border-night-700 bg-night-800/60'
                  : 'border-night-700/40 bg-night-900/40 text-parchment/40',
              ].join(' ')}
            >
              <span className="flex items-center gap-2">
                <span
                  className={`h-2 w-2 rounded-full ${player.connected ? 'bg-emerald-400' : 'bg-parchment/30'}`}
                  aria-hidden="true"
                />
                <span className={player.connected ? '' : 'line-through'}>{player.nickname}</span>
                {player.id === playerId ? (
                  <span className="rounded bg-night-700 px-1.5 py-0.5 text-[10px] uppercase tracking-wide">
                    tú
                  </span>
                ) : null}
              </span>

              <span className="flex items-center gap-2">
                {player.host ? (
                  <span className="rounded bg-blood/20 px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-blood">
                    host
                  </span>
                ) : null}
                {!player.connected ? (
                  <span className="text-[10px] uppercase tracking-wide">desconectado</span>
                ) : null}
              </span>
            </li>
          ))}
        </ul>

        <p className="mt-4 text-xs text-parchment/50">
          {conectados < MIN_PLAYERS
            ? `Faltan ${MIN_PLAYERS - conectados} jugador(es) para poder empezar.`
            : 'Ya hay jugadores suficientes para empezar.'}
        </p>
      </Card>
    </div>
  )
}
