import { useState, type FC } from 'react'
import { ArrowUpDown } from 'lucide-react'
import type { IngatlanItem } from '../../types/property'
import { openExternalUrl } from '../../utils/formatters'
import { useUiStore } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'
import { PropertyDetailsModal } from './PropertyDetailsModal'
import { ListingsTableRow } from './ListingsTableRow'

export interface ListingsTableViewProps {
  items: IngatlanItem[]
  onMarkViewed: (url: string) => void
  onHide?: (item: IngatlanItem) => void
  onRestore?: (item: IngatlanItem) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
}

export const ListingsTableView: FC<ListingsTableViewProps> = ({
  items,
  onMarkViewed,
  onHide,
  onRestore,
  onSplitSource,
  onUndoOverride,
}) => {
  const { starredUrls, toggleStarred, selectedCardUrls, toggleCardSelection } = useUiStore()
  const { sortOption, setSortOption } = useFilterStore()
  const [inspectedItem, setInspectedItem] = useState<IngatlanItem | null>(null)

  const handleSortPrice = () => {
    setSortOption(sortOption === 'price_asc' ? 'price_desc' : 'price_asc')
  }

  return (
    <>
      <div
        data-testid="listings-table-view"
        className="overflow-x-auto rounded-[20px] glass-surface"
      >
        <table className="w-full border-collapse text-left text-xs tabular-nums">
          <thead>
            <tr className="border-b border-[var(--border-hairline)] bg-white/60 text-[11px] font-bold uppercase tracking-wider text-[#42526b]">
              <th className="w-10 px-3 py-3 text-center">Kij.</th>
              <th className="px-3 py-3">Ingatlan & Lokáció</th>
              <th className="px-3 py-3">Típus</th>
              <th className="px-3 py-3 text-right">
                <button
                  type="button"
                  onClick={handleSortPrice}
                  className="inline-flex items-center gap-1 hover:text-[#0b1220] cursor-pointer"
                >
                  <span>Ár (M Ft)</span>
                  <ArrowUpDown className="h-3 w-3" />
                </button>
              </th>
              <th className="px-3 py-3 text-right">Ft/m²</th>
              <th className="px-3 py-3 text-right">
                <button
                  type="button"
                  onClick={() => setSortOption('area_desc')}
                  className="inline-flex items-center gap-1 hover:text-[#0b1220] cursor-pointer"
                >
                  <span>Méret</span>
                  <ArrowUpDown className="h-3 w-3" />
                </button>
              </th>
              <th className="px-3 py-3 text-center">Szoba</th>
              <th className="px-3 py-3 text-right">
                <button
                  type="button"
                  onClick={() => setSortOption('bargain_desc')}
                  className="inline-flex items-center gap-1 hover:text-[#0b1220] cursor-pointer"
                >
                  <span>Medián</span>
                  <ArrowUpDown className="h-3 w-3" />
                </button>
              </th>
              <th className="px-3 py-3 text-center">Források</th>
              <th className="px-3 py-3 text-right">Műveletek</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border-hairline)]">
            {items.map((item) => (
              <ListingsTableRow
                key={item.url || item.id}
                item={item}
                isSelected={Boolean(item.url && selectedCardUrls.includes(item.url))}
                isStarred={Boolean(item.url && starredUrls.includes(item.url))}
                onToggleSelect={() => {
                  if (item.url) toggleCardSelection(item.url)
                }}
                onToggleStar={() => toggleStarred(item)}
                onInspect={() => setInspectedItem(item)}
                onOpenExternal={() => {
                  openExternalUrl(item.url)
                  if (!item.viewed) onMarkViewed(item.url)
                }}
                onHide={onHide ? () => onHide(item) : undefined}
                onRestore={onRestore ? () => onRestore(item) : undefined}
              />
            ))}
          </tbody>
        </table>
      </div>

      {inspectedItem && (
        <PropertyDetailsModal
          item={inspectedItem}
          onClose={() => setInspectedItem(null)}
          onMarkViewed={onMarkViewed}
          onSplitSource={onSplitSource}
          onUndoOverride={onUndoOverride}
        />
      )}
    </>
  )
}
