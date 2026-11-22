import { create } from 'zustand'
import type { SearchCriteria } from '../types/api'
import type { PropertyType } from '../types/property'
import type { PortalId, PortalFilterOption } from '../types/portal'
import { normalizePortalKey, normalizePropertyType } from '../utils/formatters'
import { safeParseSortOption, safeParseStorageBoolean } from '../utils/validation'
import {
  parseUrlParams,
  appendCriteriaParams,
  appendUiFilterParams,
  computeCriteriaHash,
  DEFAULT_PORTALS,
  type SortOption,
} from './filterUrlSync'

export { DEFAULT_PORTALS, computeCriteriaHash, type SortOption }

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

export function isSortOption(val: unknown): val is SortOption {
  return typeof val === 'string' && VALID_SORTS.includes(val as SortOption)
}

function getStoredSortOption(): SortOption {
  const saved = safeGetStorageItem('preferred_sort_option')
  return safeParseSortOption(saved)
}

function getStoredExcludeViewed(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_viewed'))
}

function getStoredExcludeMultiSource(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_multi_source'))
}

function getStoredOnlyMultiSource(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_only_multi_source'))
}

function getStoredExcludeNoImage(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_no_image'))
}

const DEFAULT_CRITERIA: SearchCriteria = {
  cities: ['Hajdúszoboszló'],
  city: 'Hajdúszoboszló',
  propertyType: 'HAZ',
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
  onlyMultiSource: boolean
  excludeNoImage: boolean
  minQualityScore: number
  quickSearchQuery: string
  selectedPortalFilter: PortalFilterOption | string
  showHidden: boolean
  lastExecutedCriteriaHash: string | null

  setCriteria: (criteria: Partial<SearchCriteria>) => void
  addCity: (city: string) => void
  removeCity: (city: string) => void
  setCities: (cities: string[]) => void
  setPropertyType: (type: PropertyType | string) => void
  setMinPriceMillion: (min: number) => void
  setMaxPriceMillion: (max: number) => void
  setMinArea: (min: number) => void
  setMaxArea: (max: number) => void
  setMinRooms: (minRooms: number | undefined) => void
  togglePortal: (portalId: PortalId | string) => void
  setAllPortals: (portals: (PortalId | string)[]) => void
  setAutoOpen: (auto: boolean) => void
  setExcludeViewed: (exclude: boolean) => void
  setOnlyDiscounted: (onlyDiscounted: boolean) => void
  setOnlyNewConstruction: (onlyNewConstruction: boolean) => void
  setSortOption: (sort: SortOption) => void
  setOnlyNew: (onlyNew: boolean) => void
  setOnlyBargain: (onlyBargain: boolean) => void
  setExcludeMultiSource: (excludeMultiSource: boolean) => void
  setOnlyMultiSource: (onlyMultiSource: boolean) => void
  setExcludeNoImage: (excludeNoImage: boolean) => void
  setMinQualityScore: (score: number) => void
  setQuickSearchQuery: (query: string) => void
  setSelectedPortalFilter: (portal: PortalFilterOption | string) => void
  setShowHidden: (show: boolean) => void
  markCriteriaExecuted: (executedCriteria?: SearchCriteria) => void
  resetToDefaults: () => void
  syncFromUrl: () => void
  syncToUrl: (historyMode?: 'replace' | 'push') => void
}

function resolvePortalFilterForPortals(portals: string[] | undefined, currentFilter: string): string {
  const normalized = (portals || []).map(normalizePortalKey).filter((k): k is PortalId => Boolean(k) && k !== 'egyeb')
  const firstNorm = normalized[0]
  if (firstNorm && normalized.length === 1) return firstNorm
  if (currentFilter !== 'all' && !normalized.includes(currentFilter as PortalId)) return 'all'
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
      const sanitized = { ...partial }
      if (sanitized.minPriceMillion !== undefined) {
        sanitized.minPriceMillion = Number.isFinite(sanitized.minPriceMillion) ? Math.max(0, sanitized.minPriceMillion) : 0
      }
      if (sanitized.maxPriceMillion !== undefined) {
        sanitized.maxPriceMillion = Number.isFinite(sanitized.maxPriceMillion) ? Math.max(0, sanitized.maxPriceMillion) : 0
      }
      if (sanitized.minArea !== undefined) {
        sanitized.minArea = Number.isFinite(sanitized.minArea) ? Math.max(0, sanitized.minArea) : 0
      }
      if (sanitized.maxArea !== undefined) {
        sanitized.maxArea = Number.isFinite(sanitized.maxArea) ? Math.max(0, sanitized.maxArea) : 0
      }
      if (sanitized.minRooms !== undefined) {
        sanitized.minRooms = Number.isFinite(sanitized.minRooms) ? Math.max(0, Math.floor(sanitized.minRooms)) : 0
      }
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
    onlyBargain: initial.onlyBargain || false,
    excludeMultiSource: initialExcludeMulti,
    onlyMultiSource: initialOnlyMulti,
    excludeNoImage: initial.excludeNoImage ?? getStoredExcludeNoImage(),
    minQualityScore: initial.minQualityScore || 0,
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
      updateCriteria({ excludeViewed })
    },

    setOnlyDiscounted: (onlyDiscounted) => updateCriteria({ onlyDiscounted }),
    setOnlyNewConstruction: (onlyNewConstruction) => updateCriteria({ onlyNewConstruction }),

    setSortOption: (sortOption) => {
      safeSetStorageItem('preferred_sort_option', sortOption)
      set({ sortOption })
      get().syncToUrl('replace')
    },

    setOnlyNew: (onlyNew) => {
      set({ onlyNew })
      get().syncToUrl('replace')
    },

    setOnlyBargain: (onlyBargain) => {
      set({ onlyBargain })
      get().syncToUrl('replace')
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
      set({ excludeNoImage })
      get().syncToUrl('replace')
    },

    setMinQualityScore: (minQualityScore) => {
      set({ minQualityScore: Math.max(0, Math.min(100, Math.round(minQualityScore))) })
      get().syncToUrl('replace')
    },

    setQuickSearchQuery: (quickSearchQuery) => set({ quickSearchQuery }),
    setSelectedPortalFilter: (selectedPortalFilter) => set({ selectedPortalFilter }),
    setShowHidden: (showHidden) => set({ showHidden }),

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
        onlyBargain: false,
        excludeMultiSource: false,
        onlyMultiSource: false,
        excludeNoImage: false,
        minQualityScore: 0,
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
        minQualityScore: parsed.minQualityScore ?? 0,
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
        minQualityScore,
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
        minQualityScore,
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
