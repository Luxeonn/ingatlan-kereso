import { useEffect, useMemo, useState, type FC } from 'react'
import {
  LayoutGrid,
  List,
  Search,
  X,
  Trash2,
  ArrowUpDown,
  GitMerge,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore, SORT_OPTIONS, isSortOption } from '../../store/useFilterStore'
import { isItemBargain, isListingNew } from '../../utils/listingFilters'
import {
  PORTAL_CHIPS,
  getItemSourceListingsCount,
  getPrimaryPortalKey,
  getTotalSourceListingsCount,
  hasListingImage,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'
import { PortalFilterChipsRow, QuickFilterChipsRow } from './BulkFilterChips'

function computeQuickFilterCounts(sourceItems: IngatlanItem[]) {
  const counts = {
    bargain: 0,
    onlyNew: 0,
    excludeViewed: 0,
    onlyNewConstruction: 0,
    excludeMultiSource: 0,
    onlyMultiSource: 0,
    excludeNoImage: 0,
  }
  for (const item of sourceItems) {
    if (isItemBargain(item)) counts.bargain += 1
    if (isListingNew(item)) counts.onlyNew += 1
    if (!item.viewed) counts.excludeViewed += 1
    if (item.newConstruction) counts.onlyNewConstruction += 1
    const sourceCount = getItemSourceListingsCount(item)
    if (sourceCount === 1) counts.excludeMultiSource += 1
    if (sourceCount > 1) counts.onlyMultiSource += 1
    if (hasListingImage(item)) counts.excludeNoImage += 1
  }
  return counts
}

interface BulkActionBarProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  onOpenAll?: (items: IngatlanItem[]) => void
  onMarkAllViewed?: (items: IngatlanItem[]) => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
  onMergeSelected?: (urls: string[]) => Promise<boolean | void> | void
  isMerging?: boolean
  visibleCount?: number
  hiddenCount?: number
}

