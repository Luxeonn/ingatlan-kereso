import { apiClient } from './client'
import type { SearchPreset, SearchPresetInput } from '../types/api'

export async function getPresets(): Promise<SearchPreset[]> {
  return apiClient<SearchPreset[]>('/api/presets')
}

export async function createPreset(preset: SearchPresetInput): Promise<SearchPreset> {
  return apiClient<SearchPreset>('/api/presets', {
    method: 'POST',
    body: JSON.stringify(preset),
  })
}

export async function updatePreset(id: number, preset: SearchPresetInput): Promise<SearchPreset> {
  return apiClient<SearchPreset>(`/api/presets/${id}`, {
    method: 'PUT',
    body: JSON.stringify(preset),
  })
}

export async function deletePreset(id: number): Promise<{ success: boolean }> {
  return apiClient<{ success: boolean }>(`/api/presets/${id}`, {
    method: 'DELETE',
  })
}

export async function recordPresetUsage(id: number): Promise<SearchPreset> {
  return apiClient<SearchPreset>(`/api/presets/${id}/use`, {
    method: 'POST',
  })
}
