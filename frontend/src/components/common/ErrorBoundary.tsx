import { Component, type ReactNode, type ErrorInfo } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('Unhandled UI Exception caught by ErrorBoundary:', error, errorInfo)
  }

  handleReload = (): void => {
    window.location.reload()
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--bg-body,#0f172a)] p-6 text-[var(--text-primary,#f8fafc)]">
          <div className="w-full max-w-md rounded-2xl border border-red-500/30 bg-[var(--bg-card,#1e293b)] p-8 text-center shadow-2xl backdrop-blur-md">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-500/10 text-red-400">
              <AlertTriangle className="h-8 w-8" />
            </div>
            <h2 className="text-lg font-bold text-[var(--text-primary,#f8fafc)]">
              Váratlan hiba történt a felületen
            </h2>
            <p className="mt-2 text-xs text-[var(--text-secondary,#94a3b8)]">
              Az alkalmazás futása közben váratlan hiba lépett fel. Próbáld meg újratölteni az oldalt!
            </p>
            {this.state.error?.message && (
              <div className="mt-4 rounded-lg bg-black/40 p-3 text-left font-mono text-[11px] text-red-300 overflow-x-auto">
                {this.state.error.message}
              </div>
            )}
            <button
              type="button"
              onClick={this.handleReload}
              className="mt-6 inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-blue-500/25 transition-all hover:bg-blue-500"
            >
              <RefreshCw className="h-4 w-4" />
              Oldal újratöltése
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
