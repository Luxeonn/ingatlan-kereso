import { create } from 'zustand'
import type { SearchCriteria } from '../types/api'
import { normalizePortalKey } from '../utils/formatters'
import {
  parseUrlParams,
  appendCriteriaParams,
  appendUiFilterParams,
  DEFAULT_PORTALS,
  type SortOption,
} from './filterUrlSync'

export { DEFAULT_PORTALS, type SortOption }

export const SORT_OPTIONS: ReadonlyArray<{ value: SortOption; label: string }> = [
  { value: 'price_asc', label: 'Ár: Növekvő (alapértelmezett)' },
  { value: 'price_desc', label: 'Ár: Csökkenő' },
  { value: 'area_desc', label: 'Méret: Legnagyobb elöl' },
  { value: 'date_desc', label: 'Felfedezés: Legfrissebb' },
  { value: 'bargain_desc', label: 'Alkalmi vétel: Legnagyobb kedvezmény elöl' },
]

const VALID_SORTS: SortOption[] = SORT_OPTIONS.map((o) => o.value)

function safeGetStorageItem(key: string): string | null {
  if (typeof window === 'undefined') return null
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSetStorageItem(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage errors
  }
}

function getStoredSortOption(): SortOption {
  const saved = safeGetStorageItem('preferred_sort_option') as SortOption | null
  return saved && VALID_SORTS.includes(saved) ? saved : 'price_asc'
}

function getStoredExcludeViewed(): boolean {
  return safeGetStorageItem('preferred_exclude_viewed') === 'true'
}

function getStoredExcludeMultiSource(): boolean {
  return safeGetStorageItem('preferred_exclude_multi_source') === 'true'
}

const DEFAULT_CRITERIA: SearchCriteria = {
  cities: ['Hajdúszoboszló'],
  city: 'Hajdúszoboszló',
  propertyType: 'haz',
  minPriceMillion: 70,
  maxPriceMillion: 120,
  minArea: 90,
  maxArea: 200,
  portals: [...DEFAULT_PORTALS],
  autoOpenBrowser: false,
  excludeViewed: false,
  onlyDiscounted: false,
  onlyNewConstruction: false,
}


interface FilterState {
  criteria: SearchCriteria
  sortOption: SortOption
  onlyNew: boolean
  onlyBargain: boolean
  excludeMultiSource: boolean
  minQualityScore: number
  quickSearchQuery: string
  selectedPortalFilter: string
  showHidden: boolean

  setCriteria: (criteria: Partial<SearchCriteria>) => void
  addCity: (city: string) => void
  removeCity: (city: string) => void
  setCities: (cities: string[]) => void
  setPropertyType: (type: string) => void
  setMinPriceMillion: (min: number) => void
  setMaxPriceMillion: (max: number) => void
  setMinArea: (min: number) => void
  setMaxArea: (max: number) => void
  togglePortal: (portalId: string) => void
  setAllPortals: (portals: string[]) => void
  setAutoOpen: (auto: boolean) => void
  setExcludeViewed: (exclude: boolean) => void
  setOnlyNewConstruction: (onlyNewConstruction: boolean) => void
  setSortOption: (sort: SortOption) => void
  setOnlyNew: (onlyNew: boolean) => void
  setOnlyBargain: (onlyBargain: boolean) => void
  setExcludeMultiSource: (excludeMultiSource: boolean) => void
  setMinQualityScore: (score: number) => void
  setQuickSearchQuery: (query: string) => void
  setSelectedPortalFilter: (portal: string) => void
  setShowHidden: (show: boolean) => void
  resetToDefaults: () => void
  syncFromUrl: () => void
  syncToUrl: () => void
}


function resolvePortalFilterForPortals(portals: string[] | undefined, currentFilter: string): string {
  const normalized = (portals || []).map(normalizePortalKey).filter(Boolean)
  if (normalized.length === 1) return normalized[0]
  if (currentFilter !== 'all' && !normalized.includes(currentFilter)) return 'all'
  return currentFilter
}

