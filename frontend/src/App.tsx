import { lazy, Suspense, useEffect, useRef, useMemo, type FC } from 'react'
import { TopNavBar } from './components/layout/TopNavBar'
import { GlobalUndoToast } from './components/layout/WorkspaceChrome'
import { FilterSidebar } from './components/filters/FilterSidebar'
import { CollapsedFilterSummary } from './components/filters/CollapsedFilterSummary'
import { resolveCitiesSummary, resolveTypeLabel } from './components/filters/filterSummaryHelpers'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsPanel } from './components/listings/ListingsPanel'
import { ArchiveSubViewToolbar } from './components/archive/ArchiveSubViewToolbar'
import {
  SearchErrorBanner,
  PartialPortalWarningBanner,
  NewCitiesInitializedBanner,
} from './components/layout/StatusBanners'
import { useSearch } from './hooks/useSearch'
import { useSystemStatus } from './hooks/useSystemStatus'
import { useHiddenProperties, useDiscountedProperties } from './hooks/useArchiveQueries'
import { DEFAULT_PORTALS, useFilterStore } from './store/useFilterStore'
import { useUiStore, type DashboardTab, type ListingSubView } from './store/useUiStore'
import { getLastConfig } from './api/searchApi'
import { openExternalUrl } from './utils/formatters'
import { filterItemsByCities } from './utils/listingFilters'
import type { IngatlanItem } from './types/property'
import {
  resolveActiveListingsSlice,
  computePortalBannerState,
  isArchiveWorkspace,
  hasExecutedOrNonLiveView,
  resolveMedianSqmPrice,
} from './appWorkspaceHelpers'

const MarketAnalyticsPanel = lazy(() =>
  import('./components/analytics/MarketAnalyticsPanel').then((module) => ({
    default: module.MarketAnalyticsPanel,
  })),
)

const DiagnosticsModal = lazy(() =>
  import('./components/modals/DiagnosticsModal').then((module) => ({
    default: module.DiagnosticsModal,
  })),
)

const IngatlanComImportModal = lazy(() =>
  import('./components/modals/IngatlanComImportModal').then((module) => ({
    default: module.IngatlanComImportModal,
  })),
)

