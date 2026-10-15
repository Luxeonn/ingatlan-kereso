export interface PriceInfo {
  rawText?: string
  amountFt?: number
  amountMillion?: number
}

export interface PropertyDetails {
  rawText?: string
  floorAreaSqm?: number
  roomCount?: number
  pricePerSqmFt?: number
}

export interface PortalSource {
  id?: string
  portalName?: string
  colorHex?: string
}

export interface PriceHistoryEntry {
  id?: string
  url?: string
  priceMillion?: number
  priceFt?: number
  discoveredAt?: string
}

export interface StatusLogEntry {
  id?: string
  url?: string
  status?: string
  timestamp?: string
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
