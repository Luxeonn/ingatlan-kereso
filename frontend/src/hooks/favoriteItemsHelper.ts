import type { IngatlanItem } from '../types/property'
import { detectPortalFromUrl } from '../utils/formatters'

export interface RawFavoriteItemsParams {
  starredUrls: string[]
  starredItemsCache: Record<string, IngatlanItem | undefined>
  historyItems?: IngatlanItem[]
  currentItems?: IngatlanItem[]
}

function buildOrphanItem(url: string): IngatlanItem {
  const portalInfo = detectPortalFromUrl(url)
  return {
    id: `orphan-${url}`,
    portal: portalInfo.portalName,
    url,
    title: `Elmentett hirdetés (${portalInfo.portalName})`,
    price: 'N/A',
    location: 'Elmentett hivatkozás',
    city: 'Ismeretlen',
    propertyType: 'EGYEB_ISMERETLEN',
    sourcesCount: 1,
    source: {
      portalId: portalInfo.portalId,
      portalName: portalInfo.portalName,
      directListing: true,
    },
  }
}

export function buildRawFavoriteItems({
  starredUrls,
  starredItemsCache,
  historyItems,
  currentItems,
}: RawFavoriteItemsParams): IngatlanItem[] {
  const byUrl = new Map<string, IngatlanItem>()
  Object.entries(starredItemsCache).forEach(([u, item]) => {
    if (item) byUrl.set(u, item)
  })
  ;(historyItems ?? []).forEach((item) => {
    if (item.url) byUrl.set(item.url, item)
  })
  ;(currentItems ?? []).forEach((item) => {
    if (item.url) byUrl.set(item.url, item)
  })

  return starredUrls.map((url) => byUrl.get(url) ?? buildOrphanItem(url))
}
