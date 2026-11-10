import { fetchClient, unwrapApiResponse } from './client'
import type { SearchPreset, SearchPresetInput } from '../types/api'
import type { components } from '../types/generated-api'
import { validatePresetInput } from '../utils/validation'

export async function getPresets(): Promise<SearchPreset[]> {
  return unwrapApiResponse(fetchClient.GET('/api/presets'))
}

export async function createPreset(preset: SearchPresetInput): Promise<SearchPreset> {
  const validPreset = validatePresetInput(preset)
  return unwrapApiResponse(
    fetchClient.POST('/api/presets', {
      body: validPreset as components['schemas']['SearchPresetInput'],
    }),
  )
}

export async function updatePreset(id: number, preset: SearchPresetInput): Promise<SearchPreset> {
  const validPreset = validatePresetInput(preset)
  return unwrapApiResponse(
    fetchClient.PUT('/api/presets/{id}', {
      params: { path: { id } },
      body: validPreset as components['schemas']['SearchPresetInput'],
    }),
  )
}

export async function deletePreset(id: number): Promise<{ success: boolean }> {
  return unwrapApiResponse(
    fetchClient.DELETE('/api/presets/{id}', {
      params: { path: { id } },
    }),
  )
}

export async function recordPresetUsage(id: number): Promise<SearchPreset> {
  return unwrapApiResponse(
    fetchClient.POST('/api/presets/{id}/use', {
      params: { path: { id } },
    }),
  )
}
