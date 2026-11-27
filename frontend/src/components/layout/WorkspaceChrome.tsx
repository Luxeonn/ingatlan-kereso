import { type FC } from 'react'
import type { IngatlanItem } from '../../types/property'
import { cn } from '../../utils/cn'

export interface GlobalUndoToastProps {
  toastMessage: string | null
  toastUndoItem: IngatlanItem | null
  hasSelectedCards: boolean
  onRestoreUndo: (item: IngatlanItem) => void
  onClear: () => void
}

export const GlobalUndoToast: FC<GlobalUndoToastProps> = ({
  toastMessage,
  toastUndoItem,
  hasSelectedCards,
  onRestoreUndo,
  onClear,
}) => {
  if (!toastMessage) return null
  return (
    <aside
      role="status"
      aria-live="polite"
      data-testid="undo-hide-toast"
      className={cn(
        'fixed right-6 z-50 flex items-center gap-3 rounded-2xl glass-elevated px-4 py-3 animate-in fade-in slide-in-from-bottom-5 duration-200 transition-all',
        hasSelectedCards ? 'bottom-20' : 'bottom-6',
      )}
    >
      <span className="text-xs font-semibold text-[var(--text-primary)]">{toastMessage}</span>
      {toastUndoItem && (
        <button
          type="button"
          onClick={() => {
            onRestoreUndo(toastUndoItem)
            onClear()
          }}
          className="rounded-xl bg-[#0071e3] px-3 py-1 text-xs font-bold text-white hover:bg-[#0077ed] transition-colors cursor-pointer"
        >
          Visszavonás
        </button>
      )}
      <button
        type="button"
        onClick={onClear}
        aria-label="Bezárás"
        className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs cursor-pointer"
      >
        ✕
      </button>
    </aside>
  )
}
