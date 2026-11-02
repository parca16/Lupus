import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Message } from '../protocol'
import { SessionProvider } from './SessionProvider'
import { useSession } from './useSession'

const mocks = vi.hoisted(() => ({ sockets: [] as FakeSocket[] }))

interface FakeSocket {
  nickname: string | null
  sent: Array<{ type: string; payload: Record<string, unknown> }>
  closed: boolean
  options: {
    onStatusChange: (status: string) => void
    onMessage: (message: Message) => void
  }
  open: () => void
  receive: (message: Message) => void
}

vi.mock('../ws/socket', () => {
  class FakeLupusSocket implements FakeSocket {
    nickname: string | null = null
    sent: Array<{ type: string; payload: Record<string, unknown> }> = []
    closed = false
    options: FakeSocket['options']

    constructor(options: FakeSocket['options']) {
      this.options = options
      mocks.sockets.push(this)
    }

    connect(nickname: string) {
      this.nickname = nickname
    }

    send(type: string, payload: Record<string, unknown>) {
      this.sent.push({ type, payload })
      return true
    }

    close() {
      this.closed = true
    }

    open() {
      this.options.onStatusChange('open')
    }

    receive(message: Message) {
      this.options.onMessage(message)
    }
  }

  return { LupusSocket: FakeLupusSocket }
})

function Probe() {
  const session = useSession()

  return (
    <div>
      <span data-testid="sala">{session.roomCode ?? 'sin-sala'}</span>
      <span data-testid="jugador">{session.playerId ?? 'sin-jugador'}</span>
      <span data-testid="estado">{session.restoring ? 'recuperando' : 'lista'}</span>
    </div>
  )
}

function roomState(roomCode: string, playerId: string): Message {
  return {
    v: 1,
    type: 'room_state',
    id: 'm-1',
    at: 1,
    roomCode,
    playerId,
    players: [{ id: playerId, nickname: 'Ana', host: true, connected: true }],
  }
}

describe('SessionProvider (identidad persistente)', () => {
  beforeEach(() => {
    window.localStorage.clear()
    mocks.sockets.length = 0
  })

  it('al cargar recupera la sesión guardada con resume_room', () => {
    window.localStorage.setItem(
      'lupus.session',
      JSON.stringify({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' }),
    )

    render(
      <SessionProvider>
        <Probe />
      </SessionProvider>,
    )

    const socket = mocks.sockets[0]
    expect(socket.nickname).toBe('Ana')
    expect(screen.getByTestId('estado')).toHaveTextContent('recuperando')

    act(() => socket.open())

    expect(socket.sent[0]).toMatchObject({
      type: 'resume_room',
      payload: { roomCode: 'ABC234', playerId: 'p-1' },
    })
  })

  it('al recuperar el asiento guarda la sesión y deja de estar recuperando', () => {
    window.localStorage.setItem(
      'lupus.session',
      JSON.stringify({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' }),
    )

    render(
      <SessionProvider>
        <Probe />
      </SessionProvider>,
    )

    const socket = mocks.sockets[0]
    act(() => {
      socket.open()
      socket.receive(roomState('ABC234', 'p-1'))
    })

    expect(screen.getByTestId('sala')).toHaveTextContent('ABC234')
    expect(screen.getByTestId('estado')).toHaveTextContent('lista')
    expect(JSON.parse(window.localStorage.getItem('lupus.session') ?? '{}')).toMatchObject({
      roomCode: 'ABC234',
      playerId: 'p-1',
      nickname: 'Ana',
    })
  })

  it('si el asiento ya no existe borra la sesión y avisa', () => {
    window.localStorage.setItem(
      'lupus.session',
      JSON.stringify({ roomCode: 'ABC234', playerId: 'p-1', nickname: 'Ana' }),
    )

    render(
      <SessionProvider>
        <Probe />
      </SessionProvider>,
    )

    const socket = mocks.sockets[0]
    act(() => {
      socket.open()
      socket.receive({
        v: 1,
        type: 'error',
        id: 'e-1',
        at: 1,
        code: 'seat_not_found',
        message: 'Ese asiento ya no existe.',
      })
    })

    expect(window.localStorage.getItem('lupus.session')).toBeNull()
    expect(screen.getByTestId('sala')).toHaveTextContent('sin-sala')
  })

  it('sin sesión guardada no intenta recuperar nada', () => {
    render(
      <SessionProvider>
        <Probe />
      </SessionProvider>,
    )

    expect(mocks.sockets).toHaveLength(0)
    expect(screen.getByTestId('estado')).toHaveTextContent('lista')
  })
})
