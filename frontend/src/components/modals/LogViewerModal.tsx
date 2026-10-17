import { useState, useRef, useEffect, type FC } from 'react'
import { X, Copy, Trash2, Check, RefreshCw, Terminal } from 'lucide-react'
import { useLogs } from '../../hooks/useLogs'
import { useUiStore } from '../../store/useUiStore'
import { cn } from '../../utils/cn'

export const LogViewerModal: FC = () => {
  const { isLogModalOpen, setLogModalOpen } = useUiStore()
  const [level, setLevel] = useState<string>('ALL')
  const [copied, setCopied] = useState(false)
  const [autoScroll, setAutoScroll] = useState(true)
  const terminalEndRef = useRef<HTMLDivElement>(null)

  const { data: logData, isLoading, refetch, clearLogs, isClearing } = useLogs(200, level, isLogModalOpen)

  useEffect(() => {
    if (autoScroll && terminalEndRef.current) {
      terminalEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [logData?.logs, autoScroll])

  if (!isLogModalOpen) return null

  const handleCopy = () => {
    const text = logData?.rawLogs?.join('\n') || ''
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="flex h-[85vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[#0d1117] text-gray-200 shadow-2xl">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-gray-800 bg-[#161b22] px-4 py-3">
          <div className="flex items-center gap-2">
            <Terminal className="h-5 w-5 text-blue-400" />
            <h3 className="text-sm font-bold text-white">Rendszernapló (System Log Viewer)</h3>
            {logData?.level && (
              <span className="rounded bg-blue-500/10 px-2 py-0.5 text-[10px] font-mono text-blue-400 border border-blue-500/20">
                Level: {logData.level}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* Level filters */}
            <div className="hidden sm:flex items-center gap-1 rounded-lg bg-black/30 p-1 border border-gray-800 text-xs">
              {['ALL', 'INFO', 'WARN', 'ERROR'].map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setLevel(lvl)}
                  className={cn(
                    'rounded px-2 py-0.5 text-[10px] font-semibold transition-colors',
                    level === lvl ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'
                  )}
                >
                  {lvl}
                </button>
              ))}
            </div>

            {/* Refresh */}
            <button
              type="button"
              onClick={() => refetch()}
              title="Frissítés"
              className="rounded p-1 text-gray-400 hover:text-white hover:bg-gray-800"
            >
              <RefreshCw className={cn('h-4 w-4', isLoading && 'animate-spin')} />
            </button>

            {/* Copy */}
            <button
              type="button"
              onClick={handleCopy}
              title="Másolás vágólapra"
              className="flex items-center gap-1 rounded bg-gray-800 px-2.5 py-1 text-xs font-semibold text-gray-200 hover:bg-gray-700 transition-colors"
            >
              {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
              <span>{copied ? 'Másolva!' : 'Másolás'}</span>
            </button>

            {/* Clear */}
            <button
              type="button"
              onClick={() => clearLogs()}
              disabled={isClearing}
              title="Naplók ürítése"
              className="rounded p-1 text-gray-400 hover:text-rose-400 hover:bg-gray-800"
            >
              <Trash2 className="h-4 w-4" />
            </button>

            {/* Close */}
            <button
              type="button"
              aria-label="Bezárás"
              onClick={() => setLogModalOpen(false)}
              className="rounded p-1 text-gray-400 hover:text-white hover:bg-gray-800"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Terminal logs body */}
        <div className="flex-1 overflow-y-auto p-4 font-mono text-xs leading-relaxed">
          {logData?.logs && logData.logs.length > 0 ? (
            logData.logs.map((entry, idx) => {
              const lvl = entry.level || 'INFO'
              const color =
                lvl === 'ERROR'
                  ? 'text-rose-400'
                  : lvl === 'WARN'
                  ? 'text-amber-400'
                  : lvl === 'DEBUG'
                  ? 'text-gray-500'
                  : 'text-emerald-400'

              return (
                <div key={idx} className="flex items-start gap-2 py-0.5 hover:bg-white/[0.02]">
                  <span className="text-gray-500 shrink-0">{entry.timestamp?.split(' ')[1] || ''}</span>
                  <span className={cn('font-bold shrink-0 w-14', color)}>[{lvl}]</span>
                  <span className="text-gray-400 shrink-0 truncate max-w-[150px] hidden md:inline">
                    {entry.loggerName?.split('.').pop()}
                  </span>
                  <span className="text-gray-200 break-all">{entry.message}</span>
                </div>
              )
            })
          ) : (
            <div className="flex h-full items-center justify-center text-gray-500">
              {isLoading ? 'Naplók betöltése...' : 'Nincsenek rögzített naplóbejegyzések.'}
            </div>
          )}
          <div ref={terminalEndRef} />
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-gray-800 bg-[#161b22] px-4 py-2 text-[11px] text-gray-400">
          <div className="truncate">
            {logData?.logFilePath ? `Naplófájl: ${logData.logFilePath}` : 'Memória puffer'}
          </div>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              className="rounded border-gray-700 bg-gray-800 text-blue-600 focus:ring-0"
            />
            <span>Automatikus legörgetés</span>
          </label>
        </div>
      </div>
    </div>
  )
}
