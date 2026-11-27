import { useState, type FC, type MouseEvent } from 'react'
import { MapPin } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  getPortalStyle,
  getEffectivePrice,
  getEffectiveSqmPrice,
  getEffectiveImageUrl,
  openExternalUrl,
} from '../../utils/formatters'
import { PropertyDetailsDrawer } from './PropertyDetailsDrawer'
import {
  PropertyCardBadges,
  PropertyParametersChips,
  PropertyPriceSection,
  PropertyThumbnail,
} from './PropertyCardParts'
import { PropertyDetailsModal } from './PropertyDetailsModal'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'
import {
  getCardBorderClass,
  getPortalIdentifier,
  getSourcesCount,
  isSelectionModifier,
  useCardSelection,
} from './propertyCardHelpers'

interface PropertyCardProps {
  item: IngatlanItem
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
  viewMode?: 'grid' | 'list' | 'table'
}

interface PropertyTitleAndLocationProps {
  title?: string
  city?: string
  location?: string
  onOpen: (e: MouseEvent<HTMLElement>) => void
}

const PropertyTitleAndLocation: FC<PropertyTitleAndLocationProps> = ({
  title,
  city,
  location,
  onOpen,
}) => (
  <>
    <h3
      onClick={onOpen}
      className="cursor-pointer text-sm font-bold leading-snug text-[#0b1220] line-clamp-2 hover:text-[#0071e3] transition-colors"
      title={title}
    >
      {title || 'Ingatlan hirdetés'}
    </h3>

    <div className="flex items-center gap-1 text-xs text-[#42526b]">
      <MapPin className="h-3.5 w-3.5 text-[#0071e3] shrink-0" />
      <span className="font-semibold text-[#0b1220]">{city}</span>
      {location && location !== city && (
        <span className="truncate">({location})</span>
      )}
    </div>
  </>
)

interface PropertyCardContentProps {
  item: IngatlanItem
  sourcesCount: number
  isSelected: boolean
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
  onThumbnailClick: (e: MouseEvent<HTMLElement>) => void
  onImageError: () => void
  onToggleDrawer: () => void
  onToggleStarred: () => void
  onHide?: () => void
  onRestore?: () => void
}

const PropertyCardContent: FC<PropertyCardContentProps> = ({
  item,
  sourcesCount,
  isSelected,
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
  onThumbnailClick,
  onImageError,
  onToggleDrawer,
  onToggleStarred,
  onHide,
  onRestore,
}) => (
  <div
    className={cn(
      'flex flex-col justify-between w-full',
      viewMode === 'list' ? 'flex-row items-center gap-5' : '',
    )}
  >
    <PropertyThumbnail
      item={item}
      sourcesCount={sourcesCount}
      isSelected={isSelected}
      imageUrl={imageUrl}
      hasValidImage={hasValidImage}
      portalStyle={portalStyle}
      viewMode={viewMode}
      isDrawerOpen={isDrawerOpen}
      onOpen={onOpen}
      onThumbnailClick={onThumbnailClick}
      onToggleDrawer={onToggleDrawer}
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

interface CardDetailsOverlayProps {
  isOpen: boolean
  isListMode: boolean
  item: IngatlanItem
  onClose: () => void
  onMarkViewed: (url: string) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
}

const CardDetailsOverlay: FC<CardDetailsOverlayProps> = ({
  isOpen,
  isListMode,
  item,
  onClose,
  onMarkViewed,
  onSplitSource,
  onUndoOverride,
}) => {
  if (!isOpen) return null
  if (isListMode) {
    return (
      <PropertyDetailsDrawer
        item={item}
        onMarkViewed={onMarkViewed}
        onSplitSource={onSplitSource}
        onUndoOverride={onUndoOverride}
      />
    )
  }
  return (
    <PropertyDetailsModal
      item={item}
      onClose={onClose}
      onMarkViewed={onMarkViewed}
      onSplitSource={onSplitSource}
      onUndoOverride={onUndoOverride}
    />
  )
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
  const effectiveImageUrl = getEffectiveImageUrl(item)
  const hasValidImage = Boolean(effectiveImageUrl) && !imageError
  const isListMode = viewMode === 'list'
  const cardViewMode: 'grid' | 'list' = isListMode ? 'list' : 'grid'

  const toggleDrawer = () => setIsDrawerOpen((prev) => !prev)

  const handleOpen = (e?: MouseEvent<HTMLElement>) => {
    if (isSelectionModifier(e)) {
      if (e) toggleFromEvent(e)
      return
    }
    openExternalUrl(item.url)
    if (!item.viewed) onMarkViewed(item.url)
  }

  const handleThumbnailClick = (e: MouseEvent<HTMLElement>) => {
    if (isSelectionModifier(e)) {
      toggleFromEvent(e)
      return
    }
    if (isListMode) {
      e.stopPropagation()
      toggleDrawer()
      return
    }
    handleOpen(e)
  }

  const handleHide = onHide
    ? () => {
        setIsHiding(true)
        setTimeout(() => onHide(item), 150)
      }
    : undefined

  return (
    <div
      data-testid="property-card"
      data-starred={isStarred ? 'true' : undefined}
      data-multisource={sourcesCount > 1 ? 'true' : undefined}
      data-selected={isSelected ? 'true' : undefined}
      aria-pressed={isSelected}
      onClick={(e) => {
        if (isSelectionModifier(e)) toggleFromEvent(e)
      }}
      className={cn(
        'group relative flex flex-col justify-between overflow-visible hover:z-30 focus-within:z-30 rounded-[20px] glass-surface transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5 p-4',
        getCardBorderClass(isSelected, isStarred, item.viewed, sourcesCount > 1),
        isHiding ? 'opacity-0 scale-95 transition-all duration-150 pointer-events-none' : '',
      )}
    >
      <PropertyCardContent
        item={item}
        sourcesCount={sourcesCount}
        isSelected={isSelected}
        portalStyle={portalStyle}
        priceMillion={priceMillion}
        sqmPrice={sqmPrice}
        floorArea={item.detailsInfo?.areaSquareMeters}
        rooms={item.detailsInfo?.roomCount}
        imageUrl={effectiveImageUrl}
        hasValidImage={hasValidImage}
        isDrawerOpen={isDrawerOpen}
        isStarred={isStarred}
        viewMode={cardViewMode}
        onOpen={handleOpen}
        onThumbnailClick={handleThumbnailClick}
        onImageError={() => setImageError(true)}
        onToggleDrawer={toggleDrawer}
        onToggleStarred={() => toggleStarred(item)}
        onHide={handleHide}
        onRestore={onRestore ? () => onRestore(item) : undefined}
      />

      <CardDetailsOverlay
        isOpen={isDrawerOpen}
        isListMode={isListMode}
        item={item}
        onClose={() => setIsDrawerOpen(false)}
        onMarkViewed={onMarkViewed}
        onSplitSource={onSplitSource}
        onUndoOverride={onUndoOverride}
      />
    </div>
  )
}
