import { create } from 'zustand'
import { IngatlanItem } from '../types/property'

export type DashboardTab = 'listings' | 'favorites' | 'archive' | 'analytics'

interface UiState {
  theme: 'dark' | 'light'
  viewMode: 'grid' | 'list'
  dashboardTab: DashboardTab
  isDiagnosticsModalOpen: boolean
  isFiltersCollapsed: boolean
  starredUrls: string[]
  starredItemsCache: Record<string, IngatlanItem>
  toggleTheme: () => void
  setViewMode: (mode: 'grid' | 'list') => void
  setDashboardTab: (tab: DashboardTab) => void
  setDiagnosticsModalOpen: (open: boolean) => void
  setFiltersCollapsed: (collapsed: boolean) => void
  toggleStarred: (itemOrUrl: IngatlanItem | string) => void
  isStarred: (url: string) => boolean
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
    isDiagnosticsModalOpen: false,
    isFiltersCollapsed: false,
    starredUrls: getInitialStarredUrls(),
    starredItemsCache: getInitialStarredItemsCache(),
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
    setViewMode: (mode) => set({ viewMode: mode }),
    setDashboardTab: (tab) => set({ dashboardTab: tab }),
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
          // ignore storage quota errors
        }

        return {
          starredUrls: nextUrls,
          starredItemsCache: nextCache,
        }
      }),
    isStarred: (url) => get().starredUrls.includes(url),
  }
})
