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

function getStatusTheme(status: string): {
  badge: string
  messageBox: string
  borderAccent: string
  iconBg: string
} {
  if (status === 'SUCCESS') {
    return {
      badge: 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)] border-emerald-500/30',
      messageBox: 'bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)] border-emerald-500/25',
      borderAccent: 'border-l-4 border-l-emerald-500',
      iconBg: 'bg-emerald-500/12 text-emerald-700',
    }
  }
  if (status === 'BOT_PROTECTED') {
    return {
      badge: 'bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)] border-amber-500/30',
      messageBox: 'bg-[var(--status-warn-bg)] text-[var(--status-warn-fg)] border-amber-500/25',
      borderAccent: 'border-l-4 border-l-amber-500',
      iconBg: 'bg-amber-500/12 text-amber-700',
    }
  }
  if (status === 'EMPTY') {
    return {
      badge: 'bg-slate-100 text-slate-700 border-slate-300',
      messageBox: 'bg-slate-100/90 text-slate-700 border-slate-300',
      borderAccent: 'border-l-4 border-l-slate-400',
      iconBg: 'bg-slate-500/12 text-slate-700',
    }
  }
  return {
    badge: 'bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)] border-rose-500/30',
    messageBox: 'bg-[var(--status-danger-bg)] text-[var(--status-danger-fg)] border-rose-500/25',
    borderAccent: 'border-l-4 border-l-rose-500',
    iconBg: 'bg-rose-500/12 text-rose-700',
  }
}

function renderStatusIcon(status: string) {
  if (status === 'SUCCESS') return <CheckCircle className="h-5 w-5" />
  if (status === 'BOT_PROTECTED') return <ShieldAlert className="h-5 w-5" />
  if (status === 'EMPTY') return <Layers className="h-5 w-5" />
  return <XCircle className="h-5 w-5" />
}

function renderMessageIcon(status: string) {
  if (status === 'SUCCESS') return <CheckCircle className="h-4 w-4 shrink-0" />
  if (status === 'EMPTY') return <Layers className="h-4 w-4 shrink-0" />
  return <AlertTriangle className="h-4 w-4 shrink-0" />
}

const PortalCardFooterActions: FC<{ portal: PortalStatusDto; onOpenImport: () => void }> = ({
  portal: p,
  onOpenImport,
}) => {
  if (!p.fallbackUrl && p.portalId !== 'ingatlancom') return null
  return (
    <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-[var(--border-hairline)] pt-2.5">
      {p.fallbackUrl ? (
        <span
          className="truncate font-mono text-[11px] text-[var(--text-secondary)] max-w-full sm:max-w-[55%]"
          title={p.fallbackUrl}
        >
          {p.fallbackUrl}
        </span>
      ) : (
        <span />
      )}
      <div className="flex flex-wrap items-center gap-2 ml-auto">
        {p.portalId === 'ingatlancom' && (
          <button
            type="button"
            data-testid="diagnostics-import-ingatlancom-btn"
            onClick={onOpenImport}
            className="inline-flex items-center gap-1.5 rounded-lg border border-sky-500/35 bg-sky-500/10 px-3 py-1.5 text-xs font-bold text-sky-700 hover:bg-sky-500/20 transition-colors cursor-pointer"
          >
            <span>2., 3., ... oldal importálása böngészőből</span>
          </button>
        )}
        {p.fallbackUrl && (
          <button
            type="button"
            data-testid={`fallback-link-${p.portalId}`}
            onClick={() => openExternalUrl(p.fallbackUrl)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] px-3 py-1.5 text-xs font-bold text-[var(--text-primary)] transition-colors hover:border-blue-500/50 hover:text-blue-700 cursor-pointer"
          >
            <ExternalLink className="h-3.5 w-3.5 text-blue-600" />
            <span>Keresés megnyitása itt: {p.portalName}</span>
          </button>
        )}
      </div>
    </div>
  )
}

