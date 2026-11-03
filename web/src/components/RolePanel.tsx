import { GAME_ROLE_DESCRIPTIONS, GAME_ROLE_LABELS } from '../game/labels'
import { ROLE_GLOSSARY } from '../game/glossary'
import type { GameStateMessage } from '../protocol'
import { Card } from './ui/Card'

export interface RolePanelProps {
  role: string
  loverId: string
  statuses: GameStateMessage['statuses']
}

/** Panel permanente con tu carta y el glosario de roles (#57, #77). */
export function RolePanel({ role, loverId, statuses }: RolePanelProps) {
  const label = GAME_ROLE_LABELS[role] ?? role
  const descripcion = GAME_ROLE_DESCRIPTIONS[role] ?? ''
  const enamorado = loverId
    ? statuses.find((player) => player.id === loverId)?.nickname
    : undefined

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-widest text-parchment/50">Tu carta</p>
          <h3 className="mt-1 text-lg font-semibold">{label}</h3>
          <p className="mt-1 text-sm text-parchment/60">{descripcion}</p>
          {enamorado ? (
            <p className="mt-2 text-sm text-parchment/80">
              Estás enamorado de <strong>{enamorado}</strong>.
            </p>
          ) : null}
        </div>
      </div>

      <details className="mt-4 rounded-lg border border-night-700 bg-night-800/40 px-3 py-2">
        <summary className="cursor-pointer text-sm font-medium text-parchment/80">
          Glosario de roles
        </summary>
        <ul className="mt-3 flex flex-col gap-3">
          {ROLE_GLOSSARY.map((entry) => (
            <li key={entry.id} className="text-sm">
              <p className="flex flex-wrap items-center gap-2">
                <strong>{entry.label}</strong>
                <span className="rounded bg-night-700 px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-parchment/60">
                  {entry.team}
                </span>
              </p>
              <p className="text-parchment/70">{entry.summary}</p>
              <p className="mt-1 text-xs text-parchment/50">{entry.details}</p>
            </li>
          ))}
        </ul>
      </details>
    </Card>
  )
}
