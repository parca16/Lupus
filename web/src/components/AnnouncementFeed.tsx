import { useSession } from '../session/useSession'
import { Card } from './ui/Card'

const KINDS: Record<string, { icon: string; label: string; className: string }> = {
  PHASE: { icon: '📜', label: 'Fase', className: 'border-night-700' },
  DEATH: { icon: '💀', label: 'Muerte', className: 'border-blood/60' },
  LIFE_LOST: { icon: '🩸', label: 'Vida perdida', className: 'border-amber-400/50' },
  HEALED: { icon: '✨', label: 'Curación', className: 'border-emerald-400/50' },
  QUIET: { icon: '🌙', label: 'Noche tranquila', className: 'border-emerald-400/40' },
}

/** Anuncios destacados del narrador pasivo y su historial (#67). */
export function AnnouncementFeed() {
  const { announcements } = useSession()

  if (announcements.length === 0) {
    return null
  }

  const ultimo = announcements[announcements.length - 1]
  const estilo = KINDS[ultimo.kind] ?? {
    icon: '📜',
    label: 'Anuncio',
    className: 'border-night-700',
  }
  const historial = [...announcements.slice(0, -1)].reverse()

  return (
    <Card className={estilo.className} aria-live="polite">
      <div className="flex items-start gap-3">
        <span className="text-2xl" aria-hidden="true">
          {estilo.icon}
        </span>
        <div>
          <p className="text-xs uppercase tracking-widest text-parchment/50">
            {estilo.label} · Ronda {ultimo.round}
          </p>
          <p className="mt-1 text-base font-medium text-parchment">{ultimo.text}</p>
        </div>
      </div>

      {historial.length > 0 ? (
        <details className="mt-4 rounded-lg border border-night-700 bg-night-900/40 px-3 py-2">
          <summary className="cursor-pointer text-sm font-medium text-parchment/70">
            Historial de anuncios ({historial.length})
          </summary>
          <ol className="mt-3 flex flex-col gap-2 text-sm text-parchment/60">
            {historial.map((announcement) => (
              <li key={announcement.id} className="border-l-2 border-night-700 pl-3">
                <span className="text-xs uppercase tracking-wide text-parchment/40">
                  Ronda {announcement.round}
                </span>
                <p>{announcement.text}</p>
              </li>
            ))}
          </ol>
        </details>
      ) : null}
    </Card>
  )
}
