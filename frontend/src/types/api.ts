import { IngatlanItem } from './property'
import { PortalStatusDto } from './portal'

export interface SearchCriteria {
  city?: string
  cities: string[]
  propertyType: string
  minPriceMillion: number
  maxPriceMillion: number
  minArea: number
  maxArea: number
  minRooms?: number
  portals: string[]
  autoOpenBrowser: boolean
  excludeViewed: boolean
  onlyDiscounted?: boolean
}

export interface MarketStatsDto {
  averagePriceMillion?: number
  averagePricePerM2?: number
  minPriceMillion?: number
  maxPriceMillion?: number
  newRatePercent?: number
  cityBreakdown?: Record<string, number>
}

export interface DashboardResponseDto {
  items: IngatlanItem[]
  newCount: number
  totalFound: number
  executedAt?: string
  portalCounts?: Record<string, number>
  cityCounts?: Record<string, number>
  newCitiesInitialized?: string[]
  marketStats?: MarketStatsDto
  portalStatuses?: PortalStatusDto[]
}

export interface PredefinedCity {
  key?: string
  name: string
  county?: string
  description?: string
}

export interface TrackedCityInfo {
  name: string
  key: string
  count: number
}

export interface CitiesResponse {
  predefined: Array<PredefinedCity | string>
  presets: Record<string, string[]>
  trackedCities: TrackedCityInfo[] | Record<string, number>
}

export interface StatusResponse {
  status: string
  port: number
  totalInDb: number
  lastRunTime?: string
  trackedCities?: Record<string, number>
}

export interface LogEntry {
  timestamp?: string
  level?: string
  loggerName?: string
  message?: string
  throwable?: string
}

export interface LogsResponse {
  logs: LogEntry[]
  rawLogs: string[]
  logFilePath?: string
  level?: string
}
