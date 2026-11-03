import { useNavigate } from 'react-router-dom'
import { GAME_ROLE_LABELS } from '../game/labels'
import { useSession } from '../session/useSession'
import { Button } from './ui/Button'
import { Card } from './ui/Card'

const OUTCOMES: Record<string, { icon: string; title: string; subtitle: string }> = {
  VILLAGE: {
    icon: '🌅',
    title: 'Gana el pueblo',
    subtitle: 'Los lobos han caído. El pueblo amanece a salvo.',
  },
  WOLVES: {
    icon: '🐺',
    title: 'Ganan los lobos',
    subtitle: 'El pueblo no ha podido detener a la manada.',
  },
  LOVERS: {
    icon: '❤️',
    title: 'Ganan los enamorados',
    subtitle: 'El amor ha podido con lobos y aldeanos.',
  },
}

/** Pantalla final (#60): bando ganador, roles revelados y vuelta a la configuración. */
export function GameOverPanel() {
  const { gameOver, game, players, playerId, returnToLobby, leave } = useSession()
  const navigate = useNavigate()

  if (!gameOver) {
    return null
  }

  const resultado = OUTCOMES[gameOver.outcome] ?? {
    icon: '🏁',
    title: 'Fin de la partida',
    subtitle: `Resultado: ${gameOver.outcome}`,
  }
  const soyHost = players.some((player) => player.id === playerId && player.host)

  const salir = () => {
    leave()
    navigate('/')
  }

  return (
    <div className="flex flex-col gap-6">
      <Card className="border-blood/40 text-center">
        <p className="text-4xl" aria-hidden="true">
          {resultado.icon}
        </p>
        <h2 className="mt-2 text-2xl font-semibold">{resultado.title}</h2>
        <p className="mt-1 text-sm text-parchment/60">{resultado.subtitle}</p>
      </Card>

      <Card>
        <h3 className="mb-3 text-lg font-semibold">Roles revelados</h3>
        <ul className="flex flex-col gap-2">
          {gameOver.roles.map((role) => {
            const estado = game?.statuses.find((player) => player.id === role.playerId)
            return (
              <li
                key={role.playerId}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-night-700 bg-night-800/50 px-3 py-2 text-sm"
              >
                <span className="flex items-center gap-2">
                  <span>{role.nickname}</span>
                  {role.playerId === playerId ? (
                    <span className="rounded bg-night-700 px-1.5 py-0.5 text-[10px] uppercase tracking-wide">
                      tú
                    </span>
                  ) : null}
                </span>
                <span className="flex items-center gap-2 text-parchment/60">
                  {estado ? <span>{estado.alive ? 'vivo' : 'muerto'}</span> : null}
                  <strong className="text-parchment">
                    {GAME_ROLE_LABELS[role.role] ?? role.role}
                  </strong>
                </span>
              </li>
            )
          })}
        </ul>
      </Card>

      <Card className="flex flex-col items-center gap-3 text-center">
        {soyHost ? (
          <>
            <p className="text-sm text-parchment/60">
              Vuelve a la configuración para ajustar la partida con los mismos jugadores.
            </p>
            <Button onClick={returnToLobby}>Volver a configuración</Button>
          </>
        ) : (
          <p className="text-sm text-parchment/60">
            Esperando a que el host vuelva a la configuración…
          </p>
        )}
        <Button variant="ghost" onClick={salir}>
          Salir de la sala
        </Button>
      </Card>
    </div>
  )
}
