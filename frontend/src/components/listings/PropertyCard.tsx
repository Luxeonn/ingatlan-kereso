import { useState, type FC } from 'react'
import {
  MapPin,
  Ruler,
  BedDouble,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  getPortalStyle,
  getEffectivePrice,
  openExternalUrl,
} from '../../utils/formatters'
import { PropertyDetailsDrawer } from './PropertyDetailsDrawer'
import { PropertyCardBadges, PropertyThumbnail } from './PropertyCardParts'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface PropertyCardProps {
  item: IngatlanItem
  onMarkViewed: (url: string) => void
  viewMode?: 'grid' | 'list'
}

function getCardBorderClass(isStarred: boolean, isViewed?: boolean): string {
  if (isStarred) {
    return 'border-amber-500/50 bg-[var(--bg-card)] ring-1 ring-amber-500/30 shadow-[0_0_15px_-3px_rgba(245,158,11,0.15)] hover:border-amber-500/70 hover:ring-amber-500/50'
  }
  if (isViewed) {
    return 'border-[var(--border-color)] bg-[var(--bg-card)]/55 opacity-70 grayscale-[15%]'
  }
  return 'border-[var(--border-color)] bg-[var(--bg-card)] hover:border-blue-500/40'
}

function getPortalIdentifier(item: IngatlanItem): string {
  if (item.portal) return item.portal
  return item.source?.portalId ?? ''
}

interface PropertyParametersChipsProps {
  floorArea?: number | null
  rooms?: number | null
  sqmPrice?: number | null
}

const PropertyParametersChips: FC<PropertyParametersChipsProps> = ({
  floorArea,
  rooms,
  sqmPrice,
}) => (
  <div className="flex flex-wrap items-center gap-2 pt-1 text-xs text-[var(--text-secondary)]">
    {floorArea !== null && floorArea !== undefined && floorArea > 0 && (
      <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
        <Ruler className="h-3.5 w-3.5 text-purple-400" />
        <span>{formatArea(floorArea)}</span>
      </span>
    )}

    {rooms !== null && rooms !== undefined && rooms > 0 && (
      <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
        <BedDouble className="h-3.5 w-3.5 text-teal-400" />
        <span>{formatRooms(rooms)}</span>
      </span>
    )}

    {sqmPrice !== null && sqmPrice !== undefined && sqmPrice > 0 && (
      <span className="flex items-center gap-1 rounded-md bg-[var(--bg-input)] px-2 py-1 font-medium border border-[var(--border-color)]">
        <span>{formatPricePerSqm(sqmPrice)}</span>
      </span>
    )}
  </div>
)

interface PropertyTitleAndLocationProps {
  title?: string
  city?: string
  location?: string
  isNew?: boolean
  viewed?: boolean
  onOpen: () => void
}

const PropertyTitleAndLocation: FC<PropertyTitleAndLocationProps> = ({
  title,
  city,
  location,
  isNew,
  viewed,
  onOpen,
}) => (
  <>
    <h3
      onClick={onOpen}
      className={cn(
        'cursor-pointer text-sm font-bold text-[var(--text-primary)] line-clamp-2 hover:text-blue-400 transition-colors',
        isNew && !viewed ? 'pulse-title text-emerald-300' : '',
      )}
      title={title}
    >
      {title || 'Ingatlan hirdetés'}
    </h3>

    <div className="flex items-center gap-1 text-xs text-[var(--text-secondary)]">
      <MapPin className="h-3.5 w-3.5 text-blue-400 shrink-0" />
      <span className="font-semibold text-[var(--text-primary)]">{city}</span>
      {location && location !== city && (
        <span className="truncate">({location})</span>
      )}
    </div>
  </>
)

interface PropertyPriceSectionProps {
  priceMillion?: number | null
  rawPrice?: string
  viewMode: 'grid' | 'list'
}

const PropertyPriceSection: FC<PropertyPriceSectionProps> = ({
  priceMillion,
  rawPrice,
  viewMode,
}) => (
  <div
    className={cn(
      'flex items-center justify-between border-t border-[var(--border-color)] pt-3',
      viewMode === 'list' ? 'sm:border-t-0 sm:pt-0 sm:flex-col sm:items-end sm:justify-center sm:gap-1 shrink-0' : 'mt-4',
    )}
  >
    <div className={cn(viewMode === 'list' ? 'sm:text-right' : '')}>
      <div className="text-base font-extrabold text-blue-400">
        {priceMillion !== null && priceMillion !== undefined ? formatPriceMillion(priceMillion) : rawPrice}
      </div>
    </div>
  </div>
)

export const PropertyCard: FC<PropertyCardProps> = ({ item, onMarkViewed, viewMode = 'grid' }) => {
  const [imageError, setImageError] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const { starredUrls, toggleStarred } = useUiStore()
  const isStarred = starredUrls.includes(item.url)

  const sourcesCount = item.sourcesCount ?? (item.sourceListings?.length || 1)
  const portalStyle = getPortalStyle(getPortalIdentifier(item))
  const { priceMillion } = getEffectivePrice(item)

  const sqmPrice = item.detailsInfo?.pricePerSquareMeter
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount

  const hasValidImage = Boolean(item.imageUrl && item.imageUrl.trim().length > 0 && !imageError)

  const handleOpen = () => {
    openExternalUrl(item.url)
    if (!item.viewed) {
      onMarkViewed(item.url)
    }
  }

  return (
    <div
      data-testid="property-card"
      data-starred={isStarred ? 'true' : undefined}
      className={cn(
        'group relative flex flex-col justify-between overflow-hidden rounded-2xl border transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5 p-4',
        getCardBorderClass(isStarred, item.viewed),
      )}
    >
      <div
        className={cn(
          'flex flex-col justify-between w-full',
          viewMode === 'list' ? 'sm:flex-row sm:items-center sm:gap-5' : '',
        )}
      >
        <PropertyThumbnail
          item={item}
          hasValidImage={hasValidImage}
          portalStyle={portalStyle}
          viewMode={viewMode}
          onOpen={handleOpen}
          onImageError={() => setImageError(true)}
        />

        <div className={cn('flex flex-col gap-2 min-w-0', viewMode === 'list' ? 'flex-1' : '')}>
          <PropertyCardBadges
            item={item}
            sourcesCount={sourcesCount}
            isDrawerOpen={isDrawerOpen}
            isStarred={isStarred}
            onToggleDrawer={() => setIsDrawerOpen((prev) => !prev)}
            onToggleStarred={() => toggleStarred(item)}
          />

          <PropertyTitleAndLocation
            title={item.title}
            city={item.city}
            location={item.location}
            isNew={item.isNew}
            viewed={item.viewed}
            onOpen={handleOpen}
          />

          <PropertyParametersChips
            floorArea={floorArea}
            rooms={rooms}
            sqmPrice={sqmPrice}
          />
        </div>

        <PropertyPriceSection
          priceMillion={priceMillion}
          rawPrice={item.price}
          viewMode={viewMode}
        />
      </div>

      {isDrawerOpen && <PropertyDetailsDrawer item={item} onMarkViewed={onMarkViewed} />}
    </div>
  )
}
