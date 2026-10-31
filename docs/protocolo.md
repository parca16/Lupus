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

## Reglas

1. **El servidor es la autoridad.** El cliente nunca decide estado de partida; solo envía intenciones y renderiza lo que recibe.
2. **Los mensajes desconocidos se rechazan** con un `error`; no se ignoran en silencio.
3. **La versión es obligatoria.** Si `v` no coincide con la versión del servidor, se responde `unsupported_version`.
4. **Compatibilidad:** cualquier cambio incompatible sube `version` en `protocol.json` y se anota en este documento.

## Cómo se mantiene sincronizado

- `protocol/protocol.json` declara cada mensaje, sus campos y un ejemplo canónico.
- `server/.../protocol/ProtocolContractTest` comprueba que cada mensaje del contrato tiene una clase Java espejo y que el ejemplo se deserializa y vuelve a serializar sin cambios.
- `web/src/protocol/protocol.test.ts` comprueba lo mismo en el lado TypeScript.
- Añadir un mensaje solo en un lado hace fallar el test del otro lado: es la señal de que falta trabajo.
