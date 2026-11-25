import type { FC, ReactNode } from 'react'
import { Building2, Sparkles, TrendingDown } from 'lucide-react'
import type { DashboardResponseDto } from '../../types/api'
import { isItemBargain, isListingNew } from '../../utils/listingFilters'
import { useFilterStore } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface MarketStatsBarProps {
  data: DashboardResponseDto | null
  sessionViewedUrls?: ReadonlySet<string>
}

function resolveActiveKpiCounts(
  data: DashboardResponseDto,
  sessionViewedUrls?: ReadonlySet<string>,
): { total: number; newCount: number; bargainCount: number } {
  const items = data.items
  if (Array.isArray(items) && items.length > 0) {
    const activeItems = items.filter((item) => !item.hidden)
    return {
      total: activeItems.length,
      newCount: activeItems.filter((item) => isListingNew(item, sessionViewedUrls)).length,
      bargainCount: activeItems.filter(isItemBargain).length,
    }
  }
  const statsBargainCount = data.marketStats?.bargainCount
  return {
    total: data.totalFound ?? 0,
    newCount: data.newCount ?? 0,
    bargainCount:
      statsBargainCount !== null && statsBargainCount !== undefined ? statsBargainCount : 0,
  }
}

interface KpiCardButtonProps {
  testId: string
  label: string
  count: number
  isActive: boolean
  activeLabel: string
  inactiveLabel: string
  activeRingClass: string
  hoverBorderClass: string
  iconBgClass: string
  valueClass?: string
  activePillClass: string
  icon: ReactNode
  onClick: () => void
}

const KpiCardButton: FC<KpiCardButtonProps> = ({
  testId,
  label,
  count,
  isActive,
  activeLabel,
  inactiveLabel,
  activeRingClass,
  hoverBorderClass,
  iconBgClass,
  valueClass = 'text-[var(--text-primary)]',
  activePillClass,
  icon,
  onClick,
}) => (
  <button
    type="button"
    data-testid={testId}
    aria-pressed={isActive}
    onClick={onClick}
    className={cn(
      'flex items-center justify-between gap-3 rounded-xl border bg-[var(--bg-card)] p-3.5 text-left shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg cursor-pointer',
      isActive ? activeRingClass : cn('border-[var(--border-color)]', hoverBorderClass),
    )}
  >
    <div className="flex items-center gap-3">
      <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-lg', iconBgClass)}>
        {icon}
      </div>
      <div>
        <p className="text-[11px] font-semibold text-[var(--text-secondary)]">{label}</p>
        <p className={cn('text-lg font-extrabold tabular-nums', valueClass)}>{count} db</p>
      </div>
    </div>
    <span
      className={cn(
        'rounded-full px-2 py-0.5 text-[10px] font-bold',
        isActive ? activePillClass : 'bg-[var(--bg-input)] text-[var(--text-secondary)]',
      )}
    >
      {isActive ? activeLabel : inactiveLabel}
    </span>
  </button>
)

export const MarketStatsBar: FC<MarketStatsBarProps> = ({ data, sessionViewedUrls }) => {
  const { onlyNew, setOnlyNew, onlyBargain, setOnlyBargain } = useFilterStore()
  const { setDashboardTab, setListingSubView } = useUiStore()

  if (!data) return null

  const { total, newCount, bargainCount } = resolveActiveKpiCounts(data, sessionViewedUrls)
  const isAllActive = !onlyNew && !onlyBargain

  const focusListingsView = () => {
    setDashboardTab('listings')
    setListingSubView('listings')
  }

  return (
    <div
      data-testid="market-stats-bar"
      role="region"
      aria-label="Piaci gyorsösszegző szűrők"
      className="grid grid-cols-1 gap-3 sm:grid-cols-3"
    >
      <KpiCardButton
        testId="kpi-filter-all"
        label="Összes Találat"
        count={total}
        isActive={isAllActive}
        activeLabel="Aktív"
        inactiveLabel="Mind"
        activeRingClass="border-blue-500/60 ring-2 ring-blue-500/25"
        hoverBorderClass="hover:border-blue-500/40"
        iconBgClass="bg-blue-500/10 text-blue-600"
        activePillClass="bg-blue-500/15 text-blue-700"
        icon={<Building2 className="h-5 w-5" />}
        onClick={() => {
          setOnlyNew(false)
          setOnlyBargain(false)
          focusListingsView()
        }}
      />

      <KpiCardButton
        testId="kpi-filter-new"
        label="Új Felfedezés"
        count={newCount}
        isActive={onlyNew}
        activeLabel="Szűrt"
        inactiveLabel="Szűrés"
        activeRingClass="border-emerald-500/60 ring-2 ring-emerald-500/25"
        hoverBorderClass="hover:border-emerald-500/40"
        iconBgClass="bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]"
        valueClass={newCount > 0 ? 'text-[var(--status-bargain-fg)]' : 'text-[var(--text-primary)]'}
        activePillClass="bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]"
        icon={<Sparkles className={newCount > 0 ? 'h-5 w-5 animate-pulse' : 'h-5 w-5'} />}
        onClick={() => {
          const next = !onlyNew
          setOnlyNew(next)
          if (next) focusListingsView()
        }}
      />

      <KpiCardButton
        testId="kpi-filter-bargain"
        label="Alkalmi Vételek"
        count={bargainCount}
        isActive={onlyBargain}
        activeLabel="Szűrt"
        inactiveLabel="Szűrés"
        activeRingClass="border-amber-500/60 ring-2 ring-amber-500/25"
        hoverBorderClass="hover:border-amber-500/40"
        iconBgClass="bg-amber-500/10 text-amber-700"
        activePillClass="bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)]"
        icon={<TrendingDown className="h-5 w-5" />}
        onClick={() => {
          const next = !onlyBargain
          setOnlyBargain(next)
          if (next) focusListingsView()
        }}
      />
    </div>
  )
}
