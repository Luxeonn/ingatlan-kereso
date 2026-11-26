import type { FC } from 'react'
import { Layers, Filter } from 'lucide-react'
import type { MarketSegmentStatsDto } from '../../types/api'
import { formatPricePerSqm, formatPriceMillion } from '../../utils/formatters'
import { cn } from '../../utils/cn'

export interface MarketSegmentMatrixTableProps {
  segments?: MarketSegmentStatsDto[] | null
  onSelectSegment?: (city: string, propertyType?: string) => void
}

export const MarketSegmentMatrixTable: FC<MarketSegmentMatrixTableProps> = ({
  segments,
  onSelectSegment,
}) => {
  if (!segments || segments.length === 0) return null

  return (
    <section
      data-testid="market-segment-matrix-table"
      className="overflow-hidden rounded-2xl border border-[var(--border-hairline)] bg-[var(--bg-surface)] shadow-md"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--border-hairline)] px-4 py-3">
        <div className="flex items-center gap-2">
          <Layers className="h-4 w-4 text-indigo-700" />
          <h3 className="text-xs sm:text-sm font-bold text-[var(--text-primary)]">
            Város × Ingatlantípus Szegmens Mátrix (Szerver-hitelesített bázis)
          </h3>
        </div>
        <span className="text-[11px] text-[var(--text-secondary)]">
          Minőség &gt;= 70 pont alapján számított tiszta mediánok
        </span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-xs tabular-nums">
          <thead>
            <tr className="border-b border-[var(--border-hairline)] bg-[var(--bg-input)]/60 text-[11px] font-bold uppercase tracking-wider text-[var(--text-secondary)]">
              <th className="px-4 py-2.5">Település</th>
              <th className="px-4 py-2.5">Típus</th>
              <th className="px-4 py-2.5 text-right">Minta</th>
              <th className="px-4 py-2.5 text-right">Medián Ft/m²</th>
              <th className="px-4 py-2.5 text-right">Átlag ár</th>
              <th className="px-4 py-2.5 text-right">Min – Max ár</th>
              <th className="px-4 py-2.5 text-right">Alkalmi vétel</th>
              {onSelectSegment && <th className="px-4 py-2.5 text-right">Művelet</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border-hairline)]">
            {segments.map((seg, idx) => (
              <tr
                key={`${seg.city}-${seg.propertyType}-${idx}`}
                data-testid="segment-matrix-row"
                className="transition-colors hover:bg-[var(--bg-surface-hover)]"
              >
                <td className="px-4 py-2.5 font-bold text-[var(--text-primary)]">
                  {seg.city || 'Ismeretlen'}
                </td>
                <td className="px-4 py-2.5 text-[var(--text-secondary)]">
                  {seg.propertyTypeName || seg.propertyType || 'Ingatlan'}
                </td>
                <td className="px-4 py-2.5 text-right text-[var(--text-secondary)]">
                  {seg.sampleSize ?? 0} db
                </td>
                <td className="px-4 py-2.5 text-right font-bold text-[var(--text-primary)]">
                  {seg.insufficientData ? (
                    <span className="text-[11px] font-normal text-[var(--status-warn-fg)]">
                      Kevés minta (&lt;5)
                    </span>
                  ) : seg.medianPricePerM2 ? (
                    formatPricePerSqm(seg.medianPricePerM2)
                  ) : (
                    'N/A'
                  )}
                </td>
                <td className="px-4 py-2.5 text-right text-blue-600 font-semibold">
                  {seg.averagePriceMillion ? formatPriceMillion(seg.averagePriceMillion) : 'N/A'}
                </td>
                <td className="px-4 py-2.5 text-right text-[var(--text-secondary)]">
                  {seg.minPriceMillion !== undefined && seg.maxPriceMillion !== undefined
                    ? `${Number(seg.minPriceMillion.toFixed(1))} – ${Number(seg.maxPriceMillion.toFixed(1))} M Ft`
                    : '—'}
                </td>
                <td className="px-4 py-2.5 text-right">
                  <span
                    className={cn(
                      'rounded-md border px-2 py-0.5 text-[10px] font-bold',
                      (seg.bargainCount ?? 0) > 0
                        ? 'border-emerald-500/30 bg-[var(--status-bargain-bg)] text-[var(--status-bargain-fg)]'
                        : 'border-[var(--border-hairline)] bg-[var(--bg-input)] text-[var(--text-secondary)]',
                    )}
                  >
                    {seg.bargainCount ?? 0} db
                  </span>
                </td>
                {onSelectSegment && (
                  <td className="px-4 py-2.5 text-right">
                    {seg.city && (
                      <button
                        type="button"
                        onClick={() => onSelectSegment(seg.city!, seg.propertyType)}
                        className="inline-flex items-center gap-1 rounded-lg border border-[var(--border-hairline)] bg-[var(--bg-input)] px-2.5 py-1 text-[11px] font-semibold text-[var(--text-secondary)] hover:border-blue-500/40 hover:text-blue-600 cursor-pointer"
                      >
                        <Filter className="h-3 w-3" />
                        <span>Szűrés</span>
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
