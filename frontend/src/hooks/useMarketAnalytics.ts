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
    // Category medians & sample sizes (#263 & #264)
    lakasMedianPricePerSqm: number | null
    hazMedianPricePerSqm: number | null
    telekMedianPricePerSqm: number | null
    residentialMedianPricePerSqm: number | null
    plotMedianPricePerSqm: number | null
    lakasSampleSize: number
    hazSampleSize: number
    telekSampleSize: number
    residentialSampleSize: number
    plotSampleSize: number
    insufficientData: boolean
    bargainCount: number
    bargainPercent: number
    activeCategory: 'lakas' | 'haz' | 'telek' | null
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
  if (lower === upper) return sorted[lower]
  return sorted[lower] * (1 - weight) + sorted[upper] * weight
}

function calculateStdDev(arr: number[], mean: number): number {
  if (arr.length <= 1) return 0
  const sumSquaredDiffs = arr.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0)
  return Math.sqrt(sumSquaredDiffs / (arr.length - 1))
}

export function normalizeCategoryType(type?: string | null): 'lakas' | 'haz' | 'telek' | null {
  if (!type) return null
  const upper = type.toUpperCase()
  if (upper.includes('LAKAS') || upper === 'FLAT' || upper === 'APARTMENT') return 'lakas'
  if (upper.includes('HAZ') || upper === 'HOUSE') return 'haz'
  if (upper.includes('TELEK') || upper === 'LAND' || upper === 'PLOT') return 'telek'
  return null
}

