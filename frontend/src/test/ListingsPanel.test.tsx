import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ListingsPanel } from '../components/listings/ListingsPanel'
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
})
