import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore, DEFAULT_PINNED_QUICK_FILTERS } from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'
import type { IngatlanItem } from '../types/property'

const sampleItems = [
  createMockIngatlanItem({
    id: 'prop-1',
    url: 'https://dh.hu/1',
    title: 'Debreceni családi ház',
    isNew: true,
    viewed: false,
    isBargain: true,
    newConstruction: true,
  }),
  createMockIngatlanItem({
    id: 'prop-2',
    url: 'https://oc.hu/2',
    title: 'Belvárosi lakás',
    isNew: false,
    viewed: true,
    isBargain: false,
    newConstruction: false,
  }),
]

describe('BulkActionBar & Customizable QuickFilterChipsRow (#359)', () => {
  beforeEach(() => {
    localStorage.clear()
    useFilterStore.getState().resetToDefaults()
    useUiStore.setState({
      viewMode: 'grid',
      pinnedQuickFilters: [...DEFAULT_PINNED_QUICK_FILTERS],
    })
  })

  it('AC2: omits Összes megnyitása and Mind megtekintett buttons while keeping Quick Search, Sort, Count, and View Mode toggles', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
      />,
    )

    expect(screen.queryByRole('button', { name: /Összes megnyitása/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Mind megtekintett/i })).toBeNull()

    expect(screen.getByPlaceholderText(/Gyorskeresés a találatok között/i)).toBeTruthy()
    expect(screen.getByLabelText('Rendezés')).toBeTruthy()
    expect(screen.getByText('2 db')).toBeTruthy()
    expect(screen.getByTitle('Rács nézet')).toBeTruthy()
    expect(screen.getByTitle('Lista nézet')).toBeTruthy()
  })

  it('AC3: displays the 3 default pinned quick filters: Alkalmi vétel, Csak új találatok, Elrejtettek', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
        hiddenCount={3}
      />,
    )

    const filterRow = screen.getByTestId('filter-chips-row')
    expect(within(filterRow).getByTestId('bargain-filter-toggle')).toBeTruthy()
    expect(within(filterRow).getByRole('button', { name: /Csak új találatok/i })).toBeTruthy()
    expect(within(filterRow).getByTestId('hidden-filter-toggle')).toBeTruthy()

    // Non-default filters are not pinned initially
    expect(within(filterRow).queryByRole('button', { name: /Megtekintettek rejtése/i })).toBeNull()
    expect(within(filterRow).queryByRole('button', { name: /Csak új építésű/i })).toBeNull()
    expect(within(filterRow).queryByRole('button', { name: /Több forrásosak rejtése/i })).toBeNull()

    // Selector button shows (3/6)
    const selectorBtn = within(filterRow).getByTestId('quick-filter-selector-btn')
    expect(selectorBtn.textContent).toContain('Szűrők (3/6)')
  })

  it('AC4: allows customizing pinned quick filters via popover (max 3), disables unchecked options at limit, and persists to localStorage', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
      />,
    )

    const selectorBtn = screen.getByTestId('quick-filter-selector-btn')
    fireEvent.click(selectorBtn)

    const popover = screen.getByTestId('quick-filter-selector-popover')
    expect(popover).toBeTruthy()

    const bargainCheckbox = screen.getByTestId('quick-filter-checkbox-bargain') as HTMLInputElement
    const onlyNewCheckbox = screen.getByTestId('quick-filter-checkbox-onlyNew') as HTMLInputElement
    const hiddenCheckbox = screen.getByTestId('quick-filter-checkbox-hidden') as HTMLInputElement
    const excludeViewedCheckbox = screen.getByTestId('quick-filter-checkbox-excludeViewed') as HTMLInputElement
    const newConstructionCheckbox = screen.getByTestId('quick-filter-checkbox-onlyNewConstruction') as HTMLInputElement
    const excludeMultiCheckbox = screen.getByTestId('quick-filter-checkbox-excludeMultiSource') as HTMLInputElement

    // Initially 3 are checked, and the remaining 3 unchecked are disabled
    expect(bargainCheckbox.checked).toBe(true)
    expect(onlyNewCheckbox.checked).toBe(true)
    expect(hiddenCheckbox.checked).toBe(true)
    expect(excludeViewedCheckbox.checked).toBe(false)
    expect(excludeViewedCheckbox.disabled).toBe(true)
    expect(newConstructionCheckbox.checked).toBe(false)
    expect(newConstructionCheckbox.disabled).toBe(true)
    expect(excludeMultiCheckbox.checked).toBe(false)
    expect(excludeMultiCheckbox.disabled).toBe(true)

    // Uncheck 'hidden' -> now 2/6 selected, unchecked options become enabled
    fireEvent.click(hiddenCheckbox)
    expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew'])
    expect(selectorBtn.textContent).toContain('Szűrők (2/6)')
    expect(screen.queryByTestId('hidden-filter-toggle')).toBeNull()
    expect(excludeViewedCheckbox.disabled).toBe(false)
    expect(newConstructionCheckbox.disabled).toBe(false)
    expect(excludeMultiCheckbox.disabled).toBe(false)

    // Check 'excludeViewed' -> back to 3/6 selected
    fireEvent.click(excludeViewedCheckbox)
    expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew', 'excludeViewed'])
    expect(selectorBtn.textContent).toContain('Szűrők (3/6)')

    // Verify localStorage persistence
    const stored = JSON.parse(localStorage.getItem('pinned_quick_filters') || '[]')
    expect(stored).toEqual(['bargain', 'onlyNew', 'excludeViewed'])

    // Verify the newly pinned 'Megtekintettek rejtése' button is rendered and interactive
    const excludeViewedChip = screen.getByRole('button', { name: /Megtekintettek rejtése/i })
    expect(excludeViewedChip).toBeTruthy()
    fireEvent.click(excludeViewedChip)
    expect(useFilterStore.getState().criteria.excludeViewed).toBe(true)

    // Close popover via Escape key
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByTestId('quick-filter-selector-popover')).toBeNull()
  })

  it('allows pinning and toggling the excludeMultiSource quick filter (#369)', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
      />,
    )

    // Open popover, uncheck onlyNew and check excludeMultiSource
    fireEvent.click(screen.getByTestId('quick-filter-selector-btn'))
    fireEvent.click(screen.getByTestId('quick-filter-checkbox-onlyNew'))
    fireEvent.click(screen.getByTestId('quick-filter-checkbox-excludeMultiSource'))

    expect(useUiStore.getState().pinnedQuickFilters).toContain('excludeMultiSource')

    // Pinned chip renders with title and test-id
    const multiChip = screen.getByTestId('exclude-multi-source-toggle')
    expect(multiChip).toBeTruthy()
    expect(multiChip.getAttribute('aria-pressed')).toBe('false')

    // Clicking toggles excludeMultiSource in useFilterStore and updates storage
    fireEvent.click(multiChip)
    expect(useFilterStore.getState().excludeMultiSource).toBe(true)
    expect(multiChip.getAttribute('aria-pressed')).toBe('true')
    expect(localStorage.getItem('preferred_exclude_multi_source')).toBe('true')

    fireEvent.click(multiChip)
    expect(useFilterStore.getState().excludeMultiSource).toBe(false)
    expect(multiChip.getAttribute('aria-pressed')).toBe('false')
  })

  it('verifies sort select does not contain Ft/m² options (#369)', () => {
    render(
      <BulkActionBar
        items={sampleItems}
        rawItems={sampleItems}
        totalRawCount={2}
      />,
    )

    const sortSelect = screen.getByLabelText('Rendezés') as HTMLSelectElement
    const optionValues = Array.from(sortSelect.options).map((o) => o.value)
    expect(optionValues).not.toContain('sqm_price_asc')
    expect(optionValues).not.toContain('sqm_price_desc')
    expect(optionValues).toEqual(['price_asc', 'price_desc', 'area_desc', 'date_desc', 'bargain_desc'])
  })
})

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
      />,
    )

    expect(screen.getByText('Megjelenítve: 1 / 2 db (5 hirdetés)')).toBeTruthy()
  })
})
