import { useState, useMemo, useEffect, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { executeSearch } from '../api/searchApi'
import {
  markAsViewed as apiMarkAsViewed,
  openUrls as apiOpenUrls,
  getHistory as apiGetHistory,
  clearHistory as apiClearHistory,
  hideProperty as apiHideProperty,
  restoreProperty as apiRestoreProperty,
} from '../api/propertyApi'
import type { DashboardResponseDto, SearchCriteria } from '../types/api'
import type { IngatlanItem } from '../types/property'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { detectPortalFromUrl, normalizePortalKey, openExternalUrl } from '../utils/formatters'
import {
  normalizeSearchText,
  isItemBargain,
  isListingNew,
  filterAndSortItems,
  markItemsAsViewed,
} from '../utils/listingFilters'

export { normalizePortalKey, normalizeSearchText, isItemBargain, filterAndSortItems }

interface SearchMutationVariables {
  criteria: SearchCriteria
  requestId: number
}

interface RawFavoriteItemsParams {
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

function buildRawFavoriteItems({
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

export function useSearch() {
  const queryClient = useQueryClient()
  const {
    criteria,
    sortOption,
    onlyNew,
    onlyBargain,
    excludeMultiSource,
    onlyMultiSource,
    minQualityScore,
    quickSearchQuery,
    selectedPortalFilter,
    showHidden,
  } = useFilterStore()
  const { dashboardTab, listingSubView, starredUrls, starredItemsCache } = useUiStore()
  const [data, setData] = useState<DashboardResponseDto | null>(null)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [toastUndoItem, setToastUndoItem] = useState<IngatlanItem | null>(null)
  const [searchError, setSearchError] = useState<Error | null>(null)
  const [isSearchPending, setIsSearchPending] = useState(false)
  const [sessionViewedUrls, setSessionViewedUrls] = useState<Set<string>>(() => new Set())
  const searchRequestIdRef = useRef(0)

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

  const searchMutation = useMutation<DashboardResponseDto, Error, SearchMutationVariables>({
    mutationFn: ({ criteria: searchParams }) => executeSearch(searchParams),
    onMutate: ({ requestId }) => {
      setSearchError(null)
      if (requestId === searchRequestIdRef.current) {
        setData(null)
      }
    },
    onError: (err, { requestId }) => {
      if (requestId !== searchRequestIdRef.current) {
        return
      }
      setIsSearchPending(false)
      setSearchError(err)
      setData(null)
    },
    onSuccess: (result, { requestId }) => {
      if (requestId !== searchRequestIdRef.current) {
        return
      }
      setIsSearchPending(false)
      setData(result)
      invalidateDashboardQueries()

      if (criteria.autoOpenBrowser && result.items && result.items.length > 0) {
        const newUrls = result.items
          .filter(isListingNew)
          .map((item) => item.url)
          .filter(Boolean)

        if (newUrls.length > 0) {
          openClientTabs(newUrls.slice(0, 10))
        }
      }
    },
  })

  const markViewedInState = (urlSet: Set<string>) => {
    setSessionViewedUrls((prev) => {
      const next = new Set(prev)
      urlSet.forEach((u) => {
        if (u) next.add(u)
      })
      return next
    })
    setData((prev) => (prev ? { ...prev, items: markItemsAsViewed(prev.items, urlSet) } : prev))
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

  const filterResetKey = `${criteria.excludeViewed}|${criteria.onlyNewConstruction}|${onlyNew}|${onlyBargain}|${excludeMultiSource}|${onlyMultiSource}|${minQualityScore}|${selectedPortalFilter}|${quickSearchQuery}|${sortOption}|${showHidden}|${listingSubView}`
  const [prevFilterResetKey, setPrevFilterResetKey] = useState(filterResetKey)
  if (prevFilterResetKey !== filterResetKey) {
    setPrevFilterResetKey(filterResetKey)
    if (sessionViewedUrls.size > 0) {
      setSessionViewedUrls(new Set())
    }
  }

  const sharedFilterOptions = useMemo(
    () => ({
      excludeViewed: criteria.excludeViewed,
      sessionViewedUrls,
      onlyNewConstruction: criteria.onlyNewConstruction,
      onlyBargain,
      excludeMultiSource,
      onlyMultiSource,
      minQualityScore,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    }),
    [
      criteria.excludeViewed,
      sessionViewedUrls,
      criteria.onlyNewConstruction,
      onlyBargain,
      excludeMultiSource,
      onlyMultiSource,
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

  const hiddenSearchItems = useMemo(
    () => (data?.items ? data.items.filter((it) => Boolean(it.hidden)) : []),
    [data?.items],
  )

  const hiddenItems = useMemo(
    () => filterAndSortItems(hiddenSearchItems, { ...sharedFilterOptions, onlyNew: false }),
    [hiddenSearchItems, sharedFilterOptions],
  )

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

  const rawFavoriteItems = useMemo(
    () =>
      buildRawFavoriteItems({
        starredUrls,
        starredItemsCache,
        historyItems: historyQuery.data,
        currentItems: data?.items,
      }),
    [starredUrls, starredItemsCache, historyQuery.data, data?.items],
  )

  const favoriteItems = useMemo(
    () => filterAndSortItems(rawFavoriteItems, { ...sharedFilterOptions, onlyNew: false }),
    [rawFavoriteItems, sharedFilterOptions],
  )

  return {
    data,
    items: processedItems,
    rawItems: showHidden ? hiddenSearchItems : activeSearchItems,
    rawCount: showHidden ? hiddenSearchItems.length : activeSearchItems.length,
    filteredCount: processedItems.length,
    archiveItems,
    rawArchiveItems: historyQuery.data ?? [],
    isArchiveLoading: historyQuery.isLoading,
    favoriteItems,
    rawFavoriteItems,
    favoritesCount: rawFavoriteItems.length,
    hiddenItems,
    hiddenCount: hiddenSearchItems.length,
    toastMessage,
    toastUndoItem,
    clearToast: () => {
      setToastMessage(null)
      setToastUndoItem(null)
    },
    isLoading: isSearchPending,
    isError: searchError !== null,
    error: searchError,
    search: (overrideCriteria?: Partial<SearchCriteria>) => {
      const nextRequestId = searchRequestIdRef.current + 1
      searchRequestIdRef.current = nextRequestId
      setSessionViewedUrls(new Set())
      setSearchError(null)
      setIsSearchPending(true)
      searchMutation.mutate({
        criteria: { ...criteria, ...(overrideCriteria || {}) },
        requestId: nextRequestId,
      })
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
