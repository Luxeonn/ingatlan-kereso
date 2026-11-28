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
      sourcesCount: 2,
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

  it('renders live status announcement for screen readers and removes redundant ListingsSubViewNav', () => {
    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={vi.fn()}
        onTriggerSearch={vi.fn()}
      />,
    )

    expect(screen.getByTestId('listings-panel')).toBeTruthy()
    expect(screen.queryByTestId('listings-subview-nav')).toBeNull()
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
        onTriggerSearch={vi.fn()}
      />,
    )

    expect(screen.getByTestId('listings-panel')).toBeTruthy()
    expect(screen.queryByTestId('multi-source-summary-notice')).toBeNull()
  })

  it('progressively batches rendered cards with infinite scroll sentinel (#278)', () => {
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

    const manyItems: IngatlanItem[] = Array.from({ length: 50 }, (_, i) => ({
      id: `prop-${i}`,
      portal: 'dh',
      url: `https://example.com/prop-${i}`,
      title: `Ingatlan #${i}`,
      price: `${30 + i} M Ft`,
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'LAKAS',
      sourcesCount: 1,
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
        onTriggerSearch={vi.fn()}
      />,
    )

    expect(screen.getByText('50 db')).toBeTruthy()
    expect(screen.queryByText(/Megjelenítve:/i)).toBeNull()
    expect(observeMock).toHaveBeenCalled()

    const sentinel = screen.getByTestId('infinite-scroll-sentinel')
    expect(sentinel).toBeTruthy()
    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (10 / 50 db)')

    act(() => {
      observerCallback([
        {
          isIntersecting: true,
          target: sentinel,
        } as unknown as IntersectionObserverEntry,
      ])
    })

    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (20 / 50 db)')

    vi.unstubAllGlobals()
  })

  it('switches between grid view modal details and list view inline details in ListingsGrid (#357)', () => {
    useUiStore.setState({ viewMode: 'grid', listingSubView: 'listings', dashboardTab: 'listings' })

    render(
      <ListingsPanel
        items={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={vi.fn()}
        onTriggerSearch={vi.fn()}
      />,
    )

    const detailsBtn = screen.getByTestId('details-button')
    fireEvent.click(detailsBtn)

    expect(screen.getByTestId('property-details-modal')).toBeTruthy()
    const cardsInGrid = screen.getAllByTestId('property-card')
    const drawerInGrid = screen.getByTestId('multi-source-drawer')
    expect(cardsInGrid.some((c) => c.contains(drawerInGrid))).toBe(false)

    fireEvent.click(screen.getByTestId('property-details-modal-close'))
    expect(screen.queryByTestId('property-details-modal')).toBeNull()

    const listToggleBtn = screen.getByTitle('Lista nézet')
    fireEvent.click(listToggleBtn)
    expect(useUiStore.getState().viewMode).toBe('list')

    fireEvent.click(screen.getByTestId('details-button'))
    expect(screen.queryByTestId('property-details-modal')).toBeNull()
    const cardsInList = screen.getAllByTestId('property-card')
    const drawerInList = screen.getByTestId('multi-source-drawer')
    expect(cardsInList.some((c) => c.contains(drawerInList))).toBe(true)
  })
})
