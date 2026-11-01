import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { Player } from '../protocol'
import type { SessionValue } from '../session/context'
import { SessionContext } from '../session/context'
import { LobbyScreen } from './LobbyScreen'

const ANA: Player = { id: 'p-ana', nickname: 'Ana', host: true, connected: true }
const LUIS: Player = { id: 'p-luis', nickname: 'Luis', host: false, connected: false }

function renderLobby(overrides: Partial<SessionValue> = {}) {
  const value: SessionValue = {
    status: 'open',
    connectionId: 'conn-1',
    roomCode: 'ABC234',
    playerId: 'p-ana',
    players: [ANA, LUIS],
    error: null,
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    leave: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  }

  render(
    <MemoryRouter>
      <SessionContext.Provider value={value}>
        <LobbyScreen />
      </SessionContext.Provider>
    </MemoryRouter>,
  )

  return value
}

describe('LobbyScreen', () => {
  it('muestra el código de sala y la lista de jugadores con su estado', () => {
    renderLobby()

    expect(screen.getByText('ABC234')).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('Luis')).toBeInTheDocument()
    expect(screen.getByText('host')).toBeInTheDocument()
    expect(screen.getByText('tú')).toBeInTheDocument()
    expect(screen.getByText('desconectado')).toBeInTheDocument()
    expect(screen.getByText('Conectado')).toBeInTheDocument()
    expect(screen.getByText(/faltan 4 jugador/i)).toBeInTheDocument()
  })

  it('avisa cuando ya hay jugadores suficientes', () => {
    const players = Array.from({ length: 5 }, (_, index) => ({
      id: `p-${index}`,
      nickname: `Jugador${index}`,
      host: index === 0,
      connected: true,
    }))

    renderLobby({ players })

    expect(screen.getByText(/ya hay jugadores suficientes/i)).toBeInTheDocument()
  })

  it('permite salir de la sala', () => {
    const value = renderLobby()

    fireEvent.click(screen.getByRole('button', { name: 'Salir' }))

    expect(value.leave).toHaveBeenCalled()
  })

  it('invita a volver al inicio si no hay sala activa', () => {
    renderLobby({ roomCode: null, players: [] })

    expect(screen.getByText('No hay ninguna sala activa')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver al inicio' })).toBeInTheDocument()
  })
})
