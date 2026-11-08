import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  useUiStore,
  DEFAULT_PINNED_QUICK_FILTERS,
  MAX_PINNED_QUICK_FILTERS,
  type QuickFilterId,
} from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'

describe('useUiStore', () => {
  beforeEach(() => {
    localStorage.clear()
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
    document.documentElement.classList.remove('dark')
  })

  describe('theme toggling', () => {
    it('toggles theme between dark and light, updates DOM and localStorage', () => {
      expect(useUiStore.getState().theme).toBe('dark')

      useUiStore.getState().toggleTheme()
      expect(useUiStore.getState().theme).toBe('light')
      expect(localStorage.getItem('theme')).toBe('light')
      expect(document.documentElement.classList.contains('dark')).toBe(false)

      useUiStore.getState().toggleTheme()
      expect(useUiStore.getState().theme).toBe('dark')
      expect(localStorage.getItem('theme')).toBe('dark')
      expect(document.documentElement.classList.contains('dark')).toBe(true)
    })
  })

  describe('viewMode and layout', () => {
    it('updates view mode between grid and list', () => {
      expect(useUiStore.getState().viewMode).toBe('grid')
      useUiStore.getState().setViewMode('list')
      expect(useUiStore.getState().viewMode).toBe('list')

      // No-op for identical value
      useUiStore.getState().setViewMode('list')
      expect(useUiStore.getState().viewMode).toBe('list')
    })

    it('toggles collapsible filters state and diagnostics modal state', () => {
      useUiStore.getState().setFiltersCollapsed(true)
      expect(useUiStore.getState().isFiltersCollapsed).toBe(true)

      useUiStore.getState().setDiagnosticsModalOpen(true)
      expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(true)
    })
  })

  describe('tabs and subviews', () => {
    it('switches dashboardTab between listings and analytics', () => {
      useUiStore.getState().setDashboardTab('analytics')
      expect(useUiStore.getState().dashboardTab).toBe('analytics')

      useUiStore.getState().setDashboardTab('listings')
      expect(useUiStore.getState().dashboardTab).toBe('listings')
    })

    it('switches listingSubView and automatically activates listings tab', () => {
      useUiStore.getState().setDashboardTab('analytics')

      useUiStore.getState().setListingSubView('favorites')
      expect(useUiStore.getState().listingSubView).toBe('favorites')
      expect(useUiStore.getState().dashboardTab).toBe('listings')

      useUiStore.getState().setListingSubView('archive')
      expect(useUiStore.getState().listingSubView).toBe('archive')
      expect(useUiStore.getState().dashboardTab).toBe('listings')
    })
  })

  describe('favorites management (starredUrls & cache)', () => {
    const mockItem = createMockIngatlanItem({
      id: 'fav-1',
      url: 'https://dh.hu/ingatlan/fav-1',
      title: 'Kedvenc lakás',
    })

    it('stars an item, caches details, and toggles off to unstar', () => {
      expect(useUiStore.getState().isStarred(mockItem.url)).toBe(false)

      // Star item
      useUiStore.getState().toggleStarred(mockItem)
      expect(useUiStore.getState().isStarred(mockItem.url)).toBe(true)
      expect(useUiStore.getState().starredUrls).toContain(mockItem.url)
      expect(useUiStore.getState().starredItemsCache[mockItem.url]).toEqual(mockItem)
      expect(JSON.parse(localStorage.getItem('starred_urls') || '[]')).toContain(mockItem.url)

      // Unstar item
      useUiStore.getState().toggleStarred(mockItem.url)
      expect(useUiStore.getState().isStarred(mockItem.url)).toBe(false)
      expect(useUiStore.getState().starredUrls).not.toContain(mockItem.url)
      expect(useUiStore.getState().starredItemsCache[mockItem.url]).toBeUndefined()
    })

    it('ignores toggling starred for empty or invalid item', () => {
      const stateBefore = useUiStore.getState()
      useUiStore.getState().toggleStarred('')
      expect(useUiStore.getState().starredUrls).toEqual(stateBefore.starredUrls)
    })

    it('handles localStorage quota exceeded during starring without throwing', () => {
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementationOnce(() => {
        throw new Error('QuotaExceededError')
      })

      expect(() => {
        useUiStore.getState().toggleStarred(mockItem)
      }).not.toThrow()

      expect(useUiStore.getState().isStarred(mockItem.url)).toBe(true)
      setItemSpy.mockRestore()
    })
  })

  describe('pinned quick filters', () => {
    it('toggles pinning and unpinning filters up to maximum limit', () => {
      useUiStore.setState({ pinnedQuickFilters: ['bargain', 'onlyNew'] })

      // Add 3rd pinned filter
      useUiStore.getState().togglePinnedQuickFilter('excludeViewed')
      expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew', 'excludeViewed'])

      // Attempt to add 4th: should be blocked by MAX_PINNED_QUICK_FILTERS (3)
      useUiStore.getState().togglePinnedQuickFilter('onlyNewConstruction')
      expect(useUiStore.getState().pinnedQuickFilters).toHaveLength(MAX_PINNED_QUICK_FILTERS)
      expect(useUiStore.getState().pinnedQuickFilters).not.toContain('onlyNewConstruction')

      // Unpin one
      useUiStore.getState().togglePinnedQuickFilter('onlyNew')
      expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'excludeViewed'])
    })

    it('validates and clamps custom pinned quick filter lists', () => {
      useUiStore.getState().setPinnedQuickFilters([
        'bargain',
        'onlyNew',
        'hidden',
        'excludeViewed' as QuickFilterId, // 4th item
      ])
      expect(useUiStore.getState().pinnedQuickFilters).toHaveLength(MAX_PINNED_QUICK_FILTERS)
      expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew', 'hidden'])
    })
  })
})
