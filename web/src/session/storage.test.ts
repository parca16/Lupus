import { describe, expect, it, beforeEach } from 'vitest'
import { clearSession, loadSession, saveSession } from './storage'

describe('storage de sesión', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('guarda y recupera la sesión', () => {
    saveSession({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' })

    expect(loadSession()).toEqual({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' })
  })

  it('devuelve null si no hay nada guardado', () => {
    expect(loadSession()).toBeNull()
  })

  it('devuelve null si lo guardado no tiene la forma esperada', () => {
    window.localStorage.setItem('lupus.session', JSON.stringify({ roomCode: 'ABC234' }))
    expect(loadSession()).toBeNull()

    window.localStorage.setItem('lupus.session', 'no es json')
    expect(loadSession()).toBeNull()
  })

  it('borra la sesión', () => {
    saveSession({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' })

    clearSession()

    expect(loadSession()).toBeNull()
  })
})
