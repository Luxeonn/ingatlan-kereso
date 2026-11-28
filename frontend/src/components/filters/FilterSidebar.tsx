import type { FC, FormEvent } from 'react'
import {
  Search,
  Loader2,
  ChevronUp,
  SlidersHorizontal,
  AlertTriangle,
} from 'lucide-react'
import { useFilterStore, computeCriteriaHash } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { CitySelector } from './CitySelector'
import { PropertyTypeTabs } from './PropertyTypeTabs'
import { PriceStepper } from './PriceStepper'
import { AreaStepper } from './AreaStepper'
import { PortalToggleGroup } from './PortalToggleGroup'
import { SearchPresetsBar } from './SearchPresetsBar'

export interface FilterSidebarProps {
  onSearch: () => void
  isLoading: boolean
  showCollapseToggle?: boolean
}

function getValidationWarnings(
  minPrice: number,
  maxPrice: number,
  minArea: number,
  maxArea: number,
): string[] {
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

export const FilterSidebar: FC<FilterSidebarProps> = ({
  onSearch,
  isLoading,
  showCollapseToggle = true,
}) => {
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

  return (
    <aside
      data-testid="search-panel-container"
      aria-label="Keresési Paraméterek és Szűrők"
      className="sticky top-[76px] z-20 flex max-h-[calc(100vh-96px)] w-[304px] shrink-0 flex-col overflow-hidden rounded-2xl glass-chrome"
    >
      {/* Part 1: Fixed Header + Search Presets (outside form to prevent nested forms) */}
      <div className="shrink-0 border-b border-[var(--border-hairline)] px-3.5 py-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <SlidersHorizontal className="h-4 w-4 shrink-0 text-[#0071e3]" />
            <h2 className="truncate text-xs font-extrabold tracking-tight text-[var(--text-primary)]">
              Keresési Paraméterek & Szűrők
            </h2>
          </div>

          {showCollapseToggle && (
            <button
              type="button"
              aria-expanded={!isFiltersCollapsed}
              onClick={() => setFiltersCollapsed(true)}
              className="flex shrink-0 items-center gap-1 rounded-full border border-[var(--border-hairline)] bg-white/75 px-2.5 py-1 text-[11px] font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer"
            >
              <span>Összecsukás</span>
              <ChevronUp className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        {isCriteriaDirty && (
          <div className="mt-2">
            <span
              data-testid="dirty-filter-indicator"
              className="inline-flex items-center gap-1.5 rounded-full bg-[var(--status-warn-bg)] border border-amber-500/30 px-2.5 py-0.5 text-[10px] font-bold text-[var(--status-warn-fg)]"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-[var(--status-warn-fg)]" />
              <span>Szűrők módosultak</span>
            </span>
          </div>
        )}

        <SearchPresetsBar onSearch={onSearch} />
      </div>

      {/* Parts 2 & 3: Scrollable Filter Controls + Sticky Bottom CTA */}
      <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
        <div className="min-h-0 flex-1 overflow-y-auto space-y-3 px-3.5 py-3">
          <div className="rounded-xl border border-[var(--border-hairline)] bg-white/55 p-3">
            <CitySelector
              selectedCities={criteria.cities}
              onAddCity={addCity}
              onRemoveCity={removeCity}
              onSetCities={setCities}
            />
          </div>

          <div className="rounded-xl border border-[var(--border-hairline)] bg-white/55 p-3">
            <PropertyTypeTabs
              value={criteria.propertyType || 'haz'}
              onChange={setPropertyType}
            />
          </div>

          <div className="rounded-xl border border-[var(--border-hairline)] bg-white/55 p-3">
            <div className="grid grid-cols-2 gap-2.5">
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
          </div>

          <div className="rounded-xl border border-[var(--border-hairline)] bg-white/55 p-3">
            <PortalToggleGroup
              selectedPortals={criteria.portals}
              onToggle={togglePortal}
              onSelectAll={setAllPortals}
            />
          </div>
        </div>

        {/* Part 3: Pinned Bottom CTA */}
        <div className="shrink-0 space-y-2 border-t border-[var(--border-hairline)] bg-white/60 px-3.5 py-3">
          {hasValidationErrors && (
            <div
              role="alert"
              data-testid="search-validation-warning"
              className="flex items-start gap-2 rounded-xl border border-amber-500/40 bg-[var(--status-warn-bg)] px-3 py-2 text-[11px] font-semibold text-[var(--status-warn-fg)]"
            >
              <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--status-warn-fg)]" />
              <span>{validationWarnings.join(' ')}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading || criteria.portals.length === 0 || hasValidationErrors}
            className="flex h-10 w-full items-center justify-center gap-2 rounded-xl bg-[#0071e3] px-4 text-xs font-bold text-white shadow-sm transition-all duration-150 hover:bg-[#0077ed] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
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
    </aside>
  )
}
