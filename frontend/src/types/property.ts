import type { components } from './generated-api'

export type PriceInfo = components['schemas']['PriceInfo']
export type PropertyDetails = components['schemas']['PropertyDetails']
export type PortalSource = components['schemas']['PortalSource']
export type CanonicalPropertyType = components['schemas']['PropertyType']
export type PropertyType = CanonicalPropertyType
export type SourceListing = components['schemas']['SourceListing']
export type IngatlanItem = components['schemas']['IngatlanItem']

export type PriceHistoryEntry = components['schemas']['PriceHistoryEntry']
export type ListingObservation = components['schemas']['ListingObservation']
export type ListingObservationEvent = NonNullable<ListingObservation['statusEvent']>
export type StatusLogEntry = components['schemas']['StatusLogEntry']
export type ListingStatus = components['schemas']['ListingStatus']
export type ListingSortOption = components['schemas']['ListingSortOption']

export interface BargainInfo {
  isBargain: boolean
  currentPricePerSqm: number
  referenceMedianPricePerSqm: number
  deltaPercent: number // e.g. -24.1
  percentageBelowMedian?: number // e.g. 24.1
  sampleSize: number // e.g. 18
  referenceCity?: string
  referencePropertyType?: CanonicalPropertyType
}

export interface CrossPortalSpreadInfo {
  activeSourcesCount: number
  minActivePriceHuf: number
  maxActivePriceHuf: number
  spreadAmountHuf: number
  spreadMillion: number
  spreadPercent: number
  cheapestPortal: string
}
