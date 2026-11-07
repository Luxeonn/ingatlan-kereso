import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Header } from '../components/layout/Header'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore } from '../store/useFilterStore'

describe('Header layout component', () => {
  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      theme: 'dark',
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

  it('toggles theme when theme switcher button is clicked', () => {
    render(<Header />)

    expect(useUiStore.getState().theme).toBe('dark')
    const themeBtn = screen.getByTitle('Váltás világos témára')
    expect(themeBtn).toBeTruthy()

    fireEvent.click(themeBtn)
    expect(useUiStore.getState().theme).toBe('light')

    fireEvent.click(screen.getByTitle('Váltás sötét témára'))
    expect(useUiStore.getState().theme).toBe('dark')
  })
})
