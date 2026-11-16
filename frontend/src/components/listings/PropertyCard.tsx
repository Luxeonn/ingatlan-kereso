import { useState, type FC, type MouseEvent } from 'react'
import {
  MapPin,
  Ruler,
  BedDouble,
  Check,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  getPortalStyle,
  getEffectivePrice,
  getEffectiveSqmPrice,
  getEffectiveImageUrl,
  openExternalUrl,
} from '../../utils/formatters'
import { PropertyDetailsDrawer } from './PropertyDetailsDrawer'
import { PropertyCardBadges, PropertyThumbnail } from './PropertyCardParts'
import { PropertyDetailsModal } from './PropertyDetailsModal'
import { isListingNew } from '../../utils/listingFilters'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

interface PropertyCardProps {
  item: IngatlanItem
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
  viewMode?: 'grid' | 'list'
}

function getCardBorderClass(
  isSelected: boolean,
  isStarred: boolean,
  isViewed?: boolean,
  isMultiSource?: boolean,
): string {
  if (isSelected) {
    return 'border-blue-500 bg-blue-500/5 ring-2 ring-blue-500/40 shadow-lg'
  }
  if (isStarred) {
    return 'border-amber-500/50 bg-[var(--bg-card)] ring-1 ring-amber-500/30 shadow-[0_0_15px_-3px_rgba(245,158,11,0.15)] hover:border-amber-500/70 hover:ring-amber-500/50'
  }
  if (isViewed) {
    if (isMultiSource) {
      return 'border-yellow-400/60 bg-[var(--bg-card)]/55 opacity-70 grayscale-[15%]'
    }
    return 'border-[var(--border-color)] bg-[var(--bg-card)]/55 opacity-70 grayscale-[15%]'
  }
  if (isMultiSource) {
    return 'border-yellow-400 dark:border-yellow-500 bg-[var(--bg-card)] ring-1 ring-yellow-400/40 shadow-[0_0_12px_-3px_rgba(234,179,8,0.2)] hover:border-yellow-300 hover:ring-yellow-400/60'
  }
  return 'border-[var(--border-color)] bg-[var(--bg-card)] hover:border-blue-500/40'
}

function getPortalIdentifier(item: IngatlanItem): string {
  if (item.portal) return item.portal
  return item.source?.portalId ?? ''
}

