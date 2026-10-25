import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { CSSProperties, ReactNode } from 'react'
import { PriceScatterCorrelationChart } from '../components/analytics/PriceScatterCorrelationChart'
import type { ScatterPoint } from '../hooks/useMarketAnalytics'

// Mock Recharts to allow custom inspection and testing in jsdom
vi.mock('recharts', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: ReactNode }) => (
      <div style={{ width: 800, height: 400 }}>{children}</div>
    ),
    ScatterChart: ({ children }: { children: ReactNode }) => (
      <div data-testid="recharts-scatter-chart">{children}</div>
    ),
    Tooltip: ({
      content,
      wrapperStyle,
    }: {
      content?: (props: unknown) => ReactNode
      wrapperStyle?: CSSProperties
    }) => (
      <div data-testid="recharts-tooltip-wrapper" style={wrapperStyle}>
        {typeof content === 'function' &&
          content({
            active: true,
            payload: [
              {
                payload: {
                  id: 'p1',
                  title: 'Teszt lakás',
                  city: 'Debrecen',
                  portal: 'dh',
                  url: 'https://dh.hu/1',
                  area: 60,
                  price: 45,
                  sqmPrice: 750000,
                  isNew: true,
                  isDiscounted: true,
                  priceDelta: -2000000,
                  isBargain: true,
                  segment: 'bargain',
                },
              },
            ],
          })}
      </div>
    ),
    Scatter: ({
      children,
      onClick,
      data,
    }: {
      children?: ReactNode
      onClick?: (node: unknown, e?: unknown) => void
      data?: ScatterPoint[]
    }) => (
      <div
        data-testid="recharts-scatter"
        data-count={data?.length}
        onClick={(e) => {
          if (data && data.length > 0) {
            onClick?.({ payload: data[0] }, e)
          }
        }}
      >
        {children}
      </div>
    ),
    Cell: ({
      onClick,
      className,
    }: {
      onClick?: (e: unknown) => void
      className?: string
    }) => (
      <span
        data-testid="recharts-cell"
        className={className}
        onClick={(e) => onClick?.(e)}
      />
    ),
    XAxis: () => <div data-testid="recharts-xaxis" />,
    YAxis: () => <div data-testid="recharts-yaxis" />,
    ZAxis: () => <div data-testid="recharts-zaxis" />,
    CartesianGrid: () => <div data-testid="recharts-grid" />,
  }
})

