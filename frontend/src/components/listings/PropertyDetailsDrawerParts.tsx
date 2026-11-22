import { useState, type FC } from 'react'
import { ExternalLink, Clock, Unlink, RotateCcw } from 'lucide-react'
import type { SourceListing } from '../../types/property'
import type { DedupOverrideDto } from '../../api/dedupApi'
import {
  formatPriceMillion,
  formatPriceFt,
  formatDate,
  getPortalStyle,
  formatOverrideEndpointLabel,
} from '../../utils/formatters'
import { cn } from '../../utils/cn'

interface SourceThumbnailPreviewProps {
  imageUrl: string
  alt: string
  onError: () => void
}

const SourceThumbnailPreview: FC<SourceThumbnailPreviewProps> = ({ imageUrl, alt, onError }) => (
  <div
    data-testid="source-thumbnail-preview"
    className="pointer-events-none absolute left-0 bottom-full mb-1.5 z-50 h-[120px] w-[160px] overflow-hidden rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] p-1 shadow-xl animate-in fade-in-0 duration-150"
  >
    <img
      src={imageUrl}
      alt={alt}
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={onError}
      className="h-full w-full rounded object-cover"
    />
  </div>
)

function resolveSourceRowMeta(source: SourceListing) {
  const portalName = source.portal || 'Forrás'
  const hasPrice = source.priceHuf !== null && source.priceHuf !== undefined
  return {
    portalName,
    portalStyle: getPortalStyle(portalName),
    hasPrice,
    million: hasPrice ? (source.priceHuf ?? 0) / 1_000_000 : null,
    isActive: !source.status || source.status.toUpperCase() === 'ACTIVE',
    freshness: source.lastSeenAt || source.firstSeenAt,
    imageUrl: source.imageUrl?.trim() || '',
  }
}

export interface SourceItemRowProps {
  source: SourceListing
  canSplit: boolean
  isMutating: boolean
  onOpenSource: (url?: string) => void
  onSplitSource: (url: string) => void
}

export const SourceItemRow: FC<SourceItemRowProps> = ({
  source,
  canSplit,
  isMutating,
  onOpenSource,
  onSplitSource,
}) => {
  const [isPreviewOpen, setIsPreviewOpen] = useState(false)
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null)
  const [confirmSplit, setConfirmSplit] = useState(false)

  const { portalName, portalStyle, hasPrice, million, isActive, freshness, imageUrl } =
    resolveSourceRowMeta(source)
  const canShowPreview = isPreviewOpen && imageUrl.length > 0 && failedImageUrl !== imageUrl

  return (
    <div
      data-testid="source-item"
      onMouseEnter={() => setIsPreviewOpen(true)}
      onMouseLeave={() => setIsPreviewOpen(false)}
      onFocus={() => setIsPreviewOpen(true)}
      onBlur={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget)) {
          setIsPreviewOpen(false)
        }
      }}
      className="relative flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 py-2 first:pt-1 last:pb-1"
    >
      {canShowPreview && (
        <SourceThumbnailPreview
          imageUrl={imageUrl}
          alt={source.title || `${portalName} előnézet`}
          onError={() => setFailedImageUrl(imageUrl)}
        />
      )}

      <div className="flex flex-wrap items-center gap-2">
        <span
          data-testid="source-portal-badge"
          className={cn(
            'rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider uppercase',
            portalStyle.bg,
          )}
        >
          {portalName}
        </span>

        <span
          data-testid="source-status-badge"
          className={cn(
            'rounded-md px-1.5 py-0.5 text-[10px] font-bold border uppercase',
            isActive
              ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400'
              : 'border-slate-500/30 bg-slate-500/10 text-slate-400',
          )}
        >
          {isActive ? 'Aktív' : 'Inaktív'}
        </span>

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
        <div className="text-right">
          <div className="text-xs font-bold text-blue-400">
            {million !== null ? formatPriceMillion(million) : 'Ár a hirdetésben'}
          </div>
          {hasPrice && source.priceHuf !== undefined && source.priceHuf !== null && (
            <div className="text-[10px] text-[var(--text-secondary)]">
              {formatPriceFt(source.priceHuf)}
            </div>
          )}
        </div>

        {source.url && (
          <button
            type="button"
            onClick={() => onOpenSource(source.url)}
            aria-label={`Megnyitás: ${portalName}`}
            title={`Megnyitás: ${portalName}`}
            data-testid="source-external-link"
            className="flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-2.5 py-1 text-xs font-semibold text-[var(--text-primary)] hover:border-blue-500/50 hover:bg-blue-600/10 hover:text-blue-400 active:scale-95 transition-all"
          >
            <span className="hidden xs:inline">Megnyitás</span>
            <ExternalLink className="h-3.5 w-3.5 text-blue-400" aria-hidden="true" />
          </button>
        )}

        {canSplit && source.url && (
          <SplitSourceControl
            portalName={portalName}
            confirmSplit={confirmSplit}
            isMutating={isMutating}
            onRequestConfirm={() => setConfirmSplit(true)}
            onCancelConfirm={() => setConfirmSplit(false)}
            onConfirmSplit={() => {
              setConfirmSplit(false)
              if (source.url) onSplitSource(source.url)
            }}
          />
        )}
      </div>
    </div>
  )
}

interface SplitSourceControlProps {
  portalName: string
  confirmSplit: boolean
  isMutating: boolean
  onRequestConfirm: () => void
  onCancelConfirm: () => void
  onConfirmSplit: () => void
}

