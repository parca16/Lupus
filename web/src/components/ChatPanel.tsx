import { useState } from 'react'
import { useSession } from '../session/useSession'
import { Button } from './ui/Button'
import { Card } from './ui/Card'

const PUBLIC_PHASES = new Set(['DEBATE', 'VOTING', 'RUNOFF'])

const CHANNELS: Record<string, { label: string; hint: string }> = {
  public: { label: 'Público', hint: 'Todos los jugadores' },
  wolves: { label: 'Lobos', hint: 'Solo la manada' },
  lovers: { label: 'Enamorados', hint: 'Solo vosotros dos' },
}

export function ChatPanel() {
  const { game, playerId, chatMessages, sendChat } = useSession()
  const [selected, setSelected] = useState('public')
  const [draft, setDraft] = useState('')

  if (!game) {
    return null
  }

  const yo = game.statuses.find((player) => player.id === playerId)
  const enamorado = game.statuses.find((player) => player.id === game.loverId)
  const disponibles: string[] = []

  if (yo?.alive && PUBLIC_PHASES.has(game.phase)) {
    disponibles.push('public')
  }
  if (yo?.alive && game.phase === 'NIGHT' && game.role === 'WOLF') {
    disponibles.push('wolves')
  }
  if (yo?.alive && enamorado?.alive) {
    disponibles.push('lovers')
  }

  if (disponibles.length === 0) {
    return null
  }

  const activo = disponibles.includes(selected) ? selected : disponibles[0]
  const mensajes = chatMessages.filter((message) => message.channel === activo).slice(-50)

  const enviar = () => {
    const text = draft.trim()
    if (!text) {
      return
    }
    sendChat(activo, text)
    setDraft('')
  }

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-lg font-semibold">Chat</h3>
        <div className="flex flex-wrap gap-1" role="group" aria-label="Canales de chat">
          {disponibles.map((channel) => (
            <Button
              key={channel}
              variant={activo === channel ? 'primary' : 'secondary'}
              aria-pressed={activo === channel}
              onClick={() => setSelected(channel)}
              className="px-3 py-1 text-xs"
            >
              {CHANNELS[channel].label}
            </Button>
          ))}
        </div>
      </div>

      <p className="mt-1 text-xs text-parchment/50">{CHANNELS[activo].hint}</p>

      <ol
        role="log"
        aria-live="polite"
        className="mt-3 flex max-h-64 min-h-20 flex-col gap-2 overflow-y-auto rounded-lg border border-night-700 bg-night-950/60 p-3"
      >
        {mensajes.length === 0 ? (
          <li className="text-sm text-parchment/40">Todavía no hay mensajes.</li>
        ) : (
          mensajes.map((message) => (
            <li key={message.id} className="text-sm">
              <span
                className={
                  message.senderId === playerId
                    ? 'font-semibold text-parchment'
                    : 'font-semibold text-parchment/70'
                }
              >
                {message.senderId === playerId ? 'Tú' : message.senderNickname}:
              </span>{' '}
              <span className="text-parchment/80">{message.text}</span>
            </li>
          ))
        )}
      </ol>

      <form
        className="mt-3 flex gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          enviar()
        }}
      >
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          maxLength={500}
          aria-label="Mensaje de chat"
          placeholder="Escribe un mensaje…"
          className="min-w-0 flex-1 rounded-lg border border-night-700 bg-night-900 px-3 py-2 text-sm text-parchment placeholder:text-parchment/30 focus:border-blood focus:outline-none"
        />
        <Button type="submit" disabled={!draft.trim()}>
          Enviar
        </Button>
      </form>
    </Card>
  )
}
