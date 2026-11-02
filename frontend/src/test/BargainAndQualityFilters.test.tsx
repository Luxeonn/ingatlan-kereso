import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BulkActionBar } from '../components/listings/BulkActionBar'
import { useFilterStore } from '../store/useFilterStore'
import { filterAndSortItems, isItemBargain } from '../hooks/useSearch'
import type { IngatlanItem } from '../types/property'

const mockListings: IngatlanItem[] = [
  {
    id: 'prop-1',
    portal: 'Duna House',
    url: 'https://dh.hu/1',
    title: 'Nagyerdő prémium lakás',
    price: '85 M Ft',
    location: 'Debrecen, Nagyerdő',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isNew: true,
    viewed: false,
    isBargain: true,
    percentageBelowMedian: 22.5,
    referenceSampleSize: 15,
    qualityScore: 92,
    qualityDeductions: [],
    priceInfo: { amountHuf: 85_000_000, amountMillion: 85 },
    detailsInfo: { areaSquareMeters: 80, pricePerSquareMeter: 1_062_500, roomCount: 3 },
    source: { portalId: 'dh', portalName: 'Duna House' },
  },
  {
    id: 'prop-2',
    portal: 'Otthon Centrum',
    url: 'https://oc.hu/2',
    title: 'Belvárosi felújítandó lakás',
    price: '45 M Ft',
    location: 'Debrecen, Belváros',
    city: 'Debrecen',
    propertyType: 'LAKAS',
    isNew: false,
    viewed: false,
    isBargain: false,
    qualityScore: 72,
    qualityDeductions: ['Hiányzó energetikai besorolás'],
    priceInfo: { amountHuf: 45_000_000, amountMillion: 45 },
    detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 900_000, roomCount: 2 },
    source: { portalId: 'oc', portalName: 'Otthon Centrum' },
  },
  {
    id: 'prop-3',
    portal: 'Jófogás',
    url: 'https://jofogas.hu/3',
    title: 'Gyanúsan olcsó zártkert hirdetés',
    price: '15 M Ft',
    location: 'Debrecen, Külső rész',
    city: 'Debrecen',
    propertyType: 'HAZ',
    isNew: false,
    viewed: false,
    // Bargain info via bargainInfo object
    bargainInfo: {
      isBargain: true,
      currentPricePerSqm: 300_000,
      referenceMedianPricePerSqm: 600_000,
      deltaPercent: -50,
      percentageBelowMedian: 50,
      sampleSize: 8,
    },
    qualityScore: 55, // Low quality score (< 70 outlier / suspicious)
    qualityDeductions: ['Hiányzó alaprajz és paraméterek', 'Rendkívül alacsony ár'],
    priceInfo: { amountHuf: 15_000_000, amountMillion: 15 },
    detailsInfo: { areaSquareMeters: 50, pricePerSquareMeter: 300_000, roomCount: 1 },
    source: { portalId: 'jofogas', portalName: 'Jófogás' },
  },
  {
    id: 'prop-4',
    portal: 'Ingatlan.com',
    url: 'https://ingatlan.com/4',
    title: 'Modern családi ház kerttel',
    price: '110 M Ft',
    location: 'Debrecen, Ligettelek',
    city: 'Debrecen',
    propertyType: 'HAZ',
    isNew: true,
    viewed: true,
    isBargain: false,
    // Missing qualityScore (undefined)
    priceInfo: { amountHuf: 110_000_000, amountMillion: 110 },
    detailsInfo: { areaSquareMeters: 140, pricePerSquareMeter: 785_714, roomCount: 4 },
    source: { portalId: 'ingatlancom', portalName: 'Ingatlan.com' },
  },
]

