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
  Legend,
} from 'recharts'
import { MapPin, ArrowUpDown } from 'lucide-react'
import { CityStat } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

interface CityComparisonChartProps {
  cityData: CityStat[]
  onSelectCity?: (city: string) => void
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
    <div className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-md">
      {/* Header & Controls */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Település-összehasonlító Diagram
            </h3>
            <span className="rounded-full bg-indigo-500/10 px-2 py-0.5 text-[10px] font-semibold text-indigo-400">
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
          <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)]">
            <button
              type="button"
              onClick={() => setMetric('avgSqmPrice')}
              className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition-all ${
                metric === 'avgSqmPrice'
                  ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              }`}
            >
              Ft/m²
            </button>
            <button
              type="button"
              onClick={() => setMetric('avgPriceMillion')}
              className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition-all ${
                metric === 'avgPriceMillion'
                  ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
              }`}
            >
              M Ft
            </button>
          </div>

          {/* Sort order */}
          <button
            type="button"
            onClick={() => setSortBy(sortBy === 'metric' ? 'count' : 'metric')}
            className="flex items-center gap-1 rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] px-2.5 py-1.5 text-xs font-medium text-[var(--text-secondary)] transition-all hover:text-[var(--text-primary)]"
            title="Rendezési szempont váltása"
          >
            <ArrowUpDown className="h-3.5 w-3.5" />
            <span>{sortBy === 'metric' ? 'Ár szerint' : 'Kínálat szerint'}</span>
          </button>
        </div>
      </div>

      {/* Selected notification */}
      {selectedCity && (
        <div className="mb-4 flex items-center gap-2 rounded-lg bg-emerald-500/10 px-3 py-1.5 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
          <MapPin className="h-3.5 w-3.5" />
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
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" opacity={0.4} vertical={false} />
              <XAxis
                dataKey="city"
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
                tickFormatter={(val) =>
                  metric === 'avgSqmPrice'
                    ? `${Math.round(val / 1000)}k`
                    : `${val}M`
                }
              />
              <Tooltip
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload as CityStat
                    return (
                      <div className="rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3 shadow-xl backdrop-blur-md text-xs">
                        <p className="font-bold text-[var(--text-primary)] flex items-center gap-1.5 mb-1.5">
                          <MapPin className="h-3.5 w-3.5 text-indigo-400" />
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
                            <span className="font-bold text-indigo-400">{data.count} db</span>
                          </p>
                          {data.newCount > 0 && (
                            <p className="flex justify-between gap-4 text-emerald-400">
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
                wrapperStyle={{ paddingBottom: '10px', fontSize: '11px' }}
              />
              <Bar
                name={metric === 'avgSqmPrice' ? 'Átlagos Ft/m²' : 'Átlagos Ár (M Ft)'}
                dataKey={metric}
                cursor="pointer"
                radius={[6, 6, 0, 0]}
                onClick={(entry) => handleBarClick((entry as unknown as CityStat).city)}
              >
                {sortedData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={entry.city === selectedCity ? '#10b981' : '#6366f1'}
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
