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

export function normalizePortalKey(portalOrId?: string | null): string {
  const s = (portalOrId || '').toLowerCase()
  if (s.includes('dh') || s.includes('duna')) return 'dh'
  if (s.includes('oc') || s.includes('otthon')) return 'oc'
  if (s.includes('jofogas') || s.includes('jófogás')) return 'jofogas'
  if (s.includes('ingatlanbazar') || s.includes('ingatlanbazár') || s.includes('bazar')) return 'ingatlanbazar'
  if (s.includes('koltozzbe') || s.includes('költözzbe')) return 'koltozzbe'
  if (s.includes('ingatlancom') || s.includes('ingatlan.com')) return 'ingatlancom'
  return s.replace(/[^a-z0-9]/g, '')
}

export function normalizeSearchText(str?: string | null): string {
  return (str || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
}

export function filterAndSortItems(
  rawItems: IngatlanItem[],
  options: {
    onlyNew?: boolean
    excludeViewed?: boolean
    onlyNewConstruction?: boolean
    selectedPortalFilter?: string
    quickSearchQuery?: string
    sortOption: SortOption
  },
): IngatlanItem[] {
  let items = [...rawItems]

  // 1. Filter only new
  if (options.onlyNew) {
    items = items.filter((item) => item.isNew)
  }

  // 2. Filter viewed if excludeViewed is true
  if (options.excludeViewed) {
    items = items.filter((item) => !item.viewed)
  }

  // 3. Filter new construction (#202)
  if (options.onlyNewConstruction) {
    items = items.filter((item) => Boolean(item.newConstruction))
  }

  // 4. Filter by portal chip (#157)
  if (options.selectedPortalFilter && options.selectedPortalFilter !== 'all') {
    items = items.filter(
      (item) =>
        normalizePortalKey(item.source?.portalId || item.portal) === options.selectedPortalFilter,
    )
  }

  // 5. Filter by multi-keyword quick search (#157)
  const q = normalizeSearchText(options.quickSearchQuery).trim()
  if (q) {
    const tokens = q.split(/\s+/).filter(Boolean)
    items = items.filter((item) => {
      const areaStr = item.detailsInfo?.areaSquareMeters
        ? `${Math.round(item.detailsInfo.areaSquareMeters)} m2 ${Math.round(item.detailsInfo.areaSquareMeters)} m²`
        : ''
      const roomsStr = item.detailsInfo?.roomCount ? `${item.detailsInfo.roomCount} szoba` : ''
      const priceStr = item.priceInfo?.amountMillion
        ? `${item.priceInfo.amountMillion} m ft`
        : ''
      const newBuildStr = item.newConstruction ? 'uj epitesu' : ''
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
          newBuildStr,
        ].join(' '),
      )
      return tokens.every((tok) => searchable.includes(tok))
    })
  }

  // 6. Sort
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
    quickSearchQuery,
    selectedPortalFilter,
  } = useFilterStore()
  const { dashboardTab, starredUrls, starredItemsCache } = useUiStore()
  const [data, setData] = useState<DashboardResponseDto | null>(null)

  // Archive / History query (#157)
  const historyQuery = useQuery({
    queryKey: ['history', criteria.onlyNewConstruction],
    queryFn: () => apiGetHistory(undefined, false, criteria.onlyNewConstruction),
    enabled: dashboardTab === 'archive' || dashboardTab === 'favorites',
    staleTime: 30_000,
  })

  useEffect(() => {
    if (dashboardTab === 'archive') {
      historyQuery.refetch()
    }
  }, [dashboardTab]) // eslint-disable-line react-hooks/exhaustive-deps

  const searchMutation = useMutation({
    mutationFn: (searchParams: SearchCriteria) => executeSearch(searchParams),
    onSuccess: (result) => {
      setData(result)
      queryClient.invalidateQueries({ queryKey: ['portals'] })
      queryClient.invalidateQueries({ queryKey: ['cities'] })
      queryClient.invalidateQueries({ queryKey: ['history'] })

      // Zero-Server-Desktop-Call kliensoldali lapmegnyitás ha autoOpenBrowser aktív
      if (criteria.autoOpenBrowser && result.items && result.items.length > 0) {
        const newUrls = result.items
          .filter((item) => item.isNew)
          .map((item) => item.url)
          .filter(Boolean)

        if (newUrls.length > 0) {
          openClientTabs(newUrls.slice(0, 10)) // Biztonsági limit max 10 tab
        }
      }
    },
  })

  // Mark single item as viewed
  const markViewedMutation = useMutation({
    mutationFn: (url: string) => apiMarkAsViewed(url),
    onSuccess: (_, url) => {
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          items: prev.items.map((item) => (item.url === url ? { ...item, viewed: true } : item)),
        }
      })
      queryClient.invalidateQueries({ queryKey: ['history'] })
    },
  })

  // Bulk open and mark viewed
  const openUrlsMutation = useMutation({
    mutationFn: (urls: string[]) => apiOpenUrls(urls),
    onSuccess: (_, urls) => {
      const urlSet = new Set(urls)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          items: prev.items.map((item) => (urlSet.has(item.url) ? { ...item, viewed: true } : item)),
        }
      })
      queryClient.invalidateQueries({ queryKey: ['history'] })
    },
  })

  // Clear history mutation (#157)
  const clearHistoryMutation = useMutation({
    mutationFn: (city?: string) => apiClearHistory(city),
    onSuccess: () => {
      queryClient.setQueryData(['history', criteria.onlyNewConstruction], [])
      queryClient.invalidateQueries({ queryKey: ['history'] })
      queryClient.invalidateQueries({ queryKey: ['portals'] })
      queryClient.invalidateQueries({ queryKey: ['cities'] })
    },
  })

  // Open tabs in browser client-side
  const openClientTabs = (urls: string[]) => {
    urls.forEach((url) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    })
    openUrlsMutation.mutate(urls)
  }

  // Sorted and filtered live search listings
  const processedItems = useMemo(() => {
    if (!data?.items) return []
    return filterAndSortItems(data.items, {
      onlyNew,
      excludeViewed: criteria.excludeViewed,
      onlyNewConstruction: criteria.onlyNewConstruction,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    })
  }, [
    data?.items,
    onlyNew,
    criteria.excludeViewed,
    criteria.onlyNewConstruction,
    selectedPortalFilter,
    quickSearchQuery,
    sortOption,
  ])

  // Sorted and filtered archive listings (#157)
  const archiveItems = useMemo(() => {
    const rawArchive = historyQuery.data ?? []
    return filterAndSortItems(rawArchive, {
      onlyNew: false,
      excludeViewed: criteria.excludeViewed,
      onlyNewConstruction: criteria.onlyNewConstruction,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    })
  }, [
    historyQuery.data,
    criteria.excludeViewed,
    criteria.onlyNewConstruction,
    selectedPortalFilter,
    quickSearchQuery,
    sortOption,
  ])

  // Starred / Favorite items (#157)
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

    return starredUrls
      .map((url) => byUrl.get(url))
      .filter((item): item is IngatlanItem => Boolean(item))
  }, [starredUrls, starredItemsCache, historyQuery.data, data?.items])

  const favoriteItems = useMemo(() => {
    return filterAndSortItems(rawFavoriteItems, {
      onlyNew: false,
      excludeViewed: criteria.excludeViewed,
      onlyNewConstruction: criteria.onlyNewConstruction,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    })
  }, [
    rawFavoriteItems,
    criteria.excludeViewed,
    criteria.onlyNewConstruction,
    selectedPortalFilter,
    quickSearchQuery,
    sortOption,
  ])

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
    favoritesCount: starredUrls.length,
    isLoading: searchMutation.isPending,
    isError: searchMutation.isError,
    error: searchMutation.error,
    search: (overrideCriteria?: Partial<SearchCriteria>) => {
      const finalCriteria = { ...criteria, ...(overrideCriteria || {}) }
      searchMutation.mutate(finalCriteria)
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
