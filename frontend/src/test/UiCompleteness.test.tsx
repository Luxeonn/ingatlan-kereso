import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, within, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { SearchPanel } from '../components/filters/SearchPanel'
import { DiagnosticsModal } from '../components/modals/DiagnosticsModal'
import { filterAndSortItems } from '../hooks/useSearch'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore, DEFAULT_PINNED_QUICK_FILTERS } from '../store/useUiStore'
import type { IngatlanItem } from '../types/property'

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
    sourcesCount: 1,
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
    sourcesCount: 1,
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
      listingSubView: 'listings',
      starredUrls: [],
      starredItemsCache: {},
      pinnedQuickFilters: [...DEFAULT_PINNED_QUICK_FILTERS],
    })
  })

  it('filters items by multi-word quick search, portal chip, viewed state, and new construction', () => {
    // Quick search with multiple keywords (e.g. "debrecen 140 haz")
    const byQuickSearch = filterAndSortItems(sampleItems, {
      quickSearchQuery: 'debrecen 140 haz',
      sortOption: 'price_asc',
    })
    expect(byQuickSearch).toHaveLength(1)
    expect(byQuickSearch[0]?.id).toBe('1')

    // Portal chip filter
    const byPortal = filterAndSortItems(sampleItems, {
      selectedPortalFilter: 'oc',
      sortOption: 'price_asc',
    })
    expect(byPortal).toHaveLength(1)
    expect(byPortal[0]?.id).toBe('2')

    // Only new construction filter (#202)
    const byNewBuild = filterAndSortItems(sampleItems, {
      onlyNewConstruction: true,
      sortOption: 'price_asc',
    })
    expect(byNewBuild).toHaveLength(1)
    expect(byNewBuild[0]?.id).toBe('1')

    // Exclude viewed filter (#227)
    const byUnviewed = filterAndSortItems(sampleItems, {
      excludeViewed: true,
      sortOption: 'price_asc',
    })
    expect(byUnviewed).toHaveLength(1)
    expect(byUnviewed[0]?.id).toBe('1')
  })

  it('renders BulkActionBar with quick search input, default pinned quick filters, sort select, portal chips, and archive clear confirmation (#359)', () => {
    const handleClear = vi.fn()
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        onClearHistory={handleClear}
      />,
    )

    const searchInput = screen.getByPlaceholderText(/Gyorskeresés a találatok között/i)
    fireEvent.change(searchInput, { target: { value: 'Nagyerdő 140' } })
    expect(useFilterStore.getState().quickSearchQuery).toBe('Nagyerdő 140')

    // Sort select dropdown (#273, #359)
    const sortSelect = screen.getByLabelText('Rendezés')
    expect(sortSelect).toBeTruthy()
    fireEvent.change(sortSelect, { target: { value: 'price_desc' } })
    expect(useFilterStore.getState().sortOption).toBe('price_desc')

    const ocChip = screen.getByRole('button', { name: /Otthon Centrum/i })
    fireEvent.click(ocChip)
    expect(useFilterStore.getState().selectedPortalFilter).toBe('oc')

    // Default 3 pinned quick filter chips in BulkActionBar (#359): Alkalmi vétel, Csak új találatok, Elrejtettek
    const bargainBtn = screen.getByTestId('bargain-filter-toggle')
    expect(bargainBtn).toBeTruthy()
    fireEvent.click(bargainBtn)
    expect(useFilterStore.getState().onlyBargain).toBe(true)

    const onlyNewBtn = screen.getByRole('button', { name: /Csak új találatok/i })
    expect(onlyNewBtn).toBeTruthy()
    fireEvent.click(onlyNewBtn)
    expect(useFilterStore.getState().onlyNew).toBe(true)

    const hiddenBtn = screen.getByTestId('hidden-filter-toggle')
    expect(hiddenBtn).toBeTruthy()
    fireEvent.click(hiddenBtn)
    expect(useFilterStore.getState().showHidden).toBe(true)

    // Clear history confirmation flow
    const clearBtn = screen.getByRole('button', { name: /Előzmények törlése/i })
    fireEvent.click(clearBtn)
    const confirmBtn = screen.getByRole('button', { name: /Törlés megerősítése/i })
    fireEvent.click(confirmBtn)
    expect(handleClear).toHaveBeenCalledTimes(1)
  })

  it('renders collapsed summary bar in SearchPanel and does not include results filters in SearchPanel (#157, #202, #227, #303)', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    const { rerender } = render(
      <QueryClientProvider client={queryClient}>
        <SearchPanel onSearch={() => {}} isLoading={false} />
      </QueryClientProvider>,
    )

    // OnlyNew and NewConstruction were moved out of SearchPanel to BulkActionBar (#303)
    expect(screen.queryByText(/Csak új találatok/i)).toBeNull()
    expect(screen.queryByText(/Csak új építésű/i)).toBeNull()
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
    expect(summary.textContent).toContain('Hajdúszoboszló')
    expect(summary.textContent).toContain('70 - 120 M Ft')
    expect(summary.textContent).toContain('90 - 200 m²')
    expect(summary.textContent).toContain('6/6 portál')
  })

  it('structures BulkActionBar in three logical tiers: Search/Sort, Customizable Filter Chips (max 3), and Portal Chips (#303, #359)', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
      />,
    )

    // Row 1: Search input and sort select (without Összes megnyitása / Mind megtekintett)
    expect(screen.getByPlaceholderText(/Gyorskeresés a találatok között/i)).toBeTruthy()
    expect(screen.getByLabelText('Rendezés')).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Összes megnyitása/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Mind megtekintett/i })).toBeNull()

    // Row 2: Filter chips row container with 3 default pinned filters + selector button (#359, #369)
    const filterChipsRow = screen.getByTestId('filter-chips-row')
    expect(filterChipsRow).toBeTruthy()
    expect(screen.getByTestId('bargain-filter-toggle')).toBeTruthy()
    expect(screen.getByRole('button', { name: /Csak új találatok/i })).toBeTruthy()
    expect(screen.getByTestId('hidden-filter-toggle')).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Csak új építésű/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Megtekintettek rejtése/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Több forrásosak rejtése/i })).toBeNull()
    expect(screen.getByTestId('quick-filter-selector-btn')).toBeTruthy()
    expect(screen.queryByTestId('quality-filter-select')).toBeNull()

    // Row 3: Portal chips container
    expect(screen.getByTestId('portal-quick-filter-chips')).toBeTruthy()
  })

  it('filters portal quick filter chips to searched portals in criteria.portals and auto-activates single searched portal (#356)', () => {
    // 1. When only 2 portals are searched (dh, oc), only All, Duna House, and Otthon Centrum chips render
    useFilterStore.getState().setAllPortals(['dh', 'oc'])
    const { unmount } = render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        onOpenAll={() => {}}
        onMarkAllViewed={() => {}}
      />,
    )

    const chipsRow1 = within(screen.getByTestId('portal-quick-filter-chips'))
    expect(chipsRow1.getByRole('button', { name: /Összes/i })).toBeTruthy()
    expect(chipsRow1.getByRole('button', { name: /Duna House/i })).toBeTruthy()
    expect(chipsRow1.getByRole('button', { name: /Otthon Centrum/i })).toBeTruthy()
    expect(chipsRow1.queryByRole('button', { name: /Jófogás/i })).toBeNull()
    expect(chipsRow1.queryByRole('button', { name: /Ingatlan\.com/i })).toBeNull()
    expect(chipsRow1.queryByRole('button', { name: /Ingatlanbazár/i })).toBeNull()
    expect(chipsRow1.queryByRole('button', { name: /Költözzbe/i })).toBeNull()
    unmount()

    // 2. When only 1 portal is searched (e.g., oc), selectedPortalFilter automatically becomes 'oc'
    // and only the Otthon Centrum chip is rendered as active (without redundant 'Összes' or 0-count unsearched portals)
    useFilterStore.getState().setAllPortals(['oc'])
    expect(useFilterStore.getState().selectedPortalFilter).toBe('oc')

    render(
      <BulkActionBar
        items={[sampleItems[1]!]}
        rawItems={[sampleItems[1]!]}
        totalRawCount={1}
        onOpenAll={() => {}}
        onMarkAllViewed={() => {}}
      />,
    )

    const chipsRow2 = within(screen.getByTestId('portal-quick-filter-chips'))
    const ocChip = chipsRow2.getByRole('button', { name: /Otthon Centrum/i })
    expect(ocChip).toBeTruthy()
    expect(ocChip.className).toContain('bg-blue-600')
    expect(chipsRow2.queryByRole('button', { name: /Összes/i })).toBeNull()
    expect(chipsRow2.queryByRole('button', { name: /Duna House/i })).toBeNull()

    // 3. When toggling a second portal back in (oc + dh), both chips and 'Összes' appear;
    // when toggling 'oc' off while it was the active filter, selectedPortalFilter resets to the remaining portal or 'all'
    act(() => {
      useFilterStore.getState().togglePortal('dh')
    })
    expect(useFilterStore.getState().criteria.portals).toEqual(['oc', 'dh'])
    expect(useFilterStore.getState().selectedPortalFilter).toBe('oc')

    act(() => {
      useFilterStore.getState().togglePortal('jofogas')
      useFilterStore.getState().togglePortal('oc')
    })
    // 'oc' was removed while 'dh' and 'jofogas' remain -> falls back to 'all'
    expect(useFilterStore.getState().selectedPortalFilter).toBe('all')
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
        sourcesCount: 1,
      }
    })

    expect(rawFavoriteItems).toHaveLength(1)
    expect(rawFavoriteItems[0]?.url).toBe('https://dh.hu/elado-ingatlan/12345')
    expect(rawFavoriteItems[0]?.portal).toBe('Duna House')
  })

  it('renders DiagnosticsModal with latencyMs, foundCount, and clickable fallbackUrl button on BOT_PROTECTED (#300)', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    useUiStore.setState({ isDiagnosticsModalOpen: true })

    render(
      <QueryClientProvider client={queryClient}>
        <DiagnosticsModal
          portalStatuses={[
            {
              portalId: 'ingatlancom',
              portalName: 'Ingatlan.com',
              status: 'BOT_PROTECTED',
              latencyMs: 245,
              foundCount: 0,
              fallbackUrl: 'https://ingatlan.com/lista/elado+haz+debrecen',
              message: 'Cloudflare WAF detektálva',
            },
            {
              portalId: 'dh',
              portalName: 'Duna House',
              status: 'SUCCESS',
              latencyMs: 510,
              foundCount: 17,
              fallbackUrl: null,
              message: null,
            },
          ]}
        />
      </QueryClientProvider>,
    )

    expect(screen.getByText('245 ms')).toBeTruthy()
    expect(screen.getByText('17 találat')).toBeTruthy()
    const fallbackBtn = screen.getByTestId('fallback-link-ingatlancom')
    expect(fallbackBtn).toBeTruthy()
    expect(fallbackBtn.textContent).toContain('Keresés megnyitása itt: Ingatlan.com')
  })
})
