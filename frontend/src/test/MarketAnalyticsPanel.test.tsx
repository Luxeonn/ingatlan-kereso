import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MarketAnalyticsPanel } from '../components/analytics/MarketAnalyticsPanel'
import type { IngatlanItem } from '../types/property'

// Mock Recharts responsive container to avoid rendering size issues in jsdom
vi.mock('recharts', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 800, height: 400 }}>{children}</div>
    ),
  }
})

describe('MarketAnalyticsPanel component (#263 & #264)', () => {
  const mockItems: IngatlanItem[] = [
    // 5 apartments
    {
      id: 'l1',
      portal: 'dh',
      url: 'https://example.com/l1',
      title: 'Lakás 1',
      price: '40 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 40, amountHuf: 40_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 800_000 },
      priceDelta: -2_000_000,
      priceChangePercent: -4.8,
    },
    {
      id: 'l2',
      portal: 'dh',
      url: 'https://example.com/l2',
      title: 'Lakás 2',
      price: '45 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 45, amountHuf: 45_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 900_000 },
    },
    {
      id: 'l3',
      portal: 'oc',
      url: 'https://example.com/l3',
      title: 'Lakás 3',
      price: '50 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 50, amountHuf: 50_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 1_000_000 },
    },
    {
      id: 'l4',
      portal: 'oc',
      url: 'https://example.com/l4',
      title: 'Lakás 4',
      price: '55 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 55, amountHuf: 55_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 1_100_000 },
    },
    {
      id: 'l5',
      portal: 'jofogas',
      url: 'https://example.com/l5',
      title: 'Lakás 5',
      price: '60 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 60, amountHuf: 60_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 1_200_000 },
    },
    // 5 plots
    {
      id: 't1',
      portal: 'dh',
      url: 'https://example.com/t1',
      title: 'Telek 1',
      price: '20 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'TELEK',
      priceInfo: { amountMillion: 20, amountHuf: 20_000_000 },
      detailsInfo: { areaSquareMeters: 1000, pricePerSquareMeter: 20_000 },
    },
    {
      id: 't2',
      portal: 'dh',
      url: 'https://example.com/t2',
      title: 'Telek 2',
      price: '25 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'TELEK',
      priceInfo: { amountMillion: 25, amountHuf: 25_000_000 },
      detailsInfo: { areaSquareMeters: 1000, pricePerSquareMeter: 25_000 },
    },
    {
      id: 't3',
      portal: 'oc',
      url: 'https://example.com/t3',
      title: 'Telek 3',
      price: '30 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'TELEK',
      priceInfo: { amountMillion: 30, amountHuf: 30_000_000 },
      detailsInfo: { areaSquareMeters: 1000, pricePerSquareMeter: 30_000 },
    },
    {
      id: 't4',
      portal: 'oc',
      url: 'https://example.com/t4',
      title: 'Telek 4',
      price: '35 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'TELEK',
      priceInfo: { amountMillion: 35, amountHuf: 35_000_000 },
      detailsInfo: { areaSquareMeters: 1000, pricePerSquareMeter: 35_000 },
    },
    {
      id: 't5',
      portal: 'jofogas',
      url: 'https://example.com/t5',
      title: 'Telek 5',
      price: '40 M Ft',
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'TELEK',
      priceInfo: { amountMillion: 40, amountHuf: 40_000_000 },
      detailsInfo: { areaSquareMeters: 1000, pricePerSquareMeter: 40_000 },
    },
  ]

  it('renders relevant KPI cards and dynamically hides 0-sample categories (#263, #264, #276)', () => {
    render(<MarketAnalyticsPanel items={mockItems} />)

    expect(screen.getByTestId('market-analytics-panel')).toBeTruthy()

    // 1. Lakás card (5 items >= 5)
    const lakasCard = screen.getByTestId('kpi-card-lakas')
    expect(lakasCard.textContent).toMatch(/1\s*000\s*000\s*Ft\/m²/)
    expect(lakasCard.textContent).toContain('5 db lakás alapján')

    // 2. Ház card (0 items in current results -> dynamically hidden per #276)
    expect(screen.queryByTestId('kpi-card-haz')).toBeNull()

    // 3. Telek card (strictly separated from living area, 5 items >= 5)
    const telekCard = screen.getByTestId('kpi-card-telek')
    expect(telekCard.textContent).toMatch(/30\s*000\s*Ft\/m²/)
    expect(telekCard.textContent).toContain('5 db telek alapján')

    // 4. Avg price card
    expect(screen.getByTestId('kpi-card-avg-price')).toBeTruthy()

    // 5. Total samples card
    const samplesCard = screen.getByTestId('kpi-card-total-samples')
    expect(samplesCard.textContent).toContain('10 db')

    // 6. Price drops card
    const dropsCard = screen.getByTestId('kpi-card-price-drops')
    expect(dropsCard.textContent).toContain('1 db')
  })

  it('renders insufficient data warning banner when total items < 5', () => {
    const fewItems = mockItems.slice(0, 3)
    render(<MarketAnalyticsPanel items={fewItems} />)

    const banner = screen.getByTestId('insufficient-data-banner')
    expect(banner).toBeTruthy()
    expect(banner.textContent).toMatch(/Kevés minta az elemzéshez/i)
  })

  it('dynamically highlights Ház category and suppresses warning banner when house sample >= 5 (#276)', () => {
    const mockHouses: IngatlanItem[] = Array.from({ length: 6 }, (_, i) => ({
      id: `house-${i}`,
      portal: 'oc',
      url: `https://example.com/h${i}`,
      title: `Családi Ház ${i + 1}`,
      price: `${70 + i * 5} M Ft`,
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'HAZ',
      priceInfo: { amountMillion: 70 + i * 5, amountHuf: (70 + i * 5) * 1_000_000 },
      detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 700_000 + i * 50_000 },
    }))

    render(<MarketAnalyticsPanel items={mockHouses} targetPropertyType="haz" />)

    // Insufficient data banner must NOT be shown when 6 houses are present
    expect(screen.queryByTestId('insufficient-data-banner')).toBeNull()

    // Ház card must be rendered and highlighted as primary stat
    const hazCard = screen.getByTestId('kpi-card-haz')
    expect(hazCard).toBeTruthy()
    expect(hazCard.textContent).toContain('Ház Medián Fajlagos Ár')
    expect(hazCard.textContent).toContain('Kiemelt')
    expect(hazCard.textContent).toContain('6 db ház alapján')
    expect(hazCard.textContent).toMatch(/Ft\/m²/)

    // Irrelevant 0-sample categories must remain hidden
    expect(screen.queryByTestId('kpi-card-lakas')).toBeNull()
    expect(screen.queryByTestId('kpi-card-telek')).toBeNull()
  })

  it('activates insufficient data warning when targeted category has < 5 samples (#276)', () => {
    const fewHouses: IngatlanItem[] = Array.from({ length: 3 }, (_, i) => ({
      id: `house-few-${i}`,
      portal: 'dh',
      url: `https://example.com/hf${i}`,
      title: `Kis Családi Ház ${i + 1}`,
      price: `${60 + i * 5} M Ft`,
      location: 'Szeged',
      city: 'Szeged',
      propertyType: 'HAZ',
      priceInfo: { amountMillion: 60 + i * 5, amountHuf: (60 + i * 5) * 1_000_000 },
      detailsInfo: { areaSquareMeters: 90, pricePerSquareMeter: 650_000 },
    }))

    render(<MarketAnalyticsPanel items={fewHouses} targetPropertyType="haz" />)

    // Warning banner MUST be displayed for sample < 5
    const banner = screen.getByTestId('insufficient-data-banner')
    expect(banner).toBeTruthy()
    expect(banner.textContent).toContain('Kevés minta az elemzéshez')

    // Ház card displays <5 guard
    const hazCard = screen.getByTestId('kpi-card-haz')
    expect(hazCard.textContent).toContain('Kevés minta (<5)')
    expect(hazCard.textContent).toContain('3 db ház alapján')
  })

  it('renders clean empty state when items list is empty (#276)', () => {
    render(<MarketAnalyticsPanel items={[]} />)

    expect(screen.getByTestId('market-analytics-empty-state')).toBeTruthy()
    expect(screen.queryByTestId('insufficient-data-banner')).toBeNull()
    expect(screen.queryByTestId('kpi-card-lakas')).toBeNull()
    expect(screen.queryByTestId('kpi-card-haz')).toBeNull()
  })

  it('renders top price drops showcase cards and triggers onMarkViewed', () => {
    const onMarkViewed = vi.fn()
    render(<MarketAnalyticsPanel items={mockItems} onMarkViewed={onMarkViewed} />)

    const priceDropSection = screen.getByTestId('top-price-drops-section')
    expect(priceDropSection).toBeTruthy()

    const dropCards = screen.getAllByTestId('price-drop-card')
    expect(dropCards.length).toBeGreaterThan(0)
    expect(dropCards[0].textContent).toContain('Lakás 1')
    expect(dropCards[0].textContent).toContain('-5%')

    const openLink = screen.getByRole('link', { name: /Megnyitás/i })
    fireEvent.click(openLink)
    expect(onMarkViewed).toHaveBeenCalledWith('https://example.com/l1')
  })

  it('switches tabs in BargainHunterTable between below market and price drops and toggles distribution metric', () => {
    render(<MarketAnalyticsPanel items={mockItems} />)

    const priceDropsTab = screen.getByRole('button', { name: /Leárazások/i })
    fireEvent.click(priceDropsTab)
    expect(screen.getByText(/Árcsökkenés: -2/i)).toBeTruthy()

    const priceToggleBtn = screen.getByRole('button', { name: /Kínálati ár \(M Ft\)/i })
    fireEvent.click(priceToggleBtn)
    expect(screen.getByText(/Kínálati Ár Eloszlás \(M Ft\)/i)).toBeTruthy()
  })
})
