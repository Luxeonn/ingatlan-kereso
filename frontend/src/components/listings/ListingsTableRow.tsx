import { type FC } from 'react'
import {
  Star,
  Trash2,
  RotateCcw,
  ExternalLink,
  Info,
  Check,
} from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import {
  getEffectivePrice,
  getEffectiveSqmPrice,
  getCrossPortalSpread,
  getPropertyTypeConfig,
  formatPriceMillion,
  formatPricePerSqm,
  formatArea,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'

function getSourcesCount(item: IngatlanItem): number {
  return Math.max(item.sourcesCount ?? 1, item.sourceListings?.length ?? 1)
}

function getMedianPillConfig(item: IngatlanItem): { label: string; className: string } | null {
  const pct = item.percentageBelowMedian
  if (pct === null || pct === undefined || (item.referenceSampleSize ?? 0) < 5) return null
  const label = pct >= 0 ? `-${Math.round(pct)}%` : `+${Math.abs(Math.round(pct))}%`
  if (pct >= 20) {
    return {
      label,
      className: 'border-emerald-500/30 bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]',
    }
  }
  if (pct >= 0) {
    return {
      label,
      className: 'border-emerald-500/20 bg-emerald-500/8 text-[var(--status-bargain-fg)]',
    }
  }
  return {
    label,
    className: 'border-[var(--border-hairline)] bg-white/70 text-[#42526b]',
  }
}

function formatRowPriceLabel(item: IngatlanItem): string {
  const { priceMillion } = getEffectivePrice(item)
  if (priceMillion !== null && priceMillion !== undefined) {
    return formatPriceMillion(priceMillion)
  }
  return item.price || '—'
}

const TableTitleLocationCell: FC<{ item: IngatlanItem; onOpenExternal: () => void }> = ({
  item,
  onOpenExternal,
}) => {
  const showUnreadDot = Boolean(item.isNew && !item.viewed)
  const hasDistinctLocation = Boolean(item.location && item.location !== item.city)
  return (
    <td className="px-3 py-2.5 max-w-xs">
      <div className="flex items-center gap-1.5">
        {showUnreadDot && (
          <span
            className="h-2 w-2 shrink-0 rounded-full bg-[var(--status-bargain-fg)]"
            title="Új találat"
          />
        )}
        <button
          type="button"
          onClick={onOpenExternal}
          className="truncate font-bold text-[#0b1220] hover:text-[#0071e3] text-left cursor-pointer"
          title={item.title}
        >
          {item.title || 'Ingatlan hirdetés'}
        </button>
      </div>
      <div className="flex items-center gap-1.5 text-[11px] text-[#42526b] truncate">
        <span>{item.city}</span>
        {hasDistinctLocation && <span className="truncate">· {item.location}</span>}
        {item.portal && (
          <span className="rounded-full border border-[var(--border-hairline)] bg-white/75 px-1.5 py-0.2 text-[10px] text-[#0b1220]">
            {item.portal}
          </span>
        )}
      </div>
    </td>
  )
}

interface TableRowActionsCellProps {
  item: IngatlanItem
  isStarred: boolean
  onToggleStar: () => void
  onOpenExternal: () => void
  onHide?: () => void
  onRestore?: () => void
}

const TableRowActionsCell: FC<TableRowActionsCellProps> = ({
  item,
  isStarred,
  onToggleStar,
  onOpenExternal,
  onHide,
  onRestore,
}) => (
  <td className="px-3 py-2.5 text-right whitespace-nowrap">
    <div className="inline-flex items-center gap-1">
      <button
        type="button"
        onClick={onToggleStar}
        aria-label={isStarred ? 'Eltávolítás a kedvencekből' : 'Hozzáadás a kedvencekhez'}
        className={cn(
          'rounded-full border p-1.5 transition-colors cursor-pointer',
          isStarred
            ? 'border-amber-500/40 bg-[var(--status-warn-bg)] text-amber-700'
            : 'border-[var(--border-hairline)] bg-white/75 text-[#42526b] hover:text-amber-700',
        )}
      >
        <Star className={cn('h-3.5 w-3.5', isStarred && 'fill-amber-500 text-amber-700')} />
      </button>
      {item.hidden && onRestore && (
        <button
          type="button"
          onClick={onRestore}
          aria-label="Hirdetés visszaállítása"
          className="rounded-full border border-purple-500/40 bg-purple-500/12 p-1.5 text-purple-700 hover:bg-purple-500/20 cursor-pointer"
        >
          <RotateCcw className="h-3.5 w-3.5" />
        </button>
      )}
      {!item.hidden && onHide && (
        <button
          type="button"
          onClick={onHide}
          aria-label="Hirdetés elrejtése"
          className="rounded-full border border-[var(--border-hairline)] bg-white/75 p-1.5 text-[#42526b] hover:text-rose-700 cursor-pointer"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      )}
      <button
        type="button"
        onClick={onOpenExternal}
        aria-label="Megnyitás új lapon"
        className="rounded-full border border-[var(--border-hairline)] bg-white/75 p-1.5 text-[#42526b] hover:text-[#0071e3] cursor-pointer"
      >
        <ExternalLink className="h-3.5 w-3.5" />
      </button>
    </div>
  </td>
)

const TableNumericMetricsCells: FC<{ item: IngatlanItem }> = ({ item }) => {
  const sqmPrice = getEffectiveSqmPrice(item)
  const spread = getCrossPortalSpread(item)
  const medianConfig = getMedianPillConfig(item)
  const area = item.detailsInfo?.areaSquareMeters

  return (
    <>
      <td className="px-3 py-2.5 text-right whitespace-nowrap font-extrabold text-[#0071e3] tabular-nums">
        <div>{formatRowPriceLabel(item)}</div>
        {spread && (
          <div className="text-[10px] font-semibold text-[var(--status-info-fg)]">
            -{spread.spreadMillion} M árrés
          </div>
        )}
      </td>
      <td className="px-3 py-2.5 text-right whitespace-nowrap text-[#42526b] tabular-nums">
        {sqmPrice ? formatPricePerSqm(sqmPrice) : '—'}
      </td>
      <td className="px-3 py-2.5 text-right whitespace-nowrap font-semibold text-[#0b1220] tabular-nums">
        {area ? formatArea(area) : '—'}
      </td>
      <td className="px-3 py-2.5 text-center whitespace-nowrap text-[#42526b] tabular-nums">
        {item.detailsInfo?.roomCount || '—'}
      </td>
      <td className="px-3 py-2.5 text-right whitespace-nowrap tabular-nums">
        {medianConfig ? (
          <span className={cn('rounded-full border px-2 py-0.5 text-[10px] font-bold', medianConfig.className)}>
            {medianConfig.label}
          </span>
        ) : (
          <span className="text-[#42526b]">—</span>
        )}
      </td>
    </>
  )
}

export interface ListingsTableRowProps {
  item: IngatlanItem
  isSelected: boolean
  isStarred: boolean
  onToggleSelect: () => void
  onToggleStar: () => void
  onInspect: () => void
  onOpenExternal: () => void
  onHide?: () => void
  onRestore?: () => void
}

export const ListingsTableRow: FC<ListingsTableRowProps> = ({
  item,
  isSelected,
  isStarred,
  onToggleSelect,
  onToggleStar,
  onInspect,
  onOpenExternal,
  onHide,
  onRestore,
}) => {
  const sourcesCount = getSourcesCount(item)
  const typeConfig = getPropertyTypeConfig(item.propertyType)
  const isDimmed = Boolean(item.viewed && !isStarred)

  return (
    <tr
      data-testid="listings-table-row"
      data-selected={isSelected ? 'true' : undefined}
      onClick={(e) => {
        if (e.ctrlKey || e.metaKey) {
          e.preventDefault()
          onToggleSelect()
        }
      }}
      className={cn(
        'transition-colors hover:bg-white/60',
        isSelected && 'bg-[#0071e3]/[0.08]',
        isDimmed && 'opacity-75',
      )}
    >
      <td className="px-3 py-2.5 text-center">
        <button
          type="button"
          data-testid="table-row-select-checkbox"
          aria-label="Kijelölés összevonáshoz"
          aria-pressed={isSelected}
          onClick={(e) => {
            e.stopPropagation()
            onToggleSelect()
          }}
          className={cn(
            'mx-auto flex h-4.5 w-4.5 items-center justify-center rounded border transition-colors cursor-pointer',
            isSelected
              ? 'border-[#0071e3] bg-[#0071e3] text-white'
              : 'border-[var(--border-strong)] bg-white/80 text-transparent hover:border-[#0071e3]',
          )}
        >
          <Check className="h-3 w-3 stroke-[3]" />
        </button>
      </td>
      <TableTitleLocationCell item={item} onOpenExternal={onOpenExternal} />
      <td className="px-3 py-2.5 whitespace-nowrap">
        <span
          className={cn(
            'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-bold',
            typeConfig.colorClasses.badge,
          )}
        >
          {typeConfig.label}
        </span>
      </td>
      <TableNumericMetricsCells item={item} />
      <td className="px-3 py-2.5 text-center whitespace-nowrap">
        <button
          type="button"
          data-testid="table-row-inspect-button"
          onClick={onInspect}
          className="inline-flex items-center gap-1 rounded-full border border-[var(--border-hairline)] bg-white/75 px-2.5 py-1 text-[11px] font-semibold text-[#42526b] hover:border-[#0071e3]/40 hover:text-[#0071e3] cursor-pointer tabular-nums"
        >
          <Info className="h-3 w-3" />
          <span>{sourcesCount > 1 ? `${sourcesCount} forrás` : 'Részletek'}</span>
        </button>
      </td>
      <TableRowActionsCell
        item={item}
        isStarred={isStarred}
        onToggleStar={onToggleStar}
        onOpenExternal={onOpenExternal}
        onHide={onHide}
        onRestore={onRestore}
      />
    </tr>
  )
}
