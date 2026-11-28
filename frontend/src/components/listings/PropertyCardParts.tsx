import type { FC, KeyboardEvent, MouseEvent } from 'react'
import { Sparkles, Layers, Check } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { getPropertyTypeConfig } from '../../utils/formatters'
import { BargainBadge } from './BargainBadge'
import { cn } from '../../utils/cn'
import { isListingNew } from '../../utils/listingFilters'
import { isSelectionModifier } from './propertyCardHelpers'
export { PropertyParametersChips, type PropertyParametersChipsProps } from './PropertyParametersChips'
export {
  PropertyCardBadges,
  PropertyPriceSection,
  type PropertyCardBadgesProps,
  type PropertyPriceSectionProps,
} from './PropertyCardBadges'

export interface PropertyThumbnailProps {
  item: IngatlanItem
  sourcesCount: number
  isSelected: boolean
  imageUrl?: string
  hasValidImage: boolean
  portalStyle: { bg: string; text: string }
  viewMode: 'grid' | 'list'
  isDrawerOpen: boolean
  onOpen?: (e: MouseEvent<HTMLElement>) => void
  onThumbnailClick?: (e: MouseEvent<HTMLElement>) => void
  onToggleDrawer: () => void
  onImageError: () => void
}

function buildListThumbnailProps(
  isListMode: boolean,
  isDrawerOpen: boolean,
  itemId: string,
  onToggleDrawer: () => void,
) {
  if (!isListMode) return {}
  return {
    role: 'button' as const,
    tabIndex: 0,
    'aria-expanded': isDrawerOpen,
    'aria-controls': `drawer-${itemId}`,
    'aria-label': 'Részletek és források megjelenítése',
    title: 'Részletek és források megjelenítése',
    onKeyDown: (e: KeyboardEvent<HTMLElement>) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        e.stopPropagation()
        onToggleDrawer()
      }
    },
  }
}

function hasQualifyingBargain(item: IngatlanItem): boolean {
  return Boolean(
    item.isBargain &&
      (item.referenceSampleSize ?? 0) >= 5 &&
      item.percentageBelowMedian !== null &&
      item.percentageBelowMedian !== undefined &&
      item.percentageBelowMedian > 0,
  )
}

interface ThumbnailImageFrameProps {
  item: IngatlanItem
  resolvedImageUrl?: string
  hasValidImage: boolean
  portalStyle: { text: string }
  onImageError: () => void
}

const ThumbnailImageFrame: FC<ThumbnailImageFrameProps> = ({
  item,
  resolvedImageUrl,
  hasValidImage,
  portalStyle,
  onImageError,
}) => {
  const TypeIcon = getPropertyTypeConfig(item.propertyType).icon
  return (
    <div className="h-full w-full overflow-hidden rounded-[14px]">
      {hasValidImage && resolvedImageUrl ? (
        <img
          src={resolvedImageUrl}
          alt={item.title || 'Ingatlan előnézeti kép'}
          loading="lazy"
          onError={onImageError}
          className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      ) : (
        <div
          data-testid="property-image-fallback"
          className="flex h-full w-full flex-col items-center justify-center gap-1.5 bg-gradient-to-br from-slate-100/90 to-white/80 text-[#42526b]"
        >
          <TypeIcon className={cn('h-7 w-7 opacity-65', portalStyle.text)} aria-hidden="true" />
          <span className="text-[11px] font-semibold uppercase tracking-wider opacity-80">
            {item.portal || 'Ingatlan'}
          </span>
        </div>
      )}
    </div>
  )
}

const TopLeftThumbnailCapsule: FC<{ item: IngatlanItem }> = ({ item }) => {
  if (hasQualifyingBargain(item)) {
    return (
      <div className="absolute top-2.5 left-2.5 z-20">
        <BargainBadge item={item} variant="capsule" />
      </div>
    )
  }
  if (!isListingNew(item)) return null
  return (
    <span className="absolute top-2.5 left-2.5 z-10 inline-flex items-center gap-1 rounded-full glass-capsule px-2.5 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)]">
      <Sparkles className="h-3 w-3" aria-hidden="true" />
      <span>ÚJ</span>
    </span>
  )
}

