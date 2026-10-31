/**
 * Cliente WebSocket del juego, tipado contra `protocol/protocol.json`.
 *
 * Se ocupa solo del handshake y del envio/recepcion (issue #17). La reconexion automatica y la
 * cola de mensajes mientras el socket esta caido llegan en la issue #33.
 */

import type { Message, MessageType, PayloadOf } from '../protocol'
import { createMessage, parseMessage, serializeMessage } from '../protocol'

export type SocketStatus = 'idle' | 'connecting' | 'open' | 'closed'

export interface LupusSocketOptions {
  /** URL del endpoint WebSocket. Por defecto, `/ws` en el mismo host. */
  url?: string
  /** Inyectable para tests; por defecto usa el WebSocket del navegador. */
  createSocket?: (url: string) => WebSocket
  onMessage?: (message: Message) => void
  onStatusChange?: (status: SocketStatus) => void
}

export function defaultWebSocketUrl(): string {
  const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${scheme}//${window.location.host}/ws`
}

export class LupusSocket {
  private readonly url: string
  private readonly createSocket: (url: string) => WebSocket
  private readonly onMessage?: (message: Message) => void
  private readonly onStatusChange?: (status: SocketStatus) => void

  private socket: WebSocket | null = null
  private nickname = ''
  private currentStatus: SocketStatus = 'idle'
  private currentConnectionId: string | null = null

  constructor(options: LupusSocketOptions = {}) {
    this.url = options.url ?? defaultWebSocketUrl()
    this.createSocket = options.createSocket ?? ((url) => new WebSocket(url))
    this.onMessage = options.onMessage
    this.onStatusChange = options.onStatusChange
  }

  get status(): SocketStatus {
    return this.currentStatus
  }

  get connectionId(): string | null {
    return this.currentConnectionId
  }

  /** Abre la conexion y envia `hello` en cuanto el servidor acepta. */
  connect(nickname: string): void {
    if (this.socket) {
      return
    }

    this.nickname = nickname
    this.setStatus('connecting')

    const socket = this.createSocket(this.url)
    this.socket = socket

    socket.onopen = () => {
      this.setStatus('open')
      this.send('hello', { nickname: this.nickname })
    }

    socket.onmessage = (event: MessageEvent<string>) => {
      const message = parseMessage(event.data)
      if (!message) {
        return
      }
      if (message.type === 'welcome') {
        this.currentConnectionId = message.connectionId
      }
      this.onMessage?.(message)
    }

    socket.onclose = () => {
      this.socket = null
      this.setStatus('closed')
    }

    socket.onerror = () => {
      this.setStatus('closed')
    }
  }

  /** Envia un mensaje tipado. Devuelve `false` si el socket no esta abierto. */
  send<T extends MessageType>(type: T, payload: PayloadOf<T>): boolean {
    if (!this.socket || this.socket.readyState !== 1) {
      return false
    }
    this.socket.send(serializeMessage(createMessage(type, payload)))
    return true
  }

  sendEcho(text: string): boolean {
    return this.send('echo', { text })
  }

  close(): void {
    this.socket?.close()
    this.socket = null
    this.setStatus('closed')
  }

  private setStatus(status: SocketStatus): void {
    if (status === this.currentStatus) {
      return
    }
    this.currentStatus = status
    this.onStatusChange?.(status)
  }
}
