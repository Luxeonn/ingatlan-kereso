import type { FC, ReactNode } from 'react'
import { Ruler, BedDouble } from 'lucide-react'
import { formatArea, formatPricePerSqm, formatRooms } from '../../utils/formatters'
import { useFilterStore } from '../../store/useFilterStore'
import { cn } from '../../utils/cn'
import { isSelectionModifier } from './propertyCardHelpers'

export interface PropertyParametersChipsProps {
  floorArea?: number | null
  rooms?: number | null
  sqmPrice?: number | null
}

interface HotChipDef {
  id: string
  label: string
  title: string
  icon: ReactNode
}

export const PropertyParametersChips: FC<PropertyParametersChipsProps> = ({
  floorArea,
  rooms,
  sqmPrice,
}) => {
  const { quickSearchQuery, setQuickSearchQuery } = useFilterStore()
  const activeQuery = quickSearchQuery.trim()

  const chips: HotChipDef[] = []
  if (floorArea && floorArea > 0) {
    chips.push({
      id: 'hot-filter-area',
      label: formatArea(floorArea),
      title: 'Szűrés erre az alapterületre a gyorskeresőben (újrakattintással törölhető)',
      icon: <Ruler className="h-3.5 w-3.5 text-purple-700" />,
    })
  }
  if (rooms && rooms > 0) {
    chips.push({
      id: 'hot-filter-rooms',
      label: formatRooms(rooms),
      title: 'Szűrés erre a szobaszámra a gyorskeresőben (újrakattintással törölhető)',
      icon: <BedDouble className="h-3.5 w-3.5 text-teal-700" />,
    })
  }
  if (sqmPrice && sqmPrice > 0) {
    chips.push({
      id: 'hot-filter-sqm',
      label: formatPricePerSqm(sqmPrice),
      title: 'Szűrés erre a fajlagos árra a gyorskeresőben (újrakattintással törölhető)',
      icon: null,
    })
  }

  return (
    <div className="flex flex-wrap items-center gap-2 pt-1 text-xs tabular-nums">
      {chips.map((chip) => {
        const isActive = activeQuery === chip.label.trim()
        return (
          <button
            key={chip.id}
            type="button"
            data-testid={chip.id}
            aria-pressed={isActive}
            title={chip.title}
            onClick={(e) => {
              if (isSelectionModifier(e)) return
              e.stopPropagation()
              setQuickSearchQuery(isActive ? '' : chip.label.trim())
            }}
            className={cn(
              'flex items-center gap-1 rounded-full border px-2.5 py-1 font-medium transition-colors cursor-pointer',
              isActive
                ? 'border-[#0071e3]/50 bg-[#0071e3]/12 text-[var(--status-info-fg)] ring-1 ring-[#0071e3]/20'
                : 'border-[var(--border-hairline)] bg-white/75 text-[#0b1220] hover:border-[#0071e3]/45 hover:bg-[#0071e3]/[0.06] hover:text-[var(--status-info-fg)]',
            )}
          >
            {chip.icon}
            <span>{chip.label}</span>
          </button>
        )
      })}
    </div>
  )
}
