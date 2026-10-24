import type { FC } from 'react'
import type { IngatlanItem } from '../../types/property'
import { ListingsGrid } from './ListingsGrid'

export interface ListingsPanelProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  isLoading: boolean
  hasSearched: boolean
  mode?: 'listings' | 'favorites' | 'archive'
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
  mode = 'listings',
  onMarkViewed,
  onOpenAll,
  onTriggerSearch,
  onClearHistory,
  isClearingHistory = false,
}) => {
  // Multi-source grouped count calculation (#260, #261)
  const multiSourceCount = items.filter(
    (it) => (it.sourcesCount != null && it.sourcesCount > 1) || (it.sourceListings && it.sourceListings.length > 1)
  ).length

  return (
    <section className="space-y-4" data-testid="listings-panel">
      {/* Multi-source clustering summary notice if present in listings mode */}
      {mode === 'listings' && multiSourceCount > 0 && (
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
        mode={mode}
        onMarkViewed={onMarkViewed}
        onOpenAll={onOpenAll}
        onTriggerSearch={onTriggerSearch}
        onClearHistory={onClearHistory}
        isClearingHistory={isClearingHistory}
      />
    </section>
  )
}
