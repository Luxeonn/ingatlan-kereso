import { useMemo } from 'react'
import { useQueries } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import {
  getPriceHistory,
  getListingObservations,
  getStatusLog,
} from '../api/propertyApi'
import type {
  IngatlanItem,
  PriceHistoryEntry,
  ListingObservation,
  StatusLogEntry,
} from '../types/property'

export interface PropertyTimelineResult {
  priceHistory: PriceHistoryEntry[]
  observations: ListingObservation[]
  statusLog: StatusLogEntry[]
  unchangedCount: number
  isLoading: boolean
  isError: boolean
  error: Error | null
  refetchAll: () => void
}

function shouldRetryTimelineQuery(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false
  }
  return failureCount < 1
}

function resolveTimelineTarget(
  selectedItem: IngatlanItem | null,
  isOpen: boolean,
  overrideSourceUrl?: string,
) {
  const targetUrl = (overrideSourceUrl ?? selectedItem?.url ?? '').trim()
  const targetId = (selectedItem?.id ?? '').trim()
  const targetKey = targetUrl || targetId
  const isEnabled = Boolean(isOpen && selectedItem && targetKey.length > 0 && targetKey.length <= 1000)
  const lookup = targetUrl ? { url: targetUrl } : { id: targetId }
  return { targetKey, isEnabled, lookup }
}

function pickFirstError(...errors: Array<unknown>): Error | null {
  for (const err of errors) {
    if (err instanceof Error) return err
  }
  return null
}

export function usePropertyTimeline(
  selectedItem: IngatlanItem | null,
  isOpen: boolean,
  overrideSourceUrl?: string,
): PropertyTimelineResult {
  const { targetKey, isEnabled, lookup } = useMemo(
    () => resolveTimelineTarget(selectedItem, isOpen, overrideSourceUrl),
    [selectedItem, isOpen, overrideSourceUrl],
  )

  const [priceHistoryQuery, observationsQuery, statusLogQuery] = useQueries({
    queries: [
      {
        queryKey: ['property-timeline', 'price-history', targetKey] as const,
        queryFn: () => getPriceHistory(lookup),
        enabled: isEnabled,
        staleTime: 60_000,
        gcTime: 300_000,
        retry: shouldRetryTimelineQuery,
      },
      {
        queryKey: ['property-timeline', 'observations', targetKey] as const,
        queryFn: () => getListingObservations(lookup),
        enabled: isEnabled,
        staleTime: 60_000,
        gcTime: 300_000,
        retry: shouldRetryTimelineQuery,
      },
      {
        queryKey: ['property-timeline', 'status-log', targetKey] as const,
        queryFn: () => getStatusLog(lookup),
        enabled: isEnabled,
        staleTime: 60_000,
        gcTime: 300_000,
        retry: shouldRetryTimelineQuery,
      },
    ],
  })

  const observations = useMemo(() => observationsQuery.data ?? [], [observationsQuery.data])
  const unchangedCount = useMemo(
    () => observations.filter((obs) => obs.statusEvent === 'UNCHANGED').length,
    [observations],
  )

  const anyLoading =
    priceHistoryQuery.isLoading || observationsQuery.isLoading || statusLogQuery.isLoading
  const anyError =
    priceHistoryQuery.isError || observationsQuery.isError || statusLogQuery.isError

  return {
    priceHistory: priceHistoryQuery.data ?? [],
    observations,
    statusLog: statusLogQuery.data ?? [],
    unchangedCount,
    isLoading: isEnabled && anyLoading,
    isError: anyError,
    error: pickFirstError(
      priceHistoryQuery.error,
      observationsQuery.error,
      statusLogQuery.error,
    ),
    refetchAll: () => {
      void priceHistoryQuery.refetch()
      void observationsQuery.refetch()
      void statusLogQuery.refetch()
    },
  }
}
