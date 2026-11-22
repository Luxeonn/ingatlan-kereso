import { fetchClient, unwrapApiResponse } from './client'
import type { components } from '../types/generated-api'

export type DedupOverrideDto = components['schemas']['DedupOverrideDto']
export type DedupActionResponseDto = components['schemas']['DedupActionResponseDto']
export type DedupSplitRequestDto = components['schemas']['DedupSplitRequestDto']
export type DedupMergeRequestDto = components['schemas']['DedupMergeRequestDto']

export async function splitCluster(
  sourceUrl: string,
  clusterUrls: string[],
): Promise<DedupActionResponseDto> {
  return unwrapApiResponse(
    fetchClient.POST('/api/dedup/split', {
      body: { sourceUrl, clusterUrls },
    }),
  )
}

export async function mergeListings(urls: string[]): Promise<DedupActionResponseDto> {
  return unwrapApiResponse(
    fetchClient.POST('/api/dedup/merge', {
      body: { urls },
    }),
  )
}

export async function getDedupOverrides(
  urlOrUrls: string | string[],
): Promise<DedupOverrideDto[]> {
  if (Array.isArray(urlOrUrls)) {
    const cleaned = Array.from(new Set(urlOrUrls.map((u) => u.trim()).filter(Boolean)))
    if (cleaned.length === 0) {
      return []
    }
    return unwrapApiResponse(
      fetchClient.GET('/api/dedup/overrides', {
        params: {
          query: { urls: cleaned },
        },
      }),
    )
  }
  return unwrapApiResponse(
    fetchClient.GET('/api/dedup/overrides', {
      params: {
        query: { url: urlOrUrls },
      },
    }),
  )
}

export async function deleteDedupOverride(id: number): Promise<DedupActionResponseDto> {
  return unwrapApiResponse(
    fetchClient.DELETE('/api/dedup/overrides/{id}', {
      params: {
        path: { id },
      },
    }),
  )
}
