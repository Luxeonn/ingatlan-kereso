import { FC, useState } from 'react'
import {
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ZAxis,
  Cell,
} from 'recharts'
import { ExternalLink, Sparkles, TrendingDown } from 'lucide-react'
import { ScatterPoint } from '../../hooks/useMarketAnalytics'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

interface PriceScatterCorrelationChartProps {
  scatterData: ScatterPoint[]
  onOpenListing?: (url: string) => void
}

export const PriceScatterCorrelationChart: FC<PriceScatterCorrelationChartProps> = ({
  scatterData,
  onOpenListing,
}) => {
  const [filterSegment, setFilterSegment] = useState<'all' | 'bargain' | 'fair' | 'premium'>('all')

  const filteredData = filterSegment === 'all'
    ? scatterData
    : scatterData.filter((p) => p.segment === filterSegment)

  const handlePointClick = (point: ScatterPoint) => {
    if (point.url) {
      if (onOpenListing) {
        onOpenListing(point.url)
      } else {
        window.open(point.url, '_blank', 'noopener,noreferrer')
      }
    }
  }

  return (
    <div className="rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-5 shadow-md">
      {/* Header & Filter Controls */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Ár vs. Alapterület Korreláció (Szórásdiagram)
            </h3>
            <span className="rounded-full bg-purple-500/10 px-2 py-0.5 text-[10px] font-semibold text-purple-400">
              Szórásdiagram
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            Kattints a pontokra a hirdetés közvetlen megnyitásához. A zöld pontok piaci átlag alatti vételek!
          </p>
        </div>

        {/* Segment filter buttons */}
        <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)] text-xs">
          <button
            type="button"
            onClick={() => setFilterSegment('all')}
            className={`rounded-lg px-2.5 py-1 font-semibold transition-all ${
              filterSegment === 'all'
                ? 'bg-purple-600 text-white shadow-sm'
                : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
            }`}
          >
            Összes ({scatterData.length})
          </button>
          <button
            type="button"
            onClick={() => setFilterSegment('bargain')}
            className={`flex items-center gap-1 rounded-lg px-2.5 py-1 font-semibold transition-all ${
              filterSegment === 'bargain'
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'text-emerald-400 hover:text-emerald-300'
            }`}
          >
            <TrendingDown className="h-3 w-3" />
            <span>Alkalmi</span>
          </button>
          <button
            type="button"
            onClick={() => setFilterSegment('fair')}
            className={`rounded-lg px-2.5 py-1 font-semibold transition-all ${
              filterSegment === 'fair'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-blue-400 hover:text-blue-300'
            }`}
          >
            Piaci átlag
          </button>
          <button
            type="button"
            onClick={() => setFilterSegment('premium')}
            className={`rounded-lg px-2.5 py-1 font-semibold transition-all ${
              filterSegment === 'premium'
                ? 'bg-amber-600 text-white shadow-sm'
                : 'text-amber-400 hover:text-amber-300'
            }`}
          >
            Prémium
          </button>
        </div>
      </div>

      {/* Legend & Guide Bar */}
      <div className="flex flex-wrap items-center gap-4 mb-4 text-[11px] text-[var(--text-secondary)]">
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
          <span>Alkalmi / Piaci ár alatti (&gt;15% kedvezőbb)</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-blue-400" />
          <span>Piaci átlag</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-amber-400" />
          <span>Prémium szegmens</span>
        </div>
      </div>

      {/* Recharts Scatter Canvas */}
      <div className="h-80 w-full min-h-[320px]">
        {filteredData.length === 0 ? (
          <div className="flex h-full items-center justify-center text-xs text-[var(--text-secondary)]">
            Nincs megjeleníthető hirdetés a kiválasztott szegmensben.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <ScatterChart
              margin={{ top: 15, right: 20, left: 0, bottom: 20 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" opacity={0.4} />
              <XAxis
                type="number"
                dataKey="area"
                name="Alapterület"
                unit=" m²"
                stroke="var(--text-secondary)"
                tick={{ fill: 'var(--text-secondary)', fontSize: 11 }}
              />
              <YAxis
                type="number"
                dataKey="price"
                name="Kínálati Ár"
                unit=" M Ft"
                stroke="var(--text-secondary)"
                tick={{ fill: 'var(--text-secondary)', fontSize: 11 }}
              />
              <ZAxis range={[50, 150]} />
              <Tooltip
                cursor={{ strokeDasharray: '3 3' }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload as ScatterPoint
                    return (
                      <div className="max-w-xs rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3 shadow-xl backdrop-blur-md text-xs">
                        <div className="flex items-start justify-between gap-2 mb-1.5">
                          <p className="font-bold text-[var(--text-primary)] line-clamp-1">{data.title}</p>
                          {data.isNew && (
                            <span className="flex items-center gap-0.5 rounded bg-emerald-500/20 px-1.5 py-0.5 text-[10px] font-bold text-emerald-400 shrink-0">
                              <Sparkles className="h-3 w-3" /> Új
                            </span>
                          )}
                        </div>
                        <div className="space-y-1 text-[var(--text-secondary)]">
                          <p className="flex justify-between gap-4">
                            <span>Település:</span>
                            <span className="font-semibold text-[var(--text-primary)]">{data.city}</span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Alapterület:</span>
                            <span className="font-bold text-[var(--text-primary)]">{data.area} m²</span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Kínálati ár:</span>
                            <span className="font-bold text-[var(--text-primary)]">{formatPriceMillion(data.price)}</span>
                          </p>
                          <p className="flex justify-between gap-4">
                            <span>Fajlagos ár:</span>
                            <span className={`font-bold ${data.isBargain ? 'text-emerald-400' : 'text-blue-400'}`}>
                              {formatPricePerSqm(data.sqmPrice)}
                            </span>
                          </p>
                          {data.priceDelta != null && data.priceDelta < 0 && (
                            <p className="flex justify-between gap-4 text-emerald-400 font-semibold">
                              <span>Leárazás:</span>
                              <span>{data.priceDelta.toLocaleString('hu-HU')} Ft</span>
                            </p>
                          )}
                        </div>
                        <p className="mt-2 flex items-center gap-1 text-[10px] text-blue-400 underline font-medium">
                          <span>Kattints a megnyitáshoz</span>
                          <ExternalLink className="h-3 w-3" />
                        </p>
                      </div>
                    )
                  }
                  return null
                }}
              />
              <Scatter
                name="Ingatlanok"
                data={filteredData}
                onClick={(e) => handlePointClick(e as unknown as ScatterPoint)}
                cursor="pointer"
              >
                {filteredData.map((entry, index) => {
                  let fill = '#3b82f6'
                  if (entry.segment === 'bargain') fill = '#10b981'
                  if (entry.segment === 'premium') fill = '#f59e0b'
                  return (
                    <Cell
                      key={`scatter-cell-${index}`}
                      fill={fill}
                      fillOpacity={0.8}
                      stroke={entry.isNew ? '#ffffff' : undefined}
                      strokeWidth={entry.isNew ? 2 : 1}
                      className="transition-all hover:scale-125"
                    />
                  )
                })}
              </Scatter>
            </ScatterChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
