import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Message } from '../protocol'
import { LupusSocket } from './socket'
import type { LupusSocketOptions } from './socket'

/** WebSocket falso: registra lo enviado y permite simular eventos del servidor. */
class FakeSocket {
  static instances: FakeSocket[] = []

  readonly url: string
  readyState = 0
  sent: string[] = []
  onopen: (() => void) | null = null
  onmessage: ((event: MessageEvent<string>) => void) | null = null
  onclose: (() => void) | null = null
  onerror: (() => void) | null = null

  constructor(url: string) {
    this.url = url
    FakeSocket.instances.push(this)
  }

  send(data: string): void {
    this.sent.push(data)
  }

  close(): void {
    this.serverClose()
  }

  serverOpen(): void {
    this.readyState = 1
    this.onopen?.()
  }

  serverClose(): void {
    this.readyState = 3
    this.onclose?.()
  }

  serverSend(message: Message | string): void {
    const data = typeof message === 'string' ? message : JSON.stringify(message)
    this.onmessage?.({ data } as MessageEvent<string>)
  }

  static get ultimo(): FakeSocket {
    const socket = FakeSocket.instances.at(-1)
    if (!socket) {
      throw new Error('No se creo ningun socket')
    }
    return socket
  }
}

function nuevoSocket(options: Partial<LupusSocketOptions> = {}) {
  const onStatusChange = vi.fn()
  const socket = new LupusSocket({
    url: 'ws://test/ws',
    createSocket: (url) => new FakeSocket(url) as unknown as WebSocket,
    reconnectBaseDelayMs: 100,
    ...options,
    onStatusChange,
  })
  return { socket, onStatusChange, fake: () => FakeSocket.ultimo }
}

function welcome(connectionId = 'conn-1', nickname = 'Ana'): Message {
  return {
    v: 1,
    type: 'welcome',
    id: 'w-1',
    at: 1,
    connectionId,
    nickname,
    serverTime: 1,
  }
}

function parseSent(raw: string): Message {
  return JSON.parse(raw) as Message
}

describe('LupusSocket', () => {
  beforeEach(() => {
    FakeSocket.instances = []
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('abre la conexion y envia hello con el apodo al conectar', () => {
    const { socket, onStatusChange, fake } = nuevoSocket()

    socket.connect('Ana')

    expect(fake().url).toBe('ws://test/ws')
    expect(onStatusChange).toHaveBeenCalledWith('connecting')

    fake().serverOpen()

    expect(onStatusChange).toHaveBeenCalledWith('open')
    expect(fake().sent).toHaveLength(1)
    expect(parseSent(fake().sent[0])).toMatchObject({ v: 1, type: 'hello', nickname: 'Ana' })
  })

  it('guarda el identificador de conexion y avisa de los mensajes recibidos', () => {
    const recibidos: Message[] = []
    const { socket, fake } = nuevoSocket({ onMessage: (message) => recibidos.push(message) })

    socket.connect('Ana')
    fake().serverOpen()
    fake().serverSend(welcome('conn-42'))

    expect(socket.connectionId).toBe('conn-42')
    expect(recibidos).toHaveLength(1)
    expect(recibidos[0]).toMatchObject({ type: 'welcome', connectionId: 'conn-42' })
  })

  it('envia echo cuando el socket esta abierto', () => {
    const { socket, fake } = nuevoSocket()

    socket.connect('Ana')
    fake().serverOpen()
    expect(socket.sendEcho('hola')).toBe(true)

    expect(parseSent(fake().sent.at(-1)!)).toMatchObject({ type: 'echo', text: 'hola' })
  })

  it('encola los mensajes enviados antes de abrir y los manda al conectar', () => {
    const { socket, fake } = nuevoSocket()

    socket.connect('Ana')
    expect(socket.sendEcho('hola')).toBe(true)
    expect(socket.pendingMessages).toBe(1)

    fake().serverOpen()

    const tipos = fake().sent.map((raw) => parseSent(raw).type)
    expect(tipos).toEqual(['hello', 'echo'])
    expect(socket.pendingMessages).toBe(0)
  })

  it('reconecta solo y reenvia hello y la cola pendiente', () => {
    vi.useFakeTimers()
    const { socket, onStatusChange } = nuevoSocket()

    socket.connect('Ana')
    FakeSocket.ultimo.serverOpen()
    FakeSocket.ultimo.serverClose()

    expect(onStatusChange).toHaveBeenCalledWith('reconnecting')
    expect(socket.pendingMessages).toBe(0)

    expect(socket.sendEcho('mientras estaba caido')).toBe(true)
    expect(socket.pendingMessages).toBe(1)

    vi.advanceTimersByTime(100)

    const reconectado = FakeSocket.ultimo
    expect(reconectado).not.toBe(FakeSocket.instances[0])
    reconectado.serverOpen()

    const tipos = reconectado.sent.map((raw) => parseSent(raw).type)
    expect(tipos).toEqual(['hello', 'echo'])
    expect(socket.status).toBe('open')
  })

  it('deja de reintentar tras agotar los intentos', () => {
    vi.useFakeTimers()
    const { socket, onStatusChange } = nuevoSocket({ maxReconnectAttempts: 1 })

    socket.connect('Ana')
    FakeSocket.ultimo.serverOpen()
    FakeSocket.ultimo.serverClose()
    vi.advanceTimersByTime(100)
    FakeSocket.ultimo.serverClose()

    expect(socket.status).toBe('closed')
    expect(onStatusChange).toHaveBeenLastCalledWith('closed')
  })

  it('al cerrar a mano no reconecta y descarta la cola', () => {
    vi.useFakeTimers()
    const { socket, onStatusChange } = nuevoSocket()

    socket.connect('Ana')
    FakeSocket.ultimo.serverOpen()
    FakeSocket.ultimo.serverClose()
    expect(socket.sendEcho('pendiente')).toBe(true)

    socket.close()

    expect(socket.pendingMessages).toBe(0)
    expect(socket.status).toBe('closed')
    expect(onStatusChange).toHaveBeenLastCalledWith('closed')

    vi.advanceTimersByTime(5000)
    expect(FakeSocket.instances).toHaveLength(1)
  })

  it('no abre dos sockets a la vez', () => {
    const { socket, fake } = nuevoSocket()

    socket.connect('Ana')
    socket.connect('Ana')
    fake().serverOpen()

    expect(FakeSocket.instances).toHaveLength(1)
  })

  it('ignora mensajes que no cumplen el contrato', () => {
    const onMessage = vi.fn()
    const { socket, fake } = nuevoSocket({ onMessage })

    socket.connect('Ana')
    fake().serverOpen()
    fake().serverSend('no es json')
    fake().serverSend({ v: 99, type: 'welcome', id: 'x', at: 1 } as unknown as Message)

    expect(onMessage).not.toHaveBeenCalled()
  })
})
