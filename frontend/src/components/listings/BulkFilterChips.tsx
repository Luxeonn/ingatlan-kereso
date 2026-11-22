import type { FC, ReactNode } from 'react'
import {
  EyeOff,
  TrendingDown,
  Sparkles,
  Building2,
  Trash2,
  Layers,
  ImageOff,
  ArrowLeftRight,
} from 'lucide-react'
import { cn } from '../../utils/cn'
import { useUiStore, type QuickFilterId } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import { QuickFilterPopover } from './QuickFilterPopover'
export { PortalFilterChipsRow, type PortalFilterChipsRowProps } from './PortalFilterChipsRow'
import {
  cycleTriStateMode,
  getTriStateMode,
  resolveTriStateChipLabel,
} from './quickFilterTriState'

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
        'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none tabular-nums shrink-0',
        isActive ? activeClass : inactiveClass,
      )}
    >
      {count}
    </span>
  )
}

interface QuickFilterToggleButtonProps {
  filterId: QuickFilterId
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

const TRI_STATE_CONTAINER_CLASSES: Record<'all' | 'only' | 'exclude', string> = {
  all: 'border border-[var(--border-hairline)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-blue-500/40',
  only: 'border border-blue-500/50 bg-blue-500/15 text-blue-700 shadow-sm ring-1 ring-blue-500/20',
  exclude: 'border border-rose-500/50 bg-rose-500/15 text-rose-700 shadow-sm ring-1 ring-rose-500/20',
}

const TRI_STATE_BADGE_CLASSES: Record<'all' | 'only' | 'exclude', string> = {
  all: 'bg-[var(--bg-input)] text-[var(--text-primary)]',
  only: 'bg-blue-600 text-white',
  exclude: 'bg-rose-600 text-white',
}

const QuickFilterToggleButton: FC<QuickFilterToggleButtonProps> = ({
  filterId,
  testId,
  isActive,
  onClick,
  title,
  ariaLabel,
  icon,
  label,
  count,
  countTestId,
}) => {
  const mode = getTriStateMode(filterId)
  const effectiveLabel = resolveTriStateChipLabel(filterId, mode, label)
  const isInverseMode =
    (filterId === 'hidden' && mode === 'all') ||
    (filterId !== 'hidden' &&
      filterId !== 'excludeMultiSource' &&
      filterId !== 'onlyMultiSource' &&
      !isActive &&
      mode !== 'all')

  const containerClass = TRI_STATE_CONTAINER_CLASSES[mode]
  const badgeClass = TRI_STATE_BADGE_CLASSES[mode]

  return (
    <div
      className={cn(
        'flex h-8 flex-1 min-w-[150px] items-stretch overflow-hidden rounded-lg transition-all',
        containerClass,
      )}
    >
      <button
        type="button"
        data-testid={testId}
        aria-pressed={Boolean(isActive || isInverseMode)}
        aria-label={ariaLabel}
        onClick={onClick}
        title={title}
        className="flex flex-1 min-w-0 items-center justify-center gap-1.5 whitespace-nowrap px-2.5 text-xs font-bold cursor-pointer"
      >
        {icon}
        <span className="truncate">{effectiveLabel}</span>
        <QuickFilterCountBadge
          count={count}
          testId={countTestId}
          isActive={mode !== 'all'}
          activeClass={badgeClass}
          inactiveClass={TRI_STATE_BADGE_CLASSES.all}
        />
      </button>
      <button
        type="button"
        data-testid={`quick-filter-cycle-${filterId}`}
        aria-label="Szűrőmód váltása (Mind / Csak / Kizár)"
        title="Állapot váltása: Mind ↔ Csak ez ↔ Kizárva"
        onClick={(e) => {
          e.stopPropagation()
          cycleTriStateMode(filterId)
        }}
        className="flex items-center justify-center border-l border-current/20 px-1.5 opacity-70 hover:opacity-100 hover:bg-black/5 transition-opacity cursor-pointer shrink-0"
      >
        <ArrowLeftRight className="h-3 w-3" />
      </button>
    </div>
  )
}

export interface QuickFilterChipsRowProps {
  onlyNew: boolean
  setOnlyNew: (val: boolean) => void
  onlyNewCount?: number
  onlyNewConstruction?: boolean
  setOnlyNewConstruction: (val: boolean) => void
  onlyNewConstructionCount?: number
  onlyBargain: boolean
  setOnlyBargain: (val: boolean) => void
  bargainCount: number
  excludeViewed?: boolean
  setExcludeViewed: (val: boolean) => void
  excludeViewedCount?: number
  excludeMultiSource?: boolean
  setExcludeMultiSource?: (val: boolean) => void
  excludeMultiSourceCount?: number
  onlyMultiSource?: boolean
  setOnlyMultiSource?: (val: boolean) => void
  onlyMultiSourceCount?: number
  excludeNoImage?: boolean
  setExcludeNoImage?: (val: boolean) => void
  excludeNoImageCount?: number
  showHidden?: boolean
  setShowHidden?: (val: boolean) => void
  hiddenCount?: number
}

export const QuickFilterChipsRow: FC<QuickFilterChipsRowProps> = ({
  onlyNew,
  setOnlyNew,
  onlyNewCount,
  onlyNewConstruction,
  setOnlyNewConstruction,
  onlyNewConstructionCount,
  onlyBargain,
  setOnlyBargain,
  bargainCount,
  excludeViewed,
  setExcludeViewed,
  excludeViewedCount,
  excludeMultiSource,
  setExcludeMultiSource,
  excludeMultiSourceCount,
  onlyMultiSource,
  setOnlyMultiSource,
  onlyMultiSourceCount,
  excludeNoImage,
  setExcludeNoImage,
  excludeNoImageCount,
  showHidden,
  setShowHidden,
  hiddenCount,
}) => {
  const { pinnedQuickFilters, togglePinnedQuickFilter } = useUiStore()
  useFilterStore()
  const isPinned = (id: QuickFilterId) => pinnedQuickFilters.includes(id)

  return (
    <div
      data-testid="filter-chips-row"
      className="flex flex-wrap items-center justify-between gap-2 border-t border-[var(--border-hairline)] pt-2.5 text-xs w-full"
    >
      <div className="flex flex-1 flex-wrap items-center gap-2 min-w-0">
        {isPinned('bargain') && (
          <QuickFilterToggleButton
            filterId="bargain"
            testId="bargain-filter-toggle"
            isActive={onlyBargain}
            onClick={() => setOnlyBargain(!onlyBargain)}
            title="Kizárólag alkalmi vételek megjelenítése (mediánnál legalább 20%-kal olcsóbb ingatlanok)"
            activeClassName="border border-emerald-500/50 bg-emerald-500/15 text-emerald-700 shadow-sm ring-1 ring-emerald-500/30"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-emerald-700 hover:border-emerald-500/30"
            icon={<TrendingDown className="h-3.5 w-3.5 text-emerald-700 shrink-0" />}
            label="Alkalmi vétel"
            count={bargainCount}
            countTestId="bargain-filter-count"
            activeBadgeClass="bg-emerald-500/30 text-emerald-700"
            inactiveBadgeClass="bg-[var(--bg-card)] text-emerald-700"
          />
        )}

        {isPinned('onlyNew') && (
          <QuickFilterToggleButton
            filterId="onlyNew"
            isActive={onlyNew}
            onClick={() => setOnlyNew(!onlyNew)}
            activeClassName="border border-emerald-500/40 bg-emerald-500/15 text-emerald-700 shadow-sm"
            icon={<Sparkles className="h-3.5 w-3.5 text-emerald-700 shrink-0" />}
            label="Új találatok"
            count={onlyNewCount}
            countTestId="only-new-filter-count"
            activeBadgeClass="bg-emerald-500/20 text-emerald-700"
          />
        )}

        {isPinned('hidden') && setShowHidden && (
          <QuickFilterToggleButton
            filterId="hidden"
            testId="hidden-filter-toggle"
            isActive={showHidden}
            onClick={() => setShowHidden(!showHidden)}
            title="Törölt / kukázott hirdetések megtekintése"
            activeClassName="border border-purple-500/50 bg-purple-500/20 text-purple-700 shadow-sm ring-1 ring-purple-500/30"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-purple-700 hover:border-purple-500/30"
            icon={<Trash2 className="h-3.5 w-3.5 text-purple-700 shrink-0" />}
            label="Töröltek"
            count={hiddenCount}
            countTestId="hidden-filter-count"
            activeBadgeClass="bg-purple-500/30 text-purple-700"
            inactiveBadgeClass="bg-[var(--bg-card)] text-purple-700"
          />
        )}

        {isPinned('excludeViewed') && (
          <QuickFilterToggleButton
            filterId="excludeViewed"
            isActive={excludeViewed}
            onClick={() => setExcludeViewed(!excludeViewed)}
            activeClassName="border border-blue-500/40 bg-blue-500/15 text-blue-600 shadow-sm"
            icon={<EyeOff className="h-3.5 w-3.5 text-blue-600 shrink-0" />}
            label="Megtekintettek"
            count={excludeViewedCount}
            countTestId="exclude-viewed-filter-count"
            activeBadgeClass="bg-blue-500/20 text-blue-600"
          />
        )}

        {isPinned('onlyNewConstruction') && (
          <QuickFilterToggleButton
            filterId="onlyNewConstruction"
            isActive={onlyNewConstruction}
            ariaLabel="Új építésű ingatlanok"
            onClick={() => setOnlyNewConstruction(!onlyNewConstruction)}
            activeClassName="border border-cyan-500/40 bg-cyan-500/15 text-cyan-700 shadow-sm"
            icon={<Building2 className="h-3.5 w-3.5 text-cyan-700 shrink-0" />}
            label="Új építésű"
            count={onlyNewConstructionCount}
            countTestId="only-new-construction-filter-count"
            activeBadgeClass="bg-cyan-500/20 text-cyan-700"
          />
        )}

        {isPinned('excludeMultiSource') && setExcludeMultiSource && (
          <QuickFilterToggleButton
            filterId="excludeMultiSource"
            testId="exclude-multi-source-toggle"
            isActive={excludeMultiSource}
            onClick={() => setExcludeMultiSource(!excludeMultiSource)}
            title="Több portálon szereplő, összevont ingatlanok elrejtése"
            activeClassName="border border-amber-500/40 bg-amber-500/15 text-amber-700 shadow-sm"
            icon={<Layers className="h-3.5 w-3.5 text-amber-700 shrink-0" />}
            label="Összevontak nélkül"
            count={excludeMultiSourceCount}
            countTestId="exclude-multi-source-filter-count"
            activeBadgeClass="bg-amber-500/20 text-amber-700"
          />
        )}

        {isPinned('onlyMultiSource') && setOnlyMultiSource && (
          <QuickFilterToggleButton
            filterId="onlyMultiSource"
            testId="only-multi-source-toggle"
            isActive={onlyMultiSource}
            onClick={() => setOnlyMultiSource(!onlyMultiSource)}
            title="Több portálon szereplő, összevont ingatlanok szűrése"
            activeClassName="border border-indigo-500/40 bg-indigo-500/15 text-indigo-700 shadow-sm"
            inactiveClassName="border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-indigo-700 hover:border-indigo-500/30"
            icon={<Layers className="h-3.5 w-3.5 text-indigo-700 shrink-0" />}
            label="Összevontak"
            count={onlyMultiSourceCount}
            countTestId="only-multi-source-filter-count"
            activeBadgeClass="bg-indigo-500/20 text-indigo-700"
          />
        )}

        {isPinned('excludeNoImage') && setExcludeNoImage && (
          <QuickFilterToggleButton
            filterId="excludeNoImage"
            testId="exclude-no-image-filter"
            isActive={excludeNoImage}
            onClick={() => setExcludeNoImage(!excludeNoImage)}
            title="Kép nélküli vagy placeholder képpel rendelkező hirdetések szűrése"
            activeClassName="border border-rose-500/40 bg-rose-500/15 text-rose-700 shadow-sm"
            icon={<ImageOff className="h-3.5 w-3.5 text-rose-700 shrink-0" />}
            label="Kép nélküliek"
            count={excludeNoImageCount}
            countTestId="exclude-no-image-filter-count"
            activeBadgeClass="bg-rose-500/20 text-rose-700"
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
