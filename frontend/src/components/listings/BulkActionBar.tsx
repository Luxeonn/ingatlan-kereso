import { useEffect, useMemo, useState, type FC } from 'react'
import {
  LayoutGrid,
  List,
  Table2,
  Search,
  X,
  Trash2,
  ArrowUpDown,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore, SORT_OPTIONS, isSortOption } from '../../store/useFilterStore'
import { getTotalSourceListingsCount } from '../../utils/formatters'
import { cn } from '../../utils/cn'
import { PortalFilterChipsRow, QuickFilterChipsRow } from './BulkFilterChips'
import { FloatingSelectionBar } from './FloatingSelectionBar'
import {
  computePortalCounts,
  computeQuickFilterCounts,
  formatResultCount,
} from './bulkActionBarHelpers'

interface BulkActionBarProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  sessionViewedUrls?: ReadonlySet<string>
  onClearHistory?: () => void
  isClearingHistory?: boolean
  onMergeSelected?: (urls: string[]) => Promise<boolean | void> | void
  isMerging?: boolean
  hiddenCount?: number
}

export const BulkActionBar: FC<BulkActionBarProps> = ({
  items,
  rawItems,
  totalRawCount,
  sessionViewedUrls,
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
    () => computeQuickFilterCounts(sourceItems, sessionViewedUrls),
    [sourceItems, sessionViewedUrls],
  )

  const portalCounts = useMemo(() => computePortalCounts(sourceItems), [sourceItems])

  return (
    <>
      <div className="relative z-30 space-y-2.5 rounded-[20px] glass-surface p-3.5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="inline-flex items-center gap-1.5 rounded-full glass-capsule px-3 py-1.5 text-xs font-bold text-[#0b1220] shrink-0 tabular-nums">
            <span>{formatResultCount(items.length, totalRawCount, totalSourceListingsCount)}</span>
            {items.length !== totalRawCount && (
              <span className="text-[11px] font-semibold text-[#0071e3]">
                (szűrve)
              </span>
            )}
          </div>

          <div className="relative flex-1 min-w-[140px]">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[#42526b]" />
            <input
              type="text"
              value={quickSearchQuery}
              onChange={(e) => setQuickSearchQuery(e.target.value)}
              placeholder="Gyorskeresés a találatok között (cím, ár, m², szoba)..."
              aria-label="Gyorskeresés a találatok között"
              className="w-full rounded-full border border-[var(--border-hairline)] bg-white/75 py-1.5 pl-8 pr-8 text-xs font-medium text-[#0b1220] placeholder:text-[#42526b] focus:border-[#0071e3] focus:outline-none"
            />
            {quickSearchQuery && (
              <button
                type="button"
                onClick={() => setQuickSearchQuery('')}
                aria-label="Gyorskeresés törlése"
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#42526b] hover:text-[#0b1220] cursor-pointer"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2 min-w-0">
            <div className="flex items-center gap-1.5 text-xs shrink-0">
              <ArrowUpDown className="h-3.5 w-3.5 text-[#42526b] shrink-0" />
              <label htmlFor="bulk-sort-select" className="text-[#42526b] font-medium">
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
                className="max-w-[215px] truncate rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 text-xs font-semibold text-[#0b1220] focus:border-[#0071e3] focus:outline-none cursor-pointer"
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
                    className="flex items-center gap-1 rounded-full bg-rose-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-rose-500 disabled:opacity-50 cursor-pointer"
                  >
                    <span>Törlés megerősítése</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfirmClear(false)}
                    className="rounded-full border border-[var(--border-hairline)] bg-white/75 px-2.5 py-1.5 text-xs font-semibold text-[#42526b] hover:text-[#0b1220] cursor-pointer"
                  >
                    Mégse
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setConfirmClear(true)}
                  className="flex items-center gap-1.5 rounded-full border border-rose-500/35 bg-rose-500/10 px-3 py-1.5 text-xs font-semibold text-rose-700 transition-colors hover:bg-rose-500/15 shrink-0 cursor-pointer"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>Előzmények törlése</span>
                </button>
              )
            )}

            <div className="flex items-center rounded-full glass-capsule p-0.5 shrink-0">
              <button
                type="button"
                onClick={() => setViewMode('grid')}
                aria-label="Rács nézet"
                aria-pressed={viewMode === 'grid'}
                title="Rács nézet"
                className={cn(
                  'rounded-full p-1.5 transition-colors cursor-pointer',
                  viewMode === 'grid'
                    ? 'bg-[#0071e3] text-white shadow-xs'
                    : 'text-[#42526b] hover:text-[#0b1220]',
                )}
              >
                <LayoutGrid className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                onClick={() => setViewMode('list')}
                aria-label="Lista nézet"
                aria-pressed={viewMode === 'list'}
                title="Lista nézet"
                className={cn(
                  'rounded-full p-1.5 transition-colors cursor-pointer',
                  viewMode === 'list'
                    ? 'bg-[#0071e3] text-white shadow-xs'
                    : 'text-[#42526b] hover:text-[#0b1220]',
                )}
              >
                <List className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                data-testid="view-mode-table"
                onClick={() => setViewMode('table')}
                aria-label="Táblázat nézet"
                aria-pressed={viewMode === 'table'}
                title="Táblázat nézet"
                className={cn(
                  'rounded-full p-1.5 transition-colors cursor-pointer',
                  viewMode === 'table'
                    ? 'bg-[#0071e3] text-white shadow-xs'
                    : 'text-[#42526b] hover:text-[#0b1220]',
                )}
              >
                <Table2 className="h-3.5 w-3.5" />
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
      </div>

      <FloatingSelectionBar
        selectedCount={selectedItems.length}
        isMerging={isMerging}
        canMerge={Boolean(onMergeSelected)}
        onMerge={handleMergeClick}
        onClear={clearCardSelection}
      />
    </>
  )
}
