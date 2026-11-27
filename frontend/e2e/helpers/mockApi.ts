import type { Page, Route } from '@playwright/test'
import type {
  CitiesResponse,
  DashboardResponseDto,
  SearchCriteria,
  SearchPreset,
  SearchPresetInput,
  StatusResponse,
} from '../../src/types/api'
import type { components } from '../../src/types/generated-api'
import type {
  IngatlanItem,
  ListingObservation,
  PriceHistoryEntry,
  SourceListing,
  StatusLogEntry,
} from '../../src/types/property'
import type {
  IngatlanComHtmlImportRequest,
  IngatlanComHtmlImportResponse,
  PortalDescriptor,
} from '../../src/types/portal'
import type {
  DedupActionResponseDto,
  DedupMergeRequestDto,
  DedupOverrideDto,
  DedupSplitRequestDto,
} from '../../src/api/dedupApi'
import type {
  ClearHistoryResponse,
  MarkViewedResponse,
  PropertyHideResponse,
} from '../../src/api/propertyApi'

const LOCAL_HOSTNAMES = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])

const SVG_PLACEHOLDER =
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250">' +
  '<rect width="400" height="250" fill="#e2e8f0"/>' +
  '<text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#64748b" font-family="sans-serif" font-size="14">Mock Listing</text>' +
  '</svg>'

export const DEFAULT_MOCK_CONFIG: SearchCriteria = {
  cities: ['Debrecen'],
  minPriceMillion: 20,
  maxPriceMillion: 120,
  minArea: 30,
  maxArea: 150,
  propertyType: 'LAKAS',
  portals: ['dh', 'oc', 'jofogas', 'koltozzbe', 'ingatlanbazar', 'ingatlancom'],
  excludeViewed: false,
  onlyDiscounted: false,
  onlyNewConstruction: false,
}

export const DEFAULT_MOCK_PORTALS: PortalDescriptor[] = [
  {
    id: 'dh',
    name: 'Duna House',
    brandColor: '#0066cc',
    homepageUrl: 'https://dh.hu',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: true,
    savedListingCount: 5,
  },
  {
    id: 'oc',
    name: 'Otthon Centrum',
    brandColor: '#ff9800',
    homepageUrl: 'https://oc.hu',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: true,
    savedListingCount: 8,
  },
  {
    id: 'jofogas',
    name: 'Jófogás',
    brandColor: '#4caf50',
    homepageUrl: 'https://jofogas.hu',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: false,
    savedListingCount: 12,
  },
  {
    id: 'koltozzbe',
    name: 'Költözzbe',
    brandColor: '#2196f3',
    homepageUrl: 'https://koltozzbe.hu',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: true,
    savedListingCount: 3,
  },
  {
    id: 'ingatlanbazar',
    name: 'Ingatlanbazár',
    brandColor: '#9c27b0',
    homepageUrl: 'https://ingatlanbazar.hu',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: false,
    savedListingCount: 15,
  },
  {
    id: 'ingatlancom',
    name: 'Ingatlan.com',
    brandColor: '#ff5722',
    homepageUrl: 'https://ingatlan.com',
    enabledByDefault: true,
    supportedTypes: ['LAKAS', 'HAZ'],
    directListingSupported: false,
    savedListingCount: 0,
  },
]

export const DEFAULT_MOCK_CITIES: CitiesResponse = {
  predefined: [
    { key: 'debrecen', name: 'Debrecen', county: 'Hajdú-Bihar', description: 'Hajdú-Bihar vármegye székhelye' },
    { key: 'budapest', name: 'Budapest', county: 'Pest', description: 'Magyarország fővárosa' },
    { key: 'szeged', name: 'Szeged', county: 'Csongrád-Csanád', description: 'Csongrád-Csanád vármegye székhelye' },
    { key: 'miskolc', name: 'Miskolc', county: 'Borsod-Abaúj-Zemplén', description: 'Borsod-Abaúj-Zemplén vármegye székhelye' },
  ],
  presets: {
    'Főbb egyetemvárosok': ['Budapest', 'Debrecen', 'Szeged'],
    'Debrecen és környéke': ['Debrecen', 'Józsa', 'Mikepércs'],
  },
  trackedCities: [],
}

export const DEFAULT_MOCK_STATUS: StatusResponse = {
  status: 'UP',
  port: 4173,
  totalInDb: 42,
  lastRunTime: '2026-11-27T14:00:00.000Z',
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
    propertyType: 'LAKAS',
    isNew: true,
    viewed: false,
    newConstruction: false,
    isBargain: true,
    percentageBelowMedian: 25,
    discoveredAt: '2026-11-05T10:00:00.000Z',
    imageUrl: '',
    sourcesCount: 1,
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
    propertyType: 'LAKAS',
    isNew: false,
    viewed: false,
    newConstruction: false,
    isBargain: false,
    percentageBelowMedian: 0,
    discoveredAt: '2026-11-06T12:00:00.000Z',
    imageUrl: '',
    sourcesCount: 1,
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
    propertyType: 'LAKAS',
    isNew: true,
    viewed: false,
    newConstruction: true,
    isBargain: false,
    percentageBelowMedian: -15,
    discoveredAt: '2026-11-07T08:00:00.000Z',
    imageUrl: '',
    sourcesCount: 1,
    priceInfo: { amountMillion: 130, amountHuf: 130_000_000, rawText: '130 M Ft' },
    detailsInfo: { areaSquareMeters: 80, roomCount: 3, pricePerSquareMeter: 1_625_000 },
  },
]

