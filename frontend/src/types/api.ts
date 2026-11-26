import type { components, operations } from './generated-api'

export type SearchCriteria = components['schemas']['SearchCriteria']
export type MarketStatsDto = components['schemas']['MarketStatsDto']
export type MarketSegmentStatsDto = components['schemas']['MarketSegmentStatsDto']
export type DashboardResponseDto = components['schemas']['DashboardResponseDto']

export type StatusResponse = components['schemas']['StatusResponse']
export type HistoryQuery = NonNullable<operations['getHistory']['parameters']['query']>

export type CitiesResponse = components['schemas']['CitiesResponse']
export type SearchPreset = components['schemas']['SearchPreset']
export type SearchPresetInput = components['schemas']['SearchPresetInput']
