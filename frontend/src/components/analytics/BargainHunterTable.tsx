import { FC, useState } from 'react'
import { TrendingDown, Sparkles, ExternalLink, Percent, Tag, Eye } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import { BargainItem } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

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

  const handleOpen = (item: IngatlanItem) => {
    if (onMarkViewed) onMarkViewed(item.url)
    if (onOpenListing) {
      onOpenListing(item.url)
    } else {
      window.open(item.url, '_blank', 'noopener,noreferrer')
    }
  }

  return (
    <div className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-md">
      {/* Header and Switcher */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Alkalmi Vétel és Árleszállítás Figyelő
            </h3>
            <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-500/20">
              Bargain Hunter
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            A piaci átlag alatti vagy frissen leárazott ingatlanok kiemelt listája
          </p>
        </div>

        {/* Tab switcher */}
        <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)] text-xs">
          <button
            type="button"
            onClick={() => setActiveTab('belowMarket')}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-semibold transition-all ${
              activeTab === 'belowMarket'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <Percent className="h-3.5 w-3.5" />
            <span>Piaci átlag alatt ({belowMarketDeals.length})</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('priceDrops')}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-semibold transition-all ${
              activeTab === 'priceDrops'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <TrendingDown className="h-3.5 w-3.5" />
            <span>Leárazások ({topPriceDrops.length})</span>
          </button>
        </div>
      </div>

      {/* Content */}
      {activeTab === 'belowMarket' ? (
        belowMarketDeals.length === 0 ? (
          <div className="flex h-36 items-center justify-center text-xs text-[var(--text-secondary)]">
            Nem található legalább 15%-kal a városi átlagár alatti ingatlan a jelenlegi keresési halmazban.
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {belowMarketDeals.map(({ item, cityAvgSqmPrice, discountPercent, sqmDiffFt }) => {
              const area = item.detailsInfo?.floorAreaSqm
              const itemSqmPrice = item.detailsInfo?.pricePerSqmFt
              const priceMillion = item.priceInfo?.amountMillion

              return (
                <div
                  key={item.id || item.url}
                  className="group relative flex flex-col justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-card-hover)]/40 p-4 transition-all hover:border-emerald-500/50 hover:shadow-lg"
                >
                  <div>
                    {/* Badges */}
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="flex items-center gap-1 rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-emerald-400 border border-emerald-500/30">
                        <TrendingDown className="h-3 w-3" />
                        <span>-{discountPercent}% a városi átlaghoz képest</span>
                      </span>
                      {item.isNew && (
                        <span className="flex items-center gap-1 rounded-full bg-blue-500/15 px-2 py-0.5 text-[10px] font-bold text-blue-400">
                          <Sparkles className="h-3 w-3" /> Új
                        </span>
                      )}
                    </div>

                    {/* Title & Location */}
                    <h4 className="text-xs font-bold text-[var(--text-primary)] line-clamp-2 group-hover:text-emerald-400 transition-colors">
                      {item.title}
                    </h4>
                    <p className="text-[11px] text-[var(--text-secondary)] mt-1">
                      {item.city || item.location} • {area ? `${area} m²` : 'N/A'} • {item.portal}
                    </p>

                    {/* Pricing details */}
                    <div className="mt-3 rounded-lg bg-[var(--bg-input)]/70 p-2.5 text-xs space-y-1">
                      <div className="flex justify-between">
                        <span className="text-[var(--text-secondary)]">Kínálati ár:</span>
                        <span className="font-extrabold text-[var(--text-primary)]">
                          {priceMillion ? formatPriceMillion(priceMillion) : item.price}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-[var(--text-secondary)]">Fajlagos ár:</span>
                        <span className="font-bold text-emerald-400">
                          {itemSqmPrice ? formatPricePerSqm(itemSqmPrice) : 'N/A'}
                        </span>
                      </div>
                      <div className="flex justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1">
                        <span>Városi átlag ({item.city}):</span>
                        <span>{formatPricePerSqm(cityAvgSqmPrice)}</span>
                      </div>
                      <div className="flex justify-between text-[11px] text-emerald-400 font-medium">
                        <span>Megtakarítás:</span>
                        <span>~{formatPricePerSqm(sqmDiffFt)}</span>
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="mt-4 flex items-center justify-between border-t border-[var(--border-color)]/60 pt-3">
                    <button
                      type="button"
                      onClick={() => handleOpen(item)}
                      className="flex items-center gap-1 text-xs font-semibold text-emerald-400 hover:text-emerald-300 transition-colors"
                    >
                      <span>Hirdetés megnyitása</span>
                      <ExternalLink className="h-3.5 w-3.5" />
                    </button>
                    {item.viewed && (
                      <span className="flex items-center gap-1 text-[11px] text-[var(--text-secondary)]">
                        <Eye className="h-3 w-3" /> Megtekintve
                      </span>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )
      ) : topPriceDrops.length === 0 ? (
        <div className="flex h-36 items-center justify-center text-xs text-[var(--text-secondary)]">
          Nem található regisztrált árcsökkenés a jelenlegi találatok között.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {topPriceDrops.map((item) => {
            const priceMillion = item.priceInfo?.amountMillion
            const priceDelta = item.priceDelta ?? 0

            return (
              <div
                key={item.id || item.url}
                className="group relative flex flex-col justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-card-hover)]/40 p-4 transition-all hover:border-blue-500/50 hover:shadow-lg"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="flex items-center gap-1 rounded-full bg-blue-500/15 px-2.5 py-0.5 text-[11px] font-bold text-blue-400 border border-blue-500/30">
                      <Tag className="h-3 w-3" />
                      <span>Árcsökkenés: {priceDelta.toLocaleString('hu-HU')} Ft</span>
                    </span>
                    {item.isNew && (
                      <span className="flex items-center gap-1 rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-bold text-emerald-400">
                        <Sparkles className="h-3 w-3" /> Új
                      </span>
                    )}
                  </div>

                  <h4 className="text-xs font-bold text-[var(--text-primary)] line-clamp-2 group-hover:text-blue-400 transition-colors">
                    {item.title}
                  </h4>
                  <p className="text-[11px] text-[var(--text-secondary)] mt-1">
                    {item.city || item.location} • {item.portal}
                  </p>

                  <div className="mt-3 rounded-lg bg-[var(--bg-input)]/70 p-2.5 text-xs space-y-1">
                    <div className="flex justify-between">
                      <span className="text-[var(--text-secondary)]">Jelenlegi ár:</span>
                      <span className="font-extrabold text-[var(--text-primary)]">
                        {priceMillion ? formatPriceMillion(priceMillion) : item.price}
                      </span>
                    </div>
                    {item.priceChangePercent != null && (
                      <div className="flex justify-between text-emerald-400 font-semibold">
                        <span>Változás aránya:</span>
                        <span>{item.priceChangePercent > 0 ? `+${item.priceChangePercent}%` : `${item.priceChangePercent}%`}</span>
                      </div>
                    )}
                    {item.lastPriceChangeAt && (
                      <div className="flex justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1">
                        <span>Leárazva:</span>
                        <span>{new Date(item.lastPriceChangeAt).toLocaleDateString('hu-HU')}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-[var(--border-color)]/60 pt-3">
                  <button
                    type="button"
                    onClick={() => handleOpen(item)}
                    className="flex items-center gap-1 text-xs font-semibold text-blue-400 hover:text-blue-300 transition-colors"
                  >
                    <span>Hirdetés megnyitása</span>
                    <ExternalLink className="h-3.5 w-3.5" />
                  </button>
                  {item.viewed && (
                    <span className="flex items-center gap-1 text-[11px] text-[var(--text-secondary)]">
                      <Eye className="h-3 w-3" /> Megtekintve
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