const PortalStatusCard: FC<PortalStatusCardProps> = ({ portal: p, onOpenImport }) => {
  const isBot = p.status === 'BOT_PROTECTED'
  const theme = getStatusTheme(p.status)
  const isHttpOk = typeof p.httpStatusCode === 'number' && p.httpStatusCode >= 200 && p.httpStatusCode < 300

  return (
    <div
      data-testid={`portal-status-card-${p.portalId}`}
      className={cn(
        'rounded-xl border border-[var(--border-color)] bg-[var(--bg-surface)] p-4 shadow-sm transition-shadow hover:shadow-md',
        theme.borderAccent,
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className={cn('flex h-9 w-9 shrink-0 items-center justify-center rounded-lg', theme.iconBg)}>
            {renderStatusIcon(p.status)}
          </div>
          <div>
            <h4 className="text-sm font-bold text-[var(--text-primary)]">{p.portalName}</h4>
            <div className="mt-0.5 flex flex-wrap items-center gap-1.5 text-[11px] font-mono text-[var(--text-secondary)]">
              <span className="rounded bg-[var(--bg-input)] px-1.5 py-0.5 border border-[var(--border-hairline)]">
                ID: {p.portalId}
              </span>
              {p.httpStatusCode ? (
                <span
                  className={cn(
                    'rounded px-1.5 py-0.5 font-semibold border',
                    isHttpOk
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-amber-50 text-amber-800 border-amber-200',
                  )}
                >
                  HTTP {p.httpStatusCode}
                </span>
              ) : null}
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 tabular-nums">
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2 py-1 text-xs font-medium text-[var(--text-secondary)]">
            <Clock className="h-3.5 w-3.5 text-slate-600" />
            <span>{p.latencyMs} ms</span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg border border-blue-500/20 bg-blue-500/10 px-2.5 py-1 text-xs font-bold text-blue-700">
            <Layers className="h-3.5 w-3.5 text-blue-600" />
            <span>{p.foundCount} találat</span>
          </span>
          <span
            className={cn(
              'rounded-lg px-2.5 py-1 text-xs font-bold border uppercase tracking-wide',
              theme.badge,
            )}
          >
            {p.status}
          </span>
        </div>
      </div>

      {p.message && !isBot && (
        <div
          data-testid={`portal-message-${p.portalId}`}
          className={cn('mt-3 flex items-center gap-2 rounded-lg border p-2.5 text-xs font-semibold', theme.messageBox)}
        >
          {renderMessageIcon(p.status)}
          <span>{p.message}</span>
        </div>
      )}

      {p.technicalDetails && (
        <div
          data-testid={`portal-tech-details-${p.portalId}`}
          className="mt-2.5 rounded-lg bg-[var(--bg-input)]/80 px-3 py-2 text-[11px] font-mono text-[var(--text-secondary)] border border-[var(--border-hairline)]"
        >
          {p.technicalDetails}
        </div>
      )}

      {isBot && (
        <div className="mt-3 flex flex-col gap-2.5 rounded-lg bg-[var(--status-warn-bg)] p-3 text-xs text-[var(--status-warn-fg)] border border-amber-500/30">
          <div className="flex items-start gap-2 font-medium">
            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>
              A portál Cloudflare / WAF bot-védelmet alkalmaz. A rendszer automatikus graceful fallbackként közvetlen keresőlinket generált.
            </span>
          </div>
          {p.fallbackUrl && (
            <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-amber-500/20">
              <span className="truncate font-mono text-[11px] opacity-90 max-w-full sm:max-w-[60%]" title={p.fallbackUrl}>
                {p.fallbackUrl}
              </span>
              <button
                type="button"
                data-testid={`fallback-link-${p.portalId}`}
                onClick={() => openExternalUrl(p.fallbackUrl)}
                className="inline-flex w-fit shrink-0 items-center gap-1.5 rounded-lg border border-amber-500/40 bg-[var(--bg-surface)] px-3 py-1.5 text-xs font-bold text-[var(--status-warn-fg)] transition-colors hover:bg-amber-50 cursor-pointer"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                <span>Keresés megnyitása itt: {p.portalName}</span>
              </button>
            </div>
          )}
        </div>
      )}

      {!isBot && <PortalCardFooterActions portal={p} onOpenImport={onOpenImport} />}
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

        {portalStatuses.length > 0 && (
          <div className="grid grid-cols-3 gap-2.5 border-b border-[var(--border-hairline)] bg-[var(--bg-body)]/70 px-5 py-2.5 text-xs tabular-nums">
            <div className="flex items-center justify-center gap-1.5 rounded-lg border border-emerald-500/25 bg-[var(--status-bargain-bg)] px-2.5 py-1.5 font-bold text-[var(--status-bargain-fg)]">
              <CheckCircle className="h-3.5 w-3.5 shrink-0" />
              <span>
                Sikeres: {portalStatuses.filter((p) => p.status === 'SUCCESS').length}/{portalStatuses.length}
              </span>
            </div>
            <div className="flex items-center justify-center gap-1.5 rounded-lg border border-blue-500/25 bg-blue-500/10 px-2.5 py-1.5 font-bold text-blue-700">
              <Layers className="h-3.5 w-3.5 shrink-0" />
              <span>
                Összes találat: {portalStatuses.reduce((sum, p) => sum + (p.foundCount || 0), 0)} db
              </span>
            </div>
            <div className="flex items-center justify-center gap-1.5 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-surface)] px-2.5 py-1.5 font-bold text-[var(--text-primary)]">
              <Clock className="h-3.5 w-3.5 shrink-0 text-slate-600" />
              <span>
                Max válaszidő: {Math.max(...portalStatuses.map((p) => p.latencyMs || 0))} ms
              </span>
            </div>
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-5 space-y-3.5">
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
