import type { FC, ReactNode } from 'react'
import {
  Activity,
  RotateCcw,
  Home,
  Database,
  PanelLeftClose,
  PanelLeftOpen,
  LayoutGrid,
  Star,
  Archive,
  BarChart2,
} from 'lucide-react'
import { useUiStore, type DashboardTab, type ListingSubView } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import type { PortalStatusDto } from '../../types/portal'
import { cn } from '../../utils/cn'

export interface TopNavBarProps {
  totalInDb?: number
  portalStatuses?: PortalStatusDto[]
  listingsCount?: number
  favoritesCount?: number
  archiveCount?: number
  analyticsCount?: number
  dashboardTab?: DashboardTab
  listingSubView?: ListingSubView
  isFiltersCollapsed?: boolean
  onToggleFilters?: () => void
  onSelectListings?: () => void
  onSelectFavorites?: () => void
  onSelectArchive?: () => void
  onSelectAnalytics?: () => void
}

function resolvePortalHealthSummary(portalStatuses?: PortalStatusDto[]): {
  hasFailures: boolean
  label: string | null
} {
  if (!portalStatuses || portalStatuses.length === 0) {
    return { hasFailures: false, label: null }
  }
  const okCount = portalStatuses.filter((p) => p.status === 'SUCCESS').length
  return {
    hasFailures: okCount < portalStatuses.length,
    label: `${okCount}/${portalStatuses.length}`,
  }
}

interface CapsuleTabItem {
  testId: string
  label: string
  count: number
  showZeroCount: boolean
  isActive: boolean
  icon: ReactNode
  onClick: () => void
}

const NavTabCapsule: FC<{ tabs: CapsuleTabItem[] }> = ({ tabs }) => (
  <nav
    data-testid="listings-panel-header"
    role="tablist"
    aria-label="Fő nézetek"
    className="flex items-center gap-1 rounded-full p-1 glass-capsule shrink-0"
  >
    {tabs.map((tab) => (
      <button
        key={tab.testId}
        type="button"
        role="tab"
        data-testid={tab.testId}
        aria-label={tab.label}
        aria-selected={tab.isActive}
        aria-pressed={tab.isActive}
        onClick={tab.onClick}
        className={cn(
          'inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold transition-all cursor-pointer select-none whitespace-nowrap focus:outline-none focus-visible:ring-2 focus-visible:ring-[#0071e3]/50',
          tab.isActive
            ? 'bg-[#0071e3] text-white shadow-sm'
            : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]',
        )}
      >
        {tab.icon}
        <span>{tab.label}</span>
        {(tab.showZeroCount || tab.count > 0) && (
          <span
            className={cn(
              'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none tabular-nums',
              tab.isActive ? 'bg-white/25 text-white' : 'bg-black/5 text-[var(--text-secondary)]',
            )}
          >
            {tab.count}
          </span>
        )}
      </button>
    ))}
  </nav>
)

interface NavRightActionsProps {
  totalInDb?: number
  health: { hasFailures: boolean; label: string | null }
  onReset: () => void
  onOpenDiagnostics: () => void
}

const NavRightActions: FC<NavRightActionsProps> = ({
  totalInDb,
  health,
  onReset,
  onOpenDiagnostics,
}) => (
  <div className="flex items-center gap-2 shrink-0">
    {typeof totalInDb === 'number' && totalInDb >= 0 && (
      <span
        data-testid="header-db-telemetry-pill"
        title="Mentett hirdetések száma a helyi adatbázisban"
        className="inline-flex h-8 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/65 px-3 text-xs font-semibold text-[var(--text-secondary)] tabular-nums"
      >
        <Database className="h-3.5 w-3.5 text-[#0071e3]" />
        <span>{totalInDb} DB</span>
      </span>
    )}

    <button
      type="button"
      onClick={onReset}
      aria-label="Szűrők alaphelyzetbe állítása"
      title="Szűrők alaphelyzetbe állítása"
      className="flex h-8 w-8 items-center justify-center rounded-full border border-[var(--border-hairline)] bg-white/65 text-[var(--text-secondary)] transition-colors hover:border-[#0071e3]/50 hover:text-[var(--text-primary)] cursor-pointer"
    >
      <RotateCcw className="h-3.5 w-3.5" />
    </button>

    <button
      type="button"
      onClick={onOpenDiagnostics}
      title="Portál Diagnosztika és Állapot"
      className="flex h-8 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/65 px-3 text-xs font-semibold text-[var(--text-secondary)] transition-colors hover:border-[#0071e3]/50 hover:text-[var(--text-primary)] cursor-pointer tabular-nums"
    >
      <Activity
        className={cn(
          'h-3.5 w-3.5',
          health.hasFailures ? 'text-[var(--status-warn-fg)]' : 'text-[var(--status-bargain-fg)]',
        )}
      />
      <span>Diagnosztika</span>
      {health.label && (
        <span
          data-testid="header-portal-health-count"
          className={cn(
            'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none tabular-nums',
            health.hasFailures
              ? 'bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)]'
              : 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]',
          )}
        >
          {health.label}
        </span>
      )}
    </button>
  </div>
)

