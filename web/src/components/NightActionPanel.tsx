import { useState } from 'react'
import { NIGHT_STEP_LABELS } from '../game/labels'
import type { GameStateMessage } from '../protocol'
import type { GameActionOptions } from '../session/context'
import { Button } from './ui/Button'
import { Card } from './ui/Card'

/** Pasos de la noche en los que el jugador tiene que elegir un objetivo. */
const DESCRIPCIONES: Record<string, string> = {
  CUPID: 'Elige a los dos enamorados. Puedes elegirte a ti mismo.',
  SEER: 'Elige a quién observar esta noche. Verás su rol.',
  GUARDIAN: 'Elige a quién proteger esta noche. Puedes protegerte a ti mismo.',
  WOLVES: 'Elige a quién atacar esta noche.',
  WITCH: 'Usa una de tus pociones o guárdalas para otra noche.',
}

type ModoBruja = 'heal' | 'poison' | null

export interface NightActionPanelProps {
  game: GameStateMessage
  playerId: string | null
  onAction: (action: string, options?: GameActionOptions) => void
}

/**
 * Panel de acción nocturna (#58): selección de objetivo, confirmación y feedback
 * inmediato. El servidor sigue validando el turno y el objetivo.
 */
export function NightActionPanel({ game, playerId, onAction }: NightActionPanelProps) {
  const [firstTargetId, setFirstTargetId] = useState<string | null>(null)
  const [secondTargetId, setSecondTargetId] = useState<string | null>(null)
  const [witchMode, setWitchMode] = useState<ModoBruja>(null)
  const [sent, setSent] = useState(false)

  const step = game.nightStep
  if (!step || !(step in DESCRIPCIONES)) {
    return null
  }

  const yo = game.statuses.find((player) => player.id === playerId)
  if (!yo?.alive) {
    return null
  }

  const vivos = game.statuses.filter((player) => player.alive)
  const otros = vivos.filter((player) => player.id !== playerId)
  const victima = game.witchVictim
    ? game.statuses.find((player) => player.id === game.witchVictim)
    : undefined

  const enviar = (kind: string, options: GameActionOptions = {}) => {
    onAction('night_action', { kind, ...options })
    setSent(true)
  }

  const elegirPrimero = (targetId: string) => {
    setFirstTargetId((actual) => (actual === targetId ? null : targetId))
  }

  const elegirEnamorado = (targetId: string) => {
    if (firstTargetId === targetId) {
      setFirstTargetId(null)
      return
    }
    if (secondTargetId === targetId) {
      setSecondTargetId(null)
      return
    }
    if (!firstTargetId) {
      setFirstTargetId(targetId)
      return
    }
    if (!secondTargetId) {
      setSecondTargetId(targetId)
      return
    }
    // Ya había dos: el nuevo pasa a ser el segundo y el anterior segundo pasa a primero.
    setFirstTargetId(secondTargetId)
    setSecondTargetId(targetId)
  }

  const listaObjetivos = (jugadores: GameStateMessage['statuses'], onSelect: (id: string) => void) => (
    <div role="group" aria-label="Objetivos" className="flex flex-wrap gap-2">
      {jugadores.map((player) => (
        <Button
          key={player.id}
          variant={
            player.id === firstTargetId || player.id === secondTargetId ? 'primary' : 'secondary'
          }
          aria-pressed={player.id === firstTargetId || player.id === secondTargetId}
          onClick={() => onSelect(player.id)}
          disabled={sent}
        >
          {player.nickname}
        </Button>
      ))}
    </div>
  )

  const confirmar = (kind: string, objetivo = firstTargetId) => {
    if (!objetivo) {
      return
    }
    enviar(kind, { targetId: objetivo })
  }

  return (
    <Card className="border-amber-400/40">
      <p className="text-xs uppercase tracking-widest text-amber-300/80">Acción nocturna</p>
      <h3 className="mt-1 text-lg font-semibold">
        Tu turno: {step === 'WITCH' ? 'Bruja' : NIGHT_STEP_LABELS[step]}
      </h3>
      <p className="mb-4 text-sm text-parchment/60">{DESCRIPCIONES[step]}</p>

      {step === 'CUPID' ? (
        <>
          {listaObjetivos(vivos, elegirEnamorado)}
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button
              onClick={() => {
                if (firstTargetId && secondTargetId) {
                  enviar('cupid', {
                    targetId: firstTargetId,
                    secondTargetId,
                  })
                }
              }}
              disabled={sent || !firstTargetId || !secondTargetId}
            >
              Confirmar enamorados
            </Button>
            <span className="text-xs text-parchment/50">
              {firstTargetId && secondTargetId
                ? 'Dos enamorados elegidos.'
                : 'Elige dos jugadores distintos.'}
            </span>
          </div>
        </>
      ) : null}

      {step === 'SEER' ? (
        <>
          {listaObjetivos(otros, elegirPrimero)}
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button onClick={() => confirmar('seer')} disabled={sent || !firstTargetId}>
              Confirmar
            </Button>
          </div>
        </>
      ) : null}

      {step === 'GUARDIAN' ? (
        <>
          {listaObjetivos(vivos, elegirPrimero)}
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button onClick={() => confirmar('guardian')} disabled={sent || !firstTargetId}>
              Confirmar
            </Button>
            <span className="text-xs text-parchment/50">
              Recuerda: no puedes repetir objetivo dos noches seguidas.
            </span>
          </div>
        </>
      ) : null}

      {step === 'WOLVES' ? (
        <>
          {listaObjetivos(otros, elegirPrimero)}
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <Button onClick={() => confirmar('wolf_vote')} disabled={sent || !firstTargetId}>
              Confirmar ataque
            </Button>
          </div>
        </>
      ) : null}

      {step === 'WITCH' ? (
        <>
          <p className="mb-3 text-sm text-parchment/80">
            {victima ? (
              <>
                Víctima de los lobos: <strong>{victima.nickname}</strong>.
              </>
            ) : (
              'Los lobos no han elegido víctima esta noche.'
            )}
          </p>

          {witchMode === 'poison' ? (
            <>
              {listaObjetivos(
                vivos.filter((player) => player.id !== playerId),
                elegirPrimero,
              )}
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <Button
                  onClick={() => confirmar('witch_poison')}
                  disabled={sent || !firstTargetId}
                >
                  Confirmar veneno
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => {
                    setWitchMode(null)
                    setFirstTargetId(null)
                  }}
                  disabled={sent}
                >
                  Cancelar
                </Button>
              </div>
            </>
          ) : (
            <div className="flex flex-wrap gap-2">
              {victima ? (
                <Button
                  onClick={() => enviar('witch_heal', { targetId: victima.id })}
                  disabled={sent}
                >
                  Curar a {victima.nickname}
                </Button>
              ) : null}
              <Button
                variant="secondary"
                onClick={() => {
                  setWitchMode('poison')
                  setFirstTargetId(null)
                }}
                disabled={sent}
              >
                Envenenar a alguien
              </Button>
              <Button variant="ghost" onClick={() => enviar('witch_none')} disabled={sent}>
                No usar poción
              </Button>
            </div>
          )}
        </>
      ) : null}

      {sent ? (
        <p role="status" className="mt-4 text-sm text-emerald-300">
          Acción enviada. Esperando al resto…
        </p>
      ) : null}
    </Card>
  )
}
