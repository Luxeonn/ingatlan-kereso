import { useEffect, useState, type FC } from 'react'
import { Layers, Tag, AlertTriangle, Check } from 'lucide-react'
import type { IngatlanItem, SourceListing } from '../../types/property'
import { getDedupOverrides, type DedupOverrideDto } from '../../api/dedupApi'
import { clampQualityScore, getQualityConfig, getEffectivePrice, openExternalUrl } from '../../utils/formatters'
import { PropertyTypeBadge } from './PropertyTypeBadge'
import { ManualDedupStatusSection, SourceItemRow } from './PropertyDetailsDrawerParts'
import { cn } from '../../utils/cn'

export interface PropertyDetailsDrawerProps {
  item: IngatlanItem
  onMarkViewed?: (url: string) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<void> | void
  onUndoOverride?: (overrideId: number) => Promise<void> | void
  className?: string
}

function buildFallbackSources(item: IngatlanItem): SourceListing[] {
  if (item.sourceListings && item.sourceListings.length > 0) {
    return item.sourceListings
  }
  return [
    {
      portal: item.portal,
      url: item.url,
      title: item.title,
      imageUrl: item.imageUrl,
      priceHuf: getEffectivePrice(item).priceFt,
      status: item.status || 'ACTIVE',
      firstSeenAt: item.discoveredAt,
      lastSeenAt: item.discoveredAt,
      viewed: item.viewed,
    },
  ]
}

export const PropertyDetailsDrawer: FC<PropertyDetailsDrawerProps> = ({
  item,
  onMarkViewed,
  onSplitSource,
  onUndoOverride,
  className,
}) => {
  const [overrides, setOverrides] = useState<DedupOverrideDto[]>([])
  const [isMutating, setIsMutating] = useState(false)
  const hasManualOverride = Boolean(item.manuallyMerged || item.manuallySplit)

  useEffect(() => {
    if (!item.url || !hasManualOverride) return undefined
    let cancelled = false
    getDedupOverrides(item.url)
      .then((res) => {
        if (!cancelled) setOverrides(res)
      })
      .catch(() => {
        if (!cancelled) setOverrides([])
      })
    return () => {
      cancelled = true
    }
  }, [item.url, hasManualOverride])

  const sources = buildFallbackSources(item)
  const handleOpenSource = (url?: string) => {
    if (!url) return
    openExternalUrl(url)
    onMarkViewed?.(url)
  }

  const score = clampQualityScore(item.qualityScore)
  const deductions = item.qualityDeductions ?? []
  const qualityConfig = score !== null ? getQualityConfig(score) : null
  const isMultiSource = sources.length > 1
  const clusterUrls = sources.map((s) => s.url).filter((u): u is string => Boolean(u))
  const displayedOverrides = hasManualOverride ? overrides : []

  const handleSplit = async (sourceUrl: string) => {
    if (!onSplitSource || isMutating) return
    setIsMutating(true)
    try {
      await onSplitSource(sourceUrl, clusterUrls)
    } finally {
      setIsMutating(false)
    }
  }

  const handleUndo = async (overrideId: number) => {
    if (!onUndoOverride || isMutating) return
    setIsMutating(true)
    try {
      await onUndoOverride(overrideId)
      setOverrides((prev) => prev.filter((o) => o.id !== overrideId))
    } finally {
      setIsMutating(false)
    }
  }

  return (
    <div
      id={`drawer-${item.id}`}
      role="region"
      aria-label="Hirdetés részletei"
      data-testid="multi-source-drawer"
      data-drawer-component="property-details-drawer"
      className={cn(
        'mt-3.5 flex flex-col gap-3 rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)]/50 p-3.5 shadow-inner transition-all animate-in fade-in-50 duration-200 text-xs',
        className,
      )}
    >
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

      {score !== null && qualityConfig && (
        <div
          data-testid="drawer-quality-section"
          className={cn(
            'flex flex-col gap-2 pb-3',
            isMultiSource && 'border-b border-[var(--border-color)]/50',
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

          <div className="h-1.5 w-full bg-[var(--bg-card)] rounded-full overflow-hidden border border-[var(--border-color)]/50">
            <div
              className={cn('h-full transition-all duration-300 rounded-full', qualityConfig.barClass)}
              style={{ width: `${score}%` }}
            />
          </div>

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

      {isMultiSource && (
        <div data-testid="sources-list-section" className="flex flex-col gap-2">
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
            {sources.map((source, idx) => (
              <SourceItemRow
                key={source.url || `${source.portal}-${idx}`}
                source={source}
                canSplit={isMultiSource && Boolean(onSplitSource) && Boolean(source.url)}
                isMutating={isMutating}
                onOpenSource={handleOpenSource}
                onSplitSource={handleSplit}
              />
            ))}
          </div>
        </div>
      )}

      <ManualDedupStatusSection
        manuallyMerged={item.manuallyMerged}
        manuallySplit={item.manuallySplit}
        overrides={displayedOverrides}
        isMutating={isMutating}
        onUndoOverride={onUndoOverride ? (id) => void handleUndo(id) : undefined}
      />
    </div>
  )
}
