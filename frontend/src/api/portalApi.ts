import { apiClient } from './client'
import type { PortalDescriptor } from '../types/portal'

export async function getPortals(): Promise<PortalDescriptor[]> {
  return apiClient<PortalDescriptor[]>('/api/portals')
}
