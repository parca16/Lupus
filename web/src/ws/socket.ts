/**
 * Cliente WebSocket del juego, tipado contra `protocol/protocol.json`.
 *
 * Incluye reconexion automatica con backoff y cola de mensajes mientras el socket esta caido
 * (issue #33). La identidad persistente entre recargas queda para la issue #26.
 */

import type { Message, MessageType, PayloadOf } from '../protocol'
import { createMessage, parseMessage, serializeMessage } from '../protocol'

export type SocketStatus = 'idle' | 'connecting' | 'reconnecting' | 'open' | 'closed'

export interface LupusSocketOptions {
  /** URL del endpoint WebSocket. Por defecto, `/ws` en el mismo host. */
  url?: string
  /** Inyectable para tests; por defecto usa el WebSocket del navegador. */
  createSocket?: (url: string) => WebSocket
  onMessage?: (message: Message) => void
  onStatusChange?: (status: SocketStatus) => void
  /** Reintenta la conexion automaticamente si se cae. Por defecto, `true`. */
  autoReconnect?: boolean
  /** Intentos maximos antes de rendirse. Por defecto, 8. */
  maxReconnectAttempts?: number
  /** Espera inicial entre intentos; luego crece exponencialmente. Por defecto, 500 ms. */
  reconnectBaseDelayMs?: number
  /** Tope de espera entre intentos. Por defecto, 10 s. */
  maxReconnectDelayMs?: number
  /** Cuantos mensajes se guardan mientras esta caido. Por defecto, 50. */
  maxQueueSize?: number
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
  private readonly autoReconnect: boolean
  private readonly maxReconnectAttempts: number
  private readonly reconnectBaseDelayMs: number
  private readonly maxReconnectDelayMs: number
  private readonly maxQueueSize: number

  private socket: WebSocket | null = null
  private nickname = ''
  private currentStatus: SocketStatus = 'idle'
  private currentConnectionId: string | null = null

  private queue: Message[] = []
  private reconnectAttempts = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private cerradoAMano = false

  constructor(options: LupusSocketOptions = {}) {
    this.url = options.url ?? defaultWebSocketUrl()
    this.createSocket = options.createSocket ?? ((url) => new WebSocket(url))
    this.onMessage = options.onMessage
    this.onStatusChange = options.onStatusChange
    this.autoReconnect = options.autoReconnect ?? true
    this.maxReconnectAttempts = options.maxReconnectAttempts ?? 8
    this.reconnectBaseDelayMs = options.reconnectBaseDelayMs ?? 500
    this.maxReconnectDelayMs = options.maxReconnectDelayMs ?? 10_000
    this.maxQueueSize = options.maxQueueSize ?? 50
  }

  get status(): SocketStatus {
    return this.currentStatus
  }

  get connectionId(): string | null {
    return this.currentConnectionId
  }

  /** Mensajes esperando a que vuelva la conexion. */
  get pendingMessages(): number {
    return this.queue.length
  }

  /** Abre la conexion y envia `hello` en cuanto el servidor acepta. */
  connect(nickname: string): void {
    if (this.socket) {
      return
    }

    this.nickname = nickname
    this.cerradoAMano = false
    this.reconnectAttempts = 0
    this.abrir()
  }

  /**
   * Envia un mensaje tipado. Si el socket esta caido, lo deja en cola y devuelve `true`.
   * Devuelve `false` solo si la cola esta llena.
   */
  send<T extends MessageType>(type: T, payload: PayloadOf<T>): boolean {
    const message = createMessage(type, payload)

    if (this.socket?.readyState === 1) {
      this.socket.send(serializeMessage(message))
      return true
    }

    if (this.queue.length >= this.maxQueueSize) {
      return false
    }

    this.queue.push(message)
    return true
  }

  sendEcho(text: string): boolean {
    return this.send('echo', { text })
  }

  /** Cierra la conexion de forma definitiva: no reconecta y descarta la cola. */
  close(): void {
    this.cerradoAMano = true
    this.cancelarReintento()
    this.queue = []
    this.socket?.close()
    this.socket = null
    this.setStatus('closed')
  }

  private abrir(): void {
    this.setStatus(this.reconnectAttempts > 0 ? 'reconnecting' : 'connecting')

    const socket = this.createSocket(this.url)
    this.socket = socket

    socket.onopen = () => {
      this.reconnectAttempts = 0
      this.setStatus('open')
      this.enviarAhora(createMessage('hello', { nickname: this.nickname }))
      this.vaciarCola()
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
      if (this.cerradoAMano) {
        this.setStatus('closed')
        return
      }
      this.programarReintento()
    }
  }

  private programarReintento(): void {
    if (!this.autoReconnect || this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.setStatus('closed')
      return
    }

    const delay = Math.min(
      this.maxReconnectDelayMs,
      this.reconnectBaseDelayMs * 2 ** this.reconnectAttempts,
    )
    this.reconnectAttempts += 1
    this.setStatus('reconnecting')

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.abrir()
    }, delay)
  }

  private cancelarReintento(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private vaciarCola(): void {
    const pendientes = this.queue
    this.queue = []
    for (const message of pendientes) {
      this.enviarAhora(message)
    }
  }

  private enviarAhora(message: Message): void {
    if (this.socket?.readyState === 1) {
      this.socket.send(serializeMessage(message))
    }
  }

  private setStatus(status: SocketStatus): void {
    if (status === this.currentStatus) {
      return
    }
    this.currentStatus = status
    this.onStatusChange?.(status)
  }
}
