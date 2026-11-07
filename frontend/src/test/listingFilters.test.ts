import { describe, it, expect } from 'vitest'
import {
  normalizeSearchText,
  isItemBargain,
  getBargainPercentage,
  applyItemFilters,
  filterAndSortItems,
} from '../utils/listingFilters'
import { createMockIngatlanItem } from './testFixtures'
import type { BargainInfo, IngatlanItem } from '../types/property'

describe('listingFilters utility', () => {
  describe('normalizeSearchText', () => {
    it('normalizes diacritics, converts to lowercase, and handles falsy values', () => {
      expect(normalizeSearchText('Árvíztűrő TÜKÖRFÚRÓGÉP')).toBe('arvizturo tukorfurogep')
      expect(normalizeSearchText('Hajdúszoboszló, Bánomkert')).toBe('hajduszoboszlo, banomkert')
      expect(normalizeSearchText('')).toBe('')
      expect(normalizeSearchText(null)).toBe('')
      expect(normalizeSearchText(undefined)).toBe('')
    })
  })

  describe('isItemBargain', () => {
    it('returns false for null or undefined items', () => {
      expect(isItemBargain(null as unknown as IngatlanItem)).toBe(false)
      expect(isItemBargain(undefined as unknown as IngatlanItem)).toBe(false)
    })

    it('identifies bargains via isBargain flag or bargainInfo or percentage threshold', () => {
      const normalItem = createMockIngatlanItem({ isBargain: false })
      expect(isItemBargain(normalItem)).toBe(false)

      const flaggedItem = createMockIngatlanItem({ isBargain: true })
      expect(isItemBargain(flaggedItem)).toBe(true)

      const nestedFlagItem = createMockIngatlanItem({
        bargainInfo: { isBargain: true, percentageBelowMedian: 10 } as unknown as BargainInfo,
      })
      expect(isItemBargain(nestedFlagItem)).toBe(true)

      const percentageItem = createMockIngatlanItem({ percentageBelowMedian: 22 })
      expect(isItemBargain(percentageItem)).toBe(true)

      const nestedPercentageItem = createMockIngatlanItem({
        bargainInfo: { isBargain: false, percentageBelowMedian: 25 } as unknown as BargainInfo,
      })
      expect(isItemBargain(nestedPercentageItem)).toBe(true)

      const belowThresholdItem = createMockIngatlanItem({ percentageBelowMedian: 15 })
      expect(isItemBargain(belowThresholdItem)).toBe(false)
    })
  })

  describe('getBargainPercentage', () => {
    it('extracts percentage or falls back to -Infinity safely', () => {
      expect(getBargainPercentage(createMockIngatlanItem({ percentageBelowMedian: 30 }))).toBe(30)
      expect(
        getBargainPercentage(
          createMockIngatlanItem({
            bargainInfo: { percentageBelowMedian: 18 } as unknown as BargainInfo,
          }),
        ),
      ).toBe(18)
      expect(getBargainPercentage(createMockIngatlanItem())).toBe(-Infinity)
    })
  })

  describe('applyItemFilters', () => {
    const item1 = createMockIngatlanItem({
      id: '1',
      title: 'Kossuth utcai családi ház',
      city: 'Debrecen',
      location: 'Nagyerdő',
      isNew: true,
      viewed: false,
      newConstruction: true,
      isBargain: true,
      qualityScore: 92,
      portal: 'Duna House',
      sourcesCount: 1,
    })

    const item2 = createMockIngatlanItem({
      id: '2',
      title: 'Belvárosi garzonlakás',
      city: 'Debrecen',
      location: 'Belváros',
      isNew: false,
      viewed: true,
      newConstruction: false,
      isBargain: false,
      qualityScore: 65,
      portal: 'Otthon Centrum',
      source: {
        portalId: 'oc',
        portalName: 'Otthon Centrum',
        directListing: true,
      },
      sourcesCount: 3,
      sourceListings: [
        { portal: 'oc', url: 'https://oc.hu/2' },
        { portal: 'dh', url: 'https://dh.hu/2' },
        { portal: 'jf', url: 'https://jofogas.hu/2' },
      ],
    })

    const items = [item1, item2]

    it('returns empty array when input is empty or invalid', () => {
      expect(applyItemFilters([], {})).toEqual([])
      expect(applyItemFilters(null as unknown as IngatlanItem[], {})).toEqual([])
    })

    it('filters by onlyNew flag', () => {
      const filtered = applyItemFilters(items, { onlyNew: true })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('1')
    })

    it('filters by excludeViewed flag', () => {
      const filtered = applyItemFilters(items, { excludeViewed: true })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('1')
    })

    it('filters by onlyNewConstruction flag', () => {
      const filtered = applyItemFilters(items, { onlyNewConstruction: true })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('1')
    })

    it('filters by onlyBargain flag', () => {
      const filtered = applyItemFilters(items, { onlyBargain: true })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('1')
    })

    it('filters by excludeMultiSource flag', () => {
      const filtered = applyItemFilters(items, { excludeMultiSource: true })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].id).toBe('1')
    })

    it('filters by minQualityScore', () => {
      const highQuality = applyItemFilters(items, { minQualityScore: 80 })
      expect(highQuality).toHaveLength(1)
      expect(highQuality[0].id).toBe('1')

      const all = applyItemFilters(items, { minQualityScore: 50 })
      expect(all).toHaveLength(2)
    })

    it('filters by selectedPortalFilter', () => {
      const dhItems = applyItemFilters(items, { selectedPortalFilter: 'dh' })
      expect(dhItems).toHaveLength(1)
      expect(dhItems[0].id).toBe('1')

      const ocItems = applyItemFilters(items, { selectedPortalFilter: 'oc' })
      expect(ocItems).toHaveLength(1)
      expect(ocItems[0].id).toBe('2')

      const allItems = applyItemFilters(items, { selectedPortalFilter: 'all' })
      expect(allItems).toHaveLength(2)
    })

    it('filters by quickSearchQuery matching multiple tokens with diacritic insensitivity', () => {
      const matchBoth = applyItemFilters(items, { quickSearchQuery: 'Debrecen' })
      expect(matchBoth).toHaveLength(2)

      const matchNagyerdohaz = applyItemFilters(items, { quickSearchQuery: 'nagyerdő haz' })
      expect(matchNagyerdohaz).toHaveLength(1)
      expect(matchNagyerdohaz[0].id).toBe('1')

      const noMatch = applyItemFilters(items, { quickSearchQuery: 'Miskolc' })
      expect(noMatch).toHaveLength(0)
    })
  })

  describe('filterAndSortItems', () => {
    it('sorts by price_asc stably even when prices are missing without returning NaN', () => {
      const cheap = createMockIngatlanItem({ id: 'c', priceInfo: { amountHuf: 30_000_000 } })
      const exp = createMockIngatlanItem({ id: 'e', priceInfo: { amountHuf: 90_000_000 } })
      const noPrice1 = createMockIngatlanItem({ id: 'n1', priceInfo: undefined })
      const noPrice2 = createMockIngatlanItem({ id: 'n2', priceInfo: undefined })

      const sorted = filterAndSortItems([noPrice1, exp, noPrice2, cheap], {
        sortOption: 'price_asc',
      })
      expect(sorted[0].id).toBe('c')
      expect(sorted[1].id).toBe('e')
      // Both noPrice items are stably placed at the end without throwing or breaking sort
      expect(['n1', 'n2']).toContain(sorted[2].id)
      expect(['n1', 'n2']).toContain(sorted[3].id)
    })

    it('sorts by price_desc stably even when prices are missing', () => {
      const cheap = createMockIngatlanItem({ id: 'c', priceInfo: { amountHuf: 30_000_000 } })
      const exp = createMockIngatlanItem({ id: 'e', priceInfo: { amountHuf: 90_000_000 } })
      const noPrice = createMockIngatlanItem({ id: 'n', priceInfo: undefined })

      const sorted = filterAndSortItems([cheap, noPrice, exp], { sortOption: 'price_desc' })
      expect(sorted[0].id).toBe('e')
      expect(sorted[1].id).toBe('c')
      expect(sorted[2].id).toBe('n')
    })

    it('sorts by area_desc', () => {
      const small = createMockIngatlanItem({ id: 's', detailsInfo: { areaSquareMeters: 40 } })
      const large = createMockIngatlanItem({ id: 'l', detailsInfo: { areaSquareMeters: 120 } })
      const noArea = createMockIngatlanItem({ id: 'n', detailsInfo: undefined })

      const sorted = filterAndSortItems([small, noArea, large], { sortOption: 'area_desc' })
      expect(sorted[0].id).toBe('l')
      expect(sorted[1].id).toBe('s')
      expect(sorted[2].id).toBe('n')
    })

    it('sorts by date_desc', () => {
      const oldItem = createMockIngatlanItem({ id: 'old', discoveredAt: '2026-01-01 10:00:00' })
      const newItem = createMockIngatlanItem({ id: 'new', discoveredAt: '2026-03-01 10:00:00' })

      const sorted = filterAndSortItems([oldItem, newItem], { sortOption: 'date_desc' })
      expect(sorted[0].id).toBe('new')
      expect(sorted[1].id).toBe('old')
    })

    it('sorts by bargain_desc without NaN issues when no bargain info exists', () => {
      const topBargain = createMockIngatlanItem({ id: 'b1', percentageBelowMedian: 35 })
      const midBargain = createMockIngatlanItem({ id: 'b2', percentageBelowMedian: 20 })
      const nonBargain1 = createMockIngatlanItem({ id: 'nb1', percentageBelowMedian: undefined })
      const nonBargain2 = createMockIngatlanItem({ id: 'nb2', percentageBelowMedian: undefined })

      const sorted = filterAndSortItems([nonBargain1, topBargain, nonBargain2, midBargain], {
        sortOption: 'bargain_desc',
      })
      expect(sorted[0].id).toBe('b1')
      expect(sorted[1].id).toBe('b2')
      expect(['nb1', 'nb2']).toContain(sorted[2].id)
      expect(['nb1', 'nb2']).toContain(sorted[3].id)
    })

    it('uses default sortOption price_asc when omitted', () => {
      const cheap = createMockIngatlanItem({ id: 'c', priceInfo: { amountHuf: 20_000_000 } })
      const exp = createMockIngatlanItem({ id: 'e', priceInfo: { amountHuf: 80_000_000 } })

      const sorted = filterAndSortItems([exp, cheap])
      expect(sorted[0].id).toBe('c')
      expect(sorted[1].id).toBe('e')
    })
  })
})
