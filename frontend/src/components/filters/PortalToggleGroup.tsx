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
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between">
        <label className="text-xs font-semibold text-[var(--text-secondary)]">
          Keresési Portálok ({selectedPortals.length}/{allPortalIds.length})
        </label>
        <button
          type="button"
          onClick={handleToggleAll}
          className="flex items-center gap-1 text-[11px] font-medium text-blue-400 hover:text-blue-300"
        >
          {isAllSelected ? (
            <>
              <Square className="h-3 w-3" /> Összes törlése
            </>
          ) : (
            <>
              <CheckSquare className="h-3 w-3" /> Mind kijelöl
            </>
          )}
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        {isLoading || !Array.isArray(portals)
          ? DEFAULT_PORTALS.map((p) => (
              <div
                key={p}
                className="h-8 w-24 animate-pulse rounded-lg bg-[var(--bg-input)] border border-[var(--border-color)]"
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
                    'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-semibold transition-all duration-150',
                    isSelected
                      ? 'border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm'
                      : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] opacity-60 hover:opacity-100'
                  )}
                >
                  <span
                    className={cn(
                      'flex h-3.5 w-3.5 items-center justify-center rounded text-[10px]',
                      isSelected ? 'bg-blue-600 text-white' : 'border border-[var(--border-color)]'
                    )}
                  >
                    {isSelected && <Check className="h-2.5 w-2.5" />}
                  </span>
                  <span>{portal.name}</span>
                </button>
              )
            })}
      </div>
    </div>
  )
}
