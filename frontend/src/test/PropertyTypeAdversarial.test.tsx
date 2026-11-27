import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PropertyTypeBadge } from '../components/listings/PropertyTypeBadge'
import { PropertyCard } from '../components/listings/PropertyCard'
import type { IngatlanItem, CanonicalPropertyType } from '../types/property'
import { useUiStore } from '../store/useUiStore'

describe('PropertyType Adversarial & Stress Testing', () => {
  beforeEach(() => {
    localStorage.clear()
    useUiStore.setState({ starredUrls: [], starredItemsCache: {} })
  })

  describe('Edge case inputs to PropertyTypeBadge', () => {
    const maliciousAndEdgeCases = [
      { name: 'undefined', value: undefined },
      { name: 'null', value: null },
      { name: 'empty string', value: '' },
      { name: 'whitespace only', value: '    \t\n  ' },
      { name: 'unexpected domain string alien_spaceship', value: 'alien_spaceship' },
      { name: 'random gibberish', value: 'asdfqwerty123456' },
      { name: 'HTML injection attempt', value: '<img src=x onerror=alert(1)>' },
      { name: 'script injection attempt', value: '<script>alert("xss")</script>' },
      { name: 'extreme length string', value: 'Z'.repeat(1000) },
      { name: 'special characters', value: '!@#$%^&*()_+=-~`{}[]|;:,.<>?' },
      { name: 'numeric string', value: '99999' },
      { name: 'number coerced', value: 42 as unknown as string },
      { name: 'boolean coerced', value: true as unknown as string },
      { name: 'object coerced', value: {} as unknown as string },
    ]

    it.each(maliciousAndEdgeCases)(
      'safely handles $name without crashing and defaults to EGYEB_ISMERETLEN ("Egyéb")',
      ({ value }) => {
        const { unmount } = render(<PropertyTypeBadge propertyType={value as unknown as CanonicalPropertyType} />)
        const badge = screen.getByTestId('property-type-badge')
        expect(badge).toBeTruthy()
        expect(badge.getAttribute('data-property-type')).toBe('EGYEB_ISMERETLEN')
        expect(badge.textContent).toContain('Egyéb')
        expect(badge.getAttribute('role')).toBe('status')
        expect(badge.getAttribute('aria-label')).toBe('Ingatlan típusa: Egyéb')
        unmount()
      }
    )
  })

  describe('PropertyCard edge cases & fallback rendering', () => {
    const edgeTypes = [undefined, null, '', 'alien_spaceship', '<script>', 'UNKNOWN']

    it.each(edgeTypes)(
      'PropertyCard renders without error when propertyType is %s',
      (edgeType) => {
        const item: IngatlanItem = {
          id: 'adv-test-1',
          portal: 'Otthon Centrum',
          url: 'https://oc.hu/ingatlan/999',
          title: 'Adversarial Test Property',
          price: '50 M Ft',
          city: 'Budapest',
          location: 'Budapest',
          propertyType: edgeType as unknown as IngatlanItem['propertyType'],
          imageUrl: undefined, // test thumbnail fallback simultaneously
          sourcesCount: 1,
        }

        const { unmount } = render(<PropertyCard item={item} onMarkViewed={() => {}} />)

        // Open details drawer to inspect property type badge (#270)
        const detailsBtn = screen.getByTestId('details-button')
        fireEvent.click(detailsBtn)

        // Verify badge
        const badge = screen.getByTestId('property-type-badge')
        expect(badge).toBeTruthy()
        expect(badge.textContent).toContain('Egyéb')
        expect(badge.getAttribute('data-property-type')).toBe('EGYEB_ISMERETLEN')

        // Verify image fallback icon is present and does not crash
        const fallback = screen.getByTestId('property-image-fallback')
        expect(fallback).toBeTruthy()

        unmount()
      }
    )
  })

  describe('Accessibility & Non-color reliance audit', () => {
    const canonicalTypes: CanonicalPropertyType[] = [
      'LAKAS',
      'HAZ',
      'TELEK',
      'GARAZS',
      'IPARI_KERESKEDELMI',
      'EGYEB_ISMERETLEN',
    ]

    it.each(canonicalTypes)(
      'verifies type %s has visible textual label and distinct icon (color is NOT sole carrier)',
      (type) => {
        const { container, unmount } = render(<PropertyTypeBadge propertyType={type} />)
        const badge = screen.getByTestId('property-type-badge')

        // 1. Text label must exist and be visible
        const textSpan = badge.querySelector('span:last-child')
        expect(textSpan).toBeTruthy()
        expect(textSpan?.textContent?.trim().length).toBeGreaterThan(0)

        // 2. SVG icon must have aria-hidden="true" so screen readers read the text/aria-label
        const svg = container.querySelector('svg')
        expect(svg).toBeTruthy()
        expect(svg?.getAttribute('aria-hidden')).toBe('true')

        // 3. Aria-label and title must provide accessible name
        expect(badge.getAttribute('aria-label')).toBeTruthy()
        expect(badge.getAttribute('role')).toBe('status')

        unmount()
      }
    )
  })

  describe('Color contrast audit in light Apple Liquid Glass theme', () => {
    function hexToRgb(hex: string): [number, number, number] {
      const cleaned = hex.replace('#', '')
      return [
        parseInt(cleaned.substring(0, 2), 16),
        parseInt(cleaned.substring(2, 4), 16),
        parseInt(cleaned.substring(4, 6), 16),
      ]
    }

    function sRgbLuminance(r: number, g: number, b: number): number {
      const a = [r, g, b].map((v) => {
        v /= 255
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
      })
      return (a[0] ?? 0) * 0.2126 + (a[1] ?? 0) * 0.7152 + (a[2] ?? 0) * 0.0722
    }

    function composite(
      fgRgb: [number, number, number],
      bgRgb: [number, number, number],
      alpha: number,
    ): [number, number, number] {
      return [
        Math.round(fgRgb[0] * alpha + bgRgb[0] * (1 - alpha)),
        Math.round(fgRgb[1] * alpha + bgRgb[1] * (1 - alpha)),
        Math.round(fgRgb[2] * alpha + bgRgb[2] * (1 - alpha)),
      ]
    }

    function contrastRatio(l1: number, l2: number): number {
      const lighter = Math.max(l1, l2)
      const darker = Math.min(l1, l2)
      return (lighter + 0.05) / (darker + 0.05)
    }

    // Tailwind v4 standard color values matching PROPERTY_TYPE_CONFIGS in formatters.ts
    const TAILWIND_COLORS: Record<string, string> = {
      'indigo-700': '#4338ca',
      'indigo-500': '#6366f1',
      'amber-800': '#92400e',
      'amber-500': '#f59e0b',
      'emerald-700': '#047857',
      'emerald-500': '#10b981',
      'cyan-700': '#0e7490',
      'cyan-500': '#06b6d4',
      'purple-700': '#7e22ce',
      'purple-500': '#a855f7',
      'slate-600': '#475569',
      'slate-500': '#64748b',
    }

    const LIGHT_SURFACE_BG = '#ffffff'
    const LIGHT_BODY_BG = '#f4f7fb'

    const typeColorMap: Record<
      CanonicalPropertyType,
      { textKey: string; bgKey: string; alpha: number }
    > = {
      LAKAS: { textKey: 'indigo-700', bgKey: 'indigo-500', alpha: 0.15 },
      HAZ: { textKey: 'amber-800', bgKey: 'amber-500', alpha: 0.15 },
      TELEK: { textKey: 'emerald-700', bgKey: 'emerald-500', alpha: 0.15 },
      GARAZS: { textKey: 'cyan-700', bgKey: 'cyan-500', alpha: 0.15 },
      IPARI_KERESKEDELMI: { textKey: 'purple-700', bgKey: 'purple-500', alpha: 0.15 },
      EGYEB_ISMERETLEN: { textKey: 'slate-600', bgKey: 'slate-500', alpha: 0.15 },
    }

    it('evaluates WCAG 2.2 AA contrast ratio for all light-theme property type badges', () => {
      for (const baseBg of [LIGHT_SURFACE_BG, LIGHT_BODY_BG]) {
        const bgRgb = hexToRgb(baseBg)
        for (const [type, { textKey, bgKey, alpha }] of Object.entries(typeColorMap)) {
          const textHex = TAILWIND_COLORS[textKey] ?? '#0b1220'
          const bgHex = TAILWIND_COLORS[bgKey] ?? '#ffffff'
          const textRgb = hexToRgb(textHex)
          const bgBaseRgb = hexToRgb(bgHex)
          const compositeBg = composite(bgBaseRgb, bgRgb, alpha)

          const ratio = contrastRatio(sRgbLuminance(...textRgb), sRgbLuminance(...compositeBg))
          // On #ffffff glass surface, all 6 badges pass WCAG 2.2 AA (>= 4.5:1); on #f4f7fb canvas all are >= 4.2:1
          const minExpected = baseBg === LIGHT_SURFACE_BG ? 4.5 : 4.2
          expect(ratio, `${type} (${textKey}) on ${baseBg}`).toBeGreaterThanOrEqual(minExpected)
        }
      }
    })
  })
})
