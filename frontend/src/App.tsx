import { lazy, Suspense, useEffect, useRef, useMemo, type FC } from 'react'
import { LayoutGrid, BarChart2 } from 'lucide-react'
import { Header } from './components/layout/Header'
import { SearchPanel } from './components/filters/SearchPanel'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsPanel } from './components/listings/ListingsPanel'
import { DiagnosticsModal } from './components/modals/DiagnosticsModal'
import { SearchErrorBanner, PartialPortalWarningBanner } from './components/layout/StatusBanners'
import { useSearch } from './hooks/useSearch'
import { useFilterStore } from './store/useFilterStore'
import { useUiStore } from './store/useUiStore'
import { getLastConfig } from './api/searchApi'
import { openExternalUrl } from './utils/formatters'
import { cn } from './utils/cn'
import type { IngatlanItem } from './types/property'
import type { DashboardResponseDto } from './types/api'
import type { PortalStatusDto } from './types/portal'

const MarketAnalyticsPanel = lazy(() =>
  import('./components/analytics/MarketAnalyticsPanel').then((module) => ({
    default: module.MarketAnalyticsPanel,
  })),
)

interface ActiveSliceParams {
  listingSubView: string
  items: IngatlanItem[]
  rawItems: IngatlanItem[]
  rawCount: number
  favoriteItems: IngatlanItem[]
  rawFavoriteItems: IngatlanItem[]
  archiveItems: IngatlanItem[]
  rawArchiveItems: IngatlanItem[]
  isLoading: boolean
  isArchiveLoading: boolean
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
    return {
      activeItems: params.archiveItems,
      activeRawItems: params.rawArchiveItems,
      activeRawCount: params.rawArchiveItems.length,
      activeLoading: params.isArchiveLoading,
    }
  }
  return {
    activeItems: params.items,
    activeRawItems: params.rawItems,
    activeRawCount: params.rawCount,
    activeLoading: params.isLoading,
  }
}

function extractPortalFailures(statuses?: PortalStatusDto[] | null): PortalStatusDto[] {
  if (!statuses) return []
  return statuses.filter((p) => p.status === 'FAILED' || p.status === 'BOT_PROTECTED')
}

function checkIsTotalPortalFailure(
  data: DashboardResponseDto | null | undefined,
  failureCount: number,
): boolean {
  if (!data) return false
  if (data.executionStatus === 'TOTAL_FAILURE') return true
  const count = data.portalStatuses?.length ?? 0
  return count > 0 && failureCount === count
}

