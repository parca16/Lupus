import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  MESSAGE_FIELDS,
  PROTOCOL_VERSION,
  createMessage,
  isMessage,
  parseMessage,
  serializeMessage,
} from './index'

interface ContractMessage {
  direction: 'c2s' | 's2c' | 'both'
  fields: Record<string, string>
  example: Record<string, unknown>
}

interface Contract {
  version: number
  envelope: Record<string, string>
  messages: Record<string, ContractMessage>
}

const contractPath = resolve(process.cwd(), '..', 'protocol', 'protocol.json')
const contract = JSON.parse(readFileSync(contractPath, 'utf8')) as Contract

describe('protocolo (contrato compartido)', () => {
  it('declara en TypeScript los mismos mensajes que protocol.json', () => {
    expect(Object.keys(MESSAGE_FIELDS).sort()).toEqual(Object.keys(contract.messages).sort())
  })

  it('usa la misma versión que protocol.json', () => {
    expect(PROTOCOL_VERSION).toBe(contract.version)
  })

  it('declara los mismos campos extra que protocol.json', () => {
    for (const [type, definition] of Object.entries(contract.messages)) {
      expect(MESSAGE_FIELDS[type as keyof typeof MESSAGE_FIELDS], `campos de ${type}`).toEqual(
        definition.fields,
      )
    }
  })

  it('acepta todos los ejemplos del contrato', () => {
    for (const [type, definition] of Object.entries(contract.messages)) {
      expect(isMessage(definition.example), `ejemplo de ${type}`).toBe(true)
    }
  })

  it('rechaza un ejemplo al que le falta un campo obligatorio', () => {
    for (const [type, definition] of Object.entries(contract.messages)) {
      for (const field of Object.keys(definition.fields)) {
        const example = { ...definition.example }
        delete example[field]
        expect(isMessage(example), `${type} sin ${field}`).toBe(false)
      }
    }
  })

  it('rechaza tipos desconocidos y versiones no soportadas', () => {
    expect(isMessage({ v: 1, type: 'no-existe', id: 'x', at: 1 })).toBe(false)
    expect(isMessage({ v: 99, type: 'ping', id: 'x', at: 1 })).toBe(false)
    expect(isMessage(null)).toBe(false)
    expect(isMessage('hola')).toBe(false)
  })

  it('valida la forma de cada jugador de la lista', () => {
    const ejemplo = contract.messages.room_state.example

    expect(isMessage({ ...ejemplo, players: [] })).toBe(true)
    expect(
      isMessage({
        ...ejemplo,
        players: [{ id: 'x', nickname: 'Ana', host: 'si', connected: true }],
      }),
    ).toBe(false)
  })

  it('rechaza roles especiales desconocidos', () => {
    const base = contract.messages.update_settings.example

    expect(isMessage({ ...base, roles: ['seer'] })).toBe(true)
    expect(isMessage({ ...base, roles: ['inventor'] })).toBe(false)
    expect(isMessage({ ...base, roles: 'seer' })).toBe(false)
  })

  it('crea mensajes válidos y los recupera tras serializar', () => {
    const echo = createMessage('echo', { text: 'hola' }, { id: 'id-1', at: 42 })

    expect(isMessage(echo)).toBe(true)
    expect(parseMessage(serializeMessage(echo))).toEqual(echo)
  })

  it('devuelve null al parsear algo que no es del protocolo', () => {
    expect(parseMessage('no es json')).toBeNull()
    expect(parseMessage('{"v":1,"type":"ping","id":"","at":1}')).toBeNull()
  })
})
