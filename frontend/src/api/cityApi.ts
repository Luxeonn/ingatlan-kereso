import { apiClient } from './client'
import type { CitiesResponse } from '../types/api'

export async function getCities(): Promise<CitiesResponse> {
  return apiClient<CitiesResponse>('/api/cities')
}
