import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { useFilterStore } from '../store/useFilterStore'
import { useUiStore, DEFAULT_PINNED_QUICK_FILTERS } from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'

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

    // Selector button shows (3/5)
    const selectorBtn = within(filterRow).getByTestId('quick-filter-selector-btn')
    expect(selectorBtn.textContent).toContain('Szűrők (3/5)')
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

    // Initially 3 are checked, and the remaining 2 unchecked are disabled
    expect(bargainCheckbox.checked).toBe(true)
    expect(onlyNewCheckbox.checked).toBe(true)
    expect(hiddenCheckbox.checked).toBe(true)
    expect(excludeViewedCheckbox.checked).toBe(false)
    expect(excludeViewedCheckbox.disabled).toBe(true)
    expect(newConstructionCheckbox.checked).toBe(false)
    expect(newConstructionCheckbox.disabled).toBe(true)

    // Uncheck 'hidden' -> now 2/5 selected, unchecked options become enabled
    fireEvent.click(hiddenCheckbox)
    expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew'])
    expect(selectorBtn.textContent).toContain('Szűrők (2/5)')
    expect(screen.queryByTestId('hidden-filter-toggle')).toBeNull()
    expect(excludeViewedCheckbox.disabled).toBe(false)
    expect(newConstructionCheckbox.disabled).toBe(false)

    // Check 'excludeViewed' -> back to 3/5 selected
    fireEvent.click(excludeViewedCheckbox)
    expect(useUiStore.getState().pinnedQuickFilters).toEqual(['bargain', 'onlyNew', 'excludeViewed'])
    expect(selectorBtn.textContent).toContain('Szűrők (3/5)')

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
})
