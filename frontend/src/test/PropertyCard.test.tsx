import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { PropertyCard } from '../components/listings/PropertyCard'
import { IngatlanItem } from '../types/property'
import { useUiStore } from '../store/useUiStore'

describe('PropertyCard component (#270)', () => {
  const mockItem: IngatlanItem = {
    id: 'test-1',
    portal: 'Duna House',
    url: 'https://dh.hu/ingatlan/123',
    title: 'Szép felújított lakás Debrecen belvárosában',
    price: '75 M Ft',
    location: 'Debrecen',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isNew: true,
    viewed: false,
    newConstruction: true,
    imageUrl: 'https://dh.hu/images/test-1.jpg',
    priceInfo: {
      amountMillion: 75.0,
      amountHuf: 75000000,
      rawText: '75 M Ft',
    },
    detailsInfo: {
      areaSquareMeters: 85,
      roomCount: 3,
      pricePerSquareMeter: 882352,
    },
  }

  beforeEach(() => {
    localStorage.clear()
    useUiStore.setState({ starredUrls: [], starredItemsCache: {} })
  })

  it('renders title, price, parameters, thumbnail, new construction badge, and details trigger button (#270)', () => {
    render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    expect(screen.getByText('Szép felújított lakás Debrecen belvárosában')).toBeTruthy()
    expect(screen.getByText('75 M Ft')).toBeTruthy()
    expect(screen.getByText('85 m²')).toBeTruthy()
    expect(screen.getByText('3 szoba')).toBeTruthy()
    expect(screen.getByText('ÚJ')).toBeTruthy()
    expect(screen.getByTestId('new-construction-badge')).toBeTruthy()

    // Details drawer button is present on the card face (#270)
    // Portal badge is present on the card face (#288)
    expect(screen.getByTestId('property-portal-badge').textContent).toContain('Duna House')

    // Details drawer button is present on the card face (#270, #288)
    const detailsBtn = screen.getByTestId('details-button')
    expect(detailsBtn).toBeTruthy()
    expect(detailsBtn.textContent).toContain('Részletek')

    const img = screen.getByRole('img') as HTMLImageElement
    expect(img.getAttribute('src')).toBe('https://dh.hu/images/test-1.jpg')

    // Title of new unseen item has pulse-title class
    const titleEl = screen.getByText('Szép felújított lakás Debrecen belvárosában')
    expect(titleEl.className).toContain('pulse-title')
  })

  it('shows fallback placeholder with portal name when imageUrl is missing or fails to load', () => {
    const itemWithoutImg: IngatlanItem = { ...mockItem, imageUrl: undefined }
    render(<PropertyCard item={itemWithoutImg} onMarkViewed={() => {}} />)
    expect(screen.getByTestId('property-image-fallback')).toBeTruthy()
    expect(screen.getAllByText('Duna House').length).toBeGreaterThanOrEqual(2)
  })

  it('toggles favorite star and persists in store and localStorage', () => {
    render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    const starBtn = screen.getByRole('button', { name: /Hozzáadás a kedvencekhez/i })
    fireEvent.click(starBtn)
    expect(useUiStore.getState().starredUrls).toContain('https://dh.hu/ingatlan/123')
    expect(JSON.parse(localStorage.getItem('starred_urls') || '[]')).toContain('https://dh.hu/ingatlan/123')

    const unstarBtn = screen.getByRole('button', { name: /Eltávolítás a kedvencekből/i })
    fireEvent.click(unstarBtn)
    expect(useUiStore.getState().starredUrls).not.toContain('https://dh.hu/ingatlan/123')
  })

  it('applies opacity-70 and dimmed styling when item is viewed (#270)', () => {
    const viewedItem: IngatlanItem = { ...mockItem, viewed: true }
    render(<PropertyCard item={viewedItem} onMarkViewed={() => {}} />)
    const card = screen.getByTestId('property-card')
    expect(card.className).toContain('opacity-70')
    expect(card.className).toContain('grayscale-[15%]')
  })

  it('clicking card title or thumbnail opens advertisement in new window and marks as viewed (#270)', () => {
    vi.stubGlobal('open', vi.fn())
    const handleMarkViewed = vi.fn()
    render(<PropertyCard item={mockItem} onMarkViewed={handleMarkViewed} />)

    // Title click
    const titleEl = screen.getByText('Szép felújított lakás Debrecen belvárosában')
    fireEvent.click(titleEl)
    expect(window.open).toHaveBeenCalledWith('https://dh.hu/ingatlan/123', '_blank', 'noopener,noreferrer')
    expect(handleMarkViewed).toHaveBeenCalledWith('https://dh.hu/ingatlan/123')

    // Image thumbnail click
    const img = screen.getByRole('img')
    fireEvent.click(img)
    expect(window.open).toHaveBeenCalledWith('https://dh.hu/ingatlan/123', '_blank', 'noopener,noreferrer')
  })

  it('does not display sources-count-badge for single-source listings', () => {
    render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    expect(screen.queryByTestId('sources-count-badge')).toBeNull()
    expect(screen.queryByTestId('multi-source-drawer')).toBeNull()
  })

  it('displays details button with source count badge, toggles drawer, and highlights lowest active price for multi-source items (#260, #261, #270)', () => {
    const multiItem: IngatlanItem = {
      ...mockItem,
      id: 'multi-item-123',
      sourcesCount: 3,
      price: '72 M Ft',
      priceInfo: {
        amountHuf: 72000000,
        amountMillion: 72.0,
      },
      sourceListings: [
        {
          portal: 'Duna House',
          url: 'https://dh.hu/1',
          priceHuf: 75000000,
          status: 'ACTIVE',
        },
        {
          portal: 'Otthon Centrum',
          url: 'https://oc.hu/2',
          priceHuf: 72000000,
          status: 'ACTIVE',
        },
        {
          portal: 'Jófogás',
          url: 'https://jofogas.hu/3',
          priceHuf: 68000000,
          status: 'INACTIVE', // Inactive source with lower price should NOT override active lowest
        },
      ],
    }

    render(<PropertyCard item={multiItem} onMarkViewed={() => {}} />)

    // 1. Prominent source count badge is rendered on details button
    const badge = screen.getByTestId('sources-count-badge')
    expect(badge).toBeTruthy()
    expect(badge.textContent).toContain('3 forrás')
    expect(badge.getAttribute('aria-expanded')).toBe('false')
    expect(badge.getAttribute('aria-controls')).toBe('drawer-multi-item-123')
    expect(badge.getAttribute('aria-label')).toBe('3 forrás megjelenítése')

    // 2. Primary highlighted price is the lowest ACTIVE price (72 M Ft, not the 68M inactive)
    expect(screen.getByText('72 M Ft')).toBeTruthy()

    // 3. Drawer is initially not rendered
    expect(screen.queryByTestId('multi-source-drawer')).toBeNull()

    // 4. Click badge to expand drawer
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('true')
    const drawer = screen.getByTestId('multi-source-drawer')
    expect(drawer).toBeTruthy()
    expect(drawer.getAttribute('id')).toBe('drawer-multi-item-123')

    // 5. Click badge again to collapse drawer
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('multi-source-drawer')).toBeNull()
  })

  it('resiliently handles rapid toggle clicks without state corruption or leakage', () => {
    const multiItem: IngatlanItem = {
      ...mockItem,
      id: 'rapid-toggle-item',
      sourcesCount: 5,
      sourceListings: [
        { portal: 'Duna House', url: 'https://dh.hu/1', priceHuf: 75000000, status: 'ACTIVE' },
        { portal: 'Otthon Centrum', url: 'https://oc.hu/2', priceHuf: 74000000, status: 'ACTIVE' },
      ],
    }

    render(<PropertyCard item={multiItem} onMarkViewed={() => {}} />)
    const badge = screen.getByTestId('sources-count-badge')

    // Click 1: Open
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('true')
    expect(screen.getByTestId('multi-source-drawer')).toBeTruthy()

    // Click 2: Close
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('multi-source-drawer')).toBeNull()

    // Click 3: Open
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('true')
    expect(screen.getByTestId('multi-source-drawer')).toBeTruthy()

    // Click 4: Close
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByTestId('multi-source-drawer')).toBeNull()

    // Click 5: Open
    fireEvent.click(badge)
    expect(badge.getAttribute('aria-expanded')).toBe('true')
    expect(screen.getByTestId('multi-source-drawer')).toBeTruthy()
  })

  it('clicking source count badge stops event propagation to card container', () => {
    vi.stubGlobal('open', vi.fn())
    const handleMarkViewed = vi.fn()

    const multiItem: IngatlanItem = {
      ...mockItem,
      id: 'prop-stop-item',
      sourcesCount: 2,
      sourceListings: [
        { portal: 'Duna House', url: 'https://dh.hu/1', priceHuf: 75000000, status: 'ACTIVE' },
        { portal: 'Otthon Centrum', url: 'https://oc.hu/2', priceHuf: 74000000, status: 'ACTIVE' },
      ],
    }

    render(<PropertyCard item={multiItem} onMarkViewed={handleMarkViewed} />)
    const badge = screen.getByTestId('sources-count-badge')

    fireEvent.click(badge)

    // Verify window.open was NOT triggered by clicking the badge
    expect(window.open).not.toHaveBeenCalled()
    expect(handleMarkViewed).not.toHaveBeenCalled()
  })

  it('accurately evaluates lowest active price across 10 sources with mixed states and nulls', () => {
    const complexItem: IngatlanItem = {
      ...mockItem,
      id: 'complex-pricing-item',
      sourcesCount: 10,
      price: '80 M Ft', // Fallback
      priceInfo: { amountHuf: 80000000, amountMillion: 80.0 },
      sourceListings: [
        // 1. Inactive with very low price (should be IGNORED)
        { portal: 'DH', url: 'https://dh.hu/cheap-inactive', priceHuf: 40000000, status: 'INACTIVE' },
        // 2. Active with null price (should be IGNORED)
        { portal: 'OC', url: 'https://oc.hu/null-price', priceHuf: undefined, status: 'ACTIVE' },
        // 3. Active with 0 price (should be IGNORED)
        { portal: 'JF', url: 'https://jf.hu/zero-price', priceHuf: 0, status: 'ACTIVE' },
        // 4. Active with 68 M Ft (WINNER: LOWEST ACTIVE)
        { portal: 'KB', url: 'https://kb.hu/lowest-active', priceHuf: 68000000, status: 'ACTIVE' },
        // 5. Active with 72 M Ft
        { portal: 'IB', url: 'https://ib.hu/5', priceHuf: 72000000, status: 'ACTIVE' },
        // 6. Active with 75 M Ft
        { portal: 'DH', url: 'https://dh.hu/6', priceHuf: 75000000, status: 'ACTIVE' },
        // 7. Inactive with 60 M Ft (should be IGNORED)
        { portal: 'OC', url: 'https://oc.hu/7', priceHuf: 60000000, status: 'INACTIVE' },
        // 8. Active with 79 M Ft
        { portal: 'JF', url: 'https://jf.hu/8', priceHuf: 79000000, status: 'ACTIVE' },
        // 9. Active with 85 M Ft
        { portal: 'KB', url: 'https://kb.hu/9', priceHuf: 85000000, status: 'ACTIVE' },
        // 10. Active with 90 M Ft
        { portal: 'IB', url: 'https://ib.hu/10', priceHuf: 90000000, status: 'ACTIVE' },
      ],
    }

    render(<PropertyCard item={complexItem} onMarkViewed={() => {}} />)

    // Expected lowest active price is 68 M Ft (#286: exact Ft is in details drawer only)
    expect(screen.getByText('68 M Ft')).toBeTruthy()
    expect(screen.queryByText('68 000 000 Ft')).toBeNull()

    // Inactive 40M / 60M should NOT appear in card primary pricing
    expect(screen.queryByText('40 M Ft')).toBeNull()
    expect(screen.queryByText('60 M Ft')).toBeNull()
  })

  it('renders details drawer with metadata and quality score when opened (#270)', () => {
    const itemWithScore: IngatlanItem = {
      ...mockItem,
      qualityScore: 89,
      qualityDeductions: ['-11 pont: Enyhe adateltérés'],
    }
    render(<PropertyCard item={itemWithScore} onMarkViewed={() => {}} />)

    // Open details drawer
    const detailsBtn = screen.getByTestId('details-button')
    fireEvent.click(detailsBtn)

    // Details drawer content
    expect(screen.getByTestId('multi-source-drawer')).toBeTruthy()
    expect(screen.getByTestId('drawer-quality-section')).toBeTruthy()
    expect(screen.getByText('89/100 pont')).toBeTruthy()
    expect(screen.getByText(/Kiváló adatminőség/i)).toBeTruthy()
    expect(screen.getByText('-11 pont: Enyhe adateltérés')).toBeTruthy()
    expect(screen.getByTestId('property-type-badge')).toBeTruthy()
    expect(screen.getByText('Lakás')).toBeTruthy()
    // In single-source item, redundant portal and price are removed from drawer (#301)
    expect(screen.queryByTestId('sources-list-section')).toBeNull()
    expect(screen.getAllByText('Duna House').length).toBe(1)
  })

  it('highlights starred property cards and overrides viewed grayscale styling (#289)', () => {
    // 1. Unstarred, unviewed card
    const { rerender } = render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    const card = screen.getByTestId('property-card')
    expect(card.getAttribute('data-starred')).toBeNull()
    expect(card.className).not.toContain('border-amber-500/50')
    expect(card.className).not.toContain('opacity-70')

    // 2. Viewed card without star -> should have opacity-70 and grayscale
    const viewedItem: IngatlanItem = { ...mockItem, viewed: true }
    rerender(<PropertyCard item={viewedItem} onMarkViewed={() => {}} />)
    expect(card.getAttribute('data-starred')).toBeNull()
    expect(card.className).toContain('opacity-70')
    expect(card.className).toContain('grayscale-[15%]')
    expect(card.className).not.toContain('border-amber-500/50')

    // 3. Starred and viewed card -> should override grayscale/opacity and display amber border & ring highlight (#289)
    act(() => {
      useUiStore.setState({ starredUrls: [mockItem.url] })
    })
    rerender(<PropertyCard item={viewedItem} onMarkViewed={() => {}} />)
    expect(card.getAttribute('data-starred')).toBe('true')
    expect(card.className).toContain('border-amber-500/50')
    expect(card.className).toContain('ring-amber-500/30')
    expect(card.className).not.toContain('opacity-70')
    expect(card.className).not.toContain('grayscale-[15%]')

    // 4. Starred and unviewed card -> also maintains amber border & ring highlight
    rerender(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    expect(card.getAttribute('data-starred')).toBe('true')
    expect(card.className).toContain('border-amber-500/50')
    expect(card.className).toContain('ring-amber-500/30')
    expect(card.className).not.toContain('opacity-70')
    expect(card.className).not.toContain('grayscale-[15%]')
  })
})
