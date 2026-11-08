import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import {
  parseUrlParams,
  appendCriteriaParams,
  appendUiFilterParams,
  type UiFilterParamsOptions,
} from '../store/filterUrlSync'
import type { SearchCriteria } from '../types/api'

describe('filterUrlSync utility', () => {
  const originalLocation = window.location

  function setLocation(url: string) {
    Object.defineProperty(window, 'location', {
      value: new URL(url),
      writable: true,
      configurable: true,
    })
  }

  beforeEach(() => {
    setLocation('https://example.com/')
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
      configurable: true,
    })
  })

  describe('parseUrlParams', () => {
    it('returns empty object when query string has no filter params', () => {
      setLocation('https://example.com/')
      const result = parseUrlParams()
      expect(result).toEqual({})
    })

    it('preserves valid filter parameters when a single parameter is invalid or legacy (#390)', () => {
      setLocation(
        'https://example.com/?cities=Debrecen&type=haz&minPrice=55&sort=sqm_price_asc&minQuality=999',
      )
      const result = parseUrlParams()
      expect(result.criteria?.cities).toEqual(['Debrecen'])
      expect(result.criteria?.propertyType).toBe('HAZ')
      expect(result.criteria?.minPriceMillion).toBe(55)
      expect(result.sort).toBeUndefined()
      expect(result.minQualityScore).toBeUndefined()
    })

    it('parses valid multiple cities, property type, and numeric bounds', () => {
      setLocation(
        'https://example.com/?cities=Debrecen,Szeged&type=haz&minPrice=60&maxPrice=140&minArea=80&maxArea=180',
      )

      const result = parseUrlParams()
      expect(result.criteria?.cities).toEqual(['Debrecen', 'Szeged'])
      expect(result.criteria?.city).toBe('Debrecen')
      expect(result.criteria?.propertyType).toBe('HAZ')
      expect(result.criteria?.minPriceMillion).toBe(60)
      expect(result.criteria?.maxPriceMillion).toBe(140)
      expect(result.criteria?.minArea).toBe(80)
      expect(result.criteria?.maxArea).toBe(180)
    })

    it('parses single legacy city parameter when cities is absent', () => {
      setLocation('https://example.com/?city=Eger')
      const result = parseUrlParams()
      expect(result.criteria?.cities).toEqual(['Eger'])
      expect(result.criteria?.city).toBe('Eger')
    })

    it('parses boolean flags, portals, and quality score correctly', () => {
      setLocation(
        'https://example.com/?portals=dh,oc&autoOpen=true&excludeViewed=true&onlyNewConstruction=true&onlyNew=true&onlyBargain=true&excludeMulti=1&minQuality=80&sort=bargain_desc',
      )

      const result = parseUrlParams()
      expect(result.criteria?.portals).toEqual(['dh', 'oc'])
      expect(result.criteria?.autoOpenBrowser).toBe(true)
      expect(result.criteria?.excludeViewed).toBe(true)
      expect(result.criteria?.onlyNewConstruction).toBe(true)
      expect(result.onlyNew).toBe(true)
      expect(result.onlyBargain).toBe(true)
      expect(result.excludeMultiSource).toBe(true)
      expect(result.minQualityScore).toBe(80)
      expect(result.sort).toBe('bargain_desc')
    })

    it('gracefully handles and rejects invalid or malformed URL parameters', () => {
      setLocation(
        'https://example.com/?sort=nonexistent_sort&minPrice=not_a_number&minQuality=999',
      )

      const result = parseUrlParams()
      expect(result).toEqual({})
    })
  })

  describe('appendCriteriaParams & appendUiFilterParams', () => {
    const baseCriteria: SearchCriteria = {
      cities: ['Hajdúszoboszló'],
      city: 'Hajdúszoboszló',
      propertyType: 'HAZ',
      minPriceMillion: 70, // default
      maxPriceMillion: 120, // default
      minArea: 90, // default
      maxArea: 200, // default
      portals: ['dh', 'oc', 'jofogas', 'ingatlancom', 'ingatlanbazar', 'koltozzbe'],
      autoOpenBrowser: false,
      excludeViewed: false,
      onlyDiscounted: false,
      onlyNewConstruction: false,
    }

    it('omits default numeric ranges to keep URL query minimal', () => {
      const params = new URLSearchParams()
      appendCriteriaParams(params, baseCriteria)
      expect(params.get('minPrice')).toBeNull()
      expect(params.get('maxPrice')).toBeNull()
      expect(params.get('minArea')).toBeNull()
      expect(params.get('maxArea')).toBeNull()
      expect(params.get('cities')).toBe('Hajdúszoboszló')
    })

    it('serializes custom numeric ranges, portals subset, and active flags', () => {
      const customCriteria: SearchCriteria = {
        ...baseCriteria,
        cities: ['Debrecen', 'Miskolc'],
        propertyType: 'LAKAS',
        minPriceMillion: 55,
        maxPriceMillion: 110,
        minArea: 75,
        maxArea: 160,
        portals: ['dh', 'oc'],
        autoOpenBrowser: true,
        excludeViewed: true,
        onlyNewConstruction: true,
      }

      const params = new URLSearchParams()
      appendCriteriaParams(params, customCriteria)
      expect(params.get('cities')).toBe('Debrecen,Miskolc')
      expect(params.get('type')).toBe('LAKAS')
      expect(params.get('minPrice')).toBe('55')
      expect(params.get('maxPrice')).toBe('110')
      expect(params.get('minArea')).toBe('75')
      expect(params.get('maxArea')).toBe('160')
      expect(params.get('portals')).toBe('dh,oc')
      expect(params.get('autoOpen')).toBe('true')
      expect(params.get('excludeViewed')).toBe('true')
      expect(params.get('onlyNewConstruction')).toBe('true')
    })

    it('serializes UI filter options correctly', () => {
      const uiOptions: UiFilterParamsOptions = {
        sortOption: 'area_desc',
        onlyNew: true,
        onlyBargain: true,
        excludeMultiSource: true,
        minQualityScore: 70,
      }

      const params = new URLSearchParams()
      appendUiFilterParams(params, uiOptions)
      expect(params.get('sort')).toBe('area_desc')
      expect(params.get('onlyNew')).toBe('true')
      expect(params.get('onlyBargain')).toBe('true')
      expect(params.get('hideMulti')).toBe('1')
      expect(params.get('minQuality')).toBe('70')
    })

    it('supports round-trip serialization and deserialization', () => {
      const customCriteria: SearchCriteria = {
        ...baseCriteria,
        cities: ['Debrecen'],
        propertyType: 'TELEK',
        minPriceMillion: 40,
        maxPriceMillion: 90,
        minArea: 500,
        maxArea: 1500,
        portals: ['dh', 'jofogas'],
        onlyNewConstruction: false,
      }

      const uiOptions: UiFilterParamsOptions = {
        sortOption: 'price_desc',
        onlyNew: false,
        onlyBargain: true,
        excludeMultiSource: false,
        minQualityScore: 85,
      }

      const params = new URLSearchParams()
      appendCriteriaParams(params, customCriteria)
      appendUiFilterParams(params, uiOptions)

      setLocation(`https://example.com/?${params.toString()}`)
      const parsed = parseUrlParams()

      expect(parsed.criteria?.cities).toEqual(['Debrecen'])
      expect(parsed.criteria?.propertyType).toBe('TELEK')
      expect(parsed.criteria?.minPriceMillion).toBe(40)
      expect(parsed.criteria?.maxPriceMillion).toBe(90)
      expect(parsed.criteria?.minArea).toBe(500)
      expect(parsed.criteria?.maxArea).toBe(1500)
      expect(parsed.criteria?.portals).toEqual(['dh', 'jofogas'])
      expect(parsed.sort).toBe('price_desc')
      expect(parsed.onlyBargain).toBe(true)
      expect(parsed.minQualityScore).toBe(85)
    })
  })
})