export function useMarketAnalytics(
  items: IngatlanItem[],
  targetPropertyTypeOrOptions?: string | null | { targetPropertyType?: string | null }
): MarketAnalyticsData {
  const targetPropertyType =
    typeof targetPropertyTypeOrOptions === 'string'
      ? targetPropertyTypeOrOptions
      : targetPropertyTypeOrOptions?.targetPropertyType ?? null

  return useMemo(() => {
    const targetCategory = normalizeCategoryType(targetPropertyType)
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
          lakasMedianPricePerSqm: null,
          hazMedianPricePerSqm: null,
          telekMedianPricePerSqm: null,
          residentialMedianPricePerSqm: null,
          plotMedianPricePerSqm: null,
          lakasSampleSize: 0,
          hazSampleSize: 0,
          telekSampleSize: 0,
          residentialSampleSize: 0,
          plotSampleSize: 0,
          insufficientData: true,
          bargainCount: 0,
          bargainPercent: 0,
          activeCategory: targetCategory,
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
    const lakasSqmPrices: number[] = []
    const hazSqmPrices: number[] = []
    const telekSqmPrices: number[] = []
    const residentialSqmPrices: number[] = []
    const plotSqmPrices: number[] = []

    const cityMap = new Map<string, IngatlanItem[]>()
    // Bucket map for (city#type) -> sqm prices
    const segmentMap = new Map<string, number[]>()

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

      const sqPrice =
        item.detailsInfo?.pricePerSquareMeter ??
        (item.priceInfo?.amountHuf && item.detailsInfo?.areaSquareMeters && item.detailsInfo.areaSquareMeters > 0
          ? Math.round(item.priceInfo.amountHuf / item.detailsInfo.areaSquareMeters)
          : null)

      if (sqPrice != null && sqPrice > 0) {
        sqmPrices.push(sqPrice)

        const rawType = (item.propertyType || '').toUpperCase()
        if (rawType.includes('LAKAS') || rawType === 'FLAT' || rawType === 'APARTMENT') {
          lakasSqmPrices.push(sqPrice)
          residentialSqmPrices.push(sqPrice)
        } else if (rawType.includes('HAZ') || rawType === 'HOUSE') {
          hazSqmPrices.push(sqPrice)
          residentialSqmPrices.push(sqPrice)
        } else if (rawType.includes('TELEK') || rawType === 'LAND' || rawType === 'PLOT') {
          telekSqmPrices.push(sqPrice)
          plotSqmPrices.push(sqPrice)
        }

        const cityName = item.city || item.location || 'Ismeretlen'
        const bucketKey = `${cityName.toLowerCase()}#${rawType}`
        if (!segmentMap.has(bucketKey)) {
          segmentMap.set(bucketKey, [])
        }
        segmentMap.get(bucketKey)!.push(sqPrice)
      }

      const cityName = item.city || item.location || 'Ismeretlen'
      if (!cityMap.has(cityName)) {
        cityMap.set(cityName, [])
      }
      cityMap.get(cityName)!.push(item)
    })

    const totalCount = items.length

    // Category medians with >= 5 minimum sample size guard
    const lakasSampleSize = lakasSqmPrices.length
    const hazSampleSize = hazSqmPrices.length
    const telekSampleSize = telekSqmPrices.length
    const residentialSampleSize = residentialSqmPrices.length
    const plotSampleSize = plotSqmPrices.length

    const lakasMedianPricePerSqm = lakasSampleSize >= 5 ? Math.round(calculateMedian(lakasSqmPrices)) : null
    const hazMedianPricePerSqm = hazSampleSize >= 5 ? Math.round(calculateMedian(hazSqmPrices)) : null
    const telekMedianPricePerSqm = telekSampleSize >= 5 ? Math.round(calculateMedian(telekSqmPrices)) : null
    const residentialMedianPricePerSqm = residentialSampleSize >= 5 ? Math.round(calculateMedian(residentialSqmPrices)) : null
    const plotMedianPricePerSqm = plotSampleSize >= 5 ? Math.round(calculateMedian(plotSqmPrices)) : null

    // Determine active category (#276): explicit target category or inferred single-category dataset
    const sampleCounts: Record<'lakas' | 'haz' | 'telek', number> = {
      lakas: lakasSampleSize,
      haz: hazSampleSize,
      telek: telekSampleSize,
    }
    const categoriesWithSamples = (['lakas', 'haz', 'telek'] as const).filter(
      (cat) => sampleCounts[cat] > 0
    )

    const activeCategory =
      targetCategory && sampleCounts[targetCategory] > 0
        ? targetCategory
        : categoriesWithSamples.length === 1
          ? categoriesWithSamples[0]
          : targetCategory ?? null

    // Intelligent insufficientData evaluation (#276):
    // Focus specifically on the searched/dominant category's sample size if available
    const insufficientData =
      activeCategory === 'lakas'
        ? lakasSampleSize < 5
        : activeCategory === 'haz'
          ? hazSampleSize < 5
          : activeCategory === 'telek'
            ? telekSampleSize < 5
            : totalCount < 5

    const avgPriceMillion = prices.length > 0 ? prices.reduce((a, b) => a + b, 0) / prices.length : 0
    const medianPriceMillion = calculateMedian(prices)
    const minPriceMillion = prices.length > 0 ? Math.min(...prices) : 0
    const maxPriceMillion = prices.length > 0 ? Math.max(...prices) : 0
    const priceStdDev = calculateStdDev(prices, avgPriceMillion)

    const avgPricePerSqm = sqmPrices.length > 0 ? Math.round(sqmPrices.reduce((a, b) => a + b, 0) / sqmPrices.length) : 0

    // Dynamic Median Ft/m² aligned with search criteria and results (#276)
    let medianPricePerSqm = 0
    if (activeCategory === 'lakas' && lakasMedianPricePerSqm != null) {
      medianPricePerSqm = lakasMedianPricePerSqm
    } else if (activeCategory === 'haz' && hazMedianPricePerSqm != null) {
      medianPricePerSqm = hazMedianPricePerSqm
    } else if (activeCategory === 'telek' && telekMedianPricePerSqm != null) {
      medianPricePerSqm = telekMedianPricePerSqm
    } else if (residentialMedianPricePerSqm != null) {
      medianPricePerSqm = residentialMedianPricePerSqm
    } else if (plotMedianPricePerSqm != null && residentialSampleSize === 0) {
      medianPricePerSqm = plotMedianPricePerSqm
    } else if (sqmPrices.length >= 5) {
      medianPricePerSqm = Math.round(calculateMedian(sqmPrices))
    }

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
            percentage: totalCount > 0 ? Math.round((inBin.length / totalCount) * 100) : 0,
          })
        }
      }
    }

    // 2. Build Histogram for Sqm Price (ezer Ft/m²)
    const sqmPriceBins: HistogramBin[] = []
    if (sqmPrices.length > 0) {
      const sqmRange = maxPricePerSqm - minPricePerSqm
      const numBins = Math.min(8, Math.max(4, Math.ceil(sqmRange / 100_000)))
      const step = sqmRange > 0 ? Math.ceil(sqmRange / numBins / 50_000) * 50_000 || 100_000 : 100_000
      const start = Math.floor(minPricePerSqm / step) * step

      for (let i = 0; i < numBins; i++) {
        const binMin = start + i * step
        const binMax = binMin + step
        const inBin = items.filter((it) => {
          const val = it.detailsInfo?.pricePerSquareMeter
          if (val == null) return false
          return i === numBins - 1 ? val >= binMin && val <= binMax : val >= binMin && val < binMax
        })

        if (inBin.length > 0 || (binMin <= maxPricePerSqm && binMax >= minPricePerSqm)) {
          const sum = inBin.reduce((acc, it) => acc + (it.detailsInfo?.pricePerSquareMeter || 0), 0)
          sqmPriceBins.push({
            rangeLabel: `${Math.round(binMin / 1000)}k - ${Math.round(binMax / 1000)}k`,
            min: binMin,
            max: binMax,
            count: inBin.length,
            newCount: inBin.filter((it) => it.isNew).length,
            avgVal: inBin.length > 0 ? Math.round(sum / inBin.length) : 0,
            percentage: totalCount > 0 ? Math.round((inBin.length / totalCount) * 100) : 0,
          })
        }
      }
    }

    // 3. City Comparisons
    const cityComparison: CityStat[] = []
    const cityAvgSqmMap = new Map<string, number>()

    cityMap.forEach((cityItems, cityName) => {
      const cityPrices = cityItems
        .map((it) => it.priceInfo?.amountMillion)
        .filter((p): p is number => p != null && p > 0)
      const citySqmPrices = cityItems
        .map((it) => it.detailsInfo?.pricePerSquareMeter)
        .filter((p): p is number => p != null && p > 0)

      const cityAvgPrice = cityPrices.length > 0 ? cityPrices.reduce((a, b) => a + b, 0) / cityPrices.length : 0
      const cityMedianPrice = calculateMedian(cityPrices)
      const cityAvgSqm = citySqmPrices.length > 0 ? Math.round(citySqmPrices.reduce((a, b) => a + b, 0) / citySqmPrices.length) : 0
      const cityMedianSqm = Math.round(calculateMedian(citySqmPrices))

      cityAvgSqmMap.set(cityName, cityAvgSqm)

      cityComparison.push({
        city: cityName,
        count: cityItems.length,
        newCount: cityItems.filter((it) => it.isNew).length,
        avgPriceMillion: Math.round(cityAvgPrice * 10) / 10,
        medianPriceMillion: Math.round(cityMedianPrice * 10) / 10,
        avgPricePerSqm: cityAvgSqm,
        medianPricePerSqm: cityMedianSqm,
        minPriceMillion: cityPrices.length > 0 ? Math.min(...cityPrices) : 0,
        maxPriceMillion: cityPrices.length > 0 ? Math.max(...cityPrices) : 0,
      })
    })

    cityComparison.sort((a, b) => b.count - a.count)

    // 4. Scatter Plot Points & Bargain Hunter Deals
    const scatterData: ScatterPoint[] = []
    const belowMarketDeals: BargainItem[] = []
    let detectedBargainsCount = 0

    items.forEach((item) => {
      const area = item.detailsInfo?.areaSquareMeters
      const price = item.priceInfo?.amountMillion
      const sqmPrice =
        item.detailsInfo?.pricePerSquareMeter || (area && price ? Math.round((price * 1_000_000) / area) : 0)

      if (area && area > 0 && price && price > 0) {
        const city = item.city || item.location || 'Ismeretlen'
        const rawType = (item.propertyType || '').toUpperCase()
        const bucketKey = `${city.toLowerCase()}#${rawType}`
        const bucketPrices = segmentMap.get(bucketKey) || []
        const bucketMedian = bucketPrices.length >= 5 ? calculateMedian(bucketPrices) : 0

        const cityAvg = cityAvgSqmMap.get(city) || avgPricePerSqm

        let isBargain = Boolean(item.isBargain)
        if (!isBargain) {
          if (bucketMedian > 0 && sqmPrice > 0) {
            isBargain = sqmPrice <= bucketMedian * 0.80
          } else if (cityAvg > 0 && sqmPrice > 0) {
            isBargain = sqmPrice <= cityAvg * 0.85
          }
        }

        const isPriceDrop = (item.priceDelta != null && item.priceDelta < 0) || (item.priceChangePercent != null && item.priceChangePercent < 0)

        let segment: 'bargain' | 'fair' | 'premium' = 'fair'
        const refPrice = bucketMedian > 0 ? bucketMedian : cityAvg
        if (isBargain || isPriceDrop) {
          segment = 'bargain'
          detectedBargainsCount++
        } else if (refPrice > 0 && sqmPrice > refPrice * 1.25) {
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
          isDiscounted: isPriceDrop,
          priceDelta: item.priceDelta,
          priceChangePercent: item.priceChangePercent,
          isBargain,
          segment,
        })

        if (isBargain && refPrice > 0) {
          const diff = refPrice - sqmPrice
          const discountPct = Math.round((diff / refPrice) * 100)
          belowMarketDeals.push({
            item,
            cityAvgSqmPrice: Math.round(refPrice),
            discountPercent: discountPct,
            sqmDiffFt: diff,
          })
        }
      }
    })

    belowMarketDeals.sort((a, b) => b.discountPercent - a.discountPercent)

    // Top price drops sorted by largest absolute price drop
    const topPriceDrops = items
      .filter((it) => (it.priceDelta != null && it.priceDelta < 0) || (it.priceChangePercent != null && it.priceChangePercent < 0))
      .sort((a, b) => Math.abs(b.priceDelta ?? 0) - Math.abs(a.priceDelta ?? 0))

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
        lakasMedianPricePerSqm,
        hazMedianPricePerSqm,
        telekMedianPricePerSqm,
        residentialMedianPricePerSqm,
        plotMedianPricePerSqm,
        lakasSampleSize,
        hazSampleSize,
        telekSampleSize,
        residentialSampleSize,
        plotSampleSize,
        insufficientData,
        bargainCount: detectedBargainsCount,
        bargainPercent: totalCount > 0 ? Math.round((detectedBargainsCount / totalCount) * 100) : 0,
        activeCategory,
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
  }, [items, targetPropertyType])
}
