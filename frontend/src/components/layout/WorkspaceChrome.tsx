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
  const inactiveClass =
    'bg-[var(--bg-card)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border-hairline)]'

  return (
    <div
      data-testid="listings-panel-header"
      className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border-hairline)] pb-3"
    >
      <nav aria-label="Fő nézetek" className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          data-testid="tab-listings"
          aria-pressed={isListingsActive}
          onClick={onSelectListings}
          className={cn(
            'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all cursor-pointer',
            isListingsActive ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20' : inactiveClass,
          )}
        >
          <LayoutGrid className="h-4 w-4" />
          <span>Találatok</span>
        </button>

        <button
          type="button"
          data-testid="tab-archive"
          aria-pressed={isArchiveActive}
          onClick={onSelectArchive}
          className={cn(
            'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all cursor-pointer',
            isArchiveActive ? 'bg-purple-600 text-white shadow-md shadow-purple-500/20' : inactiveClass,
          )}
        >
          <Archive className="h-4 w-4" />
          <span>Archívum & Kuka</span>
        </button>

        <button
          type="button"
          data-testid="tab-analytics"
          aria-pressed={isAnalyticsActive}
          onClick={onSelectAnalytics}
          className={cn(
            'flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-all cursor-pointer',
            isAnalyticsActive ? 'bg-indigo-600 text-white shadow-md shadow-indigo-500/20' : inactiveClass,
          )}
        >
          <BarChart2 className="h-4 w-4" />
          <span>Piaci Elemzés</span>
        </button>
      </nav>
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
