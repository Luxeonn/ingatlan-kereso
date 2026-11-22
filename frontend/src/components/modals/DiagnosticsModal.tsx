import { useState, type FC } from 'react'
import {
  X,
  Activity,
  CheckCircle,
  AlertTriangle,
  XCircle,
  ShieldAlert,
  Copy,
  Check,
  Clock,
  Layers,
  ExternalLink,
} from 'lucide-react'
import type { PortalStatusDto } from '../../types/portal'
import { useUiStore } from '../../store/useUiStore'
import { usePortals } from '../../hooks/usePortals'
import { cn } from '../../utils/cn'
import { openExternalUrl } from '../../utils/formatters'

interface DiagnosticsModalProps {
  portalStatuses?: PortalStatusDto[]
}

interface PortalStatusCardProps {
  portal: PortalStatusDto
  onOpenImport: () => void
}

function getStatusBadgeClass(isSuccess: boolean, isBot: boolean): string {
  if (isSuccess) {
    return 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)] border-emerald-500/25'
  }
  if (isBot) {
    return 'bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)] border-amber-500/25'
  }
  return 'bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)] border-rose-500/25'
}

const PortalStatusCard: FC<PortalStatusCardProps> = ({ portal: p, onOpenImport }) => {
  const isSuccess = p.status === 'SUCCESS'
  const isBot = p.status === 'BOT_PROTECTED'
  const isError = !isSuccess && !isBot

  return (
    <div
      data-testid={`portal-status-card-${p.portalId}`}
      className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] p-4 shadow-sm"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2.5">
          {isSuccess && <CheckCircle className="h-5 w-5 text-emerald-700" />}
          {isBot && <ShieldAlert className="h-5 w-5 text-amber-700" />}
          {isError && <XCircle className="h-5 w-5 text-rose-700" />}
          <div>
            <h4 className="text-sm font-bold text-[var(--text-primary)]">{p.portalName}</h4>
            <span className="text-[11px] font-mono text-[var(--text-secondary)]">
              ID: {p.portalId}
              {p.httpStatusCode ? ` · HTTP ${p.httpStatusCode}` : ''}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 tabular-nums">
          <span className="flex items-center gap-1 text-xs text-[var(--text-secondary)]">
            <Clock className="h-3.5 w-3.5" />
            <span>{p.latencyMs} ms</span>
          </span>
          <span className="flex items-center gap-1 text-xs font-semibold text-[var(--text-primary)]">
            <Layers className="h-3.5 w-3.5 text-blue-600" />
            <span>{p.foundCount} találat</span>
          </span>
          <span
            className={cn(
              'rounded-md px-2 py-0.5 text-xs font-bold border uppercase',
              getStatusBadgeClass(isSuccess, isBot),
            )}
          >
            {p.status}
          </span>
        </div>
      </div>

      {p.message && !isBot && (
        <div className="mt-3 rounded-lg bg-[var(--status-danger-bg)] p-2.5 text-xs font-mono text-[var(--status-danger-fg)] border border-rose-500/25">
          {p.message}
        </div>
      )}

      {p.technicalDetails && (
        <div
          data-testid={`portal-tech-details-${p.portalId}`}
          className="mt-2 rounded-lg bg-[var(--bg-card)] p-2 text-[11px] font-mono text-[var(--text-secondary)] border border-[var(--border-hairline)]"
        >
          {p.technicalDetails}
        </div>
      )}

      {isBot && (
        <div className="mt-2.5 flex flex-col gap-2.5 rounded-lg bg-[var(--status-warn-bg)] p-3 text-xs text-[var(--status-warn-fg)] border border-amber-500/25">
          <div className="flex items-start gap-2">
            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>
              A portál Cloudflare / WAF bot-védelmet alkalmaz. A rendszer automatikus graceful fallbackként közvetlen keresőlinket generált.
            </span>
          </div>
          {p.fallbackUrl && (
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="truncate font-mono text-[11px] opacity-90" title={p.fallbackUrl}>
                {p.fallbackUrl}
              </span>
              <button
                type="button"
                data-testid={`fallback-link-${p.portalId}`}
                onClick={() => openExternalUrl(p.fallbackUrl)}
                className="inline-flex w-fit shrink-0 items-center gap-1.5 rounded-lg border border-amber-500/35 bg-[var(--bg-surface)] px-3 py-1.5 text-xs font-semibold text-[var(--status-warn-fg)] transition-colors hover:opacity-90"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                <span>Keresés megnyitása itt: {p.portalName}</span>
              </button>
            </div>
          )}
        </div>
      )}

      {!isBot && p.fallbackUrl && (
        <div className="mt-2.5 flex flex-wrap items-center justify-between gap-2">
          <span
            className="truncate font-mono text-[11px] text-[var(--text-secondary)]"
            title={p.fallbackUrl}
          >
            {p.fallbackUrl}
          </span>
          <button
            type="button"
            data-testid={`fallback-link-${p.portalId}`}
            onClick={() => openExternalUrl(p.fallbackUrl)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-3 py-1.5 text-xs font-semibold text-[var(--text-primary)] transition-colors hover:border-blue-500/40"
          >
            <ExternalLink className="h-3.5 w-3.5 text-blue-600" />
            <span>Keresés megnyitása itt: {p.portalName}</span>
          </button>
        </div>
      )}

      {p.portalId === 'ingatlancom' && (
        <div className="mt-2.5 flex justify-end">
          <button
            type="button"
            data-testid="diagnostics-import-ingatlancom-btn"
            onClick={onOpenImport}
            className="inline-flex items-center gap-1.5 rounded-lg border border-sky-400/35 bg-sky-500/15 px-3 py-1.5 text-xs font-semibold text-sky-700 hover:bg-sky-500/25 transition-colors cursor-pointer"
          >
            <span>2., 3., ... oldal importálása böngészőből</span>
          </button>
        </div>
      )}
    </div>
  )
}

