import { useRef, type FC } from 'react'
import { createPortal } from 'react-dom'
import { X, MapPin, Ruler, BedDouble, ExternalLink } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  formatPriceMillion,
  formatPricePerSqm,
  formatArea,
  formatRooms,
  getEffectivePrice,
  getEffectiveSqmPrice,
  openExternalUrl,
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
  sqmPrice?: number | null
}

const PropertyModalHeaderSummary: FC<PropertyModalHeaderSummaryProps> = ({
  item,
  priceLabel,
  sqmPrice,
}) => {
  const floorArea = item.detailsInfo?.areaSquareMeters
  const rooms = item.detailsInfo?.roomCount

  return (
    <div className="min-w-0 flex-1 space-y-1.5 tabular-nums">
      <div className="flex items-center gap-2">
        <span className="rounded-full border border-[#0071e3]/30 bg-[#0071e3]/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-[#005bb5]">
          Side-Peek Inspector
        </span>
      </div>
      <h3
        className="text-base font-bold text-[#0b1220] line-clamp-2"
        title={item.title}
      >
        {item.title || 'Ingatlan hirdetés'}
      </h3>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[#42526b]">
        <span className="flex items-center gap-1">
          <MapPin className="h-3.5 w-3.5 text-[#0071e3] shrink-0" aria-hidden="true" />
          <span className="font-semibold text-[#0b1220]">{item.city}</span>
          {item.location && item.location !== item.city && (
            <span className="truncate">({item.location})</span>
          )}
        </span>

        <span className="font-extrabold text-[#0071e3]">{priceLabel}</span>

        {sqmPrice !== null && sqmPrice !== undefined && sqmPrice > 0 && (
          <span className="rounded-full border border-[var(--border-hairline)] bg-white/75 px-2 py-0.5 text-[11px] font-medium text-[#42526b]">
            {formatPricePerSqm(sqmPrice)}
          </span>
        )}

        {Boolean(floorArea && floorArea > 0) && (
          <span className="flex items-center gap-1">
            <Ruler className="h-3.5 w-3.5 text-purple-700 shrink-0" aria-hidden="true" />
            <span>{formatArea(floorArea)}</span>
          </span>
        )}

        {Boolean(rooms && rooms > 0) && (
          <span className="flex items-center gap-1">
            <BedDouble className="h-3.5 w-3.5 text-teal-700 shrink-0" aria-hidden="true" />
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
  const sqmPrice = getEffectiveSqmPrice(item)
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
      className="fixed inset-0 z-50 flex items-stretch justify-end bg-[#0b1220]/25 backdrop-blur-sm animate-in fade-in duration-150"
    >
      <div
        ref={modalPanelRef}
        role="dialog"
        aria-modal="true"
        aria-label={`Ingatlan részletei: ${item.title || 'Ingatlan hirdetés'}`}
        data-testid="property-details-modal"
        onClick={(e) => e.stopPropagation()}
        className="flex h-full w-[480px] max-w-full flex-col overflow-hidden glass-elevated border-l border-[var(--glass-border)] animate-in slide-in-from-right duration-150"
      >
        <div className="flex items-start justify-between gap-3 border-b border-[var(--border-hairline)] glass-chrome px-5 py-4">
          <PropertyModalHeaderSummary item={item} priceLabel={priceLabel} sqmPrice={sqmPrice} />

          <div className="flex items-center gap-1.5 shrink-0">
            {item.url && (
              <button
                type="button"
                aria-label="Hirdetés megnyitása új lapon"
                title="Hirdetés megnyitása új lapon"
                onClick={(e) => {
                  e.stopPropagation()
                  openExternalUrl(item.url)
                  onMarkViewed?.(item.url)
                }}
                className="flex items-center gap-1 rounded-full border border-[var(--border-hairline)] bg-white/80 px-3 py-1.5 text-xs font-semibold text-[#0b1220] hover:border-[#0071e3]/50 hover:text-[#0071e3] transition-colors cursor-pointer"
              >
                <ExternalLink className="h-3.5 w-3.5 text-[#0071e3]" />
                <span>Megnyitás</span>
              </button>
            )}
            <button
              type="button"
              aria-label="Bezárás"
              title="Bezárás"
              data-testid="property-details-modal-close"
              onClick={(e) => {
                e.stopPropagation()
                onClose()
              }}
              className="rounded-full p-1.5 text-[#42526b] hover:bg-white/80 hover:text-[#0b1220] transition-colors cursor-pointer shrink-0"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-5">
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
