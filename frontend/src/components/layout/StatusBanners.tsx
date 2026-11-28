import type { FC } from 'react'
import { AlertCircle, AlertTriangle, Sparkles } from 'lucide-react'
import type { PortalStatusDto } from '../../types/portal'

interface SearchErrorBannerProps {
  error: unknown
  onRetry: () => void
}

export const SearchErrorBanner: FC<SearchErrorBannerProps> = ({ error, onRetry }) => (
  <div
    role="alert"
    data-testid="search-error-banner"
    className="flex items-center justify-between gap-3 rounded-2xl border border-rose-500/30 glass-surface bg-[var(--status-danger-bg)] px-4 py-3 text-xs text-[var(--status-danger-fg)]"
  >
    <div className="flex items-center gap-2.5">
      <AlertCircle className="h-5 w-5 text-[var(--status-danger-fg)] shrink-0" />
      <div>
        <p className="font-bold text-[var(--status-danger-fg)]">Hiba történt a keresés során</p>
        <p className="text-[11px] opacity-90">
          {error instanceof Error ? error.message : 'Nem sikerült lekérni a hirdetéseket a szervertől. Kérjük, próbáld újra!'}
        </p>
      </div>
    </div>
    <button
      type="button"
      onClick={onRetry}
      className="rounded-xl bg-rose-600 px-3.5 py-1.5 font-bold text-white hover:bg-rose-500 transition-colors shrink-0 cursor-pointer"
    >
      Újrapróbálás
    </button>
  </div>
)

interface PartialPortalWarningBannerProps {
  failures: PortalStatusDto[]
  isTotalFailure?: boolean
  hasIngatlanComPartial?: boolean
  ingatlanComMessage?: string | null
  showEnableAllPortals?: boolean
  onOpenDiagnostics: () => void
  onOpenImportModal?: () => void
  onEnableAllPortals?: () => void
}

export const PartialPortalWarningBanner: FC<PartialPortalWarningBannerProps> = ({
  failures,
  isTotalFailure = false,
  hasIngatlanComPartial = false,
  ingatlanComMessage = null,
  showEnableAllPortals = false,
  onOpenDiagnostics,
  onOpenImportModal,
  onEnableAllPortals,
}) => (
  <div
    role="status"
    data-testid="partial-portal-warning-banner"
    data-total-failure={isTotalFailure ? 'true' : 'false'}
    className="flex items-center justify-between gap-3 rounded-2xl border border-amber-500/35 glass-surface bg-[var(--status-warn-bg)] px-4 py-2.5 text-xs text-[var(--status-warn-fg)]"
  >
    <div className="flex items-center gap-2 min-w-0">
      <AlertTriangle className="h-4 w-4 text-[var(--status-warn-fg)] shrink-0" />
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
            <strong>Részleges Ingatlan.com korlátozás:</strong>{' '}
            {ingatlanComMessage ||
              'a Cloudflare WAF korlátozta a további oldalakat.'}{' '}
            Több oldalhoz importáld a következő oldalakat a böngésződből, vagy kapcsold be az összes portált!
          </>
        )}
      </span>
    </div>
    <div className="flex items-center gap-2 shrink-0">
      {hasIngatlanComPartial && onOpenImportModal && (
        <button
          type="button"
          data-testid="open-ingatlancom-import-btn"
          onClick={onOpenImportModal}
          className="rounded-xl border border-[#0071e3]/30 bg-[var(--status-info-bg)] px-2.5 py-1 font-bold text-[var(--status-info-fg)] hover:opacity-90 transition-colors cursor-pointer"
        >
          Ingatlan.com oldalak importálása
        </button>
      )}
      {showEnableAllPortals && onEnableAllPortals && (
        <button
          type="button"
          data-testid="enable-all-portals-rerun-btn"
          onClick={onEnableAllPortals}
          className="rounded-xl border border-emerald-500/35 bg-[var(--status-bargain-bg)] px-2.5 py-1 font-bold text-[var(--status-bargain-fg)] hover:opacity-90 transition-colors cursor-pointer"
        >
          Összes portál bekapcsolása
        </button>
      )}
      <button
        type="button"
        onClick={onOpenDiagnostics}
        className="underline font-bold text-[var(--status-warn-fg)] hover:opacity-80 text-right cursor-pointer"
      >
        Diagnosztika megtekintése
      </button>
    </div>
  </div>
)

interface NewCitiesInitializedBannerProps {
  cities?: string[] | null
}

export const NewCitiesInitializedBanner: FC<NewCitiesInitializedBannerProps> = ({ cities }) => {
  if (!cities || cities.length === 0) return null
  return (
    <div
      role="status"
      data-testid="new-cities-initialized-banner"
      className="flex items-center gap-2.5 rounded-2xl border border-[#0071e3]/30 glass-surface bg-[var(--status-info-bg)] px-4 py-2.5 text-xs text-[var(--status-info-fg)]"
    >
      <Sparkles className="h-4 w-4 shrink-0 text-[var(--status-info-fg)]" aria-hidden="true" />
      <span>
        <strong>Új település alapállapot rögzítve ({cities.join(', ')}):</strong>{' '}
        a következő kereséstől automatikusan jelöljük az újonnan megjelenő hirdetéseket.
      </span>
    </div>
  )
}
