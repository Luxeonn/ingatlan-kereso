import { useState, useRef, useCallback, type FC } from 'react'
import { SlidersHorizontal } from 'lucide-react'
import { cn } from '../../utils/cn'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { MAX_PINNED_QUICK_FILTERS, type QuickFilterId } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import { QUICK_FILTER_OPTIONS } from './quickFilterConstants'
import { getTriStateMode, setTriStateMode, type TriStateMode } from './quickFilterTriState'

interface QuickFilterPopoverProps {
  pinnedQuickFilters: QuickFilterId[]
  onToggleFilter: (id: QuickFilterId) => void
}

const MODE_BUTTONS: Array<{ mode: TriStateMode; label: string; activeClass: string }> = [
  { mode: 'all', label: 'Mind', activeClass: 'bg-slate-700 text-white shadow-xs' },
  { mode: 'only', label: 'Csak', activeClass: 'bg-[#0071e3] text-white shadow-xs' },
  { mode: 'exclude', label: 'Kizár', activeClass: 'bg-rose-600 text-white shadow-xs' },
]

export const QuickFilterPopover: FC<QuickFilterPopoverProps> = ({
  pinnedQuickFilters,
  onToggleFilter,
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const closePopover = useCallback(() => setIsOpen(false), [])
  useFilterStore()

  useOutsideDismiss(containerRef, isOpen, closePopover, true)

  const isMaxReached = pinnedQuickFilters.length >= MAX_PINNED_QUICK_FILTERS

  return (
    <div className="relative shrink-0" ref={containerRef}>
      <button
        type="button"
        data-testid="quick-filter-selector-btn"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        onClick={() => setIsOpen((prev) => !prev)}
        title="Megjelenített gyorsszűrők testreszabása"
        className={cn(
          'flex h-8 items-center gap-1.5 rounded-full border px-3 text-xs font-semibold transition-all shrink-0 cursor-pointer tabular-nums',
          isOpen
            ? 'border-[#0071e3]/45 bg-[#0071e3]/12 text-[var(--status-info-fg)] shadow-sm'
            : 'border-[var(--border-hairline)] bg-white/75 text-[#0b1220] hover:border-[#0071e3]/40',
        )}
      >
        <SlidersHorizontal className="h-3.5 w-3.5 text-[#0071e3]" />
        <span>
          Szűrők ({pinnedQuickFilters.length}/{QUICK_FILTER_OPTIONS.length})
        </span>
      </button>

      {isOpen && (
        <div
          role="dialog"
          aria-label="Gyorsszűrők testreszabása"
          data-testid="quick-filter-selector-popover"
          className="absolute right-0 top-full z-40 mt-1.5 w-96 rounded-2xl glass-elevated p-3"
        >
          <div className="mb-2 flex items-center justify-between border-b border-[var(--border-hairline)] pb-1.5 text-[11px] font-bold text-[#42526b]">
            <span>Kitűzött szűrők & 3-állapotú mód</span>
            <span className="tabular-nums">Összesen {QUICK_FILTER_OPTIONS.length} db</span>
          </div>
          <div className="space-y-1.5">
            {QUICK_FILTER_OPTIONS.map((opt) => {
              const isChecked = pinnedQuickFilters.includes(opt.id)
              const isDisabled = !isChecked && isMaxReached
              const currentMode = getTriStateMode(opt.id)

              return (
                <div
                  key={opt.id}
                  className="flex items-center justify-between gap-2 rounded-xl px-2 py-1 hover:bg-white/60"
                >
                  <label
                    className={cn(
                      'flex flex-1 min-w-0 items-center gap-2 text-xs font-semibold',
                      isDisabled
                        ? 'cursor-not-allowed opacity-50 text-[#42526b]'
                        : 'cursor-pointer text-[#0b1220]',
                    )}
                  >
                    <input
                      type="checkbox"
                      data-testid={`quick-filter-checkbox-${opt.id}`}
                      checked={isChecked}
                      disabled={isDisabled}
                      onChange={() => onToggleFilter(opt.id)}
                      className="h-3.5 w-3.5 shrink-0 rounded border-[var(--border-strong)] accent-[#0071e3]"
                    />
                    <span className="truncate">{opt.label}</span>
                  </label>

                  <div
                    role="group"
                    aria-label={`${opt.label} állapota`}
                    className="inline-flex shrink-0 items-center rounded-full border border-[var(--border-hairline)] bg-white/75 p-0.5 text-[10px] font-bold"
                  >
                    {MODE_BUTTONS.map((btn) => {
                      const active = currentMode === btn.mode
                      return (
                        <button
                          key={btn.mode}
                          type="button"
                          data-testid={`tri-state-${opt.id}-${btn.mode}`}
                          aria-pressed={active}
                          onClick={() => setTriStateMode(opt.id, btn.mode)}
                          className={cn(
                            'rounded-full px-2 py-0.5 transition-colors cursor-pointer',
                            active
                              ? btn.activeClass
                              : 'text-[#42526b] hover:text-[#0b1220]',
                          )}
                        >
                          {btn.label}
                        </button>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
