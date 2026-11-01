import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { SessionValue } from '../session/context'
import { SessionContext } from '../session/context'
import { HomeScreen } from './HomeScreen'

function renderHome(overrides: Partial<SessionValue> = {}) {
  const value: SessionValue = {
    status: 'idle',
    connectionId: null,
    roomCode: null,
    playerId: null,
    players: [],
    error: null,
    notice: null,
    createRoom: vi.fn(),
    joinRoom: vi.fn(),
    kickPlayer: vi.fn(),
    leave: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  }

  render(
    <MemoryRouter>
      <SessionContext.Provider value={value}>
        <HomeScreen />
      </SessionContext.Provider>
    </MemoryRouter>,
  )

  return value
}

describe('HomeScreen', () => {
  it('exige un apodo de entre 3 y 16 caracteres', () => {
    const value = renderHome()

    fireEvent.change(screen.getByLabelText('Tu apodo'), { target: { value: 'ab' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear sala y entrar' }))

    expect(screen.getByRole('alert')).toHaveTextContent(/entre 3 y 16/i)
    expect(value.createRoom).not.toHaveBeenCalled()
  })

  it('crea la sala con el apodo limpio', () => {
    const value = renderHome()

    fireEvent.change(screen.getByLabelText('Tu apodo'), { target: { value: '  Ana  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear sala y entrar' }))

    expect(value.createRoom).toHaveBeenCalledWith('Ana')
  })

  it('para unirse exige un código de 6 caracteres y lo normaliza', () => {
    const value = renderHome()

    fireEvent.click(screen.getByRole('button', { name: 'Unirse' }))
    fireEvent.change(screen.getByLabelText('Tu apodo'), { target: { value: 'Luis' } })
    fireEvent.change(screen.getByLabelText('Código de sala'), { target: { value: 'abc23' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar en la sala' }))

    expect(screen.getByRole('alert')).toHaveTextContent(/6 caracteres/i)
    expect(value.joinRoom).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Código de sala'), { target: { value: 'abc234' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar en la sala' }))

    expect(value.joinRoom).toHaveBeenCalledWith('ABC234', 'Luis')
  })

  it('muestra los errores que llegan del servidor', () => {
    renderHome({ error: 'No existe ninguna sala con ese código.' })

    expect(screen.getByRole('alert')).toHaveTextContent(/no existe ninguna sala/i)
  })

  it('deshabilita el botón mientras conecta', () => {
    renderHome({ status: 'connecting' })

    expect(screen.getByRole('button', { name: 'Conectando…' })).toBeDisabled()
  })
})
