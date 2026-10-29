import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PropertyDetailsDrawer as MultiSourceDrawer } from '../components/listings/PropertyDetailsDrawer'
import { IngatlanItem } from '../types/property'

describe('MultiSourceDrawer component (#260, #261)', () => {
  const multiSourceItem: IngatlanItem = {
    id: 'prop-group-1',
    portal: 'Otthon Centrum',
    url: 'https://oc.hu/ingatlan/789',
    title: 'Kiváló 3 szobás lakás Debrecen belváros',
    price: '75 M Ft',
    location: 'Debrecen',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    sourcesCount: 3,
    priceInfo: {
      amountMillion: 75.0,
      amountHuf: 75000000,
      rawText: '75 M Ft',
    },
    detailsInfo: {
      areaSquareMeters: 80,
      roomCount: 3,
      pricePerSquareMeter: 937500,
    },
    sourceListings: [
      {
        portal: 'Otthon Centrum',
        externalListingId: 'oc-789',
        url: 'https://oc.hu/ingatlan/789',
        title: 'OC Belvárosi Lakás',
        priceHuf: 75000000,
        status: 'ACTIVE',
        firstSeenAt: '2026-10-24T10:00:00Z',
        lastSeenAt: '2026-10-24T12:00:00Z',
        viewed: false,
      },
      {
        portal: 'Jófogás',
        externalListingId: 'jf-456',
        url: 'https://jofogas.hu/ingatlan/456',
        title: 'Jófogás 3 szobás lakás',
        priceHuf: 78000000,
        status: 'ACTIVE',
        firstSeenAt: '2026-10-23T15:00:00Z',
        lastSeenAt: '2026-10-24T11:00:00Z',
        viewed: false,
      },
      {
        portal: 'Duna House',
        externalListingId: 'dh-123',
        url: 'https://dh.hu/ingatlan/123',
        title: 'DH Családi Otthon',
        priceHuf: 80000000,
        status: 'INACTIVE',
        firstSeenAt: '2026-10-20T08:00:00Z',
        lastSeenAt: '2026-10-22T09:00:00Z',
        viewed: true,
      },
    ],
  }

  beforeEach(() => {
    vi.restoreAllMocks()
    vi.stubGlobal('open', vi.fn())
  })

  it('renders accessible drawer container with proper ARIA attributes', () => {
    render(<MultiSourceDrawer item={multiSourceItem} />)

    const drawer = screen.getByRole('region', { name: 'Hirdetés részletei' })
    expect(drawer).toBeTruthy()
    expect(drawer.getAttribute('id')).toBe('drawer-prop-group-1')
    expect(screen.getByTestId('multi-source-drawer')).toBe(drawer)
    expect(screen.getByText('Elérhető források (3)')).toBeTruthy()
    expect(screen.getByText('Legalacsonyabb ár kiemelve')).toBeTruthy()
  })

  it('renders all sources with portal badges, prices, status, and freshness', () => {
    render(<MultiSourceDrawer item={multiSourceItem} />)

    // 3 portal badges
    const portalBadges = screen.getAllByTestId('source-portal-badge')
    expect(portalBadges.length).toBe(3)
    expect(screen.getByText('Otthon Centrum')).toBeTruthy()
    expect(screen.getByText('Jófogás')).toBeTruthy()
    expect(screen.getByText('Duna House')).toBeTruthy()

    // Prices formatted
    expect(screen.getByText('75 M Ft')).toBeTruthy()
    expect(screen.getByText('75 000 000 Ft')).toBeTruthy()
    expect(screen.getByText('78 M Ft')).toBeTruthy()
    expect(screen.getByText('78 000 000 Ft')).toBeTruthy()
    expect(screen.getByText('80 M Ft')).toBeTruthy()
    expect(screen.getByText('80 000 000 Ft')).toBeTruthy()

    // Status badges (2 Active, 1 Inactive)
    const statusBadges = screen.getAllByTestId('source-status-badge')
    expect(statusBadges.length).toBe(3)
    expect(screen.getAllByText('Aktív').length).toBe(2)
    expect(screen.getByText('Inaktív')).toBeTruthy()

    // External link buttons with accessible labels
    expect(screen.getByLabelText('Megnyitás: Otthon Centrum')).toBeTruthy()
    expect(screen.getByLabelText('Megnyitás: Jófogás')).toBeTruthy()
    expect(screen.getByLabelText('Megnyitás: Duna House')).toBeTruthy()
  })

  it('clicking external link opens target URL in new tab and marks as viewed', () => {
    const handleMarkViewed = vi.fn()
    render(<MultiSourceDrawer item={multiSourceItem} onMarkViewed={handleMarkViewed} />)

    const ocButton = screen.getByLabelText('Megnyitás: Otthon Centrum')
    fireEvent.click(ocButton)

    expect(window.open).toHaveBeenCalledWith('https://oc.hu/ingatlan/789', '_blank', 'noopener,noreferrer')
    expect(handleMarkViewed).toHaveBeenCalledWith('https://oc.hu/ingatlan/789')
  })

  it('gracefully handles items with missing or empty sourceListings', () => {
    const fallbackItem: IngatlanItem = {
      id: 'fallback-1',
      portal: 'Duna House',
      url: 'https://dh.hu/ingatlan/999',
      title: 'Egyedi lakás',
      price: '50 M Ft',
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'LAKAS',
      priceInfo: { amountHuf: 50000000, amountMillion: 50.0 },
      sourceListings: [],
    }

    render(<MultiSourceDrawer item={fallbackItem} />)
    expect(screen.getByRole('region', { name: 'Hirdetés részletei' })).toBeTruthy()
    // For single source items, the sources comparison section is hidden (#301)
    expect(screen.queryByTestId('sources-list-section')).toBeNull()
    expect(screen.queryByText(/Elérhető források/)).toBeNull()
  })

  it('stress-tests high source counts (10 duplicate sources across portals)', () => {
    const tenSourcesItem: IngatlanItem = {
      ...multiSourceItem,
      id: 'high-count-item',
      sourcesCount: 10,
      sourceListings: Array.from({ length: 10 }, (_, i) => ({
        portal: i % 2 === 0 ? 'Duna House' : 'Otthon Centrum',
        externalListingId: `ext-${i + 1}`,
        url: `https://portal-${i + 1}.hu/listing/${i + 1}`,
        title: `Hirdetés variáció ${i + 1}`,
        priceHuf: 70000000 + i * 1000000,
        status: i === 9 ? 'INACTIVE' : 'ACTIVE',
        firstSeenAt: '2026-10-24T10:00:00Z',
        lastSeenAt: '2026-10-24T12:00:00Z',
        viewed: false,
      })),
    }

    render(<MultiSourceDrawer item={tenSourcesItem} />)

    expect(screen.getByText('Elérhető források (10)')).toBeTruthy()
    const sourceItems = screen.getAllByTestId('source-item')
    expect(sourceItems.length).toBe(10)

    const portalBadges = screen.getAllByTestId('source-portal-badge')
    expect(portalBadges.length).toBe(10)

    const externalLinks = screen.getAllByTestId('source-external-link')
    expect(externalLinks.length).toBe(10)
  })

  it('handles partial and missing data in source listings safely without throwing', () => {
    const partialDataItem: IngatlanItem = {
      ...multiSourceItem,
      id: 'partial-data-item',
      sourcesCount: 3,
      sourceListings: [
        {
          // Missing priceHuf, missing timestamps
          portal: 'Jófogás',
          url: 'https://jofogas.hu/listing/null-price',
          title: 'Ár nélküli hirdetés',
          priceHuf: undefined,
          status: 'ACTIVE',
          firstSeenAt: undefined,
          lastSeenAt: undefined,
        },
        {
          // Missing URL, missing portal, invalid timestamp
          portal: undefined,
          url: undefined,
          title: 'Link és portál nélküli hirdetés',
          priceHuf: 65000000,
          status: 'active', // lowercase status
          firstSeenAt: 'invalid-date-string',
        },
        {
          // Empty strings
          portal: '',
          url: '',
          priceHuf: undefined,
          status: 'INACTIVE',
        },
      ],
    }

    render(<MultiSourceDrawer item={partialDataItem} />)

    // Missing price renders "Ár a hirdetésben"
    const missingPriceTexts = screen.getAllByText('Ár a hirdetésben')
    expect(missingPriceTexts.length).toBe(2)

    // Missing portal falls back to "Forrás"
    const fallbackPortals = screen.getAllByText('Forrás')
    expect(fallbackPortals.length).toBe(2)

    // Items without URL do not render external link button
    const externalLinks = screen.getAllByTestId('source-external-link')
    expect(externalLinks.length).toBe(1) // Only the first source has a valid URL

    // Missing timestamps do not render freshness badges
    const freshnessBadges = screen.queryAllByTestId('source-freshness')
    expect(freshnessBadges.length).toBe(1) // Only the one with invalid string exists (and handled by formatDate)
  })

  it('secures external link button with noopener noreferrer and proper target in window.open', () => {
    const multiLinkItem: IngatlanItem = {
      ...multiSourceItem,
      id: 'sec-link-item',
      sourcesCount: 2,
      sourceListings: [
        {
          portal: 'Költözzbe',
          url: 'https://koltozzbe.hu/ingatlan/safe-link',
          priceHuf: 85000000,
          status: 'ACTIVE',
        },
        {
          portal: 'Jófogás',
          url: 'https://jofogas.hu/ingatlan/safe-link-2',
          priceHuf: 86000000,
          status: 'ACTIVE',
        },
      ],
    }

    render(<MultiSourceDrawer item={multiLinkItem} />)
    const linkBtn = screen.getByRole('button', { name: 'Megnyitás: Költözzbe' })
    expect(linkBtn.getAttribute('aria-label')).toBe('Megnyitás: Költözzbe')
    expect(linkBtn.getAttribute('title')).toBe('Megnyitás: Költözzbe')

    fireEvent.click(linkBtn)
    expect(window.open).toHaveBeenCalledTimes(1)
    expect(window.open).toHaveBeenCalledWith('https://koltozzbe.hu/ingatlan/safe-link', '_blank', 'noopener,noreferrer')
  })

  it('hides duplicate timestamps, m2 area, and single-source comparison row (#301)', () => {
    const singleSourceItem: IngatlanItem = {
      id: 'single-1',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/ingatlan/12345',
      title: 'Szép lakás Debrecenben',
      price: '55 M Ft',
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'LAKAS',
      details: 'Tégla építésű, 2. emelet',
      discoveredAt: '2026-10-25T10:00:00Z',
      lastPriceChangeAt: '2026-10-26T08:00:00Z',
      viewedAt: '2026-10-26T09:00:00Z',
      priceInfo: { amountHuf: 55000000, amountMillion: 55.0 },
      detailsInfo: { areaSquareMeters: 65, roomCount: 2, pricePerSquareMeter: 846153 },
      qualityScore: 95,
      qualityDeductions: [],
      sourcesCount: 1,
      sourceListings: [
        {
          portal: 'Otthon Centrum',
          url: 'https://oc.hu/ingatlan/12345',
          priceHuf: 55000000,
          status: 'ACTIVE',
        },
      ],
    }

    render(<MultiSourceDrawer item={singleSourceItem} />)

    // Besorolás és Kategória is present
    expect(screen.getByText('Besorolás és Kategória')).toBeTruthy()
    expect(screen.getByText('Lakás')).toBeTruthy()
    expect(screen.getByText('Tégla építésű, 2. emelet')).toBeTruthy()

    // Data quality section is present
    expect(screen.getByTestId('drawer-quality-section')).toBeTruthy()
    expect(screen.getByText('95/100 pont')).toBeTruthy()

    // 1. m² is NOT rendered in drawer (handled by card chip)
    expect(screen.queryByText('65 m²')).toBeNull()

    // 2. Timestamps section is removed
    expect(screen.queryByText('Időbélyegek')).toBeNull()
    expect(screen.queryByText(/Felfedezve:/)).toBeNull()
    expect(screen.queryByText(/Árváltozás:/)).toBeNull()
    expect(screen.queryByText(/Megtekintve:/)).toBeNull()

    // 3. Redundant single-source portal and price row is removed
    expect(screen.queryByTestId('sources-list-section')).toBeNull()
    expect(screen.queryByTestId('source-item')).toBeNull()
  })
})
