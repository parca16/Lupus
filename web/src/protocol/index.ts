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

/** Jugador tal y como viaja por el cable. */
export interface Player {
  id: string
  nickname: string
  host: boolean
  connected: boolean
}

export interface CreateRoomMessage extends Envelope<'create_room'> {
  nickname: string
}

export interface JoinRoomMessage extends Envelope<'join_room'> {
  roomCode: string
  nickname: string
}

export interface RoomStateMessage extends Envelope<'room_state'> {
  roomCode: string
  playerId: string
  players: Player[]
}

export interface ResumeRoomMessage extends Envelope<'resume_room'> {
  roomCode: string
  playerId: string
}

export interface RoomUpdatedMessage extends Envelope<'room_updated'> {
  roomCode: string
  players: Player[]
}

export interface LeaveRoomMessage extends Envelope<'leave_room'> {}

export interface KickPlayerMessage extends Envelope<'kick_player'> {
  playerId: string
}

export interface RoomClosedMessage extends Envelope<'room_closed'> {
  roomCode: string
  reason: string
}

export interface KickedMessage extends Envelope<'kicked'> {
  roomCode: string
}

/** Roles especiales que el host puede activar. */
export const ROLES = ['seer', 'witch', 'knight', 'guardian', 'cupid'] as const

export type Role = (typeof ROLES)[number]

export interface GameSettings {
  wolves: number
  roles: Role[]
  lives: number
  president: boolean
  debateSeconds: number
  roleSeconds: number
  voteSeconds: number
  showVoteDetail: boolean
}

export interface UpdateSettingsMessage extends Envelope<'update_settings'>, GameSettings {}

export interface RoomSettingsMessage extends Envelope<'room_settings'>, GameSettings {
  roomCode: string
}

export interface StartGameMessage extends Envelope<'start_game'> {}

export interface GameStartingMessage extends Envelope<'game_starting'> {
  roomCode: string
}

export type Message =
  | HelloMessage
  | WelcomeMessage
  | EchoMessage
  | PingMessage
  | PongMessage
  | ErrorMessage
  | CreateRoomMessage
  | JoinRoomMessage
  | ResumeRoomMessage
  | RoomStateMessage
  | RoomUpdatedMessage
  | LeaveRoomMessage
  | KickPlayerMessage
  | RoomClosedMessage
  | KickedMessage
  | UpdateSettingsMessage
  | RoomSettingsMessage
  | StartGameMessage
  | GameStartingMessage

export type MessageType = Message['type']

type FieldKind = 'string' | 'number' | 'boolean' | 'array'

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
  create_room: { nickname: 'string' },
  join_room: { roomCode: 'string', nickname: 'string' },
  resume_room: { roomCode: 'string', playerId: 'string' },
  room_state: { roomCode: 'string', playerId: 'string', players: 'array' },
  room_updated: { roomCode: 'string', players: 'array' },
  leave_room: {},
  kick_player: { playerId: 'string' },
  room_closed: { roomCode: 'string', reason: 'string' },
  kicked: { roomCode: 'string' },
  update_settings: {
    wolves: 'number',
    roles: 'array',
    lives: 'number',
    president: 'boolean',
    debateSeconds: 'number',
    roleSeconds: 'number',
    voteSeconds: 'number',
    showVoteDetail: 'boolean',
  },
  room_settings: {
    roomCode: 'string',
    wolves: 'number',
    roles: 'array',
    lives: 'number',
    president: 'boolean',
    debateSeconds: 'number',
    roleSeconds: 'number',
    voteSeconds: 'number',
    showVoteDetail: 'boolean',
  },
  start_game: {},
  game_starting: { roomCode: 'string' },
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

export function isPlayer(value: unknown): value is Player {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.nickname === 'string' &&
    typeof value.host === 'boolean' &&
    typeof value.connected === 'boolean'
  )
}

function matchesKind(value: unknown, kind: FieldKind): boolean {
  if (kind === 'array') {
    return Array.isArray(value)
  }
  return typeof value === kind
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

  if (!Object.entries(fields).every(([name, kind]) => matchesKind(value[name], kind))) {
    return false
  }

  if ('players' in value && !(value.players as unknown[]).every(isPlayer)) {
    return false
  }

  if ('roles' in value) {
    const roles = value.roles as unknown[]
    if (!roles.every((role) => typeof role === 'string' && (ROLES as readonly string[]).includes(role))) {
      return false
    }
  }

  return true
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