export const DEFAULT_PRICE_HISTORY: PriceHistoryEntry[] = [
  {
    id: 1,
    propertyId: 'dh-101',
    recordedAt: '2026-10-15T10:00:00.000Z',
    priceRaw: '50 M Ft',
    priceAmount: 50_000_000,
    priceMillions: 50,
    pricePerSqm: 1_000_000,
    previousPriceAmount: null,
    priceDelta: null,
    changePercent: null,
  },
  {
    id: 2,
    propertyId: 'dh-101',
    recordedAt: '2026-11-05T10:00:00.000Z',
    priceRaw: '45 M Ft',
    priceAmount: 45_000_000,
    priceMillions: 45,
    pricePerSqm: 900_000,
    previousPriceAmount: 50_000_000,
    priceDelta: -5_000_000,
    changePercent: -10,
  },
]

export const DEFAULT_OBSERVATIONS: ListingObservation[] = [
  {
    observedAt: '2026-10-15T10:00:00.000Z',
    priceHuf: 50_000_000,
    pricePerM2: 1_000_000,
    statusEvent: 'NEW',
    priceDelta: 0,
    changePercent: 0,
  },
  {
    observedAt: '2026-10-28T10:00:00.000Z',
    priceHuf: 50_000_000,
    pricePerM2: 1_000_000,
    statusEvent: 'UNCHANGED',
    priceDelta: 0,
    changePercent: 0,
  },
  {
    observedAt: '2026-11-05T10:00:00.000Z',
    priceHuf: 45_000_000,
    pricePerM2: 900_000,
    statusEvent: 'PRICE_CHANGED',
    priceDelta: -5_000_000,
    changePercent: -10,
  },
]

export const DEFAULT_STATUS_LOG: StatusLogEntry[] = [
  {
    id: 1,
    propertyId: 'dh-101',
    status: 'PRICE_DROP',
    changedAt: '2026-11-05T10:00:00.000Z',
    note: 'Árcsökkentés -5 M Ft',
  },
]

export function createMockSearchResponse(
  items: IngatlanItem[] = SAMPLE_PROPERTIES,
  overrides?: Partial<DashboardResponseDto>,
): DashboardResponseDto {
  const newCount = items.filter((i) => i.isNew).length
  const prices = items
    .map((i) => i.priceInfo?.amountMillion)
    .filter((p): p is number => typeof p === 'number')
  const avgPrice = prices.length ? Math.round(prices.reduce((a, b) => a + b, 0) / prices.length) : 0

  return {
    items,
    newCount,
    totalFound: items.length,
    executedAt: new Date().toISOString(),
    portalStatuses: [
      {
        portalId: 'dh',
        portalName: 'Duna House',
        status: 'SUCCESS',
        latencyMs: 250,
        foundCount: items.filter((i) => i.portal === 'Duna House').length,
      },
      {
        portalId: 'oc',
        portalName: 'Otthon Centrum',
        status: 'SUCCESS',
        latencyMs: 280,
        foundCount: items.filter((i) => i.portal === 'Otthon Centrum').length,
      },
      {
        portalId: 'jofogas',
        portalName: 'Jófogás',
        status: 'SUCCESS',
        latencyMs: 310,
        foundCount: items.filter((i) => i.portal === 'Jófogás').length,
      },
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
  portals?: PortalDescriptor[]
  cities?: CitiesResponse
  status?: StatusResponse
  presets?: SearchPreset[]
  historyItems?: IngatlanItem[]
  hiddenItems?: IngatlanItem[]
  discountedItems?: IngatlanItem[]
  searchItems?: IngatlanItem[]
  dedupOverrides?: DedupOverrideDto[]
  priceHistory?: PriceHistoryEntry[]
  observations?: ListingObservation[]
  statusLog?: StatusLogEntry[]
  interceptExternal?: boolean
  onSearchRequest?: (route: Route, criteria: SearchCriteria) => Promise<void> | void
  onImportHtml?: (
    route: Route,
    payload: IngatlanComHtmlImportRequest,
  ) => Promise<void> | void
}

interface OverrideSnapshot {
  affectedUrls: string[]
  beforeItems: IngatlanItem[]
}

interface MockState {
  config: SearchCriteria
  presets: SearchPreset[]
  nextPresetId: number
  searchItems: IngatlanItem[]
  historyItems: IngatlanItem[]
  hiddenItems: IngatlanItem[]
  discountedItems: IngatlanItem[]
  overrides: DedupOverrideDto[]
  nextOverrideId: number
  overrideSnapshots: Map<number, OverrideSnapshot>
  priceHistory: PriceHistoryEntry[]
  observations: ListingObservation[]
  statusLog: StatusLogEntry[]
}

function jsonResponse(route: Route, payload: unknown, status = 200): Promise<void> {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(payload),
  })
}

