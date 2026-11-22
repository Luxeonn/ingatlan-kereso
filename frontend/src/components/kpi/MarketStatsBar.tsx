import type { FC } from 'react'
import { Building2, Sparkles, TrendingDown, Ruler } from 'lucide-react'
import type { DashboardResponseDto } from '../../types/api'
import { formatPricePerSqm } from '../../utils/formatters'
import { isItemBargain } from '../../utils/listingFilters'

interface MarketStatsBarProps {
  data: DashboardResponseDto | null
}

function resolveBargainCount(data: DashboardResponseDto): number {
  const statsBargainCount = data.marketStats?.bargainCount
  if (statsBargainCount !== null && statsBargainCount !== undefined) {
    return statsBargainCount
  }
  return data.items ? data.items.filter(isItemBargain).length : 0
}

function resolveMedianSqmPrice(stats: DashboardResponseDto['marketStats']): number | undefined {
  return stats?.medianPricePerM2 ?? stats?.residentialMedianPricePerM2 ?? stats?.plotMedianPricePerM2
}

export const MarketStatsBar: FC<MarketStatsBarProps> = ({ data }) => {
  if (!data) return null

  const total = data.totalFound ?? data.items?.length ?? 0
  const newCount = data.newCount ?? 0
  const bargainCount = resolveBargainCount(data)
  const medianSqmPrice = resolveMedianSqmPrice(data.marketStats)

  return (
    <div
      data-testid="market-stats-bar"
      role="region"
      aria-label="Piaci gyorsösszegző mutatók"
      className="grid grid-cols-2 gap-3 sm:grid-cols-2 lg:grid-cols-4"
    >
      {/* 1. Total count */}
      <div className="flex items-center gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-md">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 text-blue-600">
          <Building2 className="h-5 w-5" />
        </div>
        <div>
          <p className="text-[11px] font-semibold text-[var(--text-secondary)]">Összes Találat</p>
          <p className="text-lg font-extrabold text-[var(--text-primary)]">{total} db</p>
        </div>
      </div>

      {/* 2. New items count */}
      <div
        className={`flex items-center gap-3 rounded-xl border p-3.5 shadow-md ${
          newCount > 0
            ? 'border-emerald-500/35 bg-[var(--bg-card)]'
            : 'border-[var(--border-color)] bg-[var(--bg-card)]'
        }`}
      >
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]">
          <Sparkles className={newCount > 0 ? 'h-5 w-5 animate-pulse' : 'h-5 w-5'} />
        </div>
        <div>
          <p className="text-[11px] font-semibold text-[var(--text-secondary)]">Új Felfedezés</p>
          <p
            className={`text-lg font-extrabold tabular-nums ${
              newCount > 0 ? 'text-[var(--status-bargain-fg)]' : 'text-[var(--text-primary)]'
            }`}
          >
            {newCount} db
          </p>
        </div>
      </div>

      {/* 3. Bargain Count */}
      <div className="flex items-center gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-md">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-700">
          <TrendingDown className="h-5 w-5" />
        </div>
        <div>
          <p className="text-[11px] font-semibold text-[var(--text-secondary)]">Alkalmi Vételek</p>
          <p className="text-lg font-extrabold text-[var(--text-primary)]">{bargainCount} db</p>
        </div>
      </div>

      {/* 4. Median Price per m² */}
      <div className="flex items-center gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-md">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-purple-500/10 text-purple-700">
          <Ruler className="h-5 w-5" />
        </div>
        <div>
          <p className="text-[11px] font-semibold text-[var(--text-secondary)]">Medián m² ár</p>
          <p className="text-lg font-extrabold text-[var(--text-primary)]">
            {medianSqmPrice && medianSqmPrice > 0 ? formatPricePerSqm(medianSqmPrice) : 'N/A'}
          </p>
        </div>
      </div>
    </div>
  )
}
