import type { FC } from 'react'
import { createPortal } from 'react-dom'
import { GitMerge, Loader2 } from 'lucide-react'

export interface FloatingSelectionBarProps {
  selectedCount: number
  isMerging: boolean
  canMerge: boolean
  onMerge: () => void
  onClear: () => void
}

export const FloatingSelectionBar: FC<FloatingSelectionBarProps> = ({
  selectedCount,
  isMerging,
  canMerge,
  onMerge,
  onClear,
}) => {
  if (selectedCount === 0) return null

  const bar = (
    <div
      role="region"
      aria-label="Kijelölt hirdetések műveletei"
      data-testid="selection-action-bar"
      className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 w-fit max-w-[calc(100vw-2rem)] rounded-2xl glass-elevated ring-1 ring-[#0071e3]/20 px-4 py-2.5 text-xs"
    >
      <div className="flex flex-wrap items-center justify-center gap-2.5 font-semibold text-[#0b1220]">
        <span data-testid="selection-count-label" className="tabular-nums text-[#0b1220]">
          {selectedCount} kijelölve
        </span>
        <span aria-hidden="true" className="text-[#42526b]">·</span>
        <button
          type="button"
          data-testid="merge-selected-button"
          aria-busy={isMerging}
          disabled={selectedCount < 2 || isMerging || !canMerge}
          onClick={onMerge}
          className="inline-flex items-center gap-1.5 rounded-full bg-[#0071e3] px-3.5 py-1.5 font-bold text-white shadow-sm transition-colors hover:bg-[#0077ed] disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
        >
          {isMerging ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />
          ) : (
            <GitMerge className="h-3.5 w-3.5" aria-hidden="true" />
          )}
          <span>Összevonás</span>
        </button>
        <span aria-hidden="true" className="text-[#42526b]">·</span>
        <button
          type="button"
          data-testid="clear-selection-button"
          onClick={onClear}
          className="rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-semibold text-[#42526b] transition-colors hover:text-[#0b1220] hover:bg-white cursor-pointer"
        >
          Kijelölés törlése
        </button>
      </div>
    </div>
  )

  return typeof document !== 'undefined' ? createPortal(bar, document.body) : bar
}
