import { create } from 'zustand'
import type { IngatlanItem } from '../types/property'

export type DashboardTab = 'listings' | 'analytics'
export type ListingSubView = 'listings' | 'favorites' | 'archive'
export type QuickFilterId =
  | 'bargain'
  | 'onlyNew'
  | 'hidden'
  | 'excludeViewed'
  | 'onlyNewConstruction'
  | 'excludeMultiSource'

export const ALL_QUICK_FILTER_IDS: readonly QuickFilterId[] = [
  'bargain',
  'onlyNew',
  'hidden',
  'excludeViewed',
  'onlyNewConstruction',
  'excludeMultiSource',
]

export const DEFAULT_PINNED_QUICK_FILTERS: QuickFilterId[] = [
  'bargain',
  'onlyNew',
  'hidden',
]

export const MAX_PINNED_QUICK_FILTERS = ALL_QUICK_FILTER_IDS.length

interface UiState {
  theme: 'dark' | 'light'
  viewMode: 'grid' | 'list'
  dashboardTab: DashboardTab
  listingSubView: ListingSubView
  isDiagnosticsModalOpen: boolean
  isFiltersCollapsed: boolean
  starredUrls: string[]
  starredItemsCache: Record<string, IngatlanItem>
  pinnedQuickFilters: QuickFilterId[]
  toggleTheme: () => void
  setViewMode: (mode: 'grid' | 'list') => void
  setDashboardTab: (tab: DashboardTab) => void
  setListingSubView: (subView: ListingSubView) => void
  setDiagnosticsModalOpen: (open: boolean) => void
  setFiltersCollapsed: (collapsed: boolean) => void
  toggleStarred: (itemOrUrl: IngatlanItem | string) => void
  isStarred: (url: string) => boolean
  togglePinnedQuickFilter: (id: QuickFilterId) => void
  setPinnedQuickFilters: (ids: QuickFilterId[]) => void
}

function getInitialTheme(): 'dark' | 'light' {
  if (typeof window === 'undefined') return 'dark'
  const saved = localStorage.getItem('theme')
  if (saved === 'light' || saved === 'dark') return saved
  if (typeof window.matchMedia === 'function') {
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
  }
  return 'dark'
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
  const initialTheme = getInitialTheme()
  if (typeof document !== 'undefined') {
    if (initialTheme === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  return {
    theme: initialTheme,
    viewMode: 'grid',
    dashboardTab: 'listings',
    listingSubView: 'listings',
    isDiagnosticsModalOpen: false,
    isFiltersCollapsed: false,
    starredUrls: getInitialStarredUrls(),
    starredItemsCache: getInitialStarredItemsCache(),
    pinnedQuickFilters: getInitialPinnedQuickFilters(),
    toggleTheme: () =>
      set((state) => {
        const next = state.theme === 'dark' ? 'light' : 'dark'
        if (typeof document !== 'undefined') {
          if (next === 'dark') {
            document.documentElement.classList.add('dark')
          } else {
            document.documentElement.classList.remove('dark')
          }
        }
        localStorage.setItem('theme', next)
        return { theme: next }
      }),
    setViewMode: (mode) => set((state) => (state.viewMode === mode ? state : { viewMode: mode })),
    setDashboardTab: (tab) =>
      set((state) => (state.dashboardTab === tab ? state : { dashboardTab: tab })),
    setListingSubView: (subView) =>
      set((state) =>
        state.listingSubView === subView && state.dashboardTab === 'listings'
          ? state
          : { listingSubView: subView, dashboardTab: 'listings' },
      ),
    setDiagnosticsModalOpen: (open) => set({ isDiagnosticsModalOpen: open }),
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
          // If storage quota exceeded, attempt to prune cached items not in nextUrls or trim oldest
          try {
            const prunedCache: Record<string, IngatlanItem> = {}
            nextUrls.slice(-30).forEach((u) => {
              if (nextCache[u]) prunedCache[u] = nextCache[u]
            })
            localStorage.setItem('starred_urls', JSON.stringify(nextUrls))
            localStorage.setItem('starred_items_cache', JSON.stringify(prunedCache))
          } catch {
            // Still failing: ensure starred_urls alone is safely stored
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
  }
})
