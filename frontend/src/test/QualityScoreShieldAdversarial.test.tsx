import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { QualityScoreShield } from '../components/listings/QualityScoreShield'
import { PropertyCard } from '../components/listings/PropertyCard'
import { useUiStore } from '../store/useUiStore'
import type { IngatlanItem } from '../types/property'

describe('Challenger R5-2: QualityScoreShield & PropertyCard Adversarial & Accessibility Torture Suite (#267)', () => {
  const baseItem: IngatlanItem = {
    id: 'prop-adv-test-1',
    portal: 'Duna House',
    url: 'https://dh.hu/ingatlan/adv-123',
    title: 'Modern felújított lakás Debrecen belvárosában',
    price: '55 M Ft',
    location: 'Debrecen, Belváros',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isNew: false,
    viewed: false,
    priceInfo: {
      amountMillion: 55.0,
      amountHuf: 55_000_000,
      rawText: '55 M Ft',
    },
    detailsInfo: {
      areaSquareMeters: 60,
      roomCount: 2,
      pricePerSquareMeter: 916_666,
    },
  }

  let windowOpenSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    localStorage.clear()
    useUiStore.setState({ starredUrls: [], starredItemsCache: {} })
    windowOpenSpy = vi.fn()
    vi.stubGlobal('open', windowOpenSpy)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // =========================================================================
  // 1. OUT-OF-BOUNDS SCORE CLAMPING & CORRUPTION RESILIENCE
  // =========================================================================
  describe('1. Out-of-bounds score clamping & corruption resilience', () => {
    it('clamps negative score (-50) to 0, maps to Tier 3 (SUSPICIOUS), and renders outlier warning', () => {
      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: -50,
        qualityDeductions: ['-30 pont: Érvénytelen ár', '-25 pont: Érvénytelen alapterület'],
      }

      render(<QualityScoreShield item={item} />)

      const button = screen.getByTestId('quality-score-button')
      expect(button.getAttribute('data-rating')).toBe('SUSPICIOUS')
      expect(button.getAttribute('aria-label')).toBe('Adatminőség: 0/100 pont (Gyanús / hiányos adat)')

      const pill = screen.getByTestId('quality-score-pill')
      expect(pill.textContent).toBe('0/100')

      // Open popover to check inner meter and warning
      fireEvent.click(button)
      const popover = screen.getByTestId('quality-score-popover')
      expect(popover).toBeTruthy()
      expect(screen.getByTestId('popover-score-badge').textContent).toBe('0/100 pont')

      const warning = screen.getByTestId('outlier-warning')
      expect(warning).toBeTruthy()
      expect(warning.textContent).toContain('70 pont alatti minőség: a hirdetés ki van zárva')
    })

    it('clamps massive negative score (-99999) safely to 0', () => {
      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: -99999,
      }

      render(<QualityScoreShield item={item} />)
      const pill = screen.getByTestId('quality-score-pill')
      expect(pill.textContent).toBe('0/100')
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('SUSPICIOUS')
    })

    it('clamps oversized score (150) to 100, maps to Tier 1 (EXCELLENT), without outlier warning', () => {
      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: 150,
        qualityDeductions: [],
      }

      render(<QualityScoreShield item={item} />)

      const button = screen.getByTestId('quality-score-button')
      expect(button.getAttribute('data-rating')).toBe('EXCELLENT')
      expect(button.getAttribute('aria-label')).toBe('Adatminőség: 100/100 pont (Kiváló adatminőség)')

      const pill = screen.getByTestId('quality-score-pill')
      expect(pill.textContent).toBe('100/100')

      fireEvent.click(button)
      expect(screen.getByTestId('popover-score-badge').textContent).toBe('100/100 pont')
      expect(screen.queryByTestId('outlier-warning')).toBeNull()
      expect(screen.getByTestId('no-deductions-message')).toBeTruthy()
    })

    it('clamps massive oversized score (99999) safely to 100', () => {
      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: 99999,
      }

      render(<QualityScoreShield item={item} />)
      const pill = screen.getByTestId('quality-score-pill')
      expect(pill.textContent).toBe('100/100')
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('EXCELLENT')
    })

    it('precisely evaluates boundary scores: 85 (Tier 1) vs 84 (Tier 2)', () => {
      // 85 -> Tier 1 (EXCELLENT)
      const { unmount: u1 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: 85 }} />)
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('EXCELLENT')
      expect(screen.getByTestId('quality-score-pill').textContent).toBe('85/100')
      u1()

      // 84 -> Tier 2 (ACCEPTABLE)
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 84 }} />)
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('ACCEPTABLE')
      expect(screen.getByTestId('quality-score-pill').textContent).toBe('84/100')
    })

    it('precisely evaluates boundary scores: 70 (Tier 2, no outlier) vs 69 (Tier 3, outlier warning)', () => {
      // 70 -> Tier 2 (ACCEPTABLE)
      const { unmount: u1 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: 70 }} />)
      const btn70 = screen.getByTestId('quality-score-button')
      expect(btn70.getAttribute('data-rating')).toBe('ACCEPTABLE')
      fireEvent.click(btn70)
      expect(screen.queryByTestId('outlier-warning')).toBeNull()
      u1()

      // 69 -> Tier 3 (SUSPICIOUS) with outlier warning
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 69 }} />)
      const btn69 = screen.getByTestId('quality-score-button')
      expect(btn69.getAttribute('data-rating')).toBe('SUSPICIOUS')
      fireEvent.click(btn69)
      expect(screen.getByTestId('outlier-warning')).toBeTruthy()
    })

    it('correctly rounds floating point scores (84.4 -> 84 Tier 2; 84.6 -> 85 Tier 1)', () => {
      const { unmount: u1 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: 84.4 }} />)
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('ACCEPTABLE')
      expect(screen.getByTestId('quality-score-pill').textContent).toBe('84/100')
      u1()

      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 84.6 }} />)
      expect(screen.getByTestId('quality-score-button').getAttribute('data-rating')).toBe('EXCELLENT')
      expect(screen.getByTestId('quality-score-pill').textContent).toBe('85/100')
    })

    it('safely suppresses rendering when qualityScore is null, undefined, or NaN', () => {
      const { container: c1 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: null as unknown as number }} />)
      expect(c1.firstChild).toBeNull()

      const { container: c2 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: undefined }} />)
      expect(c2.firstChild).toBeNull()

      const { container: c3 } = render(<QualityScoreShield item={{ ...baseItem, qualityScore: NaN }} />)
      expect(c3.firstChild).toBeNull()
    })
  })

  // =========================================================================
  // 2. RAPID EVENTS & PROPAGATION ISOLATION IN PROPERTYCARD & QUALITYSCORESHIELD (#267, #270)
  // =========================================================================
  describe('2. Rapid events & propagation isolation in QualityScoreShield', () => {
    it('clicking quality score button does NOT invoke window.open or trigger onMarkViewed', () => {
      const handleMarkViewed = vi.fn()
      const itemWithScore: IngatlanItem = { ...baseItem, qualityScore: 90 }

      render(
        <div onClick={() => { (windowOpenSpy as unknown as () => void)() }}>
          <QualityScoreShield item={itemWithScore} />
        </div>
      )

      const scoreBtn = screen.getByTestId('quality-score-button')
      fireEvent.click(scoreBtn)

      // Popover opens
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

      // Outer action was NOT triggered
      expect(windowOpenSpy).not.toHaveBeenCalled()
      expect(handleMarkViewed).not.toHaveBeenCalled()
    })

    it('clicking inside the open popover dialog does NOT bubble or trigger window.open', () => {
      const handleMarkViewed = vi.fn()
      const itemWithScore: IngatlanItem = {
        ...baseItem,
        qualityScore: 65,
        qualityDeductions: ['-20 pont: Alacsony megbízhatóság', '-15 pont: Hiányzó cím'],
      }

      render(
        <div onClick={() => { (windowOpenSpy as unknown as () => void)() }}>
          <QualityScoreShield item={itemWithScore} />
        </div>
      )

      // Open popover
      const scoreBtn = screen.getByTestId('quality-score-button')
      fireEvent.click(scoreBtn)

      const popover = screen.getByTestId('quality-score-popover')
      expect(popover).toBeTruthy()

      // Click on popover body
      fireEvent.click(popover)
      expect(windowOpenSpy).not.toHaveBeenCalled()
      expect(handleMarkViewed).not.toHaveBeenCalled()

      // Click on deductions list
      const list = screen.getByTestId('deductions-list')
      fireEvent.click(list)
      expect(windowOpenSpy).not.toHaveBeenCalled()

      // Click on outlier warning
      const warning = screen.getByTestId('outlier-warning')
      fireEvent.click(warning)
      expect(windowOpenSpy).not.toHaveBeenCalled()

      // Popover remains open
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
    })

    it('handles 10 rapid alternating clicks deterministically without event leakage', () => {
      const handleMarkViewed = vi.fn()
      const itemWithScore: IngatlanItem = { ...baseItem, qualityScore: 95 }

      render(
        <div onClick={() => { (windowOpenSpy as unknown as () => void)() }}>
          <QualityScoreShield item={itemWithScore} />
        </div>
      )
      const button = screen.getByTestId('quality-score-button')

      for (let i = 1; i <= 10; i++) {
        fireEvent.click(button)
        const shouldBeOpen = i % 2 === 1
        expect(button.getAttribute('aria-expanded')).toBe(String(shouldBeOpen))
        if (shouldBeOpen) {
          expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
        } else {
          expect(screen.queryByTestId('quality-score-popover')).toBeNull()
        }
      }

      expect(windowOpenSpy).not.toHaveBeenCalled()
      expect(handleMarkViewed).not.toHaveBeenCalled()
    })

    it('pressing Space or Enter on the button does NOT trigger card navigation', () => {
      const itemWithScore: IngatlanItem = { ...baseItem, qualityScore: 95 }

      render(
        <div onClick={() => { (windowOpenSpy as unknown as () => void)() }}>
          <QualityScoreShield item={itemWithScore} />
        </div>
      )
      const button = screen.getByTestId('quality-score-button')

      // Space key
      fireEvent.keyDown(button, { key: ' ' })
      expect(button.getAttribute('aria-expanded')).toBe('true')
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
      expect(windowOpenSpy).not.toHaveBeenCalled()

      // Enter key (toggles closed)
      fireEvent.keyDown(button, { key: 'Enter' })
      expect(button.getAttribute('aria-expanded')).toBe('false')
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
      expect(windowOpenSpy).not.toHaveBeenCalled()
    })

    it('thumbnail and title clicks on PropertyCard still open window normally (#270)', () => {
      const handleMarkViewed = vi.fn()
      const itemWithScore: IngatlanItem = { ...baseItem, qualityScore: 90 }

      render(<PropertyCard item={itemWithScore} onMarkViewed={handleMarkViewed} />)

      // Title click
      const title = screen.getByText(baseItem.title)
      fireEvent.click(title)
      expect(windowOpenSpy).toHaveBeenCalledWith(baseItem.url, '_blank', 'noopener,noreferrer')
      expect(handleMarkViewed).toHaveBeenCalledWith(baseItem.url)
    })
  })

  // =========================================================================
  // 3. KEYBOARD NAVIGATION TORTURE & FOCUS INTERACTIONS
  // =========================================================================
  describe('3. Keyboard navigation torture & focus interactions', () => {
    it('opens on Enter key and stops event propagation with preventDefault', () => {
      const parentKeyDownSpy = vi.fn()
      render(
        <div onKeyDown={parentKeyDownSpy}>
          <QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />
        </div>
      )
      const button = screen.getByTestId('quality-score-button')
      expect(button.getAttribute('aria-expanded')).toBe('false')

      const allowed = fireEvent.keyDown(button, { key: 'Enter' })

      // In DOM, defaultPrevented causes fireEvent to return false
      expect(allowed).toBe(false)
      // stopPropagation prevents parent handler from receiving the event
      expect(parentKeyDownSpy).not.toHaveBeenCalled()
      expect(button.getAttribute('aria-expanded')).toBe('true')
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
    })

    it('opens on Space key and prevents page scroll (preventDefault)', () => {
      const parentKeyDownSpy = vi.fn()
      render(
        <div onKeyDown={parentKeyDownSpy}>
          <QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />
        </div>
      )
      const button = screen.getByTestId('quality-score-button')

      const allowed = fireEvent.keyDown(button, { key: ' ' })

      expect(allowed).toBe(false)
      expect(parentKeyDownSpy).not.toHaveBeenCalled()
      expect(button.getAttribute('aria-expanded')).toBe('true')
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
    })

    it('closes on Escape key and stops propagation to prevent modal dialog exits', () => {
      const parentKeyDownSpy = vi.fn()
      render(
        <div onKeyDown={parentKeyDownSpy}>
          <QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />
        </div>
      )
      const button = screen.getByTestId('quality-score-button')

      // Open first
      fireEvent.click(button)
      expect(button.getAttribute('aria-expanded')).toBe('true')

      // Press Escape
      fireEvent.keyDown(button, { key: 'Escape' })

      expect(parentKeyDownSpy).not.toHaveBeenCalled()
      expect(button.getAttribute('aria-expanded')).toBe('false')
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
    })

    it('maintains focus on trigger button across onFocus auto-open, Enter, Space, and Escape keyboard cycles', () => {
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />)
      const button = screen.getByTestId('quality-score-button')

      // 1. Focusing the button (e.g. Tab navigation) triggers onFocus which opens popover
      act(() => {
        button.focus()
      })
      expect(document.activeElement).toBe(button)
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

      // 2. Escape closes popover; focus remains on button
      fireEvent.keyDown(button, { key: 'Escape' })
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
      expect(document.activeElement).toBe(button)

      // 3. Now that it is closed, pressing Enter opens popover; focus remains on button
      fireEvent.keyDown(button, { key: 'Enter' })
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
      expect(document.activeElement).toBe(button)

      // 4. Pressing Enter again toggles popover closed; focus remains on button
      fireEvent.keyDown(button, { key: 'Enter' })
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
      expect(document.activeElement).toBe(button)

      // 5. Space opens popover; focus remains on button
      fireEvent.keyDown(button, { key: ' ' })
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()
      expect(document.activeElement).toBe(button)

      // 6. Space toggles popover closed; focus remains on button
      fireEvent.keyDown(button, { key: ' ' })
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
      expect(document.activeElement).toBe(button)
    })

    it('observes hover lifecycle: mouseLeave on button closes popover even after click', () => {
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />)
      const button = screen.getByTestId('quality-score-button')

      fireEvent.click(button)
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

      fireEvent.mouseLeave(button)
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
    })

    it('opens on focus and closes on blur', () => {
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />)
      const button = screen.getByTestId('quality-score-button')

      act(() => {
        button.focus()
      })
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

      act(() => {
        button.blur()
      })
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
    })

    it('closes when clicking outside container on document mousedown', () => {
      render(
        <div>
          <span data-testid="outside-element">Külső elem</span>
          <QualityScoreShield item={{ ...baseItem, qualityScore: 88 }} />
        </div>
      )

      const button = screen.getByTestId('quality-score-button')
      fireEvent.click(button)
      expect(screen.getByTestId('quality-score-popover')).toBeTruthy()

      // Click outside
      fireEvent.mouseDown(screen.getByTestId('outside-element'))
      expect(screen.queryByTestId('quality-score-popover')).toBeNull()
    })
  })

  // =========================================================================
  // 4. SCREEN READER & WCAG 2.1 ARIA ATTRIBUTE CONFORMANCE
  // =========================================================================
  describe('4. Screen reader & WCAG 2.1 ARIA conformance', () => {
    it('verifies strict ARIA pairing: role="button", aria-haspopup="dialog", aria-controls matching popover id, aria-expanded', () => {
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 78 }} />)

      const button = screen.getByTestId('quality-score-button')
      expect(button.getAttribute('role')).toBe('button')
      expect(button.getAttribute('tabindex')).toBe('0')
      expect(button.getAttribute('aria-haspopup')).toBe('dialog')
      expect(button.getAttribute('aria-expanded')).toBe('false')

      const ariaControls = button.getAttribute('aria-controls')
      expect(ariaControls).toBeTruthy()

      // Initially dialog is not in DOM
      expect(document.getElementById(ariaControls!)).toBeNull()

      // Click to open
      fireEvent.click(button)
      expect(button.getAttribute('aria-expanded')).toBe('true')

      // Dialog is now present and id exactly matches aria-controls
      const popover = screen.getByTestId('quality-score-popover')
      expect(popover.getAttribute('id')).toBe(ariaControls)
      expect(popover.getAttribute('role')).toBe('dialog')
      expect(popover.getAttribute('aria-label')).toBe('Adatminőségi értékelés részletei')
    })

    it('ensures aria-label provides complete score and rating context for screen readers', () => {
      render(<QualityScoreShield item={{ ...baseItem, qualityScore: 94 }} />)
      const button = screen.getByTestId('quality-score-button')
      expect(button.getAttribute('aria-label')).toBe('Adatminőség: 94/100 pont (Kiváló adatminőség)')
    })
  })

  // =========================================================================
  // 5. LONG DEDUCTION STRINGS & LAYOUT STRESS RESILIENCE
  // =========================================================================
  describe('5. Long deduction strings & layout stress resilience', () => {
    it('resiliently renders 10 simultaneous deduction strings without crash or overflow breakage', () => {
      const tenDeductions = [
        '1. -30 pont: Érvénytelen vagy irreális ár (1M - 5B Ft tartományon kívül esik)',
        '2. -25 pont: Hiányzó vagy érvénytelen alapterület (< 10 m² lakás esetén)',
        '3. -20 pont: Hiányzó vagy nem beazonosítható település és lokáció megadás',
        '4. -15 pont: Ismeretlen ingatlan kategória (EGYEB_ISMERETLEN besorolás)',
        '5. -20 pont: Irreális fajlagos négyzetméterár (< 100 000 Ft/m² lakás esetén)',
        '6. -15 pont: Valószínűtlen alapterület-szobaszám sűrűség (< 12 m²/szoba)',
        '7. -10 pont: Hiányzó hirdetés cím vagy megnevezés',
        '8. -20 pont: Érvénytelen vagy hiányzó forrás URL link formátum',
        '9. -10 pont: Nincs fotó feltöltve az ingatlanhoz a forrásportálon',
        '10. -25 pont: Szélsőséges historikus árváltozás detektálva (> 50% ugrás)',
      ]

      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: 10,
        qualityDeductions: tenDeductions,
      }

      render(<QualityScoreShield item={item} />)
      const button = screen.getByTestId('quality-score-button')
      fireEvent.click(button)

      const popover = screen.getByTestId('quality-score-popover')
      expect(popover).toBeTruthy()

      // Check max-width constraint to prevent screen blowout
      expect(popover.className).toContain('max-w-[calc(100vw-2.5rem)]')

      const list = screen.getByTestId('deductions-list')
      expect(list).toBeTruthy()

      const listItems = list.querySelectorAll('li')
      expect(listItems.length).toBe(10)

      for (let i = 0; i < 10; i++) {
        expect(listItems[i].textContent).toContain(tenDeductions[i])
      }
    })

    it('resiliently renders ultra-long 300+ character deduction string', () => {
      const ultraLongDeduction =
        '-30 pont: Érvénytelenített és kritikus határértéket meghaladó hirdetési adatmező: ' +
        'https://example.com/very/long/path/with/lots/of/parameters?query=debrecen&category=lakas&id=1234567890&param=hosszu-parameter-szoveg-minta-ertek-pelda-tesztelesre ' +
        'amely súlyosan sérti az adatintegritási és statisztikai plausibilitási normákat.'

      const item: IngatlanItem = {
        ...baseItem,
        qualityScore: 70,
        qualityDeductions: [ultraLongDeduction],
      }

      render(<QualityScoreShield item={item} />)
      const button = screen.getByTestId('quality-score-button')
      fireEvent.click(button)

      const list = screen.getByTestId('deductions-list')
      expect(list.textContent).toContain(ultraLongDeduction)
    })
  })
})
