import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getPresets,
  createPreset,
  updatePreset,
  deletePreset,
  recordPresetUsage,
} from '../api/presetApi'
import type { SearchPresetInput } from '../types/api'

export function usePresets() {
  return useQuery({
    queryKey: ['presets'],
    queryFn: getPresets,
    staleTime: 60 * 1000,
  })
}

export function useCreatePreset() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (preset: SearchPresetInput) => createPreset(preset),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['presets'] })
    },
  })
}

export function useUpdatePreset() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, preset }: { id: number; preset: SearchPresetInput }) =>
      updatePreset(id, preset),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['presets'] })
    },
  })
}

export function useDeletePreset() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deletePreset(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['presets'] })
    },
  })
}

export function useApplyPreset() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => recordPresetUsage(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['presets'] })
    },
  })
}
