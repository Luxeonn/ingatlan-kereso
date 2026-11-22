import { useEffect, useState, useMemo, type FC } from 'react'
import { TrendingDown, TrendingUp, History, Clock } from 'lucide-react'
import type {
  IngatlanItem,
  PriceHistoryEntry,
  ListingObservation,
  StatusLogEntry,
} from '../../types/property'
import {
  getPriceHistory,
  getListingObservations,
  getStatusLog,
} from '../../api/propertyApi'
import { formatPriceFt, formatDate } from '../../utils/formatters'

export interface PropertyTimelineSectionProps {
  item: IngatlanItem
}

interface TimelineData {
  priceHistory: PriceHistoryEntry[]
  observations: ListingObservation[]
  statusLog: StatusLogEntry[]
}

const EMPTY_TIMELINE: TimelineData = {
  priceHistory: [],
  observations: [],
  statusLog: [],
}

interface SparklineGeometry {
  polylinePoints: string
  minMillion: number
  maxMillion: number
  latestMillion: number
}

function extractEntryMillion(e: PriceHistoryEntry): number | null {
  if (typeof e.priceMillions === 'number' && e.priceMillions > 0) return e.priceMillions
  if (typeof e.priceAmount === 'number' && e.priceAmount > 0) return e.priceAmount / 1_000_000
  return null
}

function buildSparklinePoints(entries: PriceHistoryEntry[]): SparklineGeometry | null {
  const prices = entries
    .map(extractEntryMillion)
    .filter((v): v is number => v !== null && Number.isFinite(v))

  if (prices.length < 2) return null

  const minMillion = Math.min(...prices)
  const maxMillion = Math.max(...prices)
  const range = Math.max(0.1, maxMillion - minMillion)
  const width = 220
  const height = 38

  const coords = prices.map((val, idx) => {
    const x = Math.round((idx / (prices.length - 1)) * width)
    const y = Math.round(height - ((val - minMillion) / range) * (height - 8) - 4)
    return `${x},${y}`
  })

  return {
    polylinePoints: coords.join(' '),
    minMillion: Number(minMillion.toFixed(1)),
    maxMillion: Number(maxMillion.toFixed(1)),
    latestMillion: Number((prices[prices.length - 1] ?? minMillion).toFixed(1)),
  }
}

async function loadPropertyTimelineData(
  lookup: { url: string } | { id: string },
): Promise<TimelineData> {
  const [priceHistory, observations, statusLog] = await Promise.all([
    getPriceHistory(lookup).catch(() => [] as PriceHistoryEntry[]),
    getListingObservations(lookup).catch(() => [] as ListingObservation[]),
    getStatusLog(lookup).catch(() => [] as StatusLogEntry[]),
  ])
  return {
    priceHistory: Array.isArray(priceHistory) ? priceHistory : [],
    observations: Array.isArray(observations) ? observations : [],
    statusLog: Array.isArray(statusLog) ? statusLog : [],
  }
}

function useLoadedTimelineData(item: IngatlanItem): TimelineData {
  const targetUrl = (item.url ?? '').trim()
  const targetId = (item.id ?? '').trim()
  const cacheKey = targetUrl || targetId
  const [data, setData] = useState<TimelineData>(EMPTY_TIMELINE)

  useEffect(() => {
    if (!cacheKey || cacheKey.length > 1000) return undefined
    let cancelled = false
    const lookup = targetUrl ? { url: targetUrl } : { id: targetId }
    void loadPropertyTimelineData(lookup).then((loaded) => {
      if (!cancelled) setData(loaded)
    })
    return () => {
      cancelled = true
    }
  }, [cacheKey, targetUrl, targetId])

  return data
}

const TimelineSparklineBox: FC<{ sparkline: SparklineGeometry }> = ({ sparkline }) => (
  <div
    data-testid="price-history-sparkline"
    className="flex items-center justify-between gap-3 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-card)] px-3 py-2 tabular-nums"
  >
    <svg
      viewBox="0 0 220 38"
      className="h-9 w-44 overflow-visible shrink-0"
      aria-label="Ártörténeti görbe"
    >
      <polyline
        fill="none"
        stroke="var(--accent)"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={sparkline.polylinePoints}
      />
    </svg>
    <div className="text-right text-[10px] text-[var(--text-secondary)]">
      <div>Tartomány: {sparkline.minMillion}–{sparkline.maxMillion} M Ft</div>
      <div className="font-bold text-[var(--text-primary)]">
        Aktuális: {sparkline.latestMillion} M Ft
      </div>
    </div>
  </div>
)