function apiErrorResponse(
  route: Route,
  status: number,
  error: string,
  message: string,
): Promise<void> {
  let reqPath: string
  try {
    reqPath = new URL(route.request().url()).pathname
  } catch {
    reqPath = ''
  }
  const payload: components['schemas']['ApiErrorResponse'] = {
    error,
    message,
    path: reqPath,
    status,
    timestamp: new Date().toISOString(),
  }
  return jsonResponse(route, payload, status)
}

function parseJsonBody<T>(route: Route): T | null {
  try {
    return route.request().postDataJSON() as T
  } catch {
    return null
  }
}

function cloneItem(item: IngatlanItem): IngatlanItem {
  return {
    ...item,
    priceInfo: item.priceInfo ? { ...item.priceInfo } : undefined,
    detailsInfo: item.detailsInfo ? { ...item.detailsInfo } : undefined,
    sourceListings: item.sourceListings?.map((s) => ({ ...s })),
  }
}

function collectItemUrls(item: IngatlanItem): string[] {
  const urls: string[] = []
  if (item.url) urls.push(item.url)
  for (const src of item.sourceListings ?? []) {
    if (src.url) urls.push(src.url)
  }
  return urls
}

function itemOverlapsUrls(item: IngatlanItem, urlSet: Set<string>): boolean {
  return collectItemUrls(item).some((u) => urlSet.has(u))
}

function applySourcePricing(item: IngatlanItem, source: SourceListing | undefined): IngatlanItem {
  if (!source || source.priceHuf === undefined || source.priceHuf === null) {
    return item
  }
  const amountHuf = source.priceHuf
  const rawMillion = Number((amountHuf / 1_000_000).toFixed(2))
  const priceText = `${Number.isInteger(rawMillion) ? rawMillion : rawMillion.toFixed(1)} M Ft`
  const area = item.detailsInfo?.areaSquareMeters
  const computedSqm =
    source.pricePerM2 ??
    (area && area > 0 ? Math.round(amountHuf / area) : item.detailsInfo?.pricePerSquareMeter)

  return {
    ...item,
    price: priceText,
    priceInfo: {
      ...item.priceInfo,
      amountMillion: rawMillion,
      amountHuf,
      rawText: priceText,
    },
    detailsInfo: {
      ...item.detailsInfo,
      pricePerSquareMeter: computedSqm,
    },
  }
}

function buildClusterSources(item: IngatlanItem): NonNullable<IngatlanItem['sourceListings']> {
  if (item.sourceListings && item.sourceListings.length > 0) {
    return item.sourceListings
  }
  return [
    {
      portal: item.portal,
      url: item.url,
      title: item.title,
      priceHuf: item.priceInfo?.amountHuf ?? null,
      pricePerM2: item.detailsInfo?.pricePerSquareMeter ?? null,
      status: 'ACTIVE',
    },
  ]
}

async function handlePresetRoutes(
  route: Route,
  pathname: string,
  method: string,
  state: MockState,
): Promise<boolean> {
  const useMatch = pathname.match(/^\/api\/presets\/(\d+)\/use$/)
  if (useMatch) {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const id = Number(useMatch[1])
    const found = state.presets.find((p) => p.id === id)
    if (!found) {
      await apiErrorResponse(route, 404, 'Not Found', 'Sablon nem található')
      return true
    }
    found.lastUsedAt = new Date().toISOString()
    await jsonResponse(route, found)
    return true
  }

  const itemMatch = pathname.match(/^\/api\/presets\/(\d+)$/)
  if (itemMatch) {
    const id = Number(itemMatch[1])
    if (method === 'GET') {
      const target = state.presets.find((p) => p.id === id)
      if (!target) {
        await apiErrorResponse(route, 404, 'Not Found', 'Sablon nem található')
        return true
      }
      await jsonResponse(route, target)
      return true
    }
    if (method === 'PUT') {
      const body = parseJsonBody<SearchPresetInput>(route)
      const target = state.presets.find((p) => p.id === id)
      if (!target || !body) {
        await apiErrorResponse(route, 404, 'Not Found', 'Sablon nem található')
        return true
      }
      target.name = body.name
      if (body.criteria) {
        target.criteria = body.criteria
      }
      target.updatedAt = new Date().toISOString()
      await jsonResponse(route, target)
      return true
    }
    if (method === 'DELETE') {
      const exists = state.presets.some((p) => p.id === id)
      if (!exists) {
        await apiErrorResponse(route, 404, 'Not Found', 'Sablon nem található')
        return true
      }
      state.presets = state.presets.filter((p) => p.id !== id)
      await jsonResponse(route, { success: true })
      return true
    }
    await apiErrorResponse(route, 405, 'Method Not Allowed', 'Nem támogatott metódus')
    return true
  }

  if (pathname === '/api/presets') {
    if (method === 'GET') {
      await jsonResponse(route, state.presets)
      return true
    }
    if (method === 'POST') {
      const body = parseJsonBody<SearchPresetInput>(route)
      const now = new Date().toISOString()
      const created: SearchPreset = {
        id: state.nextPresetId++,
        name: body?.name ?? 'Mentett sablon',
        criteria: body?.criteria ?? state.config,
        createdAt: now,
        updatedAt: now,
        lastUsedAt: null,
      }
      state.presets = [created, ...state.presets]
      await jsonResponse(route, created)
      return true
    }
    await apiErrorResponse(route, 405, 'Method Not Allowed', 'Nem támogatott metódus')
    return true
  }

  return false
}

