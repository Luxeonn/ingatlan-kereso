import { useState, useMemo, useEffect, useRef, useCallback, type FC } from 'react'
import {
  Search,
  X,
  LayoutGrid,
  List,
  Table2,
  Star,
  Archive,
  BarChart2,
  TrendingDown,
  Sparkles,
  ShieldCheck,
  Activity,
  Upload,
  RotateCcw,
} from 'lucide-react'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { cn } from '../../utils/cn'

export interface CommandPaletteModalProps {
  onTriggerSearch: () => void
}

interface CommandActionItem {
  id: string
  group: string
  label: string
  description: string
  icon: FC<{ className?: string }>
  onExecute: () => void
}

interface BuildActionsParams {
  onTriggerSearch: () => void
  closePalette: () => void
  onlyBargain: boolean
  onlyNew: boolean
  minQualityScore: number
  onlyDiscounted?: boolean
}

function buildCommandActions(params: BuildActionsParams): CommandActionItem[] {
  const {
    onTriggerSearch,
    closePalette,
    onlyBargain,
    onlyNew,
    minQualityScore,
    onlyDiscounted,
  } = params
  const ui = useUiStore.getState()
  const flt = useFilterStore.getState()
  const run = (fn: () => void) => () => {
    fn()
    closePalette()
  }

  return [
    {
      id: 'act-search',
      group: 'Munkaterület & Keresés',
      label: 'Keresés indítása / frissítése',
      description: 'Élő keresés futtatása a kiválasztott portálokon',
      icon: Search,
      onExecute: run(onTriggerSearch),
    },
    {
      id: 'ws-listings',
      group: 'Munkaterület & Keresés',
      label: 'Ugrás: Találatok munkaterület',
      description: 'Élő keresési találatok és többforrásos klaszterek',
      icon: LayoutGrid,
      onExecute: run(() => ui.setWorkspaceTab('listings')),
    },
    {
      id: 'ws-favorites',
      group: 'Munkaterület & Keresés',
      label: 'Ugrás: Kedvencek',
      description: 'Csillagozott hirdetések megtekintése',
      icon: Star,
      onExecute: run(() => ui.setWorkspaceTab('favorites')),
    },
    {
      id: 'ws-archive',
      group: 'Munkaterület & Keresés',
      label: 'Ugrás: Archívum, Globális Kuka & Leárazás Radar',
      description: 'Mentett adatbázis-előzmények, elrejtett és árcsökkentett ingatlanok',
      icon: Archive,
      onExecute: run(() => ui.setWorkspaceTab('archive')),
    },
    {
      id: 'ws-analytics',
      group: 'Munkaterület & Keresés',
      label: 'Ugrás: Piaci Elemzés & Szegmens Mátrix',
      description: 'Medián négyzetméterárak, hisztogram, szórásdiagram és városi mátrix',
      icon: BarChart2,
      onExecute: run(() => ui.setWorkspaceTab('analytics')),
    },
    {
      id: 'vm-grid',
      group: 'Nézetsűrűség',
      label: 'Nézet: Lebegő Kártyarács (Grid)',
      description: 'Fotó-központú 3 oszlopos kártyanézet',
      icon: LayoutGrid,
      onExecute: run(() => {
        ui.setWorkspaceTab('listings')
        ui.setViewMode('grid')
      }),
    },
    {
      id: 'vm-list',
      group: 'Nézetsűrűség',
      label: 'Nézet: Kompakt Lista (List)',
      description: 'Vízszintes kártyasorok gyors áttekintéshez',
      icon: List,
      onExecute: run(() => {
        ui.setWorkspaceTab('listings')
        ui.setViewMode('list')
      }),
    },
    {
      id: 'vm-table',
      group: 'Nézetsűrűség',
      label: 'Nézet: Asztali Elemző Táblázat (Analyst Table)',
      description: 'Nagy sűrűségű, oszloprendezhető pénzügyi táblázat (tabular-nums)',
      icon: Table2,
      onExecute: run(() => {
        ui.setWorkspaceTab('listings')
        ui.setViewMode('table')
      }),
    },
    {
      id: 'flt-bargain',
      group: 'Gyorsszűrők',
      label: onlyBargain ? 'Alkalmi vétel szűrő kikapcsolása' : 'Csak Alkalmi vételek (>=20% medián alatt)',
      description: 'Helyi piaci mediánnál legalább 20%-kal olcsóbb ingatlanok',
      icon: TrendingDown,
      onExecute: run(() => flt.setOnlyBargain(!onlyBargain)),
    },
    {
      id: 'flt-new',
      group: 'Gyorsszűrők',
      label: onlyNew ? 'Csak új találatok szűrő kikapcsolása' : 'Csak új találatok megjelenítése',
      description: 'Utolsó kereséskor felfedezett új hirdetések',
      icon: Sparkles,
      onExecute: run(() => flt.setOnlyNew(!onlyNew)),
    },
    {
      id: 'flt-quality',
      group: 'Gyorsszűrők',
      label: minQualityScore >= 70 ? 'Minimum adatminőség szűrő törlése' : 'Gyanús hirdetések kizárása (Minőség >= 70)',
      description: '70 pont alatti outlier hirdetések kiszűrése a listából',
      icon: ShieldCheck,
      onExecute: run(() => flt.setMinQualityScore(minQualityScore >= 70 ? 0 : 70)),
    },
    {
      id: 'flt-discounted',
      group: 'Gyorsszűrők',
      label: onlyDiscounted ? 'Csak leárazott szűrő kikapcsolása' : 'Csak árcsökkentett (leárazott) hirdetések',
      description: 'Dokumentált áreséssel rendelkező ingatlanok szűrése',
      icon: TrendingDown,
      onExecute: run(() => flt.setOnlyDiscounted(!onlyDiscounted)),
    },
    {
      id: 'mod-diag',
      group: 'Eszközök & Rendszer',
      label: 'Portál Diagnosztika & Állapotjelentés megnyitása',
      description: 'Válaszidők, HTTP státuszok és WAF fallback linkek',
      icon: Activity,
      onExecute: run(() => ui.setDiagnosticsModalOpen(true)),
    },
    {
      id: 'mod-import',
      group: 'Eszközök & Rendszer',
      label: 'Ingatlan.com HTML oldalak importálása',
      description: 'Böngészőből mentett további találati oldalak kézi betöltése',
      icon: Upload,
      onExecute: run(() => ui.setIngatlanComImportModalOpen(true)),
    },
    {
      id: 'sys-reset',
      group: 'Eszközök & Rendszer',
      label: 'Összes szűrő alaphelyzetbe állítása',
      description: 'Alapértelmezett keresési paraméterek visszaállítása',
      icon: RotateCcw,
      onExecute: run(() => flt.resetToDefaults()),
    },
  ]
}

