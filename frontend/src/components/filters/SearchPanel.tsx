import type { FC, FormEvent } from 'react'
import { Search, Loader2, ChevronUp, ChevronDown, SlidersHorizontal } from 'lucide-react'
import { useFilterStore, DEFAULT_PORTALS } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { CitySelector } from './CitySelector'
import { PropertyTypeTabs } from './PropertyTypeTabs'
import { PriceStepper } from './PriceStepper'
import { AreaStepper } from './AreaStepper'
import { PortalToggleGroup } from './PortalToggleGroup'
import { QuickToggles } from './QuickToggles'

interface SearchPanelProps {
  onSearch: () => void
  isLoading: boolean
}

const PROPERTY_TYPE_LABELS: Record<string, string> = {
  lakas: 'Lakás',
  haz: 'Családi ház',
  telek: 'Telek',
}

export const SearchPanel: FC<SearchPanelProps> = ({ onSearch, isLoading }) => {
  const {
    criteria,
    onlyNew,
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
    setAutoOpen,
    setOnlyNewConstruction,
    setOnlyNew,
  } = useFilterStore()

  const { isFiltersCollapsed, setFiltersCollapsed } = useUiStore()

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    onSearch()
  }

  const citiesSummary =
    criteria.cities && criteria.cities.length > 0 ? criteria.cities.join(', ') : criteria.city || 'Debrecen'
  const typeLabel =
    PROPERTY_TYPE_LABELS[(criteria.propertyType || 'lakas').toLowerCase()] || criteria.propertyType

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-xl transition-all duration-200 sm:p-5"
    >
      {/* Panel Header */}
      <div className="flex items-center justify-between border-b border-[var(--border-color)] pb-3">
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-blue-400" />
          <h2 className="text-sm font-bold tracking-tight text-[var(--text-primary)]">
            Keresési Paraméterek & Szűrők
          </h2>
        </div>
        <button
          type="button"
          onClick={() => setFiltersCollapsed(!isFiltersCollapsed)}
          className="flex items-center gap-1 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
        >
          <span>{isFiltersCollapsed ? 'Kibontás' : 'Összecsukás'}</span>
          {isFiltersCollapsed ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
        </button>
      </div>

      {/* Collapsed summary bar (#157) */}
      {isFiltersCollapsed && (
        <div
          data-testid="collapsed-filter-summary"
          className="flex flex-wrap items-center gap-2 pt-3 text-xs text-[var(--text-secondary)]"
        >
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 font-semibold text-[var(--text-primary)]">
            <span>📍</span>
            <span>{citiesSummary}</span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 font-medium">
            <span>🏠</span>
            <span>{typeLabel}</span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 font-medium">
            <span>💰</span>
            <span>
              {criteria.minPriceMillion} - {criteria.maxPriceMillion} M Ft
            </span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 font-medium">
            <span>📐</span>
            <span>
              {criteria.minArea} - {criteria.maxArea} m²
            </span>
          </span>
          <span className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1 font-medium">
            <span>🌐</span>
            <span>
              {criteria.portals.length}/{DEFAULT_PORTALS.length} portál
            </span>
          </span>
          {criteria.onlyNewConstruction && (
            <span className="inline-flex items-center gap-1 rounded-lg border border-cyan-500/40 bg-cyan-500/15 px-2.5 py-1 font-semibold text-cyan-400">
              <span>🏗️ Új építésű</span>
            </span>
          )}
        </div>
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
            value={criteria.propertyType}
            onChange={setPropertyType}
          />

          {/* Steppers grid */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <PriceStepper
              label="Minimális ár"
              value={criteria.minPriceMillion}
              onChange={setMinPriceMillion}
              min={0}
              max={criteria.maxPriceMillion - 1}
              step={1}
            />
            <PriceStepper
              label="Maximális ár"
              value={criteria.maxPriceMillion}
              onChange={setMaxPriceMillion}
              min={criteria.minPriceMillion + 1}
              max={1000}
              step={1}
            />
            <AreaStepper
              label="Minimális alapterület"
              value={criteria.minArea}
              onChange={setMinArea}
              min={1}
              max={criteria.maxArea - 1}
              step={1}
            />
            <AreaStepper
              label="Maximális alapterület"
              value={criteria.maxArea}
              onChange={setMaxArea}
              min={criteria.minArea + 1}
              max={1000}
              step={1}
            />
          </div>

          {/* Portal toggles */}
          <PortalToggleGroup
            selectedPortals={criteria.portals}
            onToggle={togglePortal}
            onSelectAll={setAllPortals}
          />

          {/* Search-specific Quick toggles */}
          <QuickToggles
            onlyNew={onlyNew}
            onToggleOnlyNew={setOnlyNew}
            onlyNewConstruction={Boolean(criteria.onlyNewConstruction)}
            onToggleOnlyNewConstruction={setOnlyNewConstruction}
            autoOpen={criteria.autoOpenBrowser}
            onToggleAutoOpen={setAutoOpen}
          />
        </div>
      )}

      {/* Submit button bar */}
      <div className="mt-4 flex items-center justify-end gap-3 border-t border-[var(--border-color)] pt-3">
        <button
          type="submit"
          disabled={isLoading || criteria.portals.length === 0}
          className="flex h-11 min-w-[200px] items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-6 font-bold text-white shadow-lg shadow-blue-500/25 transition-all duration-150 hover:from-blue-500 hover:to-indigo-500 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
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
  )
}
