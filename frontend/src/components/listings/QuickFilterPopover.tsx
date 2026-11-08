import { useState, useRef, useCallback, type FC } from 'react'
import { SlidersHorizontal } from 'lucide-react'
import { cn } from '../../utils/cn'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { MAX_PINNED_QUICK_FILTERS, type QuickFilterId } from '../../store/useUiStore'
import { QUICK_FILTER_OPTIONS } from './quickFilterConstants'

interface QuickFilterPopoverProps {
  pinnedQuickFilters: QuickFilterId[]
  onToggleFilter: (id: QuickFilterId) => void
}

export const QuickFilterPopover: FC<QuickFilterPopoverProps> = ({
  pinnedQuickFilters,
  onToggleFilter,
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const closePopover = useCallback(() => setIsOpen(false), [])

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
          'flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-all shrink-0',
          isOpen
            ? 'border-blue-500/50 bg-blue-500/15 text-blue-400 shadow-sm'
            : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
        )}
      >
        <SlidersHorizontal className="h-3.5 w-3.5 text-blue-400" />
        <span>
          Szűrők ({pinnedQuickFilters.length}/{QUICK_FILTER_OPTIONS.length})
        </span>
      </button>

      {isOpen && (
        <div
          role="dialog"
          aria-label="Gyorsszűrők testreszabása"
          data-testid="quick-filter-selector-popover"
          className="absolute right-0 top-full z-50 mt-1.5 w-60 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-2.5 shadow-2xl backdrop-blur-md"
        >
          <div className="mb-2 flex items-center justify-between border-b border-[var(--border-color)] pb-1.5 text-[11px] font-bold text-[var(--text-secondary)]">
            <span>Kitűzött szűrők</span>
            <span>Összesen {QUICK_FILTER_OPTIONS.length} db</span>
          </div>
          <div className="space-y-1">
            {QUICK_FILTER_OPTIONS.map((opt) => {
              const isChecked = pinnedQuickFilters.includes(opt.id)
              const isDisabled = !isChecked && isMaxReached
              return (
                <label
                  key={opt.id}
                  className={cn(
                    'flex items-center gap-2 rounded-lg px-2 py-1.5 text-xs font-medium transition-colors',
                    isDisabled
                      ? 'cursor-not-allowed opacity-50 text-[var(--text-secondary)]'
                      : 'cursor-pointer text-[var(--text-primary)] hover:bg-[var(--bg-input)]',
                  )}
                >
                  <input
                    type="checkbox"
                    data-testid={`quick-filter-checkbox-${opt.id}`}
                    checked={isChecked}
                    disabled={isDisabled}
                    onChange={() => onToggleFilter(opt.id)}
                    className="h-3.5 w-3.5 rounded border-[var(--border-color)] accent-blue-600"
                  />
                  <span>{opt.label}</span>
                </label>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
