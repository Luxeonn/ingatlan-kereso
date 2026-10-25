import type { FC } from 'react'
import { LayoutGrid, Star, Archive } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { ListingsGrid } from './ListingsGrid'
import { useUiStore, ListingSubView } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

export interface ListingsPanelProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  isLoading: boolean
  hasSearched: boolean
  mode?: ListingSubView
  subView?: ListingSubView
  onSubViewChange?: (subView: ListingSubView) => void
  searchCount?: number
  favoritesCount?: number
  archiveCount?: number
  onMarkViewed: (url: string) => void
  onOpenAll: (items: IngatlanItem[]) => void
  onTriggerSearch: () => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
}

export const ListingsPanel: FC<ListingsPanelProps> = ({
  items,
  rawItems,
  totalRawCount,
  isLoading,
  hasSearched,
  mode,
  subView,
  onSubViewChange,
  searchCount,
  favoritesCount,
  archiveCount,
  onMarkViewed,
  onOpenAll,
  onTriggerSearch,
  onClearHistory,
  isClearingHistory = false,
}) => {
  const { listingSubView: storeSubView, setListingSubView } = useUiStore()
  const activeMode: ListingSubView = subView ?? mode ?? storeSubView ?? 'listings'

  const handleSubViewChange = (newSubView: ListingSubView) => {
    setListingSubView(newSubView)
    onSubViewChange?.(newSubView)
  }

  const activeSearchCount = searchCount ?? (activeMode === 'listings' ? totalRawCount : 0)
  const activeFavoritesCount = favoritesCount ?? (activeMode === 'favorites' ? totalRawCount : 0)
  const activeArchiveCount = archiveCount ?? (activeMode === 'archive' ? totalRawCount : 0)

  // Multi-source grouped count calculation (#260, #261)
  const multiSourceCount = items.filter(
    (it) => (it.sourcesCount != null && it.sourcesCount > 1) || (it.sourceListings && it.sourceListings.length > 1)
  ).length

  return (
    <section className="space-y-4" data-testid="listings-panel">
      {/* Sub-view navigation toolbar (#R2) */}
      <nav
        data-testid="listings-subview-nav"
        aria-label="Hirdetés nézetek"
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-2.5 shadow-sm"
      >
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            data-testid="subview-listings"
            aria-pressed={activeMode === 'listings'}
            onClick={() => handleSubViewChange('listings')}
            className={cn(
              'flex items-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all',
              activeMode === 'listings'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
            )}
          >
            <LayoutGrid className="h-3.5 w-3.5" />
            <span>Hirdetések ({activeSearchCount})</span>
          </button>

          <button
            type="button"
            data-testid="subview-favorites"
            aria-pressed={activeMode === 'favorites'}
            onClick={() => handleSubViewChange('favorites')}
            className={cn(
              'flex items-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all',
              activeMode === 'favorites'
                ? 'bg-amber-600 text-white shadow-sm'
                : 'bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
            )}
          >
            <Star className="h-3.5 w-3.5" />
            <span>⭐ Kedvencek ({activeFavoritesCount})</span>
          </button>

          <button
            type="button"
            data-testid="subview-archive"
            aria-pressed={activeMode === 'archive'}
            onClick={() => handleSubViewChange('archive')}
            className={cn(
              'flex items-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all',
              activeMode === 'archive'
                ? 'bg-purple-600 text-white shadow-sm'
                : 'bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-color)]'
            )}
          >
            <Archive className="h-3.5 w-3.5" />
            <span>Archívum ({activeArchiveCount})</span>
          </button>
        </div>
      </nav>

      {/* Multi-source clustering summary notice if present in listings mode */}
      {activeMode === 'listings' && multiSourceCount > 0 && (
        <div
          data-testid="multi-source-summary-notice"
          className="flex items-center justify-between rounded-xl border border-indigo-500/30 bg-indigo-500/10 px-4 py-2 text-xs text-indigo-300"
        >
          <span>
            <strong className="font-bold text-indigo-200">{items.length} db</strong> ingatlan jelenik meg{' '}
            (ebből <strong className="font-bold text-indigo-200">{multiSourceCount} db</strong> több portálon is szereplő, csoportosított hirdetés).
          </span>
        </div>
      )}

      {/* Screen reader live region for results announcements */}
      <div className="sr-only" aria-live="polite">
        {isLoading
          ? 'Hirdetések betöltése folyamatban...'
          : `${items.length} hirdetés jelenik meg a listában.`}
      </div>

      {/* Virtualized Listings Grid */}
      <ListingsGrid
        items={items}
        rawItems={rawItems}
        totalRawCount={totalRawCount}
        isLoading={isLoading}
        hasSearched={hasSearched}
        mode={activeMode}
        onMarkViewed={onMarkViewed}
        onOpenAll={onOpenAll}
        onTriggerSearch={onTriggerSearch}
        onClearHistory={onClearHistory}
        isClearingHistory={isClearingHistory}
      />
    </section>
  )
}
