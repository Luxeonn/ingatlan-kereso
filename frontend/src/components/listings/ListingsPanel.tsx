import type { FC, ReactNode } from 'react'
import { LayoutGrid, Star } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { ListingsGrid } from './ListingsGrid'
import type { ListingSubView } from '../../store/useUiStore'
import { useUiStore } from '../../store/useUiStore'
import { formatPricePerSqm, getTotalSourceListingsCount } from '../../utils/formatters'
import { cn } from '../../utils/cn'

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
  archiveCount?: number
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

interface MultiSourceNoticeProps {
  itemsCount: number
  totalSourceListingsCount: number
  serverFilteredCount?: number
  medianPricePerSqm?: number | null
}

const MultiSourceNotice: FC<MultiSourceNoticeProps> = ({
  itemsCount,
  totalSourceListingsCount,
  serverFilteredCount,
  medianPricePerSqm,
}) => {
  const mergedDuplicatesCount = Math.max(0, totalSourceListingsCount - itemsCount)
  const hasFiltered = serverFilteredCount !== undefined && serverFilteredCount > 0
  const hasMedian = medianPricePerSqm !== null && medianPricePerSqm !== undefined && medianPricePerSqm > 0
  if (mergedDuplicatesCount === 0 && !hasFiltered && !hasMedian) return null

  return (
    <div
      data-testid="multi-source-summary-notice"
      className="flex flex-1 min-w-[260px] flex-wrap items-center justify-center text-center gap-1.5 rounded-lg border border-indigo-500/30 bg-indigo-500/10 px-3 py-1.5 text-xs text-indigo-700 tabular-nums"
    >
      <span>
        <strong className="font-bold text-indigo-700">{itemsCount} egyedi ingatlan</strong>
        {mergedDuplicatesCount > 0 && (
          <> ({totalSourceListingsCount} hirdetésből · {mergedDuplicatesCount} összevonva)</>
        )}
        {hasFiltered && (
          <> · <strong className="font-bold text-indigo-700">{serverFilteredCount} szűrt</strong></>
        )}
        {hasMedian && (
          <> · Medián m² ár: <strong className="font-bold text-indigo-700">{formatPricePerSqm(medianPricePerSqm)}</strong></>
        )}
      </span>
    </div>
  )
}

interface ListingsSubViewNavProps {
  activeMode: ListingSubView
  activeSearchCount: number
  activeFavoritesCount: number
  activeArchiveCount?: number
  onSubViewChange: (newSubView: ListingSubView) => void
  summaryNotice?: ReactNode
}

const ListingsSubViewNav: FC<ListingsSubViewNavProps> = ({
  activeMode,
  activeSearchCount,
  activeFavoritesCount,
  onSubViewChange,
  summaryNotice,
}) => (
  <nav
    data-testid="listings-subview-nav"
    aria-label="Hirdetés nézetek"
    className="flex flex-wrap items-center justify-center gap-3 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-body)]/70 p-2.5 shadow-xs"
  >
    <div className="flex flex-1 min-w-[280px] flex-wrap items-center gap-2">
      <button
        type="button"
        data-testid="subview-listings"
        aria-pressed={activeMode === 'listings'}
        onClick={() => onSubViewChange('listings')}
        className={cn(
          'flex flex-1 min-w-[130px] items-center justify-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer',
          activeMode === 'listings'
            ? 'bg-blue-600 text-white shadow-sm'
            : 'border border-[var(--border-hairline)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-blue-500/40 hover:text-blue-700',
        )}
      >
        <LayoutGrid className="h-3.5 w-3.5" />
        <span>Hirdetések ({activeSearchCount})</span>
      </button>

      <button
        type="button"
        data-testid="subview-favorites"
        aria-pressed={activeMode === 'favorites'}
        onClick={() => onSubViewChange('favorites')}
        className={cn(
          'flex flex-1 min-w-[130px] items-center justify-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-bold transition-all cursor-pointer',
          activeMode === 'favorites'
            ? 'bg-amber-600 text-white shadow-sm'
            : 'border border-[var(--border-hairline)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-amber-500/40 hover:text-amber-700',
        )}
      >
        <Star className="h-3.5 w-3.5" />
        <span>⭐ Kedvencek ({activeFavoritesCount})</span>
      </button>
    </div>

    {summaryNotice}
  </nav>
)

function resolveSubViewCounts(
  activeMode: ListingSubView,
  totalRawCount: number,
  searchCount?: number,
  favoritesCount?: number,
  archiveCount?: number,
) {
  return {
    activeSearchCount: searchCount ?? (activeMode === 'listings' ? totalRawCount : 0),
    activeFavoritesCount: favoritesCount ?? (activeMode === 'favorites' ? totalRawCount : 0),
    activeArchiveCount: archiveCount ?? (activeMode === 'archive' ? totalRawCount : 0),
  }
}

export const ListingsPanel: FC<ListingsPanelProps> = ({
  items,
  rawItems,
  totalRawCount,
  serverFilteredCount,
  medianPricePerSqm,
  sessionViewedUrls,
  isLoading,
  hasSearched,
  mode,
  subView,
  onSubViewChange,
  searchCount,
  favoritesCount,
  archiveCount,
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
  const { listingSubView: storeSubView, setListingSubView } = useUiStore()
  const activeMode: ListingSubView = subView ?? mode ?? storeSubView ?? 'listings'

  const handleSubViewChange = (newSubView: ListingSubView) => {
    setListingSubView(newSubView)
    onSubViewChange?.(newSubView)
  }

  const { activeSearchCount, activeFavoritesCount, activeArchiveCount } = resolveSubViewCounts(
    activeMode,
    totalRawCount,
    searchCount,
    favoritesCount,
    archiveCount,
  )

  const baseSummaryItems = rawItems ?? items
  const totalSourceListingsCount = getTotalSourceListingsCount(baseSummaryItems)

  const summaryNotice =
    activeMode === 'listings' ? (
      <MultiSourceNotice
        itemsCount={baseSummaryItems.length}
        totalSourceListingsCount={totalSourceListingsCount}
        serverFilteredCount={serverFilteredCount}
        medianPricePerSqm={medianPricePerSqm}
      />
    ) : null

  return (
    <section className="space-y-4" data-testid="listings-panel">
      {/* Sub-view navigation toolbar (hidden in Archive & Trash tab) */}
      {activeMode !== 'archive' && (
        <ListingsSubViewNav
          activeMode={activeMode}
          activeSearchCount={activeSearchCount}
          activeFavoritesCount={activeFavoritesCount}
          activeArchiveCount={activeArchiveCount}
          onSubViewChange={handleSubViewChange}
          summaryNotice={summaryNotice}
        />
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
