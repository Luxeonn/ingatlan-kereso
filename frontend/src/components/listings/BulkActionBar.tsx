import { useMemo, useState, type FC } from 'react'
import { LayoutGrid, List, ExternalLink, CheckCheck, Search, X, Trash2 } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import { normalizePortalKey } from '../../hooks/useSearch'
import { cn } from '../../utils/cn'

interface BulkActionBarProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  onOpenAll: (items: IngatlanItem[]) => void
  onMarkAllViewed: (items: IngatlanItem[]) => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
}

const PORTAL_CHIPS = [
  { id: 'all', label: 'Összes' },
  { id: 'dh', label: 'Duna House' },
  { id: 'oc', label: 'Otthon Centrum' },
  { id: 'jofogas', label: 'Jófogás' },
  { id: 'ingatlancom', label: 'Ingatlan.com' },
  { id: 'ingatlanbazar', label: 'Ingatlanbazár' },
  { id: 'koltozzbe', label: 'Költözzbe' },
]

export const BulkActionBar: FC<BulkActionBarProps> = ({
  items,
  rawItems,
  totalRawCount,
  onOpenAll,
  onMarkAllViewed,
  onClearHistory,
  isClearingHistory = false,
}) => {
  const { viewMode, setViewMode } = useUiStore()
  const {
    quickSearchQuery,
    setQuickSearchQuery,
    selectedPortalFilter,
    setSelectedPortalFilter,
  } = useFilterStore()
  const [confirmClear, setConfirmClear] = useState(false)

  const unviewedCount = items.filter((item) => !item.viewed).length
  const sourceItems = rawItems ?? items

  const portalCounts = useMemo(() => {
    const counts: Record<string, number> = { all: sourceItems.length }
    PORTAL_CHIPS.forEach((chip) => {
      if (chip.id !== 'all') counts[chip.id] = 0
    })
    sourceItems.forEach((item) => {
      const key = normalizePortalKey(item.source?.portalId || item.portal)
      if (key) {
        counts[key] = (counts[key] || 0) + 1
      }
    })
    return counts
  }, [sourceItems])

  return (
    <div className="space-y-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-sm">
      {/* Top row: Quick Search input + Count summary + Actions */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Quick Search Input (#157) */}
        <div className="relative flex-1 min-w-[220px] max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--text-secondary)]" />
          <input
            type="text"
            value={quickSearchQuery}
            onChange={(e) => setQuickSearchQuery(e.target.value)}
            placeholder="Gyorskeresés a találatok között (cím, ár, m², szoba)..."
            aria-label="Gyorskeresés a találatok között"
            className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] py-1.5 pl-8 pr-8 text-xs text-[var(--text-primary)] placeholder:text-[var(--text-secondary)] focus:border-blue-500 focus:outline-none"
          />
          {quickSearchQuery && (
            <button
              type="button"
              onClick={() => setQuickSearchQuery('')}
              aria-label="Gyorskeresés törlése"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        {/* Count summary & Action buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 mr-1">
            <span className="text-xs font-bold text-[var(--text-primary)]">
              {items.length} db találat
            </span>
            {items.length !== totalRawCount && (
              <span className="text-[11px] text-[var(--text-secondary)]">
                (összesen {totalRawCount} db-ból szűrve)
              </span>
            )}
          </div>

          {/* Open all button */}
          {items.length > 0 && (
            <button
              type="button"
              onClick={() => onOpenAll(items.slice(0, 15))}
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

          {/* Archive Clear History button (#157) */}
          {onClearHistory && (
            confirmClear ? (
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  disabled={isClearingHistory}
                  onClick={() => {
                    onClearHistory()
                    setConfirmClear(false)
                  }}
                  className="flex items-center gap-1 rounded-lg bg-rose-600 px-2.5 py-1 text-xs font-bold text-white hover:bg-rose-500 disabled:opacity-50"
                >
                  <span>Törlés megerősítése</span>
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmClear(false)}
                  className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2 py-1 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
                >
                  Mégse
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmClear(true)}
                className="flex items-center gap-1.5 rounded-lg border border-rose-500/40 bg-rose-500/10 px-3 py-1 text-xs font-semibold text-rose-400 transition-colors hover:bg-rose-500/20"
              >
                <Trash2 className="h-3.5 w-3.5" />
                <span>Előzmények törlése</span>
              </button>
            )
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

      {/* Bottom row: Portal quick-filter chips (#157) */}
      <div
        data-testid="portal-quick-filter-chips"
        className="flex flex-wrap items-center gap-1.5 border-t border-[var(--border-color)] pt-2.5"
      >
        {PORTAL_CHIPS.map((chip) => {
          const count = portalCounts[chip.id] || 0
          const isActive = selectedPortalFilter === chip.id
          return (
            <button
              key={chip.id}
              type="button"
              onClick={() => setSelectedPortalFilter(chip.id)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-[11px] font-semibold transition-all',
                isActive
                  ? 'border-blue-500/50 bg-blue-600 text-white shadow-sm'
                  : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              )}
            >
              <span>{chip.label}</span>
              <span
                className={cn(
                  'rounded-full px-1.5 py-0.2 text-[10px] font-bold',
                  isActive
                    ? 'bg-white/20 text-white'
                    : 'bg-[var(--bg-card)] text-[var(--text-secondary)]'
                )}
              >
                {count}
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
