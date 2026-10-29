import type { FC } from 'react'
import {
  AlertCircle,
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
import { useMarketAnalytics } from '../../hooks/useMarketAnalytics'
import {
  formatPriceMillion,
  formatPricePerSqm,
  formatPriceFt,
  getPortalStyle,
} from '../../utils/formatters'
import { PropertyTypeBadge } from '../listings/PropertyTypeBadge'
import { PriceDistributionChart } from './PriceDistributionChart'
import { CityComparisonChart } from './CityComparisonChart'
import { PriceScatterCorrelationChart } from './PriceScatterCorrelationChart'
import { BargainHunterTable } from './BargainHunterTable'
import { useFilterStore } from '../../store/useFilterStore'

export interface MarketAnalyticsPanelProps {
  items: IngatlanItem[]
  onMarkViewed?: (url: string) => void
  onOpenListing?: (url: string) => void
  targetPropertyType?: string | null
}

export const MarketAnalyticsPanel: FC<MarketAnalyticsPanelProps> = ({
  items,
  onMarkViewed,
  onOpenListing,
  targetPropertyType,
}) => {
  const { criteria, setMinPriceMillion, setMaxPriceMillion, setCities } = useFilterStore()
  const effectivePropertyType = targetPropertyType ?? criteria.propertyType
  const analytics = useMarketAnalytics(items, effectivePropertyType)
  const { kpi, distribution, cityComparison, scatterData, bargains } = analytics

  const categoryLabel =
    kpi.activeCategory === 'lakas'
      ? 'Lakás'
      : kpi.activeCategory === 'haz'
        ? 'Ház'
        : kpi.activeCategory === 'telek'
          ? 'Telek'
          : null

  const handleSelectRange = (type: 'price' | 'sqmPrice', min: number, max: number) => {
    if (type === 'price') {
      setMinPriceMillion(Math.floor(min))
      setMaxPriceMillion(Math.ceil(max))
    }
  }

  const handleSelectCity = (city: string) => {
    if (city) {
      setCities([city])
    }
  }

  // Category KPI definitions (#263, #264, #276)
  const categoryCards = [
    {
      id: 'lakas' as const,
      testId: 'kpi-card-lakas',
      title: 'Lakás Medián Fajlagos Ár',
      Icon: Building2,
      iconColor: 'bg-indigo-500/10 text-indigo-400',
      highlightBorder: 'border-indigo-500/50 ring-1 ring-indigo-500/30 bg-indigo-500/5',
      value:
        kpi.lakasMedianPricePerSqm != null
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
        kpi.hazMedianPricePerSqm != null
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
        kpi.telekMedianPricePerSqm != null
          ? formatPricePerSqm(kpi.telekMedianPricePerSqm)
          : 'Kevés minta (<5)',
      sampleSize: kpi.telekSampleSize,
      sampleLabel: `${kpi.telekSampleSize} db telek alapján`,
    },
  ]

  // Filter out irrelevant category cards with 0 samples (#276)
  // Put active/searched category first to highlight it as primary stat
  const visibleCategoryCards = categoryCards
    .filter((card) => card.sampleSize > 0)
    .sort((a, b) => {
      if (a.id === kpi.activeCategory) return -1
      if (b.id === kpi.activeCategory) return 1
      return 0
    })

  return (
    <div className="space-y-6 animate-in fade-in duration-200" data-testid="market-analytics-panel">
      {/* Empty State when 0 listings available */}
      {analytics.totalCount === 0 && (
        <div
          data-testid="market-analytics-empty-state"
          className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center"
        >
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-500/10 text-indigo-400 mb-3">
            <Building2 className="h-7 w-7" aria-hidden="true" />
          </div>
          <h3 className="text-base font-bold text-[var(--text-primary)]">
            Nincs megjeleníthető adat a piaci elemzéshez
          </h3>
          <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
            Válassz ki keresési feltételeket vagy indíts keresést a diagramok és statisztikák megtekintéséhez!
          </p>
        </div>
      )}

      {/* Insufficient Data State Banner (#263, #264, #276) */}
      {kpi.insufficientData && analytics.totalCount > 0 && (
        <div
          data-testid="insufficient-data-banner"
          role="status"
          className="flex items-start gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300 shadow-sm"
        >
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5 text-amber-400" aria-hidden="true" />
          <div className="text-xs">
            <h4 className="font-bold text-amber-200">
              Kevés minta az elemzéshez: a kiválasztott kategóriában legalább 5 hirdetés szükséges a megbízható statisztikához
            </h4>
            <p className="mt-1 text-amber-300/80">
              A medián és fajlagos árak csak reprezentatív minta alapján jelennek meg, hogy elkerüljük a torzító átlagokat.
            </p>
          </div>
        </div>
      )}

      {/* Dynamic Visual KPI Cards Grid (#263, #264, #276) */}
      {analytics.totalCount > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {/* Relevant Category Cards (Irrelevant 0 sample categories hidden; searched/active highlighted) */}
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

          {/* Fallback general median when no specific standard category card matches */}
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
                {analytics.totalCount} db találat alapján
              </p>
            </div>
          )}

          {/* Átlagos Kínálati Ár */}
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

          {/* Minta Elemszám (Összes találat) */}
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
              {analytics.totalCount} db
            </p>
            <div className="mt-1.5 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1.5">
              <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                <Sparkles className="h-3 w-3" /> {kpi.newCount} db új ({kpi.newPercent}%)
              </span>
              <span>{cityComparison.length} város</span>
            </div>
          </div>

          {/* Árcsökkenések Száma */}
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
      )}

      {/* Top Price Drops Showcase Section */}
      {bargains.topPriceDrops.length > 0 && (
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
              {bargains.topPriceDrops.length} találat
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {bargains.topPriceDrops.slice(0, 6).map((item) => {
              const portalStyle = getPortalStyle(item.portal)
              const currentPrice = item.priceInfo?.amountHuf
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
                        {item.priceChangePercent != null
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
      )}

      {/* Visual Statistical Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <PriceDistributionChart
          priceBins={distribution.priceBins}
          sqmPriceBins={distribution.sqmPriceBins}
          avgPrice={kpi.avgPriceMillion}
          medianPrice={kpi.medianPriceMillion}
          avgSqmPrice={kpi.avgPricePerSqm}
          medianSqmPrice={kpi.medianPricePerSqm}
          categoryLabel={categoryLabel}
          onSelectRange={handleSelectRange}
        />
        <CityComparisonChart
          cityData={cityComparison}
          onSelectCity={handleSelectCity}
        />
      </div>

      {/* Price vs Area Correlation Scatter Chart */}
      <PriceScatterCorrelationChart
        scatterData={scatterData}
        onOpenListing={onOpenListing}
      />

      {/* Bargain Hunter Table */}
      <BargainHunterTable
        topPriceDrops={bargains.topPriceDrops}
        belowMarketDeals={bargains.belowMarketDeals}
        onMarkViewed={onMarkViewed}
        onOpenListing={onOpenListing}
      />
    </div>
  )
}
