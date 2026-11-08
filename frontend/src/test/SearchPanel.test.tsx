import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SearchPanel } from '../components/filters/SearchPanel'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'

describe('SearchPanel component', () => {
  let queryClient: QueryClient
  const onSearch = vi.fn()

  const renderSearchPanel = (isLoading = false) =>
    render(
      <QueryClientProvider client={queryClient}>
        <SearchPanel onSearch={onSearch} isLoading={isLoading} />
      </QueryClientProvider>,
    )

  beforeEach(() => {
    vi.clearAllMocks()
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
      },
    })
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      isFiltersCollapsed: false,
    })
  })

  it('renders all main search controls in expanded mode', () => {
    renderSearchPanel(false)

    expect(screen.getByText('Keresési Paraméterek & Szűrők')).toBeTruthy()
    expect(screen.getByText(/Keresési Települések/i)).toBeTruthy()
    expect(screen.getByText('Ingatlan Típusa')).toBeTruthy()
    expect(screen.getByText('Minimális ár')).toBeTruthy()
    expect(screen.getByText('Maximális ár')).toBeTruthy()
    expect(screen.getByText('Minimális alapterület')).toBeTruthy()
    expect(screen.getByText('Maximális alapterület')).toBeTruthy()
    expect(screen.getByText(/Keresési Portálok/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /Ingatlanok Keresése/i })).toBeTruthy()
  })

  it('toggles collapsible summary mode when toggle button is clicked', () => {
    renderSearchPanel(false)

    const toggleBtn = screen.getByRole('button', { name: /Összecsukás/i })
    fireEvent.click(toggleBtn)

    expect(useUiStore.getState().isFiltersCollapsed).toBe(true)
    const summary = screen.getByTestId('collapsed-filter-summary')
    expect(summary).toBeTruthy()
    expect(summary.textContent).toContain('Hajdúszoboszló')
    expect(summary.textContent).toContain('Családi ház')
    expect(summary.textContent).toContain('70 - 120 M Ft')
    expect(summary.textContent).toContain('90 - 200 m²')

    // Click to expand again
    const expandBtn = screen.getByRole('button', { name: /Kibontás/i })
    fireEvent.click(expandBtn)
    expect(useUiStore.getState().isFiltersCollapsed).toBe(false)
    expect(screen.queryByTestId('collapsed-filter-summary')).toBeNull()
  })

  it('shows validation warning and disables submit when minPrice > maxPrice', () => {
    useFilterStore.getState().setMinPriceMillion(150)
    useFilterStore.getState().setMaxPriceMillion(50)

    renderSearchPanel(false)

    const warning = screen.getByTestId('search-validation-warning')
    expect(warning).toBeTruthy()
    expect(warning.textContent).toContain('A minimális ár (150 M Ft) nem lehet nagyobb a maximális árnál (50 M Ft)')

    const submitBtn = screen.getByRole('button', { name: /Ingatlanok Keresése/i })
    expect(submitBtn.getAttribute('disabled')).not.toBeNull()

    // Submitting form should be blocked
    fireEvent.click(submitBtn)
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('shows validation warning and disables submit when minArea > maxArea', () => {
    useFilterStore.getState().setMinArea(250)
    useFilterStore.getState().setMaxArea(80)

    renderSearchPanel(false)

    const warning = screen.getByTestId('search-validation-warning')
    expect(warning).toBeTruthy()
    expect(warning.textContent).toContain('A minimális alapterület (250 m²) nem lehet nagyobb a maximális alapterületnél (80 m²)')

    const submitBtn = screen.getByRole('button', { name: /Ingatlanok Keresése/i })
    expect(submitBtn.getAttribute('disabled')).not.toBeNull()
  })

  it('disables submit button when no portals are selected', () => {
    useFilterStore.getState().setAllPortals([])

    renderSearchPanel(false)

    const submitBtn = screen.getByRole('button', { name: /Ingatlanok Keresése/i })
    expect(submitBtn.getAttribute('disabled')).not.toBeNull()
  })

  it('displays loading state and disables submit when isLoading is true', () => {
    renderSearchPanel(true)

    expect(screen.getByText('Keresés folyamatban...')).toBeTruthy()
    const submitBtn = screen.getByRole('button', { name: /Keresés folyamatban.../i })
    expect(submitBtn.getAttribute('disabled')).not.toBeNull()
  })

  it('submits form successfully when inputs are valid', () => {
    renderSearchPanel(false)

    const submitBtn = screen.getByRole('button', { name: /Ingatlanok Keresése/i })
    expect(submitBtn.getAttribute('disabled')).toBeNull()

    fireEvent.click(submitBtn)
    expect(onSearch).toHaveBeenCalledTimes(1)
  })
})
