import { useCallback, useEffect, useMemo, useRef, useState, type FC } from 'react'
import { useWindowVirtualizer } from '@tanstack/react-virtual'
import type { IngatlanItem } from '../../types/property'
import { PropertyCard } from './PropertyCard'
import { ListingsTableView } from './ListingsTableView'
import { BulkActionBar } from './BulkActionBar'
import { renderEmptyState, renderLoadingSkeletons, renderNoMatchingResults } from './ListingsGridStates'
import { useUiStore, type ViewMode } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface ListingsGridProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  sessionViewedUrls?: ReadonlySet<string>
  isLoading: boolean
  hasSearched: boolean
  mode?: 'listings' | 'favorites' | 'archive'
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

const DEFAULT_INITIAL_BATCH_SIZE = 18
const DEFAULT_BATCH_STEP = 18
const GRID_GAP_PX = 16
const MIN_CARD_WIDTH_PX = 296

function calculateColumnCount(viewMode: string, containerWidth: number): number {
  if (viewMode === 'list') return 1
  if (containerWidth <= 0) return 1
  return Math.max(1, Math.floor((containerWidth + GRID_GAP_PX) / (MIN_CARD_WIDTH_PX + GRID_GAP_PX)))
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
    estimateSize: () => (viewMode === 'list' ? 160 : 460),
    gap: viewMode === 'list' ? 12 : GRID_GAP_PX,
    overscan: 4,
    scrollMargin,
  })

  useEffect(() => {
    if (typeof virtualizer.measure === 'function') {
      virtualizer.measure()
    }
  }, [virtualizer, columns, viewMode])

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
                  'z-0 hover:z-10 focus-within:z-20 has-[[data-testid="bargain-popover"]]:z-20',
                  viewMode === 'grid'
                    ? 'grid grid-cols-[repeat(auto-fill,minmax(296px,1fr))] gap-4'
                    : 'flex flex-col gap-3',
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
          className="flex w-full items-center justify-center py-6 text-xs text-[#42526b] hover:text-[#0b1220] transition-colors cursor-pointer tabular-nums"
        >
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-[#0071e3] border-t-transparent" />
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
  sessionViewedUrls,
  isLoading,
  hasSearched,
  mode = 'listings',
  onMarkViewed,
  onHide,
  onRestore,
  hiddenCount = 0,
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
  const hasItems = items.length > 0
  const effectiveColumns = viewMode === 'list' ? 1 : columns

  useEffect(() => {
    if (viewMode === 'list') return undefined
    const el = listRef.current
    if (!el) return undefined

    const measure = (reportedWidth?: number) => {
      const rect = el.getBoundingClientRect()
      const width =
        reportedWidth && reportedWidth > 0
          ? reportedWidth
          : rect.width > 0
            ? rect.width
            : el.clientWidth
      setColumns(calculateColumnCount(viewMode, width))
      setScrollMargin(rect.top + window.scrollY)
    }

    measure()
    if (typeof ResizeObserver === 'undefined') return undefined

    const observer = new ResizeObserver((entries) => {
      measure(entries[0]?.contentRect?.width)
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [viewMode, isLoading, hasSearched, hasItems])

  useEffect(() => {
    const updateScrollMargin = () => {
      if (listRef.current) setScrollMargin(listRef.current.getBoundingClientRect().top + window.scrollY)
    }
    updateScrollMargin()
    window.addEventListener('resize', updateScrollMargin)
    return () => window.removeEventListener('resize', updateScrollMargin)
  }, [hasSearched, visibleItems.length, mode, effectiveColumns, viewMode, isLoading])

  if (isLoading) return renderLoadingSkeletons()

  const emptyState = renderEmptyState(mode, hasSearched, totalRawCount, onTriggerSearch)
  if (emptyState) return emptyState

  return (
    <div className="space-y-4">
      <BulkActionBar
        items={items}
        rawItems={rawItems}
        totalRawCount={totalRawCount}
        sessionViewedUrls={sessionViewedUrls}
        onClearHistory={mode === 'archive' ? onClearHistory : undefined}
        isClearingHistory={isClearingHistory}
        onMergeSelected={onMergeSelected}
        isMerging={isMerging}
        hiddenCount={hiddenCount}
      />

      {items.length === 0 ? renderNoMatchingResults() : (
        <ListingsViewportBody
          viewMode={viewMode}
          visibleItems={visibleItems}
          totalItemsCount={items.length}
          hasMore={hasMore}
          columns={effectiveColumns}
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
