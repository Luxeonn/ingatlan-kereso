import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useSearch } from '../hooks/useSearch'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'
import * as searchApi from '../api/searchApi'
import * as propertyApi from '../api/propertyApi'
import * as formatters from '../utils/formatters'
import type { DashboardResponseDto } from '../types/api'

vi.mock('../api/searchApi', () => ({
  executeSearch: vi.fn(),
}))

vi.mock('../api/propertyApi', () => ({
  markAsViewed: vi.fn(),
  openUrls: vi.fn(),
  getHistory: vi.fn(),
  clearHistory: vi.fn(),
  hideProperty: vi.fn(),
  restoreProperty: vi.fn(),
  getHiddenProperties: vi.fn(),
}))

describe('useSearch custom hook and race condition protection (#378)', () => {
  let queryClient: QueryClient

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )

  const mockItem1 = createMockIngatlanItem({
    id: 'item-1',
    url: 'https://dh.hu/ingatlan/1',
    title: 'Debreceni felújított tégla lakás',
    city: 'Debrecen',
    viewed: false,
    hidden: false,
    priceInfo: { amountHuf: 45_000_000, amountMillion: 45 },
  })

  const mockItem2 = createMockIngatlanItem({
    id: 'item-2',
    url: 'https://oc.hu/ingatlan/2',
    title: 'Nagy kertes családi ház',
    city: 'Szeged',
    viewed: false,
    hidden: false,
    priceInfo: { amountHuf: 85_000_000, amountMillion: 85 },
  })

  const mockDashboard: DashboardResponseDto = {
    items: [mockItem1, mockItem2],
    newCount: 2,
    totalFound: 2,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    })

    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      dashboardTab: 'listings',
      listingSubView: 'listings',
      starredUrls: [],
      starredItemsCache: {},
    })

    vi.mocked(propertyApi.getHistory).mockResolvedValue([])
    vi.mocked(propertyApi.getHiddenProperties).mockResolvedValue([])
    vi.mocked(searchApi.executeSearch).mockResolvedValue(mockDashboard)
    vi.mocked(propertyApi.markAsViewed).mockResolvedValue({ success: true } as never)
    vi.mocked(propertyApi.hideProperty).mockResolvedValue({ success: true } as never)
    vi.mocked(propertyApi.restoreProperty).mockResolvedValue({ success: true } as never)
    vi.mocked(propertyApi.clearHistory).mockResolvedValue({ success: true } as never)
    vi.mocked(propertyApi.openUrls).mockResolvedValue({ success: true } as never)
  })

  it('initializes with default empty state', () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    expect(result.current.data).toBeNull()
    expect(result.current.items).toEqual([])
    expect(result.current.rawCount).toBe(0)
    expect(result.current.toastMessage).toBeNull()
    expect(result.current.isLoading).toBe(false)
  })

  it('triggers search mutation and updates results state', async () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    act(() => {
      result.current.search({ city: 'Debrecen' })
    })

    await waitFor(() => {
      expect(result.current.data).toEqual(mockDashboard)
    })

    expect(result.current.items).toHaveLength(2)
    expect(result.current.rawCount).toBe(2)
    expect(searchApi.executeSearch).toHaveBeenCalledWith(
      expect.objectContaining({ city: 'Debrecen' }),
    )
  })

  it('marks listing as viewed optimistically and updates state', async () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    act(() => {
      result.current.search({ city: 'Debrecen' })
    })

    await waitFor(() => expect(result.current.items).toHaveLength(2))

    act(() => {
      result.current.markAsViewed(mockItem1.url)
    })

    await waitFor(() => {
      const updatedItem = result.current.items.find((i) => i.id === 'item-1')
      expect(updatedItem?.viewed).toBe(true)
    })
    expect(propertyApi.markAsViewed).toHaveBeenCalledWith(mockItem1.url)
  })

  it('hides and restores property with undo toast feedback', async () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    act(() => {
      result.current.search({ city: 'Debrecen' })
    })
    await waitFor(() => expect(result.current.items).toHaveLength(2))

    // 1. Hide item
    act(() => {
      result.current.hideProperty(mockItem1)
    })

    await waitFor(() => {
      expect(result.current.items).toHaveLength(1)
      expect(result.current.toastMessage).toBe('Hirdetés elrejtve')
      expect(result.current.toastUndoItem?.id).toBe(mockItem1.id)
    })
    expect(propertyApi.hideProperty).toHaveBeenCalledWith(mockItem1.id, [mockItem1.url])

    // 2. Restore item
    act(() => {
      result.current.restoreProperty(mockItem1)
    })

    await waitFor(() => {
      expect(result.current.items).toHaveLength(2)
      expect(result.current.toastMessage).toBeNull()
      expect(result.current.toastUndoItem).toBeNull()
    })
    expect(propertyApi.restoreProperty).toHaveBeenCalledWith(mockItem1.id, [mockItem1.url])
  })

  it('clears toast notification via clearToast', async () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    act(() => {
      result.current.search({ city: 'Debrecen' })
    })
    await waitFor(() => expect(result.current.items).toHaveLength(2))

    act(() => {
      result.current.hideProperty(mockItem1)
    })
    await waitFor(() => expect(result.current.toastMessage).toBe('Hirdetés elrejtve'))

    act(() => {
      result.current.clearToast()
    })

    expect(result.current.toastMessage).toBeNull()
    expect(result.current.toastUndoItem).toBeNull()
  })

  it('resolves favorite items and falls back to buildOrphanItem for uncached URLs', async () => {
    const cachedFav = createMockIngatlanItem({
      id: 'cached-1',
      url: 'https://dh.hu/ingatlan/cached-1',
      title: 'Cache-elt kedvenc',
    })
    const orphanUrl = 'https://oc.hu/ingatlan/orphan-99'

    useUiStore.setState({
      starredUrls: [cachedFav.url, orphanUrl],
      starredItemsCache: { [cachedFav.url]: cachedFav },
    })

    const { result } = renderHook(() => useSearch(), { wrapper })

    expect(result.current.favoriteItems).toHaveLength(2)
    const cached = result.current.favoriteItems.find((i) => i.url === cachedFav.url)
    const orphan = result.current.favoriteItems.find((i) => i.url === orphanUrl)

    expect(cached?.title).toBe('Cache-elt kedvenc')
    expect(orphan?.title).toContain('Elmentett hirdetés')
    expect(orphan?.portal).toBe('Otthon Centrum')
  })

  it('opens all tabs in external browser and marks URLs as viewed', async () => {
    const openSpy = vi.spyOn(formatters, 'openExternalUrl').mockImplementation(() => {})
    const { result } = renderHook(() => useSearch(), { wrapper })

    act(() => {
      result.current.openAllTabs([mockItem1, mockItem2])
    })

    expect(openSpy).toHaveBeenCalledWith(mockItem1.url)
    expect(openSpy).toHaveBeenCalledWith(mockItem2.url)
    await waitFor(() => {
      expect(propertyApi.openUrls).toHaveBeenCalledWith([mockItem1.url, mockItem2.url])
    })

    openSpy.mockRestore()
  })

  it('clears archive history asynchronously', async () => {
    const { result } = renderHook(() => useSearch(), { wrapper })

    await act(async () => {
      await result.current.clearArchiveHistory('Debrecen')
    })

    expect(propertyApi.clearHistory).toHaveBeenCalledWith('Debrecen')
  })

  it('discards earlier slow search response when newer fast search resolves first', async () => {
    let resolveSlow!: (val: DashboardResponseDto) => void
    let resolveFast!: (val: DashboardResponseDto) => void

    const slowPromise = new Promise<DashboardResponseDto>((resolve) => {
      resolveSlow = resolve
    })
    const fastPromise = new Promise<DashboardResponseDto>((resolve) => {
      resolveFast = resolve
    })

    const slowDashboard: DashboardResponseDto = {
      items: [mockItem1],
      newCount: 1,
      totalFound: 1,
    }

    const fastDashboard: DashboardResponseDto = {
      items: [mockItem2],
      newCount: 1,
      totalFound: 1,
    }

    vi.mocked(searchApi.executeSearch)
      .mockImplementationOnce(() => slowPromise)
      .mockImplementationOnce(() => fastPromise)

    const { result } = renderHook(() => useSearch(), { wrapper })

    // 1. Trigger slow search
    act(() => {
      result.current.search({ city: 'Debrecen' })
    })
    expect(result.current.isLoading).toBe(true)

    // 2. Trigger fast search while slow is pending
    act(() => {
      result.current.search({ city: 'Szeged' })
    })

    // 3. Fast search finishes first
    await act(async () => {
      resolveFast(fastDashboard)
    })

    await waitFor(() => {
      expect(result.current.items).toHaveLength(1)
      expect(result.current.items[0]?.id).toBe('item-2')
      expect(result.current.isLoading).toBe(false)
    })

    // 4. Slow search finishes afterwards
    await act(async () => {
      resolveSlow(slowDashboard)
    })

    // 5. Must NOT overwrite with stale slow data
    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0]?.id).toBe('item-2')
    expect(result.current.isLoading).toBe(false)
  })

  it('ignores errors from superseded search requests when a newer request is active', async () => {
    let rejectSlow!: (err: Error) => void
    let resolveFast!: (val: DashboardResponseDto) => void

    const slowPromise = new Promise<DashboardResponseDto>((_, reject) => {
      rejectSlow = reject
    })
    const fastPromise = new Promise<DashboardResponseDto>((resolve) => {
      resolveFast = resolve
    })

    const fastDashboard: DashboardResponseDto = {
      items: [mockItem2],
      newCount: 1,
      totalFound: 1,
    }

    vi.mocked(searchApi.executeSearch)
      .mockImplementationOnce(() => slowPromise)
      .mockImplementationOnce(() => fastPromise)

    const { result } = renderHook(() => useSearch(), { wrapper })

    // 1. Trigger search 1 (slow, will fail)
    act(() => {
      result.current.search({ city: 'Debrecen' })
    })

    // 2. Trigger search 2 (fast, will succeed)
    act(() => {
      result.current.search({ city: 'Szeged' })
    })

    // 3. Search 2 succeeds
    await act(async () => {
      resolveFast(fastDashboard)
    })

    await waitFor(() => {
      expect(result.current.items).toHaveLength(1)
      expect(result.current.items[0]?.id).toBe('item-2')
    })

    // 4. Search 1 rejects
    await act(async () => {
      rejectSlow(new Error('Slow search failed'))
    })

    // 5. Active successful state must remain intact
    expect(result.current.isError).toBe(false)
    expect(result.current.error).toBeNull()
    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0]?.id).toBe('item-2')
  })
})
