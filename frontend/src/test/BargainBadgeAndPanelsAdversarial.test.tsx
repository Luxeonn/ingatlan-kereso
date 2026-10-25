import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { BargainBadge } from '../components/listings/BargainBadge'
import { MarketAnalyticsPanel } from '../components/analytics/MarketAnalyticsPanel'
import { ListingsPanel } from '../components/listings/ListingsPanel'
import { App } from '../App'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore } from '../store/useUiStore'
import type { IngatlanItem } from '../types/property'

// Mock Recharts responsive container for jsdom environment
vi.mock('recharts', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 800, height: 400 }}>{children}</div>
    ),
  }
})

// Mock window virtualizer for ListingsGrid in jsdom
vi.mock('@tanstack/react-virtual', () => ({
  useWindowVirtualizer: (options: { count: number }) => ({
    getVirtualItems: () =>
      Array.from({ length: options.count }, (_, index) => ({
        index,
        start: index * 100,
        size: 100,
        key: String(index),
      })),
    getTotalSize: () => options.count * 100,
    scrollToIndex: vi.fn(),
  }),
}))

describe('Challenger R4-2: BargainBadge, MarketAnalyticsPanel, ListingsPanel & App Adversarial Suite', () => {
  const baseBargainItem: IngatlanItem = {
    id: 'prop-bargain-1',
    portal: 'dh',
    url: 'https://example.com/prop/bargain-1',
    title: 'Kiváló adottságú debreceni lakás',
    price: '36 M Ft',
    location: 'Debrecen, Belváros',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isBargain: true,
    percentageBelowMedian: 28.0,
    referenceMedianPricePerSquareMeter: 1_000_000,
    referenceSampleSize: 12,
    priceInfo: {
      amountMillion: 36,
      amountHuf: 36_000_000,
    },
    detailsInfo: {
      areaSquareMeters: 50,
      pricePerSquareMeter: 720_000,
      roomCount: 2,
    },
  }

  /* -------------------------------------------------------------------------- */
  /* 1. BargainBadge Edge Cases                                                  */
  /* -------------------------------------------------------------------------- */
  describe('1. BargainBadge Edge Cases', () => {
    it('strictly omits badge from DOM when sample size < 5 (sampleSize=4, 0, -1, undefined)', () => {
      // sampleSize = 4
      const { container: c4 } = render(
        <BargainBadge item={{ ...baseBargainItem, referenceSampleSize: 4 }} />
      )
      expect(c4.firstChild).toBeNull()

      // sampleSize = 0
      const { container: c0 } = render(
        <BargainBadge item={{ ...baseBargainItem, referenceSampleSize: 0 }} />
      )
      expect(c0.firstChild).toBeNull()

      // sampleSize = -1
      const { container: cNeg } = render(
        <BargainBadge item={{ ...baseBargainItem, referenceSampleSize: -1 }} />
      )
      expect(cNeg.firstChild).toBeNull()

      // sampleSize undefined and bargainInfo undefined
      const { container: cUndef } = render(
        <BargainBadge
          item={{
            ...baseBargainItem,
            referenceSampleSize: undefined,
            bargainInfo: undefined,
          }}
        />
      )
      expect(cUndef.firstChild).toBeNull()

      // sampleSize = 5: boundary inclusion
      const { container: c5 } = render(
        <BargainBadge item={{ ...baseBargainItem, referenceSampleSize: 5 }} />
      )
      expect(c5.firstChild).not.toBeNull()
      expect(screen.getByTestId('bargain-badge-button')).toBeTruthy()
    })

    it('strictly omits badge if percentageBelowMedian is zero, negative, or null', () => {
      const { container: cZero } = render(
        <BargainBadge item={{ ...baseBargainItem, percentageBelowMedian: 0 }} />
      )
      expect(cZero.firstChild).toBeNull()

      const { container: cNeg } = render(
        <BargainBadge item={{ ...baseBargainItem, percentageBelowMedian: -15 }} />
      )
      expect(cNeg.firstChild).toBeNull()

      const { container: cNull } = render(
        <BargainBadge item={{ ...baseBargainItem, percentageBelowMedian: undefined }} />
      )
      expect(cNull.firstChild).toBeNull()
    })

    it('handles rapid click sequences deterministically without race conditions or stuck state', () => {
      render(<BargainBadge item={baseBargainItem} />)
      const button = screen.getByTestId('bargain-badge-button')

      // Perform 7 rapid clicks
      for (let i = 1; i <= 7; i++) {
        fireEvent.click(button)
      }
      // 7 clicks (odd) -> popover must be OPEN
      expect(button.getAttribute('aria-expanded')).toBe('true')
      expect(screen.getByTestId('bargain-popover')).toBeTruthy()

      // Perform 3 more clicks (total 10, even) -> popover must be CLOSED
      for (let i = 1; i <= 3; i++) {
        fireEvent.click(button)
      }
      expect(button.getAttribute('aria-expanded')).toBe('false')
      expect(screen.queryByTestId('bargain-popover')).toBeNull()
    })

    it('supports full keyboard navigation (Enter, Space, Escape) and proper ARIA dialog semantics', () => {
      render(<BargainBadge item={baseBargainItem} />)
      const button = screen.getByTestId('bargain-badge-button')

      // Initial state
      expect(button.getAttribute('role')).toBe('button')
      expect(button.getAttribute('tabindex')).toBe('0')
      expect(button.getAttribute('aria-haspopup')).toBe('dialog')
      expect(button.getAttribute('aria-expanded')).toBe('false')
      const popoverId = button.getAttribute('aria-controls')
      expect(popoverId).toBeTruthy()

      // Press Enter to open
      fireEvent.keyDown(button, { key: 'Enter' })
      expect(button.getAttribute('aria-expanded')).toBe('true')
      const popover = screen.getByTestId('bargain-popover')
      expect(popover).toBeTruthy()
      expect(popover.getAttribute('role')).toBe('dialog')
      expect(popover.getAttribute('id')).toBe(popoverId)

      // Press Enter to close
      fireEvent.keyDown(button, { key: 'Enter' })
      expect(button.getAttribute('aria-expanded')).toBe('false')
      expect(screen.queryByTestId('bargain-popover')).toBeNull()

      // Press Space to open
      fireEvent.keyDown(button, { key: ' ' })
      expect(button.getAttribute('aria-expanded')).toBe('true')
      expect(screen.getByTestId('bargain-popover')).toBeTruthy()

      // Press Escape to dismiss
      fireEvent.keyDown(button, { key: 'Escape' })
      expect(button.getAttribute('aria-expanded')).toBe('false')
      expect(screen.queryByTestId('bargain-popover')).toBeNull()
    })

    it('prevents click bubbling and keyboard bubbling to parent card or container', () => {
      const parentClickHandler = vi.fn()
      const windowOpenSpy = vi.spyOn(window, 'open').mockImplementation(() => null)

      render(
        <div
          data-testid="parent-clickable-card"
          onClick={() => {
            parentClickHandler()
            window.open('https://example.com/should-not-open', '_blank')
          }}
        >
          <BargainBadge item={baseBargainItem} />
        </div>
      )

      const badgeButton = screen.getByTestId('bargain-badge-button')

      // 1. Click badge button
      fireEvent.click(badgeButton)
      expect(parentClickHandler).not.toHaveBeenCalled()
      expect(windowOpenSpy).not.toHaveBeenCalled()
      expect(screen.getByTestId('bargain-popover')).toBeTruthy()

      // 2. Click inside the open popover
      const popover = screen.getByTestId('bargain-popover')
      fireEvent.click(popover)
      expect(parentClickHandler).not.toHaveBeenCalled()
      expect(windowOpenSpy).not.toHaveBeenCalled()

      // 3. Press Enter on button
      fireEvent.keyDown(badgeButton, { key: 'Enter' })
      expect(parentClickHandler).not.toHaveBeenCalled()
      expect(windowOpenSpy).not.toHaveBeenCalled()

      // 4. Press Escape on button
      fireEvent.keyDown(badgeButton, { key: 'Escape' })
      expect(parentClickHandler).not.toHaveBeenCalled()
      expect(windowOpenSpy).not.toHaveBeenCalled()

      windowOpenSpy.mockRestore()
    })

    it('verifies mobile screen responsive bounds and dismissal on outside click', () => {
      render(
        <div>
          <span data-testid="outside-element">Kívül eső terület</span>
          <BargainBadge item={baseBargainItem} />
        </div>
      )

      const button = screen.getByTestId('bargain-badge-button')
      fireEvent.click(button)

      const popover = screen.getByTestId('bargain-popover')
      expect(popover).toBeTruthy()

      // Mobile clipping guard: max-w-[calc(100vw-2.5rem)] ensures viewport boundary containment
      expect(popover.className).toContain('max-w-[calc(100vw-2.5rem)]')
      expect(popover.className).toContain('z-50')

      // Dismiss on mousedown outside
      fireEvent.mouseDown(screen.getByTestId('outside-element'))
      expect(screen.queryByTestId('bargain-popover')).toBeNull()
      expect(button.getAttribute('aria-expanded')).toBe('false')
    })
  })

  /* -------------------------------------------------------------------------- */
  /* 2. MarketAnalyticsPanel Edge Cases                                          */
  /* -------------------------------------------------------------------------- */
  describe('2. MarketAnalyticsPanel Edge Cases', () => {
    const mockApartments: IngatlanItem[] = Array.from({ length: 6 }, (_, i) => ({
      id: `apt-${i}`,
      portal: 'dh',
      url: `https://example.com/apt/${i}`,
      title: `Debreceni lakás ${i + 1}`,
      price: `${40 + i * 5} M Ft`,
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountMillion: 40 + i * 5, amountHuf: (40 + i * 5) * 1_000_000 },
      detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 800_000 + i * 50_000 },
      priceDelta: i === 0 ? -3_000_000 : 0,
      priceChangePercent: i === 0 ? -7.0 : 0,
    }))

    const mockHouses: IngatlanItem[] = Array.from({ length: 2 }, (_, i) => ({
      id: `house-${i}`,
      portal: 'oc',
      url: `https://example.com/house/${i}`,
      title: `Debreceni családi ház ${i + 1}`,
      price: `${80 + i * 10} M Ft`,
      city: 'Debrecen',
      location: 'Debrecen',
      propertyType: 'HAZ',
      priceInfo: { amountMillion: 80 + i * 10, amountHuf: (80 + i * 10) * 1_000_000 },
      detailsInfo: { areaSquareMeters: 120, pricePerSquareMeter: 650_000 },
    }))

    it('renders AlertCircle insufficient data banner when sample size < 5', () => {
      // 3 items (< 5)
      render(<MarketAnalyticsPanel items={mockApartments.slice(0, 3)} />)

      const banner = screen.getByTestId('insufficient-data-banner')
      expect(banner).toBeTruthy()
      expect(banner.getAttribute('role')).toBe('status')
      expect(banner.textContent).toContain('Kevés minta az elemzéshez')
      expect(banner.textContent).toContain('legalább 5 hirdetés szükséges')
    })

    it('does NOT render insufficient data banner when total sample size >= 5', () => {
      render(<MarketAnalyticsPanel items={mockApartments} />)
      expect(screen.queryByTestId('insufficient-data-banner')).toBeNull()
    })

    it('renders Lakás, Ház, and Telek KPI medians independently with "<5" fallback when insufficient', () => {
      // 6 Lakás, 2 Ház, 0 Telek
      const mixedItems = [...mockApartments, ...mockHouses]
      render(<MarketAnalyticsPanel items={mixedItems} />)

      // 1. Lakás (6 items >= 5): valid median
      const lakasCard = screen.getByTestId('kpi-card-lakas')
      expect(lakasCard.textContent).not.toContain('Kevés minta (<5)')
      expect(lakasCard.textContent).toMatch(/Ft\/m²/)
      expect(lakasCard.textContent).toContain('6 db lakás alapján')

      // 2. Ház (2 items < 5): insufficient data state
      const hazCard = screen.getByTestId('kpi-card-haz')
      expect(hazCard.textContent).toContain('Kevés minta (<5)')
      expect(hazCard.textContent).toContain('2 db ház alapján')

      // 3. Telek (0 items < 5): insufficient data state
      const telekCard = screen.getByTestId('kpi-card-telek')
      expect(telekCard.textContent).toContain('Kevés minta (<5)')
      expect(telekCard.textContent).toContain('0 db telek alapján')
    })

    it('verifies price drop cards formatting, original struck-through price, and secure link attributes', () => {
      const onMarkViewed = vi.fn()
      const onOpenListing = vi.fn()

      render(
        <MarketAnalyticsPanel
          items={mockApartments}
          onMarkViewed={onMarkViewed}
          onOpenListing={onOpenListing}
        />
      )

      const priceDropSection = screen.getByTestId('top-price-drops-section')
      expect(priceDropSection).toBeTruthy()

      const dropCards = screen.getAllByTestId('price-drop-card')
      expect(dropCards.length).toBe(1)
      const card = dropCards[0]

      // Struck-through original price verification
      // Current = 40M, delta = -3M -> original = 43M
      const struckThrough = card.querySelector('.line-through')
      expect(struckThrough).toBeTruthy()
      expect(struckThrough?.textContent).toMatch(/43\s*000\s*000\s*Ft/)

      // Price reduction amount
      expect(card.textContent).toMatch(/-3\s*000\s*000\s*Ft/)

      // External link security: target="_blank" and rel="noopener noreferrer"
      const link = card.querySelector('a')
      expect(link).toBeTruthy()
      expect(link?.getAttribute('target')).toBe('_blank')
      expect(link?.getAttribute('rel')).toBe('noopener noreferrer')

      // Click link triggers callbacks
      fireEvent.click(link!)
      expect(onMarkViewed).toHaveBeenCalledWith('https://example.com/apt/0')
      expect(onOpenListing).toHaveBeenCalledWith('https://example.com/apt/0')
    })
  })

  /* -------------------------------------------------------------------------- */
  /* 3. ListingsPanel & App View Switching & Compatibility                       */
  /* -------------------------------------------------------------------------- */
  describe('3. ListingsPanel & App View Switching & Selector Compatibility', () => {
    beforeEach(() => {
      localStorage.clear()
      useFilterStore.getState().resetToDefaults()
      useUiStore.setState({
        isFiltersCollapsed: false,
        dashboardTab: 'listings',
        listingSubView: 'listings',
        starredUrls: [],
        starredItemsCache: {},
      })
    })

    it('renders ListingsPanel with screen reader live region and multi-source banner in listings mode', () => {
      const items: IngatlanItem[] = [
        {
          id: 'item-grouped-1',
          portal: 'dh',
          url: 'https://example.com/grouped/1',
          title: 'Csoportosított lakás',
          price: '45 M Ft',
          city: 'Debrecen',
          location: 'Debrecen',
          propertyType: 'LAKAS',
          sourcesCount: 3,
        },
      ]

      render(
        <ListingsPanel
          items={items}
          totalRawCount={1}
          isLoading={false}
          hasSearched={true}
          mode="listings"
          onMarkViewed={vi.fn()}
          onOpenAll={vi.fn()}
          onTriggerSearch={vi.fn()}
        />
      )

      expect(screen.getByTestId('listings-panel')).toBeTruthy()
      expect(screen.getByTestId('multi-source-summary-notice')).toBeTruthy()
      const liveRegion = screen.getByText(/1 hirdetés jelenik meg a listában/i)
      expect(liveRegion.getAttribute('aria-live')).toBe('polite')
    })

    it('renders App with data-testid="listings-panel-header" and switches between tabs seamlessly', async () => {
      const queryClient = new QueryClient({
        defaultOptions: {
          queries: { retry: false, gcTime: 0 },
        },
      })

      render(
        <QueryClientProvider client={queryClient}>
          <App />
        </QueryClientProvider>
      )

      // 1. Selector compatibility check: data-testid="listings-panel-header" must exist
      const header = screen.getByTestId('listings-panel-header')
      expect(header).toBeTruthy()

      // 2. Default tab is listings
      expect(useUiStore.getState().dashboardTab).toBe('listings')
      expect(useUiStore.getState().listingSubView).toBe('listings')
      expect(screen.getByTestId('listings-panel')).toBeTruthy()

      // 3. Switch to Favorites sub-view within listings panel
      const favoritesBtn = screen.getByRole('button', { name: /⭐ Kedvencek/i })
      fireEvent.click(favoritesBtn)
      expect(useUiStore.getState().dashboardTab).toBe('listings')
      expect(useUiStore.getState().listingSubView).toBe('favorites')
      expect(screen.getByTestId('listings-panel')).toBeTruthy()

      // 4. Switch to Archive sub-view within listings panel
      const archiveBtn = screen.getByRole('button', { name: /Archívum/i })
      fireEvent.click(archiveBtn)
      expect(useUiStore.getState().dashboardTab).toBe('listings')
      expect(useUiStore.getState().listingSubView).toBe('archive')
      expect(screen.getByTestId('listings-panel')).toBeTruthy()

      // 5. Switch back to Hirdetések sub-view
      const activeListingsBtn = screen.getByRole('button', { name: /Hirdetések/i })
      fireEvent.click(activeListingsBtn)
      expect(useUiStore.getState().dashboardTab).toBe('listings')
      expect(useUiStore.getState().listingSubView).toBe('listings')
      expect(screen.getByTestId('listings-panel')).toBeTruthy()

      // 6. Switch to Market Analytics tab (top-level navigation)
      const analyticsBtn = screen.getByRole('button', { name: /Piaci Elemzés/i })
      fireEvent.click(analyticsBtn)
      expect(useUiStore.getState().dashboardTab).toBe('analytics')

      // Wait for lazy loaded MarketAnalyticsPanel
      const analyticsPanel = await screen.findByTestId('market-analytics-panel')
      expect(analyticsPanel).toBeTruthy()

      // 7. Switch back to Találatok tab (top-level navigation)
      const talalatokBtn = screen.getByRole('button', { name: /^Találatok$/i })
      fireEvent.click(talalatokBtn)
      expect(useUiStore.getState().dashboardTab).toBe('listings')
      expect(screen.getByTestId('listings-panel')).toBeTruthy()
    })
  })
})
