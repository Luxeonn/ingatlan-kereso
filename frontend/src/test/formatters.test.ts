import { describe, it, expect } from 'vitest'
import {
  formatPriceMillion,
  formatPriceFt,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  formatDate,
} from '../utils/formatters'

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
})
