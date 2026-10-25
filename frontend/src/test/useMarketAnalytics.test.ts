import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useMarketAnalytics } from '../hooks/useMarketAnalytics'
import { IngatlanItem } from '../types/property'

describe('useMarketAnalytics hook', () => {
  it('returns zero and empty structures when items list is empty (zero crash)', () => {
    const { result } = renderHook(() => useMarketAnalytics([]))

    expect(result.current.totalCount).toBe(0)
    expect(result.current.kpi.avgPriceMillion).toBe(0)
    expect(result.current.kpi.medianPriceMillion).toBe(0)
    expect(result.current.kpi.avgPricePerSqm).toBe(0)
    expect(result.current.distribution.priceBins).toEqual([])
    expect(result.current.distribution.sqmPriceBins).toEqual([])
    expect(result.current.cityComparison).toEqual([])
    expect(result.current.scatterData).toEqual([])
    expect(result.current.bargains.topPriceDrops).toEqual([])
    expect(result.current.bargains.belowMarketDeals).toEqual([])
  })

  it('calculates KPIs, percentiles and bins accurately with sample listings', () => {
    const sampleItems: IngatlanItem[] = [
      {
        id: '1',
        portal: 'dh',
        url: 'https://dh.hu/1',
        title: 'Lakás 1 Debrecen',
        price: '60 M Ft',
        location: 'Debrecen',
        city: 'Debrecen',
        isNew: true,
        priceInfo: { amountMillion: 60, amountHuf: 60000000 },
        detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 600000 },
      },
      {
        id: '2',
        portal: 'oc',
        url: 'https://oc.hu/2',
        title: 'Lakás 2 Debrecen',
        price: '80 M Ft',
        location: 'Debrecen',
        city: 'Debrecen',
        isNew: false,
        priceInfo: { amountMillion: 80, amountHuf: 80000000 },
        detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 800000 },
      },
      {
        id: '3',
        portal: 'jofogas',
        url: 'https://jofogas.hu/3',
        title: 'Lakás 3 Debrecen',
        price: '100 M Ft',
        location: 'Debrecen',
        city: 'Debrecen',
        isNew: false,
        priceDelta: -5000000,
        priceChangePercent: -4.8,
        priceInfo: { amountMillion: 100, amountHuf: 100000000 },
        detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 1000000 },
      },
      {
        id: '4',
        portal: 'koltozzbe',
        url: 'https://koltozzbe.hu/4',
        title: 'Ház Hajdúsámson',
        price: '50 M Ft',
        location: 'Hajdúsámson',
        city: 'Hajdúsámson',
        isNew: true,
        priceInfo: { amountMillion: 50, amountHuf: 50000000 },
        detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 500000 },
      },
    ]

    const { result } = renderHook(() => useMarketAnalytics(sampleItems))

    expect(result.current.totalCount).toBe(4)

    // Average price: (60 + 80 + 100 + 50) / 4 = 72.5 M Ft
    expect(result.current.kpi.avgPriceMillion).toBe(72.5)

    // Median price: sorted [50, 60, 80, 100] -> (60 + 80) / 2 = 70 M Ft
    expect(result.current.kpi.medianPriceMillion).toBe(70)

    // Min and Max
    expect(result.current.kpi.minPriceMillion).toBe(50)
    expect(result.current.kpi.maxPriceMillion).toBe(100)

    // Average Sqm price: (600k + 800k + 1000k + 500k) / 4 = 725k
    expect(result.current.kpi.avgPricePerSqm).toBe(725000)

    // New items count: 2 out of 4 (50%)
    expect(result.current.kpi.newCount).toBe(2)
    expect(result.current.kpi.newPercent).toBe(50)

    // Discounted items count: 1 out of 4 (25%)
    expect(result.current.kpi.discountedCount).toBe(1)
    expect(result.current.kpi.discountedPercent).toBe(25)

    // City comparison checks
    expect(result.current.cityComparison.length).toBe(2)
    const debrecenStat = result.current.cityComparison.find((c) => c.city === 'Debrecen')
    expect(debrecenStat).toBeDefined()
    expect(debrecenStat?.count).toBe(3)
    expect(debrecenStat?.avgPriceMillion).toBe(80)
    expect(debrecenStat?.avgPricePerSqm).toBe(800000)

    const samsonStat = result.current.cityComparison.find((c) => c.city === 'Hajdúsámson')
    expect(samsonStat).toBeDefined()
    expect(samsonStat?.count).toBe(1)
    expect(samsonStat?.avgPriceMillion).toBe(50)

    // Price drops check
    expect(result.current.bargains.topPriceDrops.length).toBe(1)
    expect(result.current.bargains.topPriceDrops[0].id).toBe('3')

    // Below market deals check: Debrecen average sqm is 800k; 800k * 0.85 = 680k.
    // Item 1 has 600k, which is < 680k -> detected as below market bargain!
    expect(result.current.bargains.belowMarketDeals.length).toBeGreaterThan(0)
    expect(result.current.bargains.belowMarketDeals[0].item.id).toBe('1')

    // Bins check
    expect(result.current.distribution.priceBins.length).toBeGreaterThan(0)
    expect(result.current.distribution.sqmPriceBins.length).toBeGreaterThan(0)

    // Scatter data check
    expect(result.current.scatterData.length).toBe(4)
    const scatterItem1 = result.current.scatterData.find((s) => s.id === '1')
    expect(scatterItem1?.segment).toBe('bargain')
  })

  it('accurately evaluates activeCategory and insufficientData for targeted category (#276)', () => {
    const mockHouses: IngatlanItem[] = Array.from({ length: 6 }, (_, i) => ({
      id: `h-${i}`,
      portal: 'oc',
      url: `https://example.com/h/${i}`,
      title: `Ház ${i + 1}`,
      price: `${80 + i * 5} M Ft`,
      location: 'Debrecen',
      city: 'Debrecen',
      propertyType: 'HAZ',
      priceInfo: { amountMillion: 80 + i * 5, amountHuf: (80 + i * 5) * 1_000_000 },
      detailsInfo: { areaSquareMeters: 100, pricePerSquareMeter: 800_000 + i * 20_000 },
    }))

    // Targeted Ház search with 6 samples (>= 5)
    const { result } = renderHook(() => useMarketAnalytics(mockHouses, 'haz'))
    expect(result.current.kpi.activeCategory).toBe('haz')
    expect(result.current.kpi.hazSampleSize).toBe(6)
    expect(result.current.kpi.hazMedianPricePerSqm).toBe(850_000)
    expect(result.current.kpi.medianPricePerSqm).toBe(850_000)
    expect(result.current.kpi.insufficientData).toBe(false)

    // Insufficient data when targeted category has < 5 samples
    const { result: fewResult } = renderHook(() => useMarketAnalytics(mockHouses.slice(0, 3), 'haz'))
    expect(fewResult.current.kpi.activeCategory).toBe('haz')
    expect(fewResult.current.kpi.hazSampleSize).toBe(3)
    expect(fewResult.current.kpi.hazMedianPricePerSqm).toBeNull()
    expect(fewResult.current.kpi.insufficientData).toBe(true)

    // Auto-inferred category when all listings belong to the same property type
    const { result: inferredResult } = renderHook(() => useMarketAnalytics(mockHouses))
    expect(inferredResult.current.kpi.activeCategory).toBe('haz')
    expect(inferredResult.current.kpi.insufficientData).toBe(false)
  })
})
