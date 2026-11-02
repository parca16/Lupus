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
| `resume_room` | cliente → servidor | `roomCode`, `playerId` | Recupera un asiento con la identidad guardada. |
| `room_state` | servidor → cliente | `roomCode`, `playerId`, `players` | Respuesta a `create_room`/`join_room`. |
| `room_updated` | servidor → cliente | `roomCode`, `players` | La lista de jugadores ha cambiado. |
| `leave_room` | cliente → servidor | — | Salida voluntaria. Si sale el host en el lobby, la sala se cierra. |
| `kick_player` | cliente → servidor | `playerId` | El host expulsa a un jugador. |
| `room_closed` | servidor → cliente | `roomCode`, `reason` | La sala se ha cerrado; el jugador deja de pertenecer a ella. |
| `kicked` | servidor → cliente | `roomCode` | El host ha expulsado a este jugador. |
| `update_settings` | cliente → servidor | `wolves`, `roles`, `lives`, `president`, `debateSeconds`, `roleSeconds`, `voteSeconds`, `showVoteDetail` | El host cambia la configuración. |
| `room_settings` | servidor → cliente | `roomCode` + los mismos campos | Configuración vigente (al crear, al entrar y al cambiarla). |
| `start_game` | cliente → servidor | — | El host arranca la partida. |
| `game_starting` | servidor → cliente | `roomCode` | La partida empieza. |
| `game_state` | servidor → cliente | `roomCode`, `phase`, `round`, `deadline`, `nightStep`, `role`, `loverId`, `vision`, `witchVictim`, `pendingKnight`, `ready`, `statuses` | Estado personalizado: tu rol privado y el estado público. |
| `game_dawn` | servidor → cliente | `roomCode`, `died`, `lostLife`, `healed`, `quiet` | Parte del amanecer. |
| `game_over` | servidor → cliente | `roomCode`, `outcome` | Fin de la partida. |
| `game_action` | cliente → servidor | `action`, `kind`, `targetId`, `secondTargetId` | Acción de partida (voto, noche, listo…). |

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
| `not_host` | Solo el host puede hacer esa acción. |
| `invalid_settings` | La configuración no es válida (lobos, vidas, roles o tiempos). |
| `not_enough_players` | Hacen falta al menos 5 jugadores conectados para empezar. |
| `invalid_settings_for_players` | Con ese número de jugadores no caben las cartas elegidas. |
| `game_already_started` | La partida ya ha empezado y no admite jugadores nuevos. |
| `not_your_turn` | No es tu turno o no hay votación en curso. |
| `not_alive` | Los muertos no actúan ni votan. |
| `invalid_target` | Objetivo inválido para esa acción. |
| `already_acted` | Ya has actuado en esta ventana. |
| `guardian_limit` | El Ángel ya usó sus 3 protecciones. |
| `repeated_target` | No se puede repetir objetivo en noches consecutivas. |
| `no_potions_left` | La poción ya se ha usado. |
| `invalid_action` | Acción desconocida para esa fase. |
| `seat_not_found` | Ese asiento ya no existe en la sala. |
| `already_connected` | Ese jugador ya está conectado (por ejemplo, en otra pestaña). |

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

## Salir, expulsar y reconexión

- **Salir** (`leave_room`): el jugador desaparece de la lista y se difunde `room_updated`. Si quien
  sale es el host y la sala sigue en el lobby, la sala se cierra: el resto recibe `room_closed`
  con `reason: "host_left"`.
- **Expulsar** (`kick_player`): solo el host. El expulsado recibe `kicked` y el resto
  `room_updated`. Si alguien que no es host lo intenta, recibe `error` con `not_host`.
- **Desconexión**: no es lo mismo que salir. El jugador se queda con `connected: false` y la sala
  sigue abierta (también si era el host), para que pueda volver.
- **Reconexión**: al volver, el cliente reenvía `resume_room` con su `roomCode` y su `playerId`, y
  el servidor le devuelve **su mismo asiento**, marcándolo de nuevo como conectado. Los mensajes
  enviados mientras estaba caído se guardan en cola y se mandan al reconectar.
