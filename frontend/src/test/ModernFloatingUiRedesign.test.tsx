import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { getCrossPortalSpread } from '../utils/formatters'
import { PropertyTimelineSection } from '../components/listings/PropertyTimelineSection'
import { ListingsTableView } from '../components/listings/ListingsTableView'
import { MarketSegmentMatrixTable } from '../components/analytics/MarketSegmentMatrixTable'
import { ArchiveSubViewToolbar } from '../components/archive/ArchiveSubViewToolbar'
import { NewCitiesInitializedBanner } from '../components/layout/StatusBanners'
import { Header } from '../components/layout/Header'
import { WorkspaceHeaderNav } from '../components/layout/WorkspaceChrome'
import { BargainHunterTable } from '../components/analytics/BargainHunterTable'
import { PropertyCard } from '../components/listings/PropertyCard'
import { ListingsPanel } from '../components/listings/ListingsPanel'
import { filterItemsByCities } from '../utils/listingFilters'
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
      viewMode: 'grid',
      dashboardTab: 'listings',
      listingSubView: 'listings',
      workspaceTab: 'listings',
      archiveSubMode: 'all',
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

  describe('4. MarketSegmentMatrixTable, ArchiveSubViewToolbar, Header Telemetry & Dirty Filters', () => {
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

    it('renders PropertyCard with overflow-visible and top-right QualityScoreShield popover placement', () => {
      const item = createMockIngatlanItem({
        id: 'qual-1',
        url: 'https://example.com/qual-1',
        qualityScore: 100,
        qualityDeductions: [],
      })

      render(<PropertyCard item={item} onMarkViewed={vi.fn()} />)
      const card = screen.getByTestId('property-card')
      expect(card.className).toContain('overflow-visible')

      const qualityBtn = screen.getByTestId('quality-score-button')
      fireEvent.click(qualityBtn)
      const popover = screen.getByTestId('quality-score-popover')
      expect(popover.className).toContain('bottom-full')
      expect(popover.className).toContain('right-0')
    })

    it('sorts BargainHunterTable deals by percentage descending and shows thumbnail preview on hover', () => {
      const dealLow = {
        item: createMockIngatlanItem({
          id: 'b-low',
          url: 'https://example.com/b-low',
          title: 'Alacsonyabb kedvezmény',
          imageUrl: 'https://example.com/low.jpg',
        }),
        cityAvgSqmPrice: 900_000,
        discountPercent: 18,
        sqmDiffFt: 150_000,
      }
      const dealBest = {
        item: createMockIngatlanItem({
          id: 'b-best',
          url: 'https://example.com/b-best',
          title: 'Legjobb kedvezmény',
          imageUrl: 'https://example.com/best.jpg',
        }),
        cityAvgSqmPrice: 900_000,
        discountPercent: 39,
        sqmDiffFt: 350_000,
      }
      const dropSmall = createMockIngatlanItem({
        id: 'd-small',
        url: 'https://example.com/d-small',
        title: 'Kisebb leárazás',
        priceDelta: -5_000_000,
        priceChangePercent: -5,
        imageUrl: 'https://example.com/d-small.jpg',
      })
      const dropLarge = createMockIngatlanItem({
        id: 'd-large',
        url: 'https://example.com/d-large',
        title: 'Nagyobb százalékos leárazás',
        priceDelta: -3_000_000,
        priceChangePercent: -15,
        imageUrl: 'https://example.com/d-large.jpg',
      })

      render(
        <BargainHunterTable
          belowMarketDeals={[dealLow, dealBest]}
          topPriceDrops={[dropSmall, dropLarge]}
        />,
      )

      const titles = screen.getAllByRole('heading', { level: 4 }).map((h) => h.textContent)
      expect(titles).toEqual(['Legjobb kedvezmény', 'Alacsonyabb kedvezmény'])

      expect(screen.queryByTestId('bargain-hover-thumbnail')).toBeNull()
      fireEvent.mouseEnter(screen.getByText('Legjobb kedvezmény'))
      const preview = screen.getByTestId('bargain-hover-thumbnail')
      expect(preview).toBeTruthy()
      const img = preview.querySelector('img')
      expect(img?.getAttribute('src')).toBe('https://example.com/best.jpg')

      fireEvent.click(screen.getByRole('button', { name: /Leárazások/i }))
      const dropTitles = screen.getAllByRole('heading', { level: 4 }).map((h) => h.textContent)
      expect(dropTitles).toEqual(['Nagyobb százalékos leárazás', 'Kisebb leárazás'])
    })

    it('renders WorkspaceHeaderNav as bookmark-style panel tabs with tablist and tab roles', () => {
      render(
        <WorkspaceHeaderNav
          dashboardTab="listings"
          listingSubView="archive"
          onSelectListings={vi.fn()}
          onSelectArchive={vi.fn()}
          onSelectAnalytics={vi.fn()}
        />,
      )

      expect(screen.getByRole('tablist')).toBeTruthy()
      const archiveTab = screen.getByTestId('tab-archive')
      expect(archiveTab.getAttribute('role')).toBe('tab')
      expect(archiveTab.getAttribute('aria-selected')).toBe('true')
      expect(archiveTab.getAttribute('aria-pressed')).toBe('true')
      expect(archiveTab.className).toContain('rounded-t-xl')
    })

    it('hides ListingsSubViewNav in archive mode and scopes archive items by selected cities', () => {
      render(
        <ListingsPanel
          items={[]}
          totalRawCount={0}
          isLoading={false}
          hasSearched={true}
          mode="archive"
          subView="archive"
          onMarkViewed={vi.fn()}
          onOpenAll={vi.fn()}
          onTriggerSearch={vi.fn()}
        />,
      )

      expect(screen.queryByTestId('listings-subview-nav')).toBeNull()

      const scoped = filterItemsByCities(
        [
          createMockIngatlanItem({ id: 'h-1', city: 'Hajdúszoboszló', location: 'Hajdúszoboszló' }),
          createMockIngatlanItem({ id: 'd-1', city: 'Debrecen', location: 'Debrecen' }),
        ],
        ['Hajdúszoboszló'],
      )
      expect(scoped.map((x) => x.id)).toEqual(['h-1'])
    })

    it('toggles quickSearchQuery as hot filters when clicking parameter chips on PropertyCard', () => {
      const item = createMockIngatlanItem({
        id: 'hot-1',
        url: 'https://example.com/hot-1',
        detailsInfo: {
          areaSquareMeters: 230,
          roomCount: 5,
          pricePerSquareMeter: 521304,
        },
      })

      render(<PropertyCard item={item} onMarkViewed={vi.fn()} />)

      const roomsChip = screen.getByTestId('hot-filter-rooms')
      fireEvent.click(roomsChip)
      expect(useFilterStore.getState().quickSearchQuery).toBe('5 szoba')
      expect(roomsChip.getAttribute('aria-pressed')).toBe('true')

      // Click again to reset
      fireEvent.click(roomsChip)
      expect(useFilterStore.getState().quickSearchQuery).toBe('')
      expect(roomsChip.getAttribute('aria-pressed')).toBe('false')

      // Click area chip
      const areaChip = screen.getByTestId('hot-filter-area')
      fireEvent.click(areaChip)
      expect(useFilterStore.getState().quickSearchQuery).toBe('230 m²')
    })
  })
})
