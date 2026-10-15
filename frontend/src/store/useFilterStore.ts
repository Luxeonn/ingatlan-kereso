import { create } from 'zustand'
import { SearchCriteria } from '../types/api'

export type SortOption =
  | 'price_asc'
  | 'price_desc'
  | 'sqm_price_asc'
  | 'sqm_price_desc'
  | 'area_desc'
  | 'date_desc'

export const DEFAULT_PORTALS = ['dh', 'oc', 'jofogas', 'ingatlancom', 'ingatlanbazar', 'koltozzbe']

const DEFAULT_CRITERIA: SearchCriteria = {
  cities: ['Debrecen'],
  city: 'Debrecen',
  propertyType: 'lakas',
  minPriceMillion: 70,
  maxPriceMillion: 120,
  minArea: 90,
  maxArea: 200,
  portals: [...DEFAULT_PORTALS],
  autoOpenBrowser: false,
  excludeViewed: false,
  onlyDiscounted: false,
}

interface FilterState {
  criteria: SearchCriteria
  sortOption: SortOption
  onlyNew: boolean
  
  // Actions
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
  setSortOption: (sort: SortOption) => void
  setOnlyNew: (onlyNew: boolean) => void
  resetToDefaults: () => void
  syncFromUrl: () => void
  syncToUrl: () => void
}

function parseUrlParams(): { criteria?: Partial<SearchCriteria>; sort?: SortOption; onlyNew?: boolean } {
  if (typeof window === 'undefined') return {}
  const params = new URLSearchParams(window.location.search)
  const result: Partial<SearchCriteria> = {}
  
  const citiesParam = params.get('cities') || params.get('city')
  if (citiesParam) {
    const list = citiesParam.split(',').map((c) => c.trim()).filter(Boolean)
    if (list.length > 0) {
      result.cities = list
      result.city = list[0]
    }
  }

  const type = params.get('type')
  if (type) result.propertyType = type

  const minPrice = params.get('minPrice')
  if (minPrice && !isNaN(Number(minPrice))) result.minPriceMillion = Number(minPrice)

  const maxPrice = params.get('maxPrice')
  if (maxPrice && !isNaN(Number(maxPrice))) result.maxPriceMillion = Number(maxPrice)

  const minArea = params.get('minArea')
  if (minArea && !isNaN(Number(minArea))) result.minArea = Number(minArea)

  const maxArea = params.get('maxArea')
  if (maxArea && !isNaN(Number(maxArea))) result.maxArea = Number(maxArea)

  const portalsParam = params.get('portals')
  if (portalsParam) {
    result.portals = portalsParam.split(',').map((p) => p.trim()).filter(Boolean)
  }

  const autoOpen = params.get('autoOpen')
  if (autoOpen != null) result.autoOpenBrowser = autoOpen === 'true'

  const excludeViewed = params.get('excludeViewed')
  if (excludeViewed != null) result.excludeViewed = excludeViewed === 'true'

  let sort: SortOption | undefined
  const sortParam = params.get('sort')
  if (sortParam && ['price_asc', 'price_desc', 'sqm_price_asc', 'sqm_price_desc', 'area_desc', 'date_desc'].includes(sortParam)) {
    sort = sortParam as SortOption
  }

  const onlyNewParam = params.get('onlyNew')
  const onlyNew = onlyNewParam === 'true'

  return { criteria: result, sort, onlyNew }
}

