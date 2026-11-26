import type { FC } from 'react'
import { PORTAL_CHIPS, normalizePortalKey } from '../../utils/formatters'
import { cn } from '../../utils/cn'

export interface PortalFilterChipsRowProps {
  selectedPortalFilter: string
  setSelectedPortalFilter: (id: string) => void
  portalCounts: Record<string, number>
  searchedPortals?: string[]
}

export const PortalFilterChipsRow: FC<PortalFilterChipsRowProps> = ({
  selectedPortalFilter,
  setSelectedPortalFilter,
  portalCounts,
  searchedPortals,
}) => {
  const activePortalSet =
    searchedPortals && searchedPortals.length > 0
      ? new Set(searchedPortals.map(normalizePortalKey).filter(Boolean))
      : null

  const visibleChips = PORTAL_CHIPS.filter((chip) => {
    if (!activePortalSet) return true
    if (chip.id === 'all') return activePortalSet.size > 1
    return activePortalSet.has(chip.id)
  })

  const effectiveSelectedId =
    activePortalSet && activePortalSet.size === 1
      ? Array.from(activePortalSet)[0]
      : selectedPortalFilter

  return (
    <div
      data-testid="portal-quick-filter-chips"
      className="flex flex-wrap items-center gap-2 border-t border-[var(--border-hairline)] pt-2.5"
    >
      {visibleChips.map((chip) => {
        const count = portalCounts[chip.id] || 0
        const isActive = effectiveSelectedId === chip.id
        return (
          <button
            key={chip.id}
            type="button"
            onClick={() => setSelectedPortalFilter(chip.id)}
            className={cn(
              'flex h-9 flex-1 min-w-[115px] items-center justify-center gap-1.5 whitespace-nowrap rounded-lg border px-2.5 text-xs font-bold transition-all duration-150 cursor-pointer',
              isActive
                ? 'border-blue-500/40 bg-blue-500/15 text-blue-700 shadow-sm'
                : 'border-[var(--border-color)] bg-[var(--bg-card)] text-[var(--text-primary)] opacity-80 hover:opacity-100 hover:border-blue-500/40',
            )}
          >
            <span className="truncate">{chip.label}</span>
            <span
              className={cn(
                'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none tabular-nums shrink-0',
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'bg-[var(--bg-input)] text-[var(--text-primary)]',
              )}
            >
              {count}
            </span>
          </button>
        )
      })}
    </div>
  )
}
