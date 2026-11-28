import type { FC } from 'react'
import { ExternalLink, TrendingDown } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  formatPriceFt,
  formatPriceMillion,
  getEffectivePrice,
  getPortalStyle,
} from '../../utils/formatters'
import { PropertyTypeBadge } from '../listings/PropertyTypeBadge'

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
    className="rounded-[20px] glass-surface p-5 space-y-4"
  >
    <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-3">
      <div className="flex items-center gap-2.5">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)] border border-rose-500/20">
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
      <span className="text-xs font-semibold text-[var(--status-danger-fg)] bg-[var(--status-danger-bg)] border border-rose-500/25 px-2.5 py-1 rounded-full tabular-nums">
        {topPriceDrops.length} találat
      </span>
    </div>

    <div className="grid grid-cols-3 gap-3">
      {topPriceDrops.slice(0, 6).map((item) => {
        const portalStyle = getPortalStyle(item.portal)
        const { priceFt: currentPrice, priceMillion } = getEffectivePrice(item)
        const priceDelta = item.priceDelta ?? 0
        const originalPrice = priceDelta < 0 && currentPrice ? currentPrice - priceDelta : null

        return (
          <div
            key={item.id || item.url}
            data-testid="price-drop-card"
            className="flex flex-col justify-between rounded-2xl border border-[var(--border-hairline)] bg-white/75 p-3.5 hover:border-rose-500/45 hover:bg-white/90 transition-all"
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-1.5">
                <div className="flex items-center gap-1.5">
                  <span
                    className={`rounded-full px-2 py-0.5 text-[10px] font-bold border tracking-wider uppercase ${portalStyle.bg}`}
                  >
                    {item.portal}
                  </span>
                  <PropertyTypeBadge propertyType={item.propertyType} />
                </div>
                <span className="text-xs font-extrabold text-[var(--status-danger-fg)] tabular-nums">
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

            <div className="mt-3 pt-2.5 border-t border-[var(--border-hairline)] flex items-end justify-between">
              <div>
                {priceDelta < 0 && originalPrice && originalPrice > 0 && (
                  <p className="text-[10px] text-[var(--text-secondary)] line-through tabular-nums">
                    {formatPriceFt(originalPrice)}
                  </p>
                )}
                <p className="text-sm font-extrabold text-[var(--text-primary)] tabular-nums">
                  {priceMillion ? formatPriceMillion(priceMillion) : item.price}
                </p>
                {priceDelta < 0 && (
                  <p className="text-[11px] font-semibold text-[var(--status-danger-fg)] tabular-nums">
                    -{formatPriceFt(Math.abs(priceDelta))}
                  </p>
                )}
              </div>

              <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                onClick={(e) => {
                  onMarkViewed?.(item.url)
                  if (onOpenListing) {
                    e.preventDefault()
                    onOpenListing(item.url)
                  }
                }}
                className="inline-flex items-center gap-1 rounded-full bg-[#0071e3]/10 px-2.5 py-1 text-xs font-bold text-[#005bb5] hover:bg-[#0071e3]/18 transition-colors cursor-pointer"
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
