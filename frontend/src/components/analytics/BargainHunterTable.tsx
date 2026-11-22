import { useMemo, useState, type FC } from 'react'
import { TrendingDown, Percent } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import type { BargainItem } from '../../hooks/useMarketAnalytics'
import { openExternalUrl } from '../../utils/formatters'
import { BelowMarketDealCard, PriceDropDealCard } from './BargainHunterCards'

interface BargainHunterTableProps {
  topPriceDrops: IngatlanItem[]
  belowMarketDeals: BargainItem[]
  onMarkViewed?: (url: string) => void
  onOpenListing?: (url: string) => void
}

export const BargainHunterTable: FC<BargainHunterTableProps> = ({
  topPriceDrops,
  belowMarketDeals,
  onMarkViewed,
  onOpenListing,
}) => {
  const [activeTab, setActiveTab] = useState<'belowMarket' | 'priceDrops'>('belowMarket')
  const [hoveredKey, setHoveredKey] = useState<string | null>(null)
  const [brokenImages, setBrokenImages] = useState<Record<string, boolean>>({})

  const sortedBelowMarketDeals = useMemo(
    () =>
      [...belowMarketDeals].sort(
        (a, b) => b.discountPercent - a.discountPercent || b.sqmDiffFt - a.sqmDiffFt,
      ),
    [belowMarketDeals],
  )

  const sortedPriceDrops = useMemo(
    () =>
      [...topPriceDrops].sort((a, b) => {
        const pctDiff = Math.abs(b.priceChangePercent ?? 0) - Math.abs(a.priceChangePercent ?? 0)
        if (pctDiff !== 0) return pctDiff
        return Math.abs(b.priceDelta ?? 0) - Math.abs(a.priceDelta ?? 0)
      }),
    [topPriceDrops],
  )

  const handleOpen = (item: IngatlanItem) => {
    onMarkViewed?.(item.url)
    if (onOpenListing) {
      onOpenListing(item.url)
    } else {
      openExternalUrl(item.url)
    }
  }

  const markImageBroken = (key: string) => {
    setBrokenImages((prev) => ({ ...prev, [key]: true }))
  }

  return (
    <div className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-md">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Alkalmi Vétel és Árleszállítás Figyelő
            </h3>
            <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-[var(--status-bargain-fg)] border border-emerald-500/20">
              Bargain Hunter
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            A piaci átlag alatti vagy frissen leárazott ingatlanok kiemelt listája (% alapján rendezve)
          </p>
        </div>

        <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)] text-xs">
          <button
            type="button"
            onClick={() => setActiveTab('belowMarket')}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-semibold transition-all cursor-pointer ${
              activeTab === 'belowMarket'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <Percent className="h-3.5 w-3.5" />
            <span>Piaci átlag alatt ({sortedBelowMarketDeals.length})</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('priceDrops')}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-semibold transition-all cursor-pointer ${
              activeTab === 'priceDrops'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <TrendingDown className="h-3.5 w-3.5" />
            <span>Leárazások ({sortedPriceDrops.length})</span>
          </button>
        </div>
      </div>

      {activeTab === 'belowMarket' ? (
        sortedBelowMarketDeals.length === 0 ? (
          <div className="flex h-36 items-center justify-center text-xs text-[var(--text-secondary)]">
            Nem található legalább 15%-kal a városi átlagár alatti ingatlan a jelenlegi keresési halmazban.
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {sortedBelowMarketDeals.map((deal) => {
              const key = deal.item.id || deal.item.url
              return (
                <BelowMarketDealCard
                  key={key}
                  deal={deal}
                  isHovered={hoveredKey === key}
                  isBroken={Boolean(brokenImages[key])}
                  onHoverChange={(hovered) => setHoveredKey(hovered ? key : null)}
                  onImageError={() => markImageBroken(key)}
                  onOpen={() => handleOpen(deal.item)}
                />
              )
            })}
          </div>
        )
      ) : sortedPriceDrops.length === 0 ? (
        <div className="flex h-36 items-center justify-center text-xs text-[var(--text-secondary)]">
          Nem található regisztrált árcsökkenés a jelenlegi találatok között.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {sortedPriceDrops.map((item) => {
            const key = item.id || item.url
            return (
              <PriceDropDealCard
                key={key}
                item={item}
                isHovered={hoveredKey === key}
                isBroken={Boolean(brokenImages[key])}
                onHoverChange={(hovered) => setHoveredKey(hovered ? key : null)}
                onImageError={() => markImageBroken(key)}
                onOpen={() => handleOpen(item)}
              />
            )
          })}
        </div>
      )}
    </div>
  )
}
