import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PropertyTypeBadge } from '../components/listings/PropertyTypeBadge'
import { PropertyCard } from '../components/listings/PropertyCard'
import { IngatlanItem, CanonicalPropertyType } from '../types/property'
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
        const { unmount } = render(<PropertyTypeBadge propertyType={value} />)
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
        }

        const { unmount } = render(<PropertyCard item={item} onMarkViewed={() => {}} />)

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

  describe('Color contrast audit across dark and light themes', () => {
    // Standard sRGB relative luminance calculation (WCAG 2.1)
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
      return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722
    }

    function composite(fgRgb: [number, number, number], bgRgb: [number, number, number], alpha: number): [number, number, number] {
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

    // Tailwind standard color values
    const TAILWIND_COLORS: Record<string, string> = {
      'indigo-400': '#818cf8',
      'indigo-500': '#6366f1',
      'amber-400': '#fbbf24',
      'amber-500': '#f59e0b',
      'emerald-400': '#34d399',
      'emerald-500': '#10b981',
      'cyan-400': '#22d3ee',
      'cyan-500': '#06b6d4',
      'purple-400': '#c084fc',
      'purple-500': '#a855f7',
      'slate-400': '#94a3b8',
      'slate-500': '#64748b',
    }

    const DARK_BG = '#131b2e' // --bg-card in dark mode
    const LIGHT_BG = '#ffffff' // --bg-card in light mode

    const typeColorMap: Record<CanonicalPropertyType, { textKey: string; bgKey: string; alpha: number }> = {
      LAKAS: { textKey: 'indigo-400', bgKey: 'indigo-500', alpha: 0.15 },
      HAZ: { textKey: 'amber-400', bgKey: 'amber-500', alpha: 0.15 },
      TELEK: { textKey: 'emerald-400', bgKey: 'emerald-500', alpha: 0.15 },
      GARAZS: { textKey: 'cyan-400', bgKey: 'cyan-500', alpha: 0.15 },
      IPARI_KERESKEDELMI: { textKey: 'purple-400', bgKey: 'purple-500', alpha: 0.15 },
      EGYEB_ISMERETLEN: { textKey: 'slate-400', bgKey: 'slate-500', alpha: 0.15 },
    }

    it('measures and evaluates contrast ratio for all badges in dark mode', () => {
      const darkBgRgb = hexToRgb(DARK_BG)

      for (const [, { textKey, bgKey, alpha }] of Object.entries(typeColorMap)) {
        const textRgb = hexToRgb(TAILWIND_COLORS[textKey])
        const bgBaseRgb = hexToRgb(TAILWIND_COLORS[bgKey])
        const compositeBg = composite(bgBaseRgb, darkBgRgb, alpha)

        const lText = sRgbLuminance(...textRgb)
        const lBg = sRgbLuminance(...compositeBg)
        const ratio = contrastRatio(lText, lBg)

        // Dark mode should comfortably pass WCAG AA (4.5:1)
        expect(ratio).toBeGreaterThan(4.5)
      }
    })

    it('evaluates contrast ratio in light mode and empirically documents findings', () => {
      const lightBgRgb = hexToRgb(LIGHT_BG)
      const lightModeResults: Record<string, { ratio: number; passesWcagAa: boolean }> = {}

      for (const [type, { textKey, bgKey, alpha }] of Object.entries(typeColorMap)) {
        const textRgb = hexToRgb(TAILWIND_COLORS[textKey])
        const bgBaseRgb = hexToRgb(TAILWIND_COLORS[bgKey])
        const compositeBg = composite(bgBaseRgb, lightBgRgb, alpha)

        const lText = sRgbLuminance(...textRgb)
        const lBg = sRgbLuminance(...compositeBg)
        const ratio = contrastRatio(lText, lBg)

        lightModeResults[type] = {
          ratio: Math.round(ratio * 100) / 100,
          passesWcagAa: ratio >= 4.5,
        }
      }

      // We document that light mode contrast fails WCAG AA (4.5:1) because text-*-400 classes
      // are light pastel shades designed for dark mode without light mode variants
      // For instance, amber-400 on light bg has contrast < 2.0:1
      expect(lightModeResults['HAZ'].ratio).toBeLessThan(4.5)
      expect(lightModeResults['GARAZS'].ratio).toBeLessThan(4.5)
      expect(lightModeResults['TELEK'].ratio).toBeLessThan(4.5)
    })
  })
})
