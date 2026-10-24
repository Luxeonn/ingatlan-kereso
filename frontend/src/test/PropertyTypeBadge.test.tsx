import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PropertyTypeBadge } from '../components/listings/PropertyTypeBadge'
import { CanonicalPropertyType } from '../types/property'

describe('PropertyTypeBadge component', () => {
  const canonicalCases: Array<{ type: CanonicalPropertyType; expectedLabel: string }> = [
    { type: 'LAKAS', expectedLabel: 'Lakás' },
    { type: 'HAZ', expectedLabel: 'Ház' },
    { type: 'TELEK', expectedLabel: 'Telek' },
    { type: 'GARAZS', expectedLabel: 'Garázs' },
    { type: 'IPARI_KERESKEDELMI', expectedLabel: 'Ipari / Kereskedelmi' },
    { type: 'EGYEB_ISMERETLEN', expectedLabel: 'Egyéb' },
  ]

  it.each(canonicalCases)(
    'renders canonical type $type with Hungarian label "$expectedLabel"',
    ({ type, expectedLabel }) => {
      render(<PropertyTypeBadge propertyType={type} />)

      const badge = screen.getByTestId('property-type-badge')
      expect(badge).toBeTruthy()
      expect(badge.getAttribute('data-property-type')).toBe(type)
      expect(badge.textContent).toContain(expectedLabel)
    }
  )

  it('provides WCAG accessibility attributes (role, aria-label, aria-hidden icon)', () => {
    const { container } = render(<PropertyTypeBadge propertyType="HAZ" />)

    const badge = screen.getByTestId('property-type-badge')
    expect(badge.getAttribute('role')).toBe('status')
    expect(badge.getAttribute('aria-label')).toBe('Ingatlan típusa: Ház')
    expect(badge.getAttribute('title')).toBe('Ingatlan típusa: Ház')

    const svgIcon = container.querySelector('svg')
    expect(svgIcon).toBeTruthy()
    expect(svgIcon?.getAttribute('aria-hidden')).toBe('true')
  })

  it('resolves string aliases to canonical types', () => {
    const { unmount } = render(<PropertyTypeBadge propertyType="családi ház" />)
    expect(screen.getByTestId('property-type-badge').getAttribute('data-property-type')).toBe('HAZ')
    unmount()

    render(<PropertyTypeBadge propertyType="társasházi lakás" />)
    expect(screen.getByTestId('property-type-badge').getAttribute('data-property-type')).toBe('LAKAS')
  })

  it('falls back safely to EGYEB_ISMERETLEN for null, undefined, empty, and unknown values', () => {
    const testValues = [null, undefined, '', '   ', 'ismeretlen_kategoria_xyz']

    for (const val of testValues) {
      const { unmount } = render(<PropertyTypeBadge propertyType={val} />)
      const badge = screen.getByTestId('property-type-badge')
      expect(badge.getAttribute('data-property-type')).toBe('EGYEB_ISMERETLEN')
      expect(badge.textContent).toContain('Egyéb')
      unmount()
    }
  })

  it('respects showIcon=false and custom className', () => {
    const { container } = render(
      <PropertyTypeBadge propertyType="TELEK" showIcon={false} className="custom-test-class" />
    )

    const badge = screen.getByTestId('property-type-badge')
    expect(badge.className).toContain('custom-test-class')
    expect(container.querySelector('svg')).toBeNull()
    expect(badge.textContent).toContain('Telek')
  })
})
