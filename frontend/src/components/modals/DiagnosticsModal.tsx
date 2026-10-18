import { useState, type FC } from 'react'
import { X, Activity, CheckCircle, AlertTriangle, XCircle, ShieldAlert, Copy, Check, Clock, Layers } from 'lucide-react'
import { PortalStatusDto } from '../../types/portal'
import { useUiStore } from '../../store/useUiStore'
import { usePortals } from '../../hooks/usePortals'
import { cn } from '../../utils/cn'

interface DiagnosticsModalProps {
  portalStatuses?: PortalStatusDto[]
}

export const DiagnosticsModal: FC<DiagnosticsModalProps> = ({ portalStatuses = [] }) => {
  const { isDiagnosticsModalOpen, setDiagnosticsModalOpen } = useUiStore()
  const { data: portals } = usePortals()
  const [copied, setCopied] = useState(false)

  if (!isDiagnosticsModalOpen) return null

  const handleCopy = () => {
    const summary = portalStatuses
      .map(
        (p) =>
          `[${p.portalId.toUpperCase()}] ${p.portalName}: ${p.status} (${p.durationMs}ms, ${p.itemCount} találat)${
            p.errorMessage ? ` - Hiba: ${p.errorMessage}` : ''
          }`
      )
      .join('\n')
    navigator.clipboard.writeText(summary)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-500/10 text-teal-400">
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
              {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
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

        {/* Content list */}
        <div className="flex-1 overflow-y-auto p-5 space-y-3">
          {portalStatuses.length > 0 ? (
            portalStatuses.map((p) => {
              const isSuccess = p.status === 'SUCCESS'
              const isBot = p.botProtected || p.status === 'BOT_PROTECTED'
              const isError = !isSuccess && !isBot

              return (
                <div
                  key={p.portalId}
                  className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] p-4 shadow-sm"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      {isSuccess && <CheckCircle className="h-5 w-5 text-emerald-400" />}
                      {isBot && <ShieldAlert className="h-5 w-5 text-amber-400" />}
                      {isError && <XCircle className="h-5 w-5 text-rose-400" />}
                      <div>
                        <h4 className="text-sm font-bold text-[var(--text-primary)]">
                          {p.portalName}
                        </h4>
                        <span className="text-[11px] font-mono text-[var(--text-secondary)]">
                          ID: {p.portalId}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <span className="flex items-center gap-1 text-xs text-[var(--text-secondary)]">
                        <Clock className="h-3.5 w-3.5" />
                        <span>{p.durationMs} ms</span>
                      </span>
                      <span className="flex items-center gap-1 text-xs font-semibold text-[var(--text-primary)]">
                        <Layers className="h-3.5 w-3.5 text-blue-400" />
                        <span>{p.itemCount} találat</span>
                      </span>
                      <span
                        className={cn(
                          'rounded-md px-2 py-0.5 text-xs font-bold border uppercase',
                          isSuccess
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : isBot
                            ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                        )}
                      >
                        {p.status}
                      </span>
                    </div>
                  </div>

                  {/* Error / Bot explanation */}
                  {p.errorMessage && (
                    <div className="mt-3 rounded-lg bg-black/20 p-2.5 text-xs font-mono text-rose-300 border border-rose-500/20">
                      {p.errorMessage}
                    </div>
                  )}

                  {isBot && (
                    <div className="mt-2.5 flex items-start gap-2 rounded-lg bg-amber-500/10 p-2.5 text-xs text-amber-300 border border-amber-500/20">
                      <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                      <span>
                        A portál Cloudflare / WAF bot-védelmet alkalmaz. A rendszer automatikus graceful fallbackként közvetlen keresőlinket generált.
                      </span>
                    </div>
                  )}
                </div>
              )
            })
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
                    <Activity className="h-4 w-4 text-blue-400" />
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