- **Identidad persistente**: el navegador guarda `{ roomCode, playerId, nickname }` en
  `localStorage`. Al recargar la página, el cliente reconecta y recupera el asiento solo. Si el
  asiento ya no existe (sala caducada) o ya está conectado en otra pestaña, se borra la sesión y
  se avisa con `seat_not_found`, `room_not_found` o `already_connected`.

## Configuración y arranque

La configuración la decide el host con `update_settings` y el servidor la difunde a todos como
`room_settings` (también al crear la sala y al entrar).

| Campo | Tipo | Rango / valores | Por defecto |
| ----- | ---- | --------------- | ----------- |
| `wolves` | number | 1–8 | 1 |
| `roles` | string[] | `seer`, `witch`, `knight`, `guardian`, `cupid` (sin repetir) | `[]` |
| `lives` | number | 0, 1 o 2 | 1 |
| `president` | boolean | — | `false` |
| `debateSeconds` | number | 30–600 | 120 |
| `roleSeconds` | number | 10–120 | 30 |
| `voteSeconds` | number | 15–180 | 45 |
| `showVoteDetail` | boolean | — | `true` |

Reglas de validación (issue #27):

- **Las cartas siempre suman el número de jugadores**: los aldeanos rellenan lo que falta.
- Debe quedar **al menos un aldeano**, así que `wolves + roles.size() < jugadores conectados`.
- Los roles especiales son **únicos** y de la lista conocida.
- Para empezar hacen falta **5 jugadores conectados** como mínimo.
- Cualquier configuración inválida devuelve `error` con `invalid_settings` y un mensaje claro.

Al empezar (`start_game`), el servidor valida la configuración con el número real de jugadores y
emite `game_starting` a todos. Solo el host puede cambiar la configuración o empezar la partida
(`not_host` en caso contrario).

## Caducidad de salas

- Una sala **sin actividad durante 30 minutos** se destruye (issue #25).
- Cuenta como actividad **cualquier mensaje de un jugador de la sala**, incluido `ping`; también
  crear, unirse, cambiar configuración o empezar.
- La limpieza corre cada minuto y, al caducar, avisa a los conectados con `room_closed` y
  `reason: "inactivity"`.
- Se puede ajustar en el servidor con `lupus.rooms.expiration-millis` (por defecto `1800000`) y
  `lupus.rooms.sweep-millis` (por defecto `60000`).

## Partida en curso

`start_game` crea la partida en el servidor y reparte los roles. A partir de ahí:

- Cada jugador recibe un **`game_state` personalizado** con la fase, la ronda, el plazo
  (`deadline` en milisegundos), su **rol privado**, su enamorado si lo tiene, la visión del Vidente,
  la víctima que ve la Bruja, el Caballero pendiente y el estado público de todos.
- Los cambios de fase los difunde el ticker del servidor. Los clientes no avanzan nada por su cuenta.
- Las acciones viajan en `game_action`:
  - `president_vote` y `vote`: `targetId` (vacío = abstención).
  - `ready`: botón «Listo para votar».
  - `knight_choice`: `targetId` del golpe.
  - `night_action` + `kind`: `seer`, `guardian`, `wolf_vote`, `witch_heal`, `witch_poison`,
    `witch_none` o `cupid` (con `targetId` y `secondTargetId`).
- Al amanecer se envía `game_dawn` con las muertes, vidas perdidas y curaciones.
- Al terminar se envía `game_over` con el bando ganador (`VILLAGE`, `WOLVES` o `LOVERS`).
- Con la partida en curso no se puede salir ni expulsar (`game_already_started`): solo desconectarse,
  que conserva el asiento.

## Cómo se mantiene sincronizado

- `protocol/protocol.json` declara cada mensaje, sus campos y un ejemplo canónico.
- `server/.../protocol/ProtocolContractTest` comprueba que cada mensaje del contrato tiene una clase Java espejo y que el ejemplo se deserializa y vuelve a serializar sin cambios.
- `web/src/protocol/protocol.test.ts` comprueba lo mismo en el lado TypeScript.
- Añadir un mensaje solo en un lado hace fallar el test del otro lado: es la señal de que falta trabajo.
