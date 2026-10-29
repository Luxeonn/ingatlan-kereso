import type { FC } from 'react'
import { ExternalLink, Clock, Layers, Tag, AlertTriangle, Check } from 'lucide-react'
import type { IngatlanItem, SourceListing } from '../../types/property'
import {
  formatPriceMillion,
  formatPriceFt,
  formatDate,
  getPortalStyle,
  clampQualityScore,
  getQualityConfig,
  openExternalUrl,
} from '../../utils/formatters'
import { PropertyTypeBadge } from './PropertyTypeBadge'
import { cn } from '../../utils/cn'

export interface PropertyDetailsDrawerProps {
  item: IngatlanItem
  onMarkViewed?: (url: string) => void
}

export const PropertyDetailsDrawer: FC<PropertyDetailsDrawerProps> = ({ item, onMarkViewed }) => {
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
    openExternalUrl(url)
    onMarkViewed?.(url)
  }

  const score = clampQualityScore(item.qualityScore)
  const deductions = item.qualityDeductions ?? []
  const qualityConfig = score != null ? getQualityConfig(score) : null
  const isMultiSource = sources.length > 1

  return (
    <div
      id={`drawer-${item.id}`}
      role="region"
      aria-label="Hirdetés részletei"
      data-testid="multi-source-drawer"
      data-drawer-component="property-details-drawer"
      className="mt-3.5 flex flex-col gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)]/50 p-3.5 shadow-inner transition-all animate-in fade-in-50 duration-200 text-xs"
    >
      {/* 1. SECTION: Besorolás és Kategória (letisztítva: m2 és időbélyegek nélkül) */}
      <div className="flex flex-col gap-1.5 pb-3 border-b border-[var(--border-color)]/50">
        <div className="flex items-center gap-1.5 font-semibold text-[var(--text-secondary)]">
          <Tag className="h-3.5 w-3.5 text-blue-400" aria-hidden="true" />
          <span>Besorolás és Kategória</span>
        </div>
        <div className="flex flex-wrap items-center gap-2 pt-0.5">
          <PropertyTypeBadge propertyType={item.propertyType} />
          {item.details && (
            <span className="text-[11px] text-[var(--text-secondary)] bg-[var(--bg-card)] px-2 py-0.5 rounded border border-[var(--border-color)] truncate max-w-full">
              {item.details}
            </span>
          )}
        </div>
      </div>

      {/* 2. SECTION: Data Quality (if present) */}
      {score != null && qualityConfig && (
        <div
          data-testid="drawer-quality-section"
          className={cn(
            'flex flex-col gap-2 pb-3',
            isMultiSource && 'border-b border-[var(--border-color)]/50'
          )}
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 font-semibold text-[var(--text-secondary)]">
              <qualityConfig.icon className="h-3.5 w-3.5 text-emerald-400" aria-hidden="true" />
              <span>Adatminőségi Értékelés</span>
            </div>
            <span className={cn('px-2 py-0.5 rounded border text-[11px] font-bold flex items-center gap-1', qualityConfig.badgeClass)}>
              <span>{score}/100 pont</span>
              <span className="text-[10px] font-normal opacity-80">({qualityConfig.ratingLabel})</span>
            </span>
          </div>

          {/* Progress bar */}
          <div className="h-1.5 w-full bg-[var(--bg-card)] rounded-full overflow-hidden border border-[var(--border-color)]/50">
            <div
              className={cn('h-full transition-all duration-300 rounded-full', qualityConfig.barClass)}
              style={{ width: `${score}%` }}
            />
          </div>

          {/* Deductions or Clean status */}
          {deductions.length > 0 ? (
            <div className="mt-1 flex flex-col gap-1 rounded-lg bg-[var(--bg-card)] p-2 border border-[var(--border-color)]">
              <div className="flex items-center gap-1 text-[11px] font-semibold text-amber-400">
                <AlertTriangle className="h-3 w-3" />
                <span>Levonási tételek ({deductions.length}):</span>
              </div>
              <ul className="list-disc list-inside space-y-0.5 text-[11px] text-[var(--text-secondary)]">
                {deductions.map((deduction, idx) => (
                  <li key={idx} className="leading-snug">{deduction}</li>
                ))}
              </ul>
            </div>
          ) : (
            <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 font-medium pt-0.5">
              <Check className="h-3.5 w-3.5 shrink-0" />
              <span>Minden ellenőrzött adatmező hiánytalan és megbízható.</span>
            </div>
          )}
        </div>
      )}

      {/* 3. SECTION: Multi-source listing breakdown (Kizárólag többes forrás esetén #301) */}
      {isMultiSource && (
        <div
          data-testid="sources-list-section"
          className="flex flex-col gap-2"
        >
          <div className="flex items-center justify-between text-xs font-semibold text-[var(--text-secondary)]">
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
      )}
    </div>
  )
}
