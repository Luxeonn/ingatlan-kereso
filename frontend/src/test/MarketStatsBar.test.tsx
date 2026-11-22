import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MarketStatsBar } from '../components/kpi/MarketStatsBar'
import { ListingsPanel } from '../components/listings/ListingsPanel'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import type { DashboardResponseDto } from '../types/api'
import { createMockIngatlanItem } from './testFixtures'

describe('MarketStatsBar component (3 interactive filter KPI cards)', () => {
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

  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      dashboardTab: 'listings',
      listingSubView: 'listings',
    })
  })

  it('returns null when data is null', () => {
    const { container } = render(<MarketStatsBar data={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders the 3 KPI filter cards without Medián m² ár card', () => {
    render(<MarketStatsBar data={sampleData} />)

    const bar = screen.getByTestId('market-stats-bar')
    expect(bar).toBeTruthy()
    expect(bar.getAttribute('role')).toBe('region')
    expect(bar.getAttribute('aria-label')).toBe('Piaci gyorsösszegző szűrők')

    expect(screen.getByText('Összes Találat')).toBeTruthy()
    expect(screen.getByText('42 db')).toBeTruthy()

    expect(screen.getByText('Új Felfedezés')).toBeTruthy()
    expect(screen.getByText('7 db')).toBeTruthy()

    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('3 db')).toBeTruthy()

    expect(screen.queryByText('Medián m² ár')).toBeNull()
  })

  it('acts as quick filter toggles when clicking Új Felfedezés, Alkalmi Vételek, and Összes Találat', () => {
    render(<MarketStatsBar data={sampleData} />)

    const allBtn = screen.getByTestId('kpi-filter-all')
    const newBtn = screen.getByTestId('kpi-filter-new')
    const bargainBtn = screen.getByTestId('kpi-filter-bargain')

    expect(allBtn.getAttribute('aria-pressed')).toBe('true')
    expect(newBtn.getAttribute('aria-pressed')).toBe('false')
    expect(bargainBtn.getAttribute('aria-pressed')).toBe('false')

    // Click Új Felfedezés -> enables onlyNew
    fireEvent.click(newBtn)
    expect(useFilterStore.getState().onlyNew).toBe(true)
    expect(newBtn.getAttribute('aria-pressed')).toBe('true')
    expect(allBtn.getAttribute('aria-pressed')).toBe('false')

    // Click Alkalmi Vételek -> enables onlyBargain
    fireEvent.click(bargainBtn)
    expect(useFilterStore.getState().onlyBargain).toBe(true)
    expect(bargainBtn.getAttribute('aria-pressed')).toBe('true')

    // Click Összes Találat -> resets both filters
    fireEvent.click(allBtn)
    expect(useFilterStore.getState().onlyNew).toBe(false)
    expect(useFilterStore.getState().onlyBargain).toBe(false)
    expect(allBtn.getAttribute('aria-pressed')).toBe('true')
  })

  it('handles missing marketStats and missing counts gracefully with 0 db', () => {
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
  })

  it('falls back to counting bargains from items when marketStats.bargainCount is omitted', () => {
    const fallbackData: DashboardResponseDto = {
      totalFound: 4,
      newCount: 1,
      items: [
        createMockIngatlanItem({ id: 'b-1', isBargain: true }),
        createMockIngatlanItem({ id: 'b-2', isBargain: true }),
        createMockIngatlanItem({ id: 'b-3', isBargain: false }),
      ],
    }
    render(<MarketStatsBar data={fallbackData} />)

    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('2 db')).toBeTruthy()
  })

  it('renders median sqm price inside ListingsPanel multi-source summary notice', () => {
    const items = [
      createMockIngatlanItem({ id: 'm-1', url: 'https://example.com/m-1', sourcesCount: 2 }),
    ]
    render(
      <ListingsPanel
        items={items}
        totalRawCount={1}
        serverFilteredCount={1}
        medianPricePerSqm={778571}
        isLoading={false}
        hasSearched={true}
        onMarkViewed={() => {}}
        onOpenAll={() => {}}
        onTriggerSearch={() => {}}
      />,
    )

    const notice = screen.getByTestId('multi-source-summary-notice')
    expect(notice.textContent).toContain('Medián m² ár:')
    expect(notice.textContent).toMatch(/778\s571 Ft\/m²/)
  })
})
