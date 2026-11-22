import { create } from 'zustand'
import type { IngatlanItem } from '../types/property'

export type DashboardTab = 'listings' | 'analytics'
export type ListingSubView = 'listings' | 'favorites' | 'archive'
export type WorkspaceTab = 'listings' | 'favorites' | 'archive' | 'analytics'
export type ViewMode = 'grid' | 'list' | 'table'
export type ArchiveSubMode = 'all' | 'hidden' | 'discounted'

export type QuickFilterId =
  | 'bargain'
  | 'onlyNew'
  | 'hidden'
  | 'excludeViewed'
  | 'onlyNewConstruction'
  | 'excludeMultiSource'
  | 'onlyMultiSource'
  | 'excludeNoImage'

export const ALL_QUICK_FILTER_IDS: readonly QuickFilterId[] = [
  'bargain',
  'onlyNew',
  'hidden',
  'excludeViewed',
  'onlyNewConstruction',
  'excludeMultiSource',
  'onlyMultiSource',
  'excludeNoImage',
]

export const DEFAULT_PINNED_QUICK_FILTERS: QuickFilterId[] = [
  'bargain',
  'onlyNew',
  'hidden',
]

export const MAX_PINNED_QUICK_FILTERS = ALL_QUICK_FILTER_IDS.length

interface UiState {
  viewMode: ViewMode
  dashboardTab: DashboardTab
  listingSubView: ListingSubView
  workspaceTab: WorkspaceTab
  archiveSubMode: ArchiveSubMode
  activeInspectItemKey: string | null
  isKpiStripCollapsed: boolean
  isDiagnosticsModalOpen: boolean
  isIngatlanComImportModalOpen: boolean
  isFiltersCollapsed: boolean
  starredUrls: string[]
  starredItemsCache: Record<string, IngatlanItem>
  pinnedQuickFilters: QuickFilterId[]
  selectedCardUrls: string[]
  setViewMode: (mode: ViewMode) => void
  setDashboardTab: (tab: DashboardTab) => void
  setListingSubView: (subView: ListingSubView) => void
  setWorkspaceTab: (tab: WorkspaceTab) => void
  setArchiveSubMode: (mode: ArchiveSubMode) => void
  setActiveInspectItemKey: (key: string | null) => void
  inspectStep: (orderedKeys: readonly string[], direction: 1 | -1) => void
  setKpiStripCollapsed: (collapsed: boolean) => void
  toggleKpiStrip: () => void
  setDiagnosticsModalOpen: (open: boolean) => void
  setIngatlanComImportModalOpen: (open: boolean) => void
  setFiltersCollapsed: (collapsed: boolean) => void
  toggleStarred: (itemOrUrl: IngatlanItem | string) => void
  isStarred: (url: string) => boolean
  togglePinnedQuickFilter: (id: QuickFilterId) => void
  setPinnedQuickFilters: (ids: QuickFilterId[]) => void
  toggleCardSelection: (url: string) => void
  clearCardSelection: () => void
  setSelectedCardUrls: (urls: string[]) => void
}

function getInitialStarredUrls(): string[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem('starred_urls')
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((u): u is string => typeof u === 'string') : []
  } catch {
    return []
  }
}

function getInitialStarredItemsCache(): Record<string, IngatlanItem> {
  if (typeof window === 'undefined') return {}
  try {
    const raw = localStorage.getItem('starred_items_cache')
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function getInitialPinnedQuickFilters(): QuickFilterId[] {
  if (typeof window === 'undefined') return [...DEFAULT_PINNED_QUICK_FILTERS]
  try {
    const raw = localStorage.getItem('pinned_quick_filters')
    if (!raw) return [...DEFAULT_PINNED_QUICK_FILTERS]
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return [...DEFAULT_PINNED_QUICK_FILTERS]
    const valid = parsed.filter(
      (item): item is QuickFilterId =>
        typeof item === 'string' && ALL_QUICK_FILTER_IDS.includes(item as QuickFilterId),
    )
    const unique = Array.from(new Set(valid)).slice(0, MAX_PINNED_QUICK_FILTERS)
    return unique.length > 0 ? unique : [...DEFAULT_PINNED_QUICK_FILTERS]
  } catch {
    return [...DEFAULT_PINNED_QUICK_FILTERS]
  }
}

function savePinnedQuickFilters(ids: QuickFilterId[]): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem('pinned_quick_filters', JSON.stringify(ids))
  } catch {
    // Ignore storage quota failure
  }
}

