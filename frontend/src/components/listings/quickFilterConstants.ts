import type { QuickFilterId } from '../../store/useUiStore'

export interface QuickFilterOptionMeta {
  id: QuickFilterId
  label: string
}

export const QUICK_FILTER_OPTIONS: readonly QuickFilterOptionMeta[] = [
  { id: 'bargain', label: 'Alkalmi vétel' },
  { id: 'onlyNew', label: 'Csak új találatok' },
  { id: 'hidden', label: 'Elrejtettek' },
  { id: 'excludeViewed', label: 'Megtekintettek rejtése' },
  { id: 'onlyNewConstruction', label: 'Csak új építésű' },
  { id: 'excludeMultiSource', label: 'Több forrásosak rejtése' },
]
