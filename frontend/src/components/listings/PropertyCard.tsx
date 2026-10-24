import { useState, type FC } from 'react'
import {
  ExternalLink,
  Check,
  Sparkles,
  MapPin,
  Ruler,
  BedDouble,
  TrendingDown,
  Clock,
  Star,
  Building2,
  Layers,
} from 'lucide-react'
import { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatPriceFt,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  formatDate,
  getPortalStyle,
  getPropertyTypeConfig,
} from '../../utils/formatters'
import { PropertyTypeBadge } from './PropertyTypeBadge'
import { BargainBadge } from './BargainBadge'
import { QualityScoreShield } from './QualityScoreShield'
import { MultiSourceDrawer } from './MultiSourceDrawer'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface PropertyCardProps {
  item: IngatlanItem
  onMarkViewed: (url: string) => void
  viewMode?: 'grid' | 'list'
}

export const PropertyCard: FC<PropertyCardProps> = ({ item, onMarkViewed, viewMode = 'grid' }) => {
  const [imageError, setImageError] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const { starredUrls, toggleStarred } = useUiStore()
  const isStarred = starredUrls.includes(item.url)

  const sourcesCount = item.sourcesCount ?? (item.sourceListings?.length || 1)
  const isMultiSource = sourcesCount > 1

  const portalStyle = getPortalStyle(item.portal || item.source?.portalId || '')

  // Calculate lowest active source price (#260, #261)
  const activeSources = (item.sourceListings || []).filter(
    (s) => !s.status || s.status.toUpperCase() === 'ACTIVE'
  )
  const lowestActivePriceFt =
    activeSources.length > 0
      ? activeSources.reduce((min, s) => {
          if (s.priceHuf != null && s.priceHuf > 0) {
            return min == null || s.priceHuf < min ? s.priceHuf : min
          }
          return min
        }, null as number | null)
      : null

  const priceFt = lowestActivePriceFt ?? item.priceInfo?.amountHuf
  const priceMillion =
    priceFt != null
      ? priceFt / 1_000_000
      : item.priceInfo?.amountMillion

  const sqmPrice = item.detailsInfo?.pricePerSquareMeter
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount

  const hasValidImage = Boolean(item.imageUrl && item.imageUrl.trim().length > 0 && !imageError)

  const handleOpen = () => {
    window.open(item.url, '_blank', 'noopener,noreferrer')
    if (!item.viewed) {
      onMarkViewed(item.url)
    }
  }

  return (
    <div
      data-testid="property-card"
      className={cn(
        'group relative flex flex-col justify-between overflow-hidden rounded-2xl border transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5 p-4',
        item.viewed
          ? 'border-[var(--border-color)] bg-[var(--bg-card)]/55 opacity-70 grayscale-[15%]'
          : 'border-[var(--border-color)] bg-[var(--bg-card)] hover:border-blue-500/40'
      )}
    >
      <div
        className={cn(
          'flex flex-col justify-between w-full',
          viewMode === 'list' ? 'sm:flex-row sm:items-center sm:gap-5' : ''
        )}
      >
      {/* Thumbnail / Cover Image (#157, #202) */}
      <div
        onClick={handleOpen}
        className={cn(
          'relative cursor-pointer overflow-hidden rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] shrink-0',
          viewMode === 'list' ? 'h-36 w-full sm:h-28 sm:w-44 mb-3 sm:mb-0' : 'mb-3 h-40 w-full'
        )}
      >
        {hasValidImage ? (
          <img
            src={item.imageUrl}
            alt={item.title || 'Ingatlan előnézeti kép'}
            loading="lazy"
            onError={() => setImageError(true)}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div
            data-testid="property-image-fallback"
            className="flex h-full w-full flex-col items-center justify-center gap-1.5 bg-gradient-to-br from-[var(--bg-input)] to-[var(--bg-card)] text-[var(--text-secondary)]"
          >
            {(() => {
              const TypeIcon = getPropertyTypeConfig(item.propertyType).icon
              return <TypeIcon className={cn('h-7 w-7 opacity-60', portalStyle.text)} aria-hidden="true" />
            })()}
            <span className="text-[11px] font-semibold uppercase tracking-wider opacity-75">
              {item.portal || 'Ingatlan'}
            </span>
          </div>
        )}

        {/* New construction overlay badge on thumbnail (#202) */}
        {item.newConstruction && (
          <span
            data-testid="new-construction-badge"
            className="absolute bottom-2 left-2 flex items-center gap-1 rounded-md border border-cyan-400/40 bg-slate-900/85 px-2 py-0.5 text-[11px] font-bold text-cyan-300 shadow-sm backdrop-blur-sm"
          >
            <Building2 className="h-3 w-3" />
            <span>Új építésű</span>
          </span>
        )}
      </div>

      {/* Top / Middle section: Badges, Title, Location */}
      <div className={cn('flex flex-col gap-2', viewMode === 'list' ? 'flex-1' : '')}>
        {/* Badges row */}
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5">
            {/* Portal badge */}
            <span
              className={cn(
                'rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase',
                portalStyle.bg
              )}
            >
              {item.portal}
            </span>

            {/* Property type badge (#258) */}
            <PropertyTypeBadge propertyType={item.propertyType} />

            {/* Multi-source count badge (#260, #261) */}
            {isMultiSource && (
              <button
                type="button"
                aria-expanded={isDrawerOpen}
                aria-controls={`drawer-${item.id}`}
                aria-label={`${sourcesCount} forrás megjelenítése`}
                data-testid="sources-count-badge"
                onClick={(e) => {
                  e.stopPropagation()
                  setIsDrawerOpen((prev) => !prev)
                }}
                className={cn(
                  'flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-bold border transition-all cursor-pointer',
                  isDrawerOpen
                    ? 'bg-blue-500/20 text-blue-300 border-blue-500/40 shadow-sm'
                    : 'bg-indigo-500/10 text-indigo-300 border-indigo-500/30 hover:bg-indigo-500/20'
                )}
              >
                <Layers className="h-3 w-3 text-indigo-400" aria-hidden="true" />
                <span>{sourcesCount} forrás</span>
              </button>
            )}

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

            {/* Bargain badge (#263 & #264) */}
            <BargainBadge item={item} />

            {/* Data Quality Shield (#267) */}
            <QualityScoreShield item={item} />
          </div>

          {/* Right action icons: Star / Favorite (#157) + Viewed toggle */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => toggleStarred(item)}
              aria-label={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
              title={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
              className={cn(
                'flex items-center justify-center rounded-lg border p-1 text-xs transition-all',
                isStarred
                  ? 'border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm'
                  : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-amber-400'
              )}
            >
              <Star className={cn('h-3.5 w-3.5', isStarred ? 'fill-amber-400 text-amber-400' : '')} />
            </button>

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
        </div>

        {/* Title with pulse-title class for new unseen items (#157) */}
        <h3
          onClick={handleOpen}
          className={cn(
            'cursor-pointer text-sm font-bold text-[var(--text-primary)] line-clamp-2 hover:text-blue-400 transition-colors',
            item.isNew && !item.viewed ? 'pulse-title text-emerald-300' : ''
          )}
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

      {/* Multi-source expandable drawer (#260, #261) */}
      {isDrawerOpen && <MultiSourceDrawer item={item} onMarkViewed={onMarkViewed} />}
    </div>
  )
}
