# Despliegue

## Local con Docker

```bash
docker compose up --build
```

- Web: <http://localhost:8081>
- Servidor: <http://localhost:8080/actuator/health>

El contenedor `web` sirve el build de React con nginx y hace de proxy de `/ws` hacia `server`.
El navegador usa el mismo origen para el WebSocket, así que también funciona detrás de HTTPS/WSS.

## Variables de entorno

| Variable | Dónde | Por defecto | Para qué |
| --- | --- | --- | --- |
| `LUPUS_SERVER_URL` | web/nginx | `http://server:8080` | Upstream del proxy `/ws` y `/actuator`. |
| `VITE_WS_URL` | build de web | vacío | Fuerza una URL absoluta de WebSocket si la web se sirve en otro dominio. |
| `LUPUS_RATE_MAX_MESSAGES` | servidor | `60` | Mensajes permitidos por conexión y ventana. |
| `LUPUS_RATE_WINDOW_MILLIS` | servidor | `10000` | Tamaño de la ventana del rate limit. |

## Fly.io

Hay dos apps: `lupus-server` (Spring Boot) y `lupus-web` (nginx + React).

```bash
# Servidor
cd server
fly launch --no-deploy --copy-config
fly deploy

# Web (usa el DNS interno de la app del servidor)
cd ../web
fly launch --no-deploy --copy-config
fly deploy
```

La web queda en `https://lupus-web.fly.dev` y el WebSocket viaja por el mismo origen:
`wss://lupus-web.fly.dev/ws` → nginx → `http://lupus-server.internal:8080/ws`.

## Dominio, HTTPS y WSS (#73)

1. Añade el dominio a la app web:
   ```bash
   cd web
   fly certs add lupus.example.com
   ```
2. Crea en tu DNS el registro que indique Fly (`A`/`AAAA` o `CNAME`).
3. `force_https = true` ya redirige HTTP a HTTPS.
4. El cliente detecta `https:` y usa `wss:` automáticamente (`defaultWebSocketUrl`).
   Si la web se sirve en un host distinto al servidor, compila con:
   ```bash
   VITE_WS_URL=wss://lupus-server.fly.dev/ws npm run build
   ```

## Logs y protección

- El servidor registra en el log los rechazos por rate limit.
- Cada conexión tiene un contador independiente (`lupus.rate.*`).
- Los mensajes se validan contra `protocol/protocol.json` antes de despacharse.
- En Fly: `fly logs -a lupus-server` y `fly logs -a lupus-web`.
