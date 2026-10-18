import { useState, useMemo } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { executeSearch } from '../api/searchApi'
import { markAsViewed as apiMarkAsViewed, openUrls as apiOpenUrls } from '../api/propertyApi'
import { DashboardResponseDto, SearchCriteria } from '../types/api'
import { IngatlanItem } from '../types/property'
import { useFilterStore, SortOption } from '../store/useFilterStore'

export function useSearch() {
  const queryClient = useQueryClient()
  const { criteria, sortOption, onlyNew } = useFilterStore()
  const [data, setData] = useState<DashboardResponseDto | null>(null)

  const searchMutation = useMutation({
    mutationFn: (searchParams: SearchCriteria) => executeSearch(searchParams),
    onSuccess: (result) => {
      setData(result)
      queryClient.invalidateQueries({ queryKey: ['portals'] })
      queryClient.invalidateQueries({ queryKey: ['cities'] })

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
    },
  })

  // Open tabs in browser client-side
  const openClientTabs = (urls: string[]) => {
    urls.forEach((url) => {
      window.open(url, '_blank', 'noopener,noreferrer')
    })
    openUrlsMutation.mutate(urls)
  }

  // Sorted and filtered listings
  const processedItems = useMemo(() => {
    if (!data?.items) return []

    let items = [...data.items]

    // 1. Filter only new
    if (onlyNew) {
      items = items.filter((item) => item.isNew)
    }

    // 2. Filter viewed if criteria.excludeViewed is true
    if (criteria.excludeViewed) {
      items = items.filter((item) => !item.viewed)
    }

    // 3. Sort
    items.sort((a, b) => {
      switch (sortOption as SortOption) {
        case 'price_asc':
          return (a.priceInfo?.amountFt ?? Infinity) - (b.priceInfo?.amountFt ?? Infinity)
        case 'price_desc':
          return (b.priceInfo?.amountFt ?? -Infinity) - (a.priceInfo?.amountFt ?? -Infinity)
        case 'sqm_price_asc':
          return (a.detailsInfo?.pricePerSqmFt ?? Infinity) - (b.detailsInfo?.pricePerSqmFt ?? Infinity)
        case 'sqm_price_desc':
          return (b.detailsInfo?.pricePerSqmFt ?? -Infinity) - (a.detailsInfo?.pricePerSqmFt ?? -Infinity)
        case 'area_desc':
          return (b.detailsInfo?.floorAreaSqm ?? 0) - (a.detailsInfo?.floorAreaSqm ?? 0)
        case 'date_desc':
          return (b.discoveredAt || '').localeCompare(a.discoveredAt || '')
        default:
          return 0
      }
    })

    return items
  }, [data?.items, onlyNew, criteria.excludeViewed, sortOption])

  return {
    data,
    items: processedItems,
    rawCount: data?.items?.length ?? 0,
    filteredCount: processedItems.length,
    isLoading: searchMutation.isPending,
    isError: searchMutation.isError,
    error: searchMutation.error,
    search: (overrideCriteria?: Partial<SearchCriteria>) => {
      const finalCriteria = { ...criteria, ...(overrideCriteria || {}) }
      searchMutation.mutate(finalCriteria)
    },
    markAsViewed: (url: string) => markViewedMutation.mutate(url),
    openAllTabs: (itemsToOpen: IngatlanItem[]) => {
      const urls = itemsToOpen.map((item) => item.url).filter(Boolean)
      if (urls.length > 0) {
        openClientTabs(urls)
      }
    },
  }
}
