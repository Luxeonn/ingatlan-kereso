import type { FC } from 'react'
import {
  Building2,
  Home,
  Trees,
  DollarSign,
  TrendingUp,
  TrendingDown,
  Sparkles,
} from 'lucide-react'
import type { MarketAnalyticsData } from '../../hooks/useMarketAnalytics'
import {
  formatPriceMillion,
  formatPricePerSqm,
} from '../../utils/formatters'

export interface CategoryCardDef {
  id: 'lakas' | 'haz' | 'telek'
  testId: string
  title: string
  Icon: typeof Building2
  iconColor: string
  highlightBorder: string
  value: string
  sampleSize: number
  sampleLabel: string
}

function buildCategoryCards(kpi: MarketAnalyticsData['kpi']): CategoryCardDef[] {
  return [
    {
      id: 'lakas' as const,
      testId: 'kpi-card-lakas',
      title: 'Lakás Medián Fajlagos Ár',
      Icon: Building2,
      iconColor: 'bg-[#0071e3]/12 text-[#0071e3]',
      highlightBorder: 'border-[#0071e3]/45! outline-1 outline-[#0071e3]/25',
      value:
        kpi.lakasMedianPricePerSqm !== null && kpi.lakasMedianPricePerSqm !== undefined
          ? formatPricePerSqm(kpi.lakasMedianPricePerSqm)
          : 'Kevés minta (<5)',
      sampleSize: kpi.lakasSampleSize,
      sampleLabel: `${kpi.lakasSampleSize} db lakás alapján`,
    },
    {
      id: 'haz' as const,
      testId: 'kpi-card-haz',
      title: 'Ház Medián Fajlagos Ár',
      Icon: Home,
      iconColor: 'bg-amber-500/12 text-[var(--status-warn-fg)]',
      highlightBorder: 'border-amber-500/45! outline-1 outline-amber-500/25',
      value:
        kpi.hazMedianPricePerSqm !== null && kpi.hazMedianPricePerSqm !== undefined
          ? formatPricePerSqm(kpi.hazMedianPricePerSqm)
          : 'Kevés minta (<5)',
      sampleSize: kpi.hazSampleSize,
      sampleLabel: `${kpi.hazSampleSize} db ház alapján`,
    },
    {
      id: 'telek' as const,
      testId: 'kpi-card-telek',
      title: 'Telek Medián Telekár (m²)',
      Icon: Trees,
      iconColor: 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]',
      highlightBorder: 'border-emerald-500/45! outline-1 outline-emerald-500/25',
      value:
        kpi.telekMedianPricePerSqm !== null && kpi.telekMedianPricePerSqm !== undefined
          ? formatPricePerSqm(kpi.telekMedianPricePerSqm)
          : 'Kevés minta (<5)',
      sampleSize: kpi.telekSampleSize,
      sampleLabel: `${kpi.telekSampleSize} db telek alapján`,
    },
  ]
}

function getVisibleCategoryCards(cards: CategoryCardDef[], activeCategory?: string | null): CategoryCardDef[] {
  return cards
    .filter((card) => card.sampleSize > 0)
    .sort((a, b) => {
      if (a.id === activeCategory) return -1
      if (b.id === activeCategory) return 1
      return 0
    })
}

export interface MarketKpiGridProps {
  totalCount: number
  cityCount: number
  kpi: MarketAnalyticsData['kpi']
}

export const MarketKpiGrid: FC<MarketKpiGridProps> = ({ totalCount, cityCount, kpi }) => {
  const visibleCategoryCards = getVisibleCategoryCards(buildCategoryCards(kpi), kpi.activeCategory)

  return (
    <div className="grid grid-cols-4 gap-3">
      {visibleCategoryCards.map((card) => {
        const isHighlighted = card.id === kpi.activeCategory
        return (
          <div
            key={card.id}
            data-testid={card.testId}
            className={`rounded-2xl glass-surface p-4 transition-all ${
              isHighlighted ? card.highlightBorder : ''
            }`}
          >
            <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
              <div className="flex items-center gap-1.5">
                <span className="font-semibold text-[var(--text-primary)]">{card.title}</span>
                {isHighlighted && (
                  <span className="rounded-full bg-[#0071e3]/12 px-2 py-0.5 text-[10px] font-bold text-[#005bb5] border border-[#0071e3]/25">
                    Kiemelt
                  </span>
                )}
              </div>
              <div className={`flex h-7 w-7 items-center justify-center rounded-lg ${card.iconColor}`}>
                <card.Icon className="h-4 w-4" aria-hidden="true" />
              </div>
            </div>
            <p className="text-lg font-extrabold text-[var(--text-primary)] tabular-nums">
              {card.value}
            </p>
            <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-hairline)] pt-1.5 tabular-nums">
              {card.sampleLabel}
            </p>
          </div>
        )
      })}

      {visibleCategoryCards.length === 0 && (
        <div
          data-testid="kpi-card-general-median"
          className="rounded-2xl glass-surface p-4"
        >
          <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
            <span className="font-semibold text-[var(--text-primary)]">Medián Fajlagos Ár</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#0071e3]/12 text-[#0071e3]">
              <Building2 className="h-4 w-4" aria-hidden="true" />
            </div>
          </div>
          <p className="text-lg font-extrabold text-[var(--text-primary)] tabular-nums">
            {kpi.medianPricePerSqm > 0
              ? formatPricePerSqm(kpi.medianPricePerSqm)
              : 'Kevés minta (<5)'}
          </p>
          <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-hairline)] pt-1.5 tabular-nums">
            {totalCount} db találat alapján
          </p>
        </div>
      )}

      <div
        data-testid="kpi-card-avg-price"
        className="rounded-2xl glass-surface p-4"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold text-[var(--text-primary)]">Átlagos Kínálati Ár</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#0071e3]/12 text-[#0071e3]">
            <DollarSign className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-[var(--text-primary)] tabular-nums">
          {formatPriceMillion(kpi.avgPriceMillion)}
        </p>
        <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-hairline)] pt-1.5 tabular-nums">
          Tartomány: {kpi.minPriceMillion} - {kpi.maxPriceMillion} M Ft
        </p>
      </div>

      <div
        data-testid="kpi-card-total-samples"
        className="rounded-2xl glass-surface p-4"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold text-[var(--text-primary)]">Minta Elemszám</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]">
            <TrendingUp className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-[var(--text-primary)] tabular-nums">
          {totalCount} db
        </p>
        <div className="mt-1.5 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-hairline)] pt-1.5 tabular-nums">
          <span className="flex items-center gap-1 text-[var(--status-bargain-fg)] font-semibold">
            <Sparkles className="h-3 w-3" aria-hidden="true" /> {kpi.newCount} db új ({kpi.newPercent}%)
          </span>
          <span>{cityCount} város</span>
        </div>
      </div>

      <div
        data-testid="kpi-card-price-drops"
        className="rounded-2xl glass-surface p-4"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold text-[var(--text-primary)]">Árcsökkenések Száma</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)]">
            <TrendingDown className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-[var(--status-danger-fg)] tabular-nums">
          {kpi.discountedCount} db
        </p>
        <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-hairline)] pt-1.5 tabular-nums">
          A kínálat {kpi.discountedPercent}%-a árcsökkentett
        </p>
      </div>
    </div>
  )
}
