import { useQuery } from '@tanstack/react-query'
import { getCities } from '../api/cityApi'

export function useCities() {
  return useQuery({
    queryKey: ['cities'],
    queryFn: getCities,
    staleTime: 10 * 60 * 1000, // 10 perc
  })
}
