import { useState, type ChangeEvent, type FC } from 'react'
import {
  X,
  Upload,
  ExternalLink,
  Copy,
  Check,
  FileCode2,
  Sparkles,
  AlertCircle,
} from 'lucide-react'
import { importIngatlanComHtml } from '../../api/portalApi'
import { useFilterStore } from '../../store/useFilterStore'
import { useUiStore } from '../../store/useUiStore'
import { buildIngatlanComSearchUrl } from '../../utils/ingatlanComUrl'
import { normalizePropertyType, openExternalUrl } from '../../utils/formatters'

interface IngatlanComImportModalProps {
  fetchedPages?: number | null
  onImportSuccess?: () => void
}

interface ImportStatsSummary {
  parsedCount: number
  savedCount: number
  newlyAddedCount: number
  duplicateCount: number
  city: string
}

const CONSOLE_COPY_SNIPPET = 'copy(document.documentElement.outerHTML)'

export const IngatlanComImportModal: FC<IngatlanComImportModalProps> = ({
  fetchedPages = null,
  onImportSuccess,
}) => {
  const { isIngatlanComImportModalOpen, setIngatlanComImportModalOpen } = useUiStore()
  const { criteria } = useFilterStore()

  const activeCity = criteria.cities?.[0] ?? criteria.city ?? 'Debrecen'
  const activeType = normalizePropertyType(criteria.propertyType)
  const startPage = typeof fetchedPages === 'number' && fetchedPages >= 1 ? Math.max(2, fetchedPages + 1) : 2
  const pageLinks = [startPage, startPage + 1, startPage + 2]
  const defaultPageUrl = buildIngatlanComSearchUrl(
    {
      city: activeCity,
      propertyType: activeType,
      minPriceMillion: criteria.minPriceMillion,
      maxPriceMillion: criteria.maxPriceMillion,
      minArea: criteria.minArea,
      maxArea: criteria.maxArea,
    },
    startPage,
  )

  const [htmlInput, setHtmlInput] = useState('')
  const [sourceUrl, setSourceUrl] = useState('')
  const [isImporting, setIsImporting] = useState(false)
  const [copiedSnippet, setCopiedSnippet] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [summary, setSummary] = useState<ImportStatsSummary | null>(null)

  if (!isIngatlanComImportModalOpen) return null

  const effectiveSourceUrl = sourceUrl.trim() || defaultPageUrl

  const handleCopySnippet = () => {
    void navigator.clipboard.writeText(CONSOLE_COPY_SNIPPET)
    setCopiedSnippet(true)
    setTimeout(() => setCopiedSnippet(false), 2000)
  }

  const runImportBatches = async (batches: ReadonlyArray<{ html: string; url: string }>) => {
    setIsImporting(true)
    setErrorMsg(null)
    try {
      let parsedCount = 0
      let savedCount = 0
      let newlyAddedCount = 0
      let duplicateCount = 0
      let resolvedCity = activeCity

      for (const batch of batches) {
        const res = await importIngatlanComHtml({
          html: batch.html,
          sourceUrl: batch.url,
          city: activeCity,
          propertyType: activeType,
        })
        parsedCount += res.parsedCount
        savedCount += res.savedCount
        newlyAddedCount += res.newlyAddedCount
        duplicateCount += res.duplicateCount
        resolvedCity = res.city
      }

      setSummary({ parsedCount, savedCount, newlyAddedCount, duplicateCount, city: resolvedCity })
      if (savedCount > 0) {
        setHtmlInput('')
        onImportSuccess?.()
      }
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Nem sikerült feldolgozni a megadott HTML forrást.',
      )
    } finally {
      setIsImporting(false)
    }
  }

  const handleSubmitPastedHtml = () => {
    if (!htmlInput.trim()) {
      setErrorMsg('Illeszd be a böngészőből másolt ingatlan.com HTML forrást vagy válassz fájlt.')
      return
    }
    void runImportBatches([{ html: htmlInput, url: effectiveSourceUrl }])
  }

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files ? Array.from(event.target.files) : []
    event.target.value = ''
    if (files.length === 0) return

    const batches: Array<{ html: string; url: string }> = []
    for (const file of files) {
      const content = await file.text()
      if (content.trim()) {
        batches.push({ html: content, url: effectiveSourceUrl })
      }
    }
    if (batches.length > 0) {
      await runImportBatches(batches)
    }
  }

  return (
    <div
      data-testid="ingatlancom-import-modal"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] shadow-2xl">
        <div className="flex items-center justify-between border-b border-[var(--border-color)] px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-sky-500/15 text-sky-400">
              <FileCode2 className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-[var(--text-primary)]">
                Ingatlan.com oldalak ({startPage}., {startPage + 1}., ...) közvetlen importálása
              </h3>
              <p className="text-xs text-[var(--text-secondary)]">
                A saját böngésződből lementett vagy kimásolt HTML oldalakat tartósan beolvasod az adatbázisba
              </p>
            </div>
          </div>

          <button
            type="button"
            aria-label="Bezárás"
            onClick={() => setIngatlanComImportModalOpen(false)}
            className="rounded-lg p-1 text-[var(--text-secondary)] hover:bg-[var(--bg-input)] hover:text-[var(--text-primary)]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          <div className="rounded-xl border border-sky-500/25 bg-sky-500/5 p-3.5 space-y-2.5">
            <p className="text-xs font-semibold text-sky-300">
              1. Nyisd meg a következő oldalakat a böngésződben ({activeCity}):
            </p>
            <div className="flex flex-wrap gap-2">
              {pageLinks.map((pageNum) => {
                const pageUrl = buildIngatlanComSearchUrl(
                  {
                    city: activeCity,
                    propertyType: activeType,
                    minPriceMillion: criteria.minPriceMillion,
                    maxPriceMillion: criteria.maxPriceMillion,
                    minArea: criteria.minArea,
                    maxArea: criteria.maxArea,
                  },
                  pageNum,
                )
                return (
                  <button
                    key={pageNum}
                    type="button"
                    data-testid={`open-ingatlancom-page-${pageNum}`}
                    onClick={() => openExternalUrl(pageUrl)}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-sky-400/30 bg-sky-500/15 px-3 py-1.5 text-xs font-semibold text-sky-200 hover:bg-sky-500/25 transition-colors cursor-pointer"
                  >
                    <ExternalLink className="h-3.5 w-3.5" />
                    <span>{pageNum}. oldal megnyitása</span>
                  </button>
                )
              })}
            </div>
            <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-sky-500/15">
              <span className="text-[11px] text-[var(--text-secondary)]">
                2. Mentsd le az oldalt (<kbd className="rounded bg-black/30 px-1">Ctrl+S</kbd> → csak HTML), vagy F12 Konzolból másold a DOM-ot:
              </span>
              <button
                type="button"
                onClick={handleCopySnippet}
                className="inline-flex items-center gap-1 rounded-md border border-[var(--border-color)] bg-[var(--bg-input)] px-2 py-1 text-[11px] font-mono text-[var(--text-primary)] hover:border-sky-400/40"
              >
                {copiedSnippet ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                <span>{copiedSnippet ? 'Másolva!' : CONSOLE_COPY_SNIPPET}</span>
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <label
              htmlFor="ingatlancom-html-file"
              className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border-2 border-dashed border-[var(--border-color)] bg-[var(--bg-input)] px-4 py-3 text-xs font-semibold text-[var(--text-primary)] hover:border-sky-500/50 transition-colors"
            >
              <Upload className="h-4 w-4 text-sky-400" />
              <span>Mentett .html fájl(ok) kiválasztása (több oldal egyszerre is)</span>
              <input
                id="ingatlancom-html-file"
                data-testid="ingatlancom-html-file-input"
                type="file"
                accept=".html,.htm,text/html"
                multiple
                disabled={isImporting}
                onChange={(e) => {
                  void handleFileChange(e)
                }}
                className="hidden"
              />
            </label>
          </div>

          <div className="space-y-2">
            <label className="block text-xs font-semibold text-[var(--text-secondary)]">
              Vagy illeszd be a kimásolt HTML forrást:
            </label>
            <textarea
              data-testid="ingatlancom-html-textarea"
              value={htmlInput}
              onChange={(e) => setHtmlInput(e.target.value)}
              rows={5}
              placeholder="<a href='/34567890' class='listing-card'>...</a>"
              className="w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-input)] p-3 font-mono text-xs text-[var(--text-primary)] focus:border-sky-500 focus:outline-none"
            />
            <input
              type="url"
              value={sourceUrl}
              onChange={(e) => setSourceUrl(e.target.value)}
              placeholder={`Forrás URL (alapértelmezett: ${defaultPageUrl})`}
              className="w-full rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-3 py-1.5 font-mono text-[11px] text-[var(--text-secondary)] focus:border-sky-500 focus:outline-none"
            />
          </div>

          {errorMsg && (
            <div
              role="alert"
              className="flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-200"
            >
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {summary && (
            <div
              data-testid="ingatlancom-import-summary"
              role="status"
              className="flex items-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-200"
            >
              <Sparkles className="h-4 w-4 text-emerald-400 shrink-0" />
              <span>
                {summary.parsedCount > 0
                  ? `${summary.city}: ${summary.parsedCount} kártya beolvasva -> ${summary.savedCount} egyedi (${summary.newlyAddedCount} új, ${summary.duplicateCount} frissítve). A lista frissült!`
                  : 'Nem található ingatlan.com hirdetéskártya a megadott HTML-ben (ellenőrizd, hogy betöltött-e a teljes találati oldal).'}
              </span>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-2 border-t border-[var(--border-color)] px-5 py-3">
          <button
            type="button"
            onClick={() => setIngatlanComImportModalOpen(false)}
            className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] px-3.5 py-1.5 text-xs font-semibold text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
          >
            Bezárás
          </button>
          <button
            type="button"
            data-testid="submit-ingatlancom-import"
            disabled={isImporting}
            onClick={handleSubmitPastedHtml}
            className="inline-flex items-center gap-1.5 rounded-lg bg-sky-600 px-4 py-1.5 text-xs font-bold text-white hover:bg-sky-500 disabled:opacity-50 transition-colors cursor-pointer"
          >
            <Upload className="h-3.5 w-3.5" />
            <span>{isImporting ? 'Importálás...' : 'Importálás és keresés frissítése'}</span>
          </button>
        </div>
      </div>
    </div>
  )
}