describe('Bargain and Quality Score Filter Engine (#275)', () => {
  beforeEach(() => {
    useFilterStore.getState().resetToDefaults()
  })

  describe('isItemBargain helper', () => {
    it('correctly identifies direct isBargain property', () => {
      expect(isItemBargain(mockListings[0])).toBe(true)
      expect(isItemBargain(mockListings[1])).toBe(false)
    })

    it('identifies bargainInfo nested object flag', () => {
      expect(isItemBargain(mockListings[2])).toBe(true)
    })

    it('identifies percentageBelowMedian fallback >= 20', () => {
      const itemWithPctOnly: IngatlanItem = {
        id: 'test-pct',
        portal: 'dh',
        url: 'https://test.com/1',
        title: 'Test',
        price: '50M',
        location: 'City',
        city: 'City',
        percentageBelowMedian: 24,
      }
      expect(isItemBargain(itemWithPctOnly)).toBe(true)
    })
  })

  describe('filterAndSortItems filtering logic', () => {
    it('returns all items when bargain and quality filters are not active', () => {
      const results = filterAndSortItems(mockListings, {
        sortOption: 'price_asc',
      })
      expect(results).toHaveLength(4)
    })

    it('filters strictly only bargain listings when onlyBargain is true', () => {
      const results = filterAndSortItems(mockListings, {
        onlyBargain: true,
        sortOption: 'price_asc',
      })
      expect(results).toHaveLength(2)
      expect(results.map((r) => r.id)).toEqual(['prop-3', 'prop-1']) // 15M, then 85M
    })

    it('filters out listings below quality score threshold (minQualityScore = 70)', () => {
      const results = filterAndSortItems(mockListings, {
        minQualityScore: 70,
        sortOption: 'price_asc',
      })
      // prop-1 (92) and prop-2 (72) should remain. prop-3 (55) and prop-4 (undefined) should be excluded
      expect(results).toHaveLength(2)
      expect(results.map((r) => r.id)).toEqual(['prop-2', 'prop-1'])
    })

    it('filters for excellent quality listings only (minQualityScore = 85)', () => {
      const results = filterAndSortItems(mockListings, {
        minQualityScore: 85,
        sortOption: 'price_asc',
      })
      // Only prop-1 has score 92 >= 85
      expect(results).toHaveLength(1)
      expect(results[0].id).toBe('prop-1')
    })

    it('combines onlyBargain and minQualityScore filters seamlessly', () => {
      // Both filters active: only bargains with reliable quality (>= 70)
      // prop-1 is bargain with score 92 -> MATCH
      // prop-3 is bargain but score is 55 (< 70) -> EXCLUDED
      const results = filterAndSortItems(mockListings, {
        onlyBargain: true,
        minQualityScore: 70,
        sortOption: 'price_asc',
      })
      expect(results).toHaveLength(1)
      expect(results[0].id).toBe('prop-1')
    })

    it('combines onlyBargain with portal and keyword search', () => {
      const results = filterAndSortItems(mockListings, {
        onlyBargain: true,
        selectedPortalFilter: 'dh',
        quickSearchQuery: 'Nagyerdő',
        sortOption: 'price_asc',
      })
      expect(results).toHaveLength(1)
      expect(results[0].id).toBe('prop-1')
    })
  })

  describe('useFilterStore actions and state', () => {
    it('initializes onlyBargain as false and minQualityScore as 0', () => {
      const state = useFilterStore.getState()
      expect(state.onlyBargain).toBe(false)
      expect(state.minQualityScore).toBe(0)
    })

    it('updates onlyBargain and minQualityScore via actions', () => {
      useFilterStore.getState().setOnlyBargain(true)
      expect(useFilterStore.getState().onlyBargain).toBe(true)

      useFilterStore.getState().setMinQualityScore(70)
      expect(useFilterStore.getState().minQualityScore).toBe(70)
    })

    it('resets onlyBargain and minQualityScore to defaults', () => {
      useFilterStore.getState().setOnlyBargain(true)
      useFilterStore.getState().setMinQualityScore(85)

      useFilterStore.getState().resetToDefaults()
      expect(useFilterStore.getState().onlyBargain).toBe(false)
      expect(useFilterStore.getState().minQualityScore).toBe(0)
    })
  })

  describe('BulkActionBar integration (#275)', () => {
    it('renders Bargain filter toggle with item count badge', () => {
      render(
        <BulkActionBar
          items={mockListings}
          rawItems={mockListings}
          totalRawCount={4}
          onOpenAll={vi.fn()}
          onMarkAllViewed={vi.fn()}
        />,
      )

      const bargainBtn = screen.getByTestId('bargain-filter-toggle')
      expect(bargainBtn).toBeTruthy()
      expect(bargainBtn.getAttribute('aria-pressed')).toBe('false')

      // Should display bargain count badge: 2 bargains in mockListings (prop-1, prop-3)
      const countBadge = screen.getByTestId('bargain-filter-count')
      expect(countBadge.textContent).toBe('2')

      // Clicking toggles onlyBargain in store
      fireEvent.click(bargainBtn)
      expect(useFilterStore.getState().onlyBargain).toBe(true)
    })

    it('renders Data Quality select dropdown and updates minQualityScore', () => {
      render(
        <BulkActionBar
          items={mockListings}
          rawItems={mockListings}
          totalRawCount={4}
          onOpenAll={vi.fn()}
          onMarkAllViewed={vi.fn()}
        />,
      )

      const qualitySelect = screen.getByTestId('quality-filter-select') as HTMLSelectElement
      expect(qualitySelect).toBeTruthy()
      expect(qualitySelect.value).toBe('0')

      // Change to Reliable (70)
      fireEvent.change(qualitySelect, { target: { value: '70' } })
      expect(useFilterStore.getState().minQualityScore).toBe(70)

      // Change to Excellent (85)
      fireEvent.change(qualitySelect, { target: { value: '85' } })
      expect(useFilterStore.getState().minQualityScore).toBe(85)
    })
  })
})
