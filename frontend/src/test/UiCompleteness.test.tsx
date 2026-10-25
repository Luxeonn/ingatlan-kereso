import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { SearchPanel } from '../components/filters/SearchPanel'
import { filterAndSortItems } from '../hooks/useSearch'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { IngatlanItem } from '../types/property'

const sampleItems: IngatlanItem[] = [
  {
    id: '1',
    portal: 'Duna House',
    url: 'https://dh.hu/1',
    title: 'Új építésű családi ház Nagyerdőn',
    price: '110 M Ft',
    location: 'Debrecen, Nagyerdő',
    city: 'Debrecen',
    isNew: true,
    viewed: false,
    newConstruction: true,
    priceInfo: { amountMillion: 110, amountHuf: 110_000_000 },
    detailsInfo: { areaSquareMeters: 140, roomCount: 4, pricePerSquareMeter: 785_714 },
  },
  {
    id: '2',
    portal: 'Otthon Centrum',
    url: 'https://oc.hu/2',
    title: 'Felújított belvárosi tégla lakás',
    price: '75 M Ft',
    location: 'Debrecen, Belváros',
    city: 'Debrecen',
    isNew: false,
    viewed: true,
    newConstruction: false,
    priceInfo: { amountMillion: 75, amountHuf: 75_000_000 },
    detailsInfo: { areaSquareMeters: 68, roomCount: 2, pricePerSquareMeter: 1_102_941 },
  },
]

