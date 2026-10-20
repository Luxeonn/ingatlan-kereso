import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PropertyCard } from '../components/listings/PropertyCard'
import { IngatlanItem } from '../types/property'

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

  it('renders title, portal badge, price, and parameters', () => {
    render(<PropertyCard item={mockItem} onMarkViewed={() => {}} />)
    expect(screen.getByText('Szép felújított lakás Debrecen belvárosában')).toBeTruthy()
    expect(screen.getByText('Duna House')).toBeTruthy()
    expect(screen.getByText('75 M Ft')).toBeTruthy()
    expect(screen.getByText('85 m²')).toBeTruthy()
    expect(screen.getByText('3 szoba')).toBeTruthy()
    expect(screen.getByText('ÚJ')).toBeTruthy()
  })

  it('triggers mark as viewed on toggle button click', () => {
    const handleMarkViewed = vi.fn()
    render(<PropertyCard item={mockItem} onMarkViewed={handleMarkViewed} />)
    const viewedBtn = screen.getByTitle('Megjelölés megtekintettként')
    fireEvent.click(viewedBtn)
    expect(handleMarkViewed).toHaveBeenCalledWith('https://dh.hu/ingatlan/123')
  })
})
