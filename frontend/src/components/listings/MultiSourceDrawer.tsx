import type { FC } from 'react'
import { ExternalLink, Clock, Layers } from 'lucide-react'
import type { IngatlanItem, SourceListing } from '../../types/property'
import {
  formatPriceMillion,
  formatPriceFt,
  formatDate,
  getPortalStyle,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'

interface MultiSourceDrawerProps {
  item: IngatlanItem
  onMarkViewed?: (url: string) => void
}

export const MultiSourceDrawer: FC<MultiSourceDrawerProps> = ({ item, onMarkViewed }) => {
  const sources: SourceListing[] =
    item.sourceListings && item.sourceListings.length > 0
      ? item.sourceListings
      : [
          {
            portal: item.portal,
            url: item.url,
            title: item.title,
            imageUrl: item.imageUrl,
            priceHuf: item.priceInfo?.amountHuf,
            status: item.status || 'ACTIVE',
            firstSeenAt: item.discoveredAt,
            lastSeenAt: item.discoveredAt,
            viewed: item.viewed,
          },
        ]

  const handleOpenSource = (url?: string) => {
    if (!url) return
    window.open(url, '_blank', 'noopener,noreferrer')
    if (onMarkViewed) {
      onMarkViewed(url)
    }
  }

  return (
    <div
      id={`drawer-${item.id}`}
      role="region"
      aria-label="Hirdetési források"
      data-testid="multi-source-drawer"
      className="mt-3.5 flex flex-col gap-2 rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)]/50 p-3 shadow-inner transition-all animate-in fade-in-50 duration-200"
    >
      <div className="flex items-center justify-between border-b border-[var(--border-color)]/50 pb-2 text-xs font-semibold text-[var(--text-secondary)]">
        <div className="flex items-center gap-1.5 text-[var(--text-primary)]">
          <Layers className="h-3.5 w-3.5 text-blue-400" aria-hidden="true" />
          <span>Elérhető források ({sources.length})</span>
        </div>
        <span className="text-[11px] text-[var(--text-secondary)]">
          Legalacsonyabb ár kiemelve
        </span>
      </div>

      <div className="flex flex-col divide-y divide-[var(--border-color)]/40">
        {sources.map((source, idx) => {
          const portalName = source.portal || 'Forrás'
          const portalStyle = getPortalStyle(portalName)
          const million =
            source.priceHuf != null ? source.priceHuf / 1_000_000 : null
          const isActive =
            !source.status || source.status.toUpperCase() === 'ACTIVE'
          const freshness = source.lastSeenAt || source.firstSeenAt

          return (
            <div
              key={source.url || `${source.portal}-${idx}`}
              data-testid="source-item"
              className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 py-2 first:pt-1 last:pb-1"
            >
              <div className="flex flex-wrap items-center gap-2">
                {/* Portal badge */}
                <span
                  data-testid="source-portal-badge"
                  className={cn(
                    'rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase',
                    portalStyle.bg
                  )}
                >
                  {portalName}
                </span>

                {/* Status badge */}
                <span
                  data-testid="source-status-badge"
                  className={cn(
                    'rounded-md px-1.5 py-0.5 text-[10px] font-bold border uppercase',
                    isActive
                      ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400'
                      : 'border-slate-500/30 bg-slate-500/10 text-slate-400'
                  )}
                >
                  {isActive ? 'Aktív' : 'Inaktív'}
                </span>

                {/* Freshness timestamp */}
                {freshness && (
                  <span
                    data-testid="source-freshness"
                    className="flex items-center gap-1 text-[11px] text-[var(--text-secondary)]"
                    title={`Utoljára észlelve: ${freshness}`}
                  >
                    <Clock className="h-3 w-3 opacity-60" aria-hidden="true" />
                    <span>{formatDate(freshness)}</span>
                  </span>
                )}
              </div>

              <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0">
                {/* Price display */}
                <div className="text-right">
                  <div className="text-xs font-bold text-blue-400">
                    {million != null
                      ? formatPriceMillion(million)
                      : 'Ár a hirdetésben'}
                  </div>
                  {source.priceHuf != null && (
                    <div className="text-[10px] text-[var(--text-secondary)]">
                      {formatPriceFt(source.priceHuf)}
                    </div>
                  )}
                </div>

                {/* Direct external link button */}
                {source.url && (
                  <button
                    type="button"
                    onClick={() => handleOpenSource(source.url)}
                    aria-label={`Megnyitás: ${portalName}`}
                    title={`Megnyitás: ${portalName}`}
                    data-testid="source-external-link"
                    className="flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-2.5 py-1 text-xs font-semibold text-[var(--text-primary)] hover:border-blue-500/50 hover:bg-blue-600/10 hover:text-blue-400 active:scale-95 transition-all"
                  >
                    <span className="hidden xs:inline">Megnyitás</span>
                    <ExternalLink className="h-3.5 w-3.5 text-blue-400" aria-hidden="true" />
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
