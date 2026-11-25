import type { ReactNode } from 'react'
import { Inbox, Search, Star, Archive } from 'lucide-react'
import { ListingSkeleton } from './ListingSkeleton'

export function renderNoMatchingResults(): ReactNode {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-700 mb-4">
        <Inbox className="h-8 w-8" />
      </div>
      <h3 className="text-base font-bold text-[var(--text-primary)]">
        Nem található ingatlan a megadott feltételekkel
      </h3>
      <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
        Próbáld meg módosítani a gyorskereső kifejezést, a portál szűrőt, vagy kikapcsolni az aktív szűrőket (Alkalmi vétel, Megtekintettek rejtése)!
      </p>
    </div>
  )
}

export function renderLoadingSkeletons(): ReactNode {
  return (
    <div className="space-y-4">
      <div className="h-10 w-48 rounded-xl bg-[var(--bg-card)] animate-shimmer" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <ListingSkeleton key={i} />
        ))}
      </div>
    </div>
  )
}

export function renderEmptyState(
  mode: 'listings' | 'favorites' | 'archive',
  hasSearched: boolean,
  totalRawCount: number,
  onTriggerSearch: () => void,
): ReactNode {
  if (mode === 'listings' && !hasSearched) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--border-color)] bg-[var(--bg-card)]/50 p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-600 mb-4">
          <Search className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">Készen állsz a keresésre?</h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Állítsd be a kívánt településeket, árhatárokat és portálokat, majd kattints az Ingatlanok Keresése gombra!
        </p>
        <button
          type="button"
          onClick={onTriggerSearch}
          className="mt-5 rounded-xl bg-blue-600 px-5 py-2 text-xs font-bold text-white shadow-lg shadow-blue-500/25 transition-all hover:bg-blue-500"
        >
          Keresés indítása
        </button>
      </div>
    )
  }
  if (mode === 'favorites' && totalRawCount === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/10 text-amber-700 mb-4">
          <Star className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">
          Még nincsenek kedvencnek jelölt ingatlanok
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          Kattints a kártyák jobb felső sarkában található csillag ikonra a hirdetések elmentéséhez!
        </p>
      </div>
    )
  }
  if (mode === 'archive' && totalRawCount === 0) {
    return (
      <div className="flex flex-col items-center justify-center rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-12 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-purple-500/10 text-purple-700 mb-4">
          <Archive className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-[var(--text-primary)]">Az archívum jelenleg üres</h3>
        <p className="mt-1 max-w-md text-xs text-[var(--text-secondary)]">
          A korábbi keresések során megtalált és mentett hirdetések automatikusan itt jelennek meg.
        </p>
      </div>
    )
  }
  return null
}
