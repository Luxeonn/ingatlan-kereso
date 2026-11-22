import type { SearchCriteria } from '../types/api'
import type { PropertyType } from '../types/property'
import type { PortalId, PortalFilterOption } from '../types/portal'
import { normalizePortalKey } from '../utils/formatters'
import { safeParseSortOption, safeParseStorageBoolean } from '../utils/validation'
import { DEFAULT_PORTALS, type SortOption } from './filterUrlSync'

export const SORT_OPTIONS: ReadonlyArray<{ value: SortOption; label: string }> = [
  { value: 'price_asc', label: 'Ár: Növekvő (alapértelmezett)' },
  { value: 'price_desc', label: 'Ár: Csökkenő' },
  { value: 'area_desc', label: 'Méret: Legnagyobb elöl' },
  { value: 'date_desc', label: 'Felfedezés: Legfrissebb' },
  { value: 'bargain_desc', label: 'Alkalmi vétel: Legnagyobb kedvezmény elöl' },
]

const VALID_SORTS: SortOption[] = SORT_OPTIONS.map((o) => o.value)

export function isSortOption(val: unknown): val is SortOption {
  return typeof val === 'string' && VALID_SORTS.includes(val as SortOption)
}

export function safeGetStorageItem(key: string): string | null {
  if (typeof window === 'undefined') return null
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

export function safeSetStorageItem(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage errors
  }
}

export function getStoredSortOption(): SortOption {
  return safeParseSortOption(safeGetStorageItem('preferred_sort_option'))
}

export function getStoredExcludeViewed(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_viewed'))
}

export function getStoredExcludeMultiSource(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_multi_source'))
}

export function getStoredOnlyMultiSource(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_only_multi_source'))
}

export function getStoredExcludeNoImage(): boolean {
  return safeParseStorageBoolean(safeGetStorageItem('preferred_exclude_no_image'))
}

export const DEFAULT_CRITERIA: SearchCriteria = {
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

export function resolvePortalFilterForPortals(
  portals: string[] | undefined,
  currentFilter: string,
): string {
  const normalized = (portals || [])
    .map(normalizePortalKey)
    .filter((k): k is PortalId => Boolean(k) && k !== 'egyeb')
  const firstNorm = normalized[0]
  if (firstNorm && normalized.length === 1) return firstNorm
  if (currentFilter !== 'all' && !normalized.includes(currentFilter as PortalId)) return 'all'
  return currentFilter
}

export function sanitizeCriteriaPartial(partial: Partial<SearchCriteria>): Partial<SearchCriteria> {
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
  return sanitized
}

export interface FilterState {
  criteria: SearchCriteria
  sortOption: SortOption
  onlyNew: boolean
  excludeNew: boolean
  onlyBargain: boolean
  excludeBargain: boolean
  onlyViewed: boolean
  excludeNewConstruction: boolean
  excludeMultiSource: boolean
  onlyMultiSource: boolean
  excludeNoImage: boolean
  onlyNoImage: boolean
  includeHidden: boolean
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
  setOnlyViewed: (only: boolean) => void
  setOnlyDiscounted: (onlyDiscounted: boolean) => void
  setOnlyNewConstruction: (onlyNewConstruction: boolean) => void
  setExcludeNewConstruction: (excludeNewConstruction: boolean) => void
  setSortOption: (sort: SortOption) => void
  setOnlyNew: (onlyNew: boolean) => void
  setExcludeNew: (excludeNew: boolean) => void
  setOnlyBargain: (onlyBargain: boolean) => void
  setExcludeBargain: (excludeBargain: boolean) => void
  setExcludeMultiSource: (excludeMultiSource: boolean) => void
  setOnlyMultiSource: (onlyMultiSource: boolean) => void
  setExcludeNoImage: (excludeNoImage: boolean) => void
  setOnlyNoImage: (onlyNoImage: boolean) => void
  setMinQualityScore: (score: number) => void
  setQuickSearchQuery: (query: string) => void
  setSelectedPortalFilter: (portal: PortalFilterOption | string) => void
  setShowHidden: (show: boolean) => void
  setIncludeHidden: (include: boolean) => void
  markCriteriaExecuted: (executedCriteria?: SearchCriteria) => void
  resetToDefaults: () => void
  syncFromUrl: () => void
  syncToUrl: (historyMode?: 'replace' | 'push') => void
}
