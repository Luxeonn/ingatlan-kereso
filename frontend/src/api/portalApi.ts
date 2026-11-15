import { fetchClient, unwrapApiResponse } from './client'
import type {
  IngatlanComHtmlImportRequest,
  IngatlanComHtmlImportResponse,
  PortalDescriptor,
} from '../types/portal'

export async function getPortals(): Promise<PortalDescriptor[]> {
  return unwrapApiResponse(fetchClient.GET('/api/portals'))
}

export async function importIngatlanComHtml(
  payload: IngatlanComHtmlImportRequest,
): Promise<IngatlanComHtmlImportResponse> {
  return unwrapApiResponse(
    fetchClient.POST('/api/portals/ingatlancom/import-html', {
      body: payload,
    }),
  )
}
