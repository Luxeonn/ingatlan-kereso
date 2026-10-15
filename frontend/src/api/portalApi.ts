import { apiClient } from './client'
import { PortalDescriptor } from '../types/portal'

export async function getPortals(): Promise<PortalDescriptor[]> {
  return apiClient<PortalDescriptor[]>('/api/portals')
}
