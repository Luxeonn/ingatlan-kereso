export interface PortalDescriptor {
  id: string
  name: string
  color: string
  searchUrl: string
  supportedTypes: string[]
  savedListingCount?: number
}

export interface PortalStatusDto {
  portalId: string
  portalName: string
  status: string
  durationMs: number
  itemCount: number
  errorMessage?: string
  botProtected: boolean
}
