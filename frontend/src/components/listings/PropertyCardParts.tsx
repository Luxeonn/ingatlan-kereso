import { useRef, type FC } from 'react'
import { createPortal } from 'react-dom'
import {
  Sparkles,
  TrendingDown,
  Star,
  Building2,
  Info,
  Trash2,
  RotateCcw,
  X,
  MapPin,
  Ruler,
  BedDouble,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  getPropertyTypeConfig,
  formatPriceMillion,
  formatArea,
  formatRooms,
  getEffectivePrice,
} from '../../utils/formatters'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { BargainBadge } from './BargainBadge'
import { PropertyDetailsDrawer } from './PropertyDetailsDrawer'
import { cn } from '../../utils/cn'
import { isListingNew } from '../../utils/listingFilters'

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
  onHide?: () => void
  onRestore?: () => void
}

interface PropertyActionButtonsProps {
  item: IngatlanItem
  sourcesCount: number
  isDrawerOpen: boolean
  isStarred: boolean
  onToggleDrawer: () => void
  onToggleStarred: () => void
  onHide?: () => void
  onRestore?: () => void
}

const PropertyActionButtons: FC<PropertyActionButtonsProps> = ({
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
        className="flex items-center gap-1 rounded-lg border border-purple-500/40 bg-purple-500/15 px-2 py-1 text-xs font-semibold text-purple-300 hover:bg-purple-500/25 transition-all cursor-pointer"
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
        className="flex items-center justify-center rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] p-1.5 text-xs text-[var(--text-secondary)] hover:border-rose-500/40 hover:bg-rose-500/10 hover:text-rose-400 transition-all cursor-pointer"
      >
        <Trash2 className="h-3.5 w-3.5" />
      </button>
    )}
  </div>
)

export const PropertyCardBadges: FC<PropertyCardBadgesProps> = ({
  item,
  sourcesCount,
  isDrawerOpen,
  isStarred,
  onToggleDrawer,
  onToggleStarred,
  onHide,
  onRestore,
}) => (
  <div className="flex items-center justify-between gap-2 min-h-[28px]">
    <div className="flex items-center gap-1.5 flex-nowrap overflow-hidden">
      {isListingNew(item) && (
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

    <PropertyActionButtons
      item={item}
      sourcesCount={sourcesCount}
      isDrawerOpen={isDrawerOpen}
      isStarred={isStarred}
      onToggleDrawer={onToggleDrawer}
      onToggleStarred={onToggleStarred}
      onHide={onHide}
      onRestore={onRestore}
    />
  </div>
)

export interface PropertyDetailsModalProps {
  item: IngatlanItem
  onClose: () => void
  onMarkViewed?: (url: string) => void
}

function formatModalPrice(priceMillion: number | null | undefined, rawPrice?: string): string {
  if (priceMillion !== null && priceMillion !== undefined) {
    return formatPriceMillion(priceMillion)
  }
  return rawPrice || 'N/A'
}

interface PropertyModalHeaderSummaryProps {
  item: IngatlanItem
  priceLabel: string
}

const PropertyModalHeaderSummary: FC<PropertyModalHeaderSummaryProps> = ({ item, priceLabel }) => {
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount

  return (
    <div className="min-w-0 flex-1 space-y-1.5">
      <h3
        className="text-sm font-bold text-[var(--text-primary)] line-clamp-2"
        title={item.title}
      >
        {item.title || 'Ingatlan hirdetés'}
      </h3>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[var(--text-secondary)]">
        <span className="flex items-center gap-1">
          <MapPin className="h-3.5 w-3.5 text-blue-400 shrink-0" aria-hidden="true" />
          <span className="font-semibold text-[var(--text-primary)]">{item.city}</span>
          {item.location && item.location !== item.city && (
            <span className="truncate">({item.location})</span>
          )}
        </span>

        <span className="font-extrabold text-blue-400">{priceLabel}</span>

        {Boolean(floorArea && floorArea > 0) && (
          <span className="flex items-center gap-1">
            <Ruler className="h-3.5 w-3.5 text-purple-400 shrink-0" aria-hidden="true" />
            <span>{formatArea(floorArea)}</span>
          </span>
        )}

        {Boolean(rooms && rooms > 0) && (
          <span className="flex items-center gap-1">
            <BedDouble className="h-3.5 w-3.5 text-teal-400 shrink-0" aria-hidden="true" />
            <span>{formatRooms(rooms)}</span>
          </span>
        )}
      </div>
    </div>
  )
}

export const PropertyDetailsModal: FC<PropertyDetailsModalProps> = ({
  item,
  onClose,
  onMarkViewed,
}) => {
  const modalPanelRef = useRef<HTMLDivElement>(null)
  useOutsideDismiss(modalPanelRef, true, onClose, true)

  const { priceMillion } = getEffectivePrice(item)
  const priceLabel = formatModalPrice(priceMillion, item.price)

  const modalContent = (
    <div
      data-testid="property-details-modal-backdrop"
      onClick={(e) => {
        e.stopPropagation()
        if (e.target === e.currentTarget) {
          onClose()
        }
      }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-in fade-in duration-150"
    >
      <div
        ref={modalPanelRef}
        role="dialog"
        aria-modal="true"
        aria-label={`Ingatlan részletei: ${item.title || 'Ingatlan hirdetés'}`}
        data-testid="property-details-modal"
        onClick={(e) => e.stopPropagation()}
        className="flex max-h-[85vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] shadow-2xl animate-in zoom-in-95 duration-150"
      >
        <div className="flex items-start justify-between gap-3 border-b border-[var(--border-color)] px-4 py-3.5">
          <PropertyModalHeaderSummary item={item} priceLabel={priceLabel} />

          <button
            type="button"
            aria-label="Bezárás"
            title="Bezárás"
            data-testid="property-details-modal-close"
            onClick={(e) => {
              e.stopPropagation()
              onClose()
            }}
            className="rounded-lg p-1.5 text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)] transition-colors cursor-pointer shrink-0"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>

        <div className="overflow-y-auto p-4">
          <PropertyDetailsDrawer item={item} onMarkViewed={onMarkViewed} className="mt-0" />
        </div>
      </div>
    </div>
  )

  return typeof document !== 'undefined' ? createPortal(modalContent, document.body) : modalContent
}
