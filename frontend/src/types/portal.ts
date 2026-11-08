import type { components } from './generated-api'

export const PORTAL_IDS = ['dh', 'oc', 'jofogas', 'ingatlancom', 'ingatlanbazar', 'koltozzbe'] as const
export type PortalId = (typeof PORTAL_IDS)[number]
export type PortalFilterOption = PortalId | 'all'

export type PortalDescriptor = components['schemas']['PortalDescriptor']
export type PortalStatusDto = components['schemas']['PortalStatusDto']
export type PortalScrapeStatus = components['schemas']['PortalScrapeStatus']

