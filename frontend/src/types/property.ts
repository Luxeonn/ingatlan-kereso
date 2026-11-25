import type { components } from './generated-api'

export type CanonicalPropertyType = components['schemas']['PropertyType']
export type PropertyType = CanonicalPropertyType
export type SourceListing = components['schemas']['SourceListing']
export type IngatlanItem = components['schemas']['IngatlanItem']

export type PriceHistoryEntry = components['schemas']['PriceHistoryEntry']
export type ListingObservation = components['schemas']['ListingObservation']
export type StatusLogEntry = components['schemas']['StatusLogEntry']
export type ListingSortOption = components['schemas']['ListingSortOption']

export interface CrossPortalSpreadInfo {
  activeSourcesCount: number
  minActivePriceHuf: number
  maxActivePriceHuf: number
  spreadAmountHuf: number
  spreadMillion: number
  spreadPercent: number
  cheapestPortal: string
}