export const useUiStore = create<UiState>((set, get) => {
  if (typeof document !== 'undefined') {
    document.documentElement.classList.remove('dark')
  }

  return {
    viewMode: 'grid',
    dashboardTab: 'listings',
    listingSubView: 'listings',
    workspaceTab: 'listings',
    archiveSubMode: 'all',
    activeInspectItemKey: null,
    isKpiStripCollapsed: false,
    isDiagnosticsModalOpen: false,
    isIngatlanComImportModalOpen: false,
    isFiltersCollapsed: false,
    starredUrls: getInitialStarredUrls(),
    starredItemsCache: getInitialStarredItemsCache(),
    pinnedQuickFilters: getInitialPinnedQuickFilters(),
    selectedCardUrls: [],
    setViewMode: (mode) => set((state) => (state.viewMode === mode ? state : { viewMode: mode })),
    setDashboardTab: (tab) =>
      set((state) => {
        if (state.dashboardTab === tab) return state
        const nextWorkspace: WorkspaceTab = tab === 'analytics' ? 'analytics' : state.listingSubView
        return { dashboardTab: tab, workspaceTab: nextWorkspace }
      }),
    setListingSubView: (subView) =>
      set((state) =>
        state.listingSubView === subView && state.dashboardTab === 'listings' && state.workspaceTab === subView
          ? state
          : { listingSubView: subView, dashboardTab: 'listings', workspaceTab: subView },
      ),
    setWorkspaceTab: (tab) =>
      set((state) => {
        if (tab === 'analytics') {
          return state.dashboardTab === 'analytics' && state.workspaceTab === 'analytics'
            ? state
            : { dashboardTab: 'analytics', workspaceTab: 'analytics' }
        }
        return state.dashboardTab === 'listings' && state.listingSubView === tab && state.workspaceTab === tab
          ? state
          : { dashboardTab: 'listings', listingSubView: tab, workspaceTab: tab }
      }),
    setArchiveSubMode: (archiveSubMode) => set({ archiveSubMode }),
    setActiveInspectItemKey: (activeInspectItemKey) => set({ activeInspectItemKey }),
    inspectStep: (orderedKeys, direction) =>
      set((state) => {
        if (orderedKeys.length === 0) return state
        const currentKey = state.activeInspectItemKey
        const currentIndex = currentKey ? orderedKeys.indexOf(currentKey) : -1
        if (currentIndex === -1) {
          return { activeInspectItemKey: orderedKeys[0] ?? null }
        }
        const nextIndex = Math.max(0, Math.min(orderedKeys.length - 1, currentIndex + direction))
        const nextKey = orderedKeys[nextIndex] ?? currentKey
        return nextKey === currentKey ? state : { activeInspectItemKey: nextKey }
      }),
    setKpiStripCollapsed: (isKpiStripCollapsed) => set({ isKpiStripCollapsed }),
    toggleKpiStrip: () => set((state) => ({ isKpiStripCollapsed: !state.isKpiStripCollapsed })),
    setDiagnosticsModalOpen: (open) => set({ isDiagnosticsModalOpen: open }),
    setIngatlanComImportModalOpen: (open) => set({ isIngatlanComImportModalOpen: open }),
    setFiltersCollapsed: (collapsed) => set({ isFiltersCollapsed: collapsed }),
    toggleStarred: (itemOrUrl) =>
      set((state) => {
        const url = typeof itemOrUrl === 'string' ? itemOrUrl : itemOrUrl.url
        const item = typeof itemOrUrl === 'string' ? undefined : itemOrUrl
        if (!url) return state

        const exists = state.starredUrls.includes(url)
        const nextUrls = exists
          ? state.starredUrls.filter((u) => u !== url)
          : [...state.starredUrls, url]
        const nextCache = { ...state.starredItemsCache }
        if (exists) {
          delete nextCache[url]
        } else if (item) {
          nextCache[url] = item
        }

        try {
          localStorage.setItem('starred_urls', JSON.stringify(nextUrls))
          localStorage.setItem('starred_items_cache', JSON.stringify(nextCache))
        } catch {
          try {
            const prunedCache: Record<string, IngatlanItem> = {}
            nextUrls.slice(-30).forEach((u) => {
              if (nextCache[u]) prunedCache[u] = nextCache[u]
            })
            localStorage.setItem('starred_urls', JSON.stringify(nextUrls))
            localStorage.setItem('starred_items_cache', JSON.stringify(prunedCache))
          } catch {
            try {
              localStorage.setItem('starred_urls', JSON.stringify(nextUrls))
            } catch {
              // Ignore critical storage quota failure
            }
          }
        }

        return {
          starredUrls: nextUrls,
          starredItemsCache: nextCache,
        }
      }),
    isStarred: (url) => get().starredUrls.includes(url),
    togglePinnedQuickFilter: (id) =>
      set((state) => {
        if (!ALL_QUICK_FILTER_IDS.includes(id)) return state
        const exists = state.pinnedQuickFilters.includes(id)
        if (!exists && state.pinnedQuickFilters.length >= MAX_PINNED_QUICK_FILTERS) {
          return state
        }
        const next = exists
          ? state.pinnedQuickFilters.filter((item) => item !== id)
          : [...state.pinnedQuickFilters, id]
        savePinnedQuickFilters(next)
        return { pinnedQuickFilters: next }
      }),
    setPinnedQuickFilters: (ids) =>
      set(() => {
        const valid = ids.filter((id) => ALL_QUICK_FILTER_IDS.includes(id))
        const next = Array.from(new Set(valid)).slice(0, MAX_PINNED_QUICK_FILTERS)
        savePinnedQuickFilters(next)
        return { pinnedQuickFilters: next }
      }),
    toggleCardSelection: (url) =>
      set((state) => {
        if (!url) return state
        const exists = state.selectedCardUrls.includes(url)
        return {
          selectedCardUrls: exists
            ? state.selectedCardUrls.filter((u) => u !== url)
            : [...state.selectedCardUrls, url],
        }
      }),
    clearCardSelection: () => set({ selectedCardUrls: [] }),
    setSelectedCardUrls: (urls) =>
      set({ selectedCardUrls: Array.from(new Set(urls.filter(Boolean))) }),
  }
})
