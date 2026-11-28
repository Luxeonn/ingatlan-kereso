import type { DashboardResponseDto } from './types/api'
import type { PortalStatusDto } from './types/portal'
import type { IngatlanItem } from './types/property'
import type { ArchiveSubMode, DashboardTab, ListingSubView } from './store/useUiStore'
import { extractIngatlanComFetchedPages, findPartialIngatlanComStatus } from './utils/ingatlanComUrl'

export interface ActiveSliceParams {
  listingSubView: ListingSubView
  archiveSubMode: ArchiveSubMode
  items: IngatlanItem[]
  rawItems: IngatlanItem[]
  rawCount: number
  favoriteItems: IngatlanItem[]
  rawFavoriteItems: IngatlanItem[]
  archiveItems: IngatlanItem[]
  rawArchiveItems: IngatlanItem[]
  globalHiddenItems?: IngatlanItem[]
  globalDiscountedItems?: IngatlanItem[]
  isLoading: boolean
  isArchiveLoading: boolean
}

function resolveArchiveModeSlice(params: ActiveSliceParams) {
  const hidden = params.globalHiddenItems ?? []
  const discounted = params.globalDiscountedItems ?? []
  if (params.archiveSubMode === 'hidden') {
    return {
      activeItems: hidden,
      activeRawItems: hidden,
      activeRawCount: hidden.length,
      activeLoading: params.isArchiveLoading,
    }
  }
  if (params.archiveSubMode === 'discounted') {
    return {
      activeItems: discounted,
      activeRawItems: discounted,
      activeRawCount: discounted.length,
      activeLoading: params.isArchiveLoading,
    }
  }
  return {
    activeItems: params.archiveItems,
    activeRawItems: params.rawArchiveItems,
    activeRawCount: params.rawArchiveItems.length,
    activeLoading: params.isArchiveLoading,
  }
}

export function resolveActiveListingsSlice(params: ActiveSliceParams) {
  if (params.listingSubView === 'favorites') {
    return {
      activeItems: params.favoriteItems,
      activeRawItems: params.rawFavoriteItems,
      activeRawCount: params.rawFavoriteItems.length,
      activeLoading: false,
    }
  }
  if (params.listingSubView === 'archive') {
    return resolveArchiveModeSlice(params)
  }
  return {
    activeItems: params.items,
    activeRawItems: params.rawItems,
    activeRawCount: params.rawCount,
    activeLoading: params.isLoading,
  }
}

export interface PortalBannerState {
  failures: PortalStatusDto[]
  hasPartialIngatlanCom: boolean
  ingatlanComMessage?: string | null
  ingatlanComFetchedPages: number | null
  isNarrowPortalSelection: boolean
  showPortalBanner: boolean
  isTotalPortalFailure: boolean
}

export function computePortalBannerState(
  data: DashboardResponseDto | null | undefined,
  portals: string[] | null | undefined,
  dashboardTab: DashboardTab,
  listingSubView: ListingSubView,
): PortalBannerState {
  const statuses = data?.portalStatuses ?? []
  const failures = statuses.filter((p) => p.status === 'FAILED' || p.status === 'BOT_PROTECTED')
  const partialIngatlan = findPartialIngatlanComStatus(statuses)
  const hasPartialIngatlanCom = Boolean(partialIngatlan)
  const ingatlanComFetchedPages = extractIngatlanComFetchedPages(partialIngatlan)
  const portalCount = portals?.length ?? 0
  const isNarrowPortalSelection = portalCount > 0 && portalCount <= 2
  const showPortalBanner =
    (failures.length > 0 || hasPartialIngatlanCom) &&
    dashboardTab === 'listings' &&
    listingSubView === 'listings'
  const isTotalPortalFailure = Boolean(
    data &&
      (data.executionStatus === 'TOTAL_FAILURE' ||
        (statuses.length > 0 && failures.length === statuses.length)),
  )

  return {
    failures,
    hasPartialIngatlanCom,
    ingatlanComMessage: partialIngatlan?.message,
    ingatlanComFetchedPages,
    isNarrowPortalSelection,
    showPortalBanner,
    isTotalPortalFailure,
  }
}

export function isArchiveWorkspace(
  dashboardTab: DashboardTab,
  listingSubView: ListingSubView,
): boolean {
  return dashboardTab === 'listings' && listingSubView === 'archive'
}

export function hasExecutedOrNonLiveView(
  listingSubView: ListingSubView,
  data: DashboardResponseDto | null | undefined,
): boolean {
  return listingSubView !== 'listings' || Boolean(data)
}

export function resolveMedianSqmPrice(data: DashboardResponseDto | null | undefined): number | undefined {
  const stats = data?.marketStats
  return stats?.medianPricePerM2 ?? stats?.residentialMedianPricePerM2 ?? stats?.plotMedianPricePerM2
}
