import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { GameOverMessage, GameStateMessage } from '../protocol'
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
    myVote: '',
    hasVoted: false,
    runoffCandidates: [],
    lastVoteRound: 0,
    lastVoteRunoffNeeded: false,
    lastVoteLynchedId: '',
    lastVoteTied: [],
    lastVoteAbstentions: 0,
    lastVoteCounts: [],
    ready: [],
    statuses: STATUSES,
    ...overrides,
  }
}

function finDePartida(overrides: Partial<GameOverMessage> = {}): GameOverMessage {
  return {
    v: 1,
    type: 'game_over',
    id: 'over-1',
    at: 2,
    roomCode: 'ABC234',
    outcome: 'WOLVES',
    roles: [
      { playerId: 'p1', nickname: 'Ana', role: 'WOLF' },
      { playerId: 'p2', nickname: 'Luis', role: 'VILLAGER' },
      { playerId: 'p3', nickname: 'Marta', role: 'SEER' },
    ],
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
    returnedToLobby: false,
    error: null,
    notice: null,
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    kickPlayer: vi.fn(),
    updateSettings: vi.fn(),
    startGame: vi.fn(),
    returnToLobby: vi.fn(),
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

  it('permite votar y confirma el envío sin revelar otros votos', () => {
    const value = renderGame({ game: estado({ phase: 'VOTING', role: 'VILLAGER' }) })

    expect(screen.getByText('¿A quién linchamos?')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar voto' })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Marta' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar voto' }))

    expect(value.gameAction).toHaveBeenCalledWith('vote', { targetId: 'p3' })
    expect(screen.getByRole('status')).toHaveTextContent(/voto enviado/i)
  })

  it('permite abstenerse en la votación', () => {
    const value = renderGame({ game: estado({ phase: 'VOTING', role: 'VILLAGER' }) })

    fireEvent.click(screen.getByRole('button', { name: 'Abstenerse' }))

    expect(value.gameAction).toHaveBeenCalledWith('vote', {})
  })

  it('muestra solo tu propio voto después de votar', () => {
    renderGame({
      game: estado({ phase: 'VOTING', role: 'VILLAGER', hasVoted: true, myVote: 'p3' }),
    })

    expect(screen.getByRole('status')).toHaveTextContent(/Tu voto: Marta/i)
    expect(screen.queryByRole('button', { name: 'Confirmar voto' })).not.toBeInTheDocument()
  })

  it('en la segunda vuelta solo ofrece a los empatados', () => {
    const statuses = [
      ...STATUSES,
      { id: 'p4', nickname: 'Pablo', lives: 1, alive: true, president: false },
    ]
    renderGame({
      game: estado({
        phase: 'RUNOFF',
        role: 'VILLAGER',
        runoffCandidates: ['p3'],
        statuses,
      }),
    })

    expect(screen.getByText('Vota entre los empatados')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Marta' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pablo' })).not.toBeInTheDocument()
  })

  it('muestra el recuento y el resultado al cerrar la votación', () => {
    renderGame({
      game: estado({
        phase: 'NIGHT',
        round: 2,
        role: 'VILLAGER',
        lastVoteRound: 1,
        lastVoteLynchedId: 'p3',
        lastVoteCounts: [
          { playerId: 'p3', votes: 3 },
          { playerId: 'p1', votes: 1 },
        ],
        lastVoteAbstentions: 1,
      }),
    })

    expect(screen.getByText('Resultado de la votación')).toBeInTheDocument()
    expect(screen.getByText(/Más votado:/)).toHaveTextContent(/Marta/i)
    expect(screen.getByText('3 votos')).toBeInTheDocument()
    expect(screen.getByText(/Abstenciones: 1/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar' }))

    expect(screen.queryByText('Resultado de la votación')).not.toBeInTheDocument()
  })

  it('explica el empate que lleva a la segunda vuelta', () => {
    renderGame({
      game: estado({
        phase: 'RUNOFF',
        role: 'VILLAGER',
        lastVoteRound: 1,
        lastVoteRunoffNeeded: true,
        lastVoteTied: ['p1', 'p3'],
        runoffCandidates: ['p1', 'p3'],
        lastVoteCounts: [
          { playerId: 'p1', votes: 2 },
          { playerId: 'p3', votes: 2 },
        ],
      }),
    })

    expect(screen.getByText(/Empate entre/)).toHaveTextContent(/Ana y Marta/i)
  })

  it('no muestra el resultado antiguo durante el debate', () => {
    renderGame({
      game: estado({
        phase: 'DEBATE',
        role: 'VILLAGER',
        lastVoteRound: 1,
        lastVoteLynchedId: 'p3',
        lastVoteCounts: [{ playerId: 'p3', votes: 3 }],
      }),
    })

    expect(screen.queryByText('Resultado de la votación')).not.toBeInTheDocument()
  })

  it('muestra el bando ganador y todos los roles revelados', () => {
    renderGame({
      game: estado({ phase: 'FINISHED' }),
      gameOver: finDePartida(),
    })

    expect(screen.getByText('Ganan los lobos')).toBeInTheDocument()
    expect(screen.getByText('Roles revelados')).toBeInTheDocument()
    expect(screen.getByText('Lobo')).toBeInTheDocument()
    expect(screen.getByText('Aldeano')).toBeInTheDocument()
    expect(screen.getByText('Vidente')).toBeInTheDocument()
  })

  it('el host puede volver a la configuración sin arrancar otra partida', () => {
    const value = renderGame({
      game: estado({ phase: 'FINISHED' }),
      gameOver: finDePartida(),
      players: [{ id: 'p1', nickname: 'Ana', host: true, connected: true }],
    })

    fireEvent.click(screen.getByRole('button', { name: 'Volver a configuración' }))

    expect(value.returnToLobby).toHaveBeenCalledTimes(1)
  })

  it('los invitados esperan a que el host vuelva a la configuración', () => {
    renderGame({
      game: estado({ phase: 'FINISHED' }),
      gameOver: finDePartida(),
      players: [{ id: 'p1', nickname: 'Ana', host: false, connected: true }],
    })

    expect(screen.getByText(/Esperando a que el host/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Volver a configuración' })).not.toBeInTheDocument()
  })

  it('muestra tu carta y el glosario durante la partida', () => {
    renderGame({ game: estado({ phase: 'NIGHT', role: 'SEER' }) })

    expect(screen.getByText('Tu carta')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vidente' })).toBeInTheDocument()
    expect(screen.getByText('Glosario de roles')).toBeInTheDocument()

    fireEvent.click(screen.getByText('Glosario de roles'))

    expect(screen.getByText('Lobo')).toBeInTheDocument()
    expect(screen.getByText('Bruja')).toBeInTheDocument()
    expect(screen.getByText('Presidente')).toBeInTheDocument()
  })

  it('los muertos también pueden consultar su carta y el glosario', () => {
    const statuses = STATUSES.map((player) =>
      player.id === 'p1' ? { ...player, alive: false } : player,
    )
    renderGame({ game: estado({ phase: 'NIGHT', role: 'SEER', statuses }) })

    expect(screen.getByText(/Estás muerto/)).toBeInTheDocument()
    expect(screen.getByText('Glosario de roles')).toBeInTheDocument()
  })

  it('no duplica el panel de carta durante la revelación', () => {
    renderGame({ game: estado({ phase: 'REVEAL', role: 'SEER' }) })

    expect(screen.getByText('Pulsa para revelar')).toBeInTheDocument()
    expect(screen.queryByText('Glosario de roles')).not.toBeInTheDocument()
  })

  it('avisa si no hay partida', () => {
    renderGame({ game: null })

    expect(screen.getByText('No hay ninguna partida en curso')).toBeInTheDocument()
  })
})
