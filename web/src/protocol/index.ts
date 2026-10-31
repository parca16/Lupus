/**
 * Tipos espejo en TypeScript de `protocol/protocol.json`.
 *
 * La forma canónica vive en el JSON del repositorio; este archivo y sus tests se encargan de
 * que el cliente no se desincronice del servidor.
 */

export const PROTOCOL_VERSION = 1

export interface Envelope<TType extends string> {
  /** Versión del protocolo. */
  v: number
  /** Tipo de mensaje; es el discriminante. */
  type: TType
  /** UUID del mensaje, para correlacionar peticiones y respuestas. */
  id: string
  /** Marca de tiempo en milisegundos. */
  at: number
}

export interface HelloMessage extends Envelope<'hello'> {
  nickname: string
}

export interface WelcomeMessage extends Envelope<'welcome'> {
  connectionId: string
  nickname: string
  serverTime: number
}

export interface EchoMessage extends Envelope<'echo'> {
  text: string
}

export interface PingMessage extends Envelope<'ping'> {}

export interface PongMessage extends Envelope<'pong'> {}

export interface ErrorMessage extends Envelope<'error'> {
  code: string
  message: string
}

export type Message =
  | HelloMessage
  | WelcomeMessage
  | EchoMessage
  | PingMessage
  | PongMessage
  | ErrorMessage

export type MessageType = Message['type']

type FieldKind = 'string' | 'number'

/**
 * Campos extra de cada mensaje (sin el sobre). Es el espejo en tiempo de ejecución de
 * `protocol.json` y lo que valida `isMessage`.
 */
export const MESSAGE_FIELDS: Record<MessageType, Record<string, FieldKind>> = {
  hello: { nickname: 'string' },
  welcome: { connectionId: 'string', nickname: 'string', serverTime: 'number' },
  echo: { text: 'string' },
  ping: {},
  pong: {},
  error: { code: 'string', message: 'string' },
}

export const MESSAGE_TYPES = Object.keys(MESSAGE_FIELDS) as MessageType[]

export type PayloadOf<T extends MessageType> = Omit<
  Extract<Message, { type: T }>,
  keyof Envelope<MessageType>
>

export interface MessageOptions {
  id?: string
  at?: number
}

/** Construye un mensaje válido con el sobre ya relleno. */
export function createMessage<T extends MessageType>(
  type: T,
  payload: PayloadOf<T>,
  options: MessageOptions = {},
): Extract<Message, { type: T }> {
  return {
    v: PROTOCOL_VERSION,
    type,
    id: options.id ?? crypto.randomUUID(),
    at: options.at ?? Date.now(),
    ...payload,
  } as Extract<Message, { type: T }>
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/** Valida en tiempo de ejecución que un valor desconocido es un mensaje del protocolo. */
export function isMessage(value: unknown): value is Message {
  if (!isRecord(value)) {
    return false
  }
  if (value.v !== PROTOCOL_VERSION) {
    return false
  }
  if (typeof value.id !== 'string' || value.id.length === 0) {
    return false
  }
  if (typeof value.at !== 'number' || !Number.isFinite(value.at)) {
    return false
  }
  if (typeof value.type !== 'string') {
    return false
  }

  const fields = MESSAGE_FIELDS[value.type as MessageType]
  if (!fields) {
    return false
  }

  return Object.entries(fields).every(([name, kind]) => typeof value[name] === kind)
}

export function serializeMessage(message: Message): string {
  return JSON.stringify(message)
}

/** Parsea un mensaje recibido; devuelve `null` si no cumple el contrato. */
export function parseMessage(raw: string): Message | null {
  try {
    const parsed: unknown = JSON.parse(raw)
    return isMessage(parsed) ? parsed : null
  } catch {
    return null
  }
}
