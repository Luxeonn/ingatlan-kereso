import type { FC, FormEvent } from 'react'
import {
  Search,
  Loader2,
  ChevronUp,
  ChevronDown,
  SlidersHorizontal,
  AlertTriangle,
  MapPin,
  Home,
  Coins,
  Ruler,
  Globe,
} from 'lucide-react'
import type { CanonicalPropertyType } from '../../types/property'
import { useFilterStore, DEFAULT_PORTALS, computeCriteriaHash } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { CitySelector } from './CitySelector'
import { PropertyTypeTabs } from './PropertyTypeTabs'
import { PriceStepper } from './PriceStepper'
import { AreaStepper } from './AreaStepper'
import { PortalToggleGroup } from './PortalToggleGroup'
import { SearchPresetsBar } from './SearchPresetsBar'

interface SearchPanelProps {
  onSearch: () => void
  isLoading: boolean
}

export type FilterPropertyType = CanonicalPropertyType | 'lakas' | 'haz' | 'telek'

const PROPERTY_TYPE_LABELS: Record<FilterPropertyType, string> = {
  HAZ: 'Családi ház',
  LAKAS: 'Lakás',
  TELEK: 'Telek',
  GARAZS: 'Garázs',
  IPARI_KERESKEDELMI: 'Ipari / Kereskedelmi',
  EGYEB_ISMERETLEN: 'Egyéb',
  haz: 'Családi ház',
  lakas: 'Lakás',
  telek: 'Telek',
}

function getValidationWarnings(minPrice: number, maxPrice: number, minArea: number, maxArea: number): string[] {
  const warnings: string[] = []
  if (minPrice < 0 || maxPrice < 0) {
    warnings.push('Az ár nem lehet negatív.')
  } else if (minPrice > maxPrice) {
    warnings.push(
      `A minimális ár (${minPrice} M Ft) nem lehet nagyobb a maximális árnál (${maxPrice} M Ft).`,
    )
  }

  if (minArea < 1 || maxArea < 1) {
    warnings.push('Az alapterületnek legalább 1 m²-nek kell lennie.')
  } else if (minArea > maxArea) {
    warnings.push(
      `A minimális alapterület (${minArea} m²) nem lehet nagyobb a maximális alapterületnél (${maxArea} m²).`,
    )
  }
  return warnings
}

function resolveCitiesSummary(cities?: string[], defaultCity?: string): string {
  if (cities && cities.length > 0) return cities.join(', ')
  return defaultCity || 'Hajdúszoboszló'
}

function resolveTypeLabel(propertyType?: string): string {
  const key = (propertyType || 'haz').toLowerCase()
  if (Object.prototype.hasOwnProperty.call(PROPERTY_TYPE_LABELS, key)) {
    return PROPERTY_TYPE_LABELS[key as FilterPropertyType]
  }
  return propertyType || 'Családi ház'
}

interface CollapsedFilterSummaryProps {
  citiesSummary: string
  typeLabel: string
  minPriceMillion: number
  maxPriceMillion: number
  minArea: number
  maxArea: number
  portalsCount: number
  onExpand: () => void
}

const CollapsedFilterSummary: FC<CollapsedFilterSummaryProps> = ({
  citiesSummary,
  typeLabel,
  minPriceMillion,
  maxPriceMillion,
  minArea,
  maxArea,
  portalsCount,
  onExpand,
}) => (
  <div
    data-testid="collapsed-filter-summary"
    className="flex flex-wrap items-center gap-2 pt-3 text-xs text-[var(--text-secondary)] tabular-nums"
  >
    <button
      type="button"
      onClick={onExpand}
      className="inline-flex items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-3 py-1.5 font-semibold text-[var(--text-primary)] hover:border-blue-500/40 transition-colors cursor-pointer"
    >
      <MapPin className="h-3.5 w-3.5 text-blue-400" />
      <span>{citiesSummary}</span>
    </button>
    <button
      type="button"
      onClick={onExpand}
      className="inline-flex items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-3 py-1.5 font-medium hover:border-blue-500/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
    >
      <Home className="h-3.5 w-3.5 text-amber-400" />
      <span>{typeLabel}</span>
    </button>
    <button
      type="button"
      onClick={onExpand}
      className="inline-flex items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-3 py-1.5 font-medium hover:border-blue-500/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
    >
      <Coins className="h-3.5 w-3.5 text-emerald-400" />
      <span>
        {minPriceMillion} - {maxPriceMillion} M Ft
      </span>
    </button>
    <button
      type="button"
      onClick={onExpand}
      className="inline-flex items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-3 py-1.5 font-medium hover:border-blue-500/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
    >
      <Ruler className="h-3.5 w-3.5 text-purple-400" />
      <span>
        {minArea} - {maxArea} m²
      </span>
    </button>
    <button
      type="button"
      onClick={onExpand}
      className="inline-flex items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] px-3 py-1.5 font-medium hover:border-blue-500/40 hover:text-[var(--text-primary)] transition-colors cursor-pointer"
    >
      <Globe className="h-3.5 w-3.5 text-indigo-400" />
      <span>
        {portalsCount}/{DEFAULT_PORTALS.length} portál
      </span>
    </button>
  </div>
)

