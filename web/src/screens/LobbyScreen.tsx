import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { SettingsForm } from '../components/SettingsForm'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import type { GameSettings } from '../protocol'
import { ROLE_LABELS } from '../roles'
import { useSession } from '../session/useSession'
import type { SocketStatus } from '../ws/socket'

const STATUS: Record<SocketStatus, { label: string; dot: string }> = {
  idle: { label: 'Sin conexión', dot: 'bg-parchment/30' },
  connecting: { label: 'Conectando…', dot: 'bg-amber-400' },
  reconnecting: { label: 'Reconectando…', dot: 'bg-amber-400' },
  open: { label: 'Conectado', dot: 'bg-emerald-400' },
  closed: { label: 'Desconectado', dot: 'bg-blood' },
}

const MIN_PLAYERS = 5
const MAX_PLAYERS = 16

function SettingsSummary({ settings }: { settings: GameSettings }) {
  const roles =
    settings.roles.length > 0
      ? settings.roles.map((role) => ROLE_LABELS[role]).join(', ')
      : 'Ninguno'

  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
      <dt className="text-parchment/50">Lobos</dt>
      <dd>{settings.wolves}</dd>
      <dt className="text-parchment/50">Roles</dt>
      <dd>{roles}</dd>
      <dt className="text-parchment/50">Vidas</dt>
      <dd>{settings.lives}</dd>
      <dt className="text-parchment/50">Presidente</dt>
      <dd>{settings.president ? 'Sí' : 'No'}</dd>
      <dt className="text-parchment/50">Tiempos</dt>
      <dd>
        {settings.debateSeconds}s debate · {settings.roleSeconds}s rol · {settings.voteSeconds}s votación
      </dd>
      <dt className="text-parchment/50">Detalle de votos</dt>
      <dd>{settings.showVoteDetail ? 'Sí' : 'No'}</dd>
    </dl>
  )
}

export function LobbyScreen() {
  const {
    roomCode,
    players,
    status,
    playerId,
    leave,
    kickPlayer,
    notice,
    settings,
    starting,
    updateSettings,
    startGame,
  } = useSession()
  const navigate = useNavigate()
  const [copied, setCopied] = useState(false)

  if (!roomCode) {
    return (
      <Card className="text-center">
        <h2 className="mb-2 text-xl font-semibold">No hay ninguna sala activa</h2>
        <p className="mb-6 text-sm text-parchment/60">
          {notice ?? 'Crea una sala o entra con un código para empezar.'}
        </p>
        <Link to="/">
          <Button>Volver al inicio</Button>
        </Link>
      </Card>
    )
  }

  if (starting) {
    return (
      <Card className="text-center">
        <h2 className="mb-2 text-2xl font-semibold">La partida va a empezar</h2>
        <p className="mb-4 text-sm text-parchment/60">
          Preparando el reparto de cartas para {players.length} jugadores…
        </p>
        <p className="font-mono text-lg tracking-[0.3em] text-parchment/70">{roomCode}</p>
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
  const soyHost = players.some((player) => player.id === playerId && player.host)
  const listo = conectados >= MIN_PLAYERS

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
                {soyHost && player.id !== playerId ? (
                  <Button
                    variant="ghost"
                    onClick={() => kickPlayer(player.id)}
                    aria-label={`Expulsar a ${player.nickname}`}
                  >
                    Expulsar
                  </Button>
                ) : null}
              </span>
            </li>
          ))}
        </ul>

        <p className="mt-4 text-xs text-parchment/50">
          {listo
            ? 'Hay jugadores suficientes para empezar.'
            : `Faltan ${MIN_PLAYERS - conectados} jugador(es) para poder empezar.`}
        </p>
      </Card>

      <Card>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">Configuración</h2>
          <span className="text-xs text-parchment/50">
            {soyHost ? 'Solo tú puedes cambiarla' : 'Solo el host puede cambiarla'}
          </span>
        </div>

        {settings ? (
          soyHost ? (
            <SettingsForm
              key={JSON.stringify(settings)}
              settings={settings}
              onSave={updateSettings}
            />
          ) : (
            <SettingsSummary settings={settings} />
          )
        ) : (
          <p className="text-sm text-parchment/50">Cargando configuración…</p>
        )}
      </Card>

      <Card>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">
              {listo ? 'Listo para empezar' : 'Esperando jugadores'}
            </h2>
            <p className="text-sm text-parchment/60">
              {conectados} de {MIN_PLAYERS} jugadores conectados.
            </p>
          </div>

          {soyHost ? (
            <Button onClick={startGame} disabled={!listo}>
              Empezar partida
            </Button>
          ) : (
            <span className="text-sm text-parchment/50">Esperando a que el host empiece…</span>
          )}
        </div>

        {soyHost ? (
          <p className="mt-3 text-xs text-parchment/40">
            Si sales siendo host, la sala se cierra para todos.
          </p>
        ) : null}
      </Card>
    </div>
  )
}
