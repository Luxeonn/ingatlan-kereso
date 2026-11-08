import type { FC } from 'react'
import {
  EyeOff,
  TrendingDown,
  Sparkles,
  Building2,
  Trash2,
  Layers,
} from 'lucide-react'
import { PORTAL_CHIPS, normalizePortalKey } from '../../utils/formatters'
import { cn } from '../../utils/cn'
import { useUiStore, type QuickFilterId } from '../../store/useUiStore'
import { QuickFilterPopover } from './QuickFilterPopover'

interface HiddenFilterChipProps {
  showHidden?: boolean
  setShowHidden: (val: boolean) => void
  hiddenCount: number
}

const HiddenFilterChip: FC<HiddenFilterChipProps> = ({
  showHidden,
  setShowHidden,
  hiddenCount,
}) => (
  <button
    type="button"
    data-testid="hidden-filter-toggle"
    aria-pressed={showHidden}
    onClick={() => setShowHidden(!showHidden)}
    title="Elrejtett / kukázott hirdetések megtekintése"
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      showHidden
        ? 'border border-purple-500/50 bg-purple-500/20 text-purple-300 shadow-sm ring-1 ring-purple-500/30'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-purple-400 hover:border-purple-500/30',
    )}
  >
    <Trash2 className="h-3.5 w-3.5 text-purple-400 shrink-0" />
    <span className="truncate">Elrejtettek</span>
    {hiddenCount > 0 && (
      <span
        data-testid="hidden-filter-count"
        className={cn(
          'rounded-full px-1.5 py-0.2 text-[10px] font-bold shrink-0',
          showHidden
            ? 'bg-purple-500/30 text-purple-200'
            : 'bg-[var(--bg-card)] text-purple-400',
        )}
      >
        {hiddenCount}
      </span>
    )}
  </button>
)

interface BargainChipProps {
  onlyBargain: boolean
  setOnlyBargain: (val: boolean) => void
  bargainCount: number
}

const BargainFilterChip: FC<BargainChipProps> = ({ onlyBargain, setOnlyBargain, bargainCount }) => (
  <button
    type="button"
    data-testid="bargain-filter-toggle"
    aria-pressed={onlyBargain}
    onClick={() => setOnlyBargain(!onlyBargain)}
    title="Kizárólag alkalmi vételek megjelenítése (mediánnál legalább 20%-kal olcsóbb ingatlanok)"
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      onlyBargain
        ? 'border border-emerald-500/50 bg-emerald-500/15 text-emerald-400 shadow-sm ring-1 ring-emerald-500/30'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-emerald-400 hover:border-emerald-500/30',
    )}
  >
    <TrendingDown className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
    <span className="truncate">Alkalmi vétel</span>
    {bargainCount > 0 && (
      <span
        data-testid="bargain-filter-count"
        className={cn(
          'rounded-full px-1.5 py-0.2 text-[10px] font-bold shrink-0',
          onlyBargain
            ? 'bg-emerald-500/30 text-emerald-200'
            : 'bg-[var(--bg-card)] text-emerald-400',
        )}
      >
        {bargainCount}
      </span>
    )}
  </button>
)

interface OnlyNewChipProps {
  onlyNew: boolean
  setOnlyNew: (val: boolean) => void
}

const OnlyNewFilterChip: FC<OnlyNewChipProps> = ({ onlyNew, setOnlyNew }) => (
  <button
    type="button"
    aria-pressed={onlyNew}
    onClick={() => setOnlyNew(!onlyNew)}
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      onlyNew
        ? 'border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 shadow-sm'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
    )}
  >
    <Sparkles className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
    <span className="truncate">Csak új találatok</span>
  </button>
)

interface ExcludeViewedChipProps {
  excludeViewed?: boolean
  setExcludeViewed: (val: boolean) => void
}

const ExcludeViewedFilterChip: FC<ExcludeViewedChipProps> = ({ excludeViewed, setExcludeViewed }) => (
  <button
    type="button"
    aria-pressed={excludeViewed}
    onClick={() => setExcludeViewed(!excludeViewed)}
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      excludeViewed
        ? 'border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
    )}
  >
    <EyeOff className="h-3.5 w-3.5 text-blue-400 shrink-0" />
    <span className="truncate">Megtekintettek rejtése</span>
  </button>
)

interface OnlyNewConstructionChipProps {
  onlyNewConstruction?: boolean
  setOnlyNewConstruction: (val: boolean) => void
}

const OnlyNewConstructionFilterChip: FC<OnlyNewConstructionChipProps> = ({
  onlyNewConstruction,
  setOnlyNewConstruction,
}) => (
  <button
    type="button"
    aria-pressed={onlyNewConstruction}
    aria-label="Csak új építésű ingatlanok"
    onClick={() => setOnlyNewConstruction(!onlyNewConstruction)}
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      onlyNewConstruction
        ? 'border border-cyan-500/40 bg-cyan-500/15 text-cyan-400 shadow-sm'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
    )}
  >
    <Building2 className="h-3.5 w-3.5 text-cyan-400 shrink-0" />
    <span className="truncate">Csak új építésű</span>
  </button>
)

