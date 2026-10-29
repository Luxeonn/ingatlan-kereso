import { useEffect, useRef, useState, type FC } from 'react'
import { useWindowVirtualizer } from '@tanstack/react-virtual'
import { Inbox, Search, Star, Archive } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { PropertyCard } from './PropertyCard'
import { ListingSkeleton } from './ListingSkeleton'
import { BulkActionBar } from './BulkActionBar'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface ListingsGridProps {
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
  initialBatchSize?: number
  batchSize?: number
}

const DEFAULT_INITIAL_BATCH_SIZE = 18
const DEFAULT_BATCH_STEP = 18

export const ListingsGrid: FC<ListingsGridProps> = ({
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
  initialBatchSize = DEFAULT_INITIAL_BATCH_SIZE,
  batchSize = DEFAULT_BATCH_STEP,
}) => {
  const { viewMode } = useUiStore()
  const [columns, setColumns] = useState(1)
  const [scrollMargin, setScrollMargin] = useState(0)
  const [visibleLimit, setVisibleLimit] = useState(initialBatchSize)
  const [prevResetToken, setPrevResetToken] = useState(() => ({ items, mode, initialBatchSize }))
  const listRef = useRef<HTMLDivElement>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  // Reset pagination limit during render when items, mode, or batch size changes (#278)
  if (
    prevResetToken.items !== items ||
    prevResetToken.mode !== mode ||
    prevResetToken.initialBatchSize !== initialBatchSize
  ) {
    setPrevResetToken({ items, mode, initialBatchSize })
    setVisibleLimit(initialBatchSize)
  }

  // Progressive batching via IntersectionObserver (#278)
  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel) return

    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0]
        if (first && first.isIntersecting) {
          setVisibleLimit((prev) => Math.min(prev + batchSize, items.length))
        }
      },
      { rootMargin: '400px 0px' }
    )

    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [items.length, batchSize])

  const visibleItems = items.slice(0, visibleLimit)
  const hasMore = visibleLimit < items.length

  useEffect(() => {
    const updateColumns = () => {
      setColumns(viewMode === 'list' ? 1 : window.innerWidth >= 1024 ? 3 : window.innerWidth >= 640 ? 2 : 1)
    }
    updateColumns()
    window.addEventListener('resize', updateColumns)
    return () => window.removeEventListener('resize', updateColumns)
  }, [viewMode])

  useEffect(() => {
    const updateScrollMargin = () => {
      if (listRef.current) {
        setScrollMargin(listRef.current.getBoundingClientRect().top + window.scrollY)
      }
    }
    updateScrollMargin()
    window.addEventListener('resize', updateScrollMargin)
    return () => window.removeEventListener('resize', updateScrollMargin)
  }, [hasSearched, visibleItems.length, mode])

  const rowCount = Math.ceil(visibleItems.length / columns)
  const virtualizer = useWindowVirtualizer({
    count: rowCount,
    estimateSize: () => 480,
    overscan: 1,
    scrollMargin,
  })

  // 1. Loading skeletons
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

  // 2. Initial state before search (only in 'listings' mode)
  if (mode === 'listings' && !hasSearched) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--border-color)] bg-[var(--bg-card)]/50 p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-400 mb-4">
          <Search className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Készen állsz a keresésre?
        </h3>
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

  // 3. Empty Favorites state (#157)
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

  // 4. Empty Archive state (#157)
  if (mode === 'archive' && totalRawCount === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-purple-500/10 text-purple-400 mb-4">
          <Archive className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Az archívum jelenleg üres
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          A korábbi keresések során megtalált és mentett hirdetések automatikusan itt jelennek meg.
        </p>
      </div>
    )
  }

  // 5. Render toolbar + listings (or filtered empty message while keeping BulkActionBar accessible)
  return (
    <div className="space-y-4">
      {/* Bulk action toolbar with Quick Search & Portal filter chips */}
      <BulkActionBar
        items={items}
        rawItems={rawItems}
        totalRawCount={totalRawCount}
        onOpenAll={onOpenAll}
        onMarkAllViewed={(allItems) => {
          allItems.forEach((i) => onMarkViewed(i.url))
        }}
        onClearHistory={mode === 'archive' ? onClearHistory : undefined}
        isClearingHistory={isClearingHistory}
        visibleCount={visibleItems.length}
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
        <div className="space-y-4">
          <div ref={listRef} className="relative w-full" style={{ height: virtualizer.getTotalSize() }}>
            {virtualizer.getVirtualItems().map((virtualRow) => {
              const rowItems = visibleItems.slice(virtualRow.index * columns, (virtualRow.index + 1) * columns)
              return (
                <div
                  key={virtualRow.key}
                  ref={virtualizer.measureElement}
                  data-index={virtualRow.index}
                  className={cn(
                    viewMode === 'grid'
                      ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3'
                      : 'flex flex-col gap-3'
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
                      onMarkViewed={onMarkViewed}
                      viewMode={viewMode}
                    />
                  ))}
                </div>
              )
            })}
          </div>

          {/* Infinite Scroll Sentinel / Progressive loader (#278) */}
          {hasMore && (
            <div
              ref={sentinelRef}
              data-testid="infinite-scroll-sentinel"
              className="flex items-center justify-center py-6 text-xs text-[var(--text-secondary)]"
            >
              <div className="flex items-center gap-2">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
                <span>További ingatlanok betöltése... ({visibleItems.length} / {items.length} db)</span>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
