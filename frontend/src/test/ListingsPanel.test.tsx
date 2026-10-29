import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ListingsPanel } from '../components/listings/ListingsPanel'
import { useUiStore } from '../store/useUiStore'
import type { IngatlanItem } from '../types/property'

// Mock useWindowVirtualizer to render items predictably in JSDOM
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

describe('ListingsPanel component (#264)', () => {
  const sampleItems: IngatlanItem[] = [
    {
      id: 'item-1',
      portal: 'dh',
      url: 'https://example.com/1',
      title: 'Debreceni ház',
      price: '55 M Ft',
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'HAZ',
      sourcesCount: 2, // multi-source clustered
      sourceListings: [
        { portal: 'dh', url: 'https://example.com/1' },
        { portal: 'oc', url: 'https://example.com/1-oc' },
      ],
    },
    {
      id: 'item-2',
      portal: 'oc',
      url: 'https://example.com/2',
      title: 'Szegedi lakás',
      price: '35 M Ft',
      location: 'Szeged',
      city: 'Szeged',
      propertyType: 'LAKAS',
      sourcesCount: 1,
    },
  ]

  beforeEach(() => {
    useUiStore.setState({ listingSubView: 'listings', dashboardTab: 'listings' })
  })

  it('renders listings panel with multi-source summary notice', () => {
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    expect(screen.getByTestId('listings-panel')).toBeTruthy()

    // Multi-source summary notice
    const notice = screen.getByTestId('multi-source-summary-notice')
    expect(notice).toBeTruthy()
    expect(notice.textContent).toMatch(/2 db ingatlan jelenik meg/i)
    expect(notice.textContent).toMatch(/1 db több portálon is szereplő/i)
  })

  it('renders live status announcement for screen readers', () => {
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    const liveRegion = screen.getByText(/2 hirdetés jelenik meg a listában/i)
    expect(liveRegion).toBeTruthy()
    expect(liveRegion.getAttribute('aria-live')).toBe('polite')
  })

  it('renders in favorites mode without multi-source banner', () => {
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="favorites"
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    expect(screen.getByTestId('listings-panel')).toBeTruthy()
    expect(screen.queryByTestId('multi-source-summary-notice')).toBeNull()
  })

  it('renders embedded sub-view toolbar with Hirdetések, Kedvencek, and Archívum buttons and triggers callback', () => {
    const handleSubViewChange = vi.fn()
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        searchCount={2}
        favoritesCount={5}
        archiveCount={12}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onSubViewChange={handleSubViewChange}
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    expect(screen.getByTestId('listings-subview-nav')).toBeTruthy()
    expect(screen.getByRole('button', { name: /Hirdetések \(2\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /⭐ Kedvencek \(5\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /Archívum \(12\)/i })).toBeTruthy()

    const favBtn = screen.getByRole('button', { name: /⭐ Kedvencek/i })
    fireEvent.click(favBtn)
    expect(handleSubViewChange).toHaveBeenCalledWith('favorites')

    const archBtn = screen.getByRole('button', { name: /Archívum/i })
    fireEvent.click(archBtn)
    expect(handleSubViewChange).toHaveBeenCalledWith('archive')
  })

  it('updates store directly in uncontrolled mode and sets aria-pressed correctly', async () => {
    const { useUiStore } = await import('../store/useUiStore')
    useUiStore.setState({ listingSubView: 'listings', dashboardTab: 'listings' })

    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    const listBtn = screen.getByTestId('subview-listings')
    const favBtn = screen.getByTestId('subview-favorites')
    const archBtn = screen.getByTestId('subview-archive')

    expect(listBtn.getAttribute('aria-pressed')).toBe('true')
    expect(favBtn.getAttribute('aria-pressed')).toBe('false')
    expect(archBtn.getAttribute('aria-pressed')).toBe('false')

    // Click favorites without onSubViewChange prop
    fireEvent.click(favBtn)
    expect(useUiStore.getState().listingSubView).toBe('favorites')
    expect(useUiStore.getState().dashboardTab).toBe('listings')
    expect(favBtn.getAttribute('aria-pressed')).toBe('true')
    expect(listBtn.getAttribute('aria-pressed')).toBe('false')
  })

  it('calculates deterministic fallback counts when counts are omitted', () => {
    // In archive mode with omitted counts, archiveCount falls back to totalRawCount and others to 0
    const { unmount } = render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={7}
        isLoading={false}
        hasSearched={true}
        mode="archive"
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    expect(screen.getByRole('button', { name: /Archívum \(7\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /Hirdetések \(0\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /⭐ Kedvencek \(0\)/i })).toBeTruthy()
    unmount()

    // In favorites mode with omitted counts, favoritesCount falls back to totalRawCount
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={4}
        isLoading={false}
        hasSearched={true}
        mode="favorites"
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    expect(screen.getByRole('button', { name: /⭐ Kedvencek \(4\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /Hirdetések \(0\)/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /Archívum \(0\)/i })).toBeTruthy()
  })

  it('progressively batches rendered cards with infinite scroll sentinel (#278)', () => {
    // Mock IntersectionObserver
    let observerCallback: (entries: IntersectionObserverEntry[]) => void = () => {}
    const observeMock = vi.fn()
    const disconnectMock = vi.fn()

    class MockIntersectionObserver implements IntersectionObserver {
      readonly root: Element | Document | null = null
      readonly rootMargin: string = ''
      readonly thresholds: ReadonlyArray<number> = []
      constructor(callback: IntersectionObserverCallback) {
        observerCallback = callback as (entries: IntersectionObserverEntry[]) => void
      }
      observe = observeMock
      unobserve = vi.fn()
      disconnect = disconnectMock
      takeRecords = () => []
    }

    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)

    // Generate 50 mock items
    const manyItems: IngatlanItem[] = Array.from({ length: 50 }, (_, i) => ({
      id: `prop-${i}`,
      portal: 'dh',
      url: `https://example.com/prop-${i}`,
      title: `Ingatlan #${i}`,
      price: `${30 + i} M Ft`,
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'LAKAS',
    }))

    render(
      <ListingsPanel
        items={manyItems}
        totalRawCount={50}
        isLoading={false}
        hasSearched={true}
        initialBatchSize={10}
        batchSize={10}
        onMarkViewed={vi.fn()}
        onOpenAll={vi.fn()}
        onTriggerSearch={vi.fn()}
      />
    )

    // Initially displays visibleCount / items.length
    expect(screen.getByText(/Megjelenítve: 10 \/ 50 db/i)).toBeTruthy()
    expect(observeMock).toHaveBeenCalled()

    const sentinel = screen.getByTestId('infinite-scroll-sentinel')
    expect(sentinel).toBeTruthy()
    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (10 / 50 db)')

    // Trigger intersection event wrapped in act
    act(() => {
      observerCallback([
        {
          isIntersecting: true,
          target: sentinel,
        } as unknown as IntersectionObserverEntry,
      ])
    })

    // Should batch-append next 10 items => 20 / 50 db
    expect(screen.getByText(/Megjelenítve: 20 \/ 50 db/i)).toBeTruthy()
    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (20 / 50 db)')

    vi.unstubAllGlobals()
  })
})

