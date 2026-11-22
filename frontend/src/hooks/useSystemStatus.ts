import { useQuery } from '@tanstack/react-query'
import { getSystemStatus } from '../api/propertyApi'

export function useSystemStatus(enabled = true) {
  return useQuery({
    queryKey: ['system-status'] as const,
    queryFn: () => getSystemStatus(),
    enabled,
    staleTime: 30_000,
    gcTime: 300_000,
    retry: false,
  })
}
