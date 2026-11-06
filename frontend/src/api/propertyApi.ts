import { apiClient } from './client'
import type { IngatlanItem } from '../types/property'

export async function markAsViewed(url: string): Promise<{ success: boolean; marked: boolean; url: string }> {
  return apiClient<{ success: boolean; marked: boolean; url: string }>('/api/items/viewed', {
    method: 'POST',
    body: JSON.stringify({ url }),
  })
}

export async function openUrls(urls: string[]): Promise<{ success: boolean; count: number }> {
  return apiClient<{ success: boolean; count: number }>('/api/open-urls', {
    method: 'POST',
    body: JSON.stringify(urls),
  })
}

export async function getHistory(
  city?: string,
  onlyDiscounted?: boolean,
  onlyNewConstruction?: boolean,
): Promise<IngatlanItem[]> {
  const params = new URLSearchParams()
  if (city) params.set('city', city)
  if (onlyDiscounted) params.set('onlyDiscounted', 'true')
  if (onlyNewConstruction) params.set('onlyNewConstruction', 'true')
  const query = params.toString() ? `?${params.toString()}` : ''
  return apiClient<IngatlanItem[]>(`/api/history${query}`)
}

export async function clearHistory(city?: string): Promise<{ success: boolean }> {
  return apiClient<{ success: boolean }>('/api/history/clear', {
    method: 'POST',
    body: JSON.stringify(city ? { city } : {}),
  })
}
export async function hideProperty(id: string, urls?: string[]): Promise<{ success: boolean; hidden: boolean; id: string }> {
  return apiClient<{ success: boolean; hidden: boolean; id: string }>(`/api/properties/${encodeURIComponent(id)}/hide`, {
    method: 'POST',
    body: JSON.stringify(urls && urls.length > 0 ? { urls } : {}),
  })
}

export async function restoreProperty(id: string, urls?: string[]): Promise<{ success: boolean; hidden: boolean; id: string }> {
  return apiClient<{ success: boolean; hidden: boolean; id: string }>(`/api/properties/${encodeURIComponent(id)}/restore`, {
    method: 'POST',
    body: JSON.stringify(urls && urls.length > 0 ? { urls } : {}),
  })
}

export async function getHiddenProperties(): Promise<IngatlanItem[]> {
  return apiClient<IngatlanItem[]>('/api/properties/hidden')
}
