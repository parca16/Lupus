import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import {
  GAME_ROLE_DESCRIPTIONS,
  GAME_ROLE_LABELS,
  NIGHT_STEP_LABELS,
  PHASE_DESCRIPTIONS,
  PHASE_LABELS,
  STEP_ROLE,
} from '../game/labels'
import { formatCountdown, useCountdown } from '../hooks/useCountdown'
import { useSession } from '../session/useSession'
import type { SocketStatus } from '../ws/socket'

const STATUS: Record<SocketStatus, { label: string; dot: string }> = {
  idle: { label: 'Sin conexión', dot: 'bg-parchment/30' },
  connecting: { label: 'Conectando…', dot: 'bg-amber-400' },
  reconnecting: { label: 'Reconectando…', dot: 'bg-amber-400' },
  open: { label: 'Conectado', dot: 'bg-emerald-400' },
  closed: { label: 'Desconectado', dot: 'bg-blood' },
}

export function GameScreen() {
  const { game, playerId, status, error, gameAction } = useSession()
  const [revealedKey, setRevealedKey] = useState<string | null>(null)
  const [presidentVote, setPresidentVote] = useState<{ key: string; targetId: string } | null>(null)
  const remaining = useCountdown(game?.deadline ?? 0)

  if (!game) {
    return (
      <Card className="text-center">
        <h2 className="mb-2 text-xl font-semibold">No hay ninguna partida en curso</h2>
        <p className="mb-6 text-sm text-parchment/60">
          Vuelve al inicio para crear una sala o unirte a una partida.
        </p>
        <Link to="/">
          <Button>Volver al inicio</Button>
        </Link>
      </Card>
    )
  }

  const phaseKey = `${game.phase}-${game.round}`
  const revelada = revealedKey === phaseKey
  const yo = game.statuses.find((player) => player.id === playerId)
  const vivos = game.statuses.filter((player) => player.alive && player.id !== playerId)
  const estado = STATUS[status]
  const enRevelacion = game.phase === 'DEALING' || game.phase === 'REVEAL'
  const rolDelPaso = game.nightStep ? STEP_ROLE[game.nightStep] : undefined
  const esMiTurno = rolDelPaso !== undefined && rolDelPaso === game.role
  const yaListo = playerId != null && game.ready.includes(playerId)
  const fase = PHASE_LABELS[game.phase] ?? game.phase

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex flex-col gap-1">
            <p className="text-xs uppercase tracking-widest text-parchment/50">
              Sala {game.roomCode} · Ronda {game.round}
            </p>
            <h2 className="text-2xl font-semibold">{fase}</h2>
            <p className="text-sm text-parchment/60">{PHASE_DESCRIPTIONS[game.phase]}</p>
            {game.phase === 'NIGHT' && game.nightStep ? (
              <p className="text-sm text-parchment/70">
                Turno de <strong>{NIGHT_STEP_LABELS[game.nightStep] ?? game.nightStep}</strong>
                {esMiTurno ? ' · es tu turno' : ''}
              </p>
            ) : null}
          </div>

          <div className="flex flex-col items-end gap-2">
            <span className="inline-flex items-center gap-2 rounded-full border border-night-700 px-3 py-1 text-xs text-parchment/70">
              <span className={`h-2 w-2 rounded-full ${estado.dot}`} aria-hidden="true" />
              {estado.label}
            </span>
            {game.deadline > 0 ? (
              <span className="font-mono text-2xl font-semibold text-parchment">
                {formatCountdown(remaining)}
              </span>
            ) : null}
          </div>
        </div>
      </Card>

      {error ? (
        <p role="alert" className="rounded-lg border border-blood/50 bg-blood/10 px-3 py-2 text-sm">
          {error}
        </p>
      ) : null}

      {yo && !yo.alive ? (
        <Card className="border-blood/40 text-center">
          <p className="text-sm text-parchment/80">
            Estás muerto. Puedes seguir la partida como espectador.
          </p>
        </Card>
      ) : null}

      {enRevelacion ? (
        <Card className="flex flex-col items-center gap-4 text-center">
          <h3 className="text-lg font-semibold">Tu carta</h3>

          <button
            type="button"
            onClick={() => setRevealedKey(phaseKey)}
            aria-label={revelada ? 'Tu carta revelada' : 'Revelar tu carta'}
            className={[
              'flex h-56 w-44 flex-col items-center justify-center gap-3 rounded-2xl border px-4',
              'transition-all duration-500',
              revelada
                ? 'border-blood/60 bg-night-800 shadow-2xl shadow-blood/20'
                : 'border-night-700 bg-night-900 hover:border-parchment/40',
            ].join(' ')}
          >
            {revelada ? (
              <>
                <span className="text-3xl">🐺</span>
                <span className="text-lg font-semibold">
                  {GAME_ROLE_LABELS[game.role] ?? game.role}
                </span>
                <span className="text-xs text-parchment/60">
                  {GAME_ROLE_DESCRIPTIONS[game.role] ?? ''}
                </span>
              </>
            ) : (
              <>
                <span className="text-3xl">🂠</span>
                <span className="text-sm font-medium">Pulsa para revelar</span>
                <span className="text-xs text-parchment/50">Solo tú ves esta carta</span>
              </>
            )}
          </button>

          {game.loverId ? (
            <p className="text-sm text-parchment/70">
              Estás enamorado de{' '}
              <strong>
                {game.statuses.find((player) => player.id === game.loverId)?.nickname ?? 'alguien'}
              </strong>
              .
            </p>
          ) : null}
        </Card>
      ) : null}

      {game.phase === 'PRESIDENT_ELECTION' ? (
        <Card>
          <h3 className="mb-1 text-lg font-semibold">Elige al Presidente</h3>
          <p className="mb-4 text-sm text-parchment/60">
            Su voto valdrá doble en el linchamiento. No puedes votarte a ti mismo.
          </p>

          <div className="flex flex-wrap gap-2">
            {vivos.map((player) => (
              <Button
                key={player.id}
                variant={presidentVote?.targetId === player.id ? 'primary' : 'secondary'}
                onClick={() => {
                  setPresidentVote({ key: phaseKey, targetId: player.id })
                  gameAction('president_vote', { targetId: player.id })
                }}
                disabled={presidentVote?.key === phaseKey}
              >
                {player.nickname}
              </Button>
            ))}
            <Button
              variant="ghost"
              onClick={() => {
                setPresidentVote({ key: phaseKey, targetId: '' })
                gameAction('president_vote')
              }}
              disabled={presidentVote?.key === phaseKey}
            >
              Abstenerse
            </Button>
          </div>

          {presidentVote?.key === phaseKey ? (
            <p className="mt-3 text-sm text-emerald-300">Voto enviado. Esperando al resto…</p>
          ) : null}
        </Card>
      ) : null}

      {game.phase === 'DEBATE' ? (
        <Card className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold">Debate</h3>
            <p className="text-sm text-parchment/60">
              Cuando todos estéis listos, se pasa directamente a la votación.
            </p>
          </div>
          <Button
            onClick={() => gameAction('ready')}
            disabled={yaListo}
          >
            {yaListo ? 'Listo (esperando…)' : 'Listo para votar'}
          </Button>
        </Card>
      ) : null}

      {game.phase === 'KNIGHT_REACTION' && game.pendingKnight === playerId ? (
        <Card className="border-blood/40">
          <h3 className="mb-1 text-lg font-semibold">Elige a quién golpear</h3>
          <p className="mb-4 text-sm text-parchment/60">
            Has muerto siendo el Caballero. Tu golpe puede cambiar la partida.
          </p>
          <div className="flex flex-wrap gap-2">
            {game.statuses
              .filter((player) => player.alive && player.id !== playerId)
              .map((player) => (
                <Button
                  key={player.id}
                  variant="secondary"
                  onClick={() => gameAction('knight_choice', { targetId: player.id })}
                >
                  {player.nickname}
                </Button>
              ))}
          </div>
        </Card>
      ) : null}

      {game.phase === 'VOTING' || game.phase === 'RUNOFF' ? (
        <Card className="text-center">
          <h3 className="text-lg font-semibold">Votación en curso</h3>
          <p className="mt-1 text-sm text-parchment/60">
            La interfaz de voto llega en la siguiente entrega. Mientras tanto, puedes votar por
            WebSocket.
          </p>
        </Card>
      ) : null}

      <Card>
        <h3 className="mb-3 text-lg font-semibold">Jugadores</h3>
        <ul className="flex flex-col gap-2">
          {game.statuses.map((player) => (
            <li
              key={player.id}
              className={[
                'flex items-center justify-between gap-3 rounded-lg border px-3 py-2 text-sm',
                player.alive
                  ? 'border-night-700 bg-night-800/50'
                  : 'border-night-700/40 bg-night-900/40 text-parchment/40',
              ].join(' ')}
            >
              <span className="flex items-center gap-2">
                <span
                  className={`h-2 w-2 rounded-full ${player.alive ? 'bg-emerald-400' : 'bg-parchment/30'}`}
                  aria-hidden="true"
                />
                <span className={player.alive ? '' : 'line-through'}>{player.nickname}</span>
                {player.id === playerId ? (
                  <span className="rounded bg-night-700 px-1.5 py-0.5 text-[10px] uppercase tracking-wide">
                    tú
                  </span>
                ) : null}
                {player.president ? (
                  <span className="rounded bg-blood/20 px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-blood">
                    presidente
                  </span>
                ) : null}
              </span>

              <span className="text-xs text-parchment/60">
                {player.alive
                  ? `${player.lives} ${player.lives === 1 ? 'vida' : 'vidas'}`
                  : 'muerto'}
              </span>
            </li>
          ))}
        </ul>
      </Card>

      {game.vision || game.witchVictim ? (
        <Card>
          <h3 className="mb-2 text-lg font-semibold">Información privada</h3>
          {game.vision ? (
            <p className="text-sm text-parchment/80">
              El Vidente ha visto: <strong>{GAME_ROLE_LABELS[game.vision] ?? game.vision}</strong>.
            </p>
          ) : null}
          {game.witchVictim ? (
            <p className="mt-2 text-sm text-parchment/80">
              Víctima de los lobos:{' '}
              <strong>
                {game.statuses.find((player) => player.id === game.witchVictim)?.nickname ??
                  game.witchVictim}
              </strong>
              .
            </p>
          ) : null}
        </Card>
      ) : null}
    </div>
  )
}
