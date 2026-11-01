# Protocolo de mensajes

Este documento describe los mensajes que intercambian el servidor y el cliente por WebSocket.

La **fuente única de verdad** es [`protocol/protocol.json`](../protocol/protocol.json): los tipos Java y TypeScript son espejo de ese archivo y hay tests en ambos lados que fallan si algo se desincroniza. Si quieres cambiar el protocolo, cambia primero el JSON, luego los tipos y, por último, la documentación.

## Sobre

Todos los mensajes comparten el mismo sobre (*envelope*):

| Campo  | Tipo   | Descripción |
| ------ | ------ | ----------- |
| `v`    | number | Versión del protocolo. Actualmente `1`. |
| `type` | string | Tipo de mensaje (ver tabla). |
| `id`   | string | UUID del mensaje, para correlacionar petición y respuesta. |
| `at`   | number | Marca de tiempo en milisegundos desde la época Unix. |

Los campos propios de cada mensaje viajan **en el mismo nivel** que el sobre, no anidados en un `payload`. Así el cliente puede discrimin
ar por `type` con un simple `switch` y ambos lados comparten una forma plana y fácil de validar.

Ejemplo:

```json
{
  "v": 1,
  "type": "hello",
  "id": "00000000-0000-0000-0000-000000000001",
  "at": 1700000000000,
  "nickname": "Ana"
}
```

## Mensajes

| Tipo | Dirección | Campos extra | Propósito |
| ---- | --------- | ------------ | --------- |
| `hello` | cliente → servidor | `nickname` | Presentación del jugador tras conectar. |
| `welcome` | servidor → cliente | `connectionId`, `nickname`, `serverTime` | Handshake: el servidor confirma la conexión y asigna identificador. |
| `echo` | ambos | `text` | Mensaje de prueba que el servidor devuelve tal cual (issue #17). |
| `ping` | cliente → servidor | — | Comprobación de vida. |
| `pong` | servidor → cliente | — | Respuesta a `ping`. |
| `error` | servidor → cliente | `code`, `message` | El servidor rechaza un mensaje y explica por qué. |
| `create_room` | cliente → servidor | `nickname` | Crea una sala privada; el creador es host. |
| `join_room` | cliente → servidor | `roomCode`, `nickname` | Entra en una sala por su código. |
| `room_state` | servidor → cliente | `roomCode`, `playerId`, `players` | Respuesta a `create_room`/`join_room`. |
| `room_updated` | servidor → cliente | `roomCode`, `players` | La lista de jugadores ha cambiado. |

El objeto `Player` que viaja dentro de `players` tiene esta forma:

| Campo | Tipo | Descripción |
| ----- | ---- | ----------- |
| `id` | string | Identificador del jugador dentro de la sala. |
| `nickname` | string | Apodo visible (3-16 caracteres, único en la sala). |
| `host` | boolean | `true` solo para quien creó la sala. |
| `connected` | boolean | `false` mientras esté desconectado. |

### `hello`

```json
{ "v": 1, "type": "hello", "id": "…", "at": 1700000000000, "nickname": "Ana" }
```

### `welcome`

```json
{
  "v": 1,
  "type": "welcome",
  "id": "…",
  "at": 1700000000100,
  "connectionId": "00000000-0000-0000-0000-0000000000aa",
  "nickname": "Ana",
  "serverTime": 1700000000100
}
```

### `echo`

```json
{ "v": 1, "type": "echo", "id": "…", "at": 1700000000200, "text": "hola" }
```

### `ping` / `pong`

```json
{ "v": 1, "type": "ping", "id": "…", "at": 1700000000300 }
```

```json
{ "v": 1, "type": "pong", "id": "…", "at": 1700000000310 }
```

### Salas

```json
{ "v": 1, "type": "create_room", "id": "…", "at": 1700000000500, "nickname": "Ana" }
```

```json
{
  "v": 1,
  "type": "join_room",
  "id": "…",
  "at": 1700000000600,
  "roomCode": "ABC234",
  "nickname": "Luis"
}
```

```json
{
  "v": 1,
  "type": "room_state",
  "id": "…",
  "at": 1700000000700,
  "roomCode": "ABC234",
  "playerId": "…",
  "players": [
    { "id": "…", "nickname": "Ana", "host": true, "connected": true }
  ]
}
```

```json
{
  "v": 1,
  "type": "room_updated",
  "id": "…",
  "at": 1700000000800,
  "roomCode": "ABC234",
  "players": [
    { "id": "…", "nickname": "Ana", "host": true, "connected": true },
    { "id": "…", "nickname": "Luis", "host": false, "connected": true }
  ]
}
```

Reglas de las salas:

- El código tiene **6 caracteres** y se genera en el servidor (sin caracteres ambiguos como `O`, `0`, `I`, `1`).
- El primer jugador es **host**.
- Aforo: **mínimo 5** para empezar, **máximo 16** para entrar.
- El **nickname** tiene entre 3 y 16 caracteres y es único dentro de la sala (sin distinguir mayúsculas).
- Al desconectarse, el jugador se queda en la lista con `connected: false` y se difunde `room_updated`.

### `error`

```json
{
  "v": 1,
  "type": "error",
  "id": "…",
  "at": 1700000000400,
  "code": "invalid_message",
  "message": "El mensaje no cumple el contrato del protocolo."
}
```

Códigos de error previstos:

| Código | Significado |
| ------ | ----------- |
| `invalid_message` | El JSON no cumple el contrato. |
| `unknown_type` | El `type` no existe en el protocolo. |
| `unsupported_version` | El campo `v` no es la versión soportada. |
| `invalid_nickname` | El apodo no tiene entre 3 y 16 caracteres válidos. |
| `room_not_found` | No existe ninguna sala con ese código. |
| `room_full` | La sala ya tiene 16 jugadores. |
| `nickname_taken` | Ese apodo ya está en uso en la sala. |
| `not_in_room` | El jugador no pertenece a la sala. |

## Reglas

1. **El servidor es la autoridad.** El cliente nunca decide estado de partida; solo envía intenciones y renderiza lo que recibe.
2. **Los mensajes desconocidos se rechazan** con un `error`; no se ignoran en silencio.
3. **La versión es obligatoria.** Si `v` no coincide con la versión del servidor, se responde `unsupported_version`.
4. **Compatibilidad:** cualquier cambio incompatible sube `version` en `protocol.json` y se anota en este documento.

## Handshake

1. El cliente abre `wss://<host>/ws`.
2. El servidor responde con `welcome` (con `connectionId` y `nickname` vacio).
3. El cliente envia `hello` con su apodo.
4. El servidor repite `welcome` con el mismo `connectionId` y ya con el apodo.
5. A partir de ahi el canal esta listo (`echo`, `ping`/`pong`).

El `connectionId` lo asigna siempre el servidor y no cambia durante la vida de la conexion. Es la
base para reconectar en la issue #26 y para la cola de mensajes de la issue #33.

## Cómo se mantiene sincronizado

- `protocol/protocol.json` declara cada mensaje, sus campos y un ejemplo canónico.
- `server/.../protocol/ProtocolContractTest` comprueba que cada mensaje del contrato tiene una clase Java espejo y que el ejemplo se deserializa y vuelve a serializar sin cambios.
- `web/src/protocol/protocol.test.ts` comprueba lo mismo en el lado TypeScript.
- Añadir un mensaje solo en un lado hace fallar el test del otro lado: es la señal de que falta trabajo.
