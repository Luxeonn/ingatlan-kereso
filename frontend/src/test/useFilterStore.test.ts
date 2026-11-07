import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useFilterStore, SORT_OPTIONS, DEFAULT_PORTALS } from '../store/useFilterStore'

describe('useFilterStore', () => {
  const originalLocation = window.location
  const originalPushState = window.history.pushState
  const originalReplaceState = window.history.replaceState

  function setLocation(url: string) {
    Object.defineProperty(window, 'location', {
      value: new URL(url),
      writable: true,
      configurable: true,
    })
  }

  beforeEach(() => {
    localStorage.clear()
    setLocation('https://example.com/')
    window.history.pushState = vi.fn()
    window.history.replaceState = vi.fn()
    useFilterStore.getState().resetToDefaults()
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
      configurable: true,
    })
    window.history.pushState = originalPushState
    window.history.replaceState = originalReplaceState
  })

  describe('initial state and options', () => {
    it('initializes with default search criteria and sort options', () => {
      const state = useFilterStore.getState()
      expect(state.criteria.cities).toEqual(['Hajdúszoboszló'])
      expect(state.criteria.propertyType).toBe('haz')
      expect(state.criteria.minPriceMillion).toBe(70)
      expect(state.criteria.maxPriceMillion).toBe(120)
      expect(state.criteria.portals).toEqual(DEFAULT_PORTALS)
      expect(state.sortOption).toBe('price_asc')
      expect(SORT_OPTIONS).toHaveLength(5)
    })
  })

  describe('city management actions', () => {
    it('adds unique cities and trims input whitespace', () => {
      useFilterStore.getState().addCity('  Debrecen  ')
      expect(useFilterStore.getState().criteria.cities).toEqual(['Hajdúszoboszló', 'Debrecen'])
      expect(useFilterStore.getState().criteria.city).toBe('Hajdúszoboszló')

      // Adding duplicate city is ignored
      useFilterStore.getState().addCity('Debrecen')
      expect(useFilterStore.getState().criteria.cities).toEqual(['Hajdúszoboszló', 'Debrecen'])

      // Adding empty string is ignored
      useFilterStore.getState().addCity('   ')
      expect(useFilterStore.getState().criteria.cities).toEqual(['Hajdúszoboszló', 'Debrecen'])
    })

    it('removes city and updates primary city to first remaining or default', () => {
      useFilterStore.getState().setCities(['Debrecen', 'Szeged', 'Miskolc'])
      expect(useFilterStore.getState().criteria.city).toBe('Debrecen')

      useFilterStore.getState().removeCity('Debrecen')
      expect(useFilterStore.getState().criteria.cities).toEqual(['Szeged', 'Miskolc'])
      expect(useFilterStore.getState().criteria.city).toBe('Szeged')

      // Removing all falls back to default city
      useFilterStore.getState().setCities([])
      expect(useFilterStore.getState().criteria.city).toBe('Hajdúszoboszló')
    })
  })

  describe('criteria property setters', () => {
    it('updates property type and numeric ranges', () => {
      useFilterStore.getState().setPropertyType('lakas')
      expect(useFilterStore.getState().criteria.propertyType).toBe('lakas')

      useFilterStore.getState().setMinPriceMillion(45)
      useFilterStore.getState().setMaxPriceMillion(95)
      expect(useFilterStore.getState().criteria.minPriceMillion).toBe(45)
      expect(useFilterStore.getState().criteria.maxPriceMillion).toBe(95)

      useFilterStore.getState().setMinArea(50)
      useFilterStore.getState().setMaxArea(110)
      expect(useFilterStore.getState().criteria.minArea).toBe(50)
      expect(useFilterStore.getState().criteria.maxArea).toBe(110)
    })

    it('toggles portal selection and automatically adjusts selectedPortalFilter', () => {
      expect(useFilterStore.getState().criteria.portals).toContain('dh')

      // Toggle off 'dh'
      useFilterStore.getState().togglePortal('dh')
      expect(useFilterStore.getState().criteria.portals).not.toContain('dh')

      // Toggle back on
      useFilterStore.getState().togglePortal('dh')
      expect(useFilterStore.getState().criteria.portals).toContain('dh')

      // If only single portal remains, selectedPortalFilter automatically aligns
      useFilterStore.getState().setAllPortals(['oc'])
      expect(useFilterStore.getState().selectedPortalFilter).toBe('oc')

      // If current filter is removed, falls back to 'all'
      useFilterStore.getState().setAllPortals(['dh', 'jofogas'])
      expect(useFilterStore.getState().selectedPortalFilter).toBe('all')
    })

    it('updates boolean search flags and persists preferences in localStorage', () => {
      useFilterStore.getState().setAutoOpen(true)
      expect(useFilterStore.getState().criteria.autoOpenBrowser).toBe(true)

      useFilterStore.getState().setExcludeViewed(true)
      expect(useFilterStore.getState().criteria.excludeViewed).toBe(true)
      expect(localStorage.getItem('preferred_exclude_viewed')).toBe('true')

      useFilterStore.getState().setOnlyNewConstruction(true)
      expect(useFilterStore.getState().criteria.onlyNewConstruction).toBe(true)
    })
  })

  describe('UI filters, sorting, and search query', () => {
    it('updates sortOption and persists preferred_sort_option', () => {
      useFilterStore.getState().setSortOption('area_desc')
      expect(useFilterStore.getState().sortOption).toBe('area_desc')
      expect(localStorage.getItem('preferred_sort_option')).toBe('area_desc')
    })

    it('updates onlyNew, onlyBargain, and minQualityScore', () => {
      useFilterStore.getState().setOnlyNew(true)
      expect(useFilterStore.getState().onlyNew).toBe(true)

      useFilterStore.getState().setOnlyBargain(true)
      expect(useFilterStore.getState().onlyBargain).toBe(true)

      useFilterStore.getState().setMinQualityScore(85)
      expect(useFilterStore.getState().minQualityScore).toBe(85)
    })

    it('updates quickSearchQuery, selectedPortalFilter, and showHidden', () => {
      useFilterStore.getState().setQuickSearchQuery('felújított terasz')
      expect(useFilterStore.getState().quickSearchQuery).toBe('felújított terasz')

      useFilterStore.getState().setSelectedPortalFilter('jofogas')
      expect(useFilterStore.getState().selectedPortalFilter).toBe('jofogas')

      useFilterStore.getState().setShowHidden(true)
      expect(useFilterStore.getState().showHidden).toBe(true)
    })
  })

  describe('resetToDefaults & URL synchronization', () => {
    it('resets all state values and clears URL query string', () => {
      useFilterStore.getState().addCity('Debrecen')
      useFilterStore.getState().setOnlyBargain(true)
      useFilterStore.getState().setSortOption('bargain_desc')
      useFilterStore.getState().setMinQualityScore(70)

      useFilterStore.getState().resetToDefaults()

      const state = useFilterStore.getState()
      expect(state.criteria.cities).toEqual(['Hajdúszoboszló'])
      expect(state.onlyBargain).toBe(false)
      expect(state.sortOption).toBe('price_asc')
      expect(state.minQualityScore).toBe(0)
      expect(state.quickSearchQuery).toBe('')
      expect(state.selectedPortalFilter).toBe('all')
      expect(window.history.replaceState).toHaveBeenCalled()
    })

    it('synchronizes state from URL query parameters via syncFromUrl', () => {
      setLocation(
        'https://example.com/?cities=Debrecen,Nyíregyháza&type=lakas&minPrice=40&maxPrice=80&sort=price_desc&onlyBargain=true&minQuality=75',
      )

      useFilterStore.getState().syncFromUrl()

      const state = useFilterStore.getState()
      expect(state.criteria.cities).toEqual(['Debrecen', 'Nyíregyháza'])
      expect(state.criteria.propertyType).toBe('lakas')
      expect(state.criteria.minPriceMillion).toBe(40)
      expect(state.criteria.maxPriceMillion).toBe(80)
      expect(state.sortOption).toBe('price_desc')
      expect(state.onlyBargain).toBe(true)
      expect(state.minQualityScore).toBe(75)
    })
  })
})
