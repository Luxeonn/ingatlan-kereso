import type { FC } from 'react'
import {
  Sparkles,
  TrendingDown,
  Star,
  Building2,
  Info,
  Trash2,
  RotateCcw,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  getCrossPortalSpread,
  formatPriceMillion,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'
import { isListingNew } from '../../utils/listingFilters'
import { isSelectionModifier } from './propertyCardHelpers'

export interface PropertyCardBadgesProps {
  item: IngatlanItem
  sourcesCount: number
  isDrawerOpen: boolean
  isStarred: boolean
  onToggleDrawer: () => void
  onToggleStarred: () => void
  onHide?: () => void
  onRestore?: () => void
}

const PropertyActionButtons: FC<PropertyCardBadgesProps> = ({
  item,
  sourcesCount,
  isDrawerOpen,
  isStarred,
  onToggleDrawer,
  onToggleStarred,
  onHide,
  onRestore,
}) => (
  <div className="flex shrink-0 items-center gap-1.5 ml-auto">
    {sourcesCount <= 1 && (
      <button
        type="button"
        aria-expanded={isDrawerOpen}
        aria-controls={`drawer-${item.id}`}
        aria-label="Részletek és források megjelenítése"
        title="Részletek és források"
        data-testid="details-button"
        onClick={(e) => {
          if (isSelectionModifier(e)) return
          e.stopPropagation()
          onToggleDrawer()
        }}
        className={cn(
          'flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold transition-all cursor-pointer tabular-nums',
          isDrawerOpen
            ? 'border-[#0071e3]/40 bg-[#0071e3]/12 text-[var(--status-info-fg)] shadow-sm'
            : 'border-[var(--border-hairline)] bg-white/75 text-[#42526b] hover:text-[var(--status-info-fg)] hover:border-[#0071e3]/35',
        )}
      >
        <Info className="h-3.5 w-3.5" aria-hidden="true" />
        <span className="text-[11px]">Részletek</span>
      </button>
    )}

    <button
      type="button"
      onClick={(e) => {
        e.stopPropagation()
        onToggleStarred()
      }}
      aria-label={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
      title={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
      className={cn(
        'flex items-center justify-center rounded-full border p-1.5 text-xs transition-all cursor-pointer',
        isStarred
          ? 'border-amber-500/40 bg-[var(--status-warn-bg)] text-amber-700 shadow-sm'
          : 'border-[var(--border-hairline)] bg-white/75 text-[#42526b] hover:text-amber-700',
      )}
    >
      <Star className={cn('h-3.5 w-3.5', isStarred ? 'fill-amber-500 text-amber-700' : '')} />
    </button>

    {item.hidden && onRestore && (
      <button
        type="button"
        data-testid="restore-property-button"
        onClick={(e) => {
          e.stopPropagation()
          onRestore()
        }}
        aria-label="Hirdetés visszaállítása"
        title="Hirdetés visszaállítása"
        className="flex items-center gap-1 rounded-full border border-purple-500/40 bg-purple-500/12 px-2.5 py-1 text-xs font-semibold text-purple-700 hover:bg-purple-500/20 transition-all cursor-pointer"
      >
        <RotateCcw className="h-3.5 w-3.5" />
        <span className="text-[11px]">Visszaállítás</span>
      </button>
    )}

    {!item.hidden && onHide && (
      <button
        type="button"
        data-testid="hide-property-button"
        onClick={(e) => {
          e.stopPropagation()
          onHide()
        }}
        aria-label="Hirdetés elrejtése"
        title="Hirdetés elrejtése"
        className="flex items-center justify-center rounded-full border border-[var(--border-hairline)] bg-white/75 p-1.5 text-xs text-[#42526b] hover:border-rose-500/40 hover:bg-rose-500/10 hover:text-rose-700 transition-all cursor-pointer"
      >
        <Trash2 className="h-3.5 w-3.5" />
      </button>
    )}
  </div>
)

function resolveNonBargainMarketLabel(item: IngatlanItem): string | null {
  if (item.isBargain) return null
  const pct = item.percentageBelowMedian
  if (pct === null || pct === undefined || (item.referenceSampleSize ?? 0) < 5) return null
  return pct >= 0 ? `-${Math.round(pct)}% medián` : `+${Math.abs(Math.round(pct))}% medián`
}

function hasActiveBargainBadge(item: IngatlanItem): boolean {
  return Boolean(
    item.isBargain &&
      (item.referenceSampleSize ?? 0) >= 5 &&
      item.percentageBelowMedian !== null &&
      item.percentageBelowMedian !== undefined &&
      item.percentageBelowMedian > 0,
  )
}

export const PropertyCardBadges: FC<PropertyCardBadgesProps> = (props) => {
  const { item } = props
  const spreadInfo = getCrossPortalSpread(item)
  const marketLabel = resolveNonBargainMarketLabel(item)
  const hasPriceDrop = item.priceDelta !== null && item.priceDelta !== undefined && item.priceDelta < 0
  const showInlineNewPill = isListingNew(item) && hasActiveBargainBadge(item)

  return (
    <div className="flex items-center justify-between gap-2 min-h-[28px]">
      <div className="flex items-center gap-1.5 flex-wrap">
        {showInlineNewPill && (
          <span className="flex shrink-0 items-center gap-1 rounded-full bg-[var(--status-bargain-bg)] border border-emerald-500/30 px-2 py-0.5 text-[11px] font-bold text-[var(--status-bargain-fg)]">
            <Sparkles className="h-3 w-3" /> ÚJ
          </span>
        )}

        {item.newConstruction && (
          <span
            data-testid="new-construction-badge"
            className="flex shrink-0 items-center gap-1 rounded-full border border-cyan-600/25 bg-cyan-500/12 px-2 py-0.5 text-[10px] font-bold text-cyan-800"
          >
            <Building2 className="h-3 w-3" />
            <span>Új építésű</span>
          </span>
        )}

        {hasPriceDrop && (
          <span className="flex shrink-0 items-center gap-1 rounded-full bg-[var(--status-danger-bg)] border border-rose-500/30 px-2 py-0.5 text-[11px] font-bold text-[var(--status-danger-fg)] tabular-nums">
            <TrendingDown className="h-3 w-3" />
            {Math.abs(Math.round(item.priceChangePercent || 0))}%
          </span>
        )}

        {marketLabel && (
          <span
            data-testid="market-position-pill"
            className="rounded-full border border-[var(--border-hairline)] bg-white/75 px-2 py-0.5 text-[10px] font-semibold text-[#42526b] tabular-nums"
          >
            {marketLabel}
          </span>
        )}

        {spreadInfo && (
          <span
            data-testid="cross-portal-spread-pill"
            title={`Árkülönbség a portálok között: -${spreadInfo.spreadMillion} M Ft (legolcsóbb: ${spreadInfo.cheapestPortal})`}
            className="rounded-full border border-[#0071e3]/25 bg-[var(--status-info-bg)] px-2 py-0.5 text-[10px] font-bold text-[var(--status-info-fg)] tabular-nums"
          >
            -{spreadInfo.spreadMillion} M árrés
          </span>
        )}
      </div>

      <PropertyActionButtons {...props} />
    </div>
  )
}

export interface PropertyPriceSectionProps {
  priceMillion?: number | null
  rawPrice?: string
  viewMode: 'grid' | 'list'
}

export const PropertyPriceSection: FC<PropertyPriceSectionProps> = ({
  priceMillion,
  rawPrice,
  viewMode,
}) => (
  <div
    className={cn(
      'flex items-center justify-between border-t border-[var(--border-hairline)] pt-3 tabular-nums',
      viewMode === 'list'
        ? 'border-t-0 pt-0 flex-col items-end justify-center gap-1.5 shrink-0'
        : 'mt-3.5',
    )}
  >
    <div className={cn(viewMode === 'list' ? 'text-right' : '')}>
      <div className="text-lg font-extrabold tracking-tight text-[#0071e3]">
        {priceMillion !== null && priceMillion !== undefined ? formatPriceMillion(priceMillion) : rawPrice}
      </div>
    </div>
  </div>
)
