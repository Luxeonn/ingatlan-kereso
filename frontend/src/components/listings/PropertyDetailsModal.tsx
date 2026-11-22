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
        <span className="rounded-md border border-blue-500/30 bg-[var(--accent-soft)] px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-blue-400">
          Side-Peek Inspector
        </span>
      </div>
      <h3
        className="text-sm sm:text-base font-bold text-[var(--text-primary)] line-clamp-2"
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

        {sqmPrice !== null && sqmPrice !== undefined && sqmPrice > 0 && (
          <span className="rounded bg-[var(--bg-input)] px-1.5 py-0.5 text-[11px] font-medium text-[var(--text-secondary)]">
            {formatPricePerSqm(sqmPrice)}
          </span>
        )}

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
      className="fixed inset-0 z-50 flex items-stretch justify-end bg-black/45 backdrop-blur-[2px] animate-in fade-in duration-150"
    >
      <div
        ref={modalPanelRef}
        role="dialog"
        aria-modal="true"
        aria-label={`Ingatlan részletei: ${item.title || 'Ingatlan hirdetés'}`}
        data-testid="property-details-modal"
        onClick={(e) => e.stopPropagation()}
        className="flex h-full w-full max-w-xl flex-col overflow-hidden border-l border-[var(--border-strong)] bg-[var(--bg-elevated)] shadow-2xl animate-in slide-in-from-right duration-150"
      >
        <div className="flex items-start justify-between gap-3 border-b border-[var(--border-hairline)] bg-[var(--bg-chrome)] px-5 py-4 backdrop-blur-md">
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
                className="flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 py-1.5 text-xs font-semibold text-[var(--text-primary)] hover:border-blue-500/50 hover:text-blue-400 transition-colors cursor-pointer"
              >
                <ExternalLink className="h-3.5 w-3.5 text-blue-400" />
                <span className="hidden sm:inline">Megnyitás</span>
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
              className="rounded-lg p-1.5 text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)] transition-colors cursor-pointer shrink-0"
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
