import type { QuickFilterId } from '../../store/useUiStore'

export interface QuickFilterOptionMeta {
  id: QuickFilterId
  label: string
}

export const QUICK_FILTER_OPTIONS: readonly QuickFilterOptionMeta[] = [
  { id: 'bargain', label: 'Alkalmi vétel' },
  { id: 'onlyNew', label: 'Új találatok' },
  { id: 'hidden', label: 'Töröltek' },
  { id: 'excludeViewed', label: 'Megtekintettek' },
  { id: 'onlyNewConstruction', label: 'Új építésű' },
  { id: 'onlyMultiSource', label: 'Összevontak' },
  { id: 'excludeNoImage', label: 'Kép nélküliek' },
]
