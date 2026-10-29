import { create } from 'zustand'
import { z } from 'zod'
import { SearchCriteria } from '../types/api'

export type SortOption =
  | 'price_asc'
  | 'price_desc'
  | 'sqm_price_asc'
  | 'sqm_price_desc'
  | 'area_desc'
  | 'date_desc'

export const SORT_OPTIONS: ReadonlyArray<{ value: SortOption; label: string }> = [
  { value: 'price_asc', label: 'Ár: Növekvő (alapértelmezett)' },
  { value: 'price_desc', label: 'Ár: Csökkenő' },
  { value: 'sqm_price_asc', label: 'Ft/m²: Növekvő' },
  { value: 'sqm_price_desc', label: 'Ft/m²: Csökkenő' },
  { value: 'area_desc', label: 'Méret: Legnagyobb elöl' },
  { value: 'date_desc', label: 'Felfedezés: Legfrissebb' },
]

export const DEFAULT_PORTALS = ['dh', 'oc', 'jofogas', 'ingatlancom', 'ingatlanbazar', 'koltozzbe']

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

const urlSchema = z.object({
  cities: z.string().max(1000).optional(),
  city: z.string().max(100).optional(),
  type: z.enum(['lakas', 'haz', 'telek']).optional(),
  minPrice: z.coerce.number().finite().min(0).max(100_000).optional(),
  maxPrice: z.coerce.number().finite().min(0).max(100_000).optional(),
  minArea: z.coerce.number().finite().min(0).max(100_000).optional(),
  maxArea: z.coerce.number().finite().min(0).max(100_000).optional(),
  portals: z.string().max(500).optional(),
  autoOpen: z.enum(['true', 'false']).optional(),
  excludeViewed: z.enum(['true', 'false']).optional(),
  onlyNewConstruction: z.enum(['true', 'false']).optional(),
  sort: z.enum(['price_asc', 'price_desc', 'sqm_price_asc', 'sqm_price_desc', 'area_desc', 'date_desc']).optional(),
  onlyNew: z.enum(['true', 'false']).optional(),
  onlyBargain: z.enum(['true', 'false']).optional(),
  minQuality: z.coerce.number().finite().min(0).max(100).optional(),
})

interface FilterState {
  criteria: SearchCriteria
  sortOption: SortOption
  onlyNew: boolean
  onlyBargain: boolean
  minQualityScore: number
  quickSearchQuery: string
  selectedPortalFilter: string

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
  setMinQualityScore: (score: number) => void
  setQuickSearchQuery: (query: string) => void
  setSelectedPortalFilter: (portal: string) => void
  resetToDefaults: () => void
  syncFromUrl: () => void
  syncToUrl: () => void
}

function parseUrlParams(): {
  criteria?: Partial<SearchCriteria>
  sort?: SortOption
  onlyNew?: boolean
  onlyBargain?: boolean
  minQualityScore?: number
} {
  if (typeof window === 'undefined') return {}
  const parsedUrl = urlSchema.safeParse(Object.fromEntries(new URLSearchParams(window.location.search)))
  if (!parsedUrl.success) return {}
  const params = parsedUrl.data
  const result: Partial<SearchCriteria> = {}

  const citiesParam = params.cities || params.city
  if (citiesParam) {
    const list = citiesParam
      .split(',')
      .map((c) => c.trim())
      .filter((c) => c.length > 0 && c.length <= 100)
      .slice(0, 20)
    if (list.length > 0) {
      result.cities = list
      result.city = list[0]
    }
  }

  if (params.type) result.propertyType = params.type
  if (params.minPrice !== undefined) result.minPriceMillion = params.minPrice
  if (params.maxPrice !== undefined) result.maxPriceMillion = params.maxPrice
  if (params.minArea !== undefined) result.minArea = params.minArea
  if (params.maxArea !== undefined) result.maxArea = params.maxArea

  if (params.portals) {
    const validPortals = params.portals
      .split(',')
      .map((p) => p.trim())
      .filter((p) => DEFAULT_PORTALS.includes(p))
    if (validPortals.length > 0) result.portals = validPortals
  }

  if (params.autoOpen != null) result.autoOpenBrowser = params.autoOpen === 'true'
  if (params.excludeViewed != null) result.excludeViewed = params.excludeViewed === 'true'
  if (params.onlyNewConstruction != null) result.onlyNewConstruction = params.onlyNewConstruction === 'true'

  return {
    criteria: result,
    sort: params.sort,
    onlyNew: params.onlyNew === 'true',
    onlyBargain: params.onlyBargain === 'true',
    minQualityScore: params.minQuality ?? 0,
  }
}

