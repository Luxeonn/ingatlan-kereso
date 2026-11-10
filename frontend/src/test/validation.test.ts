import { describe, it, expect } from 'vitest'
import { ZodError } from 'zod'
import {
  validateSearchCriteria,
  validatePresetInput,
  safeParseSortOption,
  safeParseStorageBoolean,
} from '../utils/validation'

describe('Runtime Boundary Validation Suite (#403)', () => {
  describe('validateSearchCriteria', () => {
    it('successfully parses and normalizes valid criteria', () => {
      const valid = {
        city: 'Budapest',
        cities: ['Budapest', 'Debrecen'],
        propertyType: 'haz',
        minPriceMillion: 50,
        maxPriceMillion: 120,
        minArea: 80,
        maxArea: 160,
        minRooms: 2,
        portals: ['dh', 'oc'],
        autoOpenBrowser: true,
        excludeViewed: true,
        onlyDiscounted: false,
        onlyNewConstruction: false,
      }

      const result = validateSearchCriteria(valid)
      expect(result.city).toBe('Budapest')
      expect(result.cities).toEqual(['Budapest', 'Debrecen'])
      expect(result.propertyType).toBe('HAZ')
      expect(result.minPriceMillion).toBe(50)
      expect(result.maxPriceMillion).toBe(120)
      expect(result.minArea).toBe(80)
      expect(result.maxArea).toBe(160)
      expect(result.minRooms).toBe(2)
      expect(result.portals).toEqual(['dh', 'oc'])
      expect(result.autoOpenBrowser).toBe(true)
      expect(result.excludeViewed).toBe(true)
    })

    it('rejects inverted price ranges (minPrice > maxPrice)', () => {
      const invalid = {
        minPriceMillion: 150,
        maxPriceMillion: 80,
      }

      expect(() => validateSearchCriteria(invalid)).toThrow(ZodError)
      try {
        validateSearchCriteria(invalid)
      } catch (err) {
        expect(err).toBeInstanceOf(ZodError)
        const zodErr = err as ZodError
        expect(zodErr.issues[0]?.message).toContain('A minimális ár nem lehet nagyobb a maximális árnál')
      }
    })

    it('rejects inverted area ranges (minArea > maxArea)', () => {
      const invalid = {
        minArea: 200,
        maxArea: 90,
      }

      expect(() => validateSearchCriteria(invalid)).toThrow(ZodError)
      try {
        validateSearchCriteria(invalid)
      } catch (err) {
        expect(err).toBeInstanceOf(ZodError)
        const zodErr = err as ZodError
        expect(zodErr.issues[0]?.message).toContain('A minimális alapterület nem lehet nagyobb a maximális alapterületnél')
      }
    })

    it('rejects negative numbers for price, area, or room count', () => {
      expect(() => validateSearchCriteria({ minPriceMillion: -10 })).toThrow(ZodError)
      expect(() => validateSearchCriteria({ maxPriceMillion: -5 })).toThrow(ZodError)
      expect(() => validateSearchCriteria({ minArea: -20 })).toThrow(ZodError)
      expect(() => validateSearchCriteria({ maxArea: -1 })).toThrow(ZodError)
      expect(() => validateSearchCriteria({ minRooms: -2 })).toThrow(ZodError)
    })

    it('normalizes unknown property type to EGYEB_ISMERETLEN without throwing', () => {
      const result = validateSearchCriteria({ propertyType: 'ismeretlen_fura_kategoria' })
      expect(result.propertyType).toBe('EGYEB_ISMERETLEN')
    })
  })

  describe('validatePresetInput', () => {
    it('successfully validates valid preset input', () => {
      const valid = {
        name: 'Debreceni házak',
        criteria: {
          city: 'Debrecen',
          minPriceMillion: 40,
          maxPriceMillion: 90,
        },
      }

      const result = validatePresetInput(valid)
      expect(result.name).toBe('Debreceni házak')
      expect(result.criteria?.city).toBe('Debrecen')
    })

    it('rejects empty or whitespace-only preset name', () => {
      expect(() => validatePresetInput({ name: '' })).toThrow(ZodError)
      expect(() => validatePresetInput({ name: '   ' })).toThrow(ZodError)
    })

    it('rejects preset name longer than 100 characters', () => {
      const longName = 'A'.repeat(101)
      expect(() => validatePresetInput({ name: longName })).toThrow(ZodError)
    })

    it('rejects preset with invalid criteria range', () => {
      const invalid = {
        name: 'Érvénytelen tartományú sablon',
        criteria: {
          minPriceMillion: 100,
          maxPriceMillion: 40,
        },
      }

      expect(() => validatePresetInput(invalid)).toThrow(ZodError)
    })
  })

  describe('safeParseSortOption', () => {
    it('returns valid sort option when known', () => {
      expect(safeParseSortOption('price_asc')).toBe('price_asc')
      expect(safeParseSortOption('price_desc')).toBe('price_desc')
      expect(safeParseSortOption('area_desc')).toBe('area_desc')
      expect(safeParseSortOption('date_desc')).toBe('date_desc')
      expect(safeParseSortOption('bargain_desc')).toBe('bargain_desc')
    })

    it('falls back to price_asc for unknown or malformed sort strings', () => {
      expect(safeParseSortOption('unknown_sort')).toBe('price_asc')
      expect(safeParseSortOption('')).toBe('price_asc')
      expect(safeParseSortOption(null)).toBe('price_asc')
      expect(safeParseSortOption(undefined)).toBe('price_asc')
      expect(safeParseSortOption(123)).toBe('price_asc')
    })
  })

  describe('safeParseStorageBoolean', () => {
    it('parses valid boolean string values', () => {
      expect(safeParseStorageBoolean('true')).toBe(true)
      expect(safeParseStorageBoolean('false')).toBe(false)
    })

    it('falls back to false for null, undefined, or unexpected strings', () => {
      expect(safeParseStorageBoolean(null)).toBe(false)
      expect(safeParseStorageBoolean(undefined)).toBe(false)
      expect(safeParseStorageBoolean('yes')).toBe(false)
      expect(safeParseStorageBoolean('1')).toBe(false)
    })
  })
})
