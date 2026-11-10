import { fetchClient, unwrapApiResponse } from './client'
import type { CitiesResponse } from '../types/api'

export async function getCities(): Promise<CitiesResponse> {
  return unwrapApiResponse(fetchClient.GET('/api/cities'))
}
