import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from '../components/common/ErrorBoundary'

const ProblemChild = () => {
  throw new Error('Szándékos teszthiba a renderelés közben')
}

describe('ErrorBoundary component', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <div>Minden rendben működik</div>
      </ErrorBoundary>
    )
    expect(screen.getByText('Minden rendben működik')).toBeTruthy()
  })

  it('catches render error and displays fallback UI instead of crashing', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>
    )

    expect(screen.getByText('Váratlan hiba történt a felületen')).toBeTruthy()
    expect(screen.getByText('Szándékos teszthiba a renderelés közben')).toBeTruthy()
    expect(screen.getByRole('button', { name: /Oldal újratöltése/i })).toBeTruthy()

    consoleSpy.mockRestore()
  })
})
