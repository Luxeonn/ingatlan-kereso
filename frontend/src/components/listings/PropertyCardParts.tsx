import type { FC } from 'react'
import {
  Sparkles,
  TrendingDown,
  Star,
  Building2,
  Info,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { getPropertyTypeConfig } from '../../utils/formatters'
import { BargainBadge } from './BargainBadge'
import { cn } from '../../utils/cn'

export interface PropertyThumbnailProps {
  item: IngatlanItem
  hasValidImage: boolean
  portalStyle: { bg: string; text: string }
  viewMode: 'grid' | 'list'
  onOpen: () => void
  onImageError: () => void
}

export const PropertyThumbnail: FC<PropertyThumbnailProps> = ({
  item,
  hasValidImage,
  portalStyle,
  viewMode,
  onOpen,
  onImageError,
}) => {
  const TypeIcon = getPropertyTypeConfig(item.propertyType).icon

  return (
    <div
      onClick={onOpen}
      className={cn(
        'relative cursor-pointer overflow-hidden rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] shrink-0',
        viewMode === 'list' ? 'h-36 w-full sm:h-28 sm:w-44 mb-3 sm:mb-0' : 'mb-3 h-40 w-full',
      )}
    >
      {hasValidImage ? (
        <img
          src={item.imageUrl}
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
            'absolute top-2 left-2 rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase backdrop-blur-md shadow-sm',
            portalStyle.bg,
          )}
        >
          {item.portal}
        </span>
      )}

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
  )
}

export interface PropertyCardBadgesProps {
  item: IngatlanItem
  sourcesCount: number
  isDrawerOpen: boolean
  isStarred: boolean
  onToggleDrawer: () => void
  onToggleStarred: () => void
}

export const PropertyCardBadges: FC<PropertyCardBadgesProps> = ({
  item,
  sourcesCount,
  isDrawerOpen,
  isStarred,
  onToggleDrawer,
  onToggleStarred,
}) => (
  <div className="flex items-center justify-between gap-2 min-h-[28px]">
    <div className="flex items-center gap-1.5 flex-nowrap overflow-hidden">
      {item.isNew && (
        <span className="flex shrink-0 items-center gap-1 rounded-md bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 text-[11px] font-bold text-emerald-400 animate-pulse">
          <Sparkles className="h-3 w-3" /> ÚJ
        </span>
      )}

      {item.priceDelta !== null && item.priceDelta !== undefined && item.priceDelta < 0 && (
        <span className="flex shrink-0 items-center gap-1 rounded-md bg-rose-500/15 border border-rose-500/30 px-2 py-0.5 text-[11px] font-bold text-rose-400">
          <TrendingDown className="h-3 w-3" />
          {Math.abs(Math.round(item.priceChangePercent || 0))}%
        </span>
      )}

      <BargainBadge item={item} />
    </div>

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
          'flex items-center gap-1 rounded-lg border px-2 py-1 text-xs font-semibold transition-all cursor-pointer',
          isDrawerOpen
            ? 'border-blue-500/40 bg-blue-500/20 text-blue-300 shadow-sm'
            : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-blue-400 hover:border-blue-500/30',
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
            ? 'border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm'
            : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-amber-400',
        )}
      >
        <Star className={cn('h-3.5 w-3.5', isStarred ? 'fill-amber-400 text-amber-400' : '')} />
      </button>
    </div>
  </div>
)
