import React, { useState, useRef, useId, useCallback } from 'react'
import { Check, AlertTriangle } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  clampQualityScore,
  getQualityConfig,
  type QualityConfig,
  type QualityTierKey,
} from '../../utils/formatters'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { cn } from '../../utils/cn'

export type { QualityConfig, QualityTierKey }

export interface QualityScoreShieldProps {
  item: IngatlanItem
  className?: string
}

export const QualityScoreShield: React.FC<QualityScoreShieldProps> = ({ item, className = '' }) => {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const popoverId = useId()
  const closePopover = useCallback(() => setIsOpen(false), [])

  useOutsideDismiss(containerRef, isOpen, closePopover)

  const clampedScore = clampQualityScore(item.qualityScore)
  if (clampedScore == null) {
    return null
  }

  const deductions = item.qualityDeductions ?? []
  const config = getQualityConfig(clampedScore)
  const Icon = config.icon

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
    <div ref={containerRef} className={cn('relative inline-block', className)}>
      <button
        type="button"
        role="button"
        tabIndex={0}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        aria-controls={popoverId}
        aria-label={`Adatminőség: ${clampedScore}/100 pont (${config.ratingLabel})`}
        data-testid="quality-score-button"
        data-rating={config.key}
        onClick={(e) => {
          e.stopPropagation()
          setIsOpen((prev) => !prev)
        }}
        onMouseEnter={() => setIsOpen(true)}
        onMouseLeave={() => setIsOpen(false)}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setIsOpen(false)}
        onKeyDown={handleKeyDown}
        className={cn(
          'inline-flex items-center gap-1.5 rounded-md border px-2 py-0.5 text-[11px] font-bold transition-all shadow-sm focus:outline-none focus:ring-2 cursor-pointer',
          config.buttonClasses,
        )}
      >
        <Icon className={cn('h-3.5 w-3.5 shrink-0', config.textClass)} aria-hidden="true" />
        <span className="hidden sm:inline">Minőség</span>
        <span
          data-testid="quality-score-pill"
          className={cn('rounded px-1 py-0.2 text-[10px] font-mono font-extrabold', config.pillClass)}
        >
          {clampedScore}/100
        </span>
      </button>

      {isOpen && (
        <div
          id={popoverId}
          role="dialog"
          aria-label="Adatminőségi értékelés részletei"
          data-testid="quality-score-popover"
          onClick={(e) => e.stopPropagation()}
          className={cn(
            'absolute top-full left-0 mt-1.5 z-50 w-72 sm:w-80 max-w-[calc(100vw-2.5rem)] rounded-xl border bg-slate-900/95 dark:bg-slate-900/95 p-3.5 shadow-2xl backdrop-blur-md text-slate-200 animate-in fade-in zoom-in-95 duration-150',
            config.popoverBorderClass,
          )}
        >
          <div className={cn('flex items-center justify-between pb-2 mb-2.5 border-b', config.popoverBorderClass)}>
            <div className={cn('flex items-center gap-1.5 text-xs font-bold', config.headerTextClass)}>
              <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
              <span>Adatminőség • {config.ratingLabel}</span>
            </div>
            <span
              data-testid="popover-score-badge"
              className={cn('rounded px-1.5 py-0.5 text-[11px] font-mono font-extrabold', config.pillClass)}
            >
              {clampedScore}/100 pont
            </span>
          </div>

          <div className="mb-3 space-y-1">
            <div className="flex justify-between text-[10px] text-slate-400">
              <span>Megbízhatósági index</span>
              <span className="font-mono font-semibold">{clampedScore}%</span>
            </div>
            <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
              <div
                className={cn('h-full transition-all duration-300 rounded-full', config.barClass)}
                style={{ width: `${clampedScore}%` }}
              />
            </div>
          </div>

          <div className="space-y-1.5 text-xs">
            <div className="text-[11px] font-semibold text-slate-300">Értékelési szempontok:</div>
            {deductions.length > 0 ? (
              <ul className="space-y-1 text-[11px] text-slate-300 pl-1" data-testid="deductions-list">
                {deductions.map((deduction, idx) => (
                  <li key={idx} className="flex items-start gap-1.5 text-rose-300/90">
                    <span className="text-rose-400 font-bold shrink-0">•</span>
                    <span>{deduction}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <div
                className="flex items-center gap-1.5 text-[11px] text-emerald-400"
                data-testid="no-deductions-message"
              >
                <Check className="h-3.5 w-3.5 shrink-0" />
                <span>Nincs levonás. Minden ellenőrzött adatmező megbízható és teljes.</span>
              </div>
            )}
          </div>

          {clampedScore < 70 && (
            <div
              data-testid="outlier-warning"
              className="mt-2.5 pt-2 border-t border-rose-500/20 text-[10px] text-rose-300/90 flex items-start gap-1.5"
            >
              <AlertTriangle className="h-3.5 w-3.5 text-rose-400 shrink-0 mt-0.5" />
              <span>
                70 pont alatti minőség: a hirdetés ki van zárva a piaci mediánár-számításból a statisztikai pontosság
                érdekében (#267).
              </span>
            </div>
          )}

          <div className="mt-2.5 pt-2 border-t border-slate-800 text-[10px] text-slate-400 italic">
            Automatikus szabályalapú minőségellenőrzés (0–100 pont).
          </div>
        </div>
      )}
    </div>
  )
}
