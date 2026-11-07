import { useState, useMemo, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { executeSearch } from '../api/searchApi'
import {
  markAsViewed as apiMarkAsViewed,
  openUrls as apiOpenUrls,
  getHistory as apiGetHistory,
  clearHistory as apiClearHistory,
  hideProperty as apiHideProperty,
  restoreProperty as apiRestoreProperty,
  getHiddenProperties as apiGetHiddenProperties,
} from '../api/propertyApi'
import type { DashboardResponseDto, SearchCriteria } from '../types/api'
import type { IngatlanItem } from '../types/property'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { detectPortalFromUrl, normalizePortalKey, openExternalUrl } from '../utils/formatters'
import {
  normalizeSearchText,
  isItemBargain,
  filterAndSortItems,
} from '../utils/listingFilters'

export { normalizePortalKey, normalizeSearchText, isItemBargain, filterAndSortItems }

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
    source: {
      portalId: portalInfo.portalId,
      portalName: portalInfo.portalName,
      directListing: true,
    },
  }
}

export function useSearch() {
  const queryClient = useQueryClient()
  const {
    criteria,
    sortOption,
    onlyNew,
    onlyBargain,
    excludeMultiSource,
    minQualityScore,
    quickSearchQuery,
    selectedPortalFilter,
    showHidden,
  } = useFilterStore()
  const { dashboardTab, listingSubView, starredUrls, starredItemsCache } = useUiStore()
  const [data, setData] = useState<DashboardResponseDto | null>(null)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [toastUndoItem, setToastUndoItem] = useState<IngatlanItem | null>(null)

  const historyQuery = useQuery({
    queryKey: ['history', criteria.onlyNewConstruction],
    queryFn: () => apiGetHistory(undefined, false, criteria.onlyNewConstruction),
    enabled: listingSubView === 'archive' || listingSubView === 'favorites',
    staleTime: 30_000,
  })

  const hiddenQuery = useQuery({
    queryKey: ['hidden-properties'],
    queryFn: () => apiGetHiddenProperties(),
    staleTime: 10_000,
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
    queryClient.invalidateQueries({ queryKey: ['hidden-properties'] })
  }

  const searchMutation = useMutation({
    mutationFn: (searchParams: SearchCriteria) => executeSearch(searchParams),
    onMutate: () => {
      setData(null)
    },
    onError: () => {
      setData(null)
    },
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

  const hideMutation = useMutation({
    mutationFn: (item: IngatlanItem) => {
      const urls = item.sourceListings?.map((s) => s.url).filter((u): u is string => Boolean(u)) ?? [item.url]
      return apiHideProperty(item.id, urls)
    },
    onSuccess: (_, item) => {
      setData((prev) =>
        prev
          ? {
              ...prev,
              items: prev.items.map((i) => (i.id === item.id || i.url === item.url ? { ...i, hidden: true } : i)),
            }
          : prev,
      )
      setToastUndoItem(item)
      setToastMessage('Hirdetés elrejtve')
      invalidateDashboardQueries()
    },
  })

  const restoreMutation = useMutation({
    mutationFn: (item: IngatlanItem) => {
      const urls = item.sourceListings?.map((s) => s.url).filter((u): u is string => Boolean(u)) ?? [item.url]
      return apiRestoreProperty(item.id, urls)
    },
    onSuccess: (_, item) => {
      setData((prev) =>
        prev
          ? {
              ...prev,
              items: prev.items.map((i) => (i.id === item.id || i.url === item.url ? { ...i, hidden: false } : i)),
            }
          : prev,
      )
      if (toastUndoItem?.id === item.id) {
        setToastMessage(null)
        setToastUndoItem(null)
      }
      invalidateDashboardQueries()
    },
  })

  const openClientTabs = (urls: string[]) => {
    urls.forEach((u) => openExternalUrl(u))
    openUrlsMutation.mutate(urls)
  }

  const sharedFilterOptions = useMemo(
    () => ({
      excludeViewed: criteria.excludeViewed,
      onlyNewConstruction: criteria.onlyNewConstruction,
      onlyBargain,
      excludeMultiSource,
      minQualityScore,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    }),
    [
      criteria.excludeViewed,
      criteria.onlyNewConstruction,
      onlyBargain,
      excludeMultiSource,
      minQualityScore,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    ],
  )

  const activeSearchItems = useMemo(
    () => (data?.items ? data.items.filter((it) => !it.hidden) : []),
    [data?.items],
  )

  const hiddenItems = useMemo(() => {
    const list = Array.isArray(hiddenQuery.data) ? hiddenQuery.data : []
    return filterAndSortItems(list, { ...sharedFilterOptions, onlyNew: false })
  }, [hiddenQuery.data, sharedFilterOptions])

  const processedItems = useMemo(() => {
    if (showHidden) {
      return hiddenItems
    }
    return filterAndSortItems(activeSearchItems, { ...sharedFilterOptions, onlyNew })
  }, [showHidden, hiddenItems, activeSearchItems, sharedFilterOptions, onlyNew])

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

    return starredUrls.map((url) => byUrl.get(url) ?? buildOrphanItem(url))
  }, [starredUrls, starredItemsCache, historyQuery.data, data?.items])

  const favoriteItems = useMemo(
    () => filterAndSortItems(rawFavoriteItems, { ...sharedFilterOptions, onlyNew: false }),
    [rawFavoriteItems, sharedFilterOptions],
  )

  return {
    data,
    items: processedItems,
    rawItems: showHidden ? (hiddenQuery.data ?? []) : activeSearchItems,
    rawCount: showHidden ? (hiddenQuery.data?.length ?? 0) : activeSearchItems.length,
    filteredCount: processedItems.length,
    archiveItems,
    rawArchiveItems: historyQuery.data ?? [],
    isArchiveLoading: historyQuery.isLoading,
    favoriteItems,
    rawFavoriteItems,
    favoritesCount: rawFavoriteItems.length,
    hiddenItems,
    hiddenCount: hiddenQuery.data?.length ?? 0,
    toastMessage,
    toastUndoItem,
    clearToast: () => {
      setToastMessage(null)
      setToastUndoItem(null)
    },
    isLoading: searchMutation.isPending,
    isError: searchMutation.isError,
    error: searchMutation.error,
    search: (overrideCriteria?: Partial<SearchCriteria>) => {
      searchMutation.mutate({ ...criteria, ...(overrideCriteria || {}) })
    },
    markAsViewed: (url: string) => markViewedMutation.mutate(url),
    hideProperty: (item: IngatlanItem) => hideMutation.mutate(item),
    restoreProperty: (item: IngatlanItem) => restoreMutation.mutate(item),
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
