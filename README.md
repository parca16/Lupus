# Lupus 🐺

Aplicación para jugar **al juego de los lobos** (*el pueblo duerme*) con amigos, a distancia, **desde el navegador** (PC y móvil). Sin instalaciones: entras por un enlace, compartes un código de sala y a jugar.

## Stack

| Capa | Tecnología |
| --- | --- |
| Servidor | Java 21 · Spring Boot 4 · WebSocket |
| Cliente | React + TypeScript + Vite |
| Estilo | Tailwind CSS |
| Build servidor | Gradle (wrapper) |
| CI/CD | GitHub Actions |
| Persistencia | En memoria (MVP) |
| Despliegue | Docker + Fly.io (previsto) |

El juego es **server-authoritative**: el servidor conoce y decide todo (roles, fases, muertes); el cliente solo renderiza. Esto evita trampas y es lo que hace que el juego funcione de verdad.

## Estructura del repositorio

```
.
├── server/   Backend Java + Spring Boot (Gradle)
├── web/      Cliente React + TypeScript (Vite)
├── docs/     Reglamento, protocolo y glosario
├── assets/   Ilustraciones de cartas y sonidos
└── .github/  CI y plantillas de issues
```

## Requisitos

- **JDK 21** (Gradle lo descargará automáticamente si no lo tienes, gracias al *toolchain resolver*).
- **Node.js 22+** con npm.

No necesitas instalar Gradle: el proyecto incluye el **wrapper** (`./gradlew`).

## Cómo arrancar

### Servidor

```bash
cd server
./gradlew bootRun          # Windows: gradlew.bat bootRun
```

Comprueba que está vivo en <http://localhost:8080/actuator/health>.

### Cliente

```bash
cd web
npm install
npm run dev
```

Abre la URL que imprime Vite (normalmente <http://localhost:5173>).

## Roadmap

El trabajo está planificado en **milestones** e **issues**:

- Versiones: `v0.1 Fundaciones` → `v0.2 Salas y lobby` → `v0.3 Motor` → `v0.4 Roles` → `v0.5 Partida jugable` → `v0.6 Despliegue` → `v0.7 Pulido` → `v1.0 Narrador activo`.
- Consulta los [issues abiertos](../../issues) y la [épica 1](../../issues/1) para empezar.

## Licencia

Pendiente de definir.