function replaceOverlappingInList(
  items: IngatlanItem[],
  overlapSet: Set<string>,
  replacements: IngatlanItem[],
): IngatlanItem[] {
  let insertAt = -1
  const remaining: IngatlanItem[] = []
  for (const item of items) {
    if (itemOverlapsUrls(item, overlapSet)) {
      if (insertAt === -1) insertAt = remaining.length
    } else {
      remaining.push(item)
    }
  }
  if (insertAt === -1) {
    return [...remaining, ...replacements]
  }
  return [
    ...remaining.slice(0, insertAt),
    ...replacements,
    ...remaining.slice(insertAt),
  ]
}

function reverseOverrideOnState(removed: DedupOverrideDto, state: MockState): IngatlanItem[] {
  const snapshot = state.overrideSnapshots.get(removed.id)
  if (snapshot) {
    state.overrideSnapshots.delete(removed.id)
    const overlapSet = new Set([removed.urlA, removed.urlB, ...snapshot.affectedUrls])
    const restored = snapshot.beforeItems.map(cloneItem)
    if (state.searchItems.some((item) => itemOverlapsUrls(item, overlapSet))) {
      state.searchItems = replaceOverlappingInList(state.searchItems, overlapSet, restored)
    } else if (state.hiddenItems.some((item) => itemOverlapsUrls(item, overlapSet))) {
      state.hiddenItems = replaceOverlappingInList(state.hiddenItems, overlapSet, restored)
    }
    return restored
  }

  const pairSet = new Set([removed.urlA, removed.urlB])
  if (removed.type === 'FORCE_SPLIT') {
    const matched = state.searchItems.filter((item) => itemOverlapsUrls(item, pairSet))
    if (matched.length >= 2) {
      const primary = matched[0]!
      const combined = matched.flatMap((m) => buildClusterSources(m))
      const remerged: IngatlanItem = {
        ...cloneItem(primary),
        manuallySplit: false,
        sourcesCount: combined.length,
        sourceListings: combined,
      }
      state.searchItems = replaceOverlappingInList(state.searchItems, pairSet, [remerged])
      return [remerged]
    }
  }

  return state.searchItems.filter((item) => itemOverlapsUrls(item, pairSet))
}

function performMergeOnCollection(
  collection: IngatlanItem[],
  urls: string[],
  state: MockState,
): { nextCollection: IngatlanItem[]; merged: IngatlanItem | null } {
  const urlSet = new Set(urls)
  const matched = collection.filter((item) => itemOverlapsUrls(item, urlSet))
  const primary = matched[0]
  if (!primary || matched.length < 2) {
    return { nextCollection: collection, merged: null }
  }
  const beforeSnapshot = matched.map(cloneItem)
  const combinedSources = matched.flatMap((m) => buildClusterSources(m))
  const merged: IngatlanItem = {
    ...cloneItem(primary),
    manuallyMerged: true,
    sourcesCount: combinedSources.length,
    sourceListings: combinedSources,
  }
  const nextCollection = replaceOverlappingInList(collection, urlSet, [merged])
  const allAffectedUrls = Array.from(new Set(matched.flatMap(collectItemUrls)))

  for (let i = 1; i < urls.length; i += 1) {
    const secondUrl = urls[i]
    if (secondUrl && urls[0]) {
      const overrideId = state.nextOverrideId++
      state.overrides.push({
        id: overrideId,
        urlA: urls[0],
        urlB: secondUrl,
        type: 'FORCE_MERGE',
        normalizedCity: primary.city,
        createdAt: new Date().toISOString(),
      })
      state.overrideSnapshots.set(overrideId, {
        affectedUrls: allAffectedUrls,
        beforeItems: beforeSnapshot,
      })
    }
  }
  return { nextCollection, merged }
}

