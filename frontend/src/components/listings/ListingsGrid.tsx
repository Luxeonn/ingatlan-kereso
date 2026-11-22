import { useCallback, useEffect, useMemo, useRef, useState, type FC, type ReactNode } from 'react'
import { useWindowVirtualizer } from '@tanstack/react-virtual'
import { Inbox, Search, Star, Archive } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { PropertyCard } from './PropertyCard'
import { ListingsTableView } from './ListingsTableView'
import { ListingSkeleton } from './ListingSkeleton'
import { BulkActionBar } from './BulkActionBar'
import { useUiStore, type ViewMode } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface ListingsGridProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  isLoading: boolean
  hasSearched: boolean
  mode?: 'listings' | 'favorites' | 'archive'
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  hiddenCount?: number
  onOpenAll: (items: IngatlanItem[]) => void
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

const DEFAULT_INITIAL_BATCH_SIZE = 18
const DEFAULT_BATCH_STEP = 18

function calculateColumnCount(viewMode: string, width: number): number {
  if (viewMode === 'list') return 1
  if (width >= 1024) return 3
  if (width >= 640) return 2
  return 1
}

function renderEmptyState(
  mode: 'listings' | 'favorites' | 'archive',
  hasSearched: boolean,
  totalRawCount: number,
  onTriggerSearch: () => void,
): ReactNode {
  if (mode === 'listings' && !hasSearched) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--border-color)] bg-[var(--bg-card)]/50 p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-400 mb-4">
          <Search className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">Készen állsz a keresésre?</h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Állítsd be a kívánt településeket, árhatárokat és portálokat, majd kattints az Ingatlanok Keresése gombra!
        </p>
        <button
          type="button"
          onClick={onTriggerSearch}
          className="mt-5 rounded-xl bg-blue-600 px-5 py-2 text-xs font-bold text-white shadow-lg shadow-blue-500/25 transition-all hover:bg-blue-500"
        >
          Keresés indítása
        </button>
      </div>
    )
  }
  if (mode === 'favorites' && totalRawCount === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-400 mb-4">
          <Star className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Még nincsenek kedvencnek jelölt ingatlanok
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Kattints a kártyák jobb felső sarkában található csillag ikonra a hirdetések elmentéséhez!
        </p>
      </div>
    )
  }
  if (mode === 'archive' && totalRawCount === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-purple-500/10 text-purple-400 mb-4">
          <Archive className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">Az archívum jelenleg üres</h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          A korábbi keresések során megtalált és mentett hirdetések automatikusan itt jelennek meg.
        </p>
      </div>
    )
  }
  return null
}

function buildItemsSignature(items: IngatlanItem[]): string {
  return `${items.length}:${items.map((item) => item.url || item.id || '').join('\0')}`
}

function hasSignificantUrlOverlap(prevUrls: string[], currentUrls: string[]): boolean {
  if (prevUrls.length === 0 || currentUrls.length === 0) return false
  const prevSet = new Set(prevUrls)
  let shared = 0
  for (const url of currentUrls) {
    if (prevSet.has(url)) shared += 1
  }
  return shared / Math.max(prevUrls.length, currentUrls.length) >= 0.5
}

interface ResetToken {
  itemsSignature: string
  itemUrls: string[]
  mode?: string
  initialBatchSize: number
}

function shouldResetBatchToken(prev: ResetToken, current: ResetToken): 'none' | 'preserve' | 'reset' {
  if (prev.mode !== current.mode || prev.initialBatchSize !== current.initialBatchSize) return 'reset'
  if (prev.itemsSignature === current.itemsSignature) return 'none'
  return hasSignificantUrlOverlap(prev.itemUrls, current.itemUrls) ? 'preserve' : 'reset'
}

interface ListingsViewportBodyProps {
  viewMode: ViewMode
  visibleItems: IngatlanItem[]
  totalItemsCount: number
  hasMore: boolean
  columns: number
  scrollMargin: number
  listRef: React.RefObject<HTMLDivElement | null>
  sentinelRef: React.RefObject<HTMLButtonElement | null>
  onLoadMore: () => void
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
}

const ListingsViewportBody: FC<ListingsViewportBodyProps> = (props) => {
  const { viewMode, visibleItems, totalItemsCount, hasMore, columns, scrollMargin, listRef, sentinelRef, onLoadMore } = props
  const rowCount = Math.ceil(visibleItems.length / columns)
  const virtualizer = useWindowVirtualizer({
    count: rowCount,
    estimateSize: () => (viewMode === 'list' ? 160 : 320),
    overscan: 1,
    scrollMargin,
  })

  return (
    <div className="space-y-4">
      {viewMode === 'table' ? (
        <ListingsTableView
          items={visibleItems}
          onMarkViewed={props.onMarkViewed}
          onHide={props.onHide}
          onRestore={props.onRestore}
          onSplitSource={props.onSplitSource}
          onUndoOverride={props.onUndoOverride}
        />
      ) : (
        <div ref={listRef} className="relative w-full" style={{ height: virtualizer.getTotalSize() }}>
          {virtualizer.getVirtualItems().map((virtualRow) => {
            const rowItems = visibleItems.slice(virtualRow.index * columns, (virtualRow.index + 1) * columns)
            return (
              <div
                key={virtualRow.key}
                ref={virtualizer.measureElement}
                data-index={virtualRow.index}
                className={cn(
                  viewMode === 'grid' ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3' : 'flex flex-col gap-3',
                )}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  transform: `translateY(${virtualRow.start - scrollMargin}px)`,
                }}
              >
                {rowItems.map((item) => (
                  <PropertyCard
                    key={item.url || item.id}
                    item={item}
                    onMarkViewed={props.onMarkViewed}
                    onHide={props.onHide}
                    onRestore={props.onRestore}
                    onSplitSource={props.onSplitSource}
                    onUndoOverride={props.onUndoOverride}
                    viewMode={viewMode}
                  />
                ))}
              </div>
            )
          })}
        </div>
      )}

      {hasMore && (
        <button
          type="button"
          ref={sentinelRef}
          data-testid="infinite-scroll-sentinel"
          onClick={onLoadMore}
          className="flex w-full items-center justify-center py-6 text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
            <span>További ingatlanok betöltése... ({visibleItems.length} / {totalItemsCount} db)</span>
          </div>
        </button>
      )}
    </div>
  )
}

