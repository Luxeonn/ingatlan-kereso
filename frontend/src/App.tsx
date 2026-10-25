import { lazy, Suspense, useEffect, useRef, type FC } from 'react'
import { LayoutGrid, BarChart2, EyeOff, ArrowUpDown } from 'lucide-react'
import { Header } from './components/layout/Header'
import { SearchPanel } from './components/filters/SearchPanel'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsPanel } from './components/listings/ListingsPanel'
import { DiagnosticsModal } from './components/modals/DiagnosticsModal'
import { useSearch } from './hooks/useSearch'
import { useFilterStore, SortOption } from './store/useFilterStore'
import { useUiStore } from './store/useUiStore'
import { getLastConfig } from './api/searchApi'
import { cn } from './utils/cn'

const MarketAnalyticsPanel = lazy(() =>
  import('./components/analytics/MarketAnalyticsPanel').then((module) => ({
    default: module.MarketAnalyticsPanel,
  })),
)

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
    search,
    markAsViewed,
    clearArchiveHistory,
    isClearingHistory,
    openAllTabs,
  } = useSearch()
  const { criteria, sortOption, setCriteria, setExcludeViewed, setSortOption } = useFilterStore()
  const { dashboardTab, setDashboardTab, listingSubView, setListingSubView } = useUiStore()
  const initialLoadDone = useRef(false)

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
  const activeItems =
    listingSubView === 'favorites'
      ? favoriteItems
      : listingSubView === 'archive'
        ? archiveItems
        : items

  const activeRawItems =
    listingSubView === 'favorites'
      ? rawFavoriteItems
      : listingSubView === 'archive'
        ? rawArchiveItems
        : rawItems

  const activeRawCount =
    listingSubView === 'favorites'
      ? rawFavoriteItems.length
      : listingSubView === 'archive'
        ? rawArchiveItems.length
        : rawCount

  const activeLoading =
    listingSubView === 'archive'
      ? isArchiveLoading
      : listingSubView === 'favorites'
        ? false
        : isLoading

  return (
    <div className="min-h-screen bg-[var(--bg-body)] text-[var(--text-primary)] transition-colors duration-200">
      {/* Sticky Header */}
      <Header />

      {/* Main Container */}
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 space-y-6">
        {/* Filters and search controls */}
        <SearchPanel onSearch={handleSearch} isLoading={isLoading} />

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

          {/* Right: Exclude Viewed toggle & Sort selector (#227) */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <button
              type="button"
              onClick={() => setExcludeViewed(!criteria.excludeViewed)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
                criteria.excludeViewed
                  ? 'border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm'
                  : 'border border-[var(--border-color)] bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              )}
            >
              <EyeOff className="h-3.5 w-3.5 text-blue-400" />
              <span>Megtekintettek rejtése</span>
            </button>

            <div className="flex items-center gap-1.5">
              <ArrowUpDown className="h-3.5 w-3.5 text-[var(--text-secondary)] shrink-0" />
              <label htmlFor="listings-sort-select" className="text-[var(--text-secondary)] font-medium">
                Rendezés:
              </label>
              <select
                id="listings-sort-select"
                aria-label="Rendezés"
                value={sortOption}
                onChange={(e) => setSortOption(e.target.value as SortOption)}
                className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-2.5 py-1.5 text-xs font-semibold text-[var(--text-primary)] focus:border-blue-500 focus:outline-none"
              >
                <option value="price_asc">Ár: Növekvő (alapértelmezett)</option>
                <option value="price_desc">Ár: Csökkenő</option>
                <option value="sqm_price_asc">Ft/m²: Növekvő</option>
                <option value="sqm_price_desc">Ft/m²: Csökkenő</option>
                <option value="area_desc">Méret: Legnagyobb elöl</option>
                <option value="date_desc">Felfedezés: Legfrissebb</option>
              </select>
            </div>
          </div>
        </div>

        {/* Decoupled Panel Routing (#264) */}
        {dashboardTab === 'listings' ? (
          <ListingsPanel
            items={activeItems}
            rawItems={activeRawItems}
            totalRawCount={activeRawCount}
            isLoading={activeLoading}
            hasSearched={listingSubView === 'listings' ? data != null : true}
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
          />
        ) : (
          <Suspense fallback={<div className="min-h-64 animate-pulse rounded-2xl bg-[var(--bg-card)]" />}>
            <MarketAnalyticsPanel
              items={items}
              onMarkViewed={markAsViewed}
              onOpenListing={(url) => {
                window.open(url, '_blank', 'noopener,noreferrer')
                markAsViewed(url)
              }}
            />
          </Suspense>
        )}
      </main>

      {/* Modals */}
      <DiagnosticsModal portalStatuses={data?.portalStatuses} />
    </div>
  )
}

export default App
