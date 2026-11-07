import { z } from 'zod'
import type { SearchCriteria } from '../types/api'
import { DEFAULT_PORTALS, type SortOption } from './useFilterStore'

export const urlSchema = z.object({
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
  sort: z.enum(['price_asc', 'price_desc', 'area_desc', 'date_desc', 'bargain_desc']).optional(),
  onlyNew: z.enum(['true', 'false']).optional(),
  onlyBargain: z.enum(['true', 'false']).optional(),
  hideMulti: z.enum(['true', 'false', '1', '0']).optional(),
  excludeMulti: z.enum(['true', 'false', '1', '0']).optional(),
  minQuality: z.coerce.number().finite().min(0).max(100).optional(),
})

type ParsedUrlParams = z.infer<typeof urlSchema>

export interface ParsedUrlFilterResult {
  criteria?: Partial<SearchCriteria>
  sort?: SortOption
  onlyNew?: boolean
  onlyBargain?: boolean
  excludeMultiSource?: boolean
  minQualityScore?: number
}

function parseCitiesParam(citiesParam?: string): { cities: string[]; city: string } | null {
  if (!citiesParam) return null
  const list = citiesParam
    .split(',')
    .map((c) => c.trim())
    .filter((c) => c.length > 0 && c.length <= 100)
    .slice(0, 20)
  if (list.length === 0) return null
  return { cities: list, city: list[0] }
}

function parsePortalsParam(portalsParam?: string): string[] | null {
  if (!portalsParam) return null
  const validPortals = portalsParam
    .split(',')
    .map((p) => p.trim())
    .filter((p) => DEFAULT_PORTALS.includes(p))
  return validPortals.length > 0 ? validPortals : null
}

function parseNumericRanges(params: ParsedUrlParams, result: Partial<SearchCriteria>): void {
  if (params.minPrice !== undefined) result.minPriceMillion = params.minPrice
  if (params.maxPrice !== undefined) result.maxPriceMillion = params.maxPrice
  if (params.minArea !== undefined) result.minArea = params.minArea
  if (params.maxArea !== undefined) result.maxArea = params.maxArea
}

function parseBooleanFlags(params: ParsedUrlParams, result: Partial<SearchCriteria>): void {
  if (params.autoOpen !== null && params.autoOpen !== undefined) result.autoOpenBrowser = params.autoOpen === 'true'
  if (params.excludeViewed !== null && params.excludeViewed !== undefined) result.excludeViewed = params.excludeViewed === 'true'
  if (params.onlyNewConstruction !== null && params.onlyNewConstruction !== undefined) {
    result.onlyNewConstruction = params.onlyNewConstruction === 'true'
  }
}

export function parseUrlParams(): ParsedUrlFilterResult {
  if (typeof window === 'undefined') return {}
  const parsedUrl = urlSchema.safeParse(Object.fromEntries(new URLSearchParams(window.location.search)))
  if (!parsedUrl.success) return {}
  const params = parsedUrl.data
  const result: Partial<SearchCriteria> = {}

  const parsedCities = parseCitiesParam(params.cities || params.city)
  if (parsedCities) {
    result.cities = parsedCities.cities
    result.city = parsedCities.city
  }

  if (params.type) result.propertyType = params.type
  parseNumericRanges(params, result)

  const parsedPortals = parsePortalsParam(params.portals)
  if (parsedPortals) {
    result.portals = parsedPortals
  }

  parseBooleanFlags(params, result)

  const hideMultiParam = params.hideMulti ?? params.excludeMulti
  const excludeMultiSource =
    hideMultiParam !== undefined
      ? hideMultiParam === 'true' || hideMultiParam === '1'
      : undefined

  return {
    criteria: result,
    sort: params.sort,
    onlyNew: params.onlyNew === 'true',
    onlyBargain: params.onlyBargain === 'true',
    excludeMultiSource,
    minQualityScore: params.minQuality ?? 0,
  }
}

function appendNumericRangeParams(params: URLSearchParams, criteria: SearchCriteria): void {
  const { minPriceMillion, maxPriceMillion, minArea, maxArea } = criteria
  if (minPriceMillion !== null && minPriceMillion !== undefined && minPriceMillion !== 70) {
    params.set('minPrice', String(minPriceMillion))
  }
  if (maxPriceMillion !== null && maxPriceMillion !== undefined && maxPriceMillion !== 120) {
    params.set('maxPrice', String(maxPriceMillion))
  }
  if (minArea !== null && minArea !== undefined && minArea !== 90) {
    params.set('minArea', String(minArea))
  }
  if (maxArea !== null && maxArea !== undefined && maxArea !== 200) {
    params.set('maxArea', String(maxArea))
  }
}

function appendFilterCategoryParams(params: URLSearchParams, criteria: SearchCriteria): void {
  if (criteria.cities && criteria.cities.length > 0) params.set('cities', criteria.cities.join(','))
  if (criteria.propertyType && criteria.propertyType !== 'lakas') params.set('type', criteria.propertyType)
  if (criteria.portals && criteria.portals.length < DEFAULT_PORTALS.length) {
    params.set('portals', criteria.portals.join(','))
  }
  if (criteria.autoOpenBrowser) params.set('autoOpen', 'true')
  if (criteria.excludeViewed) params.set('excludeViewed', 'true')
  if (criteria.onlyNewConstruction) params.set('onlyNewConstruction', 'true')
}

export function appendCriteriaParams(params: URLSearchParams, criteria: SearchCriteria): void {
  appendFilterCategoryParams(params, criteria)
  appendNumericRangeParams(params, criteria)
}

export interface UiFilterParamsOptions {
  sortOption: SortOption
  onlyNew: boolean
  onlyBargain: boolean
  excludeMultiSource: boolean
  minQualityScore: number
}

export function appendUiFilterParams(params: URLSearchParams, options: UiFilterParamsOptions): void {
  if (options.sortOption !== 'price_asc') params.set('sort', options.sortOption)
  if (options.onlyNew) params.set('onlyNew', 'true')
  if (options.onlyBargain) params.set('onlyBargain', 'true')
  if (options.excludeMultiSource) params.set('hideMulti', '1')
  if (options.minQualityScore > 0) params.set('minQuality', String(options.minQualityScore))
}
