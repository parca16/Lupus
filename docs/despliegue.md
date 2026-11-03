# Despliegue

La ruta gratuita recomendada es:

```text
Frontend React  → Vercel (Hobby, gratis)
Backend Java    → Render (Free, gratis)
WebSocket       → wss://<servicio>.onrender.com/ws
```

Vercel solo sirve el React compilado. La lógica del juego y el WebSocket viven en Render.

## Local con Docker

```bash
docker compose up --build
```

- Web: <http://localhost:8081>
- Servidor: <http://localhost:8080/actuator/health>

El contenedor `web` sirve el build de React con nginx y hace de proxy de `/ws` hacia `server`.
También se puede ejecutar solo el servidor (`docker compose up server`) para probar el backend.

## Backend en Render (gratis)

El repositorio incluye `render.yaml` en la raíz. Render lo detecta como Blueprint y crea el
servicio `lupus-server` con el `Dockerfile` de `server/`.

### Pasos

1. Crea una cuenta en <https://render.com>.
2. Entra en **New → Blueprint** y conecta el repositorio de GitHub.
3. Render lee `render.yaml`, crea el Web Service gratuito y lo despliega.
4. Copia la URL del servicio, por ejemplo:
   ```text
   https://lupus-server.onrender.com
   ```
5. Comprueba el health check:
   ```text
   https://lupus-server.onrender.com/actuator/health
   ```

Si prefieres crearlo a mano: **New → Web Service → Docker**, Root Directory `server`,
Dockerfile `./Dockerfile`, plan **Free**, health check `/actuator/health`.

> Si el nombre `lupus-server` ya está ocupado en tu cuenta, cambia `name` en `render.yaml`.
> Usa después la URL real del servicio en `VITE_WS_URL`.

### Plan Free de Render

- **0 €**.
- **750 horas de instancia al mes**.
- 0,1 CPU y 512 MB de RAM.
- HTTPS y dominio `onrender.com` incluidos.
- Soporte de WebSocket.
- Se duerme tras **15 minutos sin tráfico**.
- Los mensajes WebSocket cuentan como tráfico: mientras hay una partida activa no se duerme.
- Al despertar tarda alrededor de **1 minuto**.
- El estado del juego está en memoria: si Render reinicia el servicio, las partidas en curso se pierden.
- Es un plan para hobby/pruebas, no para producción seria.

## Frontend en Vercel (gratis)

El repositorio incluye `web/vercel.json` con las reglas de SPA para React Router.

### Pasos

1. Crea una cuenta en <https://vercel.com>.
2. **Add New → Project** e importa el repositorio.
3. Configura:
   - **Root Directory**: `web`.
   - **Framework Preset**: Vite.
   - **Build Command**: `npm run build`.
   - **Output Directory**: `dist`.
4. Añade la variable de entorno:
   ```env
   VITE_WS_URL=wss://lupus-server.onrender.com/ws
   ```
   (Sustituye la URL por la que te haya dado Render.)
5. Deploy.

Vercel te dará una URL tipo:

```text
https://lupus.vercel.app
```

### Plan Hobby de Vercel

- **0 €** para proyectos personales y no comerciales.
- HTTPS y CDN incluidos.
- Si el proyecto genera ingresos, Vercel exige el plan Pro.

## Variables de entorno

| Variable | Dónde | Por defecto | Para qué |
| --- | --- | --- | --- |
| `VITE_WS_URL` | Vercel (build) | vacío | URL absoluta del WebSocket. Si está vacía, el cliente usa `/ws` del mismo host. |
| `LUPUS_RATE_MAX_MESSAGES` | Render | `60` | Mensajes permitidos por conexión y ventana. |
| `LUPUS_RATE_WINDOW_MILLIS` | Render | `10000` | Tamaño de la ventana del rate limit. |
| `LUPUS_SERVER_URL` | docker-compose | `http://server:8080` | Upstream del proxy `/ws` de nginx en local. |

## Dominio propio y HTTPS/WSS

Tanto Render como Vercel dan HTTPS y WSS automáticamente en sus subdominios.

Para usar un dominio propio:

1. En Vercel: **Project → Settings → Domains → Add**, y crea el registro DNS que indique.
2. En Render: **Service → Settings → Custom Domains → Add**, y crea el registro DNS que indique.
3. Si cambia la URL del backend, actualiza `VITE_WS_URL` en Vercel y vuelve a desplegar.
4. El cliente detecta `https:` y usa `wss:` automáticamente.

## Logs

- Render: **Dashboard → servicio → Logs**. Ahí aparecen los rechazos por rate limit y los errores.
- Vercel: **Project → Deployments → Functions/Logs**.

## Alternativa: todo en Render

Render también ofrece Static Sites gratuitos. Si no quieres usar Vercel, se puede desplegar
`web/` como Static Site en Render con `npm run build` y `dist`, y añadir `VITE_WS_URL` igualmente.
