import { useMemo } from 'react'
import { IngatlanItem } from '../types/property'

export interface HistogramBin {
  rangeLabel: string
  min: number
  max: number
  count: number
  newCount: number
  avgVal: number
  percentage: number
}

export interface CityStat {
  city: string
  count: number
  newCount: number
  avgPriceMillion: number
  medianPriceMillion: number
  avgPricePerSqm: number
  medianPricePerSqm: number
  minPriceMillion: number
  maxPriceMillion: number
}

export interface ScatterPoint {
  id: string
  title: string
  city: string
  portal: string
  url: string
  area: number
  price: number
  sqmPrice: number
  isNew: boolean
  isDiscounted: boolean
  priceDelta?: number
  priceChangePercent?: number
  isBargain: boolean
  segment: 'bargain' | 'fair' | 'premium'
}

export interface BargainItem {
  item: IngatlanItem
  cityAvgSqmPrice: number
  discountPercent: number
  sqmDiffFt: number
}

export interface MarketAnalyticsData {
  totalCount: number
  kpi: {
    medianPriceMillion: number
    avgPriceMillion: number
    minPriceMillion: number
    maxPriceMillion: number
    medianPricePerSqm: number
    avgPricePerSqm: number
    minPricePerSqm: number
    maxPricePerSqm: number
    priceStdDev: number
    sqmPriceStdDev: number
    discountedCount: number
    discountedPercent: number
    newCount: number
    newPercent: number
  }
  distribution: {
    priceBins: HistogramBin[]
    sqmPriceBins: HistogramBin[]
    priceP25: number
    priceP50: number
    priceP75: number
    sqmPriceP25: number
    sqmPriceP50: number
    sqmPriceP75: number
  }
  cityComparison: CityStat[]
  scatterData: ScatterPoint[]
  bargains: {
    topPriceDrops: IngatlanItem[]
    belowMarketDeals: BargainItem[]
  }
}

// Math and stats helpers
function calculateMedian(arr: number[]): number {
  if (arr.length === 0) return 0
  const sorted = [...arr].sort((a, b) => a - b)
  const mid = Math.floor(sorted.length / 2)
  return sorted.length % 2 !== 0 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2
}

function calculatePercentile(arr: number[], p: number): number {
  if (arr.length === 0) return 0
  const sorted = [...arr].sort((a, b) => a - b)
  const index = (sorted.length - 1) * p
  const lower = Math.floor(index)
  const upper = Math.ceil(index)
  const weight = index - lower
  return sorted[lower] * (1 - weight) + sorted[upper] * weight
}

function calculateStdDev(arr: number[], mean: number): number {
  if (arr.length <= 1) return 0
  const sumSquares = arr.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0)
  return Math.sqrt(sumSquares / (arr.length - 1))
}

