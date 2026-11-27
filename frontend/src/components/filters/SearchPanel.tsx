import type { FC } from 'react'
import { useFilterStore } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { FilterSidebar } from './FilterSidebar'
import { CollapsedFilterSummary } from './CollapsedFilterSummary'
import {
  resolveCitiesSummary,
  resolveTypeLabel,
  type FilterPropertyType,
} from './filterSummaryHelpers'

export {
  CollapsedFilterSummary,
  FilterSidebar,
  type FilterPropertyType,
}

interface SearchPanelProps {
  onSearch: () => void
  isLoading: boolean
}

export const SearchPanel: FC<SearchPanelProps> = ({ onSearch, isLoading }) => {
  const { criteria } = useFilterStore()
  const { isFiltersCollapsed, setFiltersCollapsed } = useUiStore()

  if (isFiltersCollapsed) {
    return (
      <CollapsedFilterSummary
        citiesSummary={resolveCitiesSummary(criteria.cities, criteria.city)}
        typeLabel={resolveTypeLabel(criteria.propertyType)}
        minPriceMillion={criteria.minPriceMillion}
        maxPriceMillion={criteria.maxPriceMillion}
        minArea={criteria.minArea}
        maxArea={criteria.maxArea}
        portalsCount={criteria.portals.length}
        onExpand={() => setFiltersCollapsed(false)}
      />
    )
  }

  return <FilterSidebar onSearch={onSearch} isLoading={isLoading} showCollapseToggle={true} />
}
