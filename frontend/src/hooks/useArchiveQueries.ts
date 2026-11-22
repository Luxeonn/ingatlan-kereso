import { useMemo } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import {
  getHistory,
  getHiddenProperties,
  getDiscountedProperties,
} from '../api/propertyApi'
import type { HistoryQuery } from '../types/api'
import type { ListingSortOption } from '../types/property'

const VALID_ARCHIVE_SORTS: ReadonlySet<ListingSortOption> = new Set([
  'DISCOVERED_DESC',
  'PRICE_ASC',
  'PRICE_DESC',
  'AREA_ASC',
  'AREA_DESC',
  'ROOMS_ASC',
  'ROOMS_DESC',
  'BARGAIN_DESC',
])

export function sanitizeHistoryQuery(raw: HistoryQuery): HistoryQuery {
  const sanitized: HistoryQuery = { ...raw }

  if (
    sanitized.minPrice !== undefined &&
    sanitized.maxPrice !== undefined &&
    sanitized.minPrice > sanitized.maxPrice
  ) {
    sanitized.maxPrice = undefined
  }
  if (
    sanitized.minArea !== undefined &&
    sanitized.maxArea !== undefined &&
    sanitized.minArea > sanitized.maxArea
  ) {
    sanitized.maxArea = undefined
  }
  if (
    sanitized.minRooms !== undefined &&
    sanitized.maxRooms !== undefined &&
    sanitized.minRooms > sanitized.maxRooms
  ) {
    sanitized.maxRooms = undefined
  }

  const rawLimit = sanitized.limit ?? 50
  sanitized.limit = Math.max(1, Math.min(200, Math.floor(rawLimit)))
  sanitized.offset = Math.max(0, Math.floor(sanitized.offset ?? 0))

  if (sanitized.sortBy && !VALID_ARCHIVE_SORTS.has(sanitized.sortBy)) {
    sanitized.sortBy = 'DISCOVERED_DESC'
  }

  return sanitized
}

export function useArchiveHistory(rawQuery: HistoryQuery, enabled = true) {
  const query = useMemo(() => sanitizeHistoryQuery(rawQuery), [rawQuery])

  return useQuery({
    queryKey: ['history', 'archive', query] as const,
    queryFn: () => getHistory(query),
    enabled,
    staleTime: 30_000,
    gcTime: 300_000,
    placeholderData: keepPreviousData,
    retry: false,
  })
}

export function useHiddenProperties(enabled = true) {
  return useQuery({
    queryKey: ['properties', 'hidden'] as const,
    queryFn: () => getHiddenProperties(),
    enabled,
    staleTime: 15_000,
    gcTime: 180_000,
    retry: false,
  })
}

export function useDiscountedProperties(city?: string, enabled = true) {
  const normalizedCity = city?.trim() || undefined

  return useQuery({
    queryKey: ['properties', 'discounted', normalizedCity ?? 'ALL'] as const,
    queryFn: () => getDiscountedProperties(normalizedCity),
    enabled,
    staleTime: 60_000,
    gcTime: 300_000,
    retry: false,
  })
}
