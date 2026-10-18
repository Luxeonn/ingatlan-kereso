import { FC } from 'react'
import {
  TrendingUp,
  Ruler,
  DollarSign,
  Tag,
  ArrowRight,
  Sparkles,
  BarChart3,
  Flame,
} from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { useMarketAnalytics } from '../../hooks/useMarketAnalytics'
import { PriceDistributionChart } from './PriceDistributionChart'
import { CityComparisonChart } from './CityComparisonChart'
import { PriceScatterCorrelationChart } from './PriceScatterCorrelationChart'
import { BargainHunterTable } from './BargainHunterTable'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'
import { useFilterStore } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'

interface MarketAnalyticsDashboardProps {
  items: IngatlanItem[]
  onMarkViewed?: (url: string) => void
  onOpenListing?: (url: string) => void
}

export const MarketAnalyticsDashboard: FC<MarketAnalyticsDashboardProps> = ({
  items,
  onMarkViewed,
  onOpenListing,
}) => {
  const analytics = useMarketAnalytics(items)
  const { setMinPriceMillion, setMaxPriceMillion, setCities } = useFilterStore()
  const { setDashboardTab } = useUiStore()

  const { kpi, distribution, cityComparison, scatterData, bargains } = analytics

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

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-500/10 text-indigo-400 mb-4">
          <BarChart3 className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Nincs elegendő adat a piaci elemzéshez
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Indíts keresést a fenti szűrőpanellel a grafikonok és statisztikai mutatószámok megjelenítéséhez!
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* 1. Deep Statistical KPI Cards Grid */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {/* Median vs Average Price */}
        <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-md">
          <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-2">
            <span className="font-semibold">Medián Kínálati Ár</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400">
              <DollarSign className="h-4 w-4" />
            </div>
          </div>
          <p className="text-xl font-extrabold text-[var(--text-primary)]">
            {formatPriceMillion(kpi.medianPriceMillion)}
          </p>
          <div className="mt-2 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-2">
            <span>Átlagár:</span>
            <span className="font-semibold text-[var(--text-primary)]">{formatPriceMillion(kpi.avgPriceMillion)}</span>
          </div>
          <div className="flex items-center justify-between text-[11px] text-[var(--text-secondary)]">
            <span>Szélsőérték:</span>
            <span>{kpi.minPriceMillion} - {kpi.maxPriceMillion} M Ft</span>
          </div>
        </div>

        {/* Median vs Average Sqm Price */}
        <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-md">
          <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-2">
            <span className="font-semibold">Medián Fajlagos Ár</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-400">
              <Ruler className="h-4 w-4" />
            </div>
          </div>
          <p className="text-xl font-extrabold text-[var(--text-primary)]">
            {formatPricePerSqm(kpi.medianPricePerSqm)}
          </p>
          <div className="mt-2 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-2">
            <span>Átlagos m² ár:</span>
            <span className="font-semibold text-[var(--text-primary)]">{formatPricePerSqm(kpi.avgPricePerSqm)}</span>
          </div>
          <div className="flex items-center justify-between text-[11px] text-[var(--text-secondary)]">
            <span>Árszórás:</span>
            <span>±{formatPricePerSqm(kpi.sqmPriceStdDev)}</span>
          </div>
        </div>

        {/* Bargains & Discounts */}
        <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4 shadow-md">
          <div className="flex items-center justify-between text-xs text-emerald-400 mb-2">
            <span className="font-semibold">Alkalmi Vételek</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/15 text-emerald-400">
              <Flame className="h-4 w-4" />
            </div>
          </div>
          <p className="text-xl font-extrabold text-emerald-400">
            {bargains.belowMarketDeals.length} db
          </p>
          <div className="mt-2 flex items-center justify-between text-[11px] text-emerald-400/80 border-t border-emerald-500/20 pt-2">
            <span>&gt;15%-kal átlag alatt:</span>
            <span className="font-semibold">{bargains.belowMarketDeals.length} db</span>
          </div>
          <div className="flex items-center justify-between text-[11px] text-emerald-400/80">
            <span>Leárazások száma:</span>
            <span>{kpi.discountedCount} db ({kpi.discountedPercent}%)</span>
          </div>
        </div>

        {/* Supply Volume & Fresh Listings */}
        <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-md">
          <div className="flex items-center justify-between text-xs text-[var(--text-secondary)] mb-2">
            <span className="font-semibold">Kínálati Dinamika</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
              <TrendingUp className="h-4 w-4" />
            </div>
          </div>
          <p className="text-xl font-extrabold text-[var(--text-primary)]">
            {analytics.totalCount} db
          </p>
          <div className="mt-2 flex items-center justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-2">
            <span>Új felfedezések:</span>
            <span className="font-semibold text-emerald-400 flex items-center gap-1">
              <Sparkles className="h-3 w-3" /> {kpi.newCount} db ({kpi.newPercent}%)
            </span>
          </div>
          <div className="flex items-center justify-between text-[11px] text-[var(--text-secondary)]">
            <span>Érintett települések:</span>
            <span>{cityComparison.length} város</span>
          </div>
        </div>
      </div>

      {/* Quick link banner to switch to listings view */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 rounded-xl border border-blue-500/30 bg-blue-500/10 px-4 py-3 text-xs text-blue-400">
        <div className="flex items-center gap-2">
          <Tag className="h-4 w-4 shrink-0 text-blue-400" />
          <span>
            Az analitika valós időben frissül a felül beállított szűrők alapján. A hirdetéskártyákhoz bármikor visszaválthatsz!
          </span>
        </div>
        <button
          type="button"
          onClick={() => setDashboardTab('listings')}
          className="flex items-center gap-1.5 shrink-0 rounded-lg bg-blue-600 px-3 py-1.5 font-bold text-white shadow-md transition-all hover:bg-blue-500"
        >
          <span>Hirdetéskártyák Megtekintése</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      </div>

      {/* 2. Primary Charts: Price Distribution & City Comparison */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <PriceDistributionChart
          priceBins={distribution.priceBins}
          sqmPriceBins={distribution.sqmPriceBins}
          avgPrice={kpi.avgPriceMillion}
          medianPrice={kpi.medianPriceMillion}
          avgSqmPrice={kpi.avgPricePerSqm}
          medianSqmPrice={kpi.medianPricePerSqm}
          onSelectRange={handleSelectRange}
        />

        <CityComparisonChart
          cityData={cityComparison}
          onSelectCity={handleSelectCity}
        />
      </div>

      {/* 3. Correlation Scatter Plot: Price vs Floor Area */}
      <PriceScatterCorrelationChart
        scatterData={scatterData}
        onOpenListing={onOpenListing}
      />

      {/* 4. Bargain Hunter & Price Drop Table */}
      <BargainHunterTable
        topPriceDrops={bargains.topPriceDrops}
        belowMarketDeals={bargains.belowMarketDeals}
        onMarkViewed={onMarkViewed}
        onOpenListing={onOpenListing}
      />
    </div>
  )
}