export function useMarketAnalytics(items: IngatlanItem[]): MarketAnalyticsData {
  return useMemo(() => {
    if (!items || items.length === 0) {
      return {
        totalCount: 0,
        kpi: {
          medianPriceMillion: 0,
          avgPriceMillion: 0,
          minPriceMillion: 0,
          maxPriceMillion: 0,
          medianPricePerSqm: 0,
          avgPricePerSqm: 0,
          minPricePerSqm: 0,
          maxPricePerSqm: 0,
          priceStdDev: 0,
          sqmPriceStdDev: 0,
          discountedCount: 0,
          discountedPercent: 0,
          newCount: 0,
          newPercent: 0,
        },
        distribution: {
          priceBins: [],
          sqmPriceBins: [],
          priceP25: 0,
          priceP50: 0,
          priceP75: 0,
          sqmPriceP25: 0,
          sqmPriceP50: 0,
          sqmPriceP75: 0,
        },
        cityComparison: [],
        scatterData: [],
        bargains: {
          topPriceDrops: [],
          belowMarketDeals: [],
        },
      }
    }

    const prices: number[] = []
    const sqmPrices: number[] = []
    const cityMap = new Map<string, IngatlanItem[]>()
    let discountedCount = 0
    let newCount = 0

    items.forEach((item) => {
      if (item.isNew) newCount++
      if ((item.priceDelta != null && item.priceDelta < 0) || (item.priceChangePercent != null && item.priceChangePercent < 0)) {
        discountedCount++
      }

      const pMillion = item.priceInfo?.amountMillion
      if (pMillion != null && pMillion > 0) {
        prices.push(pMillion)
      }

      const sqPrice = item.detailsInfo?.pricePerSqmFt
      if (sqPrice != null && sqPrice > 0) {
        sqmPrices.push(sqPrice)
      }

      const cityName = item.city || item.location || 'Ismeretlen'
      if (!cityMap.has(cityName)) {
        cityMap.set(cityName, [])
      }
      cityMap.get(cityName)!.push(item)
    })

    const totalCount = items.length
    const avgPriceMillion = prices.length > 0 ? prices.reduce((a, b) => a + b, 0) / prices.length : 0
    const medianPriceMillion = calculateMedian(prices)
    const minPriceMillion = prices.length > 0 ? Math.min(...prices) : 0
    const maxPriceMillion = prices.length > 0 ? Math.max(...prices) : 0
    const priceStdDev = calculateStdDev(prices, avgPriceMillion)

    const avgPricePerSqm = sqmPrices.length > 0 ? Math.round(sqmPrices.reduce((a, b) => a + b, 0) / sqmPrices.length) : 0
    const medianPricePerSqm = Math.round(calculateMedian(sqmPrices))
    const minPricePerSqm = sqmPrices.length > 0 ? Math.min(...sqmPrices) : 0
    const maxPricePerSqm = sqmPrices.length > 0 ? Math.max(...sqmPrices) : 0
    const sqmPriceStdDev = Math.round(calculateStdDev(sqmPrices, avgPricePerSqm))

    const priceP25 = calculatePercentile(prices, 0.25)
    const priceP50 = medianPriceMillion
    const priceP75 = calculatePercentile(prices, 0.75)

    const sqmPriceP25 = calculatePercentile(sqmPrices, 0.25)
    const sqmPriceP50 = medianPricePerSqm
    const sqmPriceP75 = calculatePercentile(sqmPrices, 0.75)

    // 1. Build Histogram for Price (Million Ft)
    const priceBins: HistogramBin[] = []
    if (prices.length > 0) {
      const priceRange = maxPriceMillion - minPriceMillion
      const numBins = Math.min(8, Math.max(4, Math.ceil(priceRange / 5)))
      const step = priceRange > 0 ? Math.ceil((priceRange / numBins) * 2) / 2 || 5 : 5
      const start = Math.floor(minPriceMillion / step) * step

      for (let i = 0; i < numBins; i++) {
        const binMin = start + i * step
        const binMax = binMin + step
        const inBin = items.filter((it) => {
          const val = it.priceInfo?.amountMillion
          if (val == null) return false
          return i === numBins - 1 ? val >= binMin && val <= binMax : val >= binMin && val < binMax
        })

        if (inBin.length > 0 || (binMin <= maxPriceMillion && binMax >= minPriceMillion)) {
          const sum = inBin.reduce((acc, it) => acc + (it.priceInfo?.amountMillion || 0), 0)
          priceBins.push({
            rangeLabel: `${binMin.toFixed(0)} - ${binMax.toFixed(0)} M Ft`,
            min: binMin,
            max: binMax,
            count: inBin.length,
            newCount: inBin.filter((it) => it.isNew).length,
            avgVal: inBin.length > 0 ? Math.round((sum / inBin.length) * 10) / 10 : 0,
            percentage: Math.round((inBin.length / totalCount) * 100),
          })
        }
      }
    }

    // 2. Build Histogram for Sqm Price (ezer Ft/m²)
    const sqmPriceBins: HistogramBin[] = []
    if (sqmPrices.length > 0) {
      const minSqmK = Math.floor(minPricePerSqm / 1000)
      const maxSqmK = Math.ceil(maxPricePerSqm / 1000)
      const sqmRange = maxSqmK - minSqmK
      const numBins = Math.min(8, Math.max(4, Math.ceil(sqmRange / 50)))
      const step = sqmRange > 0 ? Math.max(25, Math.ceil(sqmRange / numBins / 25) * 25) : 50
      const start = Math.floor(minSqmK / step) * step

      for (let i = 0; i < numBins; i++) {
        const binMinK = start + i * step
        const binMaxK = binMinK + step
        const binMinFt = binMinK * 1000
        const binMaxFt = binMaxK * 1000

        const inBin = items.filter((it) => {
          const val = it.detailsInfo?.pricePerSqmFt
          if (val == null) return false
          return i === numBins - 1 ? val >= binMinFt && val <= binMaxFt : val >= binMinFt && val < binMaxFt
        })

        if (inBin.length > 0 || (binMinFt <= maxPricePerSqm && binMaxFt >= minPricePerSqm)) {
          const sum = inBin.reduce((acc, it) => acc + (it.detailsInfo?.pricePerSqmFt || 0), 0)
          sqmPriceBins.push({
            rangeLabel: `${binMinK} - ${binMaxK} eFt/m²`,
            min: binMinFt,
            max: binMaxFt,
            count: inBin.length,
            newCount: inBin.filter((it) => it.isNew).length,
            avgVal: inBin.length > 0 ? Math.round(sum / inBin.length) : 0,
            percentage: Math.round((inBin.length / totalCount) * 100),
          })
        }
      }
    }

    // 3. City Comparison
    const cityComparison: CityStat[] = []
    const cityAvgSqmMap = new Map<string, number>()

    cityMap.forEach((cityItems, city) => {
      const cPrices = cityItems.map((it) => it.priceInfo?.amountMillion).filter((p): p is number => p != null && p > 0)
      const cSqm = cityItems.map((it) => it.detailsInfo?.pricePerSqmFt).filter((s): s is number => s != null && s > 0)

      const cAvgPrice = cPrices.length > 0 ? Math.round((cPrices.reduce((a, b) => a + b, 0) / cPrices.length) * 10) / 10 : 0
      const cMedianPrice = Math.round(calculateMedian(cPrices) * 10) / 10
      const cAvgSqm = cSqm.length > 0 ? Math.round(cSqm.reduce((a, b) => a + b, 0) / cSqm.length) : 0
      const cMedianSqm = Math.round(calculateMedian(cSqm))

      cityAvgSqmMap.set(city, cAvgSqm)

      cityComparison.push({
        city,
        count: cityItems.length,
        newCount: cityItems.filter((it) => it.isNew).length,
        avgPriceMillion: cAvgPrice,
        medianPriceMillion: cMedianPrice,
        avgPricePerSqm: cAvgSqm,
        medianPricePerSqm: cMedianSqm,
        minPriceMillion: cPrices.length > 0 ? Math.min(...cPrices) : 0,
        maxPriceMillion: cPrices.length > 0 ? Math.max(...cPrices) : 0,
      })
    })

    cityComparison.sort((a, b) => b.count - a.count)

    // 4. Scatter Data (Area vs Price) & Bargain Detection
    const scatterData: ScatterPoint[] = []
    const belowMarketDeals: BargainItem[] = []

    items.forEach((item) => {
      const area = item.detailsInfo?.floorAreaSqm
      const price = item.priceInfo?.amountMillion
      const sqmPrice = item.detailsInfo?.pricePerSqmFt || (area && price ? Math.round((price * 1_000_000) / area) : 0)

      if (area && area > 0 && price && price > 0) {
        const city = item.city || item.location || 'Ismeretlen'
        const cityAvg = cityAvgSqmMap.get(city) || avgPricePerSqm

        let segment: 'bargain' | 'fair' | 'premium' = 'fair'
        const isBargainByCity = cityAvg > 0 && sqmPrice > 0 && sqmPrice <= cityAvg * 0.85
        const isBargainByDrop = item.priceDelta != null && item.priceDelta < 0

        if (isBargainByCity || isBargainByDrop) {
          segment = 'bargain'
        } else if (cityAvg > 0 && sqmPrice > cityAvg * 1.25) {
          segment = 'premium'
        }

        scatterData.push({
          id: item.id || item.url,
          title: item.title || 'Ingatlan',
          city,
          portal: item.portal,
          url: item.url,
          area,
          price,
          sqmPrice,
          isNew: Boolean(item.isNew),
          isDiscounted: isBargainByDrop,
          priceDelta: item.priceDelta,
          priceChangePercent: item.priceChangePercent,
          isBargain: segment === 'bargain',
          segment,
        })

        if (isBargainByCity && cityAvg > 0) {
          const diff = cityAvg - sqmPrice
          const discountPct = Math.round((diff / cityAvg) * 100)
          belowMarketDeals.push({
            item,
            cityAvgSqmPrice: cityAvg,
            discountPercent: discountPct,
            sqmDiffFt: diff,
          })
        }
      }
    })

    belowMarketDeals.sort((a, b) => b.discountPercent - a.discountPercent)

    // Top price drops
    const topPriceDrops = items
      .filter((it) => (it.priceDelta != null && it.priceDelta < 0) || (it.priceChangePercent != null && it.priceChangePercent < 0))
      .sort((a, b) => (a.priceDelta ?? 0) - (b.priceDelta ?? 0))

    return {
      totalCount,
      kpi: {
        medianPriceMillion: Math.round(medianPriceMillion * 10) / 10,
        avgPriceMillion: Math.round(avgPriceMillion * 10) / 10,
        minPriceMillion: Math.round(minPriceMillion * 10) / 10,
        maxPriceMillion: Math.round(maxPriceMillion * 10) / 10,
        medianPricePerSqm,
        avgPricePerSqm,
        minPricePerSqm,
        maxPricePerSqm,
        priceStdDev: Math.round(priceStdDev * 10) / 10,
        sqmPriceStdDev,
        discountedCount,
        discountedPercent: totalCount > 0 ? Math.round((discountedCount / totalCount) * 100) : 0,
        newCount,
        newPercent: totalCount > 0 ? Math.round((newCount / totalCount) * 100) : 0,
      },
      distribution: {
        priceBins,
        sqmPriceBins,
        priceP25: Math.round(priceP25 * 10) / 10,
        priceP50: Math.round(priceP50 * 10) / 10,
        priceP75: Math.round(priceP75 * 10) / 10,
        sqmPriceP25: Math.round(sqmPriceP25),
        sqmPriceP50: Math.round(sqmPriceP50),
        sqmPriceP75: Math.round(sqmPriceP75),
      },
      cityComparison,
      scatterData,
      bargains: {
        topPriceDrops,
        belowMarketDeals: belowMarketDeals.slice(0, 10),
      },
    }
  }, [items])
}
