import type { FC } from 'react'
import { useState, useRef, useCallback } from 'react'
import { ExternalLink, Sparkles, HelpCircle, X } from 'lucide-react'
import type { ScatterPoint } from '../../hooks/useMarketAnalytics'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { formatPriceMillion, formatPricePerSqm } from '../../utils/formatters'

export const CategoryHelpPopover: FC = () => {
  const [isHelpOpen, setIsHelpOpen] = useState(false)
  const helpRef = useRef<HTMLDivElement>(null)
  const closeHelp = useCallback(() => setIsHelpOpen(false), [])

  useOutsideDismiss(helpRef, isHelpOpen, closeHelp, true)

  return (
    <div className="relative" ref={helpRef}>
      <button
        type="button"
        onClick={() => setIsHelpOpen((prev) => !prev)}
        aria-expanded={isHelpOpen}
        aria-label="Kategória besorolási szabályok súgó"
        data-testid="category-help-button"
        className={`flex items-center gap-1 rounded-full border px-3 py-1.5 text-xs font-semibold transition-all cursor-pointer ${
          isHelpOpen
            ? 'border-[#0071e3]/45 bg-[#0071e3]/12 text-[#005bb5]'
            : 'border-[var(--border-hairline)] bg-white/70 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-[#0071e3]/40'
        }`}
      >
        <HelpCircle className="h-3.5 w-3.5 text-[#0071e3]" aria-hidden="true" />
        <span>Kategória súgó</span>
      </button>

      {isHelpOpen && (
        <div
          role="tooltip"
          data-testid="category-help-popover"
          className="absolute right-0 top-full mt-2 z-40 w-96 rounded-2xl glass-elevated p-4 text-xs space-y-2.5 animate-in fade-in zoom-in-95 duration-150"
        >
          <div className="flex items-center justify-between border-b border-[var(--border-hairline)] pb-2">
            <h4 className="font-bold text-[var(--text-primary)] flex items-center gap-1.5">
              <HelpCircle className="h-4 w-4 text-[#0071e3]" aria-hidden="true" />
              Kategória besorolási szabályok
            </h4>
            <button
              type="button"
              onClick={closeHelp}
              className="rounded-full p-1 text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-white/70 transition-colors cursor-pointer"
              aria-label="Súgó bezárása"
            >
              <X className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
          </div>

          <div className="space-y-2 text-left">
            <div className="flex items-start gap-2.5 rounded-xl bg-[var(--status-bargain-bg)] p-2.5 border border-emerald-500/20">
              <span className="mt-1 h-2.5 w-2.5 rounded-full bg-emerald-600 shrink-0" />
              <div>
                <span className="font-bold text-[var(--status-bargain-fg)]">Alkalmi:</span>
                <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                  A helyi piaci mediánnál/átlagnál legalább 15%-kal olcsóbb fajlagos árú vagy jelentős árcsökkenésen átesett hirdetések.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-2.5 rounded-xl bg-[#0071e3]/10 p-2.5 border border-[#0071e3]/20">
              <span className="mt-1 h-2.5 w-2.5 rounded-full bg-[#0071e3] shrink-0" />
              <div>
                <span className="font-bold text-[#005bb5]">Piaci átlag:</span>
                <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                  A reális piaci sávba tartozó (referenciaár -15% és +25% közötti) ingatlanok.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-2.5 rounded-xl bg-[var(--status-warn-bg)] p-2.5 border border-amber-500/20">
              <span className="mt-1 h-2.5 w-2.5 rounded-full bg-amber-500 shrink-0" />
              <div>
                <span className="font-bold text-[var(--status-warn-fg)]">Prémium:</span>
                <p className="mt-0.5 text-[11px] text-[var(--text-secondary)] leading-relaxed">
                  A helyi piaci referenciaárnál legalább 25%-kal magasabb fajlagos négyzetméterárú felsőkategóriás ingatlanok.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export interface ScatterTooltipContentProps {
  data: ScatterPoint
  onOpenPoint: (point: ScatterPoint) => void
}

export const ScatterTooltipContent: FC<ScatterTooltipContentProps> = ({ data, onOpenPoint }) => (
  <div
    onClick={(e) => {
      e.stopPropagation()
      onOpenPoint(data)
    }}
    className="select-none max-w-xs rounded-2xl glass-elevated p-3.5 text-xs cursor-pointer hover:border-[#0071e3]/45! transition-colors tabular-nums"
  >
    <div className="flex items-start justify-between gap-2 mb-1.5">
      <p className="font-bold text-[var(--text-primary)] line-clamp-1">{data.title}</p>
      {data.isNew && (
        <span className="flex items-center gap-0.5 rounded-full bg-[var(--status-bargain-bg)] px-2 py-0.5 text-[10px] font-bold text-[var(--status-bargain-fg)] shrink-0">
          <Sparkles className="h-3 w-3" aria-hidden="true" /> Új
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
        <span className={`font-bold ${data.isBargain ? 'text-[var(--status-bargain-fg)]' : 'text-[#0071e3]'}`}>
          {formatPricePerSqm(data.sqmPrice)}
        </span>
      </p>
      {data.priceDelta !== null && data.priceDelta !== undefined && data.priceDelta < 0 && (
        <p className="flex justify-between gap-4 text-[var(--status-bargain-fg)] font-semibold">
          <span>Leárazás:</span>
          <span>{data.priceDelta.toLocaleString('hu-HU')} Ft</span>
        </p>
      )}
    </div>
    <button
      type="button"
      onClick={(e) => {
        e.stopPropagation()
        onOpenPoint(data)
      }}
      className="mt-2.5 flex items-center justify-center gap-1.5 w-full rounded-xl bg-[#0071e3]/10 hover:bg-[#0071e3]/18 text-[#005bb5] font-semibold py-1.5 px-2.5 text-[11px] transition-colors border border-[#0071e3]/20 cursor-pointer"
    >
      <span>Hirdetés megnyitása</span>
      <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
    </button>
  </div>
)
