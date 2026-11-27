import type { FC } from 'react'
import { useState } from 'react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  ReferenceLine,
} from 'recharts'
import { Ruler, DollarSign, Info } from 'lucide-react'
import type { HistogramBin } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

interface PriceDistributionChartProps {
  priceBins: HistogramBin[]
  sqmPriceBins: HistogramBin[]
  avgPrice: number
  medianPrice: number
  avgSqmPrice: number
  medianSqmPrice: number
  categoryLabel?: string | null
  onSelectRange?: (type: 'price' | 'sqmPrice', min: number, max: number) => void
}

function isHistogramBin(entry: unknown): entry is HistogramBin {
  return typeof entry === 'object' && entry !== null && 'min' in entry && 'max' in entry && 'count' in entry
}

export const PriceDistributionChart: FC<PriceDistributionChartProps> = ({
  priceBins,
  sqmPriceBins,
  avgPrice,
  medianPrice,
  avgSqmPrice,
  medianSqmPrice,
  categoryLabel,
  onSelectRange,
}) => {
  const [metric, setMetric] = useState<'sqmPrice' | 'price'>('sqmPrice')
  const [activeBinIndex, setActiveBinIndex] = useState<number | null>(null)

  const activeBins = metric === 'sqmPrice' ? sqmPriceBins : priceBins
  const currentAvg = metric === 'sqmPrice' ? avgSqmPrice : avgPrice
  const currentMedian = metric === 'sqmPrice' ? medianSqmPrice : medianPrice

  const handleBarClick = (entry: HistogramBin, index: number) => {
    setActiveBinIndex(index === activeBinIndex ? null : index)
    if (onSelectRange) {
      onSelectRange(metric, entry.min, entry.max)
    }
  }

  return (
    <div className="rounded-[20px] glass-surface p-5">
      {/* Chart Header & Metric Switcher */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              {metric === 'sqmPrice'
                ? categoryLabel
                  ? `${categoryLabel} Fajlagos Ár Eloszlás (Ft/m²)`
                  : 'Négyzetméterár Eloszlás (Ft/m²)'
                : 'Kínálati Ár Eloszlás (M Ft)'}
            </h3>
            <span className="rounded-full bg-[#0071e3]/10 border border-[#0071e3]/20 px-2.5 py-0.5 text-[10px] font-bold text-[#005bb5]">
              Hisztogram
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            Kattints bármelyik oszlopra a tartomány szerinti szűréshez!
          </p>
        </div>

        {/* Toggle buttons */}
        <div className="flex items-center rounded-full glass-capsule p-1">
          <button
            type="button"
            onClick={() => {
              setMetric('sqmPrice')
              setActiveBinIndex(null)
            }}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-all cursor-pointer ${
              metric === 'sqmPrice'
                ? 'bg-[#0071e3] text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]'
            }`}
          >
            <Ruler className="h-3.5 w-3.5" aria-hidden="true" />
            <span>Fajlagos ár (Ft/m²)</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setMetric('price')
              setActiveBinIndex(null)
            }}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-all cursor-pointer ${
              metric === 'price'
                ? 'bg-[#0071e3] text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]'
            }`}
          >
            <DollarSign className="h-3.5 w-3.5" aria-hidden="true" />
            <span>Kínálati ár (M Ft)</span>
          </button>
        </div>
      </div>

      {/* KPI Reference Badges */}
      <div className="flex flex-wrap items-center gap-2.5 mb-4 text-xs">
        <div className="flex items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/70 px-3 py-1 tabular-nums">
          <span className="h-2 w-2 rounded-full bg-[#0071e3]" />
          <span className="text-[var(--text-secondary)]">Átlag:</span>
          <span className="font-bold text-[var(--text-primary)]">
            {metric === 'sqmPrice' ? formatPricePerSqm(currentAvg) : formatPriceMillion(currentAvg)}
          </span>
        </div>
        <div className="flex items-center gap-1.5 rounded-full border border-[var(--border-hairline)] bg-white/70 px-3 py-1 tabular-nums">
          <span className="h-2 w-2 rounded-full bg-amber-500" />
          <span className="text-[var(--text-secondary)]">Medián:</span>
          <span className="font-bold text-[var(--text-primary)]">
            {metric === 'sqmPrice' ? formatPricePerSqm(currentMedian) : formatPriceMillion(currentMedian)}
          </span>
        </div>
        {activeBinIndex !== null && activeBins[activeBinIndex] && (
          <div className="flex items-center gap-1.5 rounded-full bg-[var(--status-bargain-bg)] px-3 py-1 border border-emerald-500/25 text-[var(--status-bargain-fg)] font-semibold tabular-nums">
            <Info className="h-3.5 w-3.5" aria-hidden="true" />
            <span>Kijelölve: {activeBins[activeBinIndex].rangeLabel} ({activeBins[activeBinIndex].count} db)</span>
          </div>
        )}
      </div>

      {/* Recharts Bar Container */}
      <div className="h-80 w-full min-h-[320px]">
        {activeBins.length === 0 ? (
          <div className="flex h-full items-center justify-center text-xs text-[var(--text-secondary)]">
            Nincs elegendő adat az eloszlásgörbe megjelenítéséhez.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={activeBins}
              margin={{ top: 15, right: 15, left: -10, bottom: 25 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(15, 23, 42, 0.08)" vertical={false} />
              <XAxis
                dataKey="rangeLabel"
                stroke="#42526b"
                tick={{ fill: '#42526b', fontSize: 11 }}
                angle={-20}
                textAnchor="end"
                interval={0}
                height={45}
              />
              <YAxis
                stroke="#42526b"
                tick={{ fill: '#42526b', fontSize: 11 }}
                allowDecimals={false}
              />
              <Tooltip
                cursor={{ fill: 'rgba(0, 113, 227, 0.06)' }}
                wrapperStyle={{ zIndex: 30 }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const firstItem = payload[0]
                    const data = firstItem?.payload
                    if (!isHistogramBin(data)) return null
                    return (
                      <div className="rounded-2xl glass-elevated p-3 text-xs tabular-nums">
                        <p className="font-bold text-[var(--text-primary)] mb-1">{data.rangeLabel}</p>
                        <div className="space-y-1 text-[var(--text-secondary)]">
                          <p className="flex justify-between gap-4">
                            <span>Hirdetések:</span>
                            <span className="font-bold text-[var(--text-primary)]">{data.count} db</span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Arány:</span>
                            <span className="font-bold text-[#0071e3]">{data.percentage}%</span>
                          </p>
                          {data.newCount > 0 && (
                            <p className="flex justify-between gap-4 text-[var(--status-bargain-fg)]">
                              <span>Új találat:</span>
                              <span className="font-bold">{data.newCount} db</span>
                            </p>
                          )}
                        </div>
                      </div>
                    )
                  }
                  return null
                }}
              />
              <ReferenceLine
                y={0}
                stroke="rgba(15, 23, 42, 0.14)"
              />
              <Bar
                dataKey="count"
                cursor="pointer"
                radius={[6, 6, 0, 0]}
                onClick={(entry, index) => {
                  if (isHistogramBin(entry)) {
                    handleBarClick(entry, index)
                  }
                }}
              >
                {activeBins.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={index === activeBinIndex ? '#059669' : '#0071e3'}
                    className="transition-all duration-200 hover:opacity-80"
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