export const useFilterStore = create<FilterState>((set, get) => {
  const initial = parseUrlParams()
  const updateCriteria = (partial: Partial<SearchCriteria>) => {
    set((state) => ({ criteria: { ...state.criteria, ...partial } }))
    get().syncToUrl()
  }

  return {
    criteria: {
      ...DEFAULT_CRITERIA,
      excludeViewed: getStoredExcludeViewed(),
      ...(initial.criteria || {}),
    },
    sortOption: initial.sort || getStoredSortOption(),
    onlyNew: initial.onlyNew || false,
    onlyBargain: initial.onlyBargain || false,
    minQualityScore: initial.minQualityScore || 0,
    quickSearchQuery: '',
    selectedPortalFilter: 'all',

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

    setMinQualityScore: (minQualityScore) => {
      set({ minQualityScore })
      get().syncToUrl()
    },

    setQuickSearchQuery: (quickSearchQuery) => set({ quickSearchQuery }),
    setSelectedPortalFilter: (selectedPortalFilter) => set({ selectedPortalFilter }),

    resetToDefaults: () => {
      set({
        criteria: { ...DEFAULT_CRITERIA },
        sortOption: 'price_asc',
        onlyNew: false,
        onlyBargain: false,
        minQualityScore: 0,
        quickSearchQuery: '',
        selectedPortalFilter: 'all',
      })
      if (typeof window !== 'undefined') {
        window.history.replaceState(null, '', window.location.pathname)
      }
    },

    syncFromUrl: () => {
      const parsed = parseUrlParams()
      set({
        criteria: { ...DEFAULT_CRITERIA, ...(parsed.criteria || {}) },
        sortOption: parsed.sort || getStoredSortOption(),
        onlyNew: parsed.onlyNew ?? false,
        onlyBargain: parsed.onlyBargain ?? false,
        minQualityScore: parsed.minQualityScore ?? 0,
      })
    },

    syncToUrl: () => {
      if (typeof window === 'undefined') return
      const { criteria, sortOption, onlyNew, onlyBargain, minQualityScore } = get()
      const params = new URLSearchParams()

      if (criteria.cities && criteria.cities.length > 0) params.set('cities', criteria.cities.join(','))
      if (criteria.propertyType && criteria.propertyType !== 'lakas') params.set('type', criteria.propertyType)
      if (criteria.minPriceMillion != null && criteria.minPriceMillion !== 70) {
        params.set('minPrice', String(criteria.minPriceMillion))
      }
      if (criteria.maxPriceMillion != null && criteria.maxPriceMillion !== 120) {
        params.set('maxPrice', String(criteria.maxPriceMillion))
      }
      if (criteria.minArea != null && criteria.minArea !== 90) params.set('minArea', String(criteria.minArea))
      if (criteria.maxArea != null && criteria.maxArea !== 200) params.set('maxArea', String(criteria.maxArea))
      if (criteria.portals && criteria.portals.length < DEFAULT_PORTALS.length) {
        params.set('portals', criteria.portals.join(','))
      }
      if (sortOption !== 'price_asc') params.set('sort', sortOption)
      if (onlyNew) params.set('onlyNew', 'true')
      if (onlyBargain) params.set('onlyBargain', 'true')
      if (minQualityScore > 0) params.set('minQuality', String(minQualityScore))
      if (criteria.autoOpenBrowser) params.set('autoOpen', 'true')
      if (criteria.excludeViewed) params.set('excludeViewed', 'true')
      if (criteria.onlyNewConstruction) params.set('onlyNewConstruction', 'true')

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
