import { useEffect, useRef, FC } from 'react'
import { LayoutGrid, BarChart2 } from 'lucide-react'
import { Header } from './components/layout/Header'
import { SearchPanel } from './components/filters/SearchPanel'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsGrid } from './components/listings/ListingsGrid'
import { MarketAnalyticsDashboard } from './components/analytics/MarketAnalyticsDashboard'
import { DiagnosticsModal } from './components/modals/DiagnosticsModal'
import { useSearch } from './hooks/useSearch'
import { useFilterStore } from './store/useFilterStore'
import { useUiStore } from './store/useUiStore'
import { getLastConfig } from './api/searchApi'

export const App: FC = () => {
  const { data, items, rawCount, isLoading, search, markAsViewed, openAllTabs } = useSearch()
  const { setCriteria } = useFilterStore()
  const { dashboardTab, setDashboardTab } = useUiStore()
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
    search()
  }

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

        {/* Dashboard View Switcher */}
        <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setDashboardTab('listings')}
              className={`flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all ${
                dashboardTab === 'listings'
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
              }`}
            >
              <LayoutGrid className="h-4 w-4" />
              <span>Hirdetések ({items.length})</span>
            </button>
            <button
              type="button"
              onClick={() => setDashboardTab('analytics')}
              className={`flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all ${
                dashboardTab === 'analytics'
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20'
                  : 'bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
              }`}
            >
              <BarChart2 className="h-4 w-4" />
              <span>Piaci Elemzés és Árdiagramok</span>
            </button>
          </div>
        </div>

        {/* Dynamic View: Listings or Analytics */}
        {dashboardTab === 'listings' ? (
          <ListingsGrid
            items={items}
            totalRawCount={rawCount}
            isLoading={isLoading}
            hasSearched={data != null}
            onMarkViewed={markAsViewed}
            onOpenAll={openAllTabs}
            onTriggerSearch={handleSearch}
          />
        ) : (
          <MarketAnalyticsDashboard
            items={items}
            onMarkViewed={markAsViewed}
            onOpenListing={(url) => {
              window.open(url, '_blank', 'noopener,noreferrer')
              markAsViewed(url)
            }}
          />
        )}
      </main>

      {/* Modals */}
      <DiagnosticsModal portalStatuses={data?.portalStatuses} />
    </div>
  )
}

export default App
