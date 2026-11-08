import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, within, fireEvent, waitFor, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type * as SearchApiModule from '../api/searchApi'
import type * as PropertyApiModule from '../api/propertyApi'
import type * as PresetApiModule from '../api/presetApi'
import { App } from '../App'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore, DEFAULT_PINNED_QUICK_FILTERS } from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'
import type { DashboardResponseDto } from '../types/api'

// Mock virtualizer for JSDOM
vi.mock('@tanstack/react-virtual', () => ({
  useWindowVirtualizer: (options: { count: number }) => ({
    getVirtualItems: () =>
      Array.from({ length: options.count }, (_, index) => ({
        index,
        start: index * 100,
        size: 100,
        key: String(index),
      })),
    getTotalSize: () => options.count * 100,
    scrollToIndex: vi.fn(),
  }),
}))

vi.mock('../api/searchApi', async (importOriginal) => {
  const actual = await importOriginal<typeof SearchApiModule>()
  return {
    ...actual,
    getLastConfig: vi.fn().mockResolvedValue(null),
    getPortals: vi.fn().mockResolvedValue([]),
    getCities: vi.fn().mockResolvedValue({ predefined: [], presets: {}, trackedCities: [] }),
    executeSearch: vi.fn(),
  }
})

vi.mock('../api/presetApi', async (importOriginal) => {
  const actual = await importOriginal<typeof PresetApiModule>()
  return {
    ...actual,
    getPresets: vi.fn().mockResolvedValue([
      { id: 101, name: 'Szegedi Sablon', criteria: { city: 'Szeged', cities: ['Szeged'] } },
    ]),
    createPreset: vi.fn().mockResolvedValue({ id: 102 }),
    updatePreset: vi.fn().mockResolvedValue({ id: 101 }),
    deletePreset: vi.fn().mockResolvedValue(undefined),
    recordPresetUsage: vi.fn().mockResolvedValue(undefined),
  }
})

vi.mock('../api/propertyApi', async (importOriginal) => {
  const actual = await importOriginal<typeof PropertyApiModule>()
  return {
    ...actual,
    getHistory: vi.fn().mockResolvedValue([]),
    getHiddenProperties: vi.fn().mockResolvedValue([]),
    markAsViewed: vi.fn().mockResolvedValue({ success: true } as never),
    hideProperty: vi.fn().mockResolvedValue({ success: true } as never),
    restoreProperty: vi.fn().mockResolvedValue({ success: true } as never),
    clearHistory: vi.fn().mockResolvedValue({ success: true } as never),
    openUrls: vi.fn().mockResolvedValue({ success: true } as never),
  }
})

