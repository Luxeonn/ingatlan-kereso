import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { PropertyCard } from '../components/listings/PropertyCard'
import type { IngatlanItem } from '../types/property'

describe('PropertyCard Dismiss & Restore (#318)', () => {
  const mockItem: IngatlanItem = {
    id: 'prop-hide-1',
    portal: 'Duna House',
    url: 'https://dh.hu/ingatlan/hide-1',
    title: 'Elrejtendő ingatlan',
    price: '50 M Ft',
    location: 'Budapest',
    city: 'Budapest',
    propertyType: 'LAKAS',
    isNew: false,
    viewed: false,
    hidden: false,
    priceInfo: {
      amountMillion: 50.0,
      amountHuf: 50000000,
      rawText: '50 M Ft',
    },
  }

  it('renders hide trash button when item is active and triggers onHide with animation', () => {
    vi.useFakeTimers()
    const handleHide = vi.fn()
    render(
      <PropertyCard
        item={mockItem}
        onMarkViewed={() => {}}
        onHide={handleHide}
      />,
    )

    const hideBtn = screen.getByTestId('hide-property-button')
    expect(hideBtn).toBeTruthy()
    expect(screen.queryByTestId('restore-property-button')).toBeNull()

    fireEvent.click(hideBtn)

    // Check that card gets hiding animation class
    const card = screen.getByTestId('property-card')
    expect(card.className).toContain('opacity-0')
    expect(card.className).toContain('scale-95')

    // After animation timeout (150ms), onHide callback is called
    act(() => {
      vi.advanceTimersByTime(160)
    })
    expect(handleHide).toHaveBeenCalledTimes(1)
    expect(handleHide).toHaveBeenCalledWith(mockItem)
    vi.useRealTimers()
  })

  it('renders restore button when item is hidden and triggers onRestore immediately', () => {
    const handleRestore = vi.fn()
    const hiddenItem: IngatlanItem = {
      ...mockItem,
      hidden: true,
      hiddenAt: '2026-11-06T12:00:00Z',
    }

    render(
      <PropertyCard
        item={hiddenItem}
        onMarkViewed={() => {}}
        onRestore={handleRestore}
      />,
    )

    const restoreBtn = screen.getByTestId('restore-property-button')
    expect(restoreBtn).toBeTruthy()
    expect(screen.queryByTestId('hide-property-button')).toBeNull()

    fireEvent.click(restoreBtn)
    expect(handleRestore).toHaveBeenCalledTimes(1)
    expect(handleRestore).toHaveBeenCalledWith(hiddenItem)
  })
})
