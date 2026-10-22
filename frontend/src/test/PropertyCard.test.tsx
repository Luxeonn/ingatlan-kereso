import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PropertyCard } from '../components/listings/PropertyCard'
import { IngatlanItem } from '../types/property'
import { useUiStore } from '../store/useUiStore'

describe('PropertyCard component', () => {
  const mockItem: IngatlanItem = {
    id: 'test-1',
    portal: 'Duna House',
    url: 'https://dh.hu/ingatlan/123',
    title: 'Szép felújított lakás Debrecen belvárosában',
    price: '75 M Ft',
    location: 'Debrecen',
    city: 'Debrecen',
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

  it('renders title, portal badge, price, parameters, thumbnail, and new construction badge', () => {
    render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    expect(screen.getByText('Szép felújított lakás Debrecen belvárosában')).toBeTruthy()
    expect(screen.getByText('Duna House')).toBeTruthy()
    expect(screen.getByText('75 M Ft')).toBeTruthy()
    expect(screen.getByText('85 m²')).toBeTruthy()
    expect(screen.getByText('3 szoba')).toBeTruthy()
    expect(screen.getByText('ÚJ')).toBeTruthy()
    expect(screen.getByTestId('new-construction-badge')).toBeTruthy()

    const img = screen.getByRole('img') as HTMLImageElement
    expect(img.getAttribute('src')).toBe('https://dh.hu/images/test-1.jpg')

    // Title of new unseen item has pulse-title class
    const titleEl = screen.getByText('Szép felújított lakás Debrecen belvárosában')
    expect(titleEl.className).toContain('pulse-title')
  })

  it('shows fallback placeholder when imageUrl is missing or fails to load', () => {
    const itemWithoutImg: IngatlanItem = { ...mockItem, imageUrl: undefined }
    render(<PropertyCard item={itemWithoutImg} onMarkViewed={() => {}} />)
    expect(screen.getByTestId('property-image-fallback')).toBeTruthy()
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

  it('applies opacity-70 styling when item is viewed', () => {
    const viewedItem: IngatlanItem = { ...mockItem, viewed: true }
    render(<PropertyCard item={viewedItem} onMarkViewed={() => {}} />)
    const card = screen.getByTestId('property-card')
    expect(card.className).toContain('opacity-70')
  })

  it('triggers mark as viewed on toggle button click', () => {
    const handleMarkViewed = vi.fn()
    render(<PropertyCard item={mockItem} onMarkViewed={handleMarkViewed} />)
    const viewedBtn = screen.getByTitle('Megjelölés megtekintettként')
    fireEvent.click(viewedBtn)
    expect(handleMarkViewed).toHaveBeenCalledWith('https://dh.hu/ingatlan/123')
  })
})
