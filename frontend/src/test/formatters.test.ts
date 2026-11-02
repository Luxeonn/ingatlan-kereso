import { describe, it, expect } from 'vitest'
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
  normalizePortalKey,
  detectPortalFromUrl,
} from '../utils/formatters'
import type { IngatlanItem } from '../types/property'

describe('formatters utility', () => {
  it('formats price in millions correctly', () => {
    expect(formatPriceMillion(75.5)).toBe('75.5 M Ft')
    expect(formatPriceMillion(120)).toBe('120 M Ft')
    expect(formatPriceMillion(null)).toBe('N/A')
    expect(formatPriceMillion(undefined)).toBe('N/A')
  })

  it('formats price in HUF correctly with thousands separators', () => {
    const formatted = formatPriceFt(75500000)
    expect(formatted).toContain('75')
    expect(formatted).toContain('500')
    expect(formatted).toContain('000')
    expect(formatted).toContain('Ft')
    expect(formatPriceFt(null)).toBe('Ár a hirdetésben')
  })

  it('formats price per square meter', () => {
    const formatted = formatPricePerSqm(850000)
    expect(formatted).toContain('850')
    expect(formatted).toContain('Ft/m²')
    expect(formatPricePerSqm(0)).toBe('N/A')
  })

  it('formats floor area in m²', () => {
    expect(formatArea(95.4)).toBe('95 m²')
    expect(formatArea(null)).toBe('N/A')
  })

  it('formats room count', () => {
    expect(formatRooms(3)).toBe('3 szoba')
    expect(formatRooms(null)).toBe('')
  })

  it('formats dates safely', () => {
    expect(formatDate('')).toBe('')
    expect(formatDate(null)).toBe('')
    expect(formatDate('2026-10-15T10:00:00Z')).toBeTruthy()
  })

  it('clamps and maps quality scores into tiers', () => {
    expect(clampQualityScore(null)).toBeNull()
    expect(clampQualityScore(115)).toBe(100)
    expect(clampQualityScore(-5)).toBe(0)
    expect(getQualityConfig(90).key).toBe('EXCELLENT')
    expect(getQualityConfig(75).key).toBe('ACCEPTABLE')
    expect(getQualityConfig(50).key).toBe('SUSPICIOUS')
  })

  it('computes effective lowest active price and sqm price', () => {
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
    expect(getEffectiveSqmPrice(item)).toBe(800_000)
  })

  it('normalizes portal keys and detects portals from URLs', () => {
    expect(normalizePortalKey('Duna House')).toBe('dh')
    expect(normalizePortalKey('Otthon Centrum')).toBe('oc')
    expect(normalizePortalKey('Jófogás')).toBe('jofogas')
    expect(detectPortalFromUrl('https://www.dh.hu/ingatlan/123')).toEqual({
      portalId: 'dh',
      portalName: 'Duna House',
    })
    expect(detectPortalFromUrl('invalid-url')).toEqual({
      portalId: 'egyeb',
      portalName: 'Hirdetés',
    })
  })
})
