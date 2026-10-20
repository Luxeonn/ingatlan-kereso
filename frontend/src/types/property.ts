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

export interface IngatlanItem {
  id: string
  portal: string
  url: string
  title: string
  price: string
  location: string
  city: string
  details?: string
  discoveredAt?: string
  isNew?: boolean
  viewed?: boolean
  viewedAt?: string
  imageUrl?: string
  status?: string
  priceDelta?: number
  priceChangePercent?: number
  lastPriceChangeAt?: string
  priceInfo?: PriceInfo
  detailsInfo?: PropertyDetails
  source?: PortalSource
}
