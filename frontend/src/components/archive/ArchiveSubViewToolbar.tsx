import type { FC } from 'react'
import { Archive, Trash2, TrendingDown, Database } from 'lucide-react'
import { useUiStore, type ArchiveSubMode } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

export interface ArchiveSubViewToolbarProps {
  allCount: number
  hiddenCount?: number
  discountedCount?: number
}

export const ArchiveSubViewToolbar: FC<ArchiveSubViewToolbarProps> = ({
  allCount,
  hiddenCount = 0,
  discountedCount = 0,
}) => {
  const { archiveSubMode, setArchiveSubMode } = useUiStore()

  const tabs: Array<{
    id: ArchiveSubMode
    label: string
    count: number
    icon: FC<{ className?: string }>
  }> = [
    { id: 'all', label: 'Mentett Előzmények', count: allCount, icon: Archive },
    { id: 'discounted', label: 'Globális Leárazás Radar', count: discountedCount, icon: TrendingDown },
    { id: 'hidden', label: 'Globális Elrejtettek (Kuka)', count: hiddenCount, icon: Trash2 },
  ]

  return (
    <div
      data-testid="archive-subview-toolbar"
      className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[var(--border-hairline)] bg-[var(--bg-surface)] px-4 py-3 shadow-sm"
    >
      <div className="flex flex-wrap items-center gap-2">
        {tabs.map((tab) => {
          const Icon = tab.icon
          const isActive = archiveSubMode === tab.id
          return (
            <button
              key={tab.id}
              type="button"
              data-testid={`archive-mode-${tab.id}`}
              aria-pressed={isActive}
              onClick={() => setArchiveSubMode(tab.id)}
              className={cn(
                'flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-bold transition-all cursor-pointer tabular-nums',
                isActive
                  ? 'border-blue-500/40 bg-blue-600 text-white shadow-sm'
                  : 'border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              <span>{tab.label}</span>
              <span
                className={cn(
                  'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none',
                  isActive ? 'bg-white/20 text-white' : 'bg-[var(--bg-card)] text-[var(--text-secondary)]',
                )}
              >
                {tab.count}
              </span>
            </button>
          )
        })}
      </div>

      <span className="flex items-center gap-1.5 text-[11px] text-[var(--text-secondary)]">
        <Database className="h-3.5 w-3.5 text-indigo-700" />
        <span>Adatbázis-nézet: közvetlenül a mentett helyi tárolóból</span>
      </span>
    </div>
  )
}
