import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { useFilterStore } from '../store/useFilterStore'
import { createMockIngatlanItem } from './testFixtures'
import type { IngatlanItem } from '../types/property'

describe('BulkActionBar result counter with multi-source listings (#358)', () => {
  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
  })

  it('displays standard item count when all items have a single source', () => {
    const singleSourceItems: IngatlanItem[] = [
      createMockIngatlanItem({ id: '1', sourcesCount: 1 }),
      createMockIngatlanItem({ id: '2', sourcesCount: 1 }),
    ]

    render(
      <BulkActionBar
        items={singleSourceItems}
        rawItems={singleSourceItems}
        totalRawCount={2}
        onOpenAll={vi.fn()}
        onMarkAllViewed={vi.fn()}
      />,
    )

    expect(screen.getByText('2 db')).toBeTruthy()
  })

  it('displays both unique property count and total source listings count for 6 properties / 9 listings (4x1 + 1x2 + 1x3) (#358)', () => {
    const clusteredItems: IngatlanItem[] = [
      createMockIngatlanItem({ id: '1', sourcesCount: 1 }),
      createMockIngatlanItem({ id: '2', sourcesCount: 1 }),
      createMockIngatlanItem({ id: '3', sourcesCount: 1 }),
      createMockIngatlanItem({ id: '4', sourcesCount: undefined, sourceListings: undefined }),
      createMockIngatlanItem({
        id: '5',
        sourcesCount: 2,
        sourceListings: [
          { portal: 'dh', url: 'https://dh.hu/5a' },
          { portal: 'dh', url: 'https://dh.hu/5b' },
        ],
      }),
      createMockIngatlanItem({
        id: '6',
        sourcesCount: 3,
        sourceListings: [
          { portal: 'dh', url: 'https://dh.hu/6a' },
          { portal: 'dh', url: 'https://dh.hu/6b' },
          { portal: 'oc', url: 'https://oc.hu/6c' },
        ],
      }),
    ]

    render(
      <BulkActionBar
        items={clusteredItems}
        rawItems={clusteredItems}
        totalRawCount={6}
        onOpenAll={vi.fn()}
        onMarkAllViewed={vi.fn()}
      />,
    )

    expect(screen.getByText('6 ingatlan (9 hirdetés)')).toBeTruthy()
  })

  it('includes total source listings count when filtered or progressively batched (#358)', () => {
    const clusteredItems: IngatlanItem[] = [
      createMockIngatlanItem({ id: '1', sourcesCount: 2 }),
      createMockIngatlanItem({ id: '2', sourcesCount: 3 }),
    ]

    const { unmount } = render(
      <BulkActionBar
        items={clusteredItems}
        rawItems={clusteredItems}
        totalRawCount={5}
        onOpenAll={vi.fn()}
        onMarkAllViewed={vi.fn()}
      />,
    )

    expect(screen.getByText('2 / 5 db (5 hirdetés)')).toBeTruthy()
    expect(screen.getByText('(szűrve)')).toBeTruthy()
    unmount()

    render(
      <BulkActionBar
        items={clusteredItems}
        rawItems={clusteredItems}
        totalRawCount={2}
        visibleCount={1}
        onOpenAll={vi.fn()}
        onMarkAllViewed={vi.fn()}
      />,
    )

    expect(screen.getByText('Megjelenítve: 1 / 2 db (5 hirdetés)')).toBeTruthy()
  })
})