const TopRightSourcesCapsule: FC<{
  itemId: string
  sourcesCount: number
  isDrawerOpen: boolean
  onToggleDrawer: () => void
}> = ({ itemId, sourcesCount, isDrawerOpen, onToggleDrawer }) => {
  if (sourcesCount <= 1) return null
  return (
    <button
      type="button"
      aria-expanded={isDrawerOpen}
      aria-controls={`drawer-${itemId}`}
      aria-label={`${sourcesCount} forrás megjelenítése`}
      title="Részletek és források"
      data-testid="sources-count-badge"
      onClick={(e) => {
        if (isSelectionModifier(e)) return
        e.stopPropagation()
        onToggleDrawer()
      }}
      className={cn(
        'absolute top-2.5 right-2.5 z-10 inline-flex items-center gap-1 rounded-full glass-capsule px-2.5 py-0.5 text-[11px] font-bold transition-all cursor-pointer tabular-nums',
        isDrawerOpen
          ? 'border-[#0071e3]/50 text-[#0071e3] ring-1 ring-[#0071e3]/30'
          : 'text-[#0b1220] hover:text-[#0071e3]',
      )}
    >
      <Layers className="h-3 w-3 text-[#0071e3]" aria-hidden="true" />
      <span>{sourcesCount} forrás</span>
    </button>
  )
}

export const PropertyThumbnail: FC<PropertyThumbnailProps> = ({
  item,
  sourcesCount,
  isSelected,
  imageUrl,
  hasValidImage,
  portalStyle,
  viewMode,
  isDrawerOpen,
  onOpen,
  onThumbnailClick,
  onToggleDrawer,
  onImageError,
}) => {
  const resolvedImageUrl = imageUrl ?? item.imageUrl
  const isListMode = viewMode === 'list'
  const listA11yProps = buildListThumbnailProps(isListMode, isDrawerOpen, item.id, onToggleDrawer)

  return (
    <div
      data-testid="property-thumbnail"
      onClick={onThumbnailClick ?? onOpen}
      {...listA11yProps}
      className={cn(
        'relative cursor-pointer rounded-[14px] border border-[var(--border-hairline)] bg-white/55 shrink-0',
        isListMode
          ? 'h-36 w-64 mb-0 focus:outline-none focus:ring-2 focus:ring-[#0071e3]/50'
          : 'mb-3 aspect-[16/10] w-full',
        isListMode && isDrawerOpen ? 'ring-2 ring-[#0071e3]/40 border-[#0071e3]/60' : '',
      )}
    >
      <ThumbnailImageFrame
        item={item}
        resolvedImageUrl={resolvedImageUrl}
        hasValidImage={hasValidImage}
        portalStyle={portalStyle}
        onImageError={onImageError}
      />

      <TopLeftThumbnailCapsule item={item} />

      <TopRightSourcesCapsule
        itemId={item.id}
        sourcesCount={sourcesCount}
        isDrawerOpen={isDrawerOpen}
        onToggleDrawer={onToggleDrawer}
      />

      {item.portal && (
        <span
          data-testid="property-portal-badge"
          className={cn(
            'absolute bottom-2.5 left-2.5 z-10 rounded-full glass-capsule px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase',
            portalStyle.text,
          )}
        >
          {item.portal}
        </span>
      )}

      {isSelected && (
        <span
          data-testid="card-selected-badge"
          aria-hidden="true"
          className="absolute bottom-2.5 right-2.5 z-10 flex h-6 w-6 items-center justify-center rounded-full bg-[#0071e3] text-white shadow-md"
        >
          <Check className="h-3.5 w-3.5 stroke-[3]" />
        </span>
      )}
    </div>
  )
}
