import { useEffect, useState, type FC } from 'react'
import { Layers, Tag, TrendingDown } from 'lucide-react'
import type { IngatlanItem, SourceListing } from '../../types/property'
import { getDedupOverrides, type DedupOverrideDto } from '../../api/dedupApi'
import {
  getEffectivePrice,
  getCrossPortalSpread,
  formatPriceFt,
  formatPricePerSqm,
  openExternalUrl,
} from '../../utils/formatters'
import { PropertyTypeBadge } from './PropertyTypeBadge'
import { ManualDedupStatusSection, SourceItemRow } from './PropertyDetailsDrawerParts'
import { PropertyTimelineSection } from './PropertyTimelineSection'
import { cn } from '../../utils/cn'

export interface PropertyDetailsDrawerProps {
  item: IngatlanItem
  onMarkViewed?: (url: string) => void
  onSplitSource?: (sourceUrl: string, clusterUrls: string[]) => Promise<boolean | void> | void
  onUndoOverride?: (overrideId: number) => Promise<boolean | void> | void
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

function filterRelevantOverrides(
  overrides: DedupOverrideDto[],
  clusterUrls: string[],
): DedupOverrideDto[] {
  const clusterSet = new Set(clusterUrls)
  const byId = new Map<number, DedupOverrideDto>()
  for (const ov of overrides) {
    const isRelevant =
      ov.type === 'FORCE_MERGE'
        ? clusterSet.has(ov.urlA) && clusterSet.has(ov.urlB)
        : clusterSet.has(ov.urlA) || clusterSet.has(ov.urlB)
    if (isRelevant) {
      byId.set(ov.id, ov)
    }
  }
  return Array.from(byId.values())
}

const DrawerCategoryHeader: FC<{ item: IngatlanItem }> = ({ item }) => {
  const pct = item.percentageBelowMedian
  const hasMarketPosition =
    pct !== null && pct !== undefined && (item.referenceSampleSize ?? 0) >= 5
  const isBelow = (pct ?? 0) >= 0

  return (
    <div className="flex flex-col gap-1.5 pb-3 border-b border-[var(--border-hairline)]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 font-semibold text-[#42526b]">
          <Tag className="h-3.5 w-3.5 text-[#0071e3]" aria-hidden="true" />
          <span>Besorolás és Kategória</span>
        </div>
        {hasMarketPosition && (
          <span
            data-testid="drawer-market-position"
            className={cn(
              'rounded-full border px-2.5 py-0.5 text-[10px] font-bold tabular-nums',
              isBelow
                ? 'border-emerald-500/30 bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]'
                : 'border-[var(--border-hairline)] bg-white/75 text-[#42526b]',
            )}
          >
            {isBelow
              ? `-${Math.round(pct ?? 0)}% medián alatt`
              : `+${Math.abs(Math.round(pct ?? 0))}% medián felett`}
            {item.referenceMedianPricePerSquareMeter
              ? ` (Medián: ${formatPricePerSqm(item.referenceMedianPricePerSquareMeter)})`
              : ''}
          </span>
        )}
      </div>
      <div className="flex flex-wrap items-center gap-2 pt-0.5">
        <PropertyTypeBadge propertyType={item.propertyType} />
        {item.details && (
          <span className="text-[11px] text-[#42526b] bg-white/75 px-2.5 py-0.5 rounded-full border border-[var(--border-hairline)] truncate max-w-full">
            {item.details}
          </span>
        )}
      </div>
    </div>
  )
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

  const sources = buildFallbackSources(item)
  const clusterUrls = sources.map((s) => s.url).filter((u): u is string => Boolean(u))
  const clusterUrlKey = clusterUrls.join('\0')
  const spreadInfo = getCrossPortalSpread(item)
  const isMultiSource = sources.length > 1

  useEffect(() => {
    if (!hasManualOverride) return undefined
    const urls = clusterUrlKey ? clusterUrlKey.split('\0') : item.url ? [item.url] : []
    if (urls.length === 0) return undefined
    let cancelled = false
    getDedupOverrides(urls)
      .then((list) => {
        if (!cancelled) setOverrides(filterRelevantOverrides(list, urls))
      })
      .catch(() => {
        if (!cancelled) setOverrides([])
      })
    return () => {
      cancelled = true
    }
  }, [item.url, clusterUrlKey, hasManualOverride])

  const handleOpenSource = (url?: string) => {
    if (!url) return
    openExternalUrl(url)
    onMarkViewed?.(url)
  }

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
      const ok = await onUndoOverride(overrideId)
      if (ok !== false) {
        setOverrides((prev) => prev.filter((o) => o.id !== overrideId))
      }
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
        'mt-3.5 flex flex-col gap-3 rounded-2xl glass-surface p-3.5 transition-all animate-in fade-in-50 duration-200 text-xs',
        className,
      )}
    >
      <DrawerCategoryHeader item={item} />

      {isMultiSource && (
        <div data-testid="sources-list-section" className="flex flex-col gap-2">
          <div className="flex items-center justify-between text-xs font-semibold text-[#42526b]">
            <div className="flex items-center gap-1.5 text-[#0b1220]">
              <Layers className="h-3.5 w-3.5 text-[#0071e3]" aria-hidden="true" />
              <span>Elérhető források ({sources.length})</span>
            </div>
            <span className="text-[11px] text-[#42526b]">
              Legalacsonyabb ár kiemelve
            </span>
          </div>

          {spreadInfo && (
            <div
              data-testid="drawer-cross-portal-spread"
              className="flex items-center justify-between gap-2 rounded-lg border border-emerald-500/30 bg-[var(--status-bargain-bg)] px-2.5 py-1.5 text-[11px] text-[var(--status-bargain-fg)] tabular-nums"
            >
              <span className="flex items-center gap-1.5 font-bold">
                <TrendingDown className="h-3.5 w-3.5 shrink-0" />
                <span>
                  Portál árrés megtakarítás: -{formatPriceFt(spreadInfo.spreadAmountHuf)} (-{spreadInfo.spreadPercent}%)
                </span>
              </span>
              <span className="text-[10px] font-semibold opacity-90">
                Legjobb: {spreadInfo.cheapestPortal}
              </span>
            </div>
          )}

          <div className="flex flex-col divide-y divide-[var(--border-hairline)]">
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

      <PropertyTimelineSection item={item} />

      <ManualDedupStatusSection
        manuallyMerged={item.manuallyMerged}
        manuallySplit={item.manuallySplit}
        sourcesCount={sources.length}
        overrides={hasManualOverride ? overrides : []}
        isMutating={isMutating}
        onUndoOverride={onUndoOverride ? (id) => void handleUndo(id) : undefined}
      />
    </div>
  )
}