export const TopNavBar: FC<TopNavBarProps> = ({
  totalInDb,
  portalStatuses,
  listingsCount = 0,
  favoritesCount = 0,
  archiveCount = 0,
  analyticsCount = 0,
  dashboardTab,
  listingSubView,
  isFiltersCollapsed,
  onToggleFilters,
  onSelectListings,
  onSelectFavorites,
  onSelectArchive,
  onSelectAnalytics,
}) => {
  const ui = useUiStore()
  const { resetToDefaults } = useFilterStore()
  const health = resolvePortalHealthSummary(portalStatuses)

  const activeDashboardTab = dashboardTab ?? ui.dashboardTab
  const activeListingSubView = listingSubView ?? ui.listingSubView
  const collapsed = isFiltersCollapsed ?? ui.isFiltersCollapsed

  const selectListingView = (subView: ListingSubView, custom?: () => void) => {
    if (custom) {
      custom()
      return
    }
    ui.setDashboardTab('listings')
    ui.setListingSubView(subView)
  }

  const tabs: CapsuleTabItem[] = [
    {
      testId: 'tab-listings',
      label: 'Találatok',
      count: listingsCount,
      showZeroCount: true,
      isActive: activeDashboardTab === 'listings' && activeListingSubView === 'listings',
      icon: <LayoutGrid className="h-3.5 w-3.5" />,
      onClick: () => selectListingView('listings', onSelectListings),
    },
    {
      testId: 'subview-favorites',
      label: 'Kedvencek',
      count: favoritesCount,
      showZeroCount: true,
      isActive: activeDashboardTab === 'listings' && activeListingSubView === 'favorites',
      icon: <Star className="h-3.5 w-3.5" />,
      onClick: () => selectListingView('favorites', onSelectFavorites),
    },
    {
      testId: 'tab-archive',
      label: 'Archívum',
      count: archiveCount,
      showZeroCount: true,
      isActive: activeDashboardTab === 'listings' && activeListingSubView === 'archive',
      icon: <Archive className="h-3.5 w-3.5" />,
      onClick: () => selectListingView('archive', onSelectArchive),
    },
    {
      testId: 'tab-analytics',
      label: 'Piaci Elemzés',
      count: analyticsCount,
      showZeroCount: false,
      isActive: activeDashboardTab === 'analytics',
      icon: <BarChart2 className="h-3.5 w-3.5" />,
      onClick: () => (onSelectAnalytics ? onSelectAnalytics() : ui.setDashboardTab('analytics')),
    },
  ]

  return (
    <header className="sticky top-0 z-40 h-[56px] w-full border-b border-[var(--border-hairline)] glass-chrome">
      <div className="mx-auto flex h-full max-w-[1640px] items-center justify-between gap-3 px-5">
        <div className="flex items-center gap-2.5 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-[#0071e3] to-[#005bb5] text-white shadow-sm">
              <Home className="h-4 w-4" />
            </div>
            <div className="flex items-center gap-1.5">
              <h1 className="text-base font-extrabold tracking-tight text-[var(--text-primary)] whitespace-nowrap">
                Ingatlan Kereső
              </h1>
              <span className="rounded-full bg-[#0071e3]/10 px-2 py-0.5 text-[10px] font-bold text-[#005bb5] border border-[#0071e3]/20">
                SPA
              </span>
            </div>
          </div>

          <button
            type="button"
            data-testid="sidebar-toggle-btn"
            aria-expanded={!collapsed}
            onClick={() => (onToggleFilters ? onToggleFilters() : ui.setFiltersCollapsed(!collapsed))}
            className="inline-flex h-8 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/65 px-3 text-xs font-semibold text-[var(--text-secondary)] shadow-xs transition-colors hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] cursor-pointer whitespace-nowrap"
          >
            {collapsed ? (
              <PanelLeftOpen className="h-3.5 w-3.5 text-[#0071e3]" />
            ) : (
              <PanelLeftClose className="h-3.5 w-3.5 text-[#0071e3]" />
            )}
            <span>{collapsed ? 'Kibontás' : 'Összecsukás'}</span>
          </button>
        </div>

        <NavTabCapsule tabs={tabs} />

        <NavRightActions
          totalInDb={totalInDb}
          health={health}
          onReset={resetToDefaults}
          onOpenDiagnostics={() => ui.setDiagnosticsModalOpen(true)}
        />
      </div>
    </header>
  )
}
