import { useState } from 'react'
import type { FormEvent } from 'react'
import type { GameSettings, Role } from '../protocol'
import { ROLES } from '../protocol'
import { ROLE_LABELS } from '../roles'
import { Button } from './ui/Button'
import { Input } from './ui/Input'

export interface SettingsFormProps {
  settings: GameSettings
  onSave: (next: GameSettings) => void
}

export function SettingsForm({ settings, onSave }: SettingsFormProps) {
  const [draft, setDraft] = useState<GameSettings>(settings)

  function cambiar<K extends keyof GameSettings>(campo: K, valor: GameSettings[K]) {
    setDraft((prev) => ({ ...prev, [campo]: valor }))
  }

  function alternarRol(role: Role) {
    setDraft((prev) => ({
      ...prev,
      roles: prev.roles.includes(role)
        ? prev.roles.filter((actual) => actual !== role)
        : [...prev.roles, role],
    }))
  }

  function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onSave(draft)
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={enviar}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input
          label="Número de lobos"
          type="number"
          min={1}
          max={8}
          value={draft.wolves}
          onChange={(event) => cambiar('wolves', Number(event.target.value))}
        />

        <div className="flex flex-col gap-1.5 text-sm">
          <label className="font-medium text-parchment/80" htmlFor="lives">
            Vidas
          </label>
          <select
            id="lives"
            value={draft.lives}
            onChange={(event) => cambiar('lives', Number(event.target.value))}
            className="rounded-lg border border-night-700 bg-night-900 px-3 py-2 text-parchment focus:border-blood focus:outline-none"
          >
            <option value={0}>0 · muerte directa</option>
            <option value={1}>1 · una vida</option>
            <option value={2}>2 · dos vidas</option>
          </select>
        </div>
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1 text-sm font-medium text-parchment/80">Roles especiales</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {ROLES.map((role) => (
            <label
              key={role}
              className="flex items-center gap-2 rounded-lg border border-night-700 bg-night-800/50 px-3 py-2 text-sm"
            >
              <input
                type="checkbox"
                checked={draft.roles.includes(role)}
                onChange={() => alternarRol(role)}
                className="accent-blood"
              />
              {ROLE_LABELS[role]}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-3">
        <Input
          label="Debate (s)"
          type="number"
          min={30}
          max={600}
          value={draft.debateSeconds}
          onChange={(event) => cambiar('debateSeconds', Number(event.target.value))}
        />
        <Input
          label="Acción de rol (s)"
          type="number"
          min={10}
          max={120}
          value={draft.roleSeconds}
          onChange={(event) => cambiar('roleSeconds', Number(event.target.value))}
        />
        <Input
          label="Votación (s)"
          type="number"
          min={15}
          max={180}
          value={draft.voteSeconds}
          onChange={(event) => cambiar('voteSeconds', Number(event.target.value))}
        />
      </div>

      <div className="flex flex-col gap-2">
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={draft.president}
            onChange={(event) => cambiar('president', event.target.checked)}
            className="accent-blood"
          />
          Presidente
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={draft.showVoteDetail}
            onChange={(event) => cambiar('showVoteDetail', event.target.checked)}
            className="accent-blood"
          />
          Mostrar detalle de votos sí/no
        </label>
      </div>

      <Button type="submit" variant="secondary">
        Guardar configuración
      </Button>
    </form>
  )
}
