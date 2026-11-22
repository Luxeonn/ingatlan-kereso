import { lazy, Suspense, useEffect, useRef, useMemo, type FC } from 'react'
import { Header } from './components/layout/Header'
import { WorkspaceHeaderNav, GlobalUndoToast } from './components/layout/WorkspaceChrome'
import { SearchPanel } from './components/filters/SearchPanel'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsPanel } from './components/listings/ListingsPanel'
import { ArchiveSubViewToolbar } from './components/archive/ArchiveSubViewToolbar'
import { DiagnosticsModal } from './components/modals/DiagnosticsModal'
import { IngatlanComImportModal } from './components/modals/IngatlanComImportModal'
import {
  SearchErrorBanner,
  PartialPortalWarningBanner,
  NewCitiesInitializedBanner,
} from './components/layout/StatusBanners'
import { useSearch } from './hooks/useSearch'
import { useSystemStatus } from './hooks/useSystemStatus'
import { useHiddenProperties, useDiscountedProperties } from './hooks/useArchiveQueries'
import { DEFAULT_PORTALS, useFilterStore } from './store/useFilterStore'
import { useUiStore, type DashboardTab, type ListingSubView, type ArchiveSubMode } from './store/useUiStore'
import { getLastConfig } from './api/searchApi'
import { openExternalUrl } from './utils/formatters'
import { extractIngatlanComFetchedPages, findPartialIngatlanComStatus } from './utils/ingatlanComUrl'
import type { IngatlanItem } from './types/property'
import type { DashboardResponseDto } from './types/api'
import type { PortalStatusDto } from './types/portal'

const MarketAnalyticsPanel = lazy(() =>
  import('./components/analytics/MarketAnalyticsPanel').then((module) => ({
    default: module.MarketAnalyticsPanel,
  })),
)

