import { type FC } from 'react'
import { TrendingDown, Sparkles, ExternalLink, Tag, Eye } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import type { BargainItem } from '../../hooks/useMarketAnalytics'
import {
  formatPriceMillion,
  formatPricePerSqm,
  getEffectiveImageUrl,
  getEffectivePrice,
  getEffectiveSqmPrice,
} from '../../utils/formatters'
import { isListingNew } from '../../utils/listingFilters'

const HoverThumbnailPreview: FC<{
  item: IngatlanItem
  visible: boolean
  isBroken: boolean
  onImageError: () => void
}> = ({ item, visible, isBroken, onImageError }) => {
  const imageUrl = getEffectiveImageUrl(item)
  if (!visible || !imageUrl || isBroken) return null

  return (
    <div
      data-testid="bargain-hover-thumbnail"
      className="pointer-events-none absolute -top-3 right-3 z-40 -translate-y-full w-48 overflow-hidden rounded-xl border border-[var(--border-strong)] bg-[var(--bg-elevated)] p-1.5 shadow-2xl animate-in fade-in zoom-in-95 duration-150"
    >
      <img
        src={imageUrl}
        alt={item.title || 'Ingatlan előnézeti kép'}
        onError={onImageError}
        className="h-28 w-full rounded-lg object-cover bg-[var(--bg-input)]"
      />
      <div className="mt-1 flex items-center justify-between gap-1 px-1 text-[10px] text-[var(--text-secondary)]">
        <span className="truncate font-semibold text-[var(--text-primary)]">
          {item.city || item.location}
        </span>
        {item.portal && <span className="shrink-0">{item.portal}</span>}
      </div>
    </div>
  )
}

export interface BelowMarketDealCardProps {
  deal: BargainItem
  isHovered: boolean
  isBroken: boolean
  onHoverChange: (hovered: boolean) => void
  onImageError: () => void
  onOpen: () => void
}

export const BelowMarketDealCard: FC<BelowMarketDealCardProps> = ({
  deal,
  isHovered,
  isBroken,
  onHoverChange,
  onImageError,
  onOpen,
}) => {
  const { item, cityAvgSqmPrice, discountPercent, sqmDiffFt } = deal
  const area = item.detailsInfo?.areaSquareMeters
  const itemSqmPrice = getEffectiveSqmPrice(item)
  const priceMillion = getEffectivePrice(item).priceMillion

  return (
    <div
      onMouseEnter={() => onHoverChange(true)}
      onMouseLeave={() => onHoverChange(false)}
      onFocus={() => onHoverChange(true)}
      onBlur={() => onHoverChange(false)}
      className="group relative flex flex-col justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-card-hover)]/40 p-4 transition-all hover:z-30 hover:border-emerald-500/50 hover:shadow-lg"
    >
      <HoverThumbnailPreview
        item={item}
        visible={isHovered}
        isBroken={isBroken}
        onImageError={onImageError}
      />

      <div>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="flex items-center gap-1 rounded-full bg-emerald-500/15 px-2.5 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)] border border-emerald-500/30">
            <TrendingDown className="h-3 w-3" />
            <span>-{discountPercent}% a városi átlaghoz képest</span>
          </span>
          {isListingNew(item) && (
            <span className="flex items-center gap-1 rounded-full bg-blue-500/15 px-2 py-0.5 text-[10px] font-bold text-blue-400">
              <Sparkles className="h-3 w-3" /> Új
            </span>
          )}
        </div>

        <h4 className="text-xs font-bold text-[var(--text-primary)] line-clamp-2 group-hover:text-[var(--status-bargain-fg)] transition-colors">
          {item.title}
        </h4>
        <p className="text-[11px] text-[var(--text-secondary)] mt-1">
          {item.city || item.location} • {area ? `${area} m²` : 'N/A'} • {item.portal}
        </p>

        <div className="mt-3 rounded-lg bg-[var(--bg-input)]/70 p-2.5 text-xs space-y-1 tabular-nums">
          <div className="flex justify-between">
            <span className="text-[var(--text-secondary)]">Kínálati ár:</span>
            <span className="font-extrabold text-[var(--text-primary)]">
              {priceMillion ? formatPriceMillion(priceMillion) : item.price}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-[var(--text-secondary)]">Fajlagos ár:</span>
            <span className="font-bold text-[var(--status-bargain-fg)]">
              {itemSqmPrice ? formatPricePerSqm(itemSqmPrice) : 'N/A'}
            </span>
          </div>
          <div className="flex justify-between text-[11px] text-[var(--text-secondary)] border-t border-[var(--border-color)]/50 pt-1">
            <span>Városi átlag ({item.city}):</span>
            <span>{formatPricePerSqm(cityAvgSqmPrice)}</span>
          </div>
          <div className="flex justify-between text-[11px] text-[var(--status-bargain-fg)] font-medium">
            <span>Megtakarítás:</span>
            <span>~{formatPricePerSqm(sqmDiffFt)}</span>
          </div>
        </div>
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-[var(--border-color)]/60 pt-3">
        <button
          type="button"
          onClick={onOpen}
          className="flex items-center gap-1 text-xs font-semibold text-[var(--status-bargain-fg)] hover:opacity-80 transition-opacity cursor-pointer"
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
}

export interface PriceDropDealCardProps {
  item: IngatlanItem
  isHovered: boolean
  isBroken: boolean
  onHoverChange: (hovered: boolean) => void
  onImageError: () => void
  onOpen: () => void
}

export const PriceDropDealCard: FC<PriceDropDealCardProps> = ({
  item,
  isHovered,
  isBroken,
  onHoverChange,
  onImageError,
  onOpen,
}) => {
  const priceMillion = getEffectivePrice(item).priceMillion
  const priceDelta = item.priceDelta ?? 0

  return (
    <div
      onMouseEnter={() => onHoverChange(true)}
      onMouseLeave={() => onHoverChange(false)}
      onFocus={() => onHoverChange(true)}
      onBlur={() => onHoverChange(false)}
      className="group relative flex flex-col justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-card-hover)]/40 p-4 transition-all hover:z-30 hover:border-blue-500/50 hover:shadow-lg"
    >
      <HoverThumbnailPreview
        item={item}
        visible={isHovered}
        isBroken={isBroken}
        onImageError={onImageError}
      />

      <div>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="flex items-center gap-1 rounded-full bg-blue-500/15 px-2.5 py-0.5 text-[11px] font-bold text-blue-400 border border-blue-500/30 tabular-nums">
            <Tag className="h-3 w-3" />
            <span>Árcsökkenés: {priceDelta.toLocaleString('hu-HU')} Ft</span>
          </span>
          {isListingNew(item) && (
            <span className="flex items-center gap-1 rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-bold text-[var(--status-bargain-fg)]">
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

        <div className="mt-3 rounded-lg bg-[var(--bg-input)]/70 p-2.5 text-xs space-y-1 tabular-nums">
          <div className="flex justify-between">
            <span className="text-[var(--text-secondary)]">Jelenlegi ár:</span>
            <span className="font-extrabold text-[var(--text-primary)]">
              {priceMillion ? formatPriceMillion(priceMillion) : item.price}
            </span>
          </div>
          {item.priceChangePercent !== null && item.priceChangePercent !== undefined && (
            <div className="flex justify-between text-[var(--status-bargain-fg)] font-semibold">
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
          onClick={onOpen}
          className="flex items-center gap-1 text-xs font-semibold text-blue-400 hover:text-blue-300 transition-colors cursor-pointer"
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
}