const SplitSourceControl: FC<SplitSourceControlProps> = ({
  portalName,
  confirmSplit,
  isMutating,
  onRequestConfirm,
  onCancelConfirm,
  onConfirmSplit,
}) => {
  if (confirmSplit) {
    return (
      <div className="flex items-center gap-1">
        <button
          type="button"
          data-testid="confirm-split-button"
          disabled={isMutating}
          onClick={onConfirmSplit}
          className="rounded-lg border border-rose-500/40 bg-[var(--status-danger-bg)] px-2 py-1 text-[11px] font-bold text-[var(--status-danger-fg)] hover:opacity-90 disabled:opacity-50"
        >
          Megerősítés
        </button>
        <button
          type="button"
          onClick={onCancelConfirm}
          className="rounded-lg border border-[var(--border-color)] px-2 py-1 text-[11px] text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
        >
          Mégse
        </button>
      </div>
    )
  }
  return (
    <button
      type="button"
      data-testid="split-source-button"
      disabled={isMutating}
      onClick={onRequestConfirm}
      aria-label={`Nem ugyanaz: ${portalName}`}
      title="Nem ugyanaz – forrás leválasztása"
      className="flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-2 py-1 text-xs font-semibold text-[var(--text-secondary)] hover:border-rose-500/50 hover:bg-rose-500/10 hover:text-rose-300 transition-all disabled:opacity-50"
    >
      <Unlink className="h-3.5 w-3.5" aria-hidden="true" />
      <span className="hidden sm:inline">Nem ugyanaz</span>
    </button>
  )
}

export interface ManualDedupStatusSectionProps {
  manuallyMerged?: boolean
  manuallySplit?: boolean
  sourcesCount?: number
  overrides: DedupOverrideDto[]
  isMutating: boolean
  onUndoOverride?: (overrideId: number) => void
}

const DEFAULT_VISIBLE_OVERRIDES = 3

interface OverrideRowProps {
  overrideItem: DedupOverrideDto
  isMutating: boolean
  onUndoOverride?: (overrideId: number) => void
}

const OverrideRow: FC<OverrideRowProps> = ({ overrideItem, isMutating, onUndoOverride }) => {
  const actionLabel = overrideItem.type === 'FORCE_MERGE' ? 'Összevonás' : 'Szétválasztás'
  const shortPair = `${formatOverrideEndpointLabel(overrideItem.urlA)} ↔ ${formatOverrideEndpointLabel(overrideItem.urlB)}`
  const fullPair = `${overrideItem.urlA} ↔ ${overrideItem.urlB}`

  return (
    <div
      data-testid="dedup-override-row"
      className="flex items-center justify-between gap-2 text-[10px] text-[var(--status-info-fg)]"
    >
      <span className="truncate" title={fullPair}>
        {actionLabel}: {shortPair}
      </span>
      {onUndoOverride && (
        <button
          type="button"
          data-testid={`undo-override-${overrideItem.id}`}
          disabled={isMutating}
          onClick={() => onUndoOverride(overrideItem.id)}
          className="inline-flex shrink-0 items-center gap-1 rounded border border-indigo-400/40 bg-[var(--bg-surface)] px-2 py-0.5 font-semibold text-[var(--status-info-fg)] hover:opacity-90 disabled:opacity-50"
        >
          <RotateCcw className="h-3 w-3" aria-hidden="true" />
          <span>Visszavonás</span>
        </button>
      )}
    </div>
  )
}

export const ManualDedupStatusSection: FC<ManualDedupStatusSectionProps> = ({
  manuallyMerged,
  manuallySplit,
  sourcesCount,
  overrides,
  isMutating,
  onUndoOverride,
}) => {
  const [showAllOverrides, setShowAllOverrides] = useState(false)

  if (!manuallyMerged && !manuallySplit && overrides.length === 0) {
    return null
  }

  const hasOverflow = overrides.length > DEFAULT_VISIBLE_OVERRIDES
  const visibleOverrides =
    showAllOverrides || !hasOverflow ? overrides : overrides.slice(0, DEFAULT_VISIBLE_OVERRIDES)
  const mergedHeader =
    sourcesCount && sourcesCount > 1
      ? `Kézzel összevonva · ${sourcesCount} forrás`
      : 'Kézzel összevonva'

  return (
    <div
      data-testid="manual-dedup-status"
      className="flex flex-col gap-1.5 rounded-lg border border-indigo-500/30 bg-[var(--status-info-bg)] p-2.5 text-[11px] text-[var(--status-info-fg)]"
    >
      <div className="flex flex-wrap items-center gap-2 font-semibold">
        {manuallyMerged && <span>{mergedHeader}</span>}
        {manuallySplit && <span>Kézzel szétválasztott forrás</span>}
      </div>

      {visibleOverrides.length > 0 && (
        <div
          className={cn(
            'flex flex-col gap-1',
            showAllOverrides && hasOverflow && 'max-h-48 overflow-y-auto pr-1',
          )}
        >
          {visibleOverrides.map((ov) => (
            <OverrideRow
              key={ov.id}
              overrideItem={ov}
              isMutating={isMutating}
              onUndoOverride={onUndoOverride}
            />
          ))}
        </div>
      )}

      {hasOverflow && (
        <button
          type="button"
          data-testid="toggle-all-overrides-button"
          onClick={() => setShowAllOverrides((prev) => !prev)}
          className="self-start text-[10px] font-semibold text-[var(--status-info-fg)] underline decoration-indigo-400/50 hover:opacity-80 cursor-pointer"
        >
          {showAllOverrides
            ? 'Kevesebb megjelenítése'
            : `Összes (${overrides.length}) kapcsolat megjelenítése`}
        </button>
      )}
    </div>
  )
}