function performSplitOnSearchItems(sourceUrl: string, state: MockState): IngatlanItem[] {
  const clusterItem = state.searchItems.find(
    (item) =>
      item.url === sourceUrl ||
      (item.sourceListings ?? []).some((s) => s.url === sourceUrl),
  )
  if (!clusterItem || !clusterItem.sourceListings || clusterItem.sourceListings.length <= 1) {
    return []
  }

  const beforeSnapshot = [cloneItem(clusterItem)]
  const allAffectedUrls = collectItemUrls(clusterItem)
  const splitSource = clusterItem.sourceListings.find((s) => s.url === sourceUrl)
  const remaining = clusterItem.sourceListings.filter((s) => s.url !== sourceUrl)
  const primaryNext = remaining[0]
  const overrideId = state.nextOverrideId++

  const updatedBase: IngatlanItem = {
    ...cloneItem(clusterItem),
    url: primaryNext?.url ?? clusterItem.url,
    portal: primaryNext?.portal ?? clusterItem.portal,
    title: primaryNext?.title ?? clusterItem.title,
    manuallySplit: true,
    sourcesCount: remaining.length,
    sourceListings: remaining,
  }
  const updatedCluster = applySourcePricing(updatedBase, primaryNext)

  const detachedBase: IngatlanItem = {
    ...cloneItem(clusterItem),
    id: `${clusterItem.id}-split-${overrideId}`,
    portal: splitSource?.portal ?? clusterItem.portal,
    url: splitSource?.url ?? sourceUrl,
    title: splitSource?.title ?? clusterItem.title,
    manuallySplit: true,
    manuallyMerged: false,
    sourcesCount: 1,
    sourceListings: splitSource ? [splitSource] : [],
  }
  const detachedItem = applySourcePricing(detachedBase, splitSource)

  state.searchItems = state.searchItems.flatMap((item) =>
    item.id === clusterItem.id ? [updatedCluster, detachedItem] : [item],
  )

  if (primaryNext?.url) {
    state.overrides.push({
      id: overrideId,
      urlA: sourceUrl,
      urlB: primaryNext.url,
      type: 'FORCE_SPLIT',
      normalizedCity: clusterItem.city,
      createdAt: new Date().toISOString(),
    })
    state.overrideSnapshots.set(overrideId, {
      affectedUrls: allAffectedUrls,
      beforeItems: beforeSnapshot,
    })
  }

  return [updatedCluster, detachedItem]
}

async function handleDedupRoutes(
  route: Route,
  pathname: string,
  method: string,
  url: URL,
  state: MockState,
): Promise<boolean> {
  const overrideIdMatch = pathname.match(/^\/api\/dedup\/overrides\/(\d+)$/)
  if (overrideIdMatch) {
    const id = Number(overrideIdMatch[1])
    if (method === 'GET') {
      const found = state.overrides.find((o) => o.id === id)
      if (!found) {
        await apiErrorResponse(route, 404, 'Not Found', 'Felülbírálás nem található')
        return true
      }
      await jsonResponse(route, found)
      return true
    }
    if (method === 'DELETE') {
      const removed = state.overrides.find((o) => o.id === id)
      if (!removed) {
        await apiErrorResponse(route, 404, 'Not Found', 'Felülbírálás nem található')
        return true
      }
      state.overrides = state.overrides.filter((o) => o.id !== id)
      const affectedItems = reverseOverrideOnState(removed, state)
      const response: DedupActionResponseDto = {
        items: affectedItems,
        overrides: state.overrides,
        message: 'Kézi felülbírálás visszavonva',
      }
      await jsonResponse(route, response)
      return true
    }
    await apiErrorResponse(route, 405, 'Method Not Allowed', 'Nem támogatott metódus')
    return true
  }

  if (pathname === '/api/dedup/overrides') {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    const singleUrl = url.searchParams.get('url')
    const multiUrls = url.searchParams.getAll('urls')
    const lookupSet = new Set(
      [singleUrl, ...multiUrls].filter((u): u is string => Boolean(u && u.trim())),
    )
    const matching =
      lookupSet.size === 0
        ? state.overrides
        : state.overrides.filter((o) => lookupSet.has(o.urlA) || lookupSet.has(o.urlB))
    await jsonResponse(route, matching)
    return true
  }

  if (pathname === '/api/dedup/merge') {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const body = parseJsonBody<DedupMergeRequestDto>(route)
    const urls = body?.urls ?? []
    let mergedItem: IngatlanItem | null
    const searchMerge = performMergeOnCollection(state.searchItems, urls, state)
    if (searchMerge.merged) {
      state.searchItems = searchMerge.nextCollection
      mergedItem = searchMerge.merged
    } else {
      const hiddenMerge = performMergeOnCollection(state.hiddenItems, urls, state)
      state.hiddenItems = hiddenMerge.nextCollection
      mergedItem = hiddenMerge.merged
    }
    const response: DedupActionResponseDto = {
      items: mergedItem ? [mergedItem] : [],
      overrides: state.overrides,
      message: 'Hirdetések sikeresen összevonva',
    }
    await jsonResponse(route, response)
    return true
  }

  if (pathname === '/api/dedup/split') {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const body = parseJsonBody<DedupSplitRequestDto>(route)
    const splitResultItems = body?.sourceUrl
      ? performSplitOnSearchItems(body.sourceUrl, state)
      : []
    const response: DedupActionResponseDto = {
      items: splitResultItems,
      overrides: state.overrides,
      message: 'Forrás sikeresen leválasztva',
    }
    await jsonResponse(route, response)
    return true
  }

  return false
}