function useInitialSavedCriteriaLoad(
  setCriteria: (criteria: ReturnType<typeof useFilterStore.getState>['criteria']) => void,
) {
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

interface WorkspaceBodyProps {
  dashboardTab: DashboardTab
  listingSubView: ListingSubView
  isArchiveView: boolean
  searchState: ReturnType<typeof useSearch>
  slice: ReturnType<typeof resolveActiveListingsSlice>
  scopedRawArchiveItems: IngatlanItem[]
  scopedHiddenItems: IngatlanItem[]
  scopedDiscountedItems: IngatlanItem[]
  hasSearched: boolean
  medianPricePerSqm: number | undefined
  onTriggerSearch: () => void
  onSubViewChange: (subView: ListingSubView) => void
  onClearHistory: (() => void) | undefined
}

const WorkspaceBody: FC<WorkspaceBodyProps> = ({
  dashboardTab,
  listingSubView,
  isArchiveView,
  searchState,
  slice,
  scopedRawArchiveItems,
  scopedHiddenItems,
  scopedDiscountedItems,
  hasSearched,
  medianPricePerSqm,
  onTriggerSearch,
  onSubViewChange,
  onClearHistory,
}) => {
  if (dashboardTab !== 'listings') {
    return (
      <div role="tabpanel" className="rounded-2xl glass-surface p-4">
        <Suspense
          fallback={<div className="min-h-64 animate-shimmer rounded-2xl border border-[var(--glass-border)]" />}
        >
          <MarketAnalyticsPanel
            items={searchState.rawItems}
            serverMarketStats={searchState.data?.marketStats}
            onMarkViewed={searchState.markAsViewed}
            onOpenListing={(url) => {
              openExternalUrl(url)
              searchState.markAsViewed(url)
            }}
          />
        </Suspense>
      </div>
    )
  }

  return (
    <>
      {!isArchiveView && (
        <MarketStatsBar
          data={searchState.data}
          sessionViewedUrls={searchState.sessionViewedUrls}
          rawItems={slice.activeRawItems}
          serverFilteredCount={
            listingSubView === 'listings' ? searchState.data?.filteredCount : undefined
          }
          medianPricePerSqm={listingSubView === 'listings' ? medianPricePerSqm : undefined}
          showMultiSourceSummary={listingSubView === 'listings'}
        />
      )}

      {isArchiveView && (
        <ArchiveSubViewToolbar
          allCount={scopedRawArchiveItems.length}
          hiddenCount={scopedHiddenItems.length}
          discountedCount={scopedDiscountedItems.length}
          activeItems={slice.activeItems}
        />
      )}

      <div role="tabpanel" className="rounded-2xl glass-surface p-4 space-y-4">
        <ListingsPanel
          items={slice.activeItems}
          rawItems={slice.activeRawItems}
          totalRawCount={slice.activeRawCount}
          serverFilteredCount={searchState.data?.filteredCount}
          medianPricePerSqm={medianPricePerSqm}
          sessionViewedUrls={searchState.sessionViewedUrls}
          isLoading={slice.activeLoading}
          hasSearched={hasSearched}
          mode={listingSubView}
          subView={listingSubView}
          onSubViewChange={onSubViewChange}
          searchCount={searchState.rawCount}
          favoritesCount={searchState.favoritesCount}
          onMarkViewed={searchState.markAsViewed}
          onTriggerSearch={onTriggerSearch}
          onClearHistory={onClearHistory}
          isClearingHistory={searchState.isClearingHistory}
          onHide={searchState.hideProperty}
          onRestore={searchState.restoreProperty}
          onSplitSource={searchState.splitSource}
          onUndoOverride={searchState.undoDedupOverride}
          onMergeSelected={searchState.mergeSelected}
          isMerging={searchState.isMerging}
          hiddenCount={searchState.hiddenCount}
        />
      </div>
    </>
  )
}

export const App: FC = () => {
  const searchState = useSearch()
  const { criteria, setCriteria, setAllPortals, setSelectedPortalFilter } = useFilterStore()
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

  const scopedArchiveItems = useMemo(
    () => filterItemsByCities(searchState.archiveItems, criteria.cities),
    [searchState.archiveItems, criteria.cities],
  )
  const scopedRawArchiveItems = useMemo(
    () => filterItemsByCities(searchState.rawArchiveItems, criteria.cities),
    [searchState.rawArchiveItems, criteria.cities],
  )
  const scopedHiddenItems = useMemo(
    () => filterItemsByCities(globalHiddenData ?? [], criteria.cities),
    [globalHiddenData, criteria.cities],
  )
  const scopedDiscountedItems = useMemo(
    () => filterItemsByCities(globalDiscountedData ?? [], criteria.cities),
    [globalDiscountedData, criteria.cities],
  )

  const slice = resolveActiveListingsSlice({
    listingSubView: ui.listingSubView,
    archiveSubMode: ui.archiveSubMode,
    items: searchState.items,
    rawItems: searchState.rawItems,
    rawCount: searchState.rawCount,
    favoriteItems: searchState.favoriteItems,
    rawFavoriteItems: searchState.rawFavoriteItems,
    archiveItems: scopedArchiveItems,
    rawArchiveItems: scopedRawArchiveItems,
    globalHiddenItems: scopedHiddenItems,
    globalDiscountedItems: scopedDiscountedItems,
    isLoading: searchState.isLoading,
    isArchiveLoading: searchState.isArchiveLoading,
  })

  const hasSearched = hasExecutedOrNonLiveView(ui.listingSubView, searchState.data)
  const clearHistoryFn = isArchiveView ? () => void searchState.clearArchiveHistory() : undefined
  const medianPricePerSqm = resolveMedianSqmPrice(searchState.data)

  return (
    <div className="min-h-screen w-full overflow-x-clip flex flex-col text-[var(--text-primary)]">
      <TopNavBar
        totalInDb={systemStatus?.totalInDb}
        portalStatuses={searchState.data?.portalStatuses}
        listingsCount={searchState.rawCount}
        favoritesCount={searchState.favoritesCount}
        archiveCount={scopedRawArchiveItems.length}
        analyticsCount={searchState.rawItems.length}
        dashboardTab={ui.dashboardTab}
        listingSubView={ui.listingSubView}
        isFiltersCollapsed={ui.isFiltersCollapsed}
        onToggleFilters={() => ui.setFiltersCollapsed(!ui.isFiltersCollapsed)}
        onSelectListings={() => {
          ui.setDashboardTab('listings')
          ui.setListingSubView('listings')
        }}
        onSelectFavorites={() => {
          ui.setDashboardTab('listings')
          ui.setListingSubView('favorites')
        }}
        onSelectArchive={() => {
          ui.setDashboardTab('listings')
          ui.setListingSubView('archive')
        }}
        onSelectAnalytics={() => ui.setDashboardTab('analytics')}
      />

      <div className="mx-auto flex w-full max-w-[1640px] items-start gap-5 px-6 py-5">
        {!ui.isFiltersCollapsed && (
          <FilterSidebar
            onSearch={handleSearch}
            isLoading={searchState.isLoading}
            showCollapseToggle={false}
          />
        )}

        <main className="min-w-0 flex-1 space-y-4">
          {ui.isFiltersCollapsed && (
            <CollapsedFilterSummary
              citiesSummary={resolveCitiesSummary(criteria.cities, criteria.city)}
              typeLabel={resolveTypeLabel(criteria.propertyType)}
              minPriceMillion={criteria.minPriceMillion}
              maxPriceMillion={criteria.maxPriceMillion}
              minArea={criteria.minArea}
              maxArea={criteria.maxArea}
              portalsCount={criteria.portals.length}
              onExpand={() => ui.setFiltersCollapsed(false)}
              expandLabel="Szűrők módosítása"
              expandAriaLabel="Szűrőpanel megnyitása"
            />
          )}

          {searchState.isError && (
            <SearchErrorBanner error={searchState.error} onRetry={handleSearch} />
          )}

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
                setSelectedPortalFilter('all')
                handleSearch()
              }}
            />
          )}

          <NewCitiesInitializedBanner cities={searchState.data?.newCitiesInitialized} />

          <WorkspaceBody
            dashboardTab={ui.dashboardTab}
            listingSubView={ui.listingSubView}
            isArchiveView={isArchiveView}
            searchState={searchState}
            slice={slice}
            scopedRawArchiveItems={scopedRawArchiveItems}
            scopedHiddenItems={scopedHiddenItems}
            scopedDiscountedItems={scopedDiscountedItems}
            hasSearched={hasSearched}
            medianPricePerSqm={medianPricePerSqm}
            onTriggerSearch={handleSearch}
            onSubViewChange={ui.setListingSubView}
            onClearHistory={clearHistoryFn}
          />
        </main>
      </div>

      <GlobalUndoToast
        toastMessage={searchState.toastMessage}
        toastUndoItem={searchState.toastUndoItem}
        hasSelectedCards={ui.selectedCardUrls.length > 0}
        onRestoreUndo={searchState.restoreProperty}
        onClear={searchState.clearToast}
      />

      <Suspense fallback={null}>
        {ui.isDiagnosticsModalOpen && (
          <DiagnosticsModal portalStatuses={searchState.data?.portalStatuses} />
        )}
        {ui.isIngatlanComImportModalOpen && (
          <IngatlanComImportModal
            fetchedPages={bannerState.ingatlanComFetchedPages}
            onImportSuccess={handleSearch}
          />
        )}
      </Suspense>
    </div>
  )
}

export default App
