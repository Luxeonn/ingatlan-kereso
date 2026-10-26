import { useMemo, useState, type FC } from 'react'
import {
  LayoutGrid,
  List,
  ExternalLink,
  CheckCheck,
  Search,
  X,
  Trash2,
  EyeOff,
  ArrowUpDown,
  TrendingDown,
  ShieldCheck,
  Sparkles,
  Building2,
} from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore, type SortOption } from '../../store/useFilterStore'
import { normalizePortalKey, isItemBargain } from '../../hooks/useSearch'
import { cn } from '../../utils/cn'

interface BulkActionBarProps {
  items: IngatlanItem[]
  rawItems?: IngatlanItem[]
  totalRawCount: number
  onOpenAll: (items: IngatlanItem[]) => void
  onMarkAllViewed: (items: IngatlanItem[]) => void
  onClearHistory?: () => void
  isClearingHistory?: boolean
  visibleCount?: number
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
  visibleCount,
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
  } = useFilterStore()
  const [confirmClear, setConfirmClear] = useState(false)

  const unviewedCount = items.filter((item) => !item.viewed).length
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
      {/* 1. Sor – Keresés & Fő akciók (Search & Actions Bar) */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Bal oldal: Gyorskereső mező */}
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

        {/* Jobb oldal: Rendezés, Találatszámláló, Tömeges műveletek és Nézetváltó */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Rendezés legördülő menü */}
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
              <option value="price_asc">Ár: Növekvő (alapértelmezett)</option>
              <option value="price_desc">Ár: Csökkenő</option>
              <option value="sqm_price_asc">Ft/m²: Növekvő</option>
              <option value="sqm_price_desc">Ft/m²: Csökkenő</option>
              <option value="area_desc">Méret: Legnagyobb elöl</option>
              <option value="date_desc">Felfedezés: Legfrissebb</option>
            </select>
          </div>

          {/* Találatszámláló */}
          <div className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-primary)] shrink-0">
            <span>
              {visibleCount != null && visibleCount < items.length
                ? `Megjelenítve: ${visibleCount} / ${items.length} db`
                : items.length !== totalRawCount
                  ? `${items.length} / ${totalRawCount} db`
                  : `${items.length} db`}
            </span>
            {items.length !== totalRawCount && visibleCount == null && (
              <span className="text-[11px] font-normal text-[var(--text-secondary)]">
                (szűrve)
              </span>
            )}
          </div>

          {/* Tömeges műveletek */}
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

          {/* Nézetváltó */}
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

      {/* 2. Sor – Gyorsszűrő Chipek és Kritériumok (Filter Chips Row) */}
      <div
        data-testid="filter-chips-row"
        className="flex flex-wrap items-center gap-2 border-t border-[var(--border-color)] pt-2.5 text-xs"
      >
        {/* 1. Csak új találatok (emerald spark icon) */}
        <button
          type="button"
          aria-pressed={onlyNew}
          onClick={() => setOnlyNew(!onlyNew)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all shrink-0',
            onlyNew
              ? 'border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
          )}
        >
          <Sparkles className="h-3.5 w-3.5 text-emerald-400" />
          <span>Csak új találatok</span>
        </button>

        {/* 2. Csak új építésű (cyan building icon) */}
        <button
          type="button"
          aria-pressed={criteria.onlyNewConstruction}
          aria-label="Csak új építésű ingatlanok"
          onClick={() => setOnlyNewConstruction(!criteria.onlyNewConstruction)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all shrink-0',
            criteria.onlyNewConstruction
              ? 'border border-cyan-500/40 bg-cyan-500/15 text-cyan-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
          )}
        >
          <Building2 className="h-3.5 w-3.5 text-cyan-400" />
          <span>Csak új építésű</span>
        </button>

        {/* 3. Alkalmi vétel (zöld badge számlálóval) */}
        <button
          type="button"
          data-testid="bargain-filter-toggle"
          aria-pressed={onlyBargain}
          onClick={() => setOnlyBargain(!onlyBargain)}
          title="Kizárólag alkalmi vételek megjelenítése (mediánnál legalább 20%-kal olcsóbb ingatlanok)"
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all shrink-0',
            onlyBargain
              ? 'border border-emerald-500/50 bg-emerald-500/15 text-emerald-400 shadow-sm ring-1 ring-emerald-500/30'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-emerald-400 hover:border-emerald-500/30',
          )}
        >
          <TrendingDown className="h-3.5 w-3.5 text-emerald-400" />
          <span>Alkalmi vétel</span>
          {bargainCount > 0 && (
            <span
              data-testid="bargain-filter-count"
              className={cn(
                'rounded-full px-1.5 py-0.2 text-[10px] font-bold',
                onlyBargain
                  ? 'bg-emerald-500/30 text-emerald-200'
                  : 'bg-[var(--bg-card)] text-emerald-400',
              )}
            >
              {bargainCount}
            </span>
          )}
        </button>

        {/* 4. Megtekintettek rejtése (kék eye-off icon) */}
        <button
          type="button"
          aria-pressed={criteria.excludeViewed}
          onClick={() => setExcludeViewed(!criteria.excludeViewed)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all shrink-0',
            criteria.excludeViewed
              ? 'border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
          )}
        >
          <EyeOff className="h-3.5 w-3.5 text-blue-400" />
          <span>Megtekintettek rejtése</span>
        </button>

        {/* 5. Minőség: választó (Adatminőségi küszöb) */}
        <div className="flex items-center gap-1.5 text-xs shrink-0">
          <ShieldCheck
            className={cn(
              'h-3.5 w-3.5 shrink-0',
              minQualityScore >= 85
                ? 'text-emerald-400'
                : minQualityScore >= 70
                  ? 'text-amber-400'
                  : 'text-[var(--text-secondary)]',
            )}
          />
          <label htmlFor="listings-quality-select" className="text-[var(--text-secondary)] font-medium">
            Minőség:
          </label>
          <select
            id="listings-quality-select"
            data-testid="quality-filter-select"
            aria-label="Adatminőség szűrő"
            value={minQualityScore}
            onChange={(e) => setMinQualityScore(Number(e.target.value))}
            className={cn(
              'rounded-lg border px-2.5 py-1.5 text-xs font-semibold focus:border-blue-500 focus:outline-none transition-all',
              minQualityScore >= 85
                ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
                : minQualityScore >= 70
                  ? 'border-amber-500/40 bg-amber-500/15 text-amber-300'
                  : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-primary)]',
            )}
          >
            <option value={0}>Összes minőség</option>
            <option value={70}>Megbízható (≥70)</option>
            <option value={85}>Kiváló (≥85)</option>
          </select>
        </div>
      </div>

      {/* 3. Sor – Forrásportál szűrők (Portal Chips Row) */}
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
                  : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
              )}
            >
              <span>{chip.label}</span>
              <span
                className={cn(
                  'rounded-full px-1.5 py-0.2 text-[10px] font-bold',
                  isActive
                    ? 'bg-white/20 text-white'
                    : 'bg-[var(--bg-card)] text-[var(--text-secondary)]',
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
