import type { SortOption } from '../store/useFilterStore'
import type { IngatlanItem } from '../types/property'
import { getItemSourceListingsCount, normalizePortalKey } from './formatters'

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
      item.bargainInfo?.isBargain ||
      (item.percentageBelowMedian !== null &&
        item.percentageBelowMedian !== undefined &&
        item.percentageBelowMedian >= 20) ||
      (item.bargainInfo?.percentageBelowMedian !== null &&
        item.bargainInfo?.percentageBelowMedian !== undefined &&
        item.bargainInfo.percentageBelowMedian >= 20),
  )
}

function getPriceHuf(item: IngatlanItem, fallback: number): number {
  if (item.priceInfo && item.priceInfo.amountHuf !== undefined) {
    return item.priceInfo.amountHuf
  }
  return fallback
}

export function getBargainPercentage(item: IngatlanItem): number {
  if (item.percentageBelowMedian !== null && item.percentageBelowMedian !== undefined) {
    return item.percentageBelowMedian
  }
  if (item.bargainInfo?.percentageBelowMedian !== null && item.bargainInfo?.percentageBelowMedian !== undefined) {
    return item.bargainInfo.percentageBelowMedian
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
  onlyNewConstruction?: boolean
  onlyBargain?: boolean
  excludeMultiSource?: boolean
  minQualityScore?: number
  selectedPortalFilter?: string
  quickSearchQuery?: string
}

export function applyItemFilters(rawItems: IngatlanItem[], options: ItemFilterOptions = {}): IngatlanItem[] {
  if (!Array.isArray(rawItems) || rawItems.length === 0) {
    return []
  }
  let items = [...rawItems]

  if (options.onlyNew) {
    items = items.filter((item) => item.isNew)
  }
  if (options.excludeViewed) {
    items = items.filter((item) => !item.viewed)
  }
  if (options.onlyNewConstruction) {
    items = items.filter((item) => Boolean(item.newConstruction))
  }
  if (options.onlyBargain) {
    items = items.filter(isItemBargain)
  }
  if (options.excludeMultiSource) {
    items = items.filter((item) => getItemSourceListingsCount(item) === 1)
  }
  if (options.minQualityScore !== null && options.minQualityScore !== undefined && options.minQualityScore > 0) {
    items = items.filter(
      (item) =>
        item.qualityScore !== null &&
        item.qualityScore !== undefined &&
        item.qualityScore >= options.minQualityScore!,
    )
  }
  if (options.selectedPortalFilter && options.selectedPortalFilter !== 'all') {
    items = items.filter(
      (item) =>
        normalizePortalKey(item.source?.portalId || item.portal) === options.selectedPortalFilter,
    )
  }

  const q = normalizeSearchText(options.quickSearchQuery).trim()
  if (q) {
    const tokens = q.split(/\s+/).filter(Boolean)
    items = items.filter((item) => matchesQuickSearch(item, tokens))
  }

  return items
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
