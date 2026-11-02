import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { GameStateMessage } from '../protocol'
import type { SessionValue } from '../session/context'
import { SessionContext } from '../session/context'
import { GameScreen } from './GameScreen'

const STATUSES: GameStateMessage['statuses'] = [
  { id: 'p1', nickname: 'Ana', lives: 1, alive: true, president: false },
  { id: 'p2', nickname: 'Luis', lives: 0, alive: false, president: false },
  { id: 'p3', nickname: 'Marta', lives: 1, alive: true, president: true },
]

function estado(overrides: Partial<GameStateMessage> = {}): GameStateMessage {
  return {
    v: 1,
    type: 'game_state',
    id: 'g-1',
    at: 1,
    roomCode: 'ABC234',
    phase: 'REVEAL',
    round: 1,
    deadline: Date.now() + 10_000,
    nightStep: '',
    role: 'SEER',
    loverId: '',
    vision: '',
    witchVictim: '',
    pendingKnight: '',
    ready: [],
    statuses: STATUSES,
    ...overrides,
  }
}

function renderGame(overrides: Partial<SessionValue> = {}) {
  const value: SessionValue = {
    status: 'open',
    connectionId: 'conn-1',
    roomCode: 'ABC234',
    playerId: 'p1',
    players: [],
    settings: null,
    game: estado(),
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
        <GameScreen />
      </SessionContext.Provider>
    </MemoryRouter>,
  )

  return value
}

describe('GameScreen', () => {
  it('muestra la fase y revela la carta al pulsar', () => {
    renderGame()

    expect(screen.getByText('Revelación')).toBeInTheDocument()
    expect(screen.getByText('Pulsa para revelar')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Revelar tu carta' }))

    expect(screen.getByText('Vidente')).toBeInTheDocument()
    expect(screen.getByText(/descubres el rol/i)).toBeInTheDocument()
  })

  it('muestra la lista de jugadores con vidas, estado y presidente', () => {
    renderGame()

    expect(screen.getByText('Marta')).toBeInTheDocument()
    expect(screen.getByText('presidente')).toBeInTheDocument()
    expect(screen.getByText('muerto')).toBeInTheDocument()
    expect(screen.getAllByText('1 vida')).toHaveLength(2)
  })

  it('permite votar al Presidente en su fase', () => {
    const value = renderGame({ game: estado({ phase: 'PRESIDENT_ELECTION', role: '' }) })

    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))

    expect(value.gameAction).toHaveBeenCalledWith('president_vote', { targetId: 'p3' })
    expect(screen.getByText(/voto enviado/i)).toBeInTheDocument()
  })

  it('avisa cuando es tu turno de noche', () => {
    renderGame({ game: estado({ phase: 'NIGHT', nightStep: 'SEER', role: 'SEER' }) })

    expect(screen.getByText(/turno de/i)).toHaveTextContent(/Vidente/i)
    expect(screen.getByText(/es tu turno/i)).toBeInTheDocument()
  })

  it('no muestra el panel nocturno si el paso no es de tu rol', () => {
    renderGame({ game: estado({ phase: 'NIGHT', nightStep: 'SEER', role: 'VILLAGER' }) })

    expect(screen.getByText(/turno de/i)).toHaveTextContent(/Vidente/i)
    expect(screen.queryByText('Tu turno: Vidente')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument()
  })

  it('permite al Vidente elegir objetivo y confirma el envío', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'SEER', role: 'SEER' }),
    })

    expect(screen.getByText('Tu turno: Vidente')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'seer',
      targetId: 'p3',
    })
    expect(screen.getByRole('status')).toHaveTextContent(/acción enviada/i)
  })

  it('envía el voto de los lobos con su tipo de acción', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'WOLVES', role: 'WOLF' }),
    })

    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar ataque' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'wolf_vote',
      targetId: 'p3',
    })
  })

  it('el Ángel puede protegerse a sí mismo', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'GUARDIAN', role: 'GUARDIAN' }),
    })

    fireEvent.click(screen.getByRole('button', { name: 'Ana' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'guardian',
      targetId: 'p1',
    })
  })

  it('Cupido exige dos enamorados distintos antes de confirmar', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'CUPID', role: 'CUPID' }),
    })

    const confirmar = screen.getByRole('button', { name: 'Confirmar enamorados' })
    expect(confirmar).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Ana' }))
    expect(confirmar).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))
    expect(confirmar).toBeEnabled()

    fireEvent.click(confirmar)

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'cupid',
      targetId: 'p1',
      secondTargetId: 'p3',
    })
  })

  it('la Bruja puede curar a la víctima de los lobos', () => {
    const value = renderGame({
      game: estado({
        phase: 'NIGHT',
        nightStep: 'WITCH',
        role: 'WITCH',
        witchVictim: 'p1',
      }),
    })

    expect(screen.getByRole('button', { name: 'Curar a Ana' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Curar a Ana' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'witch_heal',
      targetId: 'p1',
    })
  })

  it('la Bruja puede envenenar a un jugador', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'WITCH', role: 'WITCH' }),
    })

    fireEvent.click(screen.getByRole('button', { name: 'Envenenar a alguien' }))
    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar veneno' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', {
      kind: 'witch_poison',
      targetId: 'p3',
    })
  })

  it('la Bruja puede guardar sus pociones', () => {
    const value = renderGame({
      game: estado({ phase: 'NIGHT', nightStep: 'WITCH', role: 'WITCH' }),
    })

    fireEvent.click(screen.getByRole('button', { name: 'No usar poción' }))

    expect(value.gameAction).toHaveBeenCalledWith('night_action', { kind: 'witch_none' })
  })

  it('ofrece el botón de listo durante el debate', () => {
    const value = renderGame({ game: estado({ phase: 'DEBATE', role: 'VILLAGER' }) })

    fireEvent.click(screen.getByRole('button', { name: 'Listo para votar' }))

    expect(value.gameAction).toHaveBeenCalledWith('ready')
  })

  it('avisa si no hay partida', () => {
    renderGame({ game: null })

    expect(screen.getByText('No hay ninguna partida en curso')).toBeInTheDocument()
  })
})
