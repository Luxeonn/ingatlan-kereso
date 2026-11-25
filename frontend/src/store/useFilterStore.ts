import { create } from 'zustand'
import type { SearchCriteria } from '../types/api'
import { normalizePropertyType } from '../utils/formatters'
import {
  parseUrlParams,
  appendCriteriaParams,
  appendUiFilterParams,
  computeCriteriaHash,
  DEFAULT_PORTALS,
  type SortOption,
} from './filterUrlSync'
import {
  SORT_OPTIONS,
  isSortOption,
  safeSetStorageItem,
  getStoredSortOption,
  getStoredExcludeViewed,
  getStoredExcludeMultiSource,
  getStoredOnlyMultiSource,
  getStoredExcludeNoImage,
  DEFAULT_CRITERIA,
  resolvePortalFilterForPortals,
  sanitizeCriteriaPartial,
  type FilterState,
} from './filterStoreHelpers'

export { DEFAULT_PORTALS, computeCriteriaHash, SORT_OPTIONS, isSortOption, type SortOption }

export const useFilterStore = create<FilterState>((set, get) => {
  const initial = parseUrlParams()
  const initialCriteria: SearchCriteria = {
    ...DEFAULT_CRITERIA,
    excludeViewed: getStoredExcludeViewed(),
    ...(initial.criteria || {}),
  }

  const updateCriteria = (partial: Partial<SearchCriteria>) => {
    set((state) => {
      const sanitized = sanitizeCriteriaPartial(partial)
      const nextCriteria = { ...state.criteria, ...sanitized }
      const nextPortalFilter =
        sanitized.portals !== undefined
          ? resolvePortalFilterForPortals(nextCriteria.portals, state.selectedPortalFilter)
          : state.selectedPortalFilter
      return { criteria: nextCriteria, selectedPortalFilter: nextPortalFilter }
    })
    get().syncToUrl('replace')
  }

  const initialOnlyMulti = initial.onlyMultiSource ?? getStoredOnlyMultiSource()
  const initialExcludeMulti = initialOnlyMulti
    ? false
    : (initial.excludeMultiSource ?? getStoredExcludeMultiSource())

  return {
    criteria: initialCriteria,
    sortOption: initial.sort || getStoredSortOption(),
    onlyNew: initial.onlyNew || false,
    excludeNew: false,
    onlyBargain: initial.onlyBargain || false,
    excludeBargain: false,
    onlyViewed: false,
    excludeNewConstruction: false,
    excludeMultiSource: initialExcludeMulti,
    onlyMultiSource: initialOnlyMulti,
    excludeNoImage: initial.excludeNoImage ?? getStoredExcludeNoImage(),
    onlyNoImage: false,
    includeHidden: false,
    quickSearchQuery: '',
    selectedPortalFilter: resolvePortalFilterForPortals(initialCriteria.portals, 'all'),
    showHidden: false,
    lastExecutedCriteriaHash: null,

    setCriteria: updateCriteria,

    addCity: (city) => {
      const trimmed = city.trim()
      if (!trimmed) return
      const existing = get().criteria.cities || []
      if (existing.includes(trimmed)) return
      const updated = [...existing, trimmed]
      updateCriteria({ cities: updated, city: updated[0] })
    },

    removeCity: (city) => {
      const updated = (get().criteria.cities || []).filter((c) => c !== city)
      const firstCity = updated[0]
      updateCriteria({ cities: updated, city: firstCity ?? 'Hajdúszoboszló' })
    },

    setCities: (cities) => {
      const list = cities.map((c) => c.trim()).filter(Boolean)
      const firstCity = list[0]
      updateCriteria({ cities: list, city: firstCity ?? 'Hajdúszoboszló' })
    },

    setPropertyType: (propertyType) => updateCriteria({ propertyType: normalizePropertyType(propertyType) }),
    setMinPriceMillion: (minPriceMillion) => updateCriteria({ minPriceMillion }),
    setMaxPriceMillion: (maxPriceMillion) => updateCriteria({ maxPriceMillion }),
    setMinArea: (minArea) => updateCriteria({ minArea }),
    setMaxArea: (maxArea) => updateCriteria({ maxArea }),
    setMinRooms: (minRooms) =>
      updateCriteria({ minRooms: minRooms !== undefined && minRooms > 0 ? Math.floor(minRooms) : undefined }),

    togglePortal: (portalId) => {
      const current = get().criteria.portals || []
      const portals = current.includes(portalId)
        ? current.filter((p) => p !== portalId)
        : [...current, portalId]
      updateCriteria({ portals })
    },

    setAllPortals: (portals) => updateCriteria({ portals }),
    setAutoOpen: (autoOpenBrowser) => updateCriteria({ autoOpenBrowser }),

    setExcludeViewed: (excludeViewed) => {
      safeSetStorageItem('preferred_exclude_viewed', String(excludeViewed))
      if (excludeViewed) set({ onlyViewed: false })
      updateCriteria({ excludeViewed })
    },

    setOnlyViewed: (onlyViewed) => {
      if (onlyViewed) {
        safeSetStorageItem('preferred_exclude_viewed', 'false')
        updateCriteria({ excludeViewed: false })
      }
      set({ onlyViewed })
    },

    setOnlyNewConstruction: (onlyNewConstruction) => {
      if (onlyNewConstruction) set({ excludeNewConstruction: false })
      updateCriteria({ onlyNewConstruction })
    },

    setExcludeNewConstruction: (excludeNewConstruction) => {
      if (excludeNewConstruction) updateCriteria({ onlyNewConstruction: false })
      set({ excludeNewConstruction })
    },

    setSortOption: (sortOption) => {
      safeSetStorageItem('preferred_sort_option', sortOption)
      set({ sortOption })
      get().syncToUrl('replace')
    },

    setOnlyNew: (onlyNew) => {
      set(onlyNew ? { onlyNew: true, excludeNew: false } : { onlyNew: false })
      get().syncToUrl('replace')
    },

    setExcludeNew: (excludeNew) => {
      set(excludeNew ? { excludeNew: true, onlyNew: false } : { excludeNew: false })
    },

    setOnlyBargain: (onlyBargain) => {
      set(onlyBargain ? { onlyBargain: true, excludeBargain: false } : { onlyBargain: false })
      get().syncToUrl('replace')
    },

    setExcludeBargain: (excludeBargain) => {
      set(excludeBargain ? { excludeBargain: true, onlyBargain: false } : { excludeBargain: false })
    },

    setExcludeMultiSource: (excludeMultiSource) => {
      safeSetStorageItem('preferred_exclude_multi_source', String(excludeMultiSource))
      if (excludeMultiSource) {
        safeSetStorageItem('preferred_only_multi_source', 'false')
        set({ excludeMultiSource: true, onlyMultiSource: false })
      } else {
        set({ excludeMultiSource: false })
      }
      get().syncToUrl('replace')
    },

    setOnlyMultiSource: (onlyMultiSource) => {
      safeSetStorageItem('preferred_only_multi_source', String(onlyMultiSource))
      if (onlyMultiSource) {
        safeSetStorageItem('preferred_exclude_multi_source', 'false')
        set({ onlyMultiSource: true, excludeMultiSource: false })
      } else {
        set({ onlyMultiSource: false })
      }
      get().syncToUrl('replace')
    },

    setExcludeNoImage: (excludeNoImage) => {
      safeSetStorageItem('preferred_exclude_no_image', String(excludeNoImage))
      set(excludeNoImage ? { excludeNoImage: true, onlyNoImage: false } : { excludeNoImage: false })
      get().syncToUrl('replace')
    },

    setOnlyNoImage: (onlyNoImage) => {
      if (onlyNoImage) safeSetStorageItem('preferred_exclude_no_image', 'false')
      set(onlyNoImage ? { onlyNoImage: true, excludeNoImage: false } : { onlyNoImage: false })
    },

    setQuickSearchQuery: (quickSearchQuery) => set({ quickSearchQuery }),
    setSelectedPortalFilter: (selectedPortalFilter) => set({ selectedPortalFilter }),
    setShowHidden: (showHidden) => set(showHidden ? { showHidden: true, includeHidden: false } : { showHidden: false }),
    setIncludeHidden: (includeHidden) =>
      set(includeHidden ? { includeHidden: true, showHidden: false } : { includeHidden: false }),

    markCriteriaExecuted: (executedCriteria) => {
      const target = executedCriteria ?? get().criteria
      set({ lastExecutedCriteriaHash: computeCriteriaHash(target) })
      get().syncToUrl('push')
    },

    resetToDefaults: () => {
      safeSetStorageItem('preferred_sort_option', 'price_asc')
      safeSetStorageItem('preferred_exclude_viewed', 'false')
      safeSetStorageItem('preferred_exclude_multi_source', 'false')
      safeSetStorageItem('preferred_only_multi_source', 'false')
      safeSetStorageItem('preferred_exclude_no_image', 'false')
      set({
        criteria: { ...DEFAULT_CRITERIA, excludeViewed: false },
        sortOption: 'price_asc',
        onlyNew: false,
        excludeNew: false,
        onlyBargain: false,
        excludeBargain: false,
        onlyViewed: false,
        excludeNewConstruction: false,
        excludeMultiSource: false,
        onlyMultiSource: false,
        excludeNoImage: false,
        onlyNoImage: false,
        includeHidden: false,
        quickSearchQuery: '',
        selectedPortalFilter: 'all',
        showHidden: false,
        lastExecutedCriteriaHash: null,
      })
      if (typeof window !== 'undefined') {
        window.history.replaceState(null, '', window.location.pathname)
      }
    },

    syncFromUrl: () => {
      const parsed = parseUrlParams()
      const nextCriteria = { ...DEFAULT_CRITERIA, ...(parsed.criteria || {}) }
      const nextOnlyMulti = parsed.onlyMultiSource ?? getStoredOnlyMultiSource()
      const nextExcludeMulti = nextOnlyMulti
        ? false
        : (parsed.excludeMultiSource ?? getStoredExcludeMultiSource())
      set((state) => ({
        criteria: nextCriteria,
        sortOption: parsed.sort || getStoredSortOption(),
        onlyNew: parsed.onlyNew ?? false,
        onlyBargain: parsed.onlyBargain ?? false,
        excludeMultiSource: nextExcludeMulti,
        onlyMultiSource: nextOnlyMulti,
        excludeNoImage: parsed.excludeNoImage ?? getStoredExcludeNoImage(),
        selectedPortalFilter: resolvePortalFilterForPortals(nextCriteria.portals, state.selectedPortalFilter),
      }))
    },

    syncToUrl: (historyMode = 'replace') => {
      if (typeof window === 'undefined') return
      const {
        criteria,
        sortOption,
        onlyNew,
        onlyBargain,
        excludeMultiSource,
        onlyMultiSource,
        excludeNoImage,
      } = get()
      const params = new URLSearchParams()
      appendCriteriaParams(params, criteria)
      appendUiFilterParams(params, {
        sortOption,
        onlyNew,
        onlyBargain,
        excludeMultiSource,
        onlyMultiSource,
        excludeNoImage,
      })

      const queryString = params.toString()
      const newUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname
      if (`${window.location.pathname}${window.location.search}` !== newUrl) {
        if (historyMode === 'push') {
          window.history.pushState(null, '', newUrl)
        } else {
          window.history.replaceState(null, '', newUrl)
        }
      }
    },
  }
})

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', () => useFilterStore.getState().syncFromUrl())
}
