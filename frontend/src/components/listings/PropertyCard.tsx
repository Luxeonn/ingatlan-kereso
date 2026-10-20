import type { FC } from 'react'
import { ExternalLink, Check, Sparkles, MapPin, Ruler, BedDouble, TrendingDown, Clock } from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatPriceFt,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  formatDate,
  getPortalStyle,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'

interface PropertyCardProps {
  item: IngatlanItem
  onMarkViewed: (url: string) => void
  viewMode?: 'grid' | 'list'
}

export const PropertyCard: FC<PropertyCardProps> = ({ item, onMarkViewed, viewMode = 'grid' }) => {
  const portalStyle = getPortalStyle(item.portal || item.source?.portalId || '')
  const priceMillion = item.priceInfo?.amountMillion
  const priceFt = item.priceInfo?.amountHuf
  const sqmPrice = item.detailsInfo?.pricePerSquareMeter
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount

  const handleOpen = () => {
    window.open(item.url, '_blank', 'noopener,noreferrer')
    if (!item.viewed) {
      onMarkViewed(item.url)
    }
  }

  return (
    <div
      className={cn(
        'group relative flex flex-col justify-between overflow-hidden rounded-2xl border transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5',
        item.viewed
          ? 'border-[var(--border-color)] bg-[var(--bg-card)]/60 opacity-80'
          : 'border-[var(--border-color)] bg-[var(--bg-card)] hover:border-blue-500/40',
        viewMode === 'list' ? 'sm:flex-row sm:items-center sm:gap-6 p-4' : 'p-4'
      )}
    >
      {/* Top / Left section: Badges, Title, Location */}
      <div className={cn('flex flex-col gap-2', viewMode === 'list' ? 'flex-1' : '')}>
        {/* Badges row */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            {/* Portal badge */}
            <span
              className={cn(
                'rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase',
                portalStyle.bg
              )}
            >
              {item.portal}
            </span>

            {/* New item badge */}
            {item.isNew && (
              <span className="flex items-center gap-1 rounded-md bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 text-[11px] font-bold text-emerald-400 animate-pulse">
                <Sparkles className="h-3 w-3" /> ÚJ
              </span>
            )}

            {/* Price drop badge */}
            {item.priceDelta != null && item.priceDelta < 0 && (
              <span className="flex items-center gap-1 rounded-md bg-rose-500/15 border border-rose-500/30 px-2 py-0.5 text-[11px] font-bold text-rose-400">
                <TrendingDown className="h-3 w-3" />
                {Math.abs(Math.round(item.priceChangePercent || 0))}%
              </span>
            )}
          </div>

          {/* Viewed checkbox / toggle */}
          <button
            type="button"
            onClick={() => onMarkViewed(item.url)}
            title={item.viewed ? 'Megtekintettnek jelölve' : 'Megjelölés megtekintettként'}
            className={cn(
              'flex items-center gap-1 text-[11px] font-semibold transition-colors',
              item.viewed
                ? 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                : 'text-blue-400 hover:text-blue-300'
            )}
          >
            <span
              className={cn(
                'flex h-4 w-4 items-center justify-center rounded border transition-colors',
                item.viewed ? 'bg-blue-600 border-blue-600 text-white' : 'border-[var(--border-color)]'
              )}
            >
              {item.viewed && <Check className="h-3 w-3" />}
            </span>
            <span className="hidden sm:inline">{item.viewed ? 'Megtekintve' : 'Nem láttam'}</span>
          </button>
        </div>

        {/* Title */}
        <h3
          onClick={handleOpen}
          className="cursor-pointer text-sm font-bold text-[var(--text-primary)] line-clamp-2 hover:text-blue-400 transition-colors"
          title={item.title}
        >
          {item.title || 'Ingatlan hirdetés'}
        </h3>

        {/* City & location */}
        <div className="flex items-center gap-1 text-xs text-[var(--text-secondary)]">
          <MapPin className="h-3.5 w-3.5 text-blue-400 shrink-0" />
          <span className="font-semibold text-[var(--text-primary)]">{item.city}</span>
          {item.location && item.location !== item.city && (
            <span className="truncate">({item.location})</span>
          )}
        </div>

        {/* Key parameters chips */}
        <div className="flex flex-wrap items-center gap-2 pt-1 text-xs text-[var(--text-secondary)]">
          {floorArea != null && floorArea > 0 && (
            <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
              <Ruler className="h-3.5 w-3.5 text-purple-400" />
              <span>{formatArea(floorArea)}</span>
            </span>
          )}

          {rooms != null && rooms > 0 && (
            <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
              <BedDouble className="h-3.5 w-3.5 text-teal-400" />
              <span>{formatRooms(rooms)}</span>
            </span>
          )}

          {sqmPrice != null && sqmPrice > 0 && (
            <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
              <span>{formatPricePerSqm(sqmPrice)}</span>
            </span>
          )}
        </div>
      </div>

      {/* Bottom / Right section: Price & Open button */}
      <div
        className={cn(
          'flex items-center justify-between border-t border-[var(--border-color)] pt-3',
          viewMode === 'list' ? 'sm:border-t-0 sm:pt-0 sm:flex-col sm:items-end sm:justify-center sm:gap-2 shrink-0' : 'mt-4'
        )}
      >
        {/* Price column */}
        <div className={cn(viewMode === 'list' ? 'sm:text-right' : '')}>
          <div className="text-base font-extrabold text-blue-400">
            {priceMillion != null ? formatPriceMillion(priceMillion) : item.price}
          </div>
          {priceFt != null && priceMillion != null && (
            <div className="text-[11px] text-[var(--text-secondary)]">
              {formatPriceFt(priceFt)}
            </div>
          )}
        </div>

        {/* Action button */}
        <div className="flex items-center gap-2">
          {item.discoveredAt && (
            <span
              className="hidden lg:flex items-center gap-1 text-[11px] text-[var(--text-secondary)]"
              title={`Felfedezve: ${item.discoveredAt}`}
            >
              <Clock className="h-3 w-3" />
              <span>{formatDate(item.discoveredAt)}</span>
            </span>
          )}
          <button
            type="button"
            onClick={handleOpen}
            className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-bold text-white shadow-md shadow-blue-500/20 transition-all hover:bg-blue-500 active:scale-95"
          >
            <span>Megnyitás</span>
            <ExternalLink className="h-3 w-3" />
          </button>
        </div>
      </div>
    </div>
  )
}