describe('UI State Management & Interaction Regression (#378)', () => {
  let queryClient: QueryClient

  const sampleItem1 = createMockIngatlanItem({
    id: 'item-deb-1',
    url: 'https://dh.hu/ingatlan/deb-1',
    title: 'Debreceni polgári lakás',
    city: 'Debrecen',
    priceInfo: { amountHuf: 65_000_000, amountMillion: 65 },
  })

  const sampleItem2 = createMockIngatlanItem({
    id: 'item-szeg-2',
    url: 'https://oc.hu/ingatlan/szeg-2',
    title: 'Szegedi újszerű ház',
    city: 'Szeged',
    priceInfo: { amountHuf: 92_000_000, amountMillion: 92 },
  })

  const sampleDashboard: DashboardResponseDto = {
    items: [sampleItem1, sampleItem2],
    newCount: 2,
    totalFound: 2,
  }

  const renderApp = () => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    })
    return render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>,
    )
  }

  beforeEach(() => {
    localStorage.clear()
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      theme: 'dark',
      viewMode: 'grid',
      dashboardTab: 'listings',
      listingSubView: 'listings',
      isDiagnosticsModalOpen: false,
      isFiltersCollapsed: false,
      starredUrls: [],
      starredItemsCache: {},
      pinnedQuickFilters: [...DEFAULT_PINNED_QUICK_FILTERS],
    })
  })

  describe('1. Tab and view navigation: state preservation', () => {
    it('preserves search results and filter state across tab switches', async () => {
      const searchApi = await import('../api/searchApi')
      vi.mocked(searchApi.executeSearch).mockResolvedValueOnce(sampleDashboard)

      renderApp()

      // Trigger search
      const searchButton = screen.getByRole('button', { name: 'Ingatlanok Keresése' })
      fireEvent.click(searchButton)

      await waitFor(() => {
        expect(screen.getByText('Debreceni polgári lakás')).toBeTruthy()
        expect(screen.getByText('Szegedi újszerű ház')).toBeTruthy()
      })

      // Switch to Piaci Elemzés tab
      const analyticsTab = screen.getByTestId('tab-analytics')
      fireEvent.click(analyticsTab)
      expect(useUiStore.getState().dashboardTab).toBe('analytics')

      // Switch back to Találatok tab
      const listingsTab = screen.getByTestId('tab-listings')
      fireEvent.click(listingsTab)
      expect(useUiStore.getState().dashboardTab).toBe('listings')

      // Results must still be present without data loss
      expect(screen.getByText('Debreceni polgári lakás')).toBeTruthy()
      expect(screen.getByText('Szegedi újszerű ház')).toBeTruthy()
    })

    it('navigates subviews without data loss and auto-switches to listings on new search', async () => {
      const searchApi = await import('../api/searchApi')
      vi.mocked(searchApi.executeSearch).mockResolvedValue(sampleDashboard)

      // Preload a favorite item
      useUiStore.setState({
        starredUrls: [sampleItem1.url],
        starredItemsCache: { [sampleItem1.url]: sampleItem1 },
      })

      renderApp()

      // Navigate to favorites subview
      const favSubViewBtn = screen.getByTestId('subview-favorites')
      fireEvent.click(favSubViewBtn)
      expect(useUiStore.getState().listingSubView).toBe('favorites')
      expect(screen.getByText('Debreceni polgári lakás')).toBeTruthy()

      // Trigger new search while in favorites subview
      const searchButton = screen.getByRole('button', { name: 'Ingatlanok Keresése' })
      fireEvent.click(searchButton)

      // handleSearch must auto-reset subview and tab to listings
      await waitFor(() => {
        expect(useUiStore.getState().listingSubView).toBe('listings')
        expect(useUiStore.getState().dashboardTab).toBe('listings')
      })
    })
  })

  describe('2. Filter & Sort modification, preservation and exact Reset', () => {
    it('modifies filters and resets criteria/sort while strictly preserving independent preferences', () => {
      renderApp()

      // Set custom independent preferences
      act(() => {
        useUiStore.getState().toggleTheme() // 'dark' -> 'light'
        useUiStore.getState().setViewMode('list')
        useUiStore.getState().toggleStarred(sampleItem1)
      })
      expect(useUiStore.getState().theme).toBe('light')
      expect(useUiStore.getState().viewMode).toBe('list')
      expect(useUiStore.getState().isStarred(sampleItem1.url)).toBe(true)

      // Modify criteria and filters
      act(() => {
        useFilterStore.getState().setPropertyType('LAKAS')
        useFilterStore.getState().setMinPriceMillion(50)
        useFilterStore.getState().setMaxPriceMillion(100)
        useFilterStore.getState().setSortOption('area_desc')
        useFilterStore.getState().setOnlyBargain(true)
      })

      expect(useFilterStore.getState().criteria.propertyType).toBe('LAKAS')
      expect(useFilterStore.getState().sortOption).toBe('area_desc')
      expect(useFilterStore.getState().onlyBargain).toBe(true)

      // Click reset in Header
      const resetButton = screen.getByTitle('Szűrők alaphelyzetbe állítása')
      fireEvent.click(resetButton)

      // Criteria and UI filters must be reset to defaults
      expect(useFilterStore.getState().criteria.propertyType).toBe('HAZ')
      expect(useFilterStore.getState().criteria.minPriceMillion).toBe(70)
      expect(useFilterStore.getState().criteria.maxPriceMillion).toBe(120)
      expect(useFilterStore.getState().sortOption).toBe('price_asc')
      expect(useFilterStore.getState().onlyBargain).toBe(false)

      // Independent preferences MUST be preserved!
      expect(useUiStore.getState().theme).toBe('light')
      expect(useUiStore.getState().viewMode).toBe('list')
      expect(useUiStore.getState().isStarred(sampleItem1.url)).toBe(true)
    })
  })

  describe('3. Modal and panel open/close workflows', () => {
    it('opens and closes Diagnostics modal via trigger and close button', async () => {
      renderApp()

      expect(screen.queryByText('Portál Diagnosztika & Állapotjelentés')).toBeNull()

      // Open modal
      const diagBtn = screen.getByRole('button', { name: /Diagnosztika/i })
      fireEvent.click(diagBtn)
      expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(true)
      expect(screen.getByText('Portál Diagnosztika & Állapotjelentés')).toBeTruthy()

      // Close modal
      const closeBtn = screen.getByRole('button', { name: 'Bezárás' })
      fireEvent.click(closeBtn)
      expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(false)
      expect(screen.queryByText('Portál Diagnosztika & Állapotjelentés')).toBeNull()
    })

    it('collapses and expands search filter panel, rendering summary chips', () => {
      renderApp()

      const collapseBtn = screen.getByRole('button', { name: /Összecsukás/i })
      expect(screen.queryByTestId('collapsed-filter-summary')).toBeNull()

      // Collapse
      fireEvent.click(collapseBtn)
      expect(useUiStore.getState().isFiltersCollapsed).toBe(true)
      const summary = screen.getByTestId('collapsed-filter-summary')
      expect(summary).toBeTruthy()
      expect(within(summary).getByText(/Hajdúszoboszló/i)).toBeTruthy()

      // Expand
      const expandBtn = screen.getByRole('button', { name: /Kibontás/i })
      fireEvent.click(expandBtn)
      expect(useUiStore.getState().isFiltersCollapsed).toBe(false)
      expect(screen.queryByTestId('collapsed-filter-summary')).toBeNull()
    })
  })

  describe('4. Error handling and retry interaction', () => {
    it('displays error banner on search failure and allows retry', async () => {
      const searchApi = await import('../api/searchApi')
      vi.mocked(searchApi.executeSearch)
        .mockRejectedValueOnce(new Error('Keresési hiba történt a szerveren.'))
        .mockResolvedValueOnce(sampleDashboard)

      renderApp()

      const searchButton = screen.getByRole('button', { name: 'Ingatlanok Keresése' })
      fireEvent.click(searchButton)

      // Error banner should appear
      await waitFor(() => {
        expect(screen.getByTestId('search-error-banner')).toBeTruthy()
        expect(screen.getByText('Hiba történt a keresés során')).toBeTruthy()
      })

      // Retry
      const retryButton = screen.getByRole('button', { name: /Újrapróbálás/i })
      fireEvent.click(retryButton)

      // Successful recovery
      await waitFor(() => {
        expect(screen.queryByTestId('search-error-banner')).toBeNull()
        expect(screen.getByText('Debreceni polgári lakás')).toBeTruthy()
      })
    })
  })

  describe('5. Concurrent requests and stale response protection', () => {
    it('discards slower stale search responses when a newer preset search completes earlier', async () => {
      const searchApi = await import('../api/searchApi')
      let resolveSlow!: (res: DashboardResponseDto) => void
      let resolveFast!: (res: DashboardResponseDto) => void

      const slowPromise = new Promise<DashboardResponseDto>((r) => {
        resolveSlow = r
      })
      const fastPromise = new Promise<DashboardResponseDto>((r) => {
        resolveFast = r
      })

      const fastDashboard: DashboardResponseDto = {
        items: [sampleItem2],
        newCount: 1,
        totalFound: 1,
      }

      const slowDashboard: DashboardResponseDto = {
        items: [sampleItem1],
        newCount: 1,
        totalFound: 1,
      }

      vi.mocked(searchApi.executeSearch)
        .mockImplementationOnce(() => slowPromise)
        .mockImplementationOnce(() => fastPromise)

      renderApp()

      // 1. Trigger first (slow) search via main search button
      const searchButton = screen.getByRole('button', { name: 'Ingatlanok Keresése' })
      fireEvent.click(searchButton)
      await waitFor(() => {
        expect(searchApi.executeSearch).toHaveBeenCalledTimes(1)
      })

      // 2. While search 1 is pending, trigger fast search by selecting preset
      await waitFor(() => {
        expect(screen.getByText('Szegedi Sablon')).toBeTruthy()
      })
      const presetButton = screen.getByText('Szegedi Sablon')
      fireEvent.click(presetButton)
      await waitFor(() => {
        expect(searchApi.executeSearch).toHaveBeenCalledTimes(2)
      })

      // 3. Fast preset search completes first
      await act(async () => {
        resolveFast(fastDashboard)
      })

      await waitFor(() => {
        expect(screen.getByText('Szegedi újszerű ház')).toBeTruthy()
      })

      // 4. Slow search completes later
      await act(async () => {
        resolveSlow(slowDashboard)
      })

      await new Promise((r) => setTimeout(r, 50))

      // 5. Must NOT overwrite the fast search with slow stale results
      expect(screen.getByText('Szegedi újszerű ház')).toBeTruthy()
      expect(screen.queryByText('Debreceni polgári lakás')).toBeNull()
    })
  })
})
