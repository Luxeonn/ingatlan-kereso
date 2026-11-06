import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, within, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type * as SearchApiModule from '../api/searchApi'
import type * as PropertyApiModule from '../api/propertyApi'
import { App } from '../App'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore, DEFAULT_PINNED_QUICK_FILTERS } from '../store/useUiStore'

// Mock @tanstack/react-virtual for JSDOM
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
    getPresets: vi.fn().mockResolvedValue([]),
  }
})

vi.mock('../api/propertyApi', async (importOriginal) => {
  const actual = await importOriginal<typeof PropertyApiModule>()
  return {
    ...actual,
    getHistory: vi.fn().mockResolvedValue([]),
    getHiddenProperties: vi.fn().mockResolvedValue([]),
  }
})

describe('App Header & View Switcher (#359)', () => {
  beforeEach(() => {
    localStorage.clear()
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      dashboardTab: 'listings',
      listingSubView: 'listings',
      pinnedQuickFilters: [...DEFAULT_PINNED_QUICK_FILTERS],
    })
  })

  it('AC1: removes duplicate Megtekintettek rejtése and Rendezés controls from listings-panel-header', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>,
    )

    const header = screen.getByTestId('listings-panel-header')
    expect(header).toBeTruthy()

    // View tabs are present
    expect(within(header).getByTestId('tab-listings')).toBeTruthy()
    expect(within(header).getByTestId('tab-analytics')).toBeTruthy()

    // Duplicate controls are absent from the top view switcher bar
    expect(within(header).queryByRole('button', { name: /Megtekintettek rejtése/i })).toBeNull()
    expect(within(header).queryByLabelText('Rendezés')).toBeNull()
  })

  it('switches between Találatok and Piaci Elemzés tabs cleanly', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>,
    )

    const listingsTab = screen.getByTestId('tab-listings')
    const analyticsTab = screen.getByTestId('tab-analytics')

    expect(listingsTab.getAttribute('aria-pressed')).toBe('true')
    expect(analyticsTab.getAttribute('aria-pressed')).toBe('false')

    fireEvent.click(analyticsTab)
    expect(useUiStore.getState().dashboardTab).toBe('analytics')
  })
})
