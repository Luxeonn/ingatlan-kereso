import { useQuery } from '@tanstack/react-query'
import { getPortals } from '../api/portalApi'

export function usePortals() {
  return useQuery({
    queryKey: ['portals'],
    queryFn: getPortals,
    staleTime: 5 * 60 * 1000, // 5 perc
  })
}
