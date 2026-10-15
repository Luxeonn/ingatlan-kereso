import { create } from 'zustand'

interface UiState {
  theme: 'dark' | 'light'
  viewMode: 'grid' | 'list'
  isLogModalOpen: boolean
  isDiagnosticsModalOpen: boolean
  isFiltersCollapsed: boolean
  toggleTheme: () => void
  setViewMode: (mode: 'grid' | 'list') => void
  setLogModalOpen: (open: boolean) => void
  setDiagnosticsModalOpen: (open: boolean) => void
  setFiltersCollapsed: (collapsed: boolean) => void
}

function getInitialTheme(): 'dark' | 'light' {
  if (typeof window === 'undefined') return 'dark'
  const saved = localStorage.getItem('theme')
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export const useUiStore = create<UiState>((set) => {
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
    isLogModalOpen: false,
    isDiagnosticsModalOpen: false,
    isFiltersCollapsed: false,
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
    setLogModalOpen: (open) => set({ isLogModalOpen: open }),
    setDiagnosticsModalOpen: (open) => set({ isDiagnosticsModalOpen: open }),
    setFiltersCollapsed: (collapsed) => set({ isFiltersCollapsed: collapsed }),
  }
})
