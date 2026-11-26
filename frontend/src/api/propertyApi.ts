import { fetchClient, unwrapApiResponse } from './client'
import type {
  IngatlanItem,
  PriceHistoryEntry,
  ListingObservation,
  StatusLogEntry,
} from '../types/property'
import type { HistoryQuery, StatusResponse } from '../types/api'
import type { components } from '../types/generated-api'

export type MarkViewedResponse = components['schemas']['MarkViewedResponse']
export type PropertyHideResponse = components['schemas']['PropertyHideResponse']
export type ClearHistoryResponse = components['schemas']['ClearHistoryResponse']

export interface PropertyLookupKey {
  id?: string
  url?: string
}

function normalizeLookupQuery(lookup: PropertyLookupKey): { id?: string; url?: string } | null {
  const url = lookup.url?.trim()
  const id = lookup.id?.trim()
  if (url && url.length <= 1000) return { url }
  if (id && id.length <= 1000) return { id }
  return null
}

export async function getSystemStatus(): Promise<StatusResponse> {
  return unwrapApiResponse(fetchClient.GET('/api/status'))
}

export async function getPriceHistory(lookup: PropertyLookupKey): Promise<PriceHistoryEntry[]> {
  const query = normalizeLookupQuery(lookup)
  if (!query) return []
  return unwrapApiResponse(
    fetchClient.GET('/api/properties/price-history', {
      params: { query },
    }),
  )
}

export async function getListingObservations(lookup: PropertyLookupKey): Promise<ListingObservation[]> {
  const query = normalizeLookupQuery(lookup)
  if (!query) return []
  return unwrapApiResponse(
    fetchClient.GET('/api/properties/observations', {
      params: { query },
    }),
  )
}

export async function getStatusLog(lookup: PropertyLookupKey): Promise<StatusLogEntry[]> {
  const query = normalizeLookupQuery(lookup)
  if (!query) return []
  return unwrapApiResponse(
    fetchClient.GET('/api/properties/status-log', {
      params: { query },
    }),
  )
}

export async function markAsViewed(url: string): Promise<MarkViewedResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/items/viewed', {
      body: { url },
    }),
  )
}

export async function getHistory(
  queryOrCity?: HistoryQuery | string,
  onlyDiscounted?: boolean,
  onlyNewConstruction?: boolean,
): Promise<IngatlanItem[]> {
  const query: HistoryQuery =
    typeof queryOrCity === 'string' || queryOrCity === undefined
      ? {
          city: queryOrCity,
          onlyDiscounted,
          onlyNewConstruction,
        }
      : queryOrCity

  return unwrapApiResponse(
    fetchClient.GET('/api/history', {
      params: {
        query,
      },
    }),
  )
}

export async function clearHistory(city?: string): Promise<ClearHistoryResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/history/clear', {
      params: {
        query: {
          city,
        },
      },
      body: city ? { city } : undefined,
    }),
  )
}

export async function hideProperty(id: string, urls?: string[]): Promise<PropertyHideResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/properties/{id}/hide', {
      params: {
        path: { id },
      },
      body: urls && urls.length > 0 ? { urls } : undefined,
    }),
  )
}

export async function restoreProperty(id: string, urls?: string[]): Promise<PropertyHideResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/properties/{id}/restore', {
      params: {
        path: { id },
      },
      body: urls && urls.length > 0 ? { urls } : undefined,
    }),
  )
}

export async function getHiddenProperties(): Promise<IngatlanItem[]> {
  return unwrapApiResponse(fetchClient.GET('/api/properties/hidden'))
}

export async function getDiscountedProperties(city?: string): Promise<IngatlanItem[]> {
  const trimmedCity = city?.trim()
  return unwrapApiResponse(
    fetchClient.GET('/api/properties/discounted', {
      params: {
        query: trimmedCity ? { city: trimmedCity } : undefined,
      },
    }),
  )
}