describe('PriceScatterCorrelationChart (#277)', () => {
  const mockScatterData: ScatterPoint[] = [
    {
      id: 'p1',
      title: 'Debreceni alkalmi lakás',
      city: 'Debrecen',
      portal: 'dh',
      url: 'https://dh.hu/1',
      area: 60,
      price: 45,
      sqmPrice: 750000,
      isNew: true,
      isDiscounted: true,
      priceDelta: -2000000,
      isBargain: true,
      segment: 'bargain',
    },
    {
      id: 'p2',
      title: 'Debreceni átlagos lakás',
      city: 'Debrecen',
      portal: 'oc',
      url: 'https://oc.hu/2',
      area: 75,
      price: 65,
      sqmPrice: 866667,
      isNew: false,
      isDiscounted: false,
      isBargain: false,
      segment: 'fair',
    },
    {
      id: 'p3',
      title: 'Debreceni prémium lakás',
      city: 'Debrecen',
      portal: 'jofogas',
      url: 'https://jofogas.hu/3',
      area: 120,
      price: 150,
      sqmPrice: 1250000,
      isNew: false,
      isDiscounted: false,
      isBargain: false,
      segment: 'premium',
    },
  ]

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders header, legend, and segment filter buttons with informative titles', () => {
    render(<PriceScatterCorrelationChart scatterData={mockScatterData} />)

    expect(screen.getByText('Ár vs. Alapterület Korreláció (Szórásdiagram)')).toBeTruthy()
    expect(screen.getByText('Összes (3)')).toBeTruthy()
    expect(screen.getByText('Alkalmi')).toBeTruthy()
    expect(screen.getByText('Piaci átlag')).toBeTruthy()
    expect(screen.getByText('Prémium')).toBeTruthy()

    // Legend items with descriptions
    expect(screen.getByText(/Helyi mediánnál ≥15%-kal olcsóbb vagy leárazott/)).toBeTruthy()
    expect(screen.getByText(/Reális piaci sáv \(-15% és \+25% között\)/)).toBeTruthy()
    expect(screen.getByText(/Helyi referenciaárnál ≥25%-kal magasabb fajlagos ár/)).toBeTruthy()

    // Title attributes for quick tooltips
    const bargainBtn = screen.getByRole('button', { name: /Alkalmi/i })
    expect(bargainBtn.getAttribute('title')).toContain('15%-kal olcsóbb')

    const fairBtn = screen.getByRole('button', { name: /Piaci átlag/i })
    expect(fairBtn.getAttribute('title')).toContain('-15% és +25% közötti')

    const premiumBtn = screen.getByRole('button', { name: /Prémium/i })
    expect(premiumBtn.getAttribute('title')).toContain('25%-kal magasabb')
  })

  it('filters data when segment buttons are clicked', () => {
    render(<PriceScatterCorrelationChart scatterData={mockScatterData} />)

    const scatterEl = screen.getByTestId('recharts-scatter')
    expect(scatterEl.getAttribute('data-count')).toBe('3')

    // Click Alkalmi
    const bargainBtn = screen.getByRole('button', { name: /Alkalmi/i })
    fireEvent.click(bargainBtn)
    expect(scatterEl.getAttribute('data-count')).toBe('1')

    // Click Piaci átlag
    const fairBtn = screen.getByRole('button', { name: /Piaci átlag/i })
    fireEvent.click(fairBtn)
    expect(scatterEl.getAttribute('data-count')).toBe('1')

    // Click Prémium
    const premiumBtn = screen.getByRole('button', { name: /Prémium/i })
    fireEvent.click(premiumBtn)
    expect(scatterEl.getAttribute('data-count')).toBe('1')

    // Click Összes
    const allBtn = screen.getByRole('button', { name: /Összes/i })
    fireEvent.click(allBtn)
    expect(scatterEl.getAttribute('data-count')).toBe('3')
  })

  it('toggles category help popover and closes with button, click-outside, and Escape key', () => {
    render(<PriceScatterCorrelationChart scatterData={mockScatterData} />)

    const helpBtn = screen.getByTestId('category-help-button')
    expect(helpBtn.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('category-help-popover')).toBeNull()

    // Open popover
    fireEvent.click(helpBtn)
    expect(helpBtn.getAttribute('aria-expanded')).toBe('true')
    const popover = screen.getByTestId('category-help-popover')
    expect(popover).toBeTruthy()
    expect(screen.getByText('Kategória besorolási szabályok')).toBeTruthy()
    expect(screen.getByText(/legalább 15%-kal olcsóbb fajlagos árú/)).toBeTruthy()
    expect(screen.getByText(/referenciaár -15% és \+25% közötti/)).toBeTruthy()
    expect(screen.getByText(/legalább 25%-kal magasabb fajlagos négyzetméterárú/)).toBeTruthy()

    // Close via close button
    const closeBtn = screen.getByRole('button', { name: /Súgó bezárása/i })
    fireEvent.click(closeBtn)
    expect(screen.queryByTestId('category-help-popover')).toBeNull()

    // Open and close via Escape key
    fireEvent.click(helpBtn)
    expect(screen.getByTestId('category-help-popover')).toBeTruthy()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByTestId('category-help-popover')).toBeNull()

    // Open and close via click outside
    fireEvent.click(helpBtn)
    expect(screen.getByTestId('category-help-popover')).toBeTruthy()
    fireEvent.mouseDown(document.body)
    expect(screen.queryByTestId('category-help-popover')).toBeNull()
  })

  it('renders tooltip with pointer-events-none to prevent flickering', () => {
    render(<PriceScatterCorrelationChart scatterData={mockScatterData} />)

    const tooltipWrapper = screen.getByTestId('recharts-tooltip-wrapper')
    // Tooltip wrapper has pointerEvents: none
    expect(tooltipWrapper.style.pointerEvents).toBe('none')

    // The inner content card also has pointer-events-none class
    const tooltipCard = tooltipWrapper.querySelector('.pointer-events-none')
    expect(tooltipCard).toBeTruthy()
    expect(screen.getByText('Teszt lakás')).toBeTruthy()
    expect(screen.getByText('60 m²')).toBeTruthy()
    expect(screen.getByText(/Kattints a megnyitáshoz/)).toBeTruthy()
  })

  it('calls onOpenListing with point url when scatter point is clicked', () => {
    const onOpenListing = vi.fn()
    render(
      <PriceScatterCorrelationChart
        scatterData={mockScatterData}
        onOpenListing={onOpenListing}
      />
    )

    const scatterEl = screen.getByTestId('recharts-scatter')
    fireEvent.click(scatterEl)

    expect(onOpenListing).toHaveBeenCalledTimes(1)
    expect(onOpenListing).toHaveBeenCalledWith('https://dh.hu/1')
  })

  it('falls back to window.open when onOpenListing prop is not provided', () => {
    const windowOpenSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
    render(<PriceScatterCorrelationChart scatterData={mockScatterData} />)

    const scatterEl = screen.getByTestId('recharts-scatter')
    fireEvent.click(scatterEl)

    expect(windowOpenSpy).toHaveBeenCalledTimes(1)
    expect(windowOpenSpy).toHaveBeenCalledWith('https://dh.hu/1', '_blank', 'noopener,noreferrer')
  })

  it('renders empty message when filtered data has 0 items', () => {
    render(<PriceScatterCorrelationChart scatterData={[]} />)

    expect(
      screen.getByText('Nincs megjeleníthető hirdetés a kiválasztott szegmensben.')
    ).toBeTruthy()
  })
})
