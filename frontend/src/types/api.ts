import type { components } from './generated-api'
import type { IngatlanItem } from './property'
import type { PortalStatusDto } from './portal'

export type SearchCriteria = Omit<
  components['schemas']['SearchCriteria'],
  'onlyDiscounted' | 'onlyNewConstruction'
> & {
  onlyDiscounted?: boolean
  onlyNewConstruction?: boolean
}

export type MarketStatsDto = components['schemas']['MarketStatsDto']

export type DashboardResponseDto = Omit<
  components['schemas']['DashboardResponseDto'],
  'items' | 'portalStatuses'
> & {
  items: IngatlanItem[]
  portalStatuses?: PortalStatusDto[]
}

export type PredefinedCity = components['schemas']['PredefinedCity']
export type TrackedCityInfo = components['schemas']['TrackedCityInfo']
export type CitiesResponse = components['schemas']['CitiesResponse']

