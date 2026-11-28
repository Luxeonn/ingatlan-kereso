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
        <label className="text-xs font-bold text-[var(--text-primary)]">
          Keresési Portálok ({selectedPortals.length}/{allPortalIds.length})
        </label>
        <button
          type="button"
          onClick={handleToggleAll}
          className="flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-white/80 px-2 py-1 text-[11px] font-bold text-[#0071e3] hover:border-[#0071e3]/40 hover:bg-[#0071e3]/5 transition-colors cursor-pointer"
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

      <div className="grid grid-cols-2 gap-1.5">
        {isLoading || !Array.isArray(portals)
          ? DEFAULT_PORTALS.map((p) => (
              <div
                key={p}
                className="h-8 w-full animate-shimmer rounded-lg border border-[var(--border-color)]"
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
                    'flex items-center justify-start gap-1.5 rounded-lg px-2 py-1.5 text-[11px] font-bold transition-all duration-150 cursor-pointer',
                    isSelected
                      ? 'border border-[#0071e3]/40 bg-[#0071e3]/12 text-[#005bb5] shadow-xs'
                      : 'border border-[var(--border-color)] bg-white/75 text-[var(--text-secondary)] opacity-80 hover:opacity-100'
                  )}
                >
                  <span
                    className={cn(
                      'flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded text-[10px]',
                      isSelected ? 'bg-[#0071e3] text-white' : 'border border-[var(--border-color)]'
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
