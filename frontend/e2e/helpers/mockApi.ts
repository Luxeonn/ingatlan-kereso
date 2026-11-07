import type { Page, Route } from '@playwright/test'
import type { DashboardResponseDto, SearchCriteria } from '../../src/types/api'
import type { IngatlanItem } from '../../src/types/property'
import type { PortalDto } from '../../src/types/portal'

export const DEFAULT_MOCK_CONFIG: SearchCriteria = {
  cities: ['Debrecen'],
  minPriceMillion: 20,
  maxPriceMillion: 120,
  minArea: 30,
  maxArea: 150,
  propertyType: 'FLAT',
  portals: ['dh', 'oc', 'jofogas', 'koltozzbe', 'ingatlanbazar', 'ingatlancom'],
  autoOpenBrowser: false,
  excludeViewed: false,
}

export const DEFAULT_MOCK_PORTALS: PortalDto[] = [
  { id: 'dh', name: 'Duna House', color: '#0066cc', searchUrl: 'https://dh.hu', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 5 },
  { id: 'oc', name: 'Otthon Centrum', color: '#ff9800', searchUrl: 'https://oc.hu', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 8 },
  { id: 'jofogas', name: 'Jófogás', color: '#4caf50', searchUrl: 'https://jofogas.hu', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 12 },
  { id: 'koltozzbe', name: 'Költözzbe', color: '#2196f3', searchUrl: 'https://koltozzbe.hu', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 3 },
  { id: 'ingatlanbazar', name: 'Ingatlanbazár', color: '#9c27b0', searchUrl: 'https://ingatlanbazar.hu', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 15 },
  { id: 'ingatlancom', name: 'Ingatlan.com', color: '#ff5722', searchUrl: 'https://ingatlan.com', supportedTypes: ['FLAT', 'HOUSE'], savedListingCount: 0 },
]

export const DEFAULT_MOCK_CITIES = {
  predefined: [
    { key: 'debrecen', name: 'Debrecen', county: 'Hajdú-Bihar' },
    { key: 'budapest', name: 'Budapest', county: 'Pest' },
    { key: 'szeged', name: 'Szeged', county: 'Csongrád-Csanád' },
    { key: 'miskolc', name: 'Miskolc', county: 'Borsod-Abaúj-Zemplén' },
  ],
  presets: {
    'Főbb egyetemvárosok': ['Budapest', 'Debrecen', 'Szeged'],
    'Debrecen és környéke': ['Debrecen', 'Józsa', 'Mikepércs'],
  },
  trackedCities: [],
}

export const SAMPLE_PROPERTIES: IngatlanItem[] = [
  {
    id: 'dh-101',
    portal: 'Duna House',
    url: 'https://dh.hu/ingatlan/dh-101',
    title: 'Nagyerdő csendes modern lakás Debrecenben',
    price: '45 M Ft',
    location: 'Debrecen, Nagyerdő',
    city: 'Debrecen',
    isNew: true,
    viewed: false,
    newConstruction: false,
    isBargain: true,
    percentageBelowMedian: 25,
    discoveredAt: '2026-11-05T10:00:00.000Z',
    imageUrl: '',
    priceInfo: { amountMillion: 45, amountHuf: 45_000_000, rawText: '45 M Ft' },
    detailsInfo: { areaSquareMeters: 50, roomCount: 2, pricePerSquareMeter: 900_000 },
  },
  {
    id: 'oc-202',
    portal: 'Otthon Centrum',
    url: 'https://oc.hu/ingatlanok/oc-202',
    title: 'Belvárosi tágas nagypolgári lakás erkélyes',
    price: '85 M Ft',
    location: 'Debrecen, Belváros',
    city: 'Debrecen',
    isNew: false,
    viewed: false,
    newConstruction: false,
    isBargain: false,
    percentageBelowMedian: 0,
    discoveredAt: '2026-11-06T12:00:00.000Z',
    imageUrl: '',
    priceInfo: { amountMillion: 85, amountHuf: 85_000_000, rawText: '85 M Ft' },
    detailsInfo: { areaSquareMeters: 120, roomCount: 4, pricePerSquareMeter: 708_333 },
  },
  {
    id: 'jf-303',
    portal: 'Jófogás',
    url: 'https://jofogas.hu/ingatlan/jf-303',
    title: 'Új építésű luxus panorámás lakás terasszal',
    price: '130 M Ft',
    location: 'Debrecen, Egyetemváros',
    city: 'Debrecen',
    isNew: true,
    viewed: false,
    newConstruction: true,
    isBargain: false,
    percentageBelowMedian: -15,
    discoveredAt: '2026-11-07T08:00:00.000Z',
    imageUrl: '',
    priceInfo: { amountMillion: 130, amountHuf: 130_000_000, rawText: '130 M Ft' },
    detailsInfo: { areaSquareMeters: 80, roomCount: 3, pricePerSquareMeter: 1_625_000 },
  },
]

