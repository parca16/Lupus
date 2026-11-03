import { describe, expect, it } from 'vitest'
import { soundForAnnouncement } from './sounds'

describe('sonidos (mapeo de anuncios públicos)', () => {
  it('traduce muertes, vidas perdidas, curaciones y noche tranquila', () => {
    expect(soundForAnnouncement('DEATH', 'DAWN')).toBe('death')
    expect(soundForAnnouncement('LIFE_LOST', 'DAWN')).toBe('hit')
    expect(soundForAnnouncement('HEALED', 'DAWN')).toBe('calm')
    expect(soundForAnnouncement('QUIET', 'DAWN')).toBe('calm')
  })

  it('distingue amanecer, votación y fases normales', () => {
    expect(soundForAnnouncement('PHASE', 'DAWN')).toBe('dawn')
    expect(soundForAnnouncement('PHASE', 'VOTING')).toBe('vote')
    expect(soundForAnnouncement('PHASE', 'RUNOFF')).toBe('vote')
    expect(soundForAnnouncement('PHASE', 'NIGHT')).toBe('phase')
  })

  it('ignora anuncios desconocidos', () => {
    expect(soundForAnnouncement('OTRO', 'NIGHT')).toBeNull()
  })
})
