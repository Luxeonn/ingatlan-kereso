import type { components } from './generated-api'

export type PriceInfo = components['schemas']['PriceInfo']
export type PropertyDetails = components['schemas']['PropertyDetails']
export type PortalSource = components['schemas']['PortalSource']
export type CanonicalPropertyType = components['schemas']['PropertyType']
export type PropertyType = CanonicalPropertyType | string
export type SourceListing = components['schemas']['SourceListing']

export interface BargainInfo {
  isBargain: boolean
  currentPricePerSqm: number
  referenceMedianPricePerSqm: number
  deltaPercent: number // e.g. -24.1
  percentageBelowMedian?: number // e.g. 24.1
  sampleSize: number // e.g. 18
  referenceCity?: string
  referencePropertyType?: string
}

export type IngatlanItem = Omit<
  components['schemas']['IngatlanItem'],
  'propertyType' | 'sourcesCount'
> & {
  propertyType?: PropertyType
  sourcesCount?: number
  bargainInfo?: BargainInfo
}

