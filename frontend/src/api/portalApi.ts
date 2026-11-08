import { fetchClient, unwrapApiResponse } from './client'
import type { PortalDescriptor } from '../types/portal'

export async function getPortals(): Promise<PortalDescriptor[]> {
  return unwrapApiResponse(fetchClient.GET('/api/portals'))
}
