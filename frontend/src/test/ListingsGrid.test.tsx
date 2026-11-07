import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
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
  const onOpenAll = vi.fn()
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
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
        onOpenAll={onOpenAll}
        onTriggerSearch={onTriggerSearch}
      />,
    )

    const sentinel = screen.getByTestId('infinite-scroll-sentinel')
    expect(sentinel).toBeTruthy()
    expect(sentinel.textContent).toContain('További ingatlanok betöltése... (10 / 25 db)')
  })
})

