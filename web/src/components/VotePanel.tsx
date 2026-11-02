import { useState } from 'react'
import type { GameStateMessage } from '../protocol'
import type { GameActionOptions } from '../session/context'
import { Button } from './ui/Button'
import { Card } from './ui/Card'

export interface VotePanelProps {
  game: GameStateMessage
  playerId: string | null
  onAction: (action: string, options?: GameActionOptions) => void
}

/**
 * Votación del linchamiento (#59): voto o abstención. Solo se muestra el voto
 * propio; el servidor no envia los votos de los demas.
 */
export function VotePanel({ game, playerId, onAction }: VotePanelProps) {
  const [selection, setSelection] = useState<string | null>(null)
  const [sent, setSent] = useState(false)

  const runoff = game.phase === 'RUNOFF'
  const yo = game.statuses.find((player) => player.id === playerId)
  if (!yo?.alive) {
    return null
  }

  const candidatos = game.statuses.filter(
    (player) =>
      player.alive &&
      player.id !== playerId &&
      player.id !== game.loverId &&
      (!runoff || game.runoffCandidates.includes(player.id)),
  )

  const votado = game.statuses.find((player) => player.id === game.myVote)

  const enviar = (targetId?: string) => {
    onAction('vote', targetId ? { targetId } : {})
    setSent(true)
  }

  return (
    <Card className="border-blood/40">
      <p className="text-xs uppercase tracking-widest text-blood">
        {runoff ? 'Segunda vuelta' : 'Votación'}
      </p>
      <h3 className="mt-1 text-lg font-semibold">
        {runoff ? 'Vota entre los empatados' : '¿A quién linchamos?'}
      </h3>
      <p className="mb-4 text-sm text-parchment/60">
        {runoff
          ? 'Solo puedes votar a los jugadores empatados. Tu voto es secreto.'
          : 'Tu voto es secreto. Puedes abstenerte si no lo tienes claro.'}
      </p>

      {game.hasVoted ? (
        <p role="status" className="text-sm text-emerald-300">
          {votado ? `Tu voto: ${votado.nickname}.` : 'Te has abstenido.'} Esperando al resto…
        </p>
      ) : sent ? (
        <p role="status" className="text-sm text-emerald-300">
          Voto enviado. Esperando al resto…
        </p>
      ) : (
        <>
          {candidatos.length > 0 ? (
            <div role="group" aria-label="Candidatos" className="flex flex-wrap gap-2">
              {candidatos.map((player) => (
                <Button
                  key={player.id}
                  variant={selection === player.id ? 'primary' : 'secondary'}
                  aria-pressed={selection === player.id}
                  onClick={() =>
                    setSelection((actual) => (actual === player.id ? null : player.id))
                  }
                >
                  {player.nickname}
                </Button>
              ))}
            </div>
          ) : (
            <p className="text-sm text-parchment/60">No hay candidatos a los que votar.</p>
          )}

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button onClick={() => enviar(selection ?? undefined)} disabled={!selection}>
              Confirmar voto
            </Button>
            <Button variant="ghost" onClick={() => enviar()}>
              Abstenerse
            </Button>
          </div>
        </>
      )}
    </Card>
  )
}
