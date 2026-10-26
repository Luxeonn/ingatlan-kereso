import { FC, useState, useRef, useEffect, useCallback } from 'react'
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
import { ExternalLink, Sparkles, TrendingDown, HelpCircle, X } from 'lucide-react'
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
  const [isHelpOpen, setIsHelpOpen] = useState(false)
  const helpRef = useRef<HTMLDivElement>(null)
  const lastClickedRef = useRef<{ url: string; time: number } | null>(null)

  useEffect(() => {
    if (!isHelpOpen) return

    const handleClickOutside = (event: MouseEvent) => {
      if (helpRef.current && !helpRef.current.contains(event.target as Node)) {
        setIsHelpOpen(false)
      }
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsHelpOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isHelpOpen])

  const filteredData = filterSegment === 'all'
    ? scatterData
    : scatterData.filter((p) => p.segment === filterSegment)

  const handlePointClick = useCallback(
    (eventData?: unknown) => {
      if (!eventData || typeof eventData !== 'object') return
      const raw = eventData as Record<string, unknown>
      const payload = (raw.payload && typeof raw.payload === 'object' ? raw.payload : raw) as Partial<ScatterPoint>
      const targetUrl = payload.url

      if (targetUrl) {
        const now = Date.now()
        if (lastClickedRef.current && lastClickedRef.current.url === targetUrl && now - lastClickedRef.current.time < 300) {
          return
        }
        lastClickedRef.current = { url: targetUrl, time: now }

        if (onOpenListing) {
          onOpenListing(targetUrl)
        } else {
          window.open(targetUrl, '_blank', 'noopener,noreferrer')
        }
      }
    },
    [onOpenListing]
  )

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

        {/* Filter Controls & Help Button */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Segment filter buttons */}
          <div className="flex items-center rounded-xl bg-[var(--bg-input)] p-1 border border-[var(--border-color)] text-xs">
            <button
              type="button"
              onClick={() => setFilterSegment('all')}
              title="Összes ingatlan megjelenítése a diagramon"
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
              title="Alkalmi: A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések."
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
              title="Piaci átlag: A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok."
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
              title="Prémium: A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok."
              className={`flex items-center gap-1 rounded-lg px-2.5 py-1 font-semibold transition-all ${
                filterSegment === 'premium'
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'text-amber-400 hover:text-amber-300'
              }`}
            >
              <Sparkles className="h-3 w-3" />
              <span>Prémium</span>
            </button>
          </div>

          {/* Category Help Popover */}
          <div className="relative" ref={helpRef}>
            <button
              type="button"
              onClick={() => setIsHelpOpen((prev) => !prev)}
              aria-expanded={isHelpOpen}
              aria-label="Kategória besorolási szabályok súgó"
              data-testid="category-help-button"
              className={`flex items-center gap-1 rounded-xl border px-2.5 py-1.5 text-xs font-semibold transition-all cursor-pointer ${
                isHelpOpen
                  ? 'border-purple-500 bg-purple-500/20 text-purple-300 shadow-sm'
                  : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-purple-500/40'
              }`}
            >
              <HelpCircle className="h-3.5 w-3.5 text-purple-400" />
              <span>Kategória súgó</span>
            </button>

            {isHelpOpen && (
              <div
                role="tooltip"
                data-testid="category-help-popover"
                className="absolute right-0 top-full mt-2 z-30 w-80 sm:w-96 rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3.5 shadow-2xl backdrop-blur-md text-xs space-y-2.5 animate-in fade-in zoom-in-95 duration-150"
              >
                <div className="flex items-center justify-between border-b border-[var(--border-color)]/60 pb-2">
                  <h4 className="font-bold text-[var(--text-primary)] flex items-center gap-1.5">
                    <HelpCircle className="h-4 w-4 text-purple-400" />
                    Kategória besorolási szabályok
                  </h4>
                  <button
                    type="button"
                    onClick={() => setIsHelpOpen(false)}
                    className="rounded p-0.5 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-input)] transition-all cursor-pointer"
                    aria-label="Súgó bezárása"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>

                <div className="space-y-2 text-left">
                  <div className="flex items-start gap-2.5 rounded-lg bg-emerald-500/10 p-2.5 border border-emerald-500/20">
                    <span className="mt-1 h-2.5 w-2.5 rounded-full bg-emerald-400 shrink-0" />
                    <div>
                      <span className="font-bold text-emerald-400">Alkalmi:</span>
                      <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                        A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések.
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5 rounded-lg bg-blue-500/10 p-2.5 border border-blue-500/20">
                    <span className="mt-1 h-2.5 w-2.5 rounded-full bg-blue-400 shrink-0" />
                    <div>
                      <span className="font-bold text-blue-400">Piaci átlag:</span>
                      <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                        A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok.
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start gap-2.5 rounded-lg bg-amber-500/10 p-2.5 border border-amber-500/20">
                    <span className="mt-1 h-2.5 w-2.5 rounded-full bg-amber-400 shrink-0" />
                    <div>
                      <span className="font-bold text-amber-400">Prémium:</span>
                      <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                        A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok.
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Legend & Guide Bar */}
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 mb-4 text-xs text-[var(--text-secondary)]">
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-400 shrink-0" />
          <span className="font-semibold text-emerald-400">Alkalmi:</span>
          <span>Helyi mediánnál ≥15%-kal olcsóbb vagy leárazott</span>
        </div>
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-blue-400 shrink-0" />
          <span className="font-semibold text-blue-400">Piaci átlag:</span>
          <span>Reális piaci sáv (-15% és +25% között)</span>
        </div>
        <div
          className="flex items-center gap-1.5 cursor-help"
          title="A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok."
        >
          <span className="h-2.5 w-2.5 rounded-full bg-amber-400 shrink-0" />
          <span className="font-semibold text-amber-400">Prémium:</span>
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
                wrapperStyle={{ zIndex: 100 }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length) {
                    const data = payload[0].payload as ScatterPoint
                    return (
                      <div
                        onClick={(e) => {
                          e.stopPropagation()
                          handlePointClick(data)
                        }}
                        className="select-none max-w-xs rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] p-3 shadow-xl backdrop-blur-md text-xs cursor-pointer hover:border-blue-500/50 transition-colors"
                      >
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
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            handlePointClick(data)
                          }}
                          className="mt-2.5 flex items-center justify-center gap-1.5 w-full rounded-lg bg-blue-600/10 hover:bg-blue-600/20 text-blue-400 font-semibold py-1.5 px-2 text-[11px] transition-colors border border-blue-500/20"
                        >
                          <span>Hirdetés megnyitása</span>
                          <ExternalLink className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    )
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
                  let fill = '#3b82f6'
                  if (entry.segment === 'bargain') fill = '#10b981'
                  if (entry.segment === 'premium') fill = '#f59e0b'
                  return (
                    <Cell
                      key={`scatter-cell-${entry.id || index}`}
                      fill={fill}
                      fillOpacity={0.8}
                      stroke={entry.isNew ? '#ffffff' : '#1e293b'}
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
