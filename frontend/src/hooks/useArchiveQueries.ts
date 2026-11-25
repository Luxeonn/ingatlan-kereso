import { useQuery } from '@tanstack/react-query'
import {
  getHiddenProperties,
  getDiscountedProperties,
} from '../api/propertyApi'

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