interface ActiveSliceParams {
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

function resolveActiveListingsSlice(params: ActiveSliceParams) {
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

interface PortalBannerState {
  failures: PortalStatusDto[]
  hasPartialIngatlanCom: boolean
  ingatlanComMessage?: string | null
  ingatlanComFetchedPages: number | null
  isNarrowPortalSelection: boolean
  showPortalBanner: boolean
  isTotalPortalFailure: boolean
}

function computePortalBannerState(
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

function useInitialSavedCriteriaLoad(setCriteria: (criteria: ReturnType<typeof useFilterStore.getState>['criteria']) => void) {
  const initialLoadDone = useRef(false)
  useEffect(() => {
    if (initialLoadDone.current) return
    initialLoadDone.current = true
    if (!window.location.search) {
      getLastConfig()
        .then((saved) => {
          if (saved) setCriteria(saved)
        })
        .catch(() => {})
    }
  }, [setCriteria])
}

function isArchiveWorkspace(dashboardTab: DashboardTab, listingSubView: ListingSubView): boolean {
  return dashboardTab === 'listings' && listingSubView === 'archive'
}

function hasExecutedOrNonLiveView(listingSubView: ListingSubView, data: DashboardResponseDto | null | undefined): boolean {
  return listingSubView !== 'listings' || Boolean(data)
}

export const App: FC = () => {
  const searchState = useSearch()
  const { criteria, setCriteria, setAllPortals } = useFilterStore()
  const ui = useUiStore()
  const { data: systemStatus } = useSystemStatus()
  const isArchiveView = isArchiveWorkspace(ui.dashboardTab, ui.listingSubView)
  const { data: globalHiddenData } = useHiddenProperties(isArchiveView)
  const { data: globalDiscountedData } = useDiscountedProperties(undefined, isArchiveView)

  useInitialSavedCriteriaLoad(setCriteria)

  const bannerState = useMemo(
    () =>
      computePortalBannerState(
        searchState.data,
        criteria.portals,
        ui.dashboardTab,
        ui.listingSubView,
      ),
    [searchState.data, criteria.portals, ui.dashboardTab, ui.listingSubView],
  )

  const handleSearch = () => {
    ui.setDashboardTab('listings')
    ui.setListingSubView('listings')
    searchState.search()
  }

  const slice = resolveActiveListingsSlice({
    listingSubView: ui.listingSubView,
    archiveSubMode: ui.archiveSubMode,
    items: searchState.items,
    rawItems: searchState.rawItems,
    rawCount: searchState.rawCount,
    favoriteItems: searchState.favoriteItems,
    rawFavoriteItems: searchState.rawFavoriteItems,
    archiveItems: searchState.archiveItems,
    rawArchiveItems: searchState.rawArchiveItems,
    globalHiddenItems: globalHiddenData,
    globalDiscountedItems: globalDiscountedData,
    isLoading: searchState.isLoading,
    isArchiveLoading: searchState.isArchiveLoading,
  })

  const hasSearched = hasExecutedOrNonLiveView(ui.listingSubView, searchState.data)
  const clearHistoryFn = isArchiveView ? () => void searchState.clearArchiveHistory() : undefined

  const handleSelectListings = () => {
    ui.setDashboardTab('listings')
    if (ui.listingSubView === 'archive') ui.setListingSubView('listings')
  }

  const handleSelectArchive = () => {
    ui.setDashboardTab('listings')
    ui.setListingSubView('archive')
  }

  return (
    <div className="min-h-screen bg-[var(--bg-body)] text-[var(--text-primary)] transition-colors duration-200">
      <Header totalInDb={systemStatus?.totalInDb} portalStatuses={searchState.data?.portalStatuses} />

      <main className="mx-auto max-w-[1600px] px-4 py-5 sm:px-6 space-y-5">
        <SearchPanel onSearch={handleSearch} isLoading={searchState.isLoading} />

        {searchState.isError && <SearchErrorBanner error={searchState.error} onRetry={handleSearch} />}

        {bannerState.showPortalBanner && (
          <PartialPortalWarningBanner
            failures={bannerState.failures}
            isTotalFailure={bannerState.isTotalPortalFailure}
            hasIngatlanComPartial={bannerState.hasPartialIngatlanCom}
            ingatlanComMessage={bannerState.ingatlanComMessage}
            showEnableAllPortals={bannerState.isNarrowPortalSelection}
            onOpenDiagnostics={() => ui.setDiagnosticsModalOpen(true)}
            onOpenImportModal={() => ui.setIngatlanComImportModalOpen(true)}
            onEnableAllPortals={() => {
              setAllPortals([...DEFAULT_PORTALS])
              handleSearch()
            }}
          />
        )}

        <NewCitiesInitializedBanner cities={searchState.data?.newCitiesInitialized} />
        <MarketStatsBar data={searchState.data} />

        <section aria-label="Munkaterület" className="space-y-0">
          <WorkspaceHeaderNav
            dashboardTab={ui.dashboardTab}
            listingSubView={ui.listingSubView}
            onSelectListings={handleSelectListings}
            onSelectArchive={handleSelectArchive}
            onSelectAnalytics={() => ui.setDashboardTab('analytics')}
          />

          <div
            role="tabpanel"
            className="rounded-b-2xl rounded-tr-2xl border border-[var(--border-hairline)] bg-[var(--bg-surface)] p-4 sm:p-5 shadow-sm space-y-4"
          >
            {isArchiveView && (
              <ArchiveSubViewToolbar
                allCount={searchState.rawArchiveItems.length}
                hiddenCount={globalHiddenData?.length}
                discountedCount={globalDiscountedData?.length}
              />
            )}

            {ui.dashboardTab === 'listings' ? (
              <ListingsPanel
                items={slice.activeItems}
                rawItems={slice.activeRawItems}
                totalRawCount={slice.activeRawCount}
                serverFilteredCount={searchState.data?.filteredCount}
                isLoading={slice.activeLoading}
                hasSearched={hasSearched}
                mode={ui.listingSubView}
                subView={ui.listingSubView}
                onSubViewChange={ui.setListingSubView}
                searchCount={searchState.rawCount}
                favoritesCount={searchState.favoritesCount}
                archiveCount={searchState.rawArchiveItems.length}
                onMarkViewed={searchState.markAsViewed}
                onOpenAll={searchState.openAllTabs}
                onTriggerSearch={handleSearch}
                onClearHistory={clearHistoryFn}
                isClearingHistory={searchState.isClearingHistory}
                onHide={searchState.hideProperty}
                onRestore={searchState.restoreProperty}
                onSplitSource={searchState.splitSource}
                onUndoOverride={searchState.undoDedupOverride}
                onMergeSelected={searchState.mergeSelected}
                isMerging={searchState.isMerging}
                hiddenCount={searchState.hiddenCount}
              />
            ) : (
              <Suspense fallback={<div className="min-h-64 animate-pulse rounded-2xl bg-[var(--bg-card)]" />}>
                <MarketAnalyticsPanel
                  items={searchState.items}
                  serverMarketStats={searchState.data?.marketStats}
                  onMarkViewed={searchState.markAsViewed}
                  onOpenListing={(url) => {
                    openExternalUrl(url)
                    searchState.markAsViewed(url)
                  }}
                />
              </Suspense>
            )}
          </div>
        </section>
      </main>

      <GlobalUndoToast
        toastMessage={searchState.toastMessage}
        toastUndoItem={searchState.toastUndoItem}
        hasSelectedCards={ui.selectedCardUrls.length > 0}
        onRestoreUndo={searchState.restoreProperty}
        onClear={searchState.clearToast}
      />

      <DiagnosticsModal portalStatuses={searchState.data?.portalStatuses} />
      <IngatlanComImportModal
        fetchedPages={bannerState.ingatlanComFetchedPages}
        onImportSuccess={handleSearch}
      />
    </div>
  )
}

export default App
