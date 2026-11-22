import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { getCrossPortalSpread } from '../utils/formatters'
import { PropertyTimelineSection } from '../components/listings/PropertyTimelineSection'
import { ListingsTableView } from '../components/listings/ListingsTableView'
import { CommandPaletteModal } from '../components/modals/CommandPaletteModal'
import { MarketSegmentMatrixTable } from '../components/analytics/MarketSegmentMatrixTable'
import { ArchiveSubViewToolbar } from '../components/archive/ArchiveSubViewToolbar'
import { NewCitiesInitializedBanner } from '../components/layout/StatusBanners'
import { Header } from '../components/layout/Header'
import { sanitizeHistoryQuery } from '../hooks/useArchiveQueries'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore } from '../store/useFilterStore'
import { createMockIngatlanItem } from './testFixtures'
import * as propertyApi from '../api/propertyApi'

describe('Modern Floating UI Redesign & Zero-Backend Features (#497)', () => {
  beforeEach(() => {
    localStorage.clear()
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      theme: 'dark',
      viewMode: 'grid',
      dashboardTab: 'listings',
      listingSubView: 'listings',
      workspaceTab: 'listings',
      archiveSubMode: 'all',
      isCommandPaletteOpen: false,
      selectedCardUrls: [],
      starredUrls: [],
      starredItemsCache: {},
    })
    vi.restoreAllMocks()
  })

  describe('1. Cross-Portal Price Spread (getCrossPortalSpread)', () => {
    it('calculates active price spread and ignores inactive or zero-price sources', () => {
      const item = createMockIngatlanItem({
        id: 'spread-1',
        sourcesCount: 3,
        sourceListings: [
          { portal: 'Duna House', url: 'https://dh.hu/1', priceHuf: 89_000_000, status: 'ACTIVE' },
          { portal: 'Otthon Centrum', url: 'https://oc.hu/1', priceHuf: 84_500_000, status: 'ACTIVE' },
          { portal: 'Jófogás', url: 'https://jofogas.hu/1', priceHuf: 70_000_000, status: 'INACTIVE' },
        ],
      })

      const spread = getCrossPortalSpread(item)
      expect(spread).not.toBeNull()
      expect(spread?.activeSourcesCount).toBe(2)
      expect(spread?.minActivePriceHuf).toBe(84_500_000)
      expect(spread?.maxActivePriceHuf).toBe(89_000_000)
      expect(spread?.spreadAmountHuf).toBe(4_500_000)
      expect(spread?.spreadMillion).toBe(4.5)
      expect(spread?.cheapestPortal).toBe('Otthon Centrum')
    })

    it('returns null when all active sources have identical prices or fewer than 2 active sources exist', () => {
      const samePriceItem = createMockIngatlanItem({
        sourceListings: [
          { portal: 'DH', url: 'https://dh.hu/1', priceHuf: 75_000_000, status: 'ACTIVE' },
          { portal: 'OC', url: 'https://oc.hu/1', priceHuf: 75_000_000, status: 'ACTIVE' },
        ],
      })
      expect(getCrossPortalSpread(samePriceItem)).toBeNull()
    })
  })

  describe('2. PropertyTimelineSection (Sparkline & Observation Timeline)', () => {
    it('fetches and renders price history sparkline, unchanged count badge, and price delta', async () => {
      vi.spyOn(propertyApi, 'getPriceHistory').mockResolvedValue([
        { id: 1, propertyId: 'tl-1', recordedAt: '2026-11-01T10:00:00Z', priceAmount: 89_000_000, priceMillions: 89.0 },
        { id: 2, propertyId: 'tl-1', recordedAt: '2026-11-15T10:00:00Z', priceAmount: 84_500_000, priceMillions: 84.5 },
      ])
      vi.spyOn(propertyApi, 'getListingObservations').mockResolvedValue([
        { observedAt: '2026-11-05T10:00:00Z', priceHuf: 89_000_000, statusEvent: 'UNCHANGED' },
        { observedAt: '2026-11-10T10:00:00Z', priceHuf: 89_000_000, statusEvent: 'UNCHANGED' },
      ])
      vi.spyOn(propertyApi, 'getStatusLog').mockResolvedValue([])

      const item = createMockIngatlanItem({
        id: 'tl-1',
        url: 'https://dh.hu/tl-1',
        priceDelta: -4_500_000,
        priceChangePercent: -5.1,
      })

      render(<PropertyTimelineSection item={item} />)

      await waitFor(() => {
        expect(screen.getByTestId('property-timeline-section')).toBeTruthy()
        expect(screen.getByTestId('price-history-sparkline')).toBeTruthy()
        expect(screen.getByTestId('timeline-unchanged-badge').textContent).toContain('2× változatlan ár')
      })
      expect(screen.getAllByTestId('price-history-entry-row')).toHaveLength(2)
    })
  })

  describe('3. ListingsTableView (Desktop Analyst Table)', () => {
    it('renders tabular rows, toggles card selection checkbox, and opens Side-Peek Inspector modal', () => {
      const items = [
        createMockIngatlanItem({
          id: 'tbl-1',
          url: 'https://dh.hu/tbl-1',
          title: 'Elemző táblázat családi ház',
          city: 'Debrecen',
          qualityScore: 94,
          percentageBelowMedian: 22,
          referenceSampleSize: 12,
          isBargain: true,
          priceInfo: { amountHuf: 84_500_000, amountMillion: 84.5 },
          detailsInfo: { areaSquareMeters: 120, roomCount: 4, pricePerSquareMeter: 704_166 },
        }),
      ]

      render(<ListingsTableView items={items} onMarkViewed={vi.fn()} />)

      expect(screen.getByTestId('listings-table-view')).toBeTruthy()
      expect(screen.getByText('Elemző táblázat családi ház')).toBeTruthy()
      expect(screen.getByText('84.5 M Ft')).toBeTruthy()
      expect(screen.getByText('-22%')).toBeTruthy()

      // Toggle selection checkbox
      const checkbox = screen.getByTestId('table-row-select-checkbox')
      fireEvent.click(checkbox)
      expect(useUiStore.getState().selectedCardUrls).toContain('https://dh.hu/tbl-1')

      // Open Side-Peek Inspector
      const inspectBtn = screen.getByTestId('table-row-inspect-button')
      fireEvent.click(inspectBtn)
      expect(screen.getByTestId('property-details-modal')).toBeTruthy()
    })
  })

  describe('4. CommandPaletteModal (Cmd/Ctrl+K)', () => {
    it('opens via Ctrl+K, filters commands, and executes view/workspace switches', () => {
      const onTriggerSearch = vi.fn()
      render(<CommandPaletteModal onTriggerSearch={onTriggerSearch} />)

      // Initially closed
      expect(screen.queryByTestId('command-palette-modal')).toBeNull()

      // Open with Ctrl+K
      fireEvent.keyDown(window, { key: 'k', ctrlKey: true })
      expect(useUiStore.getState().isCommandPaletteOpen).toBe(true)
      expect(screen.getByTestId('command-palette-modal')).toBeTruthy()

      // Filter for "Táblázat"
      const input = screen.getByTestId('command-palette-input')
      fireEvent.change(input, { target: { value: 'Táblázat' } })

      const tableCmd = screen.getByTestId('command-item-vm-table')
      fireEvent.click(tableCmd)

      expect(useUiStore.getState().viewMode).toBe('table')
      expect(useUiStore.getState().isCommandPaletteOpen).toBe(false)
    })
  })

  describe('5. MarketSegmentMatrixTable, ArchiveSubViewToolbar, Header Telemetry & Dirty Filters', () => {
    it('renders server-verified MarketSegmentMatrixTable and triggers segment filter callback', () => {
      const onSelectSegment = vi.fn()
      render(
        <MarketSegmentMatrixTable
          segments={[
            {
              city: 'Hajdúszoboszló',
              propertyType: 'HAZ',
              propertyTypeName: 'Családi ház',
              sampleSize: 18,
              insufficientData: false,
              medianPricePerM2: 745_000,
              averagePriceMillion: 86.2,
              minPriceMillion: 68.0,
              maxPriceMillion: 118.0,
              bargainCount: 4,
            },
          ]}
          onSelectSegment={onSelectSegment}
        />,
      )

      expect(screen.getByTestId('market-segment-matrix-table')).toBeTruthy()
      expect(screen.getByText('Hajdúszoboszló')).toBeTruthy()
      expect(screen.getByText('Családi ház')).toBeTruthy()
      expect(screen.getByText('4 db')).toBeTruthy()

      fireEvent.click(screen.getByRole('button', { name: /Szűrés/i }))
      expect(onSelectSegment).toHaveBeenCalledWith('Hajdúszoboszló', 'HAZ')
    })

    it('renders ArchiveSubViewToolbar, NewCitiesInitializedBanner, and Header telemetry pill', () => {
      const queryClient = new QueryClient()
      render(
        <QueryClientProvider client={queryClient}>
          <Header
            totalInDb={1240}
            portalStatuses={[
              { portalId: 'dh', portalName: 'Duna House', status: 'SUCCESS', latencyMs: 100, foundCount: 10 },
              { portalId: 'oc', portalName: 'Otthon Centrum', status: 'SUCCESS', latencyMs: 120, foundCount: 8 },
            ]}
          />
          <NewCitiesInitializedBanner cities={['Eger', 'Veszprém']} />
          <ArchiveSubViewToolbar allCount={120} hiddenCount={5} discountedCount={14} />
        </QueryClientProvider>,
      )

      expect(screen.getByTestId('header-db-telemetry-pill').textContent).toContain('1240 DB')
      expect(screen.getByTestId('header-portal-health-count').textContent).toBe('2/2')
      expect(screen.getByTestId('new-cities-initialized-banner').textContent).toContain('Eger, Veszprém')

      fireEvent.click(screen.getByTestId('archive-mode-discounted'))
      expect(useUiStore.getState().archiveSubMode).toBe('discounted')
    })

    it('sanitizes HistoryQuery parameters and tracks dirty filter state after markCriteriaExecuted', () => {
      const sanitized = sanitizeHistoryQuery({
        minPrice: 120,
        maxPrice: 70,
        limit: 500,
        offset: -10,
      })
      expect(sanitized.maxPrice).toBeUndefined()
      expect(sanitized.limit).toBe(200)
      expect(sanitized.offset).toBe(0)

      useFilterStore.getState().markCriteriaExecuted()
      const initialHash = useFilterStore.getState().lastExecutedCriteriaHash
      expect(initialHash).toBeTruthy()

      useFilterStore.getState().setMinRooms(3)
      expect(useFilterStore.getState().criteria.minRooms).toBe(3)
    })
  })
})
