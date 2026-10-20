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
  sort: z.enum(['price_asc', 'price_desc', 'sqm_price_asc', 'sqm_price_desc', 'area_desc', 'date_desc']).optional(),
  onlyNew: z.enum(['true', 'false']).optional(),
})

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
  const parsedUrl = urlSchema.safeParse(Object.fromEntries(new URLSearchParams(window.location.search)))
  if (!parsedUrl.success) return {}
  const params = parsedUrl.data
  const result: Partial<SearchCriteria> = {}
  
  const citiesParam = params.cities || params.city
  if (citiesParam) {
    const list = citiesParam.split(',').map((c) => c.trim()).filter((c) => c.length > 0 && c.length <= 100).slice(0, 20)
    if (list.length > 0) {
      result.cities = list
      result.city = list[0]
    }
  }

  const type = params.type
  if (type) result.propertyType = type

  const minPrice = params.minPrice
  if (minPrice !== undefined) result.minPriceMillion = minPrice

  const maxPrice = params.maxPrice
  if (maxPrice !== undefined) result.maxPriceMillion = maxPrice

  const minArea = params.minArea
  if (minArea !== undefined) result.minArea = minArea

  const maxArea = params.maxArea
  if (maxArea !== undefined) result.maxArea = maxArea

  const portalsParam = params.portals
  if (portalsParam) {
    const validPortals = portalsParam.split(',').map((p) => p.trim()).filter((p) => DEFAULT_PORTALS.includes(p))
    if (validPortals.length > 0) result.portals = validPortals
  }

  const autoOpen = params.autoOpen
  if (autoOpen != null) result.autoOpenBrowser = autoOpen === 'true'

  const excludeViewed = params.excludeViewed
  if (excludeViewed != null) result.excludeViewed = excludeViewed === 'true'

  let sort: SortOption | undefined
  const sortParam = params.sort
  if (sortParam) sort = sortParam

  const onlyNewParam = params.onlyNew
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
      set({
        criteria: { ...DEFAULT_CRITERIA, ...(parsed.criteria || {}) },
        sortOption: parsed.sort || 'price_asc',
        onlyNew: parsed.onlyNew ?? false,
      })
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
      if (`${window.location.pathname}${window.location.search}` !== newUrl) {
        window.history.pushState(null, '', newUrl)
      }
    },
  }
})

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', () => useFilterStore.getState().syncFromUrl())
}
