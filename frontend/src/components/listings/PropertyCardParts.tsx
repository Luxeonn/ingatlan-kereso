import type { FC, KeyboardEvent, MouseEvent } from 'react'
import {
  Sparkles,
  TrendingDown,
  Star,
  Building2,
  Info,
  Trash2,
  RotateCcw,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  getPropertyTypeConfig,
  getCrossPortalSpread,
  formatPriceMillion,
} from '../../utils/formatters'
import { BargainBadge } from './BargainBadge'
import { QualityScoreShield } from './QualityScoreShield'
import { cn } from '../../utils/cn'
import { isListingNew } from '../../utils/listingFilters'
export { PropertyParametersChips, type PropertyParametersChipsProps } from './PropertyParametersChips'

export interface PropertyThumbnailProps {
  item: IngatlanItem
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

export const PropertyThumbnail: FC<PropertyThumbnailProps> = ({
  item,
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
  const TypeIcon = getPropertyTypeConfig(item.propertyType).icon
  const resolvedImageUrl = imageUrl ?? item.imageUrl
  const isListMode = viewMode === 'list'
  const listA11yProps = buildListThumbnailProps(isListMode, isDrawerOpen, item.id, onToggleDrawer)

  return (
    <div
      data-testid="property-thumbnail"
      onClick={onThumbnailClick ?? onOpen}
      {...listA11yProps}
      className={cn(
        'relative cursor-pointer overflow-hidden rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] shrink-0',
        isListMode
          ? 'h-36 w-full sm:h-28 sm:w-44 mb-3 sm:mb-0 focus:outline-none focus:ring-2 focus:ring-blue-500/50'
          : 'mb-3 h-40 w-full',
        isListMode && isDrawerOpen ? 'ring-2 ring-blue-500/40 border-blue-500/60' : '',
      )}
    >
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
          className="flex h-full w-full flex-col items-center justify-center gap-1.5 bg-gradient-to-br from-[var(--bg-input)] to-[var(--bg-card)] text-[var(--text-secondary)]"
        >
          <TypeIcon className={cn('h-7 w-7 opacity-60', portalStyle.text)} aria-hidden="true" />
          <span className="text-[11px] font-semibold uppercase tracking-wider opacity-75">
            {item.portal || 'Ingatlan'}
          </span>
        </div>
      )}

      {item.portal && (
        <span
          data-testid="property-portal-badge"
          className={cn(
            'absolute top-2 left-2 rounded-md bg-slate-950/88 px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase shadow-sm',
            portalStyle.bg,
          )}
        >
          {item.portal}
        </span>
      )}

      {item.newConstruction && (
        <span
          data-testid="new-construction-badge"
          className="absolute bottom-2 left-2 flex items-center gap-1 rounded-md border border-cyan-400/40 bg-slate-950/90 px-2 py-0.5 text-[11px] font-bold text-cyan-300 shadow-sm"
        >
          <Building2 className="h-3 w-3" />
          <span>Új építésű</span>
        </span>
      )}
    </div>
  )
}

export interface PropertyCardBadgesProps {
  item: IngatlanItem
  sourcesCount: number
  isDrawerOpen: boolean
  isStarred: boolean
  onToggleDrawer: () => void
  onToggleStarred: () => void
  onHide?: () => void
  onRestore?: () => void
}