export const SearchPanel: FC<SearchPanelProps> = ({ onSearch, isLoading }) => {
  const {
    criteria,
    lastExecutedCriteriaHash,
    addCity,
    removeCity,
    setCities,
    setPropertyType,
    setMinPriceMillion,
    setMaxPriceMillion,
    setMinArea,
    setMaxArea,
    togglePortal,
    setAllPortals,
  } = useFilterStore()

  const { isFiltersCollapsed, setFiltersCollapsed } = useUiStore()

  const validationWarnings = getValidationWarnings(
    criteria.minPriceMillion,
    criteria.maxPriceMillion,
    criteria.minArea,
    criteria.maxArea,
  )

  const hasValidationErrors = validationWarnings.length > 0
  const isCriteriaDirty =
    lastExecutedCriteriaHash !== null && computeCriteriaHash(criteria) !== lastExecutedCriteriaHash

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (hasValidationErrors) return
    onSearch()
  }

  const citiesSummary = resolveCitiesSummary(criteria.cities, criteria.city)
  const typeLabel = resolveTypeLabel(criteria.propertyType)

  return (
    <div
      data-testid="search-panel-container"
      className="rounded-2xl border border-[var(--border-hairline)] bg-[var(--bg-surface)] p-4 shadow-[0_12px_36px_-12px_rgba(2,6,23,0.3)] transition-colors duration-200 sm:p-5"
    >
      {/* Panel Header */}
      <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-3">
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-blue-400" />
          <h2 className="text-sm font-bold tracking-tight text-[var(--text-primary)]">
            Keresési Paraméterek & Szűrők
          </h2>
          {isCriteriaDirty && (
            <span
              data-testid="dirty-filter-indicator"
              className="inline-flex items-center gap-1 rounded-full bg-[var(--status-warn-bg)] border border-amber-500/30 px-2 py-0.5 text-[10px] font-bold text-[var(--status-warn-fg)]"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-[var(--status-warn-fg)]" />
              <span>Szűrők módosultak</span>
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={() => setFiltersCollapsed(!isFiltersCollapsed)}
          className="flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 py-1 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer"
        >
          <span>{isFiltersCollapsed ? 'Kibontás' : 'Összecsukás'}</span>
          {isFiltersCollapsed ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
        </button>
      </div>

      {/* Quick Search Presets Bar (outside form to avoid invalid HTML nested forms) */}
      <SearchPresetsBar onSearch={onSearch} />

      <form onSubmit={handleSubmit} noValidate>
        {/* Collapsed floating summary bar (#157) */}
        {isFiltersCollapsed && (
          <CollapsedFilterSummary
            citiesSummary={citiesSummary}
            typeLabel={typeLabel}
            minPriceMillion={criteria.minPriceMillion}
            maxPriceMillion={criteria.maxPriceMillion}
            minArea={criteria.minArea}
            maxArea={criteria.maxArea}
            portalsCount={criteria.portals.length}
            onExpand={() => setFiltersCollapsed(false)}
          />
        )}

        {/* Main filters content */}
        {!isFiltersCollapsed && (
          <div className="space-y-4 pt-4">
            {/* City selector */}
            <CitySelector
              selectedCities={criteria.cities}
              onAddCity={addCity}
              onRemoveCity={removeCity}
              onSetCities={setCities}
            />

            {/* Property type tabs */}
            <PropertyTypeTabs
              value={criteria.propertyType || 'haz'}
              onChange={setPropertyType}
            />

            {/* Steppers grid */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <PriceStepper
                label="Minimális ár"
                value={criteria.minPriceMillion}
                onChange={setMinPriceMillion}
                min={0}
                step={1}
              />
              <PriceStepper
                label="Maximális ár"
                value={criteria.maxPriceMillion}
                onChange={setMaxPriceMillion}
                min={0}
                step={1}
              />
              <AreaStepper
                label="Minimális alapterület"
                value={criteria.minArea}
                onChange={setMinArea}
                min={1}
                step={1}
              />
              <AreaStepper
                label="Maximális alapterület"
                value={criteria.maxArea}
                onChange={setMaxArea}
                min={1}
                step={1}
              />
            </div>

            {/* Portal toggles */}
            <PortalToggleGroup
              selectedPortals={criteria.portals}
              onToggle={togglePortal}
              onSelectAll={setAllPortals}
            />
          </div>
        )}

        {/* Submit button bar */}
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-[var(--border-hairline)] pt-3">
          {hasValidationErrors ? (
            <div
              role="alert"
              data-testid="search-validation-warning"
              className="flex items-center gap-2 rounded-lg border border-amber-500/40 bg-[var(--status-warn-bg)] px-3 py-2 text-xs font-semibold text-[var(--status-warn-fg)]"
            >
              <AlertTriangle className="h-4 w-4 shrink-0 text-[var(--status-warn-fg)]" />
              <span>{validationWarnings.join(' ')}</span>
            </div>
          ) : (
            <div />
          )}

          <button
            type="submit"
            disabled={isLoading || criteria.portals.length === 0 || hasValidationErrors}
            className="flex h-11 min-w-[200px] items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-6 font-bold text-white shadow-lg shadow-blue-500/25 transition-all duration-150 hover:from-blue-500 hover:to-indigo-500 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Keresés folyamatban...</span>
              </>
            ) : (
              <>
                <Search className="h-4 w-4" />
                <span>Ingatlanok Keresése</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
