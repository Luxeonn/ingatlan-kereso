import type { FC } from 'react'
import { AlertCircle, AlertTriangle } from 'lucide-react'
import type { PortalStatusDto } from '../../types/portal'

interface SearchErrorBannerProps {
  error: unknown
  onRetry: () => void
}

export const SearchErrorBanner: FC<SearchErrorBannerProps> = ({ error, onRetry }) => (
  <div
    role="alert"
    data-testid="search-error-banner"
    className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-red-500/40 bg-red-500/10 p-4 text-xs text-red-200 shadow-md"
  >
    <div className="flex items-center gap-2.5">
      <AlertCircle className="h-5 w-5 text-red-400 shrink-0" />
      <div>
        <p className="font-bold text-red-300">Hiba történt a keresés során</p>
        <p className="text-[11px] text-red-200/80">
          {error instanceof Error ? error.message : 'Nem sikerült lekérni a hirdetéseket a szervertől. Kérjük, próbáld újra!'}
        </p>
      </div>
    </div>
    <button
      type="button"
      onClick={onRetry}
      className="rounded-lg bg-red-600 px-3.5 py-1.5 font-bold text-white hover:bg-red-500 transition-colors shrink-0"
    >
      Újrapróbálás
    </button>
  </div>
)

interface PartialPortalWarningBannerProps {
  failures: PortalStatusDto[]
  isTotalFailure?: boolean
  hasIngatlanComPartial?: boolean
  showEnableAllPortals?: boolean
  onOpenDiagnostics: () => void
  onOpenImportModal?: () => void
  onEnableAllPortals?: () => void
}

export const PartialPortalWarningBanner: FC<PartialPortalWarningBannerProps> = ({
  failures,
  isTotalFailure = false,
  hasIngatlanComPartial = false,
  showEnableAllPortals = false,
  onOpenDiagnostics,
  onOpenImportModal,
  onEnableAllPortals,
}) => (
  <div
    role="status"
    data-testid="partial-portal-warning-banner"
    data-total-failure={isTotalFailure ? 'true' : 'false'}
    className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 rounded-xl border border-amber-500/40 bg-amber-500/10 p-3 text-xs text-amber-200 shadow-sm"
  >
    <div className="flex items-center gap-2">
      <AlertTriangle className="h-4 w-4 text-amber-400 shrink-0" />
      <span>
        {isTotalFailure ? (
          <>
            <strong>Minden kiválasztott portál hibát jelzett:</strong>{' '}
            {failures.map((p) => p.portalName).join(', ')} nem válaszolt vagy bot-védelmet jelzett. Kérjük, nyisd meg a közvetlen keresőlinkeket a Diagnosztika ablakban!
          </>
        ) : failures.length > 0 ? (
          <>
            <strong>Részleges portálhiba:</strong>{' '}
            {failures.map((p) => p.portalName).join(', ')} nem válaszolt vagy bot-védelmet jelzett. A sikeres források találatai alább láthatók.
          </>
        ) : (
          <>
            <strong>Részleges Ingatlan.com korlátozás:</strong> a Cloudflare WAF csak az 1. oldalt engedte át automatikusan. Több oldalhoz importáld a 2., 3., ... oldalakat a böngésződből, vagy kapcsold be az összes portált!
          </>
        )}
      </span>
    </div>
    <div className="flex flex-wrap items-center gap-2 shrink-0">
      {hasIngatlanComPartial && onOpenImportModal && (
        <button
          type="button"
          data-testid="open-ingatlancom-import-btn"
          onClick={onOpenImportModal}
          className="rounded-lg border border-sky-400/40 bg-sky-500/20 px-2.5 py-1 font-bold text-sky-200 hover:bg-sky-500/30 transition-colors cursor-pointer"
        >
          Ingatlan.com oldalak importálása
        </button>
      )}
      {showEnableAllPortals && onEnableAllPortals && (
        <button
          type="button"
          data-testid="enable-all-portals-rerun-btn"
          onClick={onEnableAllPortals}
          className="rounded-lg border border-emerald-400/40 bg-emerald-500/20 px-2.5 py-1 font-bold text-emerald-200 hover:bg-emerald-500/30 transition-colors cursor-pointer"
        >
          Összes portál bekapcsolása
        </button>
      )}
      <button
        type="button"
        onClick={onOpenDiagnostics}
        className="underline font-bold text-amber-300 hover:text-amber-100 text-left sm:text-right cursor-pointer"
      >
        Diagnosztika megtekintése
      </button>
    </div>
  </div>
)
