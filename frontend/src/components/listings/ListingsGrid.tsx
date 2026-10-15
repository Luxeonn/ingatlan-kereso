import type { FC } from 'react'
import { Inbox, Search } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { PropertyCard } from './PropertyCard'
import { ListingSkeleton } from './ListingSkeleton'
import { BulkActionBar } from './BulkActionBar'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface ListingsGridProps {
  items: IngatlanItem[]
  totalRawCount: number
  isLoading: boolean
  hasSearched: boolean
  onMarkViewed: (url: string) => void
  onOpenAll: (items: IngatlanItem[]) => void
  onTriggerSearch: () => void
}

export const ListingsGrid: FC<ListingsGridProps> = ({
  items,
  totalRawCount,
  isLoading,
  hasSearched,
  onMarkViewed,
  onOpenAll,
  onTriggerSearch,
}) => {
  const { viewMode } = useUiStore()

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

  // 2. Initial state before search
  if (!hasSearched) {
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

  // 3. Empty results
  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-400 mb-4">
          <Inbox className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Nem található ingatlan a megadott feltételekkel
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Próbáld meg bővíteni az ár- vagy méretkorlátot, bekapcsolni további portálokat, vagy kikapcsolni a "Csak új találatok" szűrőt!
        </p>
      </div>
    )
  }

  // 4. Render listings
  return (
    <div className="space-y-4">
      {/* Bulk action toolbar */}
      <BulkActionBar
        items={items}
        totalRawCount={totalRawCount}
        onOpenAll={onOpenAll}
        onMarkAllViewed={(allItems) => {
          allItems.forEach((i) => onMarkViewed(i.url))
        }}
      />

      {/* Grid or List */}
      <div
        className={cn(
          viewMode === 'grid'
            ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3'
            : 'flex flex-col gap-3'
        )}
      >
        {items.map((item) => (
          <PropertyCard
            key={item.url || item.id}
            item={item}
            onMarkViewed={onMarkViewed}
            viewMode={viewMode}
          />
        ))}
      </div>
    </div>
  )
}
