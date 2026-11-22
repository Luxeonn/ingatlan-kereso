import type { FC } from 'react'
import { Check, CheckSquare, Square } from 'lucide-react'
import { usePortals } from '../../hooks/usePortals'
import { cn } from '../../utils/cn'
import { DEFAULT_PORTALS } from '../../store/useFilterStore'

interface PortalToggleGroupProps {
  selectedPortals: string[]
  onToggle: (portalId: string) => void
  onSelectAll: (portals: string[]) => void
}

export const PortalToggleGroup: FC<PortalToggleGroupProps> = ({
  selectedPortals,
  onToggle,
  onSelectAll,
}) => {
  const { data: portals, isLoading } = usePortals()

  const allPortalIds = Array.isArray(portals) ? portals.map((p) => p.id.toLowerCase()) : DEFAULT_PORTALS
  const isAllSelected = allPortalIds.length > 0 && allPortalIds.every((id) => selectedPortals.includes(id))

  const handleToggleAll = () => {
    if (isAllSelected) {
      onSelectAll([])
    } else {
      onSelectAll(allPortalIds)
    }
  }

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between gap-2">
        <label className="text-sm font-bold text-[var(--text-primary)]">
          Keresési Portálok ({selectedPortals.length}/{allPortalIds.length})
        </label>
        <button
          type="button"
          onClick={handleToggleAll}
          className="flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-card)] px-2.5 py-1 text-xs font-bold text-blue-600 hover:border-blue-500/40 hover:bg-blue-500/5 transition-colors cursor-pointer"
        >
          {isAllSelected ? (
            <>
              <Square className="h-3.5 w-3.5" /> Összes törlése
            </>
          ) : (
            <>
              <CheckSquare className="h-3.5 w-3.5" /> Mind kijelöl
            </>
          )}
        </button>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
        {isLoading || !Array.isArray(portals)
          ? DEFAULT_PORTALS.map((p) => (
              <div
                key={p}
                className="h-9 w-full animate-pulse rounded-lg bg-[var(--bg-input)] border border-[var(--border-color)]"
              />
            ))
          : portals.map((portal) => {
              const pId = portal.id.toLowerCase()
              const isSelected = selectedPortals.includes(pId)
              return (
                <button
                  key={portal.id}
                  type="button"
                  onClick={() => onToggle(pId)}
                  className={cn(
                    'flex items-center justify-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-bold transition-all duration-150 cursor-pointer',
                    isSelected
                      ? 'border border-blue-500/40 bg-blue-500/15 text-blue-700 shadow-sm'
                      : 'border border-[var(--border-color)] bg-[var(--bg-card)] text-[var(--text-secondary)] opacity-75 hover:opacity-100'
                  )}
                >
                  <span
                    className={cn(
                      'flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded text-[10px]',
                      isSelected ? 'bg-blue-600 text-white' : 'border border-[var(--border-color)]'
                    )}
                  >
                    {isSelected && <Check className="h-2.5 w-2.5" />}
                  </span>
                  <span className="truncate">{portal.name}</span>
                </button>
              )
            })}
      </div>
    </div>
  )
}