interface ExcludeMultiSourceChipProps {
  excludeMultiSource?: boolean
  setExcludeMultiSource: (val: boolean) => void
}

const ExcludeMultiSourceFilterChip: FC<ExcludeMultiSourceChipProps> = ({
  excludeMultiSource,
  setExcludeMultiSource,
}) => (
  <button
    type="button"
    data-testid="exclude-multi-source-toggle"
    aria-pressed={excludeMultiSource}
    onClick={() => setExcludeMultiSource(!excludeMultiSource)}
    title="Több közvetítő által hirdetett, összekapcsolt ingatlanok elrejtése"
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      excludeMultiSource
        ? 'border border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm'
        : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
    )}
  >
    <Layers className="h-3.5 w-3.5 text-amber-400 shrink-0" />
    <span className="truncate">Több forrásosak rejtése</span>
  </button>
)

export interface QuickFilterChipsRowProps {
  onlyNew: boolean
  setOnlyNew: (val: boolean) => void
  onlyNewConstruction?: boolean
  setOnlyNewConstruction: (val: boolean) => void
  onlyBargain: boolean
  setOnlyBargain: (val: boolean) => void
  bargainCount: number
  excludeViewed?: boolean
  setExcludeViewed: (val: boolean) => void
  excludeMultiSource?: boolean
  setExcludeMultiSource?: (val: boolean) => void
  showHidden?: boolean
  setShowHidden?: (val: boolean) => void
  hiddenCount?: number
}

export const QuickFilterChipsRow: FC<QuickFilterChipsRowProps> = ({
  onlyNew,
  setOnlyNew,
  onlyNewConstruction,
  setOnlyNewConstruction,
  onlyBargain,
  setOnlyBargain,
  bargainCount,
  excludeViewed,
  setExcludeViewed,
  excludeMultiSource,
  setExcludeMultiSource,
  showHidden,
  setShowHidden,
  hiddenCount = 0,
}) => {
  const { pinnedQuickFilters, togglePinnedQuickFilter } = useUiStore()
  const isPinned = (id: QuickFilterId) => pinnedQuickFilters.includes(id)

  return (
    <div
      data-testid="filter-chips-row"
      className="flex items-center gap-2 border-t border-[var(--border-color)] pt-2.5 text-xs w-full"
    >
      <div className="flex flex-1 items-center gap-2 min-w-0">
        {isPinned('bargain') && (
          <BargainFilterChip
            onlyBargain={onlyBargain}
            setOnlyBargain={setOnlyBargain}
            bargainCount={bargainCount}
          />
        )}

        {isPinned('onlyNew') && (
          <OnlyNewFilterChip onlyNew={onlyNew} setOnlyNew={setOnlyNew} />
        )}

        {isPinned('hidden') && setShowHidden && (
          <HiddenFilterChip
            showHidden={showHidden}
            setShowHidden={setShowHidden}
            hiddenCount={hiddenCount}
          />
        )}

        {isPinned('excludeViewed') && (
          <ExcludeViewedFilterChip
            excludeViewed={excludeViewed}
            setExcludeViewed={setExcludeViewed}
          />
        )}

        {isPinned('onlyNewConstruction') && (
          <OnlyNewConstructionFilterChip
            onlyNewConstruction={onlyNewConstruction}
            setOnlyNewConstruction={setOnlyNewConstruction}
          />
        )}

        {isPinned('excludeMultiSource') && setExcludeMultiSource && (
          <ExcludeMultiSourceFilterChip
            excludeMultiSource={excludeMultiSource}
            setExcludeMultiSource={setExcludeMultiSource}
          />
        )}
      </div>

      <div className="shrink-0">
        <QuickFilterPopover
          pinnedQuickFilters={pinnedQuickFilters}
          onToggleFilter={togglePinnedQuickFilter}
        />
      </div>
    </div>
  )
}

export interface PortalFilterChipsRowProps {
  selectedPortalFilter: string
  setSelectedPortalFilter: (id: string) => void
  portalCounts: Record<string, number>
  searchedPortals?: string[]
}

export const PortalFilterChipsRow: FC<PortalFilterChipsRowProps> = ({
  selectedPortalFilter,
  setSelectedPortalFilter,
  portalCounts,
  searchedPortals,
}) => {
  const activePortalSet =
    searchedPortals && searchedPortals.length > 0
      ? new Set(searchedPortals.map(normalizePortalKey).filter(Boolean))
      : null

  const visibleChips = PORTAL_CHIPS.filter((chip) => {
    if (!activePortalSet) return true
    if (chip.id === 'all') return activePortalSet.size > 1
    return activePortalSet.has(chip.id)
  })

  const effectiveSelectedId =
    activePortalSet && activePortalSet.size === 1
      ? Array.from(activePortalSet)[0]
      : selectedPortalFilter

  return (
    <div
      data-testid="portal-quick-filter-chips"
      className="flex flex-wrap items-center gap-1.5 border-t border-[var(--border-color)] pt-2.5"
    >
      {visibleChips.map((chip) => {
        const count = portalCounts[chip.id] || 0
        const isActive = effectiveSelectedId === chip.id
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
  )
}