function formatResultCount(
  itemsLength: number,
  totalRawCount: number,
  totalSourceListingsCount: number,
): string {
  const sourceSuffix = totalSourceListingsCount > itemsLength ? ` (${totalSourceListingsCount} hirdetés)` : ''
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
  onClearHistory,
  isClearingHistory = false,
  onMergeSelected,
  isMerging = false,
  hiddenCount = 0,
}) => {
  const { viewMode, setViewMode, selectedCardUrls, clearCardSelection } = useUiStore()
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
    excludeMultiSource,
    setExcludeMultiSource,
    onlyMultiSource,
    setOnlyMultiSource,
    excludeNoImage,
    setExcludeNoImage,
    quickSearchQuery,
    setQuickSearchQuery,
    selectedPortalFilter,
    setSelectedPortalFilter,
    showHidden,
    setShowHidden,
  } = useFilterStore()
  const [confirmClear, setConfirmClear] = useState(false)

  const totalSourceListingsCount = useMemo(() => getTotalSourceListingsCount(items), [items])
  const sourceItems = rawItems ?? items

  const selectedItems = useMemo(
    () => items.filter((it) => Boolean(it.url && selectedCardUrls.includes(it.url))),
    [items, selectedCardUrls],
  )

  useEffect(() => {
    if (selectedCardUrls.length === 0) return undefined
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        clearCardSelection()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [selectedCardUrls.length, clearCardSelection])

  const handleMergeClick = () => {
    if (!onMergeSelected || selectedItems.length < 2 || isMerging) return
    const allUrls = new Set<string>()
    for (const item of selectedItems) {
      if (item.url) allUrls.add(item.url)
      for (const src of item.sourceListings ?? []) {
        if (src.url) allUrls.add(src.url)
      }
    }
    void onMergeSelected(Array.from(allUrls))
  }

  const quickFilterCounts = useMemo(
    () => computeQuickFilterCounts(sourceItems),
    [sourceItems],
  )

  const portalCounts = useMemo(() => {
    const counts: Record<string, number> = { all: sourceItems.length }
    PORTAL_CHIPS.forEach((chip) => {
      if (chip.id !== 'all') counts[chip.id] = 0
    })
    sourceItems.forEach((item) => {
      const primaryKey = getPrimaryPortalKey(item)
      if (primaryKey) {
        counts[primaryKey] = (counts[primaryKey] || 0) + 1
      }
    })
    return counts
  }, [sourceItems])

  return (
    <div className="space-y-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-primary)] shrink-0 tabular-nums">
          <span>{formatResultCount(items.length, totalRawCount, totalSourceListingsCount)}</span>
          {items.length !== totalRawCount && (
            <span className="text-[11px] font-normal text-[var(--text-secondary)]">
              (szűrve)
            </span>
          )}
        </div>

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

        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <div className="flex items-center gap-1.5 text-xs shrink-0">
            <ArrowUpDown className="h-3.5 w-3.5 text-[var(--text-secondary)] shrink-0" />
            <label htmlFor="bulk-sort-select" className="text-[var(--text-secondary)] font-medium">
              Rendezés:
            </label>
            <select
              id="bulk-sort-select"
              aria-label="Rendezés"
              value={sortOption}
              onChange={(e) => {
                const val = e.target.value
                if (isSortOption(val)) {
                  setSortOption(val)
                }
              }}
              className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1.5 text-xs font-semibold text-[var(--text-primary)] focus:border-blue-500 focus:outline-none"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

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
        onlyNewCount={quickFilterCounts.onlyNew}
        onlyNewConstruction={criteria.onlyNewConstruction}
        setOnlyNewConstruction={setOnlyNewConstruction}
        onlyNewConstructionCount={quickFilterCounts.onlyNewConstruction}
        onlyBargain={onlyBargain}
        setOnlyBargain={setOnlyBargain}
        bargainCount={quickFilterCounts.bargain}
        excludeViewed={criteria.excludeViewed}
        setExcludeViewed={setExcludeViewed}
        excludeViewedCount={quickFilterCounts.excludeViewed}
        excludeMultiSource={excludeMultiSource}
        setExcludeMultiSource={setExcludeMultiSource}
        excludeMultiSourceCount={quickFilterCounts.excludeMultiSource}
        onlyMultiSource={onlyMultiSource}
        setOnlyMultiSource={setOnlyMultiSource}
        onlyMultiSourceCount={quickFilterCounts.onlyMultiSource}
        excludeNoImage={excludeNoImage}
        setExcludeNoImage={setExcludeNoImage}
        excludeNoImageCount={quickFilterCounts.excludeNoImage}
        showHidden={showHidden}
        setShowHidden={setShowHidden}
        hiddenCount={hiddenCount}
      />

      <PortalFilterChipsRow
        selectedPortalFilter={selectedPortalFilter}
        setSelectedPortalFilter={setSelectedPortalFilter}
        portalCounts={portalCounts}
        searchedPortals={criteria.portals}
      />

      {selectedItems.length > 0 && (
        <div
          data-testid="selection-action-bar"
          className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-blue-500/40 bg-blue-500/10 px-3 py-2 text-xs"
        >
          <div className="flex flex-wrap items-center gap-2 font-semibold text-blue-200">
            <span data-testid="selection-count-label">{selectedItems.length} kijelölve</span>
            <span aria-hidden="true">·</span>
            <button
              type="button"
              data-testid="merge-selected-button"
              disabled={selectedItems.length < 2 || isMerging || !onMergeSelected}
              onClick={handleMergeClick}
              className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1 font-bold text-white shadow-sm transition-colors hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <GitMerge className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Összevonás</span>
            </button>
            <span aria-hidden="true">·</span>
            <button
              type="button"
              data-testid="clear-selection-button"
              onClick={clearCardSelection}
              className="rounded-lg border border-blue-400/30 px-2.5 py-1 font-semibold text-blue-200 transition-colors hover:bg-blue-500/20"
            >
              Kijelölés törlése
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
