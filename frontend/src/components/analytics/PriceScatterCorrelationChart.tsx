import type { FC } from 'react'
import { useState, useRef, useCallback } from 'react'
import { ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ZAxis, Cell } from 'recharts'
import { Sparkles, TrendingDown } from 'lucide-react'
import type { ScatterPoint } from '../../hooks/useMarketAnalytics'
import { openExternalUrl } from '../../utils/formatters'
import { CategoryHelpPopover, ScatterTooltipContent } from './PriceScatterParts'

interface PriceScatterCorrelationChartProps {
  scatterData: ScatterPoint[]
  onOpenListing?: (url: string) => void
}

function isScatterPoint(val: unknown): val is ScatterPoint {
  return typeof val === 'object' && val !== null && 'url' in val && typeof (val as { url: unknown }).url === 'string'
}

function extractScatterUrl(eventData: unknown): string | null {
  if (!eventData || typeof eventData !== 'object') return null
  const candidate = 'payload' in eventData ? (eventData as { payload?: unknown }).payload : eventData
  if (candidate && typeof candidate === 'object' && 'url' in candidate) {
    const url = (candidate as { url?: unknown }).url
    return typeof url === 'string' ? url : null
  }
  return null
}

export const PriceScatterCorrelationChart: FC<PriceScatterCorrelationChartProps> = ({
  scatterData,
  onOpenListing,
}) => {
  const [filterSegment, setFilterSegment] = useState<'all' | 'bargain' | 'fair' | 'premium'>('all')
  const lastClickedRef = useRef<{ url: string; time: number } | null>(null)

  const filteredData = filterSegment === 'all'
    ? scatterData
    : scatterData.filter((p) => p.segment === filterSegment)

  const handlePointClick = useCallback(
    (eventData?: unknown) => {
      const targetUrl = extractScatterUrl(eventData)
      if (targetUrl) {
        const now = Date.now()
        if (lastClickedRef.current && lastClickedRef.current.url === targetUrl && now - lastClickedRef.current.time < 300) {
          return
        }
        lastClickedRef.current = { url: targetUrl, time: now }

        if (onOpenListing) {
          onOpenListing(targetUrl)
        } else {
          openExternalUrl(targetUrl)
        }
      }
    },
    [onOpenListing],
  )

  return (
    <div className="rounded-[20px] glass-surface p-5">
      {/* Header & Filter Controls */}
      <div className="flex items-center justify-between gap-4 mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-[var(--text-primary)]">
              Ár vs. Alapterület Korreláció (Szórásdiagram)
            </h3>
            <span className="rounded-full bg-[#0071e3]/10 border border-[#0071e3]/20 px-2.5 py-0.5 text-[10px] font-bold text-[#005bb5]">
              Szórásdiagram
            </span>
          </div>
          <p className="text-xs text-[var(--text-secondary)] mt-0.5">
            Kattints a pontokra a hirdetés közvetlen megnyitásához. A zöld pontok piaci átlag alatti vételek!
          </p>
        </div>

        {/* Filter Controls & Help Button */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Segment filter buttons */}
          <div className="flex items-center rounded-full glass-capsule p-1 text-xs tabular-nums">
            <button
              type="button"
              onClick={() => setFilterSegment('all')}
              title="Összes ingatlan megjelenítése a diagramon"
              className={`rounded-full px-3 py-1 font-semibold transition-all cursor-pointer ${
                filterSegment === 'all'
                  ? 'bg-[#0071e3] text-white shadow-sm'
                  : 'text-[var(--text-secondary)] hover:bg-white/60 hover:text-[var(--text-primary)]'
              }`}
            >
              Összes ({scatterData.length})
            </button>
            <button
              type="button"
              onClick={() => setFilterSegment('bargain')}
              title="Alkalmi: A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések."
              className={`flex items-center gap-1 rounded-full px-3 py-1 font-semibold transition-all cursor-pointer ${
                filterSegment === 'bargain'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-[var(--status-bargain-fg)] hover:bg-white/60'
              }`}
            >
              <TrendingDown className="h-3 w-3" aria-hidden="true" />
              <span>Alkalmi</span>
            </button>
            <button
              type="button"
              onClick={() => setFilterSegment('fair')}
              title="Piaci átlag: A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok."
              className={`rounded-full px-3 py-1 font-semibold transition-all cursor-pointer ${
                filterSegment === 'fair'
                  ? 'bg-[#005bb5] text-white shadow-sm'
                  : 'text-[#005bb5] hover:bg-white/60'
              }`}
            >
              Piaci átlag
            </button>
            <button
              type="button"
              onClick={() => setFilterSegment('premium')}
              title="Prémium: A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok."
              className={`flex items-center gap-1 rounded-full px-3 py-1 font-semibold transition-all cursor-pointer ${
                filterSegment === 'premium'
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'text-[var(--status-warn-fg)] hover:bg-white/60'
              }`}
            >
              <Sparkles className="h-3 w-3" aria-hidden="true" />
              <span>Prémium</span>
            </button>
          </div>

          <CategoryHelpPopover />
        </div>
      </div>

      {/* Legend & Guide Bar */}
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 mb-4 text-xs text-[var(--text-secondary)] tabular-nums">
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-600 shrink-0" />
          <span className="font-semibold text-[var(--status-bargain-fg)]">Alkalmi:</span>
          <span>Helyi mediánnál ≥15%-kal olcsóbb vagy leárazott</span>
        </div>
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-[#0071e3] shrink-0" />
          <span className="font-semibold text-[#0071e3]">Piaci átlag:</span>
          <span>Reális piaci sáv (-15% és +25% között)</span>
        </div>
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-amber-500 shrink-0" />
          <span className="font-semibold text-[var(--status-warn-fg)]">Prémium:</span>
          <span>Helyi referenciaárnál ≥25%-kal magasabb fajlagos ár</span>
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
            <ScatterChart margin={{ top: 15, right: 20, left: 0, bottom: 20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(15, 23, 42, 0.08)" />
              <XAxis
                type="number"
                dataKey="area"
                name="Alapterület"
                unit=" m²"
                stroke="#42526b"
                tick={{ fill: '#42526b', fontSize: 11 }}
              />
              <YAxis
                type="number"
                dataKey="price"
                name="Kínálati Ár"
                unit=" M Ft"
                stroke="#42526b"
                tick={{ fill: '#42526b', fontSize: 11 }}
              />
              <ZAxis range={[50, 150]} />
              <Tooltip
                cursor={{ strokeDasharray: '3 3' }}
                wrapperStyle={{ zIndex: 30 }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length > 0) {
                    const firstItem = payload[0]
                    const data = firstItem?.payload
                    if (!isScatterPoint(data)) return null
                    return <ScatterTooltipContent data={data} onOpenPoint={handlePointClick} />
                  }
                  return null
                }}
              />
              <Scatter
                name="Ingatlanok"
                data={filteredData}
                onClick={handlePointClick}
                cursor="pointer"
              >
                {filteredData.map((entry, index) => {
                  const fill = entry.segment === 'bargain' ? '#059669' : entry.segment === 'premium' ? '#d97706' : '#0071e3'
                  return (
                    <Cell
                      key={`scatter-cell-${entry.id || index}`}
                      fill={fill}
                      fillOpacity={0.85}
                      stroke={entry.isNew ? '#ffffff' : '#0b1220'}
                      strokeWidth={entry.isNew ? 2 : 1}
                      className="cursor-pointer transition-opacity hover:opacity-100"
                      onClick={() => handlePointClick(entry)}
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
