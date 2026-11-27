import type { FC } from 'react'
import { AlertCircle, Building2 } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import type { MarketStatsDto } from '../../types/api'
import { useMarketAnalytics } from '../../hooks/useMarketAnalytics'
import { PriceDistributionChart } from './PriceDistributionChart'
import { CityComparisonChart } from './CityComparisonChart'
import { PriceScatterCorrelationChart } from './PriceScatterCorrelationChart'
import { BargainHunterTable } from './BargainHunterTable'
import { MarketKpiGrid } from './MarketAnalyticsKpiSection'
import { TopPriceDropsSection } from './TopPriceDropsSection'
import { MarketSegmentMatrixTable } from './MarketSegmentMatrixTable'
import { useFilterStore } from '../../store/useFilterStore'

export interface MarketAnalyticsPanelProps {
  items: IngatlanItem[]
  serverMarketStats?: MarketStatsDto | null
  onMarkViewed?: (url: string) => void
  onOpenListing?: (url: string) => void
  targetPropertyType?: string | null
}

function getCategoryLabel(activeCategory?: string | null): string | null {
  if (activeCategory === 'lakas') return 'Lakás'
  if (activeCategory === 'haz') return 'Ház'
  if (activeCategory === 'telek') return 'Telek'
  return null
}

export const MarketAnalyticsPanel: FC<MarketAnalyticsPanelProps> = ({
  items,
  serverMarketStats,
  onMarkViewed,
  onOpenListing,
  targetPropertyType,
}) => {
  const { criteria, setMinPriceMillion, setMaxPriceMillion, setCities, setPropertyType } = useFilterStore()
  const effectivePropertyType = targetPropertyType ?? criteria.propertyType
  const analytics = useMarketAnalytics(items, effectivePropertyType)
  const { kpi, distribution, cityComparison, scatterData, bargains } = analytics

  const categoryLabel = getCategoryLabel(kpi.activeCategory)

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

  const handleSelectSegment = (city: string, propertyType?: string) => {
    if (city) setCities([city])
    if (propertyType) setPropertyType(propertyType)
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-200" data-testid="market-analytics-panel">
      {analytics.totalCount === 0 && (
        <div
          data-testid="market-analytics-empty-state"
          className="flex flex-col items-center justify-center rounded-[20px] glass-surface border-dashed! border-[var(--border-strong)]! p-12 text-center"
        >
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#0071e3]/12 text-[#0071e3] mb-3">
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

      {kpi.insufficientData && analytics.totalCount > 0 && (
        <div
          data-testid="insufficient-data-banner"
          role="status"
          className="flex items-start gap-3 rounded-2xl glass-surface border-amber-500/35! bg-[var(--status-warn-bg)]! outline-1 outline-amber-500/25 p-4 text-[var(--status-warn-fg)]"
        >
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5 text-[var(--status-warn-fg)]" aria-hidden="true" />
          <div className="text-xs">
            <h4 className="font-bold text-[var(--status-warn-fg)]">
              Kevés minta az elemzéshez: a kiválasztott kategóriában legalább 5 hirdetés szükséges a megbízható statisztikához
            </h4>
            <p className="mt-1 text-[var(--text-secondary)]">
              A medián és fajlagos árak csak reprezentatív minta alapján jelennek meg, hogy elkerüljük a torzító átlagokat.
            </p>
          </div>
        </div>
      )}

      {analytics.totalCount > 0 && (
        <MarketKpiGrid
          totalCount={analytics.totalCount}
          cityCount={cityComparison.length}
          kpi={kpi}
        />
      )}

      <MarketSegmentMatrixTable
        segments={serverMarketStats?.segments}
        onSelectSegment={handleSelectSegment}
      />

      {bargains.topPriceDrops.length > 0 && (
        <TopPriceDropsSection
          topPriceDrops={bargains.topPriceDrops}
          onMarkViewed={onMarkViewed}
          onOpenListing={onOpenListing}
        />
      )}

      <div className="grid grid-cols-2 gap-6">
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

      <PriceScatterCorrelationChart
        scatterData={scatterData}
        onOpenListing={onOpenListing}
      />

      <BargainHunterTable
        topPriceDrops={bargains.topPriceDrops}
        belowMarketDeals={bargains.belowMarketDeals}
        onMarkViewed={onMarkViewed}
        onOpenListing={onOpenListing}
      />
    </div>
  )
}