function resolvePropertyId(
  url: URL,
  pathPropertyId: string | null,
  state: MockState,
): string {
  if (pathPropertyId && pathPropertyId.trim()) {
    return decodeURIComponent(pathPropertyId.trim())
  }
  const paramId = url.searchParams.get('id')
  if (paramId && paramId.trim()) {
    return paramId.trim()
  }
  const paramUrl = url.searchParams.get('url')
  if (paramUrl && paramUrl.trim()) {
    const lookupUrl = paramUrl.trim()
    const pools = [state.searchItems, state.hiddenItems, state.historyItems, SAMPLE_PROPERTIES]
    for (const pool of pools) {
      const matched = pool.find(
        (item) =>
          item.url === lookupUrl ||
          (item.sourceListings ?? []).some((s) => s.url === lookupUrl),
      )
      if (matched) return matched.id
    }
  }
  return 'dh-101'
}

function matchesPropertyIdOrUrls(
  item: IngatlanItem,
  id: string,
  urlSet: Set<string>,
): boolean {
  if (item.id === id) return true
  if (urlSet.size > 0 && itemOverlapsUrls(item, urlSet)) return true
  return false
}

function findOrSynthesizeHiddenTarget(
  id: string,
  urls: string[],
  state: MockState,
): IngatlanItem {
  const urlSet = new Set(urls.filter(Boolean))
  const pools = [state.searchItems, state.hiddenItems, state.historyItems, SAMPLE_PROPERTIES]
  for (const pool of pools) {
    const found = pool.find((item) => matchesPropertyIdOrUrls(item, id, urlSet))
    if (found) return cloneItem(found)
  }
  const primaryUrl = urls[0] ?? `https://ingatlan.local/${encodeURIComponent(id)}`
  return {
    ...cloneItem(SAMPLE_PROPERTIES[0]!),
    id,
    url: primaryUrl,
    title: `Elrejtett ingatlan (${id})`,
    hidden: true,
    sourcesCount: 1,
    sourceListings: [{ portal: 'Duna House', url: primaryUrl, priceHuf: 45_000_000, status: 'ACTIVE' }],
  }
}

async function handlePropertyRoutes(
  route: Route,
  pathname: string,
  method: string,
  url: URL,
  state: MockState,
): Promise<boolean> {
  const priceHistoryMatch = pathname.match(/^\/api\/properties\/([^/]+)\/price-history$/)
  if (pathname === '/api/properties/price-history' || priceHistoryMatch) {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    const propertyId = resolvePropertyId(url, priceHistoryMatch?.[1] ?? null, state)
    await jsonResponse(
      route,
      state.priceHistory.map((entry) => ({ ...entry, propertyId })),
    )
    return true
  }

  const observationsMatch = pathname.match(/^\/api\/properties\/([^/]+)\/observations$/)
  if (pathname === '/api/properties/observations' || observationsMatch) {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    await jsonResponse(route, state.observations)
    return true
  }

  const statusLogMatch = pathname.match(/^\/api\/properties\/([^/]+)\/status-log$/)
  if (pathname === '/api/properties/status-log' || statusLogMatch) {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    const propertyId = resolvePropertyId(url, statusLogMatch?.[1] ?? null, state)
    await jsonResponse(
      route,
      state.statusLog.map((entry) => ({ ...entry, propertyId })),
    )
    return true
  }

  if (pathname === '/api/properties/hidden') {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    await jsonResponse(route, state.hiddenItems)
    return true
  }

  if (pathname === '/api/properties/discounted') {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    await jsonResponse(route, state.discountedItems)
    return true
  }

  const hideMatch = pathname.match(/^\/api\/properties\/([^/]+)\/hide$/)
  if (hideMatch) {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const id = decodeURIComponent(hideMatch[1] ?? '')
    const body = parseJsonBody<{ urls?: string[] }>(route)
    const urls = body?.urls ?? []
    const urlSet = new Set(urls.filter(Boolean))

    state.searchItems = state.searchItems.map((item) =>
      matchesPropertyIdOrUrls(item, id, urlSet) ? { ...item, hidden: true } : item,
    )
    const target = findOrSynthesizeHiddenTarget(id, urls, state)
    target.hidden = true
    const alreadyHidden = state.hiddenItems.some((item) =>
      matchesPropertyIdOrUrls(item, id, urlSet),
    )
    if (!alreadyHidden) {
      state.hiddenItems = [target, ...state.hiddenItems]
    }
    const response: PropertyHideResponse = { success: true, hidden: true, id }
    await jsonResponse(route, response)
    return true
  }

  const restoreMatch = pathname.match(/^\/api\/properties\/([^/]+)\/restore$/)
  if (restoreMatch) {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const id = decodeURIComponent(restoreMatch[1] ?? '')
    const body = parseJsonBody<{ urls?: string[] }>(route)
    const urlSet = new Set((body?.urls ?? []).filter(Boolean))

    state.searchItems = state.searchItems.map((item) =>
      matchesPropertyIdOrUrls(item, id, urlSet) ? { ...item, hidden: false } : item,
    )
    state.hiddenItems = state.hiddenItems.filter(
      (item) => !matchesPropertyIdOrUrls(item, id, urlSet),
    )
    const response: PropertyHideResponse = { success: true, hidden: false, id }
    await jsonResponse(route, response)
    return true
  }

  if (pathname === '/api/items/viewed') {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const body = parseJsonBody<{ url?: string }>(route)
    const targetUrl = body?.url ?? ''
    state.searchItems = state.searchItems.map((item) =>
      item.url === targetUrl ? { ...item, viewed: true } : item,
    )
    const response: MarkViewedResponse = { success: true, marked: true, url: targetUrl }
    await jsonResponse(route, response)
    return true
  }

  return false
}

