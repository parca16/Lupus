import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { GameSettings, Player } from '../protocol'
import type { SessionValue } from '../session/context'
import { SessionContext } from '../session/context'
import { LobbyScreen } from './LobbyScreen'

const ANA: Player = { id: 'p-ana', nickname: 'Ana', host: true, connected: true }
const LUIS: Player = { id: 'p-luis', nickname: 'Luis', host: false, connected: false }

const SETTINGS: GameSettings = {
  wolves: 2,
  roles: ['seer', 'witch'],
  lives: 1,
  president: false,
  debateSeconds: 120,
  roleSeconds: 30,
  voteSeconds: 45,
  showVoteDetail: true,
}

function cincoJugadores(): Player[] {
  return Array.from({ length: 5 }, (_, index) => ({
    id: index === 0 ? 'p-ana' : `p-${index}`,
    nickname: index === 0 ? 'Ana' : `Jugador${index}`,
    host: index === 0,
    connected: true,
  }))
}

function renderLobby(overrides: Partial<SessionValue> = {}) {
  const value: SessionValue = {
    status: 'open',
    connectionId: 'conn-1',
    roomCode: 'ABC234',
    playerId: 'p-ana',
    players: [ANA, LUIS],
    settings: SETTINGS,
    game: null,
    dawn: null,
    gameOver: null,
    starting: false,
    restoring: false,
    error: null,
    notice: null,
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    kickPlayer: vi.fn(),
    updateSettings: vi.fn(),
    startGame: vi.fn(),
    gameAction: vi.fn(),
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

  it('permite salir de la sala', () => {
    const value = renderLobby()

    fireEvent.click(screen.getByRole('button', { name: 'Salir' }))

    expect(value.leave).toHaveBeenCalled()
  })

  it('deja al host expulsar a otros jugadores', () => {
    const value = renderLobby()

    fireEvent.click(screen.getByRole('button', { name: 'Expulsar a Luis' }))

    expect(value.kickPlayer).toHaveBeenCalledWith('p-luis')
  })

  it('no muestra la opción de expulsar a quien no es host', () => {
    renderLobby({ playerId: 'p-luis' })

    expect(screen.queryByRole('button', { name: /expulsar a/i })).not.toBeInTheDocument()
  })

  it('el host puede guardar una configuración nueva', () => {
    const value = renderLobby()

    expect(screen.getByText('Solo tú puedes cambiarla')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Número de lobos'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }))

    expect(value.updateSettings).toHaveBeenCalledWith(expect.objectContaining({ wolves: 3 }))
  })

  it('quien no es host ve la configuración en modo lectura', () => {
    renderLobby({ playerId: 'p-luis' })

    expect(screen.queryByLabelText('Número de lobos')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar configuración' })).not.toBeInTheDocument()
    expect(screen.getByText('Vidente, Bruja')).toBeInTheDocument()
  })

  it('deshabilita Empezar hasta tener cinco jugadores conectados', () => {
    const { unmount } = render(
      <MemoryRouter>
        <SessionContext.Provider
          value={{
            status: 'open',
            connectionId: 'c',
            roomCode: 'ABC234',
            playerId: 'p-ana',
            players: [ANA, LUIS],
            settings: SETTINGS,
            game: null,
            dawn: null,
            gameOver: null,
            starting: false,
            restoring: false,
            error: null,
            notice: null,
            createRoom: vi.fn(),
            joinRoom: vi.fn(),
            kickPlayer: vi.fn(),
            updateSettings: vi.fn(),
            startGame: vi.fn(),
            gameAction: vi.fn(),
            leave: vi.fn(),
            clearError: vi.fn(),
          }}
        >
          <LobbyScreen />
        </SessionContext.Provider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('button', { name: 'Empezar partida' })).toBeDisabled()
    unmount()
  })

  it('el host puede empezar con cinco jugadores conectados', () => {
    const value = renderLobby({ players: cincoJugadores() })

    const boton = screen.getByRole('button', { name: 'Empezar partida' })
    expect(boton).toBeEnabled()
    expect(screen.getByText('Listo para empezar')).toBeInTheDocument()

    fireEvent.click(boton)
    expect(value.startGame).toHaveBeenCalled()
  })

  it('avisa cuando la partida está empezando', () => {
    renderLobby({ starting: true })

    expect(screen.getByText('La partida va a empezar')).toBeInTheDocument()
  })

  it('invita a volver al inicio si no hay sala activa y explica el motivo', () => {
    renderLobby({
      roomCode: null,
      players: [],
      notice: 'El host te ha expulsado de la sala.',
    })

    expect(screen.getByText('No hay ninguna sala activa')).toBeInTheDocument()
    expect(screen.getByText('El host te ha expulsado de la sala.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver al inicio' })).toBeInTheDocument()
  })
})
