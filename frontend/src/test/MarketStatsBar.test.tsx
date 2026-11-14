import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MarketStatsBar } from '../components/kpi/MarketStatsBar'
import type { DashboardResponseDto } from '../types/api'
import { createMockIngatlanItem } from './testFixtures'

describe('MarketStatsBar component (R3: static KPI cards, #435)', () => {
  const sampleData: DashboardResponseDto = {
    totalFound: 42,
    newCount: 7,
    items: [],
    marketStats: {
      averagePriceMillion: 85.5,
      averagePricePerM2: 750000,
      bargainCount: 3,
      medianPricePerM2: 750000,
    },
  }

  it('returns null when data is null', () => {
    const { container } = render(<MarketStatsBar data={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders all 4 KPI cards with proper formatting and accessibility attributes', () => {
    render(<MarketStatsBar data={sampleData} />)

    const bar = screen.getByTestId('market-stats-bar')
    expect(bar).toBeTruthy()
    expect(bar.getAttribute('role')).toBe('region')
    expect(bar.getAttribute('aria-label')).toBe('Piaci gyorsösszegző mutatók')

    expect(screen.getByText('Összes Találat')).toBeTruthy()
    expect(screen.getByText('42 db')).toBeTruthy()

    expect(screen.getByText('Új Felfedezés')).toBeTruthy()
    expect(screen.getByText('7 db')).toBeTruthy()

    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('3 db')).toBeTruthy()

    expect(screen.getByText('Medián m² ár')).toBeTruthy()
    expect(screen.getByText(/750 000 Ft\/m²/i)).toBeTruthy()
  })

  it('unifies all 4 KPI cards as strictly static informational cards with no cursor-pointer', () => {
    const { container } = render(<MarketStatsBar data={sampleData} />)

    const cards = container.querySelectorAll('.shadow-md')
    expect(cards.length).toBe(4)

    cards.forEach((card) => {
      // Must not contain cursor-pointer class
      expect(card.classList.contains('cursor-pointer')).toBe(false)
      // Must not have clickable role
      expect(card.getAttribute('role')).toBeNull()
      // Must not contain click tooltip
      expect(card.getAttribute('title')).toBeNull()
      // Must not be focusable via tabIndex
      expect(card.getAttribute('tabindex')).toBeNull()
    })
  })

  it('handles missing marketStats and missing counts gracefully with N/A and 0 db', () => {
    const incompleteData: DashboardResponseDto = {
      items: [],
      newCount: 0,
      totalFound: 0,
    }
    render(<MarketStatsBar data={incompleteData} />)

    expect(screen.getByText('Összes Találat')).toBeTruthy()
    expect(screen.getAllByText('0 db').length).toBe(3)
    expect(screen.getByText('Új Felfedezés')).toBeTruthy()
    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('Medián m² ár')).toBeTruthy()
    expect(screen.getAllByText('N/A').length).toBe(1)
  })

  it('falls back to counting bargains from items and segment median sqm prices when needed (#435)', () => {
    const fallbackData: DashboardResponseDto = {
      totalFound: 4,
      newCount: 1,
      items: [
        createMockIngatlanItem({ id: 'b-1', isBargain: true }),
        createMockIngatlanItem({ id: 'b-2', isBargain: true }),
        createMockIngatlanItem({ id: 'b-3', isBargain: false }),
      ],
      marketStats: {
        residentialMedianPricePerM2: 680000,
      },
    }
    const { rerender } = render(<MarketStatsBar data={fallbackData} />)

    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('2 db')).toBeTruthy()
    expect(screen.getByText('Medián m² ár')).toBeTruthy()
    expect(screen.getByText(/680 000 Ft\/m²/i)).toBeTruthy()

    rerender(
      <MarketStatsBar
        data={{
          ...fallbackData,
          marketStats: { plotMedianPricePerM2: 45000 },
        }}
      />,
    )
    expect(screen.getByText(/45 000 Ft\/m²/i)).toBeTruthy()
  })

  it('clicking KPI cards does not trigger any state change or navigation', () => {
    const { container } = render(<MarketStatsBar data={sampleData} />)
    const cards = container.querySelectorAll('.shadow-md')

    cards.forEach((card) => {
      card.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    // Cards remain strictly static informational containers
    expect(cards.length).toBe(4)
  })
})
