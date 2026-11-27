import React, { useState, useRef, useId, useCallback } from 'react'
import { TrendingDown, Sparkles } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { formatPricePerSqm, getEffectiveSqmPrice } from '../../utils/formatters'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'

export interface BargainBadgeProps {
  item: IngatlanItem
  className?: string
  variant?: 'default' | 'capsule'
}

interface BargainStats {
  isBargain: boolean
  sampleSize: number
  percentageBelow: number | null
  refMedian: number | null | undefined
  currentPricePerSqm: number | null | undefined
  roundedPct: number
}

function resolvePercentageBelow(item: IngatlanItem): number | null {
  if (item.percentageBelowMedian !== undefined && item.percentageBelowMedian !== null) {
    return item.percentageBelowMedian
  }
  return null
}

function resolveSampleSize(item: IngatlanItem): number {
  if (item.referenceSampleSize !== undefined && item.referenceSampleSize !== null) {
    return item.referenceSampleSize
  }
  return 0
}

function extractBargainStats(item: IngatlanItem): BargainStats | null {
  const isBargain = Boolean(item.isBargain)
  const sampleSize = resolveSampleSize(item)
  const percentageBelow = resolvePercentageBelow(item)

  if (!isBargain || sampleSize < 5 || percentageBelow === null || percentageBelow <= 0) {
    return null
  }

  return {
    isBargain,
    sampleSize,
    percentageBelow,
    refMedian: item.referenceMedianPricePerSquareMeter,
    currentPricePerSqm: getEffectiveSqmPrice(item),
    roundedPct: Math.round(percentageBelow),
  }
}

interface BargainPopoverProps {
  popoverId: string
  city?: string
  currentPricePerSqm?: number | null
  refMedian?: number | null
  roundedPct: number
  sampleSize: number
}

const BargainPopover: React.FC<BargainPopoverProps> = ({
  popoverId,
  city,
  currentPricePerSqm,
  refMedian,
  roundedPct,
  sampleSize,
}) => (
  <div
    id={popoverId}
    role="dialog"
    aria-label="Alkalmi vétel részletei"
    data-testid="bargain-popover"
    onClick={(e) => e.stopPropagation()}
    className="absolute top-full left-0 mt-1.5 z-50 w-72 max-w-[calc(100vw-2.5rem)] rounded-2xl border border-emerald-500/30 glass-elevated p-3.5 text-[#0b1220] animate-in fade-in zoom-in-95 duration-150"
  >
    <div className="flex items-center justify-between pb-2 mb-2 border-b border-[var(--border-hairline)]">
      <div className="flex items-center gap-1.5 text-xs font-bold text-[var(--status-bargain-fg)]">
        <Sparkles className="h-3.5 w-3.5 text-[var(--status-bargain-fg)]" aria-hidden="true" />
        <span>Piaci Árelemzés • Alkalmi Vétel</span>
      </div>
      <span className="text-[10px] text-[#42526b] font-mono">
        {city}
      </span>
    </div>

    <div className="space-y-1.5 text-xs tabular-nums">
      <div className="flex justify-between items-center">
        <span className="text-[#42526b]">Kínálati m² ár:</span>
        <span className="font-bold text-[var(--status-bargain-fg)] font-mono">
          {formatPricePerSqm(currentPricePerSqm)}
        </span>
      </div>

      {refMedian !== null && refMedian !== undefined && (
        <div className="flex justify-between items-center">
          <span className="text-[#42526b]">Városi medián m² ár:</span>
          <span className="font-semibold text-[#0b1220] font-mono">
            {formatPricePerSqm(refMedian)}
          </span>
        </div>
      )}

      <div className="flex justify-between items-center pt-1 border-t border-[var(--border-hairline)]">
        <span className="text-[#42526b]">Eltérés a mediántól:</span>
        <span className="font-extrabold text-[var(--status-bargain-fg)] font-mono">
          -{roundedPct}%
        </span>
      </div>

      <div className="flex justify-between items-center text-[11px] text-[#42526b]">
        <span>Minta elemszáma:</span>
        <span className="font-medium text-[#0b1220]">{sampleSize} hirdetés</span>
      </div>
    </div>

    <div className="mt-2.5 pt-2 border-t border-[var(--border-hairline)] text-[10px] text-[#42526b] italic">
      Legalább 5 azonos típusú helyi hirdetésből számított medián alapján.
    </div>
  </div>
)

export const BargainBadge: React.FC<BargainBadgeProps> = ({
  item,
  className = '',
  variant = 'default',
}) => {
  const [isPinned, setIsPinned] = useState(false)
  const [isHoverOrFocus, setIsHoverOrFocus] = useState(false)
  const isOpen = isPinned || isHoverOrFocus
  const containerRef = useRef<HTMLDivElement>(null)
  const popoverId = useId()
  const closePopover = useCallback(() => {
    setIsPinned(false)
    setIsHoverOrFocus(false)
  }, [])

  useOutsideDismiss(containerRef, isOpen, closePopover, true)

  const stats = extractBargainStats(item)
  if (!stats) {
    return null
  }

  const { sampleSize, refMedian, currentPricePerSqm, roundedPct } = stats

  const handleTogglePin = () => {
    if (isPinned) {
      closePopover()
    } else {
      setIsPinned(true)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.stopPropagation()
      closePopover()
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      e.stopPropagation()
      handleTogglePin()
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
          handleTogglePin()
        }}
        onMouseEnter={() => setIsHoverOrFocus(true)}
        onMouseLeave={() => setIsHoverOrFocus(false)}
        onFocus={() => setIsHoverOrFocus(true)}
        onBlur={() => setIsHoverOrFocus(false)}
        onKeyDown={handleKeyDown}
        className={
          variant === 'capsule'
            ? 'inline-flex items-center gap-1 rounded-full glass-capsule px-2.5 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)] hover:opacity-95 transition-all focus:outline-none focus:ring-2 focus:ring-emerald-400/50 cursor-pointer tabular-nums'
            : 'inline-flex items-center gap-1.5 rounded-full bg-[var(--status-bargain-bg)] border border-emerald-500/30 px-2.5 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)] hover:opacity-90 transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-400/50 cursor-pointer tabular-nums'
        }
      >
        <TrendingDown className="h-3 w-3 shrink-0 text-[var(--status-bargain-fg)]" aria-hidden="true" />
        <span>Alkalmi vétel</span>
        <span
          data-testid="bargain-discount-pill"
          className="ml-0.5 rounded bg-emerald-500/20 px-1 py-0.2 text-[10px] font-extrabold text-[var(--status-bargain-fg)]"
        >
          -{roundedPct}%
        </span>
      </button>

      {isOpen && (
        <BargainPopover
          popoverId={popoverId}
          city={item.city}
          currentPricePerSqm={currentPricePerSqm}
          refMedian={refMedian}
          roundedPct={roundedPct}
          sampleSize={sampleSize}
        />
      )}
    </div>
  )
}
