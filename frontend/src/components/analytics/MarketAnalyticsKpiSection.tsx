import type { FC } from 'react'
import {
  Building2,
  Home,
  Trees,
  DollarSign,
  TrendingUp,
  TrendingDown,
  Sparkles,
  ExternalLink,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import type { MarketAnalyticsData } from '../../hooks/useMarketAnalytics'
import {
  formatPriceMillion,
  formatPricePerSqm,
  formatPriceFt,
  getEffectivePrice,
  getPortalStyle,
} from '../../utils/formatters'
import { PropertyTypeBadge } from '../listings/PropertyTypeBadge'

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
      iconColor: 'bg-indigo-500/10 text-indigo-400',
      highlightBorder: 'border-indigo-500/50 ring-1 ring-indigo-500/30 bg-indigo-500/5',
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
      iconColor: 'bg-amber-500/10 text-amber-400',
      highlightBorder: 'border-amber-500/50 ring-1 ring-amber-500/30 bg-amber-500/5',
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
      iconColor: 'bg-emerald-500/10 text-emerald-400',
      highlightBorder: 'border-emerald-500/50 ring-1 ring-emerald-500/30 bg-emerald-500/5',
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
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {visibleCategoryCards.map((card) => {
        const isHighlighted = card.id === kpi.activeCategory
        return (
          <div
            key={card.id}
            data-testid={card.testId}
            className={`rounded-xl border p-4 shadow-sm transition-all ${
              isHighlighted
                ? `${card.highlightBorder} shadow-md`
                : 'border-[var(--border-color)] bg-[var(--bg-card)]'
            }`}
          >
            <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
              <div className="flex items-center gap-1.5">
                <span className="font-semibold text-[var(--text-primary)]">{card.title}</span>
                {isHighlighted && (
                  <span className="rounded bg-blue-500/15 px-1.5 py-0.5 text-[10px] font-bold text-blue-400 border border-blue-500/30">
                    Kiemelt
                  </span>
                )}
              </div>
              <div className={`flex h-7 w-7 items-center justify-center rounded-lg ${card.iconColor}`}>
                <card.Icon className="h-4 w-4" aria-hidden="true" />
              </div>
            </div>
            <p className="text-lg font-extrabold text-[var(--text-primary)] font-mono">
              {card.value}
            </p>
            <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
              {card.sampleLabel}
            </p>
          </div>
        )
      })}

      {visibleCategoryCards.length === 0 && (
        <div
          data-testid="kpi-card-general-median"
          className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-sm"
        >
          <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
            <span className="font-semibold">Medián Fajlagos Ár</span>
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-400">
              <Building2 className="h-4 w-4" aria-hidden="true" />
            </div>
          </div>
          <p className="text-lg font-extrabold text-[var(--text-primary)] font-mono">
            {kpi.medianPricePerSqm > 0
              ? formatPricePerSqm(kpi.medianPricePerSqm)
              : 'Kevés minta (<5)'}
          </p>
          <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
            {totalCount} db találat alapján
          </p>
        </div>
      )}

      <div
        data-testid="kpi-card-avg-price"
        className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-sm"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold">Átlagos Kínálati Ár</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400">
            <DollarSign className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-[var(--text-primary)] font-mono">
          {formatPriceMillion(kpi.avgPriceMillion)}
        </p>
        <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
          Tartomány: {kpi.minPriceMillion} - {kpi.maxPriceMillion} M Ft
        </p>
      </div>

      <div
        data-testid="kpi-card-total-samples"
        className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-sm"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold">Minta Elemszám</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
            <TrendingUp className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-[var(--text-primary)] font-mono">
          {totalCount} db
        </p>
        <div className="mt-1.5 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
          <span className="flex items-center gap-1 text-emerald-400 font-semibold">
            <Sparkles className="h-3 w-3" /> {kpi.newCount} db új ({kpi.newPercent}%)
          </span>
          <span>{cityCount} város</span>
        </div>
      </div>

      <div
        data-testid="kpi-card-price-drops"
        className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-sm"
      >
        <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-1.5">
          <span className="font-semibold">Árcsökkenések Száma</span>
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-rose-500/10 text-rose-400">
            <TrendingDown className="h-4 w-4" aria-hidden="true" />
          </div>
        </div>
        <p className="text-lg font-extrabold text-rose-400 font-mono">
          {kpi.discountedCount} db
        </p>
        <p className="mt-1.5 text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
          A kínálat {kpi.discountedPercent}%-a árcsökkentett
        </p>
      </div>
    </div>
  )
}