const PropertyActionButtons: FC<PropertyCardBadgesProps> = ({
  item,
  sourcesCount,
  isDrawerOpen,
  isStarred,
  onToggleDrawer,
  onToggleStarred,
  onHide,
  onRestore,
}) => (
  <div className="flex shrink-0 items-center gap-1.5 ml-auto">
    <button
      type="button"
      aria-expanded={isDrawerOpen}
      aria-controls={`drawer-${item.id}`}
      aria-label={
        sourcesCount > 1
          ? `${sourcesCount} forrás megjelenítése`
          : 'Részletek és források megjelenítése'
      }
      title="Részletek és források"
      data-testid={sourcesCount > 1 ? 'sources-count-badge' : 'details-button'}
      onClick={(e) => {
        e.stopPropagation()
        onToggleDrawer()
      }}
      className={cn(
        'flex items-center gap-1 rounded-lg border px-2 py-1 text-xs font-semibold transition-all cursor-pointer tabular-nums',
        isDrawerOpen
          ? 'border-blue-500/40 bg-[var(--accent-soft)] text-blue-600 shadow-sm'
          : 'border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-blue-600 hover:border-blue-500/30',
      )}
    >
      <Info className="h-3.5 w-3.5" aria-hidden="true" />
      <span className="text-[11px]">
        {sourcesCount > 1 ? `${sourcesCount} forrás` : 'Részletek'}
      </span>
    </button>

    <button
      type="button"
      onClick={onToggleStarred}
      aria-label={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
      title={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
      className={cn(
        'flex items-center justify-center rounded-lg border p-1.5 text-xs transition-all cursor-pointer',
        isStarred
          ? 'border-amber-500/40 bg-[var(--status-warn-bg)] text-amber-700 shadow-sm'
          : 'border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-amber-700',
      )}
    >
      <Star className={cn('h-3.5 w-3.5', isStarred ? 'fill-amber-500 text-amber-700' : '')} />
    </button>

    {item.hidden && onRestore && (
      <button
        type="button"
        data-testid="restore-property-button"
        onClick={(e) => {
          e.stopPropagation()
          onRestore()
        }}
        aria-label="Hirdetés visszaállítása"
        title="Hirdetés visszaállítása"
        className="flex items-center gap-1 rounded-lg border border-purple-500/40 bg-purple-500/15 px-2 py-1 text-xs font-semibold text-purple-700 hover:bg-purple-500/25 transition-all cursor-pointer"
      >
        <RotateCcw className="h-3.5 w-3.5" />
        <span className="text-[11px]">Visszaállítás</span>
      </button>
    )}

    {!item.hidden && onHide && (
      <button
        type="button"
        data-testid="hide-property-button"
        onClick={(e) => {
          e.stopPropagation()
          onHide()
        }}
        aria-label="Hirdetés elrejtése"
        title="Hirdetés elrejtése"
        className="flex items-center justify-center rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] p-1.5 text-xs text-[var(--text-secondary)] hover:border-rose-500/40 hover:bg-rose-500/10 hover:text-rose-700 transition-all cursor-pointer"
      >
        <Trash2 className="h-3.5 w-3.5" />
      </button>
    )}
  </div>
)

function resolveNonBargainMarketLabel(item: IngatlanItem): string | null {
  if (item.isBargain) return null
  const pct = item.percentageBelowMedian
  if (pct === null || pct === undefined || (item.referenceSampleSize ?? 0) < 5) return null
  return pct >= 0 ? `-${Math.round(pct)}% medián` : `+${Math.abs(Math.round(pct))}% medián`
}

export const PropertyCardBadges: FC<PropertyCardBadgesProps> = (props) => {
  const { item } = props
  const spreadInfo = getCrossPortalSpread(item)
  const marketLabel = resolveNonBargainMarketLabel(item)
  const hasPriceDrop = item.priceDelta !== null && item.priceDelta !== undefined && item.priceDelta < 0

  return (
    <div className="flex items-center justify-between gap-2 min-h-[28px]">
      <div className="flex items-center gap-1.5 flex-wrap">
        {isListingNew(item) && (
          <span className="flex shrink-0 items-center gap-1 rounded-md bg-[var(--status-bargain-bg)] border border-emerald-500/30 px-2 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)]">
            <Sparkles className="h-3 w-3" /> ÚJ
          </span>
        )}

        {hasPriceDrop && (
          <span className="flex shrink-0 items-center gap-1 rounded-md bg-[var(--status-danger-bg)] border border-rose-500/30 px-2 py-0.5 text-[11px] font-bold text-[var(--status-danger-fg)] tabular-nums">
            <TrendingDown className="h-3 w-3" />
            {Math.abs(Math.round(item.priceChangePercent || 0))}%
          </span>
        )}

        <BargainBadge item={item} />

        {marketLabel && (
          <span
            data-testid="market-position-pill"
            className="rounded-md border border-[var(--border-hairline)] bg-[var(--bg-input)] px-1.5 py-0.5 text-[10px] font-semibold text-[var(--text-secondary)] tabular-nums"
          >
            {marketLabel}
          </span>
        )}

        {spreadInfo && (
          <span
            data-testid="cross-portal-spread-pill"
            title={`Árkülönbség a portálok között: -${spreadInfo.spreadMillion} M Ft (legolcsóbb: ${spreadInfo.cheapestPortal})`}
            className="rounded-md border border-indigo-500/30 bg-[var(--status-info-bg)] px-1.5 py-0.5 text-[10px] font-bold text-[var(--status-info-fg)] tabular-nums"
          >
            -{spreadInfo.spreadMillion} M árrés
          </span>
        )}
      </div>

      <PropertyActionButtons {...props} />
    </div>
  )
}

export interface PropertyPriceSectionProps {
  item?: IngatlanItem
  priceMillion?: number | null
  rawPrice?: string
  viewMode: 'grid' | 'list'
}

export const PropertyPriceSection: FC<PropertyPriceSectionProps> = ({
  item,
  priceMillion,
  rawPrice,
  viewMode,
}) => (
  <div
    className={cn(
      'flex items-center justify-between border-t border-[var(--border-hairline)] pt-3 tabular-nums',
      viewMode === 'list'
        ? 'sm:border-t-0 sm:pt-0 sm:flex-col sm:items-end sm:justify-center sm:gap-1.5 shrink-0'
        : 'mt-4',
    )}
  >
    <div className={cn(viewMode === 'list' ? 'sm:text-right' : '')}>
      <div className="text-base font-extrabold text-blue-600">
        {priceMillion !== null && priceMillion !== undefined ? formatPriceMillion(priceMillion) : rawPrice}
      </div>
    </div>
    {item && item.qualityScore !== null && item.qualityScore !== undefined && (
      <QualityScoreShield item={item} popoverPlacement="top-right" />
    )}
  </div>
)