async function handleHistoryAndPortalRoutes(
  route: Route,
  pathname: string,
  method: string,
  state: MockState,
  options: SetupMockOptions,
): Promise<boolean> {
  if (pathname === '/api/history/clear') {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    state.historyItems = []
    state.hiddenItems = []
    state.discountedItems = []
    const response: ClearHistoryResponse = { success: true, clearedAll: true }
    await jsonResponse(route, response)
    return true
  }

  if (pathname === '/api/history') {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    await jsonResponse(route, state.historyItems)
    return true
  }

  if (pathname === '/api/portals/ingatlancom/import-html') {
    if (method !== 'POST') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
      return true
    }
    const payload = parseJsonBody<IngatlanComHtmlImportRequest>(route) ?? { html: '' }
    if (options.onImportHtml) {
      await options.onImportHtml(route, payload)
      return true
    }
    if (!payload.html || payload.html.trim().length === 0) {
      await apiErrorResponse(
        route,
        400,
        'Bad Request',
        'A HTML tartalom nem lehet üres.',
      )
      return true
    }
    const importedItem: IngatlanItem = {
      id: 'ic-imported-901',
      portal: 'Ingatlan.com',
      url: 'https://ingatlan.com/34567890',
      title: 'Importált debreceni belvárosi tégla lakás',
      price: '62 M Ft',
      location: 'Debrecen, Belváros',
      city: payload.city ?? 'Debrecen',
      propertyType: payload.propertyType ?? 'LAKAS',
      isNew: true,
      viewed: false,
      newConstruction: false,
      isBargain: false,
      sourcesCount: 1,
      priceInfo: { amountMillion: 62, amountHuf: 62_000_000, rawText: '62 M Ft' },
      detailsInfo: { areaSquareMeters: 64, roomCount: 2, pricePerSquareMeter: 968_750 },
    }
    if (!state.searchItems.some((item) => item.id === importedItem.id)) {
      state.searchItems = [importedItem, ...state.searchItems]
    }
    const response: IngatlanComHtmlImportResponse = {
      parsedCount: 1,
      savedCount: 1,
      newlyAddedCount: 1,
      duplicateCount: 0,
      city: payload.city ?? 'Debrecen',
      items: [importedItem],
    }
    await jsonResponse(route, response)
    return true
  }

  if (pathname === '/api/portals') {
    if (method !== 'GET') {
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
      return true
    }
    await jsonResponse(route, options.portals ?? DEFAULT_MOCK_PORTALS)
    return true
  }

  return false
}

function wrapSearchRouteCapture(route: Route, state: MockState): Route {
  return new Proxy(route, {
    get(target, prop) {
      if (prop === 'fulfill') {
        return async (fulfillOptions?: Parameters<Route['fulfill']>[0]) => {
          if (
            fulfillOptions &&
            (fulfillOptions.status === undefined || fulfillOptions.status === 200) &&
            typeof fulfillOptions.body === 'string'
          ) {
            try {
              const parsed = JSON.parse(fulfillOptions.body) as Partial<DashboardResponseDto>
              if (Array.isArray(parsed.items)) {
                state.searchItems = parsed.items.map(cloneItem)
              }
            } catch {
              // ignore non-JSON body
            }
          }
          return target.fulfill(fulfillOptions)
        }
      }
      const value = Reflect.get(target, prop, target)
      if (typeof value === 'function') {
        return value.bind(target)
      }
      return value
    },
  })
}

