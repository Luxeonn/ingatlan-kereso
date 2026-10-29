import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { BargainBadge } from '../components/listings/BargainBadge'
import type { IngatlanItem } from '../types/property'

describe('BargainBadge component (#263 & #264)', () => {
  const baseBargainItem: IngatlanItem = {
    id: 'test-1',
    portal: 'dh',
    url: 'https://example.com/prop/1',
    title: 'Szép lakás',
    price: '38 M Ft',
    location: 'Debrecen',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isBargain: true,
    percentageBelowMedian: 24.1,
    referenceMedianPricePerSquareMeter: 1_000_000,
    referenceSampleSize: 15,
    detailsInfo: {
      areaSquareMeters: 50,
      pricePerSquareMeter: 760_000,
    },
  }

  it('renders visible label, percentage pill, and accessible aria-label when bargain with sample size >= 5', () => {
    render(<BargainBadge item={baseBargainItem} />)

    const button = screen.getByRole('button', { name: /Alkalmi vétel: 24%-kal a helyi piaci mediánár alatt/i })
    expect(button).toBeTruthy()
    expect(button.textContent).toContain('Alkalmi vétel')

    const pill = screen.getByTestId('bargain-discount-pill')
    expect(pill.textContent).toContain('-24%')
  })

  it('strictly suppresses badge rendering when reference sample size < 5', () => {
    const insufficientSampleItem: IngatlanItem = {
      ...baseBargainItem,
      referenceSampleSize: 4,
    }

    const { container } = render(<BargainBadge item={insufficientSampleItem} />)
    expect(container.firstChild).toBeNull()
  })

  it('strictly suppresses badge rendering when isBargain is false', () => {
    const notBargainItem: IngatlanItem = {
      ...baseBargainItem,
      isBargain: false,
    }

    const { container } = render(<BargainBadge item={notBargainItem} />)
    expect(container.firstChild).toBeNull()
  })

  it('toggles accessible popover on button click with metrics details', () => {
    render(<BargainBadge item={baseBargainItem} />)

    const button = screen.getByRole('button')
    expect(button.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('bargain-popover')).toBeNull()

    // Click to open
    fireEvent.click(button)
    expect(button.getAttribute('aria-expanded')).toBe('true')

    const popover = screen.getByTestId('bargain-popover')
    expect(popover).toBeTruthy()
    expect(popover.textContent).toContain('Piaci Árelemzés • Alkalmi Vétel')
    expect(popover.textContent).toMatch(/760\s*000\s*Ft\/m²/)
    expect(popover.textContent).toMatch(/1\s*000\s*000\s*Ft\/m²/)
    expect(popover.textContent).toContain('-24%')
    expect(popover.textContent).toContain('15 hirdetés')

    // Click again to close
    fireEvent.click(button)
    expect(button.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('bargain-popover')).toBeNull()
  })

  it('dismisses popover on Escape key press', () => {
    render(<BargainBadge item={baseBargainItem} />)

    const button = screen.getByRole('button')
    fireEvent.click(button)
    expect(screen.getByTestId('bargain-popover')).toBeTruthy()

    // Press Escape
    fireEvent.keyDown(button, { key: 'Escape' })
    expect(screen.queryByTestId('bargain-popover')).toBeNull()
    expect(button.getAttribute('aria-expanded')).toBe('false')
  })

  it('opens on hover/focus and closes on unhover/blur', () => {
    render(<BargainBadge item={baseBargainItem} />)

    const button = screen.getByRole('button')
    fireEvent.mouseEnter(button)
    expect(screen.getByTestId('bargain-popover')).toBeTruthy()

    fireEvent.mouseLeave(button)
    expect(screen.queryByTestId('bargain-popover')).toBeNull()

    fireEvent.focus(button)
    expect(screen.getByTestId('bargain-popover')).toBeTruthy()

    fireEvent.blur(button)
    expect(screen.queryByTestId('bargain-popover')).toBeNull()
  })
})
