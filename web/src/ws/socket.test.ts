import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Message } from '../protocol'
import { LupusSocket } from './socket'

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
    this.readyState = 3
    this.onclose?.()
  }

  serverOpen(): void {
    this.readyState = 1
    this.onopen?.()
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

function nuevoSocket(options: { onMessage?: (message: Message) => void } = {}) {
  const onStatusChange = vi.fn()
  const socket = new LupusSocket({
    url: 'ws://test/ws',
    createSocket: (url) => new FakeSocket(url) as unknown as WebSocket,
    onStatusChange,
    ...options,
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

describe('LupusSocket', () => {
  beforeEach(() => {
    FakeSocket.instances = []
  })

  it('abre la conexion y envia hello con el apodo al conectar', () => {
    const { socket, onStatusChange, fake } = nuevoSocket()

    socket.connect('Ana')

    expect(fake().url).toBe('ws://test/ws')
    expect(onStatusChange).toHaveBeenCalledWith('connecting')

    fake().serverOpen()

    expect(onStatusChange).toHaveBeenCalledWith('open')
    expect(fake().sent).toHaveLength(1)
    const hello = JSON.parse(fake().sent[0]) as Message
    expect(hello).toMatchObject({ v: 1, type: 'hello', nickname: 'Ana' })
    expect(hello.id).toEqual(expect.any(String))
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

  it('envia echo solo cuando el socket esta abierto', () => {
    const { socket, fake } = nuevoSocket()

    socket.connect('Ana')
    expect(socket.sendEcho('antes de abrir')).toBe(false)
    expect(fake().sent).toHaveLength(0)

    fake().serverOpen()
    expect(socket.sendEcho('hola')).toBe(true)

    const echo = JSON.parse(fake().sent.at(-1)!) as Message
    expect(echo).toMatchObject({ type: 'echo', text: 'hola' })
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

  it('no abre dos sockets a la vez y pasa a closed al cerrar', () => {
    const { socket, onStatusChange, fake } = nuevoSocket()

    socket.connect('Ana')
    socket.connect('Ana')
    fake().serverOpen()
    expect(FakeSocket.instances).toHaveLength(1)

    socket.close()
    expect(onStatusChange).toHaveBeenLastCalledWith('closed')
    expect(socket.status).toBe('closed')
  })
})