export const ListingsGrid: FC<ListingsGridProps> = ({
  items,
  rawItems,
  totalRawCount,
  isLoading,
  hasSearched,
  mode = 'listings',
  onMarkViewed,
  onHide,
  onRestore,
  hiddenCount = 0,
  onOpenAll,
  onTriggerSearch,
  onClearHistory,
  isClearingHistory = false,
  onSplitSource,
  onUndoOverride,
  onMergeSelected,
  isMerging = false,
  initialBatchSize = DEFAULT_INITIAL_BATCH_SIZE,
  batchSize = DEFAULT_BATCH_STEP,
}) => {
  const { viewMode } = useUiStore()
  const itemsSignature = useMemo(() => buildItemsSignature(items), [items])
  const itemUrls = useMemo(() => items.map((item) => item.url || item.id || ''), [items])
  const initialLimit = items.length <= initialBatchSize + 3 ? items.length : initialBatchSize
  const [columns, setColumns] = useState(1)
  const [scrollMargin, setScrollMargin] = useState(0)
  const [visibleLimit, setVisibleLimit] = useState(initialLimit)
  const [prevResetToken, setPrevResetToken] = useState<ResetToken>(() => ({
    itemsSignature,
    itemUrls,
    mode,
    initialBatchSize,
  }))
  const listRef = useRef<HTMLDivElement>(null)
  const sentinelRef = useRef<HTMLButtonElement>(null)

  const resetAction = shouldResetBatchToken(prevResetToken, { itemsSignature, itemUrls, mode, initialBatchSize })
  if (resetAction !== 'none') {
    setPrevResetToken({ itemsSignature, itemUrls, mode, initialBatchSize })
    setVisibleLimit((prev) =>
      resetAction === 'reset' ? initialLimit : Math.min(Math.max(prev, initialLimit), items.length),
    )
  }

  const handleLoadMore = useCallback(() => {
    setVisibleLimit((prev) => Math.min(prev + batchSize, items.length))
  }, [batchSize, items.length])

  const hasMore = visibleLimit < items.length

  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel || !hasMore) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) handleLoadMore()
      },
      { rootMargin: '400px 0px' },
    )
    observer.observe(sentinel)

    if (typeof window !== 'undefined' && sentinel.getBoundingClientRect) {
      const rect = sentinel.getBoundingClientRect()
      if (rect.bottom > 0 && rect.top <= window.innerHeight + 400) handleLoadMore()
    }
    return () => observer.disconnect()
  }, [hasMore, visibleLimit, handleLoadMore])

  const visibleItems = items.slice(0, visibleLimit)

  useEffect(() => {
    const updateColumns = () => setColumns(calculateColumnCount(viewMode, window.innerWidth))
    updateColumns()
    window.addEventListener('resize', updateColumns)
    return () => window.removeEventListener('resize', updateColumns)
  }, [viewMode])

  useEffect(() => {
    const updateScrollMargin = () => {
      if (listRef.current) setScrollMargin(listRef.current.getBoundingClientRect().top + window.scrollY)
    }
    updateScrollMargin()
    window.addEventListener('resize', updateScrollMargin)
    return () => window.removeEventListener('resize', updateScrollMargin)
  }, [hasSearched, visibleItems.length, mode])

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-10 w-48 rounded-xl bg-[var(--bg-card)] animate-shimmer" />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <ListingSkeleton key={i} />
          ))}
        </div>
      </div>
    )
  }

  const emptyState = renderEmptyState(mode, hasSearched, totalRawCount, onTriggerSearch)
  if (emptyState) return emptyState

  return (
    <div className="space-y-4">
      <BulkActionBar
        items={items}
        rawItems={rawItems}
        totalRawCount={totalRawCount}
        onOpenAll={onOpenAll}
        onMarkAllViewed={(allItems) => allItems.forEach((i) => onMarkViewed(i.url))}
        onClearHistory={mode === 'archive' ? onClearHistory : undefined}
        isClearingHistory={isClearingHistory}
        onMergeSelected={onMergeSelected}
        isMerging={isMerging}
        visibleCount={visibleItems.length}
        hiddenCount={hiddenCount}
      />

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-400 mb-4">
            <Inbox className="h-8 w-8" />
          </div>
          <h3 className="text-base font-bold text-[var(--text-primary)]">
            Nem található ingatlan a megadott feltételekkel
          </h3>
          <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
            Próbáld meg módosítani a gyorskereső kifejezést, a portál szűrőt, vagy kikapcsolni az aktív szűrőket (Alkalmi vétel, Minőség, Megtekintettek rejtése)!
          </p>
        </div>
      ) : (
        <ListingsViewportBody
          viewMode={viewMode}
          visibleItems={visibleItems}
          totalItemsCount={items.length}
          hasMore={hasMore}
          columns={columns}
          scrollMargin={scrollMargin}
          listRef={listRef}
          sentinelRef={sentinelRef}
          onLoadMore={handleLoadMore}
          onMarkViewed={onMarkViewed}
          onHide={onHide}
          onRestore={onRestore}
          onSplitSource={onSplitSource}
          onUndoOverride={onUndoOverride}
        />
      )}
    </div>
  )
}
