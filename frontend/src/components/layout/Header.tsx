import { Sun, Moon, Activity, RotateCcw, Home } from 'lucide-react'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'

export const Header: React.FC = () => {
  const { theme, toggleTheme, setDiagnosticsModalOpen } = useUiStore()
  const { resetToDefaults } = useFilterStore()

  return (
    <header className="sticky top-0 z-40 w-full border-b border-[var(--border-color)] bg-[var(--bg-card)]/90 backdrop-blur-md transition-colors duration-200">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        {/* Brand logo & title */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg shadow-blue-500/20">
            <Home className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-extrabold tracking-tight text-[var(--text-primary)]">
                Ingatlan Kereső
              </h1>
              <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-[11px] font-semibold text-blue-400 border border-blue-500/20">
                SPA
              </span>
            </div>
            <p className="text-xs text-[var(--text-secondary)]">
              Valós idejű többportos aggregátor & piaci elemző
            </p>
          </div>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Reset button */}
          <button
            type="button"
            onClick={resetToDefaults}
            title="Szűrők alaphelyzetbe állítása"
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)]"
          >
            <RotateCcw className="h-4 w-4" />
          </button>

          {/* Diagnostics modal button */}
          <button
            type="button"
            onClick={() => setDiagnosticsModalOpen(true)}
            title="Portál Diagnosztika és Állapot"
            className="flex h-9 items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 text-xs font-medium text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)] sm:px-3"
          >
            <Activity className="h-4 w-4 text-emerald-400" />
            <span className="hidden sm:inline">Diagnosztika</span>
          </button>

          {/* Theme switcher */}
          <button
            type="button"
            onClick={toggleTheme}
            title={`Váltás ${theme === 'dark' ? 'világos' : 'sötét'} témára`}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-[var(--text-primary)]"
          >
            {theme === 'dark' ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4 text-blue-600" />}
          </button>
        </div>
      </div>
    </header>
  )
}
