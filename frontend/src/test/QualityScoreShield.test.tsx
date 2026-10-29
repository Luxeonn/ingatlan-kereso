import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { QualityScoreShield } from '../components/listings/QualityScoreShield'
import type { IngatlanItem } from '../types/property'

describe('QualityScoreShield component (#267)', () => {
  const baseItem: IngatlanItem = {
    id: 'test-1',
    portal: 'Duna House',
    url: 'https://example.com/prop/1',
    title: 'Szép lakás Debrecenben',
    price: '45 M Ft',
    location: 'Debrecen',
    city: 'Debrecen',
    propertyType: 'LAKAS',
  }

  it('strictly suppresses rendering when qualityScore is null or undefined', () => {
    const { container: c1 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: undefined }} />)
    expect(c1.firstChild).toBeNull()

    const { container: c2 } = render(
      <QualityScoreShield item={{ ...baseItem, qualityScore: null as unknown as number }} />
    )
    expect(c2.firstChild).toBeNull()
  })

  it('renders Tier 1 (Excellent, 85-100) with green shield, 92/100 score pill, and no deductions message', () => {
    const item: IngatlanItem = {
      ...baseItem,
      qualityScore: 92,
      qualityDeductions: [],
    }

    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button', { name: /Adatminőség: 92\/100 pont \(Kiváló adatminőség\)/i })
    expect(button).toBeTruthy()
    expect(button.getAttribute('data-rating')).toBe('EXCELLENT')

    const pill = screen.getByTestId('quality-score-pill')
    expect(pill.textContent).toBe('92/100')

    // Click to view popover
    fireEvent.click(button)
    expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
    expect(screen.getByTestId('no-deductions-message').textContent).toContain('Nincs levonás')
    expect(screen.queryByTestId('outlier-warning')).toBeNull()
  })

  it('renders Tier 2 (Acceptable, 70-84) with amber shield and itemized deductions', () => {
    const item: IngatlanItem = {
      ...baseItem,
      qualityScore: 75,
      qualityDeductions: ['-15 pont: Eltérő szobaszám arány', '-10 pont: Hiányzó fényképek'],
    }

    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button', { name: /Adatminőség: 75\/100 pont \(Elfogadható adatminőség\)/i })
    expect(button).toBeTruthy()
    expect(button.getAttribute('data-rating')).toBe('ACCEPTABLE')

    fireEvent.click(button)
    const list = screen.getByTestId('deductions-list')
    expect(list.textContent).toContain('Hiányzó fényképek')
    expect(list.textContent).toContain('Eltérő szobaszám arány')
    expect(screen.queryByTestId('outlier-warning')).toBeNull()
  })

  it('renders Tier 3 (Suspicious, <70) with red shield and outlier warning notice', () => {
    const item: IngatlanItem = {
      ...baseItem,
      qualityScore: 50,
      qualityDeductions: ['-30 pont: Nem reális ár (1M - 5B HUF tartományon kívül)', '-20 pont: Hiányzó település'],
    }

    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button', { name: /Adatminőség: 50\/100 pont \(Gyanús \/ hiányos adat\)/i })
    expect(button).toBeTruthy()
    expect(button.getAttribute('data-rating')).toBe('SUSPICIOUS')

    fireEvent.click(button)
    const popover = screen.getByTestId('quality-score-popover')
    expect(popover).toBeTruthy()
    expect(popover.textContent).toContain('Gyanús / hiányos adat')

    const warning = screen.getByTestId('outlier-warning')
    expect(warning.textContent).toContain('70 pont alatti minőség: a hirdetés ki van zárva')
  })

  it('toggles popover on click and dismisses on Escape key', () => {
    const item: IngatlanItem = { ...baseItem, qualityScore: 88 }
    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button')
    expect(button.getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(button)
    expect(button.getAttribute('aria-expanded')).toBe('true')
    expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

    fireEvent.keyDown(button, { key: 'Escape' })
    expect(button.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('quality-score-popover')).toBeNull()
  })

  it('stops event propagation on click to avoid triggering card container open', () => {
    const item: IngatlanItem = { ...baseItem, qualityScore: 88 }
    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button')
    const stopPropagation = vi.fn()
    fireEvent.click(button, { stopPropagation })

    expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
  })

  it('opens on hover and closes on mouseLeave', () => {
    const item: IngatlanItem = { ...baseItem, qualityScore: 88 }
    render(<QualityScoreShield item={item} />)

    const button = screen.getByRole('button')
    fireEvent.mouseEnter(button)
    expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

    fireEvent.mouseLeave(button)
    expect(screen.queryByTestId('quality-score-popover')).toBeNull()
  })
})
