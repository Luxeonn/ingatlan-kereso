import type { FC } from 'react'
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

  return (
    <div
      role="region"
      aria-label="Kijelölt hirdetések műveletei"
      data-testid="selection-action-bar"
      className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 w-fit max-w-[calc(100vw-2rem)] rounded-2xl border border-[var(--border-strong)] bg-[var(--bg-chrome)] px-4 py-2.5 text-xs shadow-2xl backdrop-blur-md ring-1 ring-blue-500/20"
    >
      <div className="flex flex-wrap items-center justify-center gap-2.5 font-semibold text-[var(--text-primary)]">
        <span data-testid="selection-count-label" className="tabular-nums text-[var(--text-primary)]">
          {selectedCount} kijelölve
        </span>
        <span aria-hidden="true" className="text-[var(--text-secondary)]">·</span>
        <button
          type="button"
          data-testid="merge-selected-button"
          aria-busy={isMerging}
          disabled={selectedCount < 2 || isMerging || !canMerge}
          onClick={onMerge}
          className="inline-flex items-center gap-1.5 rounded-xl bg-blue-600 px-3.5 py-1.5 font-bold text-white shadow-sm transition-colors hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isMerging ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />
          ) : (
            <GitMerge className="h-3.5 w-3.5" aria-hidden="true" />
          )}
          <span>Összevonás</span>
        </button>
        <span aria-hidden="true" className="text-[var(--text-secondary)]">·</span>
        <button
          type="button"
          data-testid="clear-selection-button"
          onClick={onClear}
          className="rounded-xl border border-[var(--border-strong)] bg-[var(--bg-input)] px-3 py-1.5 font-semibold text-[var(--text-secondary)] transition-colors hover:text-[var(--text-primary)]"
        >
          Kijelölés törlése
        </button>
      </div>
    </div>
  )
}