export const CommandPaletteModal: FC<CommandPaletteModalProps> = ({ onTriggerSearch }) => {
  const { isCommandPaletteOpen, setCommandPaletteOpen } = useUiStore()
  const { onlyBargain, onlyNew, minQualityScore, criteria } = useFilterStore()
  const [query, setQuery] = useState('')
  const [activeIndex, setActiveIndex] = useState(0)
  const panelRef = useRef<HTMLDivElement>(null)

  const closePalette = useCallback(() => {
    setCommandPaletteOpen(false)
    setQuery('')
    setActiveIndex(0)
  }, [setCommandPaletteOpen])

  useOutsideDismiss(panelRef, isCommandPaletteOpen, closePalette, true)

  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setCommandPaletteOpen(!useUiStore.getState().isCommandPaletteOpen)
      }
    }
    window.addEventListener('keydown', handleGlobalKeyDown)
    return () => window.removeEventListener('keydown', handleGlobalKeyDown)
  }, [setCommandPaletteOpen])

  const actions = useMemo(
    () =>
      buildCommandActions({
        onTriggerSearch,
        closePalette,
        onlyBargain,
        onlyNew,
        minQualityScore,
        onlyDiscounted: criteria.onlyDiscounted,
      }),
    [onTriggerSearch, closePalette, onlyBargain, onlyNew, minQualityScore, criteria.onlyDiscounted],
  )

  const filteredActions = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return actions
    return actions.filter(
      (a) =>
        a.label.toLowerCase().includes(q) ||
        a.description.toLowerCase().includes(q) ||
        a.group.toLowerCase().includes(q),
    )
  }, [actions, query])

  if (!isCommandPaletteOpen) return null

  const safeIndex = filteredActions.length > 0 ? Math.min(activeIndex, filteredActions.length - 1) : 0

  return (
    <div
      data-testid="command-palette-backdrop"
      className="fixed inset-0 z-50 flex items-start justify-center bg-black/60 p-4 pt-16 sm:pt-24 backdrop-blur-sm animate-in fade-in duration-150"
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Gyorsugró és parancspaletta"
        data-testid="command-palette-modal"
        className="w-full max-w-xl overflow-hidden rounded-2xl border border-[var(--border-strong)] bg-[var(--bg-elevated)] shadow-2xl"
      >
        <div className="flex items-center gap-2.5 border-b border-[var(--border-hairline)] px-4 py-3">
          <Search className="h-4 w-4 text-blue-400 shrink-0" />
          <input
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setActiveIndex(0)
            }}
            onKeyDown={(e) => {
              const len = filteredActions.length
              if (e.key === 'ArrowDown' && len > 0) {
                e.preventDefault()
                setActiveIndex((prev) => (prev + 1) % len)
              } else if (e.key === 'ArrowUp' && len > 0) {
                e.preventDefault()
                setActiveIndex((prev) => (prev - 1 + len) % len)
              } else if (e.key === 'Enter' && filteredActions[safeIndex]) {
                e.preventDefault()
                filteredActions[safeIndex]!.onExecute()
              }
            }}
            placeholder="Parancs vagy nézet keresése (pl. Táblázat, Archívum, Alkalmi, Minőség)..."
            aria-label="Parancspaletta keresés"
            data-testid="command-palette-input"
            autoFocus
            className="flex-1 bg-transparent text-xs sm:text-sm text-[var(--text-primary)] placeholder:text-[var(--text-secondary)] focus:outline-none"
          />
          <button
            type="button"
            onClick={closePalette}
            aria-label="Parancspaletta bezárása"
            className="rounded-lg p-1 text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)] cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="max-h-80 overflow-y-auto p-2 space-y-1">
          {filteredActions.length === 0 ? (
            <div className="py-8 text-center text-xs text-[var(--text-secondary)]">
              Nincs találat a megadott kifejezésre.
            </div>
          ) : (
            filteredActions.map((action, idx) => {
              const Icon = action.icon
              const isSelected = idx === safeIndex
              return (
                <button
                  key={action.id}
                  type="button"
                  data-testid={`command-item-${action.id}`}
                  onClick={action.onExecute}
                  onMouseEnter={() => setActiveIndex(idx)}
                  className={cn(
                    'flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2 text-left transition-colors cursor-pointer',
                    isSelected
                      ? 'bg-[var(--accent-soft)] text-[var(--text-primary)]'
                      : 'text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)]',
                  )}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] text-blue-400">
                      <Icon className="h-3.5 w-3.5" />
                    </div>
                    <div className="min-w-0">
                      <div className="text-xs font-bold text-[var(--text-primary)] truncate">
                        {action.label}
                      </div>
                      <div className="text-[11px] text-[var(--text-secondary)] truncate">
                        {action.description}
                      </div>
                    </div>
                  </div>
                  <span className="shrink-0 rounded-md border border-[var(--border-hairline)] bg-[var(--bg-input)] px-1.5 py-0.5 text-[10px] font-medium text-[var(--text-secondary)]">
                    {action.group}
                  </span>
                </button>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
