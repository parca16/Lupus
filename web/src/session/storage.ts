/** Persistencia de la identidad del jugador para sobrevivir a recargas (issue #26). */

export interface StoredSession {
  roomCode: string
  playerId: string
  nickname: string
}

const KEY = 'lupus.session'

function storage(): Storage | null {
  try {
    return window.localStorage
  } catch {
    return null
  }
}

export function loadSession(): StoredSession | null {
  const store = storage()
  if (!store) {
    return null
  }

  try {
    const raw = store.getItem(KEY)
    if (!raw) {
      return null
    }

    const parsed: unknown = JSON.parse(raw)
    if (typeof parsed !== 'object' || parsed === null) {
      return null
    }

    const candidate = parsed as Record<string, unknown>
    if (
      typeof candidate.roomCode === 'string' &&
      typeof candidate.playerId === 'string' &&
      typeof candidate.nickname === 'string'
    ) {
      return {
        roomCode: candidate.roomCode,
        playerId: candidate.playerId,
        nickname: candidate.nickname,
      }
    }
  } catch {
    return null
  }

  return null
}

export function saveSession(session: StoredSession): void {
  try {
    storage()?.setItem(KEY, JSON.stringify(session))
  } catch {
    // Sin almacenamiento disponible (modo privado): la sesión solo vive en memoria.
  }
}

export function clearSession(): void {
  try {
    storage()?.removeItem(KEY)
  } catch {
    // Nada que limpiar.
  }
}
