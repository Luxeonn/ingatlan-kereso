import { useState, useMemo, useEffect, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { executeSearch } from '../api/searchApi'
import {
  markAsViewed as apiMarkAsViewed,
  getHistory as apiGetHistory,
  clearHistory as apiClearHistory,
  hideProperty as apiHideProperty,
  restoreProperty as apiRestoreProperty,
} from '../api/propertyApi'
import { useDedupActions } from './useDedupActions'
import { buildRawFavoriteItems } from './favoriteItemsHelper'
import type { DashboardResponseDto, SearchCriteria } from '../types/api'
import type { IngatlanItem } from '../types/property'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import { normalizePortalKey } from '../utils/formatters'
import {
  normalizeSearchText,
  isItemBargain,
  filterAndSortItems,
  markItemsAsViewed,
} from '../utils/listingFilters'

export { normalizePortalKey, normalizeSearchText, isItemBargain, filterAndSortItems }

interface SearchMutationVariables {
  criteria: SearchCriteria
  requestId: number
  silent?: boolean
}

export function useSearch() {
  const queryClient = useQueryClient()
  const {
    criteria,
    sortOption,
    onlyNew,
    excludeNew,
    onlyBargain,
    excludeBargain,
    onlyViewed,
    excludeNewConstruction,
    excludeMultiSource,
    onlyMultiSource,
    excludeNoImage,
    onlyNoImage,
    includeHidden,
    quickSearchQuery,
    selectedPortalFilter,
    showHidden,
    markCriteriaExecuted,
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
    queryClient.invalidateQueries({ queryKey: ['system-status'] })
    queryClient.invalidateQueries({ queryKey: ['properties'] })
  }

  const searchMutation = useMutation<DashboardResponseDto, Error, SearchMutationVariables>({
    mutationFn: ({ criteria: searchParams }) => executeSearch(searchParams),
    onMutate: ({ requestId, silent }) => {
      setSearchError(null)
      if (!silent && requestId === searchRequestIdRef.current) {
        setData(null)
      }
    },
    onError: (err, { requestId, silent }) => {
      if (requestId !== searchRequestIdRef.current) {
        return
      }
      setIsSearchPending(false)
      setSearchError(err)
      if (!silent) {
        setData(null)
      }
    },
    onSuccess: (result, { requestId }) => {
      if (requestId !== searchRequestIdRef.current) {
        return
      }
      setIsSearchPending(false)
      setData(result)
      invalidateDashboardQueries()
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

  const filterResetKey = `${criteria.excludeViewed}|${onlyViewed}|${criteria.onlyNewConstruction}|${excludeNewConstruction}|${onlyNew}|${excludeNew}|${onlyBargain}|${excludeBargain}|${excludeMultiSource}|${onlyMultiSource}|${excludeNoImage}|${onlyNoImage}|${includeHidden}|${selectedPortalFilter}|${quickSearchQuery}|${sortOption}|${showHidden}|${listingSubView}`
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
      onlyViewed,
      sessionViewedUrls,
      onlyNewConstruction: criteria.onlyNewConstruction,
      excludeNewConstruction,
      onlyBargain,
      excludeBargain,
      excludeMultiSource,
      onlyMultiSource,
      excludeNoImage,
      onlyNoImage,
      selectedPortalFilter,
      quickSearchQuery,
      sortOption,
    }),
    [
      criteria.excludeViewed,
      onlyViewed,
      sessionViewedUrls,
      criteria.onlyNewConstruction,
      excludeNewConstruction,
      onlyBargain,
      excludeBargain,
      excludeMultiSource,
      onlyMultiSource,
      excludeNoImage,
      onlyNoImage,
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

  const processedItems = useMemo(() => {
    if (showHidden) {
      return filterAndSortItems(hiddenSearchItems, { ...sharedFilterOptions, onlyNew: false })
    }
    const baseItems = includeHidden ? [...activeSearchItems, ...hiddenSearchItems] : activeSearchItems
    return filterAndSortItems(baseItems, { ...sharedFilterOptions, onlyNew, excludeNew })
  }, [showHidden, includeHidden, activeSearchItems, hiddenSearchItems, sharedFilterOptions, onlyNew, excludeNew])

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

  const triggerSearch = (overrideCriteria?: Partial<SearchCriteria>, silent = false) => {
    const nextRequestId = searchRequestIdRef.current + 1
    searchRequestIdRef.current = nextRequestId
    setSessionViewedUrls(new Set())
    setSearchError(null)
    if (!silent) {
      setIsSearchPending(true)
    }
    const effectiveCriteria = { ...criteria, ...(overrideCriteria || {}) }
    markCriteriaExecuted(effectiveCriteria)
    searchMutation.mutate({
      criteria: effectiveCriteria,
      requestId: nextRequestId,
      silent,
    })
  }

  const handleAfterMerge = () => {
    if (listingSubView === 'listings') {
      triggerSearch(undefined, true)
    } else {
      void historyQuery.refetch()
    }
  }

  const { splitSource, mergeSelected, undoDedupOverride, isMerging } = useDedupActions(
    setData,
    setToastMessage,
    handleAfterMerge,
  )

    return {
    data,
    sessionViewedUrls,
    items: processedItems,
    rawItems: showHidden ? hiddenSearchItems : activeSearchItems,
    rawCount: showHidden ? hiddenSearchItems.length : activeSearchItems.length,
    archiveItems,
    rawArchiveItems: historyQuery.data ?? [],
    isArchiveLoading: historyQuery.isLoading,
    favoriteItems,
    rawFavoriteItems,
    favoritesCount: rawFavoriteItems.length,
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
    search: (overrideCriteria?: Partial<SearchCriteria>) => triggerSearch(overrideCriteria, false),
    markAsViewed: (url: string) => markViewedMutation.mutate(url),
    hideProperty: (item: IngatlanItem) => hideMutation.mutate(item),
    restoreProperty: (item: IngatlanItem) => restoreMutation.mutate(item),
    clearArchiveHistory: (city?: string) => clearHistoryMutation.mutateAsync(city),
    isClearingHistory: clearHistoryMutation.isPending,
    splitSource,
    mergeSelected,
    undoDedupOverride,
    isMerging,
  }
}
