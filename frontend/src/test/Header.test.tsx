import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { TopNavBar } from '../components/layout/TopNavBar'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore } from '../store/useFilterStore'

describe('TopNavBar layout component', () => {
  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      isDiagnosticsModalOpen: false,
      isFiltersCollapsed: false,
      dashboardTab: 'listings',
      listingSubView: 'listings',
    })
  })

  it('renders brand title, SPA badge, and 4-item navigation capsule with counts', () => {
    render(
      <TopNavBar
        listingsCount={12}
        favoritesCount={3}
        archiveCount={8}
        analyticsCount={12}
      />,
    )

    expect(screen.getByText('Ingatlan Kereső')).toBeTruthy()
    expect(screen.getByText('SPA')).toBeTruthy()
    expect(screen.getByRole('tablist')).toBeTruthy()
    expect(screen.getByTestId('tab-listings').textContent).toContain('Találatok')
    expect(screen.getByTestId('tab-listings').textContent).toContain('12')
    expect(screen.getByTestId('subview-favorites').textContent).toContain('Kedvencek')
    expect(screen.getByTestId('subview-favorites').textContent).toContain('3')
    expect(screen.getByTestId('tab-archive').textContent).toContain('Archívum')
    expect(screen.getByTestId('tab-archive').textContent).toContain('8')
    expect(screen.getByTestId('tab-analytics').textContent).toContain('Piaci Elemzés')
  })

  it('toggles FilterSidebar collapse state with accessible aria-expanded and labels', () => {
    const { rerender } = render(<TopNavBar />)

    const toggleBtn = screen.getByTestId('sidebar-toggle-btn')
    expect(toggleBtn.getAttribute('aria-expanded')).toBe('true')
    expect(toggleBtn.textContent).toContain('Összecsukás')

    fireEvent.click(toggleBtn)
    expect(useUiStore.getState().isFiltersCollapsed).toBe(true)

    rerender(<TopNavBar />)
    const collapsedToggle = screen.getByTestId('sidebar-toggle-btn')
    expect(collapsedToggle.getAttribute('aria-expanded')).toBe('false')
    expect(collapsedToggle.textContent).toContain('Kibontás')
  })

  it('switches across Találatok, Kedvencek, Archívum, and Piaci Elemzés tabs', () => {
    const onSelectFavorites = vi.fn()
    render(<TopNavBar onSelectFavorites={onSelectFavorites} />)

    fireEvent.click(screen.getByTestId('subview-favorites'))
    expect(onSelectFavorites).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByTestId('tab-archive'))
    expect(useUiStore.getState().dashboardTab).toBe('listings')
    expect(useUiStore.getState().listingSubView).toBe('archive')

    fireEvent.click(screen.getByTestId('tab-analytics'))
    expect(useUiStore.getState().dashboardTab).toBe('analytics')

    fireEvent.click(screen.getByTestId('tab-listings'))
    expect(useUiStore.getState().dashboardTab).toBe('listings')
    expect(useUiStore.getState().listingSubView).toBe('listings')
  })

  it('triggers resetToDefaults when reset button is clicked', () => {
    useFilterStore.getState().addCity('Debrecen')
    useFilterStore.getState().setOnlyBargain(true)

    render(<TopNavBar />)

    const resetBtn = screen.getByTitle('Szűrők alaphelyzetbe állítása')
    expect(resetBtn).toBeTruthy()

    fireEvent.click(resetBtn)

    expect(useFilterStore.getState().criteria.cities).toEqual(['Hajdúszoboszló'])
    expect(useFilterStore.getState().onlyBargain).toBe(false)
  })

  it('opens diagnostics modal when diagnostics button is clicked', () => {
    render(<TopNavBar />)

    const diagBtn = screen.getByTitle('Portál Diagnosztika és Állapot')
    expect(diagBtn).toBeTruthy()

    fireEvent.click(diagBtn)

    expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(true)
  })

  it('does not render theme switcher or command palette buttons (light-only, no Ctrl+K shortcut)', () => {
    render(<TopNavBar />)

    expect(screen.queryByTestId('command-palette-trigger')).toBeNull()
    expect(screen.queryByTitle(/Váltás .* témára/i)).toBeNull()
  })
})