export function createMockSearchResponse(
  items: IngatlanItem[] = SAMPLE_PROPERTIES,
  overrides?: Partial<DashboardResponseDto>,
): DashboardResponseDto {
  const newCount = items.filter((i) => i.isNew).length
  const prices = items.map((i) => i.priceInfo?.amountMillion).filter((p): p is number => typeof p === 'number')
  const avgPrice = prices.length ? Math.round(prices.reduce((a, b) => a + b, 0) / prices.length) : 0

  return {
    items,
    newCount,
    totalFound: items.length,
    executedAt: new Date().toISOString(),
    portalStatuses: [
      { portalId: 'dh', portalName: 'Duna House', status: 'SUCCESS', durationMs: 250, itemCount: items.filter((i) => i.portal === 'Duna House').length, botProtected: false },
      { portalId: 'oc', portalName: 'Otthon Centrum', status: 'SUCCESS', durationMs: 280, itemCount: items.filter((i) => i.portal === 'Otthon Centrum').length, botProtected: false },
      { portalId: 'jofogas', portalName: 'Jófogás', status: 'SUCCESS', durationMs: 310, itemCount: items.filter((i) => i.portal === 'Jófogás').length, botProtected: false },
    ],
    marketStats: {
      averagePriceMillion: avgPrice,
      averagePricePerM2: 1_077_777,
      minPriceMillion: prices.length ? Math.min(...prices) : 0,
      maxPriceMillion: prices.length ? Math.max(...prices) : 0,
      newRatePercent: items.length ? Math.round((newCount / items.length) * 100) : 0,
      cityBreakdown: { Debrecen: items.length },
    },
    ...overrides,
  }
}

export interface SetupMockOptions {
  config?: SearchCriteria
  portals?: PortalDto[]
  cities?: typeof DEFAULT_MOCK_CITIES
  onSearchRequest?: (route: Route, criteria: SearchCriteria) => Promise<void> | void
}

export async function setupStandardApiMocks(page: Page, options: SetupMockOptions = {}) {
  await page.route('**/api/**', async (route) => {
    const url = route.request().url()

    if (url.includes('/api/config')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(options.config ?? DEFAULT_MOCK_CONFIG),
      })
    } else if (url.includes('/api/presets')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    } else if (url.includes('/api/portals')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(options.portals ?? DEFAULT_MOCK_PORTALS),
      })
    } else if (url.includes('/api/cities')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(options.cities ?? DEFAULT_MOCK_CITIES),
      })
    } else if (url.includes('/api/properties/hidden')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    } else if (url.includes('/api/history/clear')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, clearedAll: true }),
      })
    } else if (url.includes('/api/history')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    } else if (url.includes('/api/status')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: 'UP', port: 4173, totalInDb: 0, trackedCities: {} }),
      })
    } else if (url.includes('/api/search')) {
      if (options.onSearchRequest) {
        const body = route.request().postDataJSON() as SearchCriteria
        await options.onSearchRequest(route, body)
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createMockSearchResponse()),
        })
      }
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({}),
      })
    }
  })
}
