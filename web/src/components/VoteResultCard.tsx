import { useState } from 'react'
import type { GameStateMessage } from '../protocol'
import { Button } from './ui/Button'
import { Card } from './ui/Card'

const FASES_CON_RESULTADO = new Set([
  'RUNOFF',
  'NIGHT',
  'DAWN',
  'KNIGHT_REACTION',
  'FINISHED',
])

export interface VoteResultCardProps {
  game: GameStateMessage
}

/** Recuento de la última votación (#59). Nunca muestra quién votó a quién. */
export function VoteResultCard({ game }: VoteResultCardProps) {
  const [dismissed, setDismissed] = useState(false)

  if (dismissed || game.lastVoteRound <= 0 || !FASES_CON_RESULTADO.has(game.phase)) {
    return null
  }

  const nombre = (playerId: string) =>
    game.statuses.find((player) => player.id === playerId)?.nickname ?? playerId

  const counts = [...game.lastVoteCounts].sort(
    (a, b) => b.votes - a.votes || nombre(a.playerId).localeCompare(nombre(b.playerId)),
  )
  const empatados = game.lastVoteTied.map(nombre)

  return (
    <Card className="border-parchment/20">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-widest text-parchment/50">
            Ronda {game.lastVoteRound}
          </p>
          <h3 className="mt-1 text-lg font-semibold">Resultado de la votación</h3>
        </div>
        <Button variant="ghost" onClick={() => setDismissed(true)}>
          Cerrar
        </Button>
      </div>

      <p className="mt-3 text-sm text-parchment/80">
        {game.lastVoteRunoffNeeded ? (
          <>
            Empate entre <strong>{empatados.join(' y ')}</strong>. Se pasa a segunda vuelta.
          </>
        ) : game.lastVoteLynchedId ? (
          <>
            Más votado: <strong>{nombre(game.lastVoteLynchedId)}</strong>.
          </>
        ) : empatados.length > 0 ? (
          <>
            Empate final entre <strong>{empatados.join(' y ')}</strong>. Nadie es linchado.
          </>
        ) : (
          'Nadie ha recibido votos.'
        )}
      </p>

      {counts.length > 0 ? (
        <ul className="mt-3 flex flex-col gap-1 text-sm text-parchment/70">
          {counts.map((count) => (
            <li key={count.playerId} className="flex items-center justify-between gap-3">
              <span>{nombre(count.playerId)}</span>
              <span className="font-mono">
                {count.votes} {count.votes === 1 ? 'voto' : 'votos'}
              </span>
            </li>
          ))}
        </ul>
      ) : null}

      <p className="mt-3 text-xs text-parchment/50">
        Abstenciones: {game.lastVoteAbstentions}. El voto del Presidente vale doble.
      </p>
    </Card>
  )
}
