import { apiClient } from './client'
import { SearchCriteria, DashboardResponseDto } from '../types/api'

export async function getLastConfig(): Promise<SearchCriteria> {
  return apiClient<SearchCriteria>('/api/config')
}

export async function saveLastConfig(criteria: SearchCriteria): Promise<{ success: boolean }> {
  return apiClient<{ success: boolean }>('/api/config', {
    method: 'POST',
    body: JSON.stringify(criteria),
  })
}

export async function executeSearch(criteria: SearchCriteria): Promise<DashboardResponseDto> {
  return apiClient<DashboardResponseDto>('/api/search', {
    method: 'POST',
    body: JSON.stringify(criteria),
  })
}