export async function setupOfflineExternalMocks(page: Page): Promise<void> {
  await page.context().route(/^https?:\/\//i, async (route) => {
    const rawUrl = route.request().url()
    let hostname: string
    try {
      hostname = new URL(rawUrl).hostname.toLowerCase()
    } catch {
      await route.abort()
      return
    }

    if (LOCAL_HOSTNAMES.has(hostname)) {
      await route.fallback()
      return
    }

    const resourceType = route.request().resourceType()
    if (resourceType === 'stylesheet' || /\.css(\?.*)?$/i.test(rawUrl)) {
      await route.fulfill({
        status: 200,
        contentType: 'text/css',
        body: '',
      })
      return
    }

    if (resourceType === 'font' || /\.(woff2?|ttf|otf|eot)(\?.*)?$/i.test(rawUrl)) {
      await route.fulfill({
        status: 200,
        contentType: 'font/woff2',
        body: '',
      })
      return
    }

    if (resourceType === 'image' || /\.(png|jpe?g|webp|gif|svg|ico)(\?.*)?$/i.test(rawUrl)) {
      await route.fulfill({
        status: 200,
        contentType: 'image/svg+xml',
        body: SVG_PLACEHOLDER,
      })
      return
    }

    if (resourceType === 'document') {
      await route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<html><body><h1>Mock External Listing</h1></body></html>',
      })
      return
    }

    await route.abort()
  })
}

export async function setupStandardApiMocks(page: Page, options: SetupMockOptions = {}): Promise<void> {
  if (options.interceptExternal !== false) {
    await setupOfflineExternalMocks(page)
  }

  const initialPresets = options.presets ? [...options.presets] : []
  const initialOverrides = options.dedupOverrides ? [...options.dedupOverrides] : []
  const state: MockState = {
    config: options.config ?? DEFAULT_MOCK_CONFIG,
    presets: initialPresets,
    nextPresetId: Math.max(99, ...initialPresets.map((p) => p.id)) + 1,
    searchItems: options.searchItems ? options.searchItems.map(cloneItem) : SAMPLE_PROPERTIES.map(cloneItem),
    historyItems: options.historyItems ? options.historyItems.map(cloneItem) : [],
    hiddenItems: options.hiddenItems ? options.hiddenItems.map(cloneItem) : [],
    discountedItems: options.discountedItems ? options.discountedItems.map(cloneItem) : [],
    overrides: initialOverrides,
    nextOverrideId: Math.max(0, ...initialOverrides.map((o) => o.id)) + 1,
    overrideSnapshots: new Map(),
    priceHistory: options.priceHistory ?? DEFAULT_PRICE_HISTORY,
    observations: options.observations ?? DEFAULT_OBSERVATIONS,
    statusLog: options.statusLog ?? DEFAULT_STATUS_LOG,
  }

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const parsedUrl = new URL(request.url())
    const pathname = parsedUrl.pathname
    const method = request.method().toUpperCase()

    if (pathname === '/api/config') {
      if (method === 'POST') {
        const nextConfig = parseJsonBody<SearchCriteria>(route)
        if (nextConfig) {
          state.config = nextConfig
        }
        await jsonResponse(route, { success: true })
        return
      }
      if (method === 'GET') {
        await jsonResponse(route, state.config)
        return
      }
      await apiErrorResponse(route, 405, 'Method Not Allowed', 'Nem támogatott metódus')
      return
    }

    if (await handlePresetRoutes(route, pathname, method, state)) return
    if (await handleDedupRoutes(route, pathname, method, parsedUrl, state)) return
    if (await handlePropertyRoutes(route, pathname, method, parsedUrl, state)) return
    if (await handleHistoryAndPortalRoutes(route, pathname, method, state, options)) return

    if (pathname === '/api/cities') {
      if (method !== 'GET') {
        await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
        return
      }
      await jsonResponse(route, options.cities ?? DEFAULT_MOCK_CITIES)
      return
    }

    if (pathname === '/api/status') {
      if (method !== 'GET') {
        await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak GET metódus támogatott')
        return
      }
      await jsonResponse(route, options.status ?? DEFAULT_MOCK_STATUS)
      return
    }

    if (pathname === '/api/search') {
      if (method !== 'POST') {
        await apiErrorResponse(route, 405, 'Method Not Allowed', 'Csak POST metódus támogatott')
        return
      }
      if (options.onSearchRequest) {
        const body = parseJsonBody<SearchCriteria>(route) ?? state.config
        await options.onSearchRequest(wrapSearchRouteCapture(route, state), body)
        return
      }
      await jsonResponse(route, createMockSearchResponse(state.searchItems))
      return
    }

    await apiErrorResponse(
      route,
      404,
      'Not Found',
      `Ismeretlen végpont: ${method} ${pathname}`,
    )
  })
}
