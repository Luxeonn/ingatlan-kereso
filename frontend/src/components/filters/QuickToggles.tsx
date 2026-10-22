import type { FC } from 'react'
import { Sparkles, ExternalLink, Building2 } from 'lucide-react'
import { cn } from '../../utils/cn'

interface QuickTogglesProps {
  onlyNew: boolean
  onToggleOnlyNew: (val: boolean) => void
  onlyNewConstruction?: boolean
  onToggleOnlyNewConstruction?: (val: boolean) => void
  autoOpen: boolean
  onToggleAutoOpen: (val: boolean) => void
}

export const QuickToggles: FC<QuickTogglesProps> = ({
  onlyNew,
  onToggleOnlyNew,
  onlyNewConstruction = false,
  onToggleOnlyNewConstruction,
  autoOpen,
  onToggleAutoOpen,
}) => {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[var(--border-color)] pt-3 text-xs">
      {/* Search-related Quick Toggles */}
      <div className="flex flex-wrap items-center gap-2">
        {/* Only new */}
        <button
          type="button"
          onClick={() => onToggleOnlyNew(!onlyNew)}
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
            onlyNew
              ? 'border border-emerald-500/40 bg-emerald-500/15 text-emerald-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
          )}
        >
          <Sparkles className="h-3.5 w-3.5 text-emerald-400" />
          <span>Csak új találatok</span>
        </button>

        {/* Only new construction (#202) */}
        {onToggleOnlyNewConstruction && (
          <button
            type="button"
            onClick={() => onToggleOnlyNewConstruction(!onlyNewConstruction)}
            className={cn(
              'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
              onlyNewConstruction
                ? 'border border-cyan-500/40 bg-cyan-500/15 text-cyan-400 shadow-sm'
                : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            )}
          >
            <Building2 className="h-3.5 w-3.5 text-cyan-400" />
            <span>Csak új építésű ingatlanok</span>
          </button>
        )}

        {/* Auto open in browser */}
        <button
          type="button"
          onClick={() => onToggleAutoOpen(!autoOpen)}
          title="Keresés után az új találatok automatikusan megnyílnak a böngészőben"
          className={cn(
            'flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 font-semibold transition-all',
            autoOpen
              ? 'border border-amber-500/40 bg-amber-500/15 text-amber-400 shadow-sm'
              : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
          )}
        >
          <ExternalLink className="h-3.5 w-3.5 text-amber-400" />
          <span>Lapok azonnali nyitása</span>
        </button>
      </div>
    </div>
  )
}
