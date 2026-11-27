import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { ListingsGrid } from '../components/listings/ListingsGrid'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore } from '../store/useFilterStore'
import { createMockIngatlanItem } from './testFixtures'

// Mock virtualizer so items render deterministically in jsdom
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
    measureElement: () => {},
  }),
}))

describe('ListingsGrid component', () => {
  const onMarkViewed = vi.fn()
  const onTriggerSearch = vi.fn()
  const onClearHistory = vi.fn()

  const sampleItems = [
    createMockIngatlanItem({
      id: 'item-1',
      url: 'https://dh.hu/ingatlan/1',
      title: 'Első ingatlan Debrecen',
      price: '45 M Ft',
    }),
    createMockIngatlanItem({
      id: 'item-2',
      url: 'https://oc.hu/ingatlan/2',
      title: 'Második ingatlan Szeged',
      price: '65 M Ft',
    }),
  ]

  beforeEach(() => {
    vi.clearAllMocks()

    class MockIntersectionObserver implements IntersectionObserver {
      readonly root: Element | Document | null = null
      readonly rootMargin: string = ''
      readonly thresholds: ReadonlyArray<number> = []
      observe = vi.fn()
      unobserve = vi.fn()
      disconnect = vi.fn()
      takeRecords = () => []
    }
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)

    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      viewMode: 'grid',
      dashboardTab: 'listings',
      listingSubView: 'listings',
      starredUrls: [],
      starredItemsCache: {},
    })
  })

  it('renders loading skeletons when isLoading is true', () => {
    const { container } = render(
      <ListingsGrid
        items={[]}
        totalRawCount={0}
        isLoading={true}
        hasSearched={false}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    const skeletons = container.querySelectorAll('.animate-shimmer')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('renders initial search call to action when mode is listings and search has not run', () => {
    render(
      <ListingsGrid
        items={[]}
        totalRawCount={0}
        isLoading={false}
        hasSearched={false}
        mode="listings"
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    expect(screen.getByText('Készen állsz a keresésre?')).toBeTruthy()
    const searchBtn = screen.getByRole('button', { name: 'Keresés indítása' })
    expect(searchBtn).toBeTruthy()

    fireEvent.click(searchBtn)
    expect(onTriggerSearch).toHaveBeenCalledTimes(1)
  })

  it('renders empty favorites state when mode is favorites and list is empty', () => {
    render(
      <ListingsGrid
        items={[]}
        totalRawCount={0}
        isLoading={false}
        hasSearched={true}
        mode="favorites"
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    expect(screen.getByText('Még nincsenek kedvencnek jelölt ingatlanok')).toBeTruthy()
  })

  it('renders empty archive state when mode is archive and list is empty', () => {
    render(
      <ListingsGrid
        items={[]}
        totalRawCount={0}
        isLoading={false}
        hasSearched={true}
        mode="archive"
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
        onClearHistory={onClearHistory}
      />,
    )

    expect(screen.getByText('Az archívum jelenleg üres')).toBeTruthy()
  })

  it('renders BulkActionBar and empty filtered message when filters match 0 items', () => {
    render(
      <ListingsGrid
        items={[]}
        rawItems={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    // Bulk action bar is still accessible so user can loosen filters
    expect(screen.getByPlaceholderText(/Gyorskeresés a találatok között/i)).toBeTruthy()
    expect(screen.getByText('Nem található ingatlan a megadott feltételekkel')).toBeTruthy()
  })

  it('renders property cards and bulk action toolbar when items are present', () => {
    render(
      <ListingsGrid
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        mode="listings"
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    expect(screen.getByText('Első ingatlan Debrecen')).toBeTruthy()
    expect(screen.getByText('Második ingatlan Szeged')).toBeTruthy()
  })

  it('renders infinite scroll sentinel when more items are available', () => {
    const manyItems = Array.from({ length: 25 }, (_, i) =>
      createMockIngatlanItem({
        id: `item-${i}`,
        url: `https://dh.hu/ingatlan/${i}`,
        title: `Ingatlan ${i}`,
      }),
    )

    render(
      <ListingsGrid
        items={manyItems}
        rawItems={manyItems}
        totalRawCount={25}
        isLoading={false}
        hasSearched={true}
        initialBatchSize={10}
        batchSize={5}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    const sentinel = screen.getByTestId('infinite-scroll-sentinel')
    expect(sentinel).toBeTruthy()
    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (10 / 25 db)')
  })

  it('preserves visibleLimit when items receives a new array reference with identical URLs/order (#459)', () => {
    const manyItems = Array.from({ length: 45 }, (_, i) =>
      createMockIngatlanItem({
        id: `item-${i}`,
        url: `https://dh.hu/ingatlan/${i}`,
        title: `Ingatlan ${i}`,
        viewed: false,
      }),
    )

    const { rerender } = render(
      <ListingsGrid
        items={manyItems}
        rawItems={manyItems}
        totalRawCount={45}
        isLoading={false}
        hasSearched={true}
        initialBatchSize={18}
        batchSize={18}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    // Load next batch -> visibleLimit becomes 36
    const sentinel = screen.getByTestId('infinite-scroll-sentinel')
    fireEvent.click(sentinel)
    expect(screen.getByTestId('infinite-scroll-sentinel').textContent).toContain('(36 / 45 db)')

    // Rerender with a new array reference (e.g. after markAsViewed updates viewed=true)
    const updatedItems = manyItems.map((item, idx) =>
      idx === 20 ? { ...item, viewed: true, isNew: false } : item,
    )

    rerender(
      <ListingsGrid
        items={updatedItems}
        rawItems={updatedItems}
        totalRawCount={45}
        isLoading={false}
        hasSearched={true}
        initialBatchSize={18}
        batchSize={18}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    // visibleLimit must remain 36
    expect(screen.getByTestId('infinite-scroll-sentinel').textContent).toContain('(36 / 45 db)')

    // Rerender with genuinely different list content -> visibleLimit resets to 18
    const newSearchItems = Array.from({ length: 45 }, (_, i) =>
      createMockIngatlanItem({
        id: `other-${i}`,
        url: `https://oc.hu/ingatlan/${i}`,
        title: `Másik ingatlan ${i}`,
      }),
    )

    rerender(
      <ListingsGrid
        items={newSearchItems}
        rawItems={newSearchItems}
        totalRawCount={45}
        isLoading={false}
        hasSearched={true}
        initialBatchSize={18}
        batchSize={18}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    expect(screen.getByTestId('infinite-scroll-sentinel').textContent).toContain('(18 / 45 db)')
  })

  it('computes virtualizer columns from container ResizeObserver width (876->2, 1036->3, 1360->4, 1560->5) and updates dynamically', () => {
    let activeWidth = 876
    let observerCallback: ResizeObserverCallback | null = null
    let observedEl: Element | null = null

    class MockResizeObserver implements ResizeObserver {
      constructor(cb: ResizeObserverCallback) {
        observerCallback = cb
      }
      observe = vi.fn((el: Element) => {
        observedEl = el
      })
      unobserve = vi.fn()
      disconnect = vi.fn()
    }
    vi.stubGlobal('ResizeObserver', MockResizeObserver)

    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => ({
      width: activeWidth,
      height: 600,
      top: 0,
      left: 0,
      bottom: 600,
      right: activeWidth,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    }))

    const tenItems = Array.from({ length: 10 }, (_, i) =>
      createMockIngatlanItem({
        id: `resize-${i}`,
        url: `https://dh.hu/resize/${i}`,
        title: `Ingatlan ${i}`,
      }),
    )

    const { container } = render(
      <ListingsGrid
        items={tenItems}
        rawItems={tenItems}
        totalRawCount={10}
        isLoading={false}
        hasSearched={true}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    // 876px -> 2 columns -> ceil(10 / 2) = 5 rows
    expect(container.querySelectorAll('[data-index]').length).toBe(5)

    const triggerContainerResize = (width: number) => {
      activeWidth = width
      act(() => {
        observerCallback?.(
          [
            {
              target: observedEl!,
              contentRect: { width, height: 600 } as DOMRectReadOnly,
              borderBoxSize: [],
              contentBoxSize: [],
              devicePixelContentBoxSize: [],
            },
          ],
          {} as ResizeObserver,
        )
      })
    }

    // 1036px -> 3 columns -> ceil(10 / 3) = 4 rows
    triggerContainerResize(1036)
    expect(container.querySelectorAll('[data-index]').length).toBe(4)

    // 1360px -> 4 columns -> ceil(10 / 4) = 3 rows
    triggerContainerResize(1360)
    expect(container.querySelectorAll('[data-index]').length).toBe(3)

    // 1560px -> 5 columns -> ceil(10 / 5) = 2 rows
    triggerContainerResize(1560)
    expect(container.querySelectorAll('[data-index]').length).toBe(2)
  })

  it('applies row stacking-context classes in grid and list modes below BulkActionBar z-30 (#535)', () => {
    const { container, rerender } = render(
      <ListingsGrid
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    const gridRow = container.querySelector('[data-index="0"]')
    expect(gridRow).toBeTruthy()
    expect(gridRow?.className).toContain('z-0')
    expect(gridRow?.className).toContain('hover:z-10')
    expect(gridRow?.className).toContain('focus-within:z-20')
    expect(gridRow?.className).toContain('has-[[data-testid="bargain-popover"]]:z-20')
    expect(gridRow?.className).not.toContain('z-30')

    act(() => {
      useUiStore.setState({ viewMode: 'list' })
    })
    rerender(
      <ListingsGrid
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        isLoading={false}
        hasSearched={true}
        onMarkViewed={onMarkViewed}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    const listRow = container.querySelector('[data-index="0"]')
    expect(listRow).toBeTruthy()
    expect(listRow?.className).toContain('z-0')
    expect(listRow?.className).toContain('hover:z-10')
    expect(listRow?.className).toContain('focus-within:z-20')
    expect(listRow?.className).toContain('has-[[data-testid="bargain-popover"]]:z-20')
  })
})

