export interface PriceInfo {
  rawText?: string
  amountHuf?: number
  amountMillion?: number
}

export interface PropertyDetails {
  rawText?: string
  areaSquareMeters?: number
  roomCount?: number
  pricePerSquareMeter?: number
}

export interface PortalSource {
  portalId?: string
  portalName?: string
  themeColor?: string
  directListing?: boolean
}

export interface PriceHistoryEntry {
  id: number
  propertyId: string
  recordedAt: string
  priceRaw: string
  priceAmount: number
  priceMillions: number
  pricePerSqm?: number
  previousPriceAmount?: number
  priceDelta?: number
  changePercent?: number
}

export interface StatusLogEntry {
  id: number
  propertyId: string
  status: string
  changedAt: string
  note: string
}

export type CanonicalPropertyType =
  | 'LAKAS'
  | 'HAZ'
  | 'TELEK'
  | 'GARAZS'
  | 'IPARI_KERESKEDELMI'
  | 'EGYEB_ISMERETLEN'

export type PropertyType = CanonicalPropertyType | string

export interface SourceListing {
  portal?: string
  externalListingId?: string
  url?: string
  title?: string
  imageUrl?: string
  firstSeenAt?: string
  lastSeenAt?: string
  status?: string
  priceHuf?: number
  pricePerM2?: number
  viewed?: boolean
  viewedAt?: string
}

export interface ListingObservation {
  observedAt?: string
  priceHuf?: number
  pricePerM2?: number
  statusEvent?: 'NEW' | 'UNCHANGED' | 'PRICE_CHANGED' | 'RELISTED'
  priceDelta?: number
  priceChangePercent?: number
}

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

export interface IngatlanItem {
  id: string
  portal: string
  url: string
  title: string
  price: string
  location: string
  city: string
  propertyType?: PropertyType
  sourcesCount?: number
  sourceListings?: SourceListing[]
  details?: string
  discoveredAt?: string
  isNew?: boolean
  viewed?: boolean
  viewedAt?: string
  imageUrl?: string
  newConstruction?: boolean
  status?: string
  priceDelta?: number
  priceChangePercent?: number
  lastPriceChangeAt?: string
  priceInfo?: PriceInfo
  detailsInfo?: PropertyDetails
  source?: PortalSource
  isBargain?: boolean
  referenceMedianPricePerSquareMeter?: number
  percentageBelowMedian?: number
  referenceSampleSize?: number
  bargainInfo?: BargainInfo
  qualityScore?: number
  qualityDeductions?: string[]
}

export type QualityRating = 'EXCELLENT' | 'ACCEPTABLE' | 'SUSPICIOUS'

export interface QualityScoreInfo {
  score: number
  rating: QualityRating
  ratingLabel: string
  deductions: string[]
}

export interface MarketSegmentStats {
  city?: string
  propertyType?: PropertyType
  propertyTypeName?: string
  residential?: boolean
  plot?: boolean
  sampleSize?: number
  insufficientData?: boolean
  medianPricePerM2?: number
  averagePricePerM2?: number
  medianPriceMillion?: number
  averagePriceMillion?: number
  minPriceMillion?: number
  maxPriceMillion?: number
  bargainCount?: number
}

export interface PriceDropItem {
  id: string
  url: string
  title?: string
  city?: string
  portal?: string
  propertyType?: PropertyType
  currentPriceHuf?: number
  currentPriceMillion?: number
  priceDelta?: number
  changePercent?: number
  pricePerM2?: number
  imageUrl?: string
  lastPriceChangeAt?: string
}
