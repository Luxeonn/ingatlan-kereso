import { type FC } from 'react'
import { LayoutGrid, BarChart2, Archive } from 'lucide-react'
import type { DashboardTab, ListingSubView } from '../../store/useUiStore'
import type { IngatlanItem } from '../../types/property'
import { cn } from '../../utils/cn'

export interface WorkspaceHeaderNavProps {
  dashboardTab: DashboardTab
  listingSubView: ListingSubView
  onSelectListings: () => void
  onSelectArchive: () => void
  onSelectAnalytics: () => void
}

export const WorkspaceHeaderNav: FC<WorkspaceHeaderNavProps> = ({
  dashboardTab,
  listingSubView,
  onSelectListings,
  onSelectArchive,
  onSelectAnalytics,
}) => {
  const isListingsActive = dashboardTab === 'listings' && listingSubView !== 'archive'
  const isArchiveActive = dashboardTab === 'listings' && listingSubView === 'archive'
  const isAnalyticsActive = dashboardTab === 'analytics'

  const baseTabClass =
    'relative flex items-center gap-2 rounded-t-xl border px-4 py-2.5 text-xs font-bold transition-colors cursor-pointer select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/40'
  const inactiveTabClass =
    'border-transparent bg-transparent text-[var(--text-secondary)] hover:bg-[var(--bg-input)]/80 hover:text-[var(--text-primary)]'

  return (
    <div
      data-testid="listings-panel-header"
      className="relative z-10 flex flex-wrap items-end justify-between gap-2"
    >
      <div role="tablist" aria-label="Fő nézetek" className="flex flex-wrap items-end gap-1">
        <button
          type="button"
          role="tab"
          data-testid="tab-listings"
          aria-selected={isListingsActive}
          aria-pressed={isListingsActive}
          onClick={onSelectListings}
          className={cn(
            baseTabClass,
            isListingsActive
              ? '-mb-px border-[var(--border-hairline)] border-t-2 border-t-blue-500 border-b-[var(--bg-surface)] bg-[var(--bg-surface)] text-blue-600'
              : inactiveTabClass,
          )}
        >
          <LayoutGrid className="h-4 w-4" />
          <span>Találatok</span>
        </button>

        <button
          type="button"
          role="tab"
          data-testid="tab-archive"
          aria-selected={isArchiveActive}
          aria-pressed={isArchiveActive}
          onClick={onSelectArchive}
          className={cn(
            baseTabClass,
            isArchiveActive
              ? '-mb-px border-[var(--border-hairline)] border-t-2 border-t-purple-500 border-b-[var(--bg-surface)] bg-[var(--bg-surface)] text-purple-700'
              : inactiveTabClass,
          )}
        >
          <Archive className="h-4 w-4" />
          <span>Archívum & Kuka</span>
        </button>

        <button
          type="button"
          role="tab"
          data-testid="tab-analytics"
          aria-selected={isAnalyticsActive}
          aria-pressed={isAnalyticsActive}
          onClick={onSelectAnalytics}
          className={cn(
            baseTabClass,
            isAnalyticsActive
              ? '-mb-px border-[var(--border-hairline)] border-t-2 border-t-indigo-500 border-b-[var(--bg-surface)] bg-[var(--bg-surface)] text-indigo-700'
              : inactiveTabClass,
          )}
        >
          <BarChart2 className="h-4 w-4" />
          <span>Piaci Elemzés</span>
        </button>
      </div>
    </div>
  )
}

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
        'fixed right-6 z-50 flex items-center gap-3 rounded-xl border border-[var(--border-strong)] bg-[var(--bg-chrome)] px-4 py-3 shadow-xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-5 duration-200 transition-all',
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
          className="rounded-lg bg-blue-600 px-2.5 py-1 text-xs font-bold text-white hover:bg-blue-500 transition-colors cursor-pointer"
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
