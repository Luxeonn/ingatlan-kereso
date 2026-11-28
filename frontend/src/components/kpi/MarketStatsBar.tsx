import type { FC, ReactNode } from 'react'
import { Building2, Sparkles, TrendingDown } from 'lucide-react'
import type { DashboardResponseDto } from '../../types/api'
import type { IngatlanItem } from '../../types/property'
import { isItemBargain, isListingNew } from '../../utils/listingFilters'
import { formatPricePerSqm, getTotalSourceListingsCount } from '../../utils/formatters'
import { useFilterStore } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

export interface MultiSourceNoticeProps {
  itemsCount: number
  totalSourceListingsCount: number
  serverFilteredCount?: number
  medianPricePerSqm?: number | null
}

export const MultiSourceNotice: FC<MultiSourceNoticeProps> = ({
  itemsCount,
  totalSourceListingsCount,
  serverFilteredCount,
  medianPricePerSqm,
}) => {
  const mergedDuplicatesCount = Math.max(0, totalSourceListingsCount - itemsCount)
  const hasFiltered = serverFilteredCount !== undefined && serverFilteredCount > 0
  const hasMedian =
    medianPricePerSqm !== null && medianPricePerSqm !== undefined && medianPricePerSqm > 0
  if (mergedDuplicatesCount === 0 && !hasFiltered && !hasMedian) return null

  return (
    <div
      data-testid="multi-source-summary-notice"
      className="inline-flex items-center gap-1.5 rounded-full border border-[#0071e3]/20 bg-[#0071e3]/8 px-3 py-1 text-xs text-[#005bb5] tabular-nums"
    >
      <span>
        <strong className="font-bold text-[#005bb5]">{itemsCount} egyedi ingatlan</strong>
        {mergedDuplicatesCount > 0 && (
          <> ({totalSourceListingsCount} hirdetésből · {mergedDuplicatesCount} összevonva)</>
        )}
        {hasFiltered && (
          <> · <strong className="font-bold text-[#005bb5]">{serverFilteredCount} szűrt</strong></>
        )}
        {hasMedian && (
          <> · Medián m² ár: <strong className="font-bold text-[#005bb5]">{formatPricePerSqm(medianPricePerSqm)}</strong></>
        )}
      </span>
    </div>
  )
}

interface MarketStatsBarProps {
  data: DashboardResponseDto | null
  sessionViewedUrls?: ReadonlySet<string>
  rawItems?: IngatlanItem[]
  serverFilteredCount?: number
  medianPricePerSqm?: number | null
  showMultiSourceSummary?: boolean
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

interface KpiPillButtonProps {
  testId: string
  label: string
  count: number
  isActive: boolean
  activeLabel: string
  inactiveLabel: string
  activeClasses: string
  iconBoxClass: string
  activePillClass: string
  icon: ReactNode
  onClick: () => void
}

const KpiPillButton: FC<KpiPillButtonProps> = ({
  testId,
  label,
  count,
  isActive,
  activeLabel,
  inactiveLabel,
  activeClasses,
  iconBoxClass,
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
      'inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-left transition-all cursor-pointer whitespace-nowrap',
      isActive
        ? activeClasses
        : 'border-[var(--border-hairline)] bg-white/75 text-[var(--text-primary)] hover:border-[#0071e3]/35',
    )}
  >
    <span className={cn('flex h-6 w-6 shrink-0 items-center justify-center rounded-full', iconBoxClass)}>
      {icon}
    </span>
    <span className="text-xs font-semibold text-[var(--text-secondary)]">{label}</span>
    <span className="text-xs font-extrabold text-[var(--text-primary)] tabular-nums">{count} db</span>
    <span
      className={cn(
        'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none',
        isActive ? activePillClass : 'bg-black/5 text-[var(--text-secondary)]',
      )}
    >
      {isActive ? activeLabel : inactiveLabel}
    </span>
  </button>
)

export const MarketStatsBar: FC<MarketStatsBarProps> = ({
  data,
  sessionViewedUrls,
  rawItems,
  serverFilteredCount,
  medianPricePerSqm,
  showMultiSourceSummary = true,
}) => {
  const { onlyNew, setOnlyNew, onlyBargain, setOnlyBargain } = useFilterStore()
  const { setDashboardTab, setListingSubView } = useUiStore()

  if (!data) return null

  const { total, newCount, bargainCount } = resolveActiveKpiCounts(data, sessionViewedUrls)
  const isAllActive = !onlyNew && !onlyBargain
  const summaryItems = rawItems ?? data.items ?? []
  const totalSourceListingsCount = getTotalSourceListingsCount(summaryItems)

  const focusListingsView = () => {
    setDashboardTab('listings')
    setListingSubView('listings')
  }

  return (
    <div
      data-testid="market-stats-bar"
      role="region"
      aria-label="Piaci gyorsösszegző szűrők"
      className="flex flex-wrap items-center justify-between gap-2.5 rounded-2xl glass-surface px-3.5 py-2"
    >
      <div className="flex flex-wrap items-center gap-2">
        <KpiPillButton
          testId="kpi-filter-all"
          label="Összes Találat"
          count={total}
          isActive={isAllActive}
          activeLabel="Aktív"
          inactiveLabel="Mind"
          activeClasses="border-[#0071e3]/45 bg-[#0071e3]/10 ring-1 ring-[#0071e3]/20"
          iconBoxClass="bg-[#0071e3]/12 text-[#0071e3]"
          activePillClass="bg-[#0071e3] text-white"
          icon={<Building2 className="h-3.5 w-3.5" />}
          onClick={() => {
            setOnlyNew(false)
            setOnlyBargain(false)
            focusListingsView()
          }}
        />

        <KpiPillButton
          testId="kpi-filter-new"
          label="Új Felfedezés"
          count={newCount}
          isActive={onlyNew}
          activeLabel="Szűrt"
          inactiveLabel="Szűrés"
          activeClasses="border-emerald-500/45 bg-emerald-500/10 ring-1 ring-emerald-500/20"
          iconBoxClass="bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]"
          activePillClass="bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]"
          icon={<Sparkles className="h-3.5 w-3.5" />}
          onClick={() => {
            const next = !onlyNew
            setOnlyNew(next)
            if (next) focusListingsView()
          }}
        />

        <KpiPillButton
          testId="kpi-filter-bargain"
          label="Alkalmi Vételek"
          count={bargainCount}
          isActive={onlyBargain}
          activeLabel="Szűrt"
          inactiveLabel="Szűrés"
          activeClasses="border-amber-500/45 bg-amber-500/10 ring-1 ring-amber-500/20"
          iconBoxClass="bg-amber-500/12 text-amber-700"
          activePillClass="bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)]"
          icon={<TrendingDown className="h-3.5 w-3.5" />}
          onClick={() => {
            const next = !onlyBargain
            setOnlyBargain(next)
            if (next) focusListingsView()
          }}
        />
      </div>

      {showMultiSourceSummary && (
        <MultiSourceNotice
          itemsCount={summaryItems.length}
          totalSourceListingsCount={totalSourceListingsCount}
          serverFilteredCount={serverFilteredCount}
          medianPricePerSqm={medianPricePerSqm}
        />
      )}
    </div>
  )
}
