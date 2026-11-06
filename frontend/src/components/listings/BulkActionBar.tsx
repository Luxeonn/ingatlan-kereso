import { useMemo, useState, type FC } from 'react'
import {
  LayoutGrid,
  List,
  ExternalLink,
  CheckCheck,
  Search,
  X,
  Trash2,
  ArrowUpDown,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore, SORT_OPTIONS, type SortOption } from '../../store/useFilterStore'
import { isItemBargain } from '../../hooks/useSearch'
import { PORTAL_CHIPS, getTotalSourceListingsCount, normalizePortalKey } from '../../utils/formatters'
import { cn } from '../../utils/cn'
import { PortalFilterChipsRow, QuickFilterChipsRow } from './BulkFilterChips'

interface BulkActionBarProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  onOpenAll: (items: IngatlanItem[]) => void
  onMarkAllViewed: (items: IngatlanItem[]) => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
  visibleCount?: number
  hiddenCount?: number
}

function formatResultCount(
  visibleCount: number | undefined,
  itemsLength: number,
  totalRawCount: number,
  totalSourceListingsCount: number,
): string {
  const sourceSuffix = totalSourceListingsCount > itemsLength ? ` (${totalSourceListingsCount} hirdetés)` : ''
  if (visibleCount !== null && visibleCount !== undefined && visibleCount < itemsLength) {
    return `Megjelenítve: ${visibleCount} / ${itemsLength} db${sourceSuffix}`
  }
  if (itemsLength !== totalRawCount) {
    return `${itemsLength} / ${totalRawCount} db${sourceSuffix}`
  }
  if (totalSourceListingsCount > itemsLength) {
    return `${itemsLength} ingatlan (${totalSourceListingsCount} hirdetés)`
  }
  return `${itemsLength} db`
}

export const BulkActionBar: FC<BulkActionBarProps> = ({
  items,
  rawItems,
  totalRawCount,
  onOpenAll,
  onMarkAllViewed,
  onClearHistory,
  isClearingHistory = false,
  visibleCount,
  hiddenCount = 0,
}) => {
  const { viewMode, setViewMode } = useUiStore()
  const {
    criteria,
    onlyNew,
    setOnlyNew,
    setOnlyNewConstruction,
    setExcludeViewed,
    sortOption,
    setSortOption,
    onlyBargain,
    setOnlyBargain,
    minQualityScore,
    setMinQualityScore,
    quickSearchQuery,
    setQuickSearchQuery,
    selectedPortalFilter,
    setSelectedPortalFilter,
    showHidden,
    setShowHidden,
  } = useFilterStore()
  const [confirmClear, setConfirmClear] = useState(false)

  const unviewedCount = items.filter((item) => !item.viewed).length
  const totalSourceListingsCount = useMemo(() => getTotalSourceListingsCount(items), [items])
  const sourceItems = rawItems ?? items

  const bargainCount = useMemo(
    () => sourceItems.filter((it) => isItemBargain(it)).length,
    [sourceItems],
  )

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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm sm:max-w-md">
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

        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex items-center gap-1.5 text-xs shrink-0">
            <ArrowUpDown className="h-3.5 w-3.5 text-[var(--text-secondary)] shrink-0" />
            <label htmlFor="bulk-sort-select" className="text-[var(--text-secondary)] font-medium">
              Rendezés:
            </label>
            <select
              id="bulk-sort-select"
              aria-label="Rendezés"
              value={sortOption}
              onChange={(e) => setSortOption(e.target.value as SortOption)}
              className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1.5 text-xs font-semibold text-[var(--text-primary)] focus:border-blue-500 focus:outline-none"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-primary)] shrink-0">
            <span>{formatResultCount(visibleCount, items.length, totalRawCount, totalSourceListingsCount)}</span>
            {items.length !== totalRawCount && (visibleCount === null || visibleCount === undefined) && (
              <span className="text-[11px] font-normal text-[var(--text-secondary)]">
                (szűrve)
              </span>
            )}
          </div>

          {items.length > 0 && (
            <button
              type="button"
              onClick={() => onOpenAll(items.slice(0, 15))}
              title="A listában látható első 15 ingatlan megnyitása új böngészőlapokon"
              className="flex items-center gap-1.5 rounded-lg border border-blue-500/40 bg-blue-500/10 px-3 py-1.5 text-xs font-semibold text-blue-400 transition-colors hover:bg-blue-500/20 shrink-0"
            >
              <ExternalLink className="h-3.5 w-3.5" />
              <span>Összes megnyitása</span>
            </button>
          )}

          {unviewedCount > 0 && (
            <button
              type="button"
              onClick={() => onMarkAllViewed(items)}
              className="flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-3 py-1.5 text-xs font-semibold text-[var(--text-secondary)] transition-colors hover:text-[var(--text-primary)] shrink-0"
            >
              <CheckCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span className="hidden sm:inline">Mind megtekintett</span>
            </button>
          )}

          {onClearHistory && (
            confirmClear ? (
              <div className="flex items-center gap-1.5 shrink-0">
                <button
                  type="button"
                  disabled={isClearingHistory}
                  onClick={() => {
                    onClearHistory()
                    setConfirmClear(false)
                  }}
                  className="flex items-center gap-1 rounded-lg bg-rose-600 px-2.5 py-1.5 text-xs font-bold text-white hover:bg-rose-500 disabled:opacity-50"
                >
                  <span>Törlés megerősítése</span>
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmClear(false)}
                  className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2 py-1.5 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
                >
                  Mégse
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmClear(true)}
                className="flex items-center gap-1.5 rounded-lg border border-rose-500/40 bg-rose-500/10 px-3 py-1.5 text-xs font-semibold text-rose-400 transition-colors hover:bg-rose-500/20 shrink-0"
              >
                <Trash2 className="h-3.5 w-3.5" />
                <span>Előzmények törlése</span>
              </button>
            )
          )}

          <div className="flex items-center rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] p-0.5 shrink-0">
            <button
              type="button"
              onClick={() => setViewMode('grid')}
              title="Rács nézet"
              className={cn(
                'rounded-md p-1 transition-colors',
                viewMode === 'grid'
                  ? 'bg-blue-600 text-white'
                  : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
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
                  : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
              )}
            >
              <List className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>

      <QuickFilterChipsRow
        onlyNew={onlyNew}
        setOnlyNew={setOnlyNew}
        onlyNewConstruction={criteria.onlyNewConstruction}
        setOnlyNewConstruction={setOnlyNewConstruction}
        onlyBargain={onlyBargain}
        setOnlyBargain={setOnlyBargain}
        bargainCount={bargainCount}
        excludeViewed={criteria.excludeViewed}
        setExcludeViewed={setExcludeViewed}
        minQualityScore={minQualityScore}
        setMinQualityScore={setMinQualityScore}
        showHidden={showHidden}
        setShowHidden={setShowHidden}
        hiddenCount={hiddenCount}
      />

      <PortalFilterChipsRow
        selectedPortalFilter={selectedPortalFilter}
        setSelectedPortalFilter={setSelectedPortalFilter}
        portalCounts={portalCounts}
      />
    </div>
  )
}
