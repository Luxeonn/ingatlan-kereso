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
              'flex h-8 flex-1 min-w-[115px] items-center justify-center gap-1.5 whitespace-nowrap rounded-full border px-3 text-xs font-bold transition-all duration-150 cursor-pointer',
              isActive
                ? 'border-[#0071e3]/45 bg-blue-500/15 text-[var(--status-info-fg)] shadow-sm'
                : 'border-[var(--border-hairline)] bg-white/75 text-[#0b1220] hover:bg-white/95 hover:border-[#0071e3]/40',
            )}
          >
            <span className="truncate">{chip.label}</span>
            <span
              className={cn(
                'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none tabular-nums shrink-0',
                isActive
                  ? 'bg-[#0071e3] text-white'
                  : 'bg-[#0b1220]/[0.07] text-[#0b1220]',
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
