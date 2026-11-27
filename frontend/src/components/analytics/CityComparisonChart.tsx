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
  Legend,
} from 'recharts'
import { MapPin, ArrowUpDown } from 'lucide-react'
import type { CityStat } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

interface CityComparisonChartProps {
  cityData: CityStat[]
  onSelectCity?: (city: string) => void
}

function isCityStat(entry: unknown): entry is CityStat {
  return typeof entry === 'object' && entry !== null && 'city' in entry && typeof (entry as { city: unknown }).city === 'string'
}

export const CityComparisonChart: FC<CityComparisonChartProps> = ({
  cityData,
  onSelectCity,
}) => {
  const [metric, setMetric] = useState<'avgSqmPrice' | 'avgPriceMillion'>('avgSqmPrice')
  const [sortBy, setSortBy] = useState<'metric' | 'count'>('metric')
  const [selectedCity, setSelectedCity] = useState<string | null>(null)

  const sortedData = [...cityData].sort((a, b) => {
    if (sortBy === 'count') return b.count - a.count
    return metric === 'avgSqmPrice'
      ? b.avgPricePerSqm - a.avgPricePerSqm
      : b.avgPriceMillion - a.avgPriceMillion
  })

  const handleBarClick = (city: string) => {
    setSelectedCity(city === selectedCity ? null : city)
    if (onSelectCity) {
      onSelectCity(city)
    }
  }

  return (
    <div className="rounded-[20px] glass-surface p-5">
      {/* Header & Controls */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Település-összehasonlító Diagram
            </h3>
            <span className="rounded-full bg-[#0071e3]/10 border border-[#0071e3]/20 px-2.5 py-0.5 text-[10px] font-bold text-[#005bb5]">
              Városok
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            Kattints a város oszlopára az adott településre történő azonnali szűréshez!
          </p>
        </div>

        {/* View and sort toggles */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Metric switcher */}
          <div className="flex items-center rounded-full glass-capsule p-1">
            <button
              type="button"
              onClick={() => setMetric('avgSqmPrice')}
              className={`rounded-full px-3 py-1 text-xs font-semibold transition-all cursor-pointer ${
                metric === 'avgSqmPrice'
                  ? 'bg-[#0071e3] text-white shadow-sm'
                  : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]'
              }`}
            >
              Ft/m²
            </button>
            <button
              type="button"
              onClick={() => setMetric('avgPriceMillion')}
              className={`rounded-full px-3 py-1 text-xs font-semibold transition-all cursor-pointer ${
                metric === 'avgPriceMillion'
                  ? 'bg-[#0071e3] text-white shadow-sm'
                  : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]'
              }`}
            >
              M Ft
            </button>
          </div>

          {/* Sort order */}
          <button
            type="button"
            onClick={() => setSortBy(sortBy === 'metric' ? 'count' : 'metric')}
            className="flex items-center gap-1 rounded-full border border-[var(--border-hairline)] bg-white/70 px-3 py-1.5 text-xs font-semibold text-[var(--text-secondary)] transition-all hover:border-[#0071e3]/40 hover:text-[var(--text-primary)] cursor-pointer"
            title="Rendezési szempont váltása"
          >
            <ArrowUpDown className="h-3.5 w-3.5" aria-hidden="true" />
            <span>{sortBy === 'metric' ? 'Ár szerint' : 'Kínálat szerint'}</span>
          </button>
        </div>
      </div>

      {/* Selected notification */}
      {selectedCity && (
        <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-[var(--status-bargain-bg)] px-3 py-1 border border-emerald-500/25 text-[var(--status-bargain-fg)] text-xs font-semibold">
          <MapPin className="h-3.5 w-3.5" aria-hidden="true" />
          <span>Kiválasztott város: {selectedCity}</span>
        </div>
      )}

      {/* Chart Canvas */}
      <div className="h-80 w-full min-h-[320px]">
        {sortedData.length === 0 ? (
          <div className="flex h-full items-center justify-center text-xs text-[var(--text-secondary)]">
            Nincs összehasonlítható települési adat.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={sortedData}
              margin={{ top: 15, right: 15, left: 10, bottom: 25 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(15, 23, 42, 0.08)" vertical={false} />
              <XAxis
                dataKey="city"
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
                tickFormatter={(val) =>
                  metric === 'avgSqmPrice'
                    ? `${Math.round(val / 1000)}k`
                    : `${val}M`
                }
              />
              <Tooltip
                cursor={{ fill: 'rgba(0, 113, 227, 0.06)' }}
                wrapperStyle={{ zIndex: 30 }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const firstItem = payload[0]
                    const data = firstItem?.payload
                    if (!isCityStat(data)) return null
                    return (
                      <div className="rounded-2xl glass-elevated p-3 text-xs tabular-nums">
                        <p className="font-bold text-[var(--text-primary)] flex items-center gap-1.5 mb-1.5">
                          <MapPin className="h-3.5 w-3.5 text-[#0071e3]" aria-hidden="true" />
                          <span>{data.city}</span>
                        </p>
                        <div className="space-y-1 text-[var(--text-secondary)]">
                          <p className="flex justify-between gap-4">
                            <span>Átlagos m² ár:</span>
                            <span className="font-bold text-[var(--text-primary)]">
                              {formatPricePerSqm(data.avgPricePerSqm)}
                            </span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Átlagos kínálati ár:</span>
                            <span className="font-bold text-[var(--text-primary)]">
                              {formatPriceMillion(data.avgPriceMillion)}
                            </span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Kínálati volumen:</span>
                            <span className="font-bold text-[#0071e3]">{data.count} db</span>
                          </p>
                          {data.newCount > 0 && (
                            <p className="flex justify-between gap-4 text-[var(--status-bargain-fg)]">
                              <span>Új hirdetés:</span>
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
              <Legend
                verticalAlign="top"
                align="right"
                wrapperStyle={{ paddingBottom: '10px', fontSize: '11px', color: '#42526b' }}
              />
              <Bar
                name={metric === 'avgSqmPrice' ? 'Átlagos Ft/m²' : 'Átlagos Ár (M Ft)'}
                dataKey={metric}
                cursor="pointer"
                radius={[6, 6, 0, 0]}
                onClick={(entry) => {
                  if (isCityStat(entry)) {
                    handleBarClick(entry.city)
                  }
                }}
              >
                {sortedData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={entry.city === selectedCity ? '#059669' : '#0071e3'}
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
