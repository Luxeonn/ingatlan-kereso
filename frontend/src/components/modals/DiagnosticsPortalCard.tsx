import type { FC, ReactNode } from 'react'
import {
  CheckCircle,
  AlertTriangle,
  XCircle,
  ShieldAlert,
  Clock,
  Layers,
  ExternalLink,
} from 'lucide-react'
import type { PortalStatusDto } from '../../types/portal'
import { cn } from '../../utils/cn'
import { openExternalUrl } from '../../utils/formatters'

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

function renderStatusIcon(status: string): ReactNode {
  if (status === 'SUCCESS') return <CheckCircle className="h-5 w-5" />
  if (status === 'BOT_PROTECTED') return <ShieldAlert className="h-5 w-5" />
  if (status === 'EMPTY') return <Layers className="h-5 w-5" />
  return <XCircle className="h-5 w-5" />
}

function renderMessageIcon(status: string): ReactNode {
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
          className="truncate font-mono text-[11px] text-[#42526b] max-w-[55%]"
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
            className="inline-flex items-center gap-1.5 rounded-full border border-[#0071e3]/35 bg-[#0071e3]/10 px-3 py-1.5 text-xs font-bold text-[#005bb5] hover:bg-[#0071e3]/15 transition-colors cursor-pointer"
          >
            <span>2., 3., ... oldal importálása böngészőből</span>
          </button>
        )}
        {p.fallbackUrl && (
          <button
            type="button"
            data-testid={`fallback-link-${p.portalId}`}
            onClick={() => openExternalUrl(p.fallbackUrl)}
            className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/80 px-3 py-1.5 text-xs font-bold text-[#0b1220] transition-colors hover:border-[#0071e3]/50 hover:text-[#0071e3] cursor-pointer"
          >
            <ExternalLink className="h-3.5 w-3.5 text-[#0071e3]" />
            <span>Keresés megnyitása itt: {p.portalName}</span>
          </button>
        )}
      </div>
    </div>
  )
}

export const PortalStatusCard: FC<PortalStatusCardProps> = ({ portal: p, onOpenImport }) => {
  const isBot = p.status === 'BOT_PROTECTED'
  const theme = getStatusTheme(p.status)
  const isHttpOk = typeof p.httpStatusCode === 'number' && p.httpStatusCode >= 200 && p.httpStatusCode < 300

  return (
    <div
      data-testid={`portal-status-card-${p.portalId}`}
      className={cn(
        'rounded-2xl glass-surface p-4 transition-shadow hover:shadow-md',
        theme.borderAccent,
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className={cn('flex h-9 w-9 shrink-0 items-center justify-center rounded-xl', theme.iconBg)}>
            {renderStatusIcon(p.status)}
          </div>
          <div>
            <h4 className="text-sm font-bold text-[#0b1220]">{p.portalName}</h4>
            <div className="mt-0.5 flex flex-wrap items-center gap-1.5 text-[11px] font-mono text-[#42526b]">
              <span className="rounded bg-white/75 px-1.5 py-0.5 border border-[var(--border-hairline)]">
                ID: {p.portalId}
              </span>
              {p.httpStatusCode ? (
                <span
                  className={cn(
                    'rounded px-1.5 py-0.5 font-semibold border tabular-nums',
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

        <div className="flex flex-wrap items-center gap-2 tabular-nums">
          <span className="inline-flex items-center gap-1 rounded-full border border-[var(--border-hairline)] bg-white/75 px-2.5 py-1 text-xs font-medium text-[#42526b]">
            <Clock className="h-3.5 w-3.5 text-[#42526b]" />
            <span>{p.latencyMs} ms</span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-full border border-[#0071e3]/20 bg-[#0071e3]/10 px-2.5 py-1 text-xs font-bold text-[#005bb5]">
            <Layers className="h-3.5 w-3.5 text-[#005bb5]" />
            <span>{p.foundCount} találat</span>
          </span>
          <span
            className={cn(
              'rounded-full px-2.5 py-1 text-xs font-bold border uppercase tracking-wide',
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
          className={cn('mt-3 flex items-center gap-2 rounded-xl border p-2.5 text-xs font-semibold', theme.messageBox)}
        >
          {renderMessageIcon(p.status)}
          <span>{p.message}</span>
        </div>
      )}

      {p.technicalDetails && (
        <div
          data-testid={`portal-tech-details-${p.portalId}`}
          className="mt-2.5 rounded-xl bg-white/60 px-3 py-2 text-[11px] font-mono text-[#42526b] border border-[var(--border-hairline)]"
        >
          {p.technicalDetails}
        </div>
      )}

      {isBot && (
        <div className="mt-3 flex flex-col gap-2.5 rounded-xl bg-[var(--status-warn-bg)] p-3 text-xs text-[var(--status-warn-fg)] border border-amber-500/30">
          <div className="flex items-start gap-2 font-medium">
            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>
              A portál Cloudflare / WAF bot-védelmet alkalmaz. A rendszer automatikus graceful fallbackként közvetlen keresőlinket generált.
            </span>
          </div>
          {p.fallbackUrl && (
            <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-amber-500/20">
              <span className="truncate font-mono text-[11px] opacity-90 max-w-[60%]" title={p.fallbackUrl}>
                {p.fallbackUrl}
              </span>
              <button
                type="button"
                data-testid={`fallback-link-${p.portalId}`}
                onClick={() => openExternalUrl(p.fallbackUrl)}
                className="inline-flex w-fit shrink-0 items-center gap-1.5 rounded-full border border-amber-500/40 bg-white/80 px-3 py-1.5 text-xs font-bold text-[var(--status-warn-fg)] transition-colors hover:bg-amber-50 cursor-pointer"
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