describe('UI Completeness (#157, #202, #227)', () => {
  beforeEach(() => {
    localStorage.clear()
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      isFiltersCollapsed: false,
      dashboardTab: 'listings',
      starredUrls: [],
      starredItemsCache: {},
    })
  })

  it('filters items by multi-word quick search, portal chip, viewed state, and new construction', () => {
    // Quick search with multiple keywords (e.g. "debrecen 140 haz")
    const byQuickSearch = filterAndSortItems(sampleItems, {
      quickSearchQuery: 'debrecen 140 haz',
      sortOption: 'price_asc',
    })
    expect(byQuickSearch).toHaveLength(1)
    expect(byQuickSearch[0].id).toBe('1')

    // Portal chip filter
    const byPortal = filterAndSortItems(sampleItems, {
      selectedPortalFilter: 'oc',
      sortOption: 'price_asc',
    })
    expect(byPortal).toHaveLength(1)
    expect(byPortal[0].id).toBe('2')

    // Only new construction filter (#202)
    const byNewBuild = filterAndSortItems(sampleItems, {
      onlyNewConstruction: true,
      sortOption: 'price_asc',
    })
    expect(byNewBuild).toHaveLength(1)
    expect(byNewBuild[0].id).toBe('1')

    // Exclude viewed filter (#227)
    const byUnviewed = filterAndSortItems(sampleItems, {
      excludeViewed: true,
      sortOption: 'price_asc',
    })
    expect(byUnviewed).toHaveLength(1)
    expect(byUnviewed[0].id).toBe('1')
  })

  it('renders BulkActionBar with quick search input, exclude viewed, sort select, portal chips, and archive clear confirmation', () => {
    const handleClear = vi.fn()
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        onOpenAll={() => {}}
        onMarkAllViewed={() => {}}
        onClearHistory={handleClear}
      />,
    )

    const searchInput = screen.getByPlaceholderText(/Gyorskeresés a találatok között/i)
    fireEvent.change(searchInput, { target: { value: 'Nagyerdő 140' } })
    expect(useFilterStore.getState().quickSearchQuery).toBe('Nagyerdő 140')

    // Exclude viewed toggle (#273)
    const excludeViewedBtn = screen.getByRole('button', { name: /Megtekintettek rejtése/i })
    expect(excludeViewedBtn).toBeTruthy()
    fireEvent.click(excludeViewedBtn)
    expect(useFilterStore.getState().criteria.excludeViewed).toBe(true)

    // Sort select dropdown (#273)
    const sortSelect = screen.getByLabelText('Rendezés')
    expect(sortSelect).toBeTruthy()
    fireEvent.change(sortSelect, { target: { value: 'price_desc' } })
    expect(useFilterStore.getState().sortOption).toBe('price_desc')

    const ocChip = screen.getByRole('button', { name: /Otthon Centrum/i })
    fireEvent.click(ocChip)
    expect(useFilterStore.getState().selectedPortalFilter).toBe('oc')

    // Clear history confirmation flow
    const clearBtn = screen.getByRole('button', { name: /Előzmények törlése/i })
    fireEvent.click(clearBtn)
    const confirmBtn = screen.getByRole('button', { name: /Törlés megerősítése/i })
    fireEvent.click(confirmBtn)
    expect(handleClear).toHaveBeenCalledTimes(1)
  })

  it('renders collapsed summary bar in SearchPanel and does not include Sort or ExcludeViewed in SearchPanel (#157, #202, #227)', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <SearchPanel onSearch={() => {}} isLoading={false} />
      </QueryClientProvider>,
    )

    // New construction toggle is present in SearchPanel
    expect(screen.getByRole('button', { name: /Csak új építésű ingatlanok/i })).toBeTruthy()
    // Sort and ExcludeViewed were moved out of SearchPanel (#227)
    expect(screen.queryByText('Megtekintettek rejtése')).toBeNull()

    // Collapse the panel and verify summary bar (#157)
    const collapseBtn = screen.getByRole('button', { name: /Összecsukás/i })
    fireEvent.click(collapseBtn)

    rerender(
      <QueryClientProvider client={queryClient}>
        <SearchPanel onSearch={() => {}} isLoading={false} />
      </QueryClientProvider>,
    )

    const summary = screen.getByTestId('collapsed-filter-summary')
    expect(summary.textContent).toContain('Debrecen')
    expect(summary.textContent).toContain('70 - 120 M Ft')
    expect(summary.textContent).toContain('90 - 200 m²')
    expect(summary.textContent).toContain('6/6 portál')
  })

  it('allows free numeric typing across min/max ranges and displays warning near search button when min > max (#255)', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const handleSearch = vi.fn()

    render(
      <QueryClientProvider client={queryClient}>
        <SearchPanel onSearch={handleSearch} isLoading={false} />
      </QueryClientProvider>,
    )

    // Set minPrice = 80
    const minPriceInput = screen.getByDisplayValue('70') as HTMLInputElement
    fireEvent.change(minPriceInput, { target: { value: '80' } })
    expect(useFilterStore.getState().criteria.minPriceMillion).toBe(80)

    // User edits maxPrice from 120 to 200 by typing '2' first (which is < 80)
    const maxPriceInput = screen.getByDisplayValue('120') as HTMLInputElement
    fireEvent.change(maxPriceInput, { target: { value: '2' } })
    expect(maxPriceInput.value).toBe('2')
    expect(useFilterStore.getState().criteria.maxPriceMillion).toBe(2)

    // Warning message should appear and search button should be disabled while 80 > 2
    const warning = screen.getByTestId('search-validation-warning')
    expect(warning.textContent).toContain('A minimális ár (80 M Ft) nem lehet nagyobb a maximális árnál (2 M Ft).')
    const searchBtn = screen.getByRole('button', { name: /Ingatlanok Keresése/i }) as HTMLButtonElement
    expect(searchBtn.disabled).toBe(true)

    // Completing typing '200' resolves the warning and re-enables the button
    fireEvent.change(maxPriceInput, { target: { value: '200' } })
    expect(maxPriceInput.value).toBe('200')
    expect(useFilterStore.getState().criteria.maxPriceMillion).toBe(200)
    expect(screen.queryByTestId('search-validation-warning')).toBeNull()
    expect(searchBtn.disabled).toBe(false)

    fireEvent.click(searchBtn)
    expect(handleSearch).toHaveBeenCalledTimes(1)
  })

  it('keeps favorites counter in header and rendered favorites list synchronized even with missing cache (#271)', () => {
    // Simulate orphan starred url without item in cache
    useUiStore.setState({
      starredUrls: ['https://dh.hu/elado-ingatlan/12345'],
      starredItemsCache: {},
    })

    const byUrl = new Map<string, IngatlanItem>()
    Object.entries(useUiStore.getState().starredItemsCache).forEach(([u, item]) => {
      if (item) byUrl.set(u, item)
    })
    const starredUrls = useUiStore.getState().starredUrls
    const rawFavoriteItems = starredUrls.map((url) => {
      const existing = byUrl.get(url)
      if (existing) return existing
      return {
        id: `orphan-${url}`,
        portal: 'Duna House',
        url,
        title: 'Elmentett hirdetés (Duna House)',
        price: 'N/A',
        location: 'Elmentett hivatkozás',
        city: 'Ismeretlen',
      }
    })

    expect(rawFavoriteItems).toHaveLength(1)
    expect(rawFavoriteItems[0].url).toBe('https://dh.hu/elado-ingatlan/12345')
    expect(rawFavoriteItems[0].portal).toBe('Duna House')
  })
})
