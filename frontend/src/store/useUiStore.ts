import { create } from 'zustand'
import type { IngatlanItem } from '../types/property'

export type DashboardTab = 'listings' | 'analytics'
export type ListingSubView = 'listings' | 'favorites' | 'archive'
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
  archiveSubMode: ArchiveSubMode
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
  setArchiveSubMode: (mode: ArchiveSubMode) => void
  setDiagnosticsModalOpen: (open: boolean) => void
  setIngatlanComImportModalOpen: (open: boolean) => void
  setFiltersCollapsed: (collapsed: boolean) => void
  toggleStarred: (itemOrUrl: IngatlanItem | string) => void
  isStarred: (url: string) => boolean
  togglePinnedQuickFilter: (id: QuickFilterId) => void
  toggleCardSelection: (url: string) => void
  clearCardSelection: () => void
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
    archiveSubMode: 'all',
    isDiagnosticsModalOpen: false,
    isIngatlanComImportModalOpen: false,
    isFiltersCollapsed: false,
    starredUrls: getInitialStarredUrls(),
    starredItemsCache: getInitialStarredItemsCache(),
    pinnedQuickFilters: getInitialPinnedQuickFilters(),
    selectedCardUrls: [],
    setViewMode: (mode) => set((state) => (state.viewMode === mode ? state : { viewMode: mode })),
    setDashboardTab: (tab) =>
      set((state) => (state.dashboardTab === tab ? state : { dashboardTab: tab })),
    setListingSubView: (subView) =>
      set((state) =>
        state.listingSubView === subView && state.dashboardTab === 'listings'
          ? state
          : { listingSubView: subView, dashboardTab: 'listings' },
      ),
    setArchiveSubMode: (archiveSubMode) => set({ archiveSubMode }),
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
  }
})