export const DiagnosticsModal: FC<DiagnosticsModalProps> = ({ portalStatuses = [] }) => {
  const { isDiagnosticsModalOpen, setDiagnosticsModalOpen, setIngatlanComImportModalOpen } = useUiStore()
  const { data: portals } = usePortals()
  const [copied, setCopied] = useState(false)

  if (!isDiagnosticsModalOpen) return null

  const handleCopy = () => {
    const summary = portalStatuses
      .map(
        (p) =>
          `[${p.portalId.toUpperCase()}] ${p.portalName}: ${p.status} (${p.latencyMs}ms, ${p.foundCount} találat)${
            p.message ? ` - Infó: ${p.message}` : ''
          }${p.fallbackUrl ? ` - Fallback: ${p.fallbackUrl}` : ''}`,
      )
      .join('\n')
    navigator.clipboard.writeText(summary)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[var(--bg-elevated)] shadow-2xl">
        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-500/10 text-teal-700">
              <Activity className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-[var(--text-primary)]">
                Portál Diagnosztika & Állapotjelentés
              </h3>
              <p className="text-xs text-[var(--text-secondary)]">
                Web scraping megbízhatóság, válaszidők és bot-védelem állapotok
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleCopy}
              className="flex items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
            >
              {copied ? <Check className="h-3.5 w-3.5 text-emerald-700" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copied ? 'Másolva!' : 'Export'}</span>
            </button>
            <button
              type="button"
              aria-label="Bezárás"
              onClick={() => setDiagnosticsModalOpen(false)}
              className="rounded-lg p-1 text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)]"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-3">
          {portalStatuses.length > 0 ? (
            portalStatuses.map((p) => (
              <PortalStatusCard
                key={p.portalId}
                portal={p}
                onOpenImport={() => {
                  setDiagnosticsModalOpen(false)
                  setIngatlanComImportModalOpen(true)
                }}
              />
            ))
          ) : (
            <div className="space-y-3">
              <p className="text-xs text-[var(--text-secondary)]">
                Még nem futott le keresés a jelenlegi munkamenetben. Az alábbi motorok állnak rendelkezésre:
              </p>
              {Array.isArray(portals) &&
                portals.map((p) => (
                  <div
                    key={p.id}
                    className="flex items-center justify-between rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] p-3.5"
                  >
                    <div className="flex items-center gap-2">
                      <Activity className="h-4 w-4 text-blue-600" />
                      <span className="text-xs font-bold text-[var(--text-primary)]">{p.name}</span>
                      <span className="text-[11px] font-mono text-[var(--text-secondary)]">({p.id})</span>
                    </div>
                    <span className="text-xs text-[var(--text-secondary)]">
                      {p.savedListingCount ?? 0} elmentett hirdetés az adatbázisban
                    </span>
                  </div>
                ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
