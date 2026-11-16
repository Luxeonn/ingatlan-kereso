import { useRef, type FC } from 'react'
import { createPortal } from 'react-dom'
import { X, MapPin, Ruler, BedDouble } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatArea,
  formatRooms,
  getEffectivePrice,
} from '../../utils/formatters'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { PropertyDetailsDrawer } from './PropertyDetailsDrawer'

export interface PropertyDetailsModalProps {
  item: IngatlanItem
  onClose: () => void
  onMarkViewed?: (url: string) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
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
  onSplitSource,
  onUndoOverride,
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
          <PropertyDetailsDrawer
            item={item}
            onMarkViewed={onMarkViewed}
            onSplitSource={onSplitSource}
            onUndoOverride={onUndoOverride}
            className="mt-0"
          />
        </div>
      </div>
    </div>
  )

  return typeof document !== 'undefined' ? createPortal(modalContent, document.body) : modalContent
}
