import type { FC } from 'react'
import type { IngatlanItem } from '../../types/property'
import { ListingsGrid } from './ListingsGrid'
import type { ListingSubView } from '../../store/useUiStore'
import { useUiStore } from '../../store/useUiStore'

export interface ListingsPanelProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  serverFilteredCount?: number
  medianPricePerSqm?: number | null
  sessionViewedUrls?: ReadonlySet<string>
  isLoading: boolean
  hasSearched: boolean
  mode?: ListingSubView
  subView?: ListingSubView
  onSubViewChange?: (subView: ListingSubView) => void
  searchCount?: number
  favoritesCount?: number
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  hiddenCount?: number
  onTriggerSearch: () => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
  onMergeSelected?: (urls: string[]) => Promise<boolean | void> | void
  isMerging?: boolean
  initialBatchSize?: number
  batchSize?: number
}

export const ListingsPanel: FC<ListingsPanelProps> = ({
  items,
  rawItems,
  totalRawCount,
  serverFilteredCount: _serverFilteredCount,
  medianPricePerSqm: _medianPricePerSqm,
  sessionViewedUrls,
  isLoading,
  hasSearched,
  mode,
  subView,
  onSubViewChange: _onSubViewChange,
  searchCount: _searchCount,
  favoritesCount: _favoritesCount,
  onMarkViewed,
  onHide,
  onRestore,
  hiddenCount,
  onTriggerSearch,
  onClearHistory,
  isClearingHistory = false,
  onSplitSource,
  onUndoOverride,
  onMergeSelected,
  isMerging = false,
  initialBatchSize,
  batchSize,
}) => {
  const { listingSubView: storeSubView } = useUiStore()
  const activeMode: ListingSubView = subView ?? mode ?? storeSubView ?? 'listings'

  return (
    <section className="space-y-4" data-testid="listings-panel">
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
        sessionViewedUrls={sessionViewedUrls}
        isLoading={isLoading}
        hasSearched={hasSearched}
        mode={activeMode}
        onMarkViewed={onMarkViewed}
        onHide={onHide}
        onRestore={onRestore}
        hiddenCount={hiddenCount}
        onTriggerSearch={onTriggerSearch}
        onClearHistory={onClearHistory}
        isClearingHistory={isClearingHistory}
        onSplitSource={onSplitSource}
        onUndoOverride={onUndoOverride}
        onMergeSelected={onMergeSelected}
        isMerging={isMerging}
        initialBatchSize={initialBatchSize}
        batchSize={batchSize}
      />
    </section>
  )
}
