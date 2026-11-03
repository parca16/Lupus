import { useCallback, useEffect, useRef, useState } from 'react'
import { STEP_ROLE } from '../game/labels'
import { playSound, vibrate } from '../game/audio'
import { soundForAnnouncement } from '../game/sounds'
import { useSession } from '../session/useSession'

const STORAGE_KEY = 'lupus.muted'

function leerSilencio(): boolean {
  try {
    return window.localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

/** Sonidos y vibración ligados a anuncios públicos y a tu propio turno (#61). */
export function useGameSounds() {
  const { announcements, game } = useSession()
  const [muted, setMuted] = useState(leerSilencio)
  const lastAnnouncement = useRef<string | null>(null)
  const lastTurn = useRef<string | null>(null)

  const toggleMuted = useCallback(() => {
    setMuted((previous) => {
      const next = !previous
      try {
        window.localStorage.setItem(STORAGE_KEY, next ? '1' : '0')
      } catch {
        // localStorage puede no estar disponible; el toggle sigue funcionando en memoria.
      }
      return next
    })
  }, [])

  useEffect(() => {
    if (announcements.length === 0) {
      lastAnnouncement.current = null
      return
    }

    const latest = announcements[announcements.length - 1]
    if (lastAnnouncement.current === latest.id) {
      return
    }
    lastAnnouncement.current = latest.id

    if (muted) {
      return
    }

    const sound = soundForAnnouncement(latest.kind, game?.phase ?? '')
    if (sound) {
      playSound(sound)
    }
    if (latest.kind === 'DEATH') {
      vibrate([80, 40, 120])
    } else if (latest.kind === 'PHASE' && (game?.phase === 'VOTING' || game?.phase === 'RUNOFF')) {
      vibrate(60)
    }
  }, [announcements, game?.phase, muted])

  const esMiTurno =
    game?.nightStep !== undefined &&
    game.nightStep !== '' &&
    STEP_ROLE[game.nightStep] === game.role

  useEffect(() => {
    if (!game || !esMiTurno) {
      return
    }

    const turnKey = `${game.round}-${game.nightStep}-${game.role}`
    if (lastTurn.current === turnKey) {
      return
    }
    lastTurn.current = turnKey

    if (muted) {
      return
    }
    playSound('turn')
    vibrate(70)
  }, [esMiTurno, game, muted])

  return { muted, toggleMuted }
}
