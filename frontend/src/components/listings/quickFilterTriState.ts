import type { QuickFilterId } from '../../store/useUiStore'
import { useFilterStore } from '../../store/useFilterStore'

export type TriStateMode = 'all' | 'only' | 'exclude'

interface TriStateConfig {
  onlyLabel: string
  excludeLabel: string
  allLabel?: string
  readMode: () => TriStateMode
  applyMode: (mode: TriStateMode) => void
}

const TRI_STATE_MAP: Record<QuickFilterId, TriStateConfig> = {
  bargain: {
    onlyLabel: 'Csak alkalmi vétel',
    excludeLabel: 'Nem alkalmi',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyBargain) return 'only'
      if (s.excludeBargain) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyBargain(true)
      else if (mode === 'exclude') s.setExcludeBargain(true)
      else {
        s.setOnlyBargain(false)
        s.setExcludeBargain(false)
      }
    },
  },
  onlyNew: {
    onlyLabel: 'Csak új találatok',
    excludeLabel: 'Nem új találatok',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyNew) return 'only'
      if (s.excludeNew) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyNew(true)
      else if (mode === 'exclude') s.setExcludeNew(true)
      else {
        s.setOnlyNew(false)
        s.setExcludeNew(false)
      }
    },
  },
  hidden: {
    onlyLabel: 'Csak töröltek',
    excludeLabel: 'Töröltek nélkül',
    allLabel: 'Töröltek is',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.showHidden) return 'only'
      if (s.includeHidden) return 'all'
      return 'exclude'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setShowHidden(true)
      else if (mode === 'all') s.setIncludeHidden(true)
      else {
        s.setShowHidden(false)
        s.setIncludeHidden(false)
      }
    },
  },
  excludeViewed: {
    onlyLabel: 'Csak megtekintettek',
    excludeLabel: 'Megtekintettek nélkül',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyViewed) return 'only'
      if (s.criteria.excludeViewed) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyViewed(true)
      else if (mode === 'exclude') s.setExcludeViewed(true)
      else {
        s.setOnlyViewed(false)
        s.setExcludeViewed(false)
      }
    },
  },
  onlyNewConstruction: {
    onlyLabel: 'Csak új építésű',
    excludeLabel: 'Nem új építésű',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.criteria.onlyNewConstruction) return 'only'
      if (s.excludeNewConstruction) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyNewConstruction(true)
      else if (mode === 'exclude') s.setExcludeNewConstruction(true)
      else {
        s.setOnlyNewConstruction(false)
        s.setExcludeNewConstruction(false)
      }
    },
  },
  excludeMultiSource: {
    onlyLabel: 'Csak összevontak',
    excludeLabel: 'Összevontak nélkül',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyMultiSource) return 'only'
      if (s.excludeMultiSource) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyMultiSource(true)
      else if (mode === 'exclude') s.setExcludeMultiSource(true)
      else {
        s.setOnlyMultiSource(false)
        s.setExcludeMultiSource(false)
      }
    },
  },
  onlyMultiSource: {
    onlyLabel: 'Csak összevontak',
    excludeLabel: 'Összevontak nélkül',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyMultiSource) return 'only'
      if (s.excludeMultiSource) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyMultiSource(true)
      else if (mode === 'exclude') s.setExcludeMultiSource(true)
      else {
        s.setOnlyMultiSource(false)
        s.setExcludeMultiSource(false)
      }
    },
  },
  excludeNoImage: {
    onlyLabel: 'Csak kép nélküliek',
    excludeLabel: 'Kép nélküliek nélkül',
    readMode: () => {
      const s = useFilterStore.getState()
      if (s.onlyNoImage) return 'only'
      if (s.excludeNoImage) return 'exclude'
      return 'all'
    },
    applyMode: (mode) => {
      const s = useFilterStore.getState()
      if (mode === 'only') s.setOnlyNoImage(true)
      else if (mode === 'exclude') s.setExcludeNoImage(true)
      else {
        s.setOnlyNoImage(false)
        s.setExcludeNoImage(false)
      }
    },
  },
}

export function getTriStateMode(id: QuickFilterId): TriStateMode {
  return TRI_STATE_MAP[id].readMode()
}

export function setTriStateMode(id: QuickFilterId, mode: TriStateMode): void {
  TRI_STATE_MAP[id].applyMode(mode)
}

export function cycleTriStateMode(id: QuickFilterId): void {
  const current = getTriStateMode(id)
  const next: TriStateMode = current === 'all' ? 'only' : current === 'only' ? 'exclude' : 'all'
  setTriStateMode(id, next)
}

export function resolveTriStateChipLabel(
  id: QuickFilterId,
  mode: TriStateMode,
  defaultLabel: string,
): string {
  const cfg = TRI_STATE_MAP[id]
  if (mode === 'only') return cfg.onlyLabel
  if (mode === 'exclude' && id !== 'hidden') return cfg.excludeLabel
  if (mode === 'all' && id === 'hidden' && cfg.allLabel) return cfg.allLabel
  return defaultLabel
}
