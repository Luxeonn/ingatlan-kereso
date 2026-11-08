import { fetchClient, unwrapApiResponse } from './client'
import type { SearchCriteria, DashboardResponseDto } from '../types/api'
import type { components } from '../types/generated-api'

export async function getLastConfig(): Promise<SearchCriteria> {
  return unwrapApiResponse(fetchClient.GET('/api/config'))
}

export async function executeSearch(criteria: SearchCriteria): Promise<DashboardResponseDto> {
  return unwrapApiResponse(
    fetchClient.POST('/api/search', {
      body: criteria as components['schemas']['SearchCriteria'],
    }),
  )
}