export const useFilterStore = create<FilterState>((set, get) => {
  const initial = parseUrlParams()
  const initialCriteria: SearchCriteria = {
    ...DEFAULT_CRITERIA,
    ...(initial.criteria || {}),
  }

  return {
    criteria: initialCriteria,
    sortOption: initial.sort || 'price_asc',
    onlyNew: initial.onlyNew || false,

    setCriteria: (partial) => {
      set((state) => {
        const next = { ...state.criteria, ...partial }
        return { criteria: next }
      })
      get().syncToUrl()
    },

    addCity: (city) => {
      const trimmed = city.trim()
      if (!trimmed) return
      set((state) => {
        const existing = state.criteria.cities || []
        if (existing.includes(trimmed)) return state
        const updated = [...existing, trimmed]
        return {
          criteria: {
            ...state.criteria,
            cities: updated,
            city: updated[0],
          },
        }
      })
      get().syncToUrl()
    },

    removeCity: (city) => {
      set((state) => {
        const updated = (state.criteria.cities || []).filter((c) => c !== city)
        return {
          criteria: {
            ...state.criteria,
            cities: updated,
            city: updated.length > 0 ? updated[0] : 'Debrecen',
          },
        }
      })
      get().syncToUrl()
    },

    setCities: (cities) => {
      const list = cities.map((c) => c.trim()).filter(Boolean)
      set((state) => ({
        criteria: {
          ...state.criteria,
          cities: list,
          city: list.length > 0 ? list[0] : 'Debrecen',
        },
      }))
      get().syncToUrl()
    },

    setPropertyType: (type) => {
      set((state) => ({
        criteria: { ...state.criteria, propertyType: type },
      }))
      get().syncToUrl()
    },

    setMinPriceMillion: (min) => {
      set((state) => ({
        criteria: { ...state.criteria, minPriceMillion: min },
      }))
      get().syncToUrl()
    },

    setMaxPriceMillion: (max) => {
      set((state) => ({
        criteria: { ...state.criteria, maxPriceMillion: max },
      }))
      get().syncToUrl()
    },

    setMinArea: (min) => {
      set((state) => ({
        criteria: { ...state.criteria, minArea: min },
      }))
      get().syncToUrl()
    },

    setMaxArea: (max) => {
      set((state) => ({
        criteria: { ...state.criteria, maxArea: max },
      }))
      get().syncToUrl()
    },

    togglePortal: (portalId) => {
      set((state) => {
        const current = state.criteria.portals || []
        const exists = current.includes(portalId)
        const updated = exists ? current.filter((p) => p !== portalId) : [...current, portalId]
        return {
          criteria: { ...state.criteria, portals: updated },
        }
      })
      get().syncToUrl()
    },

    setAllPortals: (portals) => {
      set((state) => ({
        criteria: { ...state.criteria, portals },
      }))
      get().syncToUrl()
    },

    setAutoOpen: (auto) => {
      set((state) => ({
        criteria: { ...state.criteria, autoOpenBrowser: auto },
      }))
      get().syncToUrl()
    },

    setExcludeViewed: (exclude) => {
      set((state) => ({
        criteria: { ...state.criteria, excludeViewed: exclude },
      }))
      get().syncToUrl()
    },

    setSortOption: (sort) => {
      set({ sortOption: sort })
      get().syncToUrl()
    },

    setOnlyNew: (onlyNew) => {
      set({ onlyNew })
      get().syncToUrl()
    },

    resetToDefaults: () => {
      set({
        criteria: { ...DEFAULT_CRITERIA },
        sortOption: 'price_asc',
        onlyNew: false,
      })
      if (typeof window !== 'undefined') {
        window.history.replaceState(null, '', window.location.pathname)
      }
    },

    syncFromUrl: () => {
      const parsed = parseUrlParams()
      if (parsed.criteria) {
        set((state) => ({
          criteria: { ...state.criteria, ...parsed.criteria },
          sortOption: parsed.sort || state.sortOption,
          onlyNew: parsed.onlyNew ?? state.onlyNew,
        }))
      }
    },

    syncToUrl: () => {
      if (typeof window === 'undefined') return
      const { criteria, sortOption, onlyNew } = get()
      const params = new URLSearchParams()

      if (criteria.cities && criteria.cities.length > 0) {
        params.set('cities', criteria.cities.join(','))
      }
      if (criteria.propertyType && criteria.propertyType !== 'lakas') {
        params.set('type', criteria.propertyType)
      }
      if (criteria.minPriceMillion != null && criteria.minPriceMillion !== 70) {
        params.set('minPrice', String(criteria.minPriceMillion))
      }
      if (criteria.maxPriceMillion != null && criteria.maxPriceMillion !== 120) {
        params.set('maxPrice', String(criteria.maxPriceMillion))
      }
      if (criteria.minArea != null && criteria.minArea !== 90) {
        params.set('minArea', String(criteria.minArea))
      }
      if (criteria.maxArea != null && criteria.maxArea !== 200) {
        params.set('maxArea', String(criteria.maxArea))
      }
      if (criteria.portals && criteria.portals.length < DEFAULT_PORTALS.length) {
        params.set('portals', criteria.portals.join(','))
      }
      if (sortOption !== 'price_asc') {
        params.set('sort', sortOption)
      }
      if (onlyNew) {
        params.set('onlyNew', 'true')
      }
      if (criteria.autoOpenBrowser) {
        params.set('autoOpen', 'true')
      }
      if (criteria.excludeViewed) {
        params.set('excludeViewed', 'true')
      }

      const queryString = params.toString()
      const newUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname
      window.history.replaceState(null, '', newUrl)
    },
  }
})
