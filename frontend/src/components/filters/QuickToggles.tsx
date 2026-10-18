import type { FC } from 'react'
import { ArrowUpDown, EyeOff, Sparkles, ExternalLink } from 'lucide-react'
import { SortOption } from '../../store/useFilterStore'
import { cn } from '../../utils/cn'

interface QuickTogglesProps {
  onlyNew: boolean
  onToggleOnlyNew: (val: boolean) => void
  excludeViewed: boolean
  onToggleExcludeViewed: (val: boolean) => void
  autoOpen: boolean
  onToggleAutoOpen: (val: boolean) => void
  sortOption: SortOption
  onChangeSortOption: (val: SortOption) => void
}

export const QuickToggles: FC<QuickTogglesProps> = ({
  onlyNew,
  onToggleOnlyNew,
  excludeViewed,
  onToggleExcludeViewed,
  autoOpen,
  onToggleAutoOpen,
  sortOption,
  onChangeSortOption,
}) => {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--border-color)] pt-3 text-xs">
      {/* Toggles */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Only new */}
        <button
          type="button"
          onClick={() => onToggleOnlyNew(!onlyNew)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
            onlyNew
              ? 'border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
          )}
        >
          <Sparkles className="h-3.5 w-3.5 text-emerald-400" />
          <span>Csak új találatok</span>
        </button>

        {/* Exclude viewed */}
        <button
          type="button"
          onClick={() => onToggleExcludeViewed(!excludeViewed)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
            excludeViewed
              ? 'border border-blue-500/40 bg-blue-500/15 text-blue-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
          )}
        >
          <EyeOff className="h-3.5 w-3.5 text-blue-400" />
          <span>Megtekintettek rejtése</span>
        </button>

        {/* Auto open in browser */}
        <button
          type="button"
          onClick={() => onToggleAutoOpen(!autoOpen)}
          title="Keresés után az új találatok automatikusan megnyílnak a böngészőben"
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
            autoOpen
              ? 'border border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
          )}
        >
          <ExternalLink className="h-3.5 w-3.5 text-amber-400" />
          <span>Lapok azonnali nyitása</span>
        </button>
      </div>

      {/* Sort selector */}
      <div className="flex items-center gap-2">
        <ArrowUpDown className="h-3.5 w-3.5 text-[var(--text-secondary)] shrink-0" />
        <span className="text-[var(--text-secondary)] font-medium">Rendezés:</span>
        <select
          value={sortOption}
          onChange={(e) => onChangeSortOption(e.target.value as SortOption)}
          className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 text-xs font-semibold text-[var(--text-primary)] focus:border-blue-500 focus:outline-none"
        >
          <option value="price_asc">Ár: Növekvő (alapértelmezett)</option>
          <option value="price_desc">Ár: Csökkenő</option>
          <option value="sqm_price_asc">Ft/m²: Növekvő</option>
          <option value="sqm_price_desc">Ft/m²: Csökkenő</option>
          <option value="area_desc">Méret: Legnagyobb elöl</option>
          <option value="date_desc">Felfedezés: Legfrissebb</option>
        </select>
      </div>
    </div>
  )
}
