import { useState, FC } from 'react'
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
import { HistogramBin } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

interface PriceDistributionChartProps {
  priceBins: HistogramBin[]
  sqmPriceBins: HistogramBin[]
  avgPrice: number
  medianPrice: number
  avgSqmPrice: number
  medianSqmPrice: number
  onSelectRange?: (type: 'price' | 'sqmPrice', min: number, max: number) => void
}

export const PriceDistributionChart: FC<PriceDistributionChartProps> = ({
  priceBins,
  sqmPriceBins,
  avgPrice,
  medianPrice,
  avgSqmPrice,
  medianSqmPrice,
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
    <div className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-md">
      {/* Chart Header & Metric Switcher */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              {metric === 'sqmPrice' ? 'Négyzetméterár Eloszlás (Ft/m²)' : 'Kínálati Ár Eloszlás (M Ft)'}
            </h3>
            <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-400">
              Hisztogram
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            Kattints bármelyik oszlopra a tartomány szerinti szűréshez!
          </p>
        </div>

        {/* Toggle buttons */}
        <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)]">
          <button
            type="button"
            onClick={() => {
              setMetric('sqmPrice')
              setActiveBinIndex(null)
            }}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
              metric === 'sqmPrice'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <Ruler className="h-3.5 w-3.5" />
            <span>Fajlagos ár (Ft/m²)</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setMetric('price')
              setActiveBinIndex(null)
            }}
            className={`flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
              metric === 'price'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            <DollarSign className="h-3.5 w-3.5" />
            <span>Kínálati ár (M Ft)</span>
          </button>
        </div>
      </div>

      {/* KPI Reference Badges */}
      <div className="flex flex-wrap items-center gap-3 mb-4 text-xs">
        <div className="flex items-center gap-1.5 rounded-lg bg-[var(--bg-input)] px-2.5 py-1 border border-[var(--border-color)]">
          <span className="h-2 w-2 rounded-full bg-blue-500" />
          <span className="text-[var(--text-secondary)]">Átlag:</span>
          <span className="font-bold text-[var(--text-primary)]">
            {metric === 'sqmPrice' ? formatPricePerSqm(currentAvg) : formatPriceMillion(currentAvg)}
          </span>
        </div>
        <div className="flex items-center gap-1.5 rounded-lg bg-[var(--bg-input)] px-2.5 py-1 border border-[var(--border-color)]">
          <span className="h-2 w-2 rounded-full bg-amber-400" />
          <span className="text-[var(--text-secondary)]">Medián:</span>
          <span className="font-bold text-[var(--text-primary)]">
            {metric === 'sqmPrice' ? formatPricePerSqm(currentMedian) : formatPriceMillion(currentMedian)}
          </span>
        </div>
        {activeBinIndex !== null && activeBins[activeBinIndex] && (
          <div className="flex items-center gap-1.5 rounded-lg bg-emerald-500/10 px-2.5 py-1 border border-emerald-500/30 text-emerald-400 font-semibold">
            <Info className="h-3.5 w-3.5" />
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
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" opacity={0.4} vertical={false} />
              <XAxis
                dataKey="rangeLabel"
                stroke="var(--text-secondary)"
                tick={{ fill: 'var(--text-secondary)', fontSize: 11 }}
                angle={-20}
                textAnchor="end"
                interval={0}
                height={45}
              />
              <YAxis
                stroke="var(--text-secondary)"
                tick={{ fill: 'var(--text-secondary)', fontSize: 11 }}
                allowDecimals={false}
              />
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload as HistogramBin
                    return (
                      <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3 shadow-xl backdrop-blur-md text-xs">
                        <p className="font-bold text-[var(--text-primary)] mb-1">{data.rangeLabel}</p>
                        <div className="space-y-1 text-[var(--text-secondary)]">
                          <p className="flex justify-between gap-4">
                            <span>Hirdetések:</span>
                            <span className="font-bold text-[var(--text-primary)]">{data.count} db</span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Arány:</span>
                            <span className="font-bold text-blue-400">{data.percentage}%</span>
                          </p>
                          {data.newCount > 0 && (
                            <p className="flex justify-between gap-4 text-emerald-400">
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
                stroke="var(--border-color)"
              />
              <Bar
                dataKey="count"
                cursor="pointer"
                radius={[6, 6, 0, 0]}
                onClick={(entry, index) => handleBarClick(entry as unknown as HistogramBin, index)}
              >
                {activeBins.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={index === activeBinIndex ? '#10b981' : '#3b82f6'}
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