export const App: FC = () => {
  const {
    data,
    items,
    rawItems,
    rawCount,
    archiveItems,
    rawArchiveItems,
    isArchiveLoading,
    favoriteItems,
    rawFavoriteItems,
    favoritesCount,
    isLoading,
    isError,
    error,
    search,
    markAsViewed,
    clearArchiveHistory,
    isClearingHistory,
    hiddenCount,
    toastMessage,
    toastUndoItem,
    clearToast,
    hideProperty,
    restoreProperty,
    openAllTabs,
  } = useSearch()
  const { setCriteria } = useFilterStore()
  const {
    dashboardTab,
    setDashboardTab,
    listingSubView,
    setListingSubView,
    setDiagnosticsModalOpen,
  } = useUiStore()
  const initialLoadDone = useRef(false)

  const partialPortalFailures = useMemo(
    () => extractPortalFailures(data?.portalStatuses),
    [data?.portalStatuses],
  )
  const isTotalPortalFailure = checkIsTotalPortalFailure(data, partialPortalFailures.length)

  // Load last saved criteria from backend on first mount
  useEffect(() => {
    if (initialLoadDone.current) return
    initialLoadDone.current = true

    // Check if URL has params; if not, load from backend config
    if (!window.location.search) {
      getLastConfig()
        .then((saved) => {
          if (saved) {
            setCriteria(saved)
          }
        })
        .catch(() => {
          // ignore error, defaults already set in store
        })
    }
  }, [setCriteria])

  const handleSearch = () => {
    if (dashboardTab !== 'listings') {
      setDashboardTab('listings')
    }
    if (listingSubView !== 'listings') {
      setListingSubView('listings')
    }
    search()
  }

  // Active listings data slice based on current listing sub-view
  const { activeItems, activeRawItems, activeRawCount, activeLoading } = resolveActiveListingsSlice({
    listingSubView,
    items,
    rawItems,
    rawCount,
    favoriteItems,
    rawFavoriteItems,
    archiveItems,
    rawArchiveItems,
    isLoading,
    isArchiveLoading,
  })

  return (
    <div className="min-h-screen bg-[var(--bg-body)] text-[var(--text-primary)] transition-colors duration-200">
      {/* Sticky Header */}
      <Header />

      {/* Main Container */}
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 space-y-6">
        {/* Filters and search controls */}
        <SearchPanel onSearch={handleSearch} isLoading={isLoading} />

        {/* API Error Alert Banner */}
        {isError && <SearchErrorBanner error={error} onRetry={handleSearch} />}

        {/* Partial or Total Portal Warning Banner */}
        {partialPortalFailures.length > 0 && dashboardTab === 'listings' && listingSubView === 'listings' && (
          <PartialPortalWarningBanner
            failures={partialPortalFailures}
            isTotalFailure={isTotalPortalFailure}
            onOpenDiagnostics={() => setDiagnosticsModalOpen(true)}
          />
        )}

        {/* Market stats bar */}
        <MarketStatsBar data={data} />

        {/* Dashboard View Switcher & Listings Header Controls (#157, #227, #264) */}
        <div
          data-testid="listings-panel-header"
          className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border-color)] pb-3"
        >
          {/* Left: View Navigation Tabs (R1: strictly 2 primary tabs) */}
          <nav aria-label="Fő nézetek" className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              data-testid="tab-listings"
              aria-pressed={dashboardTab === 'listings'}
              onClick={() => setDashboardTab('listings')}
              className={cn(
                'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all',
                dashboardTab === 'listings'
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
              )}
            >
              <LayoutGrid className="h-4 w-4" />
              <span>Találatok</span>
            </button>

            <button
              type="button"
              data-testid="tab-analytics"
              aria-pressed={dashboardTab === 'analytics'}
              onClick={() => setDashboardTab('analytics')}
              className={cn(
                'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all',
                dashboardTab === 'analytics'
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                  : 'bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
              )}
            >
              <BarChart2 className="h-4 w-4" />
              <span>Piaci Elemzés</span>
            </button>
          </nav>
        </div>

        {/* Decoupled Panel Routing (#264) */}
        {dashboardTab === 'listings' ? (
          <ListingsPanel
            items={activeItems}
            rawItems={activeRawItems}
            totalRawCount={activeRawCount}
            isLoading={activeLoading}
            hasSearched={listingSubView === 'listings' ? data !== null && data !== undefined : true}
            mode={listingSubView}
            subView={listingSubView}
            onSubViewChange={setListingSubView}
            searchCount={rawCount}
            favoritesCount={favoritesCount}
            archiveCount={rawArchiveItems.length}
            onMarkViewed={markAsViewed}
            onOpenAll={openAllTabs}
            onTriggerSearch={handleSearch}
            onClearHistory={
              listingSubView === 'archive'
                ? () => {
                    void clearArchiveHistory()
                  }
                : undefined
            }
isClearingHistory={isClearingHistory}
            onHide={hideProperty}
            onRestore={restoreProperty}
            hiddenCount={hiddenCount}
          />
        ) : (
          <Suspense fallback={<div className="min-h-64 animate-pulse rounded-2xl bg-[var(--bg-card)]" />}>
            <MarketAnalyticsPanel
              items={items}
              onMarkViewed={markAsViewed}
              onOpenListing={(url) => {
                openExternalUrl(url)
                markAsViewed(url)
              }}
            />
          </Suspense>
        )}
      </main>

{/* Toast Notification */}
      {toastMessage && (
        <aside
          role="status"
          aria-live="polite"
          data-testid="undo-hide-toast"
          className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] px-4 py-3 shadow-xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-5 duration-200"
        >
          <span className="text-xs font-semibold text-[var(--text-primary)]">{toastMessage}</span>
          {toastUndoItem && (
            <button
              type="button"
              onClick={() => {
                restoreProperty(toastUndoItem)
                clearToast()
              }}
              className="rounded-lg bg-blue-600 px-2.5 py-1 text-xs font-bold text-white hover:bg-blue-500 transition-colors cursor-pointer"
            >
              Visszavonás
            </button>
          )}
          <button
            type="button"
            onClick={clearToast}
            aria-label="Bezárás"
            className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs"
          >
            ✕
          </button>
        </aside>
      )}

      {/* Modals */}
      <DiagnosticsModal portalStatuses={data?.portalStatuses} />
    </div>
  )
}

export default App
