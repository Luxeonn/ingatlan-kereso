import { describe, it, expect, vi } from 'vitest'
import {
  formatPriceMillion,
  formatPriceFt,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  formatDate,
  clampQualityScore,
  getQualityConfig,
  getEffectivePrice,
  getEffectiveSqmPrice,
  getItemSourceListingsCount,
  getTotalSourceListingsCount,
  normalizePropertyType,
  getPropertyTypeConfig,
  normalizePortalKey,
  getPortalStyle,
  detectPortalFromUrl,
  openExternalUrl,
} from '../utils/formatters'
import type { IngatlanItem } from '../types/property'

describe('formatters utility', () => {
  describe('formatPriceMillion', () => {
    it('formats price in millions correctly with decimal precision', () => {
      expect(formatPriceMillion(75.5)).toBe('75.5 M Ft')
      expect(formatPriceMillion(120)).toBe('120 M Ft')
      expect(formatPriceMillion(0)).toBe('0 M Ft')
    })

    it('returns N/A for null, undefined, NaN, and negative numbers', () => {
      expect(formatPriceMillion(null)).toBe('N/A')
      expect(formatPriceMillion(undefined)).toBe('N/A')
      expect(formatPriceMillion(NaN)).toBe('N/A')
      expect(formatPriceMillion(-10)).toBe('N/A')
    })
  })

  describe('formatPriceFt', () => {
    it('formats price in HUF correctly with thousands separators', () => {
      const formatted = formatPriceFt(75500000)
      expect(formatted).toContain('75')
      expect(formatted).toContain('500')
      expect(formatted).toContain('000')
      expect(formatted).toContain('Ft')
    })

    it('returns fallback text for null, undefined, NaN, zero or negative amounts', () => {
      expect(formatPriceFt(null)).toBe('Ár a hirdetésben')
      expect(formatPriceFt(undefined)).toBe('Ár a hirdetésben')
      expect(formatPriceFt(NaN)).toBe('Ár a hirdetésben')
      expect(formatPriceFt(0)).toBe('Ár a hirdetésben')
      expect(formatPriceFt(-5000)).toBe('Ár a hirdetésben')
    })
  })

  describe('formatPricePerSqm', () => {
    it('formats price per square meter with rounding', () => {
      const formatted = formatPricePerSqm(850000.4)
      expect(formatted).toContain('850')
      expect(formatted).toContain('Ft/m²')
    })

    it('returns N/A for zero, negative, null, undefined, and NaN', () => {
      expect(formatPricePerSqm(0)).toBe('N/A')
      expect(formatPricePerSqm(-100)).toBe('N/A')
      expect(formatPricePerSqm(null)).toBe('N/A')
      expect(formatPricePerSqm(undefined)).toBe('N/A')
      expect(formatPricePerSqm(NaN)).toBe('N/A')
    })
  })

  describe('formatArea', () => {
    it('formats floor area in m² with rounding', () => {
      expect(formatArea(95.4)).toBe('95 m²')
      expect(formatArea(95.6)).toBe('96 m²')
    })

    it('returns N/A for null, undefined, NaN, zero or negative area', () => {
      expect(formatArea(null)).toBe('N/A')
      expect(formatArea(undefined)).toBe('N/A')
      expect(formatArea(NaN)).toBe('N/A')
      expect(formatArea(0)).toBe('N/A')
      expect(formatArea(-12)).toBe('N/A')
    })
  })

  describe('formatRooms', () => {
    it('formats room count for integers and fractions', () => {
      expect(formatRooms(3)).toBe('3 szoba')
      expect(formatRooms(1.5)).toBe('1.5 szoba')
    })

    it('returns empty string for null, undefined, NaN, zero or negative rooms', () => {
      expect(formatRooms(null)).toBe('')
      expect(formatRooms(undefined)).toBe('')
      expect(formatRooms(NaN)).toBe('')
      expect(formatRooms(0)).toBe('')
      expect(formatRooms(-1)).toBe('')
    })
  })

  describe('formatDate', () => {
    it('formats valid ISO date strings safely', () => {
      const formatted = formatDate('2026-10-15T10:00:00Z')
      expect(formatted).toBeTruthy()
      expect(formatted).toContain('2026')
    })

    it('returns empty string for null, undefined, empty, or whitespace-only strings', () => {
      expect(formatDate('')).toBe('')
      expect(formatDate('   ')).toBe('')
      expect(formatDate(null)).toBe('')
      expect(formatDate(undefined)).toBe('')
    })

    it('returns original input when date parsing fails', () => {
      expect(formatDate('not-a-date')).toBe('not-a-date')
    })
  })

  describe('normalizePropertyType & getPropertyTypeConfig', () => {
    it('recognizes canonical keys directly regardless of case', () => {
      expect(normalizePropertyType('LAKAS')).toBe('LAKAS')
      expect(normalizePropertyType('haz')).toBe('HAZ')
      expect(normalizePropertyType('Telek')).toBe('TELEK')
      expect(normalizePropertyType('garazs')).toBe('GARAZS')
      expect(normalizePropertyType('ipari_kereskedelmi')).toBe('IPARI_KERESKEDELMI')
    })

    it('maps keyword variations to canonical property types', () => {
      expect(normalizePropertyType('társasházi panel')).toBe('LAKAS')
      expect(normalizePropertyType('családi ikerház')).toBe('HAZ')
      expect(normalizePropertyType('építési telek')).toBe('TELEK')
      expect(normalizePropertyType('teremgarázs parkoló')).toBe('GARAZS')
      expect(normalizePropertyType('ipari telephely raktár')).toBe('IPARI_KERESKEDELMI')
    })

    it('falls back to EGYEB_ISMERETLEN for unrecognized, empty, or null types', () => {
      expect(normalizePropertyType(null)).toBe('EGYEB_ISMERETLEN')
      expect(normalizePropertyType('')).toBe('EGYEB_ISMERETLEN')
      expect(normalizePropertyType('kastely')).toBe('EGYEB_ISMERETLEN')
      expect(getPropertyTypeConfig('unknown').key).toBe('EGYEB_ISMERETLEN')
      expect(getPropertyTypeConfig('lakas').label).toBe('Lakás')
    })
  })

  describe('clampQualityScore & getQualityConfig', () => {
    it('clamps quality scores into 0-100 integer range', () => {
      expect(clampQualityScore(null)).toBeNull()
      expect(clampQualityScore(undefined)).toBeNull()
      expect(clampQualityScore(NaN)).toBeNull()
      expect(clampQualityScore(115)).toBe(100)
      expect(clampQualityScore(-5)).toBe(0)
      expect(clampQualityScore(82.6)).toBe(83)
    })

    it('maps scores to correct quality tiers', () => {
      expect(getQualityConfig(90).key).toBe('EXCELLENT')
      expect(getQualityConfig(85).key).toBe('EXCELLENT')
      expect(getQualityConfig(84).key).toBe('ACCEPTABLE')
      expect(getQualityConfig(70).key).toBe('ACCEPTABLE')
      expect(getQualityConfig(69).key).toBe('SUSPICIOUS')
      expect(getQualityConfig(0).key).toBe('SUSPICIOUS')
    })
  })

  describe('getEffectivePrice & getEffectiveSqmPrice', () => {
    it('computes effective lowest active price from multiple sources', () => {
      const item = {
        id: '1',
        portal: 'Duna House',
        url: 'https://dh.hu/1',
        title: 'Teszt',
        price: '80 M Ft',
        location: 'Debrecen',
        city: 'Debrecen',
        priceInfo: { amountHuf: 80_000_000, amountMillion: 80 },
        detailsInfo: { areaSquareMeters: 100 },
        sourceListings: [
          { portal: 'DH', url: 'https://dh.hu/1', priceHuf: 80_000_000, status: 'ACTIVE' },
          { portal: 'OC', url: 'https://oc.hu/1', priceHuf: 75_000_000, status: 'ACTIVE' },
          { portal: 'JF', url: 'https://jofogas.hu/1', priceHuf: 70_000_000, status: 'INACTIVE' },
        ],
      } as IngatlanItem

      expect(getEffectivePrice(item)).toEqual({ priceFt: 75_000_000, priceMillion: 75 })
    })

    it('falls back to item.priceInfo when sourceListings is empty or inactive', () => {
      const item = {
        id: '2',
        portal: 'OC',
        url: 'https://oc.hu/2',
        priceInfo: { amountHuf: 50_000_000, amountMillion: 50 },
        sourceListings: [],
      } as unknown as IngatlanItem

      expect(getEffectivePrice(item)).toEqual({ priceFt: 50_000_000, priceMillion: 50 })
    })

    it('computes effective sqm price with priority order and fallbacks', () => {
      // 1. Direct pricePerSquareMeter
      const directItem = {
        detailsInfo: { pricePerSquareMeter: 900_000, areaSquareMeters: 50 },
      } as unknown as IngatlanItem
      expect(getEffectiveSqmPrice(directItem)).toBe(900_000)

      // 2. Computed from amountHuf / area
      const hufItem = {
        priceInfo: { amountHuf: 40_000_000 },
        detailsInfo: { areaSquareMeters: 50 },
      } as unknown as IngatlanItem
      expect(getEffectiveSqmPrice(hufItem)).toBe(800_000)

      // 3. Computed from amountMillion / area
      const millionItem = {
        priceInfo: { amountMillion: 40 },
        detailsInfo: { areaSquareMeters: 50 },
      } as unknown as IngatlanItem
      expect(getEffectiveSqmPrice(millionItem)).toBe(800_000)

      // Missing area or price returns null
      const invalidItem = {
        priceInfo: {},
        detailsInfo: { areaSquareMeters: 0 },
      } as unknown as IngatlanItem
      expect(getEffectiveSqmPrice(invalidItem)).toBeNull()
    })
  })

  describe('source listing counts', () => {
    it('calculates item and total source listing counts accurately', () => {
      const item1 = {
        sourcesCount: 3,
        sourceListings: [{ portal: 'dh' }, { portal: 'oc' }],
      } as unknown as IngatlanItem
      const item2 = {
        sourcesCount: 1,
        sourceListings: [],
      } as unknown as IngatlanItem

      expect(getItemSourceListingsCount(item1)).toBe(3)
      expect(getItemSourceListingsCount(item2)).toBe(1)
      expect(getTotalSourceListingsCount([item1, item2])).toBe(4)
      expect(getTotalSourceListingsCount([])).toBe(0)
    })
  })

  describe('portal helpers', () => {
    it('normalizes portal keys regardless of accents, cases, and extra spaces', () => {
      expect(normalizePortalKey('Duna House')).toBe('dh')
      expect(normalizePortalKey('Otthon Centrum')).toBe('oc')
      expect(normalizePortalKey('Jófogás')).toBe('jofogas')
      expect(normalizePortalKey('Ingatlanbazár')).toBe('ingatlanbazar')
      expect(normalizePortalKey('Költözzbe')).toBe('koltozzbe')
      expect(normalizePortalKey('Ingatlan.com')).toBe('ingatlancom')
      expect(normalizePortalKey(null)).toBe('')
    })

    it('returns theme styles for portals with fallback', () => {
      expect(getPortalStyle('dh').text).toContain('orange')
      expect(getPortalStyle('ismeretlen_portal').text).toContain('blue')
    })

    it('detects portal from valid and invalid URLs', () => {
      expect(detectPortalFromUrl('https://www.dh.hu/ingatlan/123')).toEqual({
        portalId: 'dh',
        portalName: 'Duna House',
      })
      expect(detectPortalFromUrl('https://aldomain.koltozzbe.hu/hirdetes/456')).toEqual({
        portalId: 'koltozzbe',
        portalName: 'Költözzbe',
      })
      expect(detectPortalFromUrl('invalid-url')).toEqual({
        portalId: 'egyeb',
        portalName: 'Hirdetés',
      })
      expect(detectPortalFromUrl('https://example.com/item')).toEqual({
        portalId: 'egyeb',
        portalName: 'Hirdetés',
      })
    })
  })

  describe('openExternalUrl', () => {
    it('opens URL in a new tab with security flags', () => {
      const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)

      openExternalUrl('https://dh.hu/ingatlan/1')
      expect(openSpy).toHaveBeenCalledWith('https://dh.hu/ingatlan/1', '_blank', 'noopener,noreferrer')

      openSpy.mockClear()
      openExternalUrl(null)
      openExternalUrl('')
      expect(openSpy).not.toHaveBeenCalled()

      openSpy.mockRestore()
    })
  })
})