const TimelineDeltaRow: FC<{ priceDelta: number; priceChangePercent?: number | null }> = ({
  priceDelta,
  priceChangePercent,
}) => {
  const isDrop = priceDelta < 0
  return (
    <div
      data-testid="timeline-price-delta-row"
      className="flex items-center justify-between rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-card)] px-2.5 py-1.5 tabular-nums"
    >
      <span className="flex items-center gap-1.5 font-semibold text-[var(--text-primary)]">
        {isDrop ? (
          <TrendingDown className="h-3.5 w-3.5 text-[var(--status-danger-fg)]" />
        ) : (
          <TrendingUp className="h-3.5 w-3.5 text-[var(--status-warn-fg)]" />
        )}
        <span>Utolsó árváltozás</span>
      </span>
      <span
        className={
          isDrop ? 'font-bold text-[var(--status-danger-fg)]' : 'font-bold text-[var(--status-warn-fg)]'
        }
      >
        {priceDelta > 0 ? '+' : ''}
        {formatPriceFt(priceDelta)} ({Math.round(priceChangePercent || 0)}%)
      </span>
    </div>
  )
}

const TimelineHistoryRows: FC<{ entries: PriceHistoryEntry[] }> = ({ entries }) => {
  if (entries.length === 0) return null
  return (
    <div className="space-y-1 max-h-36 overflow-y-auto pr-1">
      {entries.slice(0, 5).map((entry, idx) => (
        <div
          key={entry.id ?? idx}
          data-testid="price-history-entry-row"
          className="flex items-center justify-between rounded-md bg-[var(--bg-card)]/70 px-2.5 py-1 text-[11px] tabular-nums"
        >
          <span className="flex items-center gap-1 text-[var(--text-secondary)]">
            <Clock className="h-3 w-3 opacity-60" />
            <span>{formatDate(entry.recordedAt)}</span>
          </span>
          <span className="font-semibold text-[var(--text-primary)]">
            {formatPriceFt(entry.priceAmount)}
          </span>
        </div>
      ))}
    </div>
  )
}

export const PropertyTimelineSection: FC<PropertyTimelineSectionProps> = ({ item }) => {
  const { priceHistory, observations, statusLog } = useLoadedTimelineData(item)
  const sparkline = useMemo(() => buildSparklinePoints(priceHistory), [priceHistory])
  const unchangedCount = useMemo(
    () => observations.filter((o) => o.statusEvent === 'UNCHANGED').length,
    [observations],
  )

  const priceDelta = item.priceDelta ?? 0
  const hasTimelineRecords =
    priceHistory.length > 0 || observations.length > 0 || statusLog.length > 0

  if (!hasTimelineRecords && priceDelta === 0) {
    return null
  }

  return (
    <div
      data-testid="property-timeline-section"
      className="flex flex-col gap-2 border-t border-[var(--border-hairline)] pt-3 text-xs"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5 font-semibold text-[var(--text-primary)]">
          <History className="h-3.5 w-3.5 text-blue-400" aria-hidden="true" />
          <span>Ártörténet & Észlelési Idővonal</span>
        </div>
        {unchangedCount > 0 && (
          <span
            data-testid="timeline-unchanged-badge"
            className="rounded-md border border-[var(--border-hairline)] bg-[var(--bg-card)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-secondary)] tabular-nums"
          >
            {unchangedCount}× változatlan ár
          </span>
        )}
      </div>

      {sparkline && <TimelineSparklineBox sparkline={sparkline} />}

      {priceDelta !== 0 && (
        <TimelineDeltaRow priceDelta={priceDelta} priceChangePercent={item.priceChangePercent} />
      )}

      <TimelineHistoryRows entries={priceHistory} />
    </div>
  )
}
