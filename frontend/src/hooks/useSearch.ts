import { useState, useMemo, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { executeSearch } from '../api/searchApi'
import {
  markAsViewed as apiMarkAsViewed,
  openUrls as apiOpenUrls,
  getHistory as apiGetHistory,
  clearHistory as apiClearHistory,
} from '../api/propertyApi'
import { DashboardResponseDto, SearchCriteria } from '../types/api'
import { IngatlanItem } from '../types/property'
import { useFilterStore, SortOption } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { detectPortalFromUrl, normalizePortalKey, openExternalUrl } from '../utils/formatters'

export { normalizePortalKey }

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
      (item.percentageBelowMedian != null && item.percentageBelowMedian >= 20) ||
      (item.bargainInfo?.percentageBelowMedian != null && item.bargainInfo.percentageBelowMedian >= 20),
  )
}

export function filterAndSortItems(
  rawItems: IngatlanItem[],
  options: {
    onlyNew?: boolean
    excludeViewed?: boolean
    onlyNewConstruction?: boolean
    onlyBargain?: boolean
    minQualityScore?: number
    selectedPortalFilter?: string
    quickSearchQuery?: string
    sortOption: SortOption
  },
): IngatlanItem[] {
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
  if (options.minQualityScore != null && options.minQualityScore > 0) {
    items = items.filter((item) => item.qualityScore != null && item.qualityScore >= options.minQualityScore!)
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
    items = items.filter((item) => {
      const area = item.detailsInfo?.areaSquareMeters
      const areaStr = area ? `${Math.round(area)} m2 ${Math.round(area)} m²` : ''
      const roomsStr = item.detailsInfo?.roomCount ? `${item.detailsInfo.roomCount} szoba` : ''
      const priceStr = item.priceInfo?.amountMillion ? `${item.priceInfo.amountMillion} m ft` : ''
      const searchable = normalizeSearchText(
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
      return tokens.every((tok) => searchable.includes(tok))
    })
  }

  items.sort((a, b) => {
    switch (options.sortOption) {
      case 'price_asc':
        return (a.priceInfo?.amountHuf ?? Infinity) - (b.priceInfo?.amountHuf ?? Infinity)
      case 'price_desc':
        return (b.priceInfo?.amountHuf ?? -Infinity) - (a.priceInfo?.amountHuf ?? -Infinity)
      case 'sqm_price_asc':
        return (
          (a.detailsInfo?.pricePerSquareMeter ?? Infinity) -
          (b.detailsInfo?.pricePerSquareMeter ?? Infinity)
        )
      case 'sqm_price_desc':
        return (
          (b.detailsInfo?.pricePerSquareMeter ?? -Infinity) -
          (a.detailsInfo?.pricePerSquareMeter ?? -Infinity)
        )
      case 'area_desc':
        return (b.detailsInfo?.areaSquareMeters ?? 0) - (a.detailsInfo?.areaSquareMeters ?? 0)
      case 'date_desc':
        return (b.discoveredAt || '').localeCompare(a.discoveredAt || '')
      default:
        return 0
    }
  })

  return items
}

export function useSearch() {
  const queryClient = useQueryClient()
  const {
    criteria,
    sortOption,
    onlyNew,
    onlyBargain,
    minQualityScore,
    quickSearchQuery,
    selectedPortalFilter,
  } = useFilterStore()
  const { dashboardTab, listingSubView, starredUrls, starredItemsCache } = useUiStore()
  const [data, setData] = useState<DashboardResponseDto | null>(null)

  const historyQuery = useQuery({
    queryKey: ['history', criteria.onlyNewConstruction],
    queryFn: () => apiGetHistory(undefined, false, criteria.onlyNewConstruction),
    enabled: listingSubView === 'archive' || listingSubView === 'favorites',
    staleTime: 30_000,
  })

  useEffect(() => {
    if (dashboardTab === 'listings' && listingSubView === 'archive') {
      historyQuery.refetch()
    }
  }, [dashboardTab, listingSubView]) // eslint-disable-line react-hooks/exhaustive-deps

  const invalidateDashboardQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['portals'] })
    queryClient.invalidateQueries({ queryKey: ['cities'] })
    queryClient.invalidateQueries({ queryKey: ['history'] })
  }

  const searchMutation = useMutation({
    mutationFn: (searchParams: SearchCriteria) => executeSearch(searchParams),
    onSuccess: (result) => {
      setData(result)
      invalidateDashboardQueries()

      if (criteria.autoOpenBrowser && result.items && result.items.length > 0) {
        const newUrls = result.items
          .filter((item) => item.isNew)
          .map((item) => item.url)
          .filter(Boolean)

        if (newUrls.length > 0) {
          openClientTabs(newUrls.slice(0, 10))
        }
      }
    },
  })

  const markViewedInState = (urlSet: Set<string>) => {
    setData((prev) =>
      prev
        ? {
            ...prev,
            items: prev.items.map((item) => (urlSet.has(item.url) ? { ...item, viewed: true } : item)),
          }
        : prev,
    )
    queryClient.invalidateQueries({ queryKey: ['history'] })
  }

  const markViewedMutation = useMutation({
    mutationFn: (url: string) => apiMarkAsViewed(url),
    onSuccess: (_, url) => markViewedInState(new Set([url])),
  })

  const openUrlsMutation = useMutation({
    mutationFn: (urls: string[]) => apiOpenUrls(urls),
    onSuccess: (_, urls) => markViewedInState(new Set(urls)),
  })

  const clearHistoryMutation = useMutation({
    mutationFn: (city?: string) => apiClearHistory(city),
    onSuccess: () => {
      queryClient.setQueryData(['history', criteria.onlyNewConstruction], [])
      invalidateDashboardQueries()
    },
  })

  const openClientTabs = (urls: string[]) => {
    urls.forEach(openExternalUrl)
    openUrlsMutation.mutate(urls)
  }

  const sharedFilterOptions = useMemo(
    () => ({
      excludeViewed: criteria.excludeViewed,
      onlyNewConstruction: criteria.onlyNewConstruction,
      onlyBargain,
      minQualityScore,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    }),
    [
      criteria.excludeViewed,
      criteria.onlyNewConstruction,
      onlyBargain,
      minQualityScore,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    ],
  )

  const processedItems = useMemo(
    () => (data?.items ? filterAndSortItems(data.items, { ...sharedFilterOptions, onlyNew }) : []),
    [data?.items, sharedFilterOptions, onlyNew],
  )

  const archiveItems = useMemo(
    () => filterAndSortItems(historyQuery.data ?? [], { ...sharedFilterOptions, onlyNew: false }),
    [historyQuery.data, sharedFilterOptions],
  )

  const rawFavoriteItems = useMemo(() => {
    const byUrl = new Map<string, IngatlanItem>()
    Object.entries(starredItemsCache).forEach(([u, item]) => {
      if (item) byUrl.set(u, item)
    })
    ;(historyQuery.data ?? []).forEach((item) => {
      if (item.url) byUrl.set(item.url, item)
    })
    ;(data?.items ?? []).forEach((item) => {
      if (item.url) byUrl.set(item.url, item)
    })

    return starredUrls.map((url) => {
      const existing = byUrl.get(url)
      if (existing) return existing

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
        source: {
          portalId: portalInfo.portalId,
          portalName: portalInfo.portalName,
          directListing: true,
        },
      } satisfies IngatlanItem
    })
  }, [starredUrls, starredItemsCache, historyQuery.data, data?.items])

  const favoriteItems = useMemo(
    () => filterAndSortItems(rawFavoriteItems, { ...sharedFilterOptions, onlyNew: false }),
    [rawFavoriteItems, sharedFilterOptions],
  )

  return {
    data,
    items: processedItems,
    rawItems: data?.items ?? [],
    rawCount: data?.items?.length ?? 0,
    filteredCount: processedItems.length,
    archiveItems,
    rawArchiveItems: historyQuery.data ?? [],
    isArchiveLoading: historyQuery.isLoading,
    favoriteItems,
    rawFavoriteItems,
    favoritesCount: rawFavoriteItems.length,
    isLoading: searchMutation.isPending,
    isError: searchMutation.isError,
    error: searchMutation.error,
    search: (overrideCriteria?: Partial<SearchCriteria>) => {
      searchMutation.mutate({ ...criteria, ...(overrideCriteria || {}) })
    },
    markAsViewed: (url: string) => markViewedMutation.mutate(url),
    clearArchiveHistory: (city?: string) => clearHistoryMutation.mutateAsync(city),
    isClearingHistory: clearHistoryMutation.isPending,
    openAllTabs: (itemsToOpen: IngatlanItem[]) => {
      const urls = itemsToOpen.map((item) => item.url).filter(Boolean)
      if (urls.length > 0) {
        openClientTabs(urls)
      }
    },
  }
}