export const useFilterStore = create<FilterState>((set, get) => {
  const initial = parseUrlParams()
  const initialCriteria: SearchCriteria = {
    ...DEFAULT_CRITERIA,
    excludeViewed: getStoredExcludeViewed(),
    ...(initial.criteria || {}),
  }

  const updateCriteria = (partial: Partial<SearchCriteria>) => {
    set((state) => {
      const nextCriteria = { ...state.criteria, ...partial }
      const nextPortalFilter =
        partial.portals !== undefined
          ? resolvePortalFilterForPortals(nextCriteria.portals, state.selectedPortalFilter)
          : state.selectedPortalFilter
      return { criteria: nextCriteria, selectedPortalFilter: nextPortalFilter }
    })
    get().syncToUrl()
  }

  return {
    criteria: initialCriteria,
    sortOption: initial.sort || getStoredSortOption(),
    onlyNew: initial.onlyNew || false,
    onlyBargain: initial.onlyBargain || false,
    excludeMultiSource: initial.excludeMultiSource ?? getStoredExcludeMultiSource(),
    minQualityScore: initial.minQualityScore || 0,
    quickSearchQuery: '',
    selectedPortalFilter: resolvePortalFilterForPortals(initialCriteria.portals, 'all'),
    showHidden: false,

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
      updateCriteria({ cities: updated, city: updated[0] || 'Hajdúszoboszló' })
    },

    setCities: (cities) => {
      const list = cities.map((c) => c.trim()).filter(Boolean)
      updateCriteria({ cities: list, city: list[0] || 'Hajdúszoboszló' })
    },

    setPropertyType: (propertyType) => updateCriteria({ propertyType }),
    setMinPriceMillion: (minPriceMillion) => updateCriteria({ minPriceMillion }),
    setMaxPriceMillion: (maxPriceMillion) => updateCriteria({ maxPriceMillion }),
    setMinArea: (minArea) => updateCriteria({ minArea }),
    setMaxArea: (maxArea) => updateCriteria({ maxArea }),

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
      updateCriteria({ excludeViewed })
    },

    setOnlyNewConstruction: (onlyNewConstruction) => updateCriteria({ onlyNewConstruction }),

    setSortOption: (sortOption) => {
      safeSetStorageItem('preferred_sort_option', sortOption)
      set({ sortOption })
      get().syncToUrl()
    },

    setOnlyNew: (onlyNew) => {
      set({ onlyNew })
      get().syncToUrl()
    },

    setOnlyBargain: (onlyBargain) => {
      set({ onlyBargain })
      get().syncToUrl()
    },

    setExcludeMultiSource: (excludeMultiSource) => {
      safeSetStorageItem('preferred_exclude_multi_source', String(excludeMultiSource))
      set({ excludeMultiSource })
      get().syncToUrl()
    },

    setMinQualityScore: (minQualityScore) => {
      set({ minQualityScore })
      get().syncToUrl()
    },

    setQuickSearchQuery: (quickSearchQuery) => set({ quickSearchQuery }),
    setSelectedPortalFilter: (selectedPortalFilter) => set({ selectedPortalFilter }),
    setShowHidden: (showHidden) => set({ showHidden }),

    resetToDefaults: () => {
      safeSetStorageItem('preferred_sort_option', 'price_asc')
      safeSetStorageItem('preferred_exclude_viewed', 'false')
      safeSetStorageItem('preferred_exclude_multi_source', 'false')
      set({
        criteria: { ...DEFAULT_CRITERIA, excludeViewed: false },
        sortOption: 'price_asc',
        onlyNew: false,
        onlyBargain: false,
        excludeMultiSource: false,
        minQualityScore: 0,
        quickSearchQuery: '',
        selectedPortalFilter: 'all',
        showHidden: false,
      })
      if (typeof window !== 'undefined') {
        window.history.replaceState(null, '', window.location.pathname)
      }
    },

    syncFromUrl: () => {
      const parsed = parseUrlParams()
      const nextCriteria = { ...DEFAULT_CRITERIA, ...(parsed.criteria || {}) }
      set((state) => ({
        criteria: nextCriteria,
        sortOption: parsed.sort || getStoredSortOption(),
        onlyNew: parsed.onlyNew ?? false,
        onlyBargain: parsed.onlyBargain ?? false,
        excludeMultiSource: parsed.excludeMultiSource ?? getStoredExcludeMultiSource(),
        minQualityScore: parsed.minQualityScore ?? 0,
        selectedPortalFilter: resolvePortalFilterForPortals(nextCriteria.portals, state.selectedPortalFilter),
      }))
    },

    syncToUrl: () => {
      if (typeof window === 'undefined') return
      const { criteria, sortOption, onlyNew, onlyBargain, excludeMultiSource, minQualityScore } = get()
      const params = new URLSearchParams()
      appendCriteriaParams(params, criteria)
      appendUiFilterParams(params, {
        sortOption,
        onlyNew,
        onlyBargain,
        excludeMultiSource,
        minQualityScore,
      })

      const queryString = params.toString()
      const newUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname
      if (`${window.location.pathname}${window.location.search}` !== newUrl) {
        window.history.pushState(null, '', newUrl)
      }
    },
  }
})

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', () => useFilterStore.getState().syncFromUrl())
}