export interface TopPriceDropsSectionProps {
  topPriceDrops: IngatlanItem[]
  onMarkViewed?: (url: string) => void
  onOpenListing?: (url: string) => void
}

export const TopPriceDropsSection: FC<TopPriceDropsSectionProps> = ({
  topPriceDrops,
  onMarkViewed,
  onOpenListing,
}) => (
  <section
    data-testid="top-price-drops-section"
    className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-sm space-y-4"
  >
    <div className="flex items-center justify-between border-b border-[var(--border-color)]/50 pb-3">
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-rose-500/15 text-rose-400">
          <TrendingDown className="h-4 w-4" aria-hidden="true" />
        </div>
        <div>
          <h3 className="text-sm font-bold text-[var(--text-primary)]">
            Legnagyobb Árcsökkenések
          </h3>
          <p className="text-xs text-[var(--text-secondary)]">
            Közelmúltban regisztrált jelentős árengedmények
          </p>
        </div>
      </div>
      <span className="text-xs font-semibold text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2.5 py-1 rounded-full">
        {topPriceDrops.length} találat
      </span>
    </div>

    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
      {topPriceDrops.slice(0, 6).map((item) => {
        const portalStyle = getPortalStyle(item.portal)
        const currentPrice = getEffectivePrice(item).priceFt
        const priceDelta = item.priceDelta ?? 0
        const originalPrice = currentPrice ? currentPrice - priceDelta : null

        return (
          <div
            key={item.id || item.url}
            data-testid="price-drop-card"
            className="flex flex-col justify-between rounded-xl border border-[var(--border-color)]/80 bg-[var(--bg-card)] p-3.5 hover:border-rose-500/40 transition-all shadow-sm"
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-1.5">
                <div className="flex items-center gap-1.5">
                  <span
                    className={`rounded px-1.5 py-0.5 text-[10px] font-bold border tracking-wider uppercase ${portalStyle.bg}`}
                  >
                    {item.portal}
                  </span>
                  <PropertyTypeBadge propertyType={item.propertyType} />
                </div>
                <span className="text-xs font-extrabold text-rose-400 font-mono">
                  {item.priceChangePercent !== null && item.priceChangePercent !== undefined
                    ? `${Math.round(item.priceChangePercent)}%`
                    : '-'}
                </span>
              </div>

              <h4 className="text-xs font-bold text-[var(--text-primary)] line-clamp-1">
                {item.title}
              </h4>
              <p className="text-[11px] text-[var(--text-secondary)]">
                {item.city}
              </p>
            </div>

            <div className="mt-3 pt-2.5 border-t border-[var(--border-color)]/50 flex items-end justify-between">
              <div>
                {originalPrice && originalPrice > 0 && (
                  <p className="text-[10px] text-slate-400 line-through">
                    {formatPriceFt(originalPrice)}
                  </p>
                )}
                <p className="text-sm font-extrabold text-[var(--text-primary)] font-mono">
                  {item.price}
                </p>
                {priceDelta < 0 && (
                  <p className="text-[11px] font-semibold text-rose-400">
                    -{formatPriceFt(Math.abs(priceDelta))}
                  </p>
                )}
              </div>

              <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => {
                  onMarkViewed?.(item.url)
                  onOpenListing?.(item.url)
                }}
                className="inline-flex items-center gap-1 rounded-lg bg-blue-500/10 px-2 py-1 text-xs font-bold text-blue-400 hover:bg-blue-500/20 transition-all cursor-pointer"
              >
                <span>Megnyitás</span>
                <ExternalLink className="h-3 w-3" aria-hidden="true" />
              </a>
            </div>
          </div>
        )
      })}
    </div>
  </section>
)
