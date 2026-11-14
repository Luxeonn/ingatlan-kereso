import type { FC, ReactNode } from 'react'
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

interface QuickFilterCountBadgeProps {
  count?: number
  testId?: string
  activeClass: string
  inactiveClass: string
  isActive?: boolean
}

const QuickFilterCountBadge: FC<QuickFilterCountBadgeProps> = ({
  count,
  testId,
  activeClass,
  inactiveClass,
  isActive,
}) => {
  if (!count || count <= 0) return null
  return (
    <span
      data-testid={testId}
      className={cn(
        'rounded-full px-1.5 py-0.2 text-[10px] font-bold shrink-0',
        isActive ? activeClass : inactiveClass,
      )}
    >
      {count}
    </span>
  )
}

interface QuickFilterToggleButtonProps {
  testId?: string
  isActive?: boolean
  onClick: () => void
  title?: string
  ariaLabel?: string
  activeClassName: string
  inactiveClassName?: string
  icon: ReactNode
  label: string
  count?: number
  countTestId?: string
  activeBadgeClass?: string
  inactiveBadgeClass?: string
}

const QuickFilterToggleButton: FC<QuickFilterToggleButtonProps> = ({
  testId,
  isActive,
  onClick,
  title,
  ariaLabel,
  activeClassName,
  inactiveClassName = 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
  icon,
  label,
  count,
  countTestId,
  activeBadgeClass = 'bg-white/20 text-white',
  inactiveBadgeClass = 'bg-[var(--bg-card)] text-[var(--text-secondary)]',
}) => (
  <button
    type="button"
    data-testid={testId}
    aria-pressed={isActive}
    aria-label={ariaLabel}
    onClick={onClick}
    title={title}
    className={cn(
      'flex flex-1 min-w-0 items-center justify-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
      isActive ? activeClassName : inactiveClassName,
    )}
  >
    {icon}
    <span className="truncate">{label}</span>
    <QuickFilterCountBadge
      count={count}
      testId={countTestId}
      isActive={isActive}
      activeClass={activeBadgeClass}
      inactiveClass={inactiveBadgeClass}
    />
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
  onlyMultiSource?: boolean
  setOnlyMultiSource?: (val: boolean) => void
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
  onlyMultiSource,
  setOnlyMultiSource,
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
          <QuickFilterToggleButton
            testId="bargain-filter-toggle"
            isActive={onlyBargain}
            onClick={() => setOnlyBargain(!onlyBargain)}
            title="Kizárólag alkalmi vételek megjelenítése (mediánnál legalább 20%-kal olcsóbb ingatlanok)"
            activeClassName="border border-emerald-500/50 bg-emerald-500/15 text-emerald-400 shadow-sm ring-1 ring-emerald-500/30"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-emerald-400 hover:border-emerald-500/30"
            icon={<TrendingDown className="h-3.5 w-3.5 text-emerald-400 shrink-0" />}
            label="Alkalmi vétel"
            count={bargainCount}
            countTestId="bargain-filter-count"
            activeBadgeClass="bg-emerald-500/30 text-emerald-200"
            inactiveBadgeClass="bg-[var(--bg-card)] text-emerald-400"
          />
        )}

        {isPinned('onlyNew') && (
          <QuickFilterToggleButton
            isActive={onlyNew}
            onClick={() => setOnlyNew(!onlyNew)}
            activeClassName="border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 shadow-sm"
            icon={<Sparkles className="h-3.5 w-3.5 text-emerald-400 shrink-0" />}
            label="Csak új találatok"
          />
        )}

        {isPinned('hidden') && setShowHidden && (
          <QuickFilterToggleButton
            testId="hidden-filter-toggle"
            isActive={showHidden}
            onClick={() => setShowHidden(!showHidden)}
            title="Elrejtett / kukázott hirdetések megtekintése"
            activeClassName="border border-purple-500/50 bg-purple-500/20 text-purple-300 shadow-sm ring-1 ring-purple-500/30"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-purple-400 hover:border-purple-500/30"
            icon={<Trash2 className="h-3.5 w-3.5 text-purple-400 shrink-0" />}
            label="Elrejtettek"
            count={hiddenCount}
            countTestId="hidden-filter-count"
            activeBadgeClass="bg-purple-500/30 text-purple-200"
            inactiveBadgeClass="bg-[var(--bg-card)] text-purple-400"
          />
        )}

        {isPinned('excludeViewed') && (
          <QuickFilterToggleButton
            isActive={excludeViewed}
            onClick={() => setExcludeViewed(!excludeViewed)}
            activeClassName="border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm"
            icon={<EyeOff className="h-3.5 w-3.5 text-blue-400 shrink-0" />}
            label="Megtekintettek rejtése"
          />
        )}

        {isPinned('onlyNewConstruction') && (
          <QuickFilterToggleButton
            isActive={onlyNewConstruction}
            ariaLabel="Csak új építésű ingatlanok"
            onClick={() => setOnlyNewConstruction(!onlyNewConstruction)}
            activeClassName="border border-cyan-500/40 bg-cyan-500/15 text-cyan-400 shadow-sm"
            icon={<Building2 className="h-3.5 w-3.5 text-cyan-400 shrink-0" />}
            label="Csak új építésű"
          />
        )}

        {isPinned('excludeMultiSource') && setExcludeMultiSource && (
          <QuickFilterToggleButton
            testId="exclude-multi-source-toggle"
            isActive={excludeMultiSource}
            onClick={() => setExcludeMultiSource(!excludeMultiSource)}
            title="Több közvetítő által hirdetett, összekapcsolt ingatlanok elrejtése"
            activeClassName="border border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm"
            icon={<Layers className="h-3.5 w-3.5 text-amber-400 shrink-0" />}
            label="Több forrásosak rejtése"
          />
        )}

        {isPinned('onlyMultiSource') && setOnlyMultiSource && (
          <QuickFilterToggleButton
            testId="only-multi-source-toggle"
            isActive={onlyMultiSource}
            onClick={() => setOnlyMultiSource(!onlyMultiSource)}
            title="Kizárólag több portálon / forrásban szereplő, összevont ingatlanok megjelenítése"
            activeClassName="border border-indigo-500/40 bg-indigo-500/15 text-indigo-400 shadow-sm"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-indigo-400 hover:border-indigo-500/30"
            icon={<Layers className="h-3.5 w-3.5 text-indigo-400 shrink-0" />}
            label="Csak több forrásosak"
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
