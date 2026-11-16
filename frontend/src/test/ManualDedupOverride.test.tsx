import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { PropertyCard } from '../components/listings/PropertyCard'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { PropertyDetailsDrawer } from '../components/listings/PropertyDetailsDrawer'
import { useUiStore } from '../store/useUiStore'
import { createMockIngatlanItem } from './testFixtures'
import * as formatters from '../utils/formatters'
import * as dedupApi from '../api/dedupApi'

vi.mock('../api/dedupApi', () => ({
  getDedupOverrides: vi.fn().mockResolvedValue([]),
  splitCluster: vi.fn(),
  mergeListings: vi.fn(),
  deleteDedupOverride: vi.fn(),
}))

describe('Manual Dedup Overrides UI (#461)', () => {
  beforeEach(() => {
    useUiStore.setState({ selectedCardUrls: [], starredUrls: [] })
    vi.restoreAllMocks()
  })

  it('PropertyCard: Ctrl/Cmd+kattintás kijelöl és nem nyit meg linket / nem jelöl megtekintettnek', () => {
    const openSpy = vi.spyOn(formatters, 'openExternalUrl').mockImplementation(() => {})
    const onMarkViewed = vi.fn()
    const item = createMockIngatlanItem({
      id: 'ctrl-1',
      url: 'https://dh.hu/ctrl-1',
      title: 'Kossuth utcai lakás',
    })

    render(<PropertyCard item={item} onMarkViewed={onMarkViewed} />)

    const titleEl = screen.getByText('Kossuth utcai lakás')
    fireEvent.click(titleEl, { ctrlKey: true })

    expect(useUiStore.getState().selectedCardUrls).toEqual(['https://dh.hu/ctrl-1'])
    expect(screen.getByTestId('property-card').getAttribute('data-selected')).toBe('true')
    expect(screen.getByTestId('property-card').getAttribute('aria-pressed')).toBe('true')
    expect(screen.getByTestId('card-selected-badge')).toBeTruthy()
    expect(openSpy).not.toHaveBeenCalled()
    expect(onMarkViewed).not.toHaveBeenCalled()

    // Cmd+click (metaKey) toggles off
    fireEvent.click(titleEl, { metaKey: true })
    expect(useUiStore.getState().selectedCardUrls).toEqual([])
    expect(openSpy).not.toHaveBeenCalled()
    expect(onMarkViewed).not.toHaveBeenCalled()

    // Normal click still opens and marks viewed
    fireEvent.click(titleEl)
    expect(openSpy).toHaveBeenCalledWith('https://dh.hu/ctrl-1')
    expect(onMarkViewed).toHaveBeenCalledWith('https://dh.hu/ctrl-1')
  })

  it('BulkActionBar: összevonás gomb állapota, összes forrás URL küldése és Esc törlés', () => {
    const item1 = createMockIngatlanItem({
      id: 'b1',
      url: 'https://dh.hu/b1',
      sourceListings: [
        { portal: 'Duna House', url: 'https://dh.hu/b1', priceHuf: 40_000_000 },
        { portal: 'Otthon Centrum', url: 'https://oc.hu/b1b', priceHuf: 41_000_000 },
      ],
      sourcesCount: 2,
    })
    const item2 = createMockIngatlanItem({
      id: 'b2',
      url: 'https://jofogas.hu/b2',
      sourceListings: [{ portal: 'Jófogás', url: 'https://jofogas.hu/b2', priceHuf: 42_000_000 }],
      sourcesCount: 1,
    })
    const onMergeSelected = vi.fn()

    useUiStore.setState({ selectedCardUrls: ['https://dh.hu/b1'] })
    const { rerender } = render(
      <BulkActionBar
        items={[item1, item2]}
        totalRawCount={2}
        onMergeSelected={onMergeSelected}
      />,
    )

    expect(screen.getByTestId('selection-count-label').textContent).toBe('1 kijelölve')
    expect((screen.getByTestId('merge-selected-button') as HTMLButtonElement).disabled).toBe(true)

    useUiStore.setState({ selectedCardUrls: ['https://dh.hu/b1', 'https://jofogas.hu/b2'] })
    rerender(
      <BulkActionBar
        items={[item1, item2]}
        totalRawCount={2}
        onMergeSelected={onMergeSelected}
      />,
    )

    expect(screen.getByTestId('selection-count-label').textContent).toBe('2 kijelölve')
    const mergeBtn = screen.getByTestId('merge-selected-button') as HTMLButtonElement
    expect(mergeBtn.disabled).toBe(false)

    fireEvent.click(mergeBtn)
    expect(onMergeSelected).toHaveBeenCalledWith([
      'https://dh.hu/b1',
      'https://oc.hu/b1b',
      'https://jofogas.hu/b2',
    ])

    // Escape clears selection
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(useUiStore.getState().selectedCardUrls).toEqual([])
  })

  it('PropertyDetailsDrawer: Nem ugyanaz gomb + megerősítés és Visszavonás működése', async () => {
    vi.mocked(dedupApi.getDedupOverrides).mockResolvedValue([
      {
        id: 42,
        urlA: 'https://dh.hu/d1',
        urlB: 'https://oc.hu/d2',
        type: 'FORCE_MERGE',
        normalizedCity: 'debrecen',
        createdAt: '2026-11-16 12:00:00',
      },
    ])

    const onSplitSource = vi.fn()
    const onUndoOverride = vi.fn()
    const item = createMockIngatlanItem({
      id: 'd1',
      url: 'https://dh.hu/d1',
      manuallyMerged: true,
      sourcesCount: 2,
      sourceListings: [
        { portal: 'Duna House', url: 'https://dh.hu/d1', priceHuf: 45_000_000 },
        { portal: 'Otthon Centrum', url: 'https://oc.hu/d2', priceHuf: 47_000_000 },
      ],
    })

    render(
      <PropertyDetailsDrawer
        item={item}
        onSplitSource={onSplitSource}
        onUndoOverride={onUndoOverride}
      />,
    )

    const splitButtons = screen.getAllByTestId('split-source-button')
    expect(splitButtons).toHaveLength(2)

    // Split primary (cheapest) source
    fireEvent.click(splitButtons[0]!)
    const confirmBtn = screen.getByTestId('confirm-split-button')
    fireEvent.click(confirmBtn)

    await waitFor(() => {
      expect(onSplitSource).toHaveBeenCalledWith('https://dh.hu/d1', [
        'https://dh.hu/d1',
        'https://oc.hu/d2',
      ])
    })

    // Undo button from fetched overrides
    const undoBtn = await screen.findByTestId('undo-override-42')
    fireEvent.click(undoBtn)
    await waitFor(() => {
      expect(onUndoOverride).toHaveBeenCalledWith(42)
    })
  })
})
