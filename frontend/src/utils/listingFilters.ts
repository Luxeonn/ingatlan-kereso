import type { SortOption } from '../store/useFilterStore'
import type { IngatlanItem } from '../types/property'
import {
  getEffectivePrice,
  getPrimaryPortalKey,
  getItemSourceListingsCount,
  hasListingImage,
} from './formatters'

export function normalizeSearchText(str?: string | null): string {
  return (str || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

export function isItemBargain(item: IngatlanItem): boolean {
  if (!item) return false
  return Boolean(
    item.isBargain ||
      (item.percentageBelowMedian !== null &&
        item.percentageBelowMedian !== undefined &&
        item.percentageBelowMedian >= 20),
  )
}

export function hasItemPriceDrop(item: IngatlanItem): boolean {
  if (!item) return false
  return Boolean(
    item.status === 'PRICE_DROP' ||
      (item.priceDelta !== null && item.priceDelta !== undefined && item.priceDelta < 0),
  )
}

function getPriceHuf(item: IngatlanItem, fallback: number): number {
  const { priceFt, priceMillion } = getEffectivePrice(item)
  if (typeof priceFt === 'number' && !Number.isNaN(priceFt) && priceFt > 0) {
    return priceFt
  }
  if (typeof priceMillion === 'number' && !Number.isNaN(priceMillion) && priceMillion > 0) {
    return Math.round(priceMillion * 1_000_000)
  }
  return fallback
}

export function getBargainPercentage(item: IngatlanItem): number {
  if (item.percentageBelowMedian !== null && item.percentageBelowMedian !== undefined) {
    return item.percentageBelowMedian
  }
  return -Infinity
}

function getArea(item: IngatlanItem): number {
  if (
    item.detailsInfo &&
    item.detailsInfo.areaSquareMeters !== undefined &&
    !isNaN(item.detailsInfo.areaSquareMeters)
  ) {
    return item.detailsInfo.areaSquareMeters
  }
  return 0
}

function comparePriceAsc(a: IngatlanItem, b: IngatlanItem): number {
  const pA = getPriceHuf(a, Infinity)
  const pB = getPriceHuf(b, Infinity)
  if (pA === pB) return 0
  return pA - pB
}

function comparePriceDesc(a: IngatlanItem, b: IngatlanItem): number {
  const pA = getPriceHuf(a, -Infinity)
  const pB = getPriceHuf(b, -Infinity)
  if (pA === pB) return 0
  return pB - pA
}

function compareBargainDesc(a: IngatlanItem, b: IngatlanItem): number {
  const bA = getBargainPercentage(a)
  const bB = getBargainPercentage(b)
  if (bA === bB) return 0
  return bB - bA
}

const COMPARATORS: Record<SortOption, (a: IngatlanItem, b: IngatlanItem) => number> = {
  price_asc: comparePriceAsc,
  price_desc: comparePriceDesc,
  area_desc: (a, b) => getArea(b) - getArea(a),
  date_desc: (a, b) => (b.discoveredAt || '').localeCompare(a.discoveredAt || ''),
  bargain_desc: compareBargainDesc,
}

function compareItems(a: IngatlanItem, b: IngatlanItem, sortOption: SortOption): number {
  const comparator = COMPARATORS[sortOption]
  return comparator ? comparator(a, b) : 0
}

function buildSearchableText(item: IngatlanItem): string {
  const area = item.detailsInfo?.areaSquareMeters
  const areaStr = area ? `${Math.round(area)} m2 ${Math.round(area)} m²` : ''
  const roomsStr = item.detailsInfo?.roomCount ? `${item.detailsInfo.roomCount} szoba` : ''
  const priceStr = item.priceInfo?.amountMillion ? `${item.priceInfo.amountMillion} m ft` : ''
  return normalizeSearchText(
    [
      item.title,
      item.city,
      item.location,
      item.portal,
      item.price,
      priceStr,
      item.details,
      areaStr,
      roomsStr,
      item.newConstruction ? 'uj epitesu' : '',
    ].join(' '),
  )
}

function matchesQuickSearch(item: IngatlanItem, tokens: string[]): boolean {
  const searchable = buildSearchableText(item)
  return tokens.every((tok) => searchable.includes(tok))
}

export interface ItemFilterOptions {
  onlyNew?: boolean
  excludeViewed?: boolean
  sessionViewedUrls?: ReadonlySet<string>
  onlyNewConstruction?: boolean
  onlyDiscounted?: boolean
  minRooms?: number
  onlyBargain?: boolean
  excludeMultiSource?: boolean
  onlyMultiSource?: boolean
  excludeNoImage?: boolean
  minQualityScore?: number
  selectedPortalFilter?: string
  quickSearchQuery?: string
}

export function isListingNew(item: IngatlanItem | null | undefined): boolean {
  if (!item) return false
  return Boolean(item.isNew)
}

function isSessionViewed(item: IngatlanItem, sessionViewedUrls?: ReadonlySet<string>): boolean {
  if (!sessionViewedUrls || sessionViewedUrls.size === 0) return false
  if (item.url && sessionViewedUrls.has(item.url)) return true
  return Boolean(item.sourceListings?.some((sl) => sl.url && sessionViewedUrls.has(sl.url)))
}

function matchesSourceAndImageFlags(item: IngatlanItem, options: ItemFilterOptions): boolean {
  if (options.excludeMultiSource && getItemSourceListingsCount(item) !== 1) return false
  if (options.onlyMultiSource && getItemSourceListingsCount(item) <= 1) return false
  if (options.excludeNoImage && !hasListingImage(item)) return false
  return true
}

function matchesRoomsAndDiscountFlags(item: IngatlanItem, options: ItemFilterOptions): boolean {
  if (options.onlyDiscounted && !hasItemPriceDrop(item)) return false
  if (options.minRooms && options.minRooms > 0) {
    const rooms = item.detailsInfo?.roomCount ?? 0
    if (rooms < options.minRooms) return false
  }
  return true
}

function matchesItemFlags(item: IngatlanItem, options: ItemFilterOptions): boolean {
  const sessionViewed = isSessionViewed(item, options.sessionViewedUrls)
  if (options.onlyNew && !isListingNew(item) && !sessionViewed) return false
  if (options.excludeViewed && item.viewed && !sessionViewed) return false
  if (options.onlyNewConstruction && !item.newConstruction) return false
  if (options.onlyBargain && !isItemBargain(item)) return false
  if (!matchesRoomsAndDiscountFlags(item, options)) return false
  return matchesSourceAndImageFlags(item, options)
}

export function applyItemFilters(rawItems: IngatlanItem[], options: ItemFilterOptions = {}): IngatlanItem[] {
  if (!Array.isArray(rawItems) || rawItems.length === 0) {
    return []
  }
  let items = rawItems.filter((item) => matchesItemFlags(item, options))

  const minQuality = options.minQualityScore
  if (minQuality !== null && minQuality !== undefined && minQuality > 0) {
    items = items.filter(
      (item) =>
        item.qualityScore !== null &&
        item.qualityScore !== undefined &&
        item.qualityScore >= minQuality,
    )
  }
  const selectedPortal = options.selectedPortalFilter
  if (selectedPortal && selectedPortal !== 'all') {
    items = items.filter((item) => getPrimaryPortalKey(item) === selectedPortal)
  }

  const q = normalizeSearchText(options.quickSearchQuery).trim()
  if (q) {
    const tokens = q.split(/\s+/).filter(Boolean)
    items = items.filter((item) => matchesQuickSearch(item, tokens))
  }

  return items
}

export function markItemsAsViewed(items: IngatlanItem[], urlSet: Set<string>): IngatlanItem[] {
  return items.map((item) => {
    const directMatch = Boolean(item.url && urlSet.has(item.url))
    const sourceMatch = Boolean(item.sourceListings?.some((sl) => sl.url && urlSet.has(sl.url)))
    if (!directMatch && !sourceMatch) return item
    return {
      ...item,
      viewed: true,
      isNew: false,
      sourceListings: item.sourceListings?.map((sl) =>
        sl.url && urlSet.has(sl.url) ? { ...sl, viewed: true } : sl,
      ),
    }
  })
}

export function filterAndSortItems(
  rawItems: IngatlanItem[],
  options: ItemFilterOptions & { sortOption?: SortOption } = { sortOption: 'price_asc' },
): IngatlanItem[] {
  const items = applyItemFilters(rawItems, options)
  const sortOption = options.sortOption || 'price_asc'
  items.sort((a, b) => compareItems(a, b, sortOption))
  return items
}

export function filterItemsByCities(items: IngatlanItem[], cities?: string[] | null): IngatlanItem[] {
  if (!cities || cities.length === 0) return items
  const normalizedTargets = cities.map((c) => normalizeSearchText(c).trim()).filter(Boolean)
  if (normalizedTargets.length === 0) return items
  return items.filter((item) => {
    const cityText = normalizeSearchText(item.city)
    const locationText = normalizeSearchText(item.location)
    return normalizedTargets.some(
      (target) => cityText === target || cityText.includes(target) || locationText.includes(target),
    )
  })
}
