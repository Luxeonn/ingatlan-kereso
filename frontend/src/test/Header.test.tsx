import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Header } from '../components/layout/Header'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore } from '../store/useFilterStore'

describe('Header layout component', () => {
  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      isDiagnosticsModalOpen: false,
    })
  })

  it('renders brand title, subtitle and SPA badge', () => {
    render(<Header />)

    expect(screen.getByText('Ingatlan Kereső')).toBeTruthy()
    expect(screen.getByText('SPA')).toBeTruthy()
    expect(screen.getByText(/Valós idejű többportos aggregátor/i)).toBeTruthy()
  })

  it('triggers resetToDefaults when reset button is clicked', () => {
    useFilterStore.getState().addCity('Debrecen')
    useFilterStore.getState().setOnlyBargain(true)

    render(<Header />)

    const resetBtn = screen.getByTitle('Szűrők alaphelyzetbe állítása')
    expect(resetBtn).toBeTruthy()

    fireEvent.click(resetBtn)

    expect(useFilterStore.getState().criteria.cities).toEqual(['Hajdúszoboszló'])
    expect(useFilterStore.getState().onlyBargain).toBe(false)
  })

  it('opens diagnostics modal when diagnostics button is clicked', () => {
    render(<Header />)

    const diagBtn = screen.getByTitle('Portál Diagnosztika és Állapot')
    expect(diagBtn).toBeTruthy()

    fireEvent.click(diagBtn)

    expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(true)
  })

  it('does not render theme switcher or command palette buttons (light-only, no Ctrl+K shortcut)', () => {
    render(<Header />)

    expect(screen.queryByTestId('command-palette-trigger')).toBeNull()
    expect(screen.queryByTitle(/Váltás .* témára/i)).toBeNull()
  })
})
