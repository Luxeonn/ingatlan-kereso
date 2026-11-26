import { useMemo, type FC } from 'react'
import { Archive, Trash2, TrendingDown } from 'lucide-react'
import { useUiStore, type ArchiveSubMode } from '../../store/useUiStore'
import type { IngatlanItem } from '../../types/property'
import {
  formatPricePerSqm,
  getEffectiveSqmPrice,
  getTotalSourceListingsCount,
} from '../../utils/formatters'
import { calculateMedian } from '../../utils/marketAnalyticsMath'
import { cn } from '../../utils/cn'

export interface ArchiveSubViewToolbarProps {
  allCount: number
  hiddenCount?: number
  discountedCount?: number
  activeItems?: IngatlanItem[]
}

const MODE_NOUNS: Record<ArchiveSubMode, string> = {
  all: 'archivált ingatlan',
  discounted: 'leárazott ingatlan',
  hidden: 'kukázott ingatlan',
}

function computeArchiveSummary(items: IngatlanItem[]) {
  const uniqueCount = items.length
  const totalSources = getTotalSourceListingsCount(items)
  const mergedCount = Math.max(0, totalSources - uniqueCount)
  const sqmPrices = items
    .map(getEffectiveSqmPrice)
    .filter((v): v is number => v !== null && v !== undefined && v > 0)
  const medianSqmPrice = sqmPrices.length > 0 ? Math.round(calculateMedian(sqmPrices)) : 0
  return { uniqueCount, totalSources, mergedCount, medianSqmPrice }
}

export const ArchiveSubViewToolbar: FC<ArchiveSubViewToolbarProps> = ({
  allCount,
  hiddenCount = 0,
  discountedCount = 0,
  activeItems = [],
}) => {
  const { archiveSubMode, setArchiveSubMode } = useUiStore()
  const summary = useMemo(() => computeArchiveSummary(activeItems), [activeItems])

  const tabs: Array<{
    id: ArchiveSubMode
    label: string
    count: number
    icon: FC<{ className?: string }>
  }> = [
    { id: 'all', label: 'Mentett Előzmények', count: allCount, icon: Archive },
    { id: 'discounted', label: 'Leárazás Radar', count: discountedCount, icon: TrendingDown },
    { id: 'hidden', label: 'Töröltek (Kuka)', count: hiddenCount, icon: Trash2 },
  ]

  return (
    <div
      data-testid="archive-subview-toolbar"
      className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-body)]/70 px-3.5 py-3 shadow-xs"
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
                'flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-bold transition-all cursor-pointer tabular-nums',
                isActive
                  ? 'border-blue-500/40 bg-blue-600 text-white shadow-sm'
                  : 'border-[var(--border-hairline)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-blue-500/40 hover:text-blue-700',
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              <span>{tab.label}</span>
              <span
                className={cn(
                  'rounded-full px-1.5 py-0.5 text-[10px] font-bold leading-none',
                  isActive ? 'bg-white/20 text-white' : 'bg-[var(--bg-input)] text-[var(--text-primary)]',
                )}
              >
                {tab.count}
              </span>
            </button>
          )
        })}
      </div>

      <div
        data-testid="archive-summary-notice"
        className="inline-flex flex-wrap items-center gap-1.5 rounded-lg border border-indigo-500/30 bg-indigo-500/10 px-3 py-1.5 text-xs text-indigo-700 tabular-nums"
      >
        <span>
          <strong className="font-bold text-indigo-700">
            {summary.uniqueCount} {MODE_NOUNS[archiveSubMode]}
          </strong>
          {summary.mergedCount > 0 && (
            <> ({summary.totalSources} hirdetésből · {summary.mergedCount} összevonva)</>
          )}
          {summary.medianSqmPrice > 0 && (
            <>
              {' '}
              · Medián m² ár:{' '}
              <strong className="font-bold text-indigo-700">
                {formatPricePerSqm(summary.medianSqmPrice)}
              </strong>
            </>
          )}
        </span>
      </div>
    </div>
  )
}
