/** Sonidos del juego y su activación por eventos públicos (#61). */

export type SoundName = 'phase' | 'dawn' | 'death' | 'hit' | 'vote' | 'calm' | 'turn'

/** Elige el sonido de un anuncio. Solo usa información pública. */
export function soundForAnnouncement(kind: string, phase: string): SoundName | null {
  switch (kind) {
    case 'DEATH':
      return 'death'
    case 'LIFE_LOST':
      return 'hit'
    case 'HEALED':
    case 'QUIET':
      return 'calm'
    case 'PHASE':
      if (phase === 'DAWN') {
        return 'dawn'
      }
      if (phase === 'VOTING' || phase === 'RUNOFF') {
        return 'vote'
      }
      return 'phase'
    default:
      return null
  }
}
