import type { IngatlanItem } from '../../types/property'
import { isItemBargain, isListingNew } from '../../utils/listingFilters'
import {
  PORTAL_CHIPS,
  getItemSourceListingsCount,
  getPrimaryPortalKey,
  hasListingImage,
} from '../../utils/formatters'

export interface QuickFilterCounts {
  bargain: number
  onlyNew: number
  excludeViewed: number
  onlyNewConstruction: number
  excludeMultiSource: number
  onlyMultiSource: number
  excludeNoImage: number
}

export function computeQuickFilterCounts(
  sourceItems: IngatlanItem[],
  sessionViewedUrls?: ReadonlySet<string>,
): QuickFilterCounts {
  const counts: QuickFilterCounts = {
    bargain: 0,
    onlyNew: 0,
    excludeViewed: 0,
    onlyNewConstruction: 0,
    excludeMultiSource: 0,
    onlyMultiSource: 0,
    excludeNoImage: 0,
  }
  for (const item of sourceItems) {
    if (isItemBargain(item)) counts.bargain += 1
    if (isListingNew(item, sessionViewedUrls)) counts.onlyNew += 1
    if (item.viewed) counts.excludeViewed += 1
    if (item.newConstruction) counts.onlyNewConstruction += 1
    const sourceCount = getItemSourceListingsCount(item)
    if (sourceCount === 1) counts.excludeMultiSource += 1
    if (sourceCount > 1) counts.onlyMultiSource += 1
    if (!hasListingImage(item)) counts.excludeNoImage += 1
  }
  return counts
}

export function computePortalCounts(sourceItems: IngatlanItem[]): Record<string, number> {
  const counts: Record<string, number> = { all: sourceItems.length }
  PORTAL_CHIPS.forEach((chip) => {
    if (chip.id !== 'all') counts[chip.id] = 0
  })
  sourceItems.forEach((item) => {
    const primaryKey = getPrimaryPortalKey(item)
    if (primaryKey) {
      counts[primaryKey] = (counts[primaryKey] || 0) + 1
    }
  })
  return counts
}

export function formatResultCount(
  itemsLength: number,
  totalRawCount: number,
  totalSourceListingsCount: number,
): string {
  const sourceSuffix =
    totalSourceListingsCount > itemsLength ? ` (${totalSourceListingsCount} hirdetés)` : ''
  if (itemsLength !== totalRawCount) {
    return `${itemsLength} / ${totalRawCount} db${sourceSuffix}`
  }
  if (totalSourceListingsCount > itemsLength) {
    return `${itemsLength} ingatlan (${totalSourceListingsCount} hirdetés)`
  }
  return `${itemsLength} db`
}
