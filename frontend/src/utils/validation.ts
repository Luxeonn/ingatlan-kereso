import { z } from 'zod'
import type { SearchCriteria, SearchPresetInput } from '../types/api'
import { normalizePropertyType } from './formatters'
import { DEFAULT_PORTALS, type SortOption } from '../store/filterUrlSync'

export const searchCriteriaSchema = z.object({
  city: z.string().max(200).optional(),
  cities: z.array(z.string().max(200)).max(50).optional(),
  propertyType: z.string().max(50).optional(),
  minPriceMillion: z.number().finite().min(0, 'A minimális ár nem lehet negatív').optional(),
  maxPriceMillion: z.number().finite().min(0, 'A maximális ár nem lehet negatív').optional(),
  minArea: z.number().finite().min(0, 'A minimális terület nem lehet negatív').optional(),
  maxArea: z.number().finite().min(0, 'A maximális terület nem lehet negatív').optional(),
  minRooms: z.number().finite().min(0, 'A szobaszám nem lehet negatív').optional(),
  portals: z.array(z.string()).optional(),
  autoOpenBrowser: z.boolean().optional(),
  excludeViewed: z.boolean().optional(),
  onlyDiscounted: z.boolean().optional(),
  onlyNewConstruction: z.boolean().optional(),
}).refine(
  (data) => {
    if (data.minPriceMillion !== undefined && data.maxPriceMillion !== undefined) {
      return data.minPriceMillion <= data.maxPriceMillion
    }
    return true
  },
  {
    message: 'A minimális ár nem lehet nagyobb a maximális árnál',
    path: ['minPriceMillion'],
  },
).refine(
  (data) => {
    if (data.minArea !== undefined && data.maxArea !== undefined) {
      return data.minArea <= data.maxArea
    }
    return true
  },
  {
    message: 'A minimális alapterület nem lehet nagyobb a maximális alapterületnél',
    path: ['minArea'],
  },
)

export const searchPresetInputSchema = z.object({
  name: z.string().trim().min(1, 'A sablon neve nem lehet üres').max(100, 'A sablon neve legfeljebb 100 karakter lehet'),
  criteria: searchCriteriaSchema.optional(),
})

export const sortOptionSchema = z.enum([
  'price_asc',
  'price_desc',
  'area_desc',
  'date_desc',
  'bargain_desc',
]).catch('price_asc')

export const booleanStorageSchema = z.enum(['true', 'false']).catch('false')

export function validateSearchCriteria(input: unknown): SearchCriteria {
  const parsed = searchCriteriaSchema.parse(input)
  const defaultCity = parsed.city || 'Hajdúszoboszló'
  return {
    city: defaultCity,
    cities: parsed.cities && parsed.cities.length > 0 ? parsed.cities : [defaultCity],
    propertyType: normalizePropertyType(parsed.propertyType),
    minPriceMillion: parsed.minPriceMillion ?? 70,
    maxPriceMillion: parsed.maxPriceMillion ?? 120,
    minArea: parsed.minArea ?? 90,
    maxArea: parsed.maxArea ?? 200,
    minRooms: parsed.minRooms,
    portals: parsed.portals ?? [...DEFAULT_PORTALS],
    autoOpenBrowser: parsed.autoOpenBrowser ?? false,
    excludeViewed: parsed.excludeViewed ?? false,
    onlyDiscounted: parsed.onlyDiscounted ?? false,
    onlyNewConstruction: parsed.onlyNewConstruction ?? false,
  }
}

export function validatePresetInput(input: unknown): SearchPresetInput {
  const parsed = searchPresetInputSchema.parse(input)
  return {
    name: parsed.name,
    criteria: parsed.criteria ? validateSearchCriteria(parsed.criteria) : undefined,
  }
}

export function safeParseSortOption(val: unknown): SortOption {
  return sortOptionSchema.parse(typeof val === 'string' ? val : '')
}

export function safeParseStorageBoolean(val: unknown): boolean {
  return booleanStorageSchema.parse(typeof val === 'string' ? val : '') === 'true'
}
