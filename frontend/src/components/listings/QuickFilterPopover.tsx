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
  { mode: 'only', label: 'Csak', activeClass: 'bg-emerald-600 text-white shadow-xs' },
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
          'flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-all shrink-0 cursor-pointer',
          isOpen
            ? 'border-blue-500/50 bg-blue-500/15 text-blue-600 shadow-sm'
            : 'border-[var(--border-color)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-blue-500/40',
        )}
      >
        <SlidersHorizontal className="h-3.5 w-3.5 text-blue-600" />
        <span>
          Szűrők ({pinnedQuickFilters.length}/{QUICK_FILTER_OPTIONS.length})
        </span>
      </button>

      {isOpen && (
        <div
          role="dialog"
          aria-label="Gyorsszűrők testreszabása"
          data-testid="quick-filter-selector-popover"
          className="absolute right-0 top-full z-50 mt-1.5 w-80 sm:w-96 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3 shadow-2xl backdrop-blur-md"
        >
          <div className="mb-2 flex items-center justify-between border-b border-[var(--border-color)] pb-1.5 text-[11px] font-bold text-[var(--text-secondary)]">
            <span>Kitűzött szűrők & 3-állapotú mód</span>
            <span>Összesen {QUICK_FILTER_OPTIONS.length} db</span>
          </div>
          <div className="space-y-1.5">
            {QUICK_FILTER_OPTIONS.map((opt) => {
              const isChecked = pinnedQuickFilters.includes(opt.id)
              const isDisabled = !isChecked && isMaxReached
              const currentMode = getTriStateMode(opt.id)

              return (
                <div
                  key={opt.id}
                  className="flex items-center justify-between gap-2 rounded-lg px-2 py-1 hover:bg-[var(--bg-input)]/70"
                >
                  <label
                    className={cn(
                      'flex flex-1 min-w-0 items-center gap-2 text-xs font-semibold',
                      isDisabled
                        ? 'cursor-not-allowed opacity-50 text-[var(--text-secondary)]'
                        : 'cursor-pointer text-[var(--text-primary)]',
                    )}
                  >
                    <input
                      type="checkbox"
                      data-testid={`quick-filter-checkbox-${opt.id}`}
                      checked={isChecked}
                      disabled={isDisabled}
                      onChange={() => onToggleFilter(opt.id)}
                      className="h-3.5 w-3.5 shrink-0 rounded border-[var(--border-color)] accent-blue-600"
                    />
                    <span className="truncate">{opt.label}</span>
                  </label>

                  <div
                    role="group"
                    aria-label={`${opt.label} állapota`}
                    className="inline-flex shrink-0 items-center rounded-md border border-[var(--border-hairline)] bg-[var(--bg-input)] p-0.5 text-[10px] font-bold"
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
                            'rounded px-1.5 py-0.5 transition-colors cursor-pointer',
                            active
                              ? btn.activeClass
                              : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
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
