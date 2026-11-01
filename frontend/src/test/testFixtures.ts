import type { MarketStatsDto } from '../types/api'
import type { PortalStatusDto } from '../types/portal'
import type { IngatlanItem } from '../types/property'

/**
 * Shared Object Mother / Test Data Builder factory for frontend Vitest suites.
 * Produces valid OpenAPI-typed mock objects in a single line with partial overrides.
 */
export function createMockIngatlanItem(overrides: Partial<IngatlanItem> = {}): IngatlanItem {
  const id = overrides.id ?? 'mock-1'
  const url = overrides.url ?? `https://dh.hu/ingatlan/${id}`
  return {
    id,
    url,
    title: 'Debreceni felújított tégla lakás',
    city: 'Debrecen',
    location: 'Debrecen, Belváros',
    portal: 'Duna House',
    propertyType: 'LAKAS',
    price: '45 M Ft',
    priceInfo: {
      rawText: '45 M Ft',
      amountHuf: 45_000_000,
      amountMillion: 45,
    },
    details: '60 m², 2 szoba',
    detailsInfo: {
      rawText: '60 m², 2 szoba',
      areaSquareMeters: 60,
      roomCount: 2,
      pricePerSquareMeter: 750_000,
    },
    source: {
      portalId: 'dh',
      portalName: 'Duna House',
      themeColor: '#008638',
      directListing: true,
    },
    discoveredAt: '2026-03-15 10:00:00',
    status: 'ACTIVE',
    qualityScore: 95,
    qualityDeductions: [],
    isNew: true,
    viewed: false,
    newConstruction: false,
    isBargain: false,
    sourcesCount: 1,
    ...overrides,
  }
}

export function createMockMarketStats(overrides: Partial<MarketStatsDto> = {}): MarketStatsDto {
  return {
    averagePriceMillion: 48.5,
    averagePricePerM2: 780_000,
    minPriceMillion: 28,
    maxPriceMillion: 85,
    newRatePercent: 25,
    cityBreakdown: { Debrecen: 10 },
    medianPricePerM2: 760_000,
    totalSampleSize: 10,
    insufficientData: false,
    residentialMedianPricePerM2: 760_000,
    residentialAveragePricePerM2: 780_000,
    residentialSampleSize: 10,
    residentialInsufficientData: false,
    plotMedianPricePerM2: undefined,
    plotAveragePricePerM2: undefined,
    plotSampleSize: 0,
    plotInsufficientData: true,
    segments: [],
    priceDropCount: 1,
    priceDropRatePercent: 10,
    averagePriceDropAmountHuf: 2_000_000,
    averagePriceDropPercent: 4.5,
    topPriceDrops: [],
    bargainCount: 1,
    bargainRatePercent: 10,
    ...overrides,
  }
}

export function createMockPortalStatus(overrides: Partial<PortalStatusDto> = {}): PortalStatusDto {
  return {
    portalId: 'dh',
    portalName: 'Duna House',
    status: 'SUCCESS',
    durationMs: 320,
    itemCount: 12,
    errorMessage: null,
    botProtected: false,
    ...overrides,
  }
}
