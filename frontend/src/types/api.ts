import type { components, operations } from './generated-api'

export type SearchCriteria = components['schemas']['SearchCriteria']
export type SearchExecutionStatus = components['schemas']['SearchExecutionStatus']
export type MarketStatsDto = components['schemas']['MarketStatsDto']
export type MarketSegmentStatsDto = components['schemas']['MarketSegmentStatsDto']
export type PriceDropItemDto = components['schemas']['PriceDropItemDto']
export type DashboardResponseDto = components['schemas']['DashboardResponseDto']

export type StatusResponse = components['schemas']['StatusResponse']
export type ApiErrorResponse = components['schemas']['ApiErrorResponse']
export type HistoryQuery = NonNullable<operations['getHistory']['parameters']['query']>

export type PredefinedCity = components['schemas']['PredefinedCity']
export type TrackedCityInfo = components['schemas']['TrackedCityInfo']
export type CitiesResponse = components['schemas']['CitiesResponse']
export type SearchPreset = components['schemas']['SearchPreset']
export type SearchPresetInput = components['schemas']['SearchPresetInput']
