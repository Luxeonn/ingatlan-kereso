import type { FC } from 'react'
import { LayoutGrid, List, ExternalLink, CheckCheck } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface BulkActionBarProps {
  items: IngatlanItem[]
  totalRawCount: number
  onOpenAll: (items: IngatlanItem[]) => void
  onMarkAllViewed: (items: IngatlanItem[]) => void
}

export const BulkActionBar: FC<BulkActionBarProps> = ({
  items,
  totalRawCount,
  onOpenAll,
  onMarkAllViewed,
}) => {
  const { viewMode, setViewMode } = useUiStore()

  const unviewedCount = items.filter((item) => !item.viewed).length

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] px-4 py-2.5 shadow-sm">
      {/* Count summary */}
      <div className="flex items-center gap-2">
        <span className="text-sm font-bold text-[var(--text-primary)]">
          {items.length} db találat
        </span>
        {items.length !== totalRawCount && (
          <span className="text-xs text-[var(--text-secondary)]">
            (összesen {totalRawCount} db-ból szűrve)
          </span>
        )}
      </div>

      {/* Actions and view mode switch */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Open all button */}
        {items.length > 0 && (
          <button
            type="button"
            onClick={() => onOpenAll(items.slice(0, 15))} // Biztonsági limit max 15 tab egyszerre
            title="A listában látható első 15 ingatlan megnyitása új böngészőlapokon"
            className="flex items-center gap-1.5 rounded-lg border border-blue-500/40 bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-400 transition-colors hover:bg-blue-500/20"
          >
            <ExternalLink className="h-3.5 w-3.5" />
            <span>Összes megnyitása</span>
          </button>
        )}

        {/* Mark all viewed */}
        {unviewedCount > 0 && (
          <button
            type="button"
            onClick={() => onMarkAllViewed(items)}
            className="flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-3 py-1 text-xs font-semibold text-[var(--text-secondary)] transition-colors hover:text-[var(--text-primary)]"
          >
            <CheckCheck className="h-3.5 w-3.5 text-emerald-400" />
            <span className="hidden sm:inline">Mind megtekintett</span>
          </button>
        )}

        {/* View mode toggle */}
        <div className="flex items-center rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] p-0.5">
          <button
            type="button"
            onClick={() => setViewMode('grid')}
            title="Rács nézet"
            className={cn(
              'rounded-md p-1 transition-colors',
              viewMode === 'grid'
                ? 'bg-blue-600 text-white'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            )}
          >
            <LayoutGrid className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            onClick={() => setViewMode('list')}
            title="Lista nézet"
            className={cn(
              'rounded-md p-1 transition-colors',
              viewMode === 'list'
                ? 'bg-blue-600 text-white'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            )}
          >
            <List className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  )
}
