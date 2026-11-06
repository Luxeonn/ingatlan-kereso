import type { SortOption } from '../store/useFilterStore'
import type { IngatlanItem } from '../types/property'
import { normalizePortalKey } from '../utils/formatters'

export function normalizeSearchText(str?: string | null): string {
  return (str || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

export function isItemBargain(item: IngatlanItem): boolean {
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

function getSqmPrice(item: IngatlanItem, fallback: number): number {
  if (item.detailsInfo && item.detailsInfo.pricePerSquareMeter !== undefined) {
    return item.detailsInfo.pricePerSquareMeter
  }
  return fallback
}

function getArea(item: IngatlanItem): number {
  if (item.detailsInfo && item.detailsInfo.areaSquareMeters !== undefined) {
    return item.detailsInfo.areaSquareMeters
  }
  return 0
}

const COMPARATORS: Record<SortOption, (a: IngatlanItem, b: IngatlanItem) => number> = {
  price_asc: (a, b) => getPriceHuf(a, Infinity) - getPriceHuf(b, Infinity),
  price_desc: (a, b) => getPriceHuf(b, -Infinity) - getPriceHuf(a, -Infinity),
  sqm_price_asc: (a, b) => getSqmPrice(a, Infinity) - getSqmPrice(b, Infinity),
  sqm_price_desc: (a, b) => getSqmPrice(b, -Infinity) - getSqmPrice(a, -Infinity),
  area_desc: (a, b) => getArea(b) - getArea(a),
  date_desc: (a, b) => (b.discoveredAt || '').localeCompare(a.discoveredAt || ''),
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
  minQualityScore?: number
  selectedPortalFilter?: string
  quickSearchQuery?: string
}

export function applyItemFilters(rawItems: IngatlanItem[], options: ItemFilterOptions): IngatlanItem[] {
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
  options: ItemFilterOptions & { sortOption: SortOption },
): IngatlanItem[] {
  const items = applyItemFilters(rawItems, options)
  items.sort((a, b) => compareItems(a, b, options.sortOption))
  return items
}
