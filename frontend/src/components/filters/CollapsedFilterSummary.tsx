import type { FC } from 'react'
import {
  MapPin,
  Home,
  Coins,
  Ruler,
  Globe,
  SlidersHorizontal,
  ChevronDown,
} from 'lucide-react'
import { DEFAULT_PORTALS } from '../../store/useFilterStore'

export interface CollapsedFilterSummaryProps {
  citiesSummary: string
  typeLabel: string
  minPriceMillion: number
  maxPriceMillion: number
  minArea: number
  maxArea: number
  portalsCount: number
  onExpand: () => void
  expandLabel?: string
  expandAriaLabel?: string
}

export const CollapsedFilterSummary: FC<CollapsedFilterSummaryProps> = ({
  citiesSummary,
  typeLabel,
  minPriceMillion,
  maxPriceMillion,
  minArea,
  maxArea,
  portalsCount,
  onExpand,
  expandLabel = 'Kibontás',
  expandAriaLabel,
}) => (
  <div
    data-testid="collapsed-filter-summary"
    className="flex items-center justify-between gap-2.5 rounded-2xl glass-surface px-3.5 py-2 text-xs text-[var(--text-secondary)] tabular-nums"
  >
    <div className="flex flex-1 min-w-0 items-center gap-2">
      <button
        type="button"
        onClick={onExpand}
        className="inline-flex min-w-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-semibold text-[var(--text-primary)] hover:border-[#0071e3]/40 transition-colors cursor-pointer"
      >
        <MapPin className="h-3.5 w-3.5 shrink-0 text-[#0071e3]" />
        <span className="truncate">{citiesSummary}</span>
      </button>
      <button
        type="button"
        onClick={onExpand}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-medium hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
      >
        <Home className="h-3.5 w-3.5 shrink-0 text-amber-700" />
        <span>{typeLabel}</span>
      </button>
      <button
        type="button"
        onClick={onExpand}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-medium hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
      >
        <Coins className="h-3.5 w-3.5 shrink-0 text-emerald-700" />
        <span>
          {minPriceMillion} - {maxPriceMillion} M Ft
        </span>
      </button>
      <button
        type="button"
        onClick={onExpand}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-medium hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
      >
        <Ruler className="h-3.5 w-3.5 shrink-0 text-purple-700" />
        <span>
          {minArea} - {maxArea} m²
        </span>
      </button>
      <button
        type="button"
        onClick={onExpand}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-3 py-1.5 font-medium hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
      >
        <Globe className="h-3.5 w-3.5 shrink-0 text-[#0071e3]" />
        <span>
          {portalsCount}/{DEFAULT_PORTALS.length} portál
        </span>
      </button>
    </div>

    <button
      type="button"
      onClick={onExpand}
      aria-label={expandAriaLabel}
      aria-expanded={false}
      className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-[#0071e3] px-3.5 py-1.5 text-xs font-bold text-white shadow-xs transition-colors hover:bg-[#0077ed] cursor-pointer whitespace-nowrap"
    >
      <SlidersHorizontal className="h-3.5 w-3.5" />
      <span>{expandLabel}</span>
      <ChevronDown className="h-3.5 w-3.5" />
    </button>
  </div>
)
