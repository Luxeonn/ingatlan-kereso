import { useEffect, useRef } from 'react'
import { Header } from './components/layout/Header'
import { SearchPanel } from './components/filters/SearchPanel'
import { MarketStatsBar } from './components/kpi/MarketStatsBar'
import { ListingsGrid } from './components/listings/ListingsGrid'
import { LogViewerModal } from './components/modals/LogViewerModal'
import { DiagnosticsModal } from './components/modals/DiagnosticsModal'
import { useSearch } from './hooks/useSearch'
import { useFilterStore } from './store/useFilterStore'
import { getLastConfig } from './api/searchApi'

export const App: React.FC = () => {
  const { data, items, rawCount, isLoading, search, markAsViewed, openAllTabs } = useSearch()
  const { setCriteria } = useFilterStore()
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

        {/* Listings section */}
        <ListingsGrid
          items={items}
          totalRawCount={rawCount}
          isLoading={isLoading}
          hasSearched={data != null}
          onMarkViewed={markAsViewed}
          onOpenAll={openAllTabs}
          onTriggerSearch={handleSearch}
        />
      </main>

      {/* Modals */}
      <LogViewerModal />
      <DiagnosticsModal portalStatuses={data?.portalStatuses} />
    </div>
  )
}

export default App
