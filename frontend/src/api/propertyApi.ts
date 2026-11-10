import { fetchClient, unwrapApiResponse } from './client'
import type { IngatlanItem } from '../types/property'
import type { components } from '../types/generated-api'

export type MarkViewedResponse = components['schemas']['MarkViewedResponse']
export type OpenUrlsResponse = components['schemas']['OpenUrlsResponse']
export type PropertyHideResponse = components['schemas']['PropertyHideResponse']
export type ClearHistoryResponse = components['schemas']['ClearHistoryResponse']

export async function markAsViewed(url: string): Promise<MarkViewedResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/items/viewed', {
      body: { url },
    }),
  )
}

export async function openUrls(urls: string[]): Promise<OpenUrlsResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/open-urls', {
      body: urls,
    }),
  )
}

export async function getHistory(
  city?: string,
  onlyDiscounted?: boolean,
  onlyNewConstruction?: boolean,
): Promise<IngatlanItem[]> {
  return unwrapApiResponse(
    fetchClient.GET('/api/history', {
      params: {
        query: {
          city,
          onlyDiscounted,
          onlyNewConstruction,
        },
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
