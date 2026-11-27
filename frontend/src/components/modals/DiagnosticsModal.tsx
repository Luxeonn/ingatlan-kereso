import { useCallback, useRef, useState, type FC } from 'react'
import {
  X,
  Activity,
  CheckCircle,
  Copy,
  Check,
  Clock,
  Layers,
} from 'lucide-react'
import type { PortalStatusDto } from '../../types/portal'
import { useUiStore } from '../../store/useUiStore'
import { usePortals } from '../../hooks/usePortals'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { PortalStatusCard } from './DiagnosticsPortalCard'

interface DiagnosticsModalProps {
  portalStatuses?: PortalStatusDto[]
}

export const DiagnosticsModal: FC<DiagnosticsModalProps> = ({ portalStatuses = [] }) => {
  const { isDiagnosticsModalOpen, setDiagnosticsModalOpen, setIngatlanComImportModalOpen } = useUiStore()
  const { data: portals } = usePortals()
  const [copied, setCopied] = useState(false)
  const panelRef = useRef<HTMLDivElement>(null)
  const closeModal = useCallback(() => setDiagnosticsModalOpen(false), [setDiagnosticsModalOpen])

  useOutsideDismiss(panelRef, isDiagnosticsModalOpen, closeModal, true)

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
    void navigator.clipboard.writeText(summary)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div
      data-testid="diagnostics-modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget) closeModal()
      }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#0b1220]/25 p-4 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Portál Diagnosztika és Állapotjelentés"
        data-testid="diagnostics-modal"
        className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl glass-elevated"
      >
        <div className="flex items-center justify-between border-b border-[var(--border-hairline)] glass-chrome px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-teal-500/12 text-teal-700">
              <Activity className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-[#0b1220]">
                Portál Diagnosztika & Állapotjelentés
              </h3>
              <p className="text-xs text-[#42526b]">
                Web scraping megbízhatóság, válaszidők és bot-védelem állapotok
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleCopy}
              className="flex items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/80 px-3 py-1.5 text-xs font-semibold text-[#42526b] hover:text-[#0b1220] cursor-pointer"
            >
              {copied ? <Check className="h-3.5 w-3.5 text-emerald-700" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copied ? 'Másolva!' : 'Export'}</span>
            </button>
            <button
              type="button"
              aria-label="Bezárás"
              onClick={closeModal}
              className="rounded-full p-1.5 text-[#42526b] hover:bg-white/80 hover:text-[#0b1220] cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {portalStatuses.length > 0 && (
          <div className="grid grid-cols-3 gap-2.5 border-b border-[var(--border-hairline)] bg-white/50 px-5 py-2.5 text-xs tabular-nums">
            <div className="flex items-center justify-center gap-1.5 rounded-full border border-emerald-500/25 bg-[var(--status-bargain-bg)] px-2.5 py-1.5 font-bold text-[var(--status-bargain-fg)]">
              <CheckCircle className="h-3.5 w-3.5 shrink-0" />
              <span>
                Sikeres: {portalStatuses.filter((p) => p.status === 'SUCCESS').length}/{portalStatuses.length}
              </span>
            </div>
            <div className="flex items-center justify-center gap-1.5 rounded-full border border-[#0071e3]/25 bg-[#0071e3]/10 px-2.5 py-1.5 font-bold text-[#005bb5]">
              <Layers className="h-3.5 w-3.5 shrink-0" />
              <span>
                Összes találat: {portalStatuses.reduce((sum, p) => sum + (p.foundCount || 0), 0)} db
              </span>
            </div>
            <div className="flex items-center justify-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/75 px-2.5 py-1.5 font-bold text-[#0b1220]">
              <Clock className="h-3.5 w-3.5 shrink-0 text-[#42526b]" />
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
              <p className="text-xs text-[#42526b]">
                Még nem futott le keresés a jelenlegi munkamenetben. Az alábbi motorok állnak rendelkezésre:
              </p>
              {Array.isArray(portals) &&
                portals.map((p) => (
                  <div
                    key={p.id}
                    className="flex items-center justify-between rounded-2xl glass-surface p-3.5 tabular-nums"
                  >
                    <div className="flex items-center gap-2">
                      <Activity className="h-4 w-4 text-[#0071e3]" />
                      <span className="text-xs font-bold text-[#0b1220]">{p.name}</span>
                      <span className="text-[11px] font-mono text-[#42526b]">({p.id})</span>
                    </div>
                    <span className="text-xs text-[#42526b]">
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
