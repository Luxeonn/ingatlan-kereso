import type { components } from './generated-api'
import type { IngatlanItem, PropertyType } from './property'
import type { PortalStatusDto } from './portal'

export type SearchCriteria = Omit<
  components['schemas']['SearchCriteria'],
  'propertyType' | 'onlyDiscounted' | 'onlyNewConstruction'
> & {
  propertyType?: PropertyType
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
export type SearchPreset = Omit<components['schemas']['SearchPreset'], 'criteria'> & {
  criteria: SearchCriteria
}
export type SearchPresetInput = Omit<components['schemas']['SearchPresetInput'], 'criteria'> & {
  criteria?: SearchCriteria
}

