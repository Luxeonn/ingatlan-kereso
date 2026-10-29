import React, { useState, useRef, useId, useCallback } from 'react'
import { TrendingDown, Sparkles } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { formatPricePerSqm } from '../../utils/formatters'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'

export interface BargainBadgeProps {
  item: IngatlanItem
  className?: string
}

export const BargainBadge: React.FC<BargainBadgeProps> = ({ item, className = '' }) => {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const popoverId = useId()
  const closePopover = useCallback(() => setIsOpen(false), [])

  useOutsideDismiss(containerRef, isOpen, closePopover)

  const isBargain = Boolean(item.isBargain || item.bargainInfo?.isBargain)
  const sampleSize = item.referenceSampleSize ?? item.bargainInfo?.sampleSize ?? 0
  const percentageBelow =
    item.percentageBelowMedian ??
    item.bargainInfo?.percentageBelowMedian ??
    (item.bargainInfo?.deltaPercent != null ? Math.abs(item.bargainInfo.deltaPercent) : null)
  const refMedian = item.referenceMedianPricePerSquareMeter ?? item.bargainInfo?.referenceMedianPricePerSqm
  const currentPricePerSqm =
    item.detailsInfo?.pricePerSquareMeter ?? item.bargainInfo?.currentPricePerSqm

  // Guard: Must be flagged as bargain AND have at least 5 reference samples
  if (!isBargain || sampleSize < 5 || percentageBelow == null || percentageBelow <= 0) {
    return null
  }

  const roundedPct = Math.round(percentageBelow)

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.stopPropagation()
      setIsOpen(false)
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      e.stopPropagation()
      setIsOpen((prev) => !prev)
    }
  }

  return (
    <div ref={containerRef} className={`relative inline-block ${className}`}>
      <button
        type="button"
        role="button"
        tabIndex={0}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        aria-controls={popoverId}
        aria-label={`Alkalmi vétel: ${roundedPct}%-kal a helyi piaci mediánár alatt`}
        data-testid="bargain-badge-button"
        onClick={(e) => {
          e.stopPropagation()
          setIsOpen((prev) => !prev)
        }}
        onMouseEnter={() => setIsOpen(true)}
        onMouseLeave={() => setIsOpen(false)}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setIsOpen(false)}
        onKeyDown={handleKeyDown}
        className="inline-flex items-center gap-1.5 rounded-md bg-emerald-500/15 border border-emerald-500/30 px-2 py-0.5 text-[11px] font-bold text-emerald-400 hover:bg-emerald-500/25 transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-400/50 cursor-pointer"
      >
        <TrendingDown className="h-3 w-3 shrink-0 text-emerald-400" aria-hidden="true" />
        <span>Alkalmi vétel</span>
        <span
          data-testid="bargain-discount-pill"
          className="ml-0.5 rounded bg-emerald-500/25 px-1 py-0.2 text-[10px] font-extrabold text-emerald-300"
        >
          -{roundedPct}%
        </span>
      </button>

      {isOpen && (
        <div
          id={popoverId}
          role="dialog"
          aria-label="Alkalmi vétel részletei"
          data-testid="bargain-popover"
          onClick={(e) => e.stopPropagation()}
          className="absolute top-full left-0 mt-1.5 z-50 w-72 max-w-[calc(100vw-2.5rem)] rounded-xl border border-emerald-500/30 bg-slate-900/95 dark:bg-slate-900/95 p-3.5 shadow-2xl backdrop-blur-md text-slate-200 animate-in fade-in zoom-in-95 duration-150"
        >
          <div className="flex items-center justify-between pb-2 mb-2 border-b border-emerald-500/20">
            <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-400">
              <Sparkles className="h-3.5 w-3.5 text-emerald-400" aria-hidden="true" />
              <span>Piaci Árelemzés • Alkalmi Vétel</span>
            </div>
            <span className="text-[10px] text-slate-400 font-mono">
              {item.city}
            </span>
          </div>

          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between items-center">
              <span className="text-slate-400">Kínálati m² ár:</span>
              <span className="font-bold text-emerald-400 font-mono">
                {formatPricePerSqm(currentPricePerSqm)}
              </span>
            </div>

            {refMedian != null && (
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Városi medián m² ár:</span>
                <span className="font-semibold text-slate-300 font-mono">
                  {formatPricePerSqm(refMedian)}
                </span>
              </div>
            )}

            <div className="flex justify-between items-center pt-1 border-t border-slate-700/50">
              <span className="text-slate-400">Eltérés a mediántól:</span>
              <span className="font-extrabold text-emerald-400 font-mono">
                -{roundedPct}%
              </span>
            </div>

            <div className="flex justify-between items-center text-[11px] text-slate-400">
              <span>Minta elemszáma:</span>
              <span className="font-medium text-slate-300">{sampleSize} hirdetés</span>
            </div>
          </div>

          <div className="mt-2.5 pt-2 border-t border-slate-800 text-[10px] text-slate-400 italic">
            Legalább 5 azonos típusú helyi hirdetésből számított medián alapján.
          </div>
        </div>
      )}
    </div>
  )
}