function getSourcesCount(item: IngatlanItem): number {
  return item.sourcesCount ?? (item.sourceListings?.length || 1)
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
  onOpen: (e: MouseEvent<HTMLElement>) => void
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

interface PropertyCardContentProps {
  item: IngatlanItem
  sourcesCount: number
  portalStyle: ReturnType<typeof getPortalStyle>
  priceMillion: number | null | undefined
  sqmPrice?: number
  floorArea?: number
  rooms?: number
  imageUrl?: string
  hasValidImage: boolean
  isDrawerOpen: boolean
  isStarred: boolean
  viewMode: 'grid' | 'list'
  onOpen: (e: MouseEvent<HTMLElement>) => void
  onImageError: () => void
  onToggleDrawer: () => void
  onToggleStarred: () => void
  onHide?: () => void
  onRestore?: () => void
}

const PropertyCardContent: FC<PropertyCardContentProps> = ({
  item,
  sourcesCount,
  portalStyle,
  priceMillion,
  sqmPrice,
  floorArea,
  rooms,
  imageUrl,
  hasValidImage,
  isDrawerOpen,
  isStarred,
  viewMode,
  onOpen,
  onImageError,
  onToggleDrawer,
  onToggleStarred,
  onHide,
  onRestore,
}) => (
  <div
    className={cn(
      'flex flex-col justify-between w-full',
      viewMode === 'list' ? 'sm:flex-row sm:items-center sm:gap-5' : '',
    )}
  >
    <PropertyThumbnail
      item={item}
      imageUrl={imageUrl}
      hasValidImage={hasValidImage}
      portalStyle={portalStyle}
      viewMode={viewMode}
      onOpen={onOpen}
      onImageError={onImageError}
    />

    <div className={cn('flex flex-col gap-2 min-w-0', viewMode === 'list' ? 'flex-1' : '')}>
      <PropertyCardBadges
        item={item}
        sourcesCount={sourcesCount}
        isDrawerOpen={isDrawerOpen}
        isStarred={isStarred}
        onToggleDrawer={onToggleDrawer}
        onToggleStarred={onToggleStarred}
        onHide={onHide}
        onRestore={onRestore}
      />

      <PropertyTitleAndLocation
        title={item.title}
        city={item.city}
        location={item.location}
        isNew={isListingNew(item)}
        viewed={item.viewed}
        onOpen={onOpen}
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
)

function isSelectionModifier(e?: MouseEvent<HTMLElement>): boolean {
  return Boolean(e && (e.ctrlKey || e.metaKey))
}

function useCardSelection(url: string | undefined) {
  const { selectedCardUrls, toggleCardSelection } = useUiStore()
  const isSelected = Boolean(url && selectedCardUrls.includes(url))

  const toggleFromEvent = (e: MouseEvent<HTMLElement>) => {
    e.preventDefault()
    e.stopPropagation()
    if (url) toggleCardSelection(url)
  }

  return { isSelected, toggleFromEvent }
}

export const PropertyCard: FC<PropertyCardProps> = ({
  item,
  onMarkViewed,
  onHide,
  onRestore,
  onSplitSource,
  onUndoOverride,
  viewMode = 'grid',
}) => {
  const [imageError, setImageError] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const { starredUrls, toggleStarred } = useUiStore()
  const isStarred = starredUrls.includes(item.url)
  const { isSelected, toggleFromEvent } = useCardSelection(item.url)
  const [isHiding, setIsHiding] = useState(false)

  const sourcesCount = getSourcesCount(item)
  const portalStyle = getPortalStyle(getPortalIdentifier(item))
  const { priceMillion } = getEffectivePrice(item)
  const sqmPrice = getEffectiveSqmPrice(item) ?? undefined
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount
  const effectiveImageUrl = getEffectiveImageUrl(item)
  const hasValidImage = Boolean(effectiveImageUrl) && !imageError

  const handleOpen = (e?: MouseEvent<HTMLElement>) => {
    if (isSelectionModifier(e)) {
      if (e) toggleFromEvent(e)
      return
    }
    openExternalUrl(item.url)
    if (!item.viewed) onMarkViewed(item.url)
  }

  const handleCardClick = (e: MouseEvent<HTMLDivElement>) => {
    if (isSelectionModifier(e)) toggleFromEvent(e)
  }

  const handleHide = onHide
    ? () => {
        setIsHiding(true)
        setTimeout(() => onHide(item), 150)
      }
    : undefined

  const handleRestore = onRestore ? () => onRestore(item) : undefined

  return (
    <div
      data-testid="property-card"
      data-starred={isStarred ? 'true' : undefined}
      data-multisource={sourcesCount > 1 ? 'true' : undefined}
      data-selected={isSelected ? 'true' : undefined}
      aria-pressed={isSelected}
      onClick={handleCardClick}
      className={cn(
        'group relative flex flex-col justify-between overflow-hidden rounded-2xl border transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5 p-4',
        getCardBorderClass(isSelected, isStarred, item.viewed, sourcesCount > 1),
        isHiding ? 'opacity-0 scale-95 transition-all duration-150 pointer-events-none' : '',
      )}
    >
      {isSelected && (
        <span
          data-testid="card-selected-badge"
          aria-hidden="true"
          className="absolute top-2.5 right-2.5 z-10 flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-white shadow-md"
        >
          <Check className="h-3.5 w-3.5 stroke-[3]" />
        </span>
      )}
      <PropertyCardContent
        item={item}
        sourcesCount={sourcesCount}
        portalStyle={portalStyle}
        priceMillion={priceMillion}
        sqmPrice={sqmPrice}
        floorArea={floorArea}
        rooms={rooms}
        imageUrl={effectiveImageUrl}
        hasValidImage={hasValidImage}
        isDrawerOpen={isDrawerOpen}
        isStarred={isStarred}
        viewMode={viewMode}
        onOpen={handleOpen}
        onImageError={() => setImageError(true)}
        onToggleDrawer={() => setIsDrawerOpen((prev) => !prev)}
        onToggleStarred={() => toggleStarred(item)}
        onHide={handleHide}
        onRestore={handleRestore}
      />

      {isDrawerOpen &&
        (viewMode === 'list' ? (
          <PropertyDetailsDrawer item={item} onMarkViewed={onMarkViewed} onSplitSource={onSplitSource} onUndoOverride={onUndoOverride} />
        ) : (
          <PropertyDetailsModal item={item} onClose={() => setIsDrawerOpen(false)} onMarkViewed={onMarkViewed} onSplitSource={onSplitSource} onUndoOverride={onUndoOverride} />
        ))}
    </div>
  )
}
