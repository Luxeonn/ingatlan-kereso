import type { FC } from 'react'
import { Sun, Moon, Activity, RotateCcw, Home, Command, Database } from 'lucide-react'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import type { PortalStatusDto } from '../../types/portal'
import { cn } from '../../utils/cn'

export interface HeaderProps {
  totalInDb?: number
  portalStatuses?: PortalStatusDto[]
}

function resolvePortalHealthSummary(portalStatuses?: PortalStatusDto[]): {
  hasFailures: boolean
  label: string | null
} {
  if (!portalStatuses || portalStatuses.length === 0) {
    return { hasFailures: false, label: null }
  }
  const okCount = portalStatuses.filter((p) => p.status === 'SUCCESS').length
  const hasFailures = okCount < portalStatuses.length
  return {
    hasFailures,
    label: `${okCount}/${portalStatuses.length}`,
  }
}

export const Header: FC<HeaderProps> = ({ totalInDb, portalStatuses }) => {
  const { theme, toggleTheme, setDiagnosticsModalOpen, toggleCommandPalette } = useUiStore()
  const { resetToDefaults } = useFilterStore()
  const health = resolvePortalHealthSummary(portalStatuses)

  return (
    <header className="sticky top-0 z-40 w-full border-b border-[var(--border-hairline)] bg-[var(--bg-chrome)] backdrop-blur-md shadow-[0_8px_28px_-12px_rgba(2,6,23,0.35)] transition-colors duration-200">
      <div className="mx-auto flex max-w-[1600px] items-center justify-between px-4 py-2.5 sm:px-6">
        {/* Brand logo & title */}
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-md shadow-blue-500/20">
            <Home className="h-4.5 w-4.5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-extrabold tracking-tight text-[var(--text-primary)]">
                Ingatlan Kereső
              </h1>
              <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-400 border border-blue-500/20">
                SPA
              </span>
            </div>
            <p className="text-[11px] text-[var(--text-secondary)]">
              Valós idejű többportos aggregátor & piaci elemző
            </p>
          </div>
        </div>

        {/* Floating telemetry & action icons */}
        <div className="flex items-center gap-2 sm:gap-2.5">
          {/* Command Palette Trigger (Cmd/Ctrl+K) */}
          <button
            type="button"
            data-testid="command-palette-trigger"
            onClick={toggleCommandPalette}
            title="Gyorskereső és parancspaletta (Ctrl+K)"
            className="flex h-9 items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 text-xs font-semibold text-[var(--text-secondary)] transition-colors hover:border-blue-500/50 hover:text-[var(--text-primary)] cursor-pointer"
          >
            <Command className="h-3.5 w-3.5 text-blue-400" />
            <span className="hidden md:inline">Gyorsugró</span>
            <kbd className="hidden sm:inline-block rounded border border-[var(--border-hairline)] bg-[var(--bg-surface)] px-1.5 py-0.5 font-mono text-[10px] text-[var(--text-secondary)]">
              Ctrl+K
            </kbd>
          </button>

          {/* Database telemetry badge */}
          {typeof totalInDb === 'number' && totalInDb >= 0 && (
            <span
              data-testid="header-db-telemetry-pill"
              title="Mentett hirdetések száma a helyi adatbázisban"
              className="hidden lg:inline-flex h-9 items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 text-xs font-semibold text-[var(--text-secondary)] tabular-nums"
            >
              <Database className="h-3.5 w-3.5 text-indigo-400" />
              <span>{totalInDb} DB</span>
            </span>
          )}

          {/* Reset button */}
          <button
            type="button"
            onClick={resetToDefaults}
            title="Szűrők alaphelyzetbe állítása"
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)] cursor-pointer"
          >
            <RotateCcw className="h-4 w-4" />
          </button>

          {/* Diagnostics modal button / Health Pill */}
          <button
            type="button"
            onClick={() => setDiagnosticsModalOpen(true)}
            title="Portál Diagnosztika és Állapot"
            className="flex h-9 items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 text-xs font-medium text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)] sm:px-3 cursor-pointer tabular-nums"
          >
            <Activity
              className={cn(
                'h-4 w-4',
                health.hasFailures ? 'text-[var(--status-warn-fg)]' : 'text-[var(--status-bargain-fg)]',
              )}
            />
            <span className="hidden sm:inline">Diagnosztika</span>
            {health.label && (
              <span
                data-testid="header-portal-health-count"
                className={cn(
                  'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none',
                  health.hasFailures
                    ? 'bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)]'
                    : 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]',
                )}
              >
                {health.label}
              </span>
            )}
          </button>

          {/* Theme switcher */}
          <button
            type="button"
            onClick={toggleTheme}
            title={`Váltás ${theme === 'dark' ? 'világos' : 'sötét'} témára`}
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)] cursor-pointer"
          >
            {theme === 'dark' ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4 text-blue-600" />}
          </button>
        </div>
      </div>
    </header>
  )
}
