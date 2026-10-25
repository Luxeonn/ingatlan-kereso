import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MarketAnalyticsDashboard } from '../components/analytics/MarketAnalyticsDashboard'
import { IngatlanItem } from '../types/property'

// Mock Recharts ResponsiveContainer for jsdom environment
vi.mock('recharts', async () => {
  const original = await vi.importActual<Record<string, unknown>>('recharts')
  return {
    ...original,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 800, height: 400 }}>{children}</div>
    ),
  }
})

describe('MarketAnalyticsDashboard component', () => {
  const mockItems: IngatlanItem[] = [
    {
      id: 'test-1',
      portal: 'Duna House',
      url: 'https://dh.hu/1',
      title: 'Szép felújított lakás Debrecen belvárosában',
      price: '75 M Ft',
      location: 'Debrecen',
      city: 'Debrecen',
      isNew: true,
      priceInfo: {
        amountMillion: 75.0,
        amountHuf: 75000000,
      },
      detailsInfo: {
        areaSquareMeters: 85,
        pricePerSquareMeter: 882352,
      },
    },
    {
      id: 'test-2',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/2',
      title: 'Kiváló ár-érték arányú lakás',
      price: '55 M Ft',
      location: 'Debrecen',
      city: 'Debrecen',
      isNew: false,
      priceDelta: -3000000,
      priceChangePercent: -5.1,
      priceInfo: {
        amountMillion: 55.0,
        amountHuf: 55000000,
      },
      detailsInfo: {
        areaSquareMeters: 90,
        pricePerSquareMeter: 611111,
      },
    },
    {
      id: 'test-3',
      portal: 'Költözzbe',
      url: 'https://koltozzbe.hu/3',
      title: 'Családi ház Hajdúsámsonban',
      price: '65 M Ft',
      location: 'Hajdúsámson',
      city: 'Hajdúsámson',
      isNew: true,
      priceInfo: {
        amountMillion: 65.0,
        amountHuf: 65000000,
      },
      detailsInfo: {
        areaSquareMeters: 120,
        pricePerSquareMeter: 541666,
      },
    },
  ]

  it('renders empty message when no items are provided', () => {
    render(<MarketAnalyticsDashboard items={[]} />)
    expect(screen.getByText('Nincs elegendő adat a piaci elemzéshez')).toBeTruthy()
  })

  it('renders all KPI summary cards with calculated values', () => {
    render(<MarketAnalyticsDashboard items={mockItems} />)

    expect(screen.getByText('Medián Kínálati Ár')).toBeTruthy()
    expect(screen.getByText('Medián Fajlagos Ár')).toBeTruthy()
    expect(screen.getByText('Alkalmi Vételek')).toBeTruthy()
    expect(screen.getByText('Kínálati Dinamika')).toBeTruthy()

    // 3 items in total
    expect(screen.getByText('3 db')).toBeTruthy()
  })

  it('renders chart titles and chart components properly', () => {
    render(<MarketAnalyticsDashboard items={mockItems} />)

    expect(screen.getByText(/Négyzetméterár Eloszlás/i)).toBeTruthy()
    expect(screen.getByText(/Település-összehasonlító Diagram/i)).toBeTruthy()
    expect(screen.getByText(/Ár vs. Alapterület Korreláció/i)).toBeTruthy()
    expect(screen.getByText(/Alkalmi Vétel és Árleszállítás Figyelő/i)).toBeTruthy()
  })

  it('switches tabs in BargainHunterTable between below market and price drops', () => {
    render(<MarketAnalyticsDashboard items={mockItems} />)

    const priceDropsTab = screen.getByRole('button', { name: /Leárazások/i })
    fireEvent.click(priceDropsTab)

    expect(screen.getByText(/Árcsökkenés: -3/i)).toBeTruthy()
    expect(screen.getByText('Kiváló ár-érték arányú lakás')).toBeTruthy()
  })

  it('toggles metric in PriceDistributionChart between sqm price and total price', () => {
    render(<MarketAnalyticsDashboard items={mockItems} />)

    const priceToggleBtn = screen.getByRole('button', { name: /Kínálati ár \(M Ft\)/i })
    fireEvent.click(priceToggleBtn)

    expect(screen.getByText(/Kínálati Ár Eloszlás \(M Ft\)/i)).toBeTruthy()
  })

  it('navigates back to listings view when clicking Hirdetéskártyák Megtekintése quick link', async () => {
    const { useUiStore } = await import('../store/useUiStore')
    useUiStore.setState({ dashboardTab: 'analytics' })

    render(<MarketAnalyticsDashboard items={mockItems} />)

    const viewListingsBtn = screen.getByRole('button', { name: /Hirdetéskártyák Megtekintése/i })
    fireEvent.click(viewListingsBtn)

    expect(useUiStore.getState().dashboardTab).toBe('listings')
  })
})
