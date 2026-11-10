import { fetchClient, unwrapApiResponse } from './client'
import type { SearchCriteria, DashboardResponseDto } from '../types/api'
import type { components } from '../types/generated-api'
import { validateSearchCriteria } from '../utils/validation'

export async function getLastConfig(): Promise<SearchCriteria> {
  return unwrapApiResponse(fetchClient.GET('/api/config'))
}

export async function executeSearch(criteria: SearchCriteria): Promise<DashboardResponseDto> {
  const validCriteria = validateSearchCriteria(criteria)
  return unwrapApiResponse(
    fetchClient.POST('/api/search', {
      body: validCriteria as components['schemas']['SearchCriteria'],
    }),
  )
}
