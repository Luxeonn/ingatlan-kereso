import { useMemo } from 'react'
import type { IngatlanItem } from '../types/property'
import {
  type BargainItem,
  type CityStat,
  type HistogramBin,
  type HistogramOptions,
  type MarketAnalyticsData,
  type ScatterPoint,
  calculateDistributionStats,
  calculateMedian,
  calculateStdDev,
  createEmptyMarketAnalyticsData,
  hasPriceDrop,
  normalizeCategoryType,
} from '../utils/marketAnalyticsMath'
import {
  aggregateItems,
  buildCityComparison,
  buildScatterAndBargainData,
} from '../utils/marketAnalyticsAggregation'

export {
  type BargainItem,
  type CityStat,
  type HistogramBin,
  type HistogramOptions,
  type MarketAnalyticsData,
  type ScatterPoint,
  normalizeCategoryType,
}

function resolveActiveCategory(
  targetCategory: 'lakas' | 'haz' | 'telek' | null,
  sampleCounts: Record<'lakas' | 'haz' | 'telek', number>,
): 'lakas' | 'haz' | 'telek' | null {
  if (targetCategory && sampleCounts[targetCategory] > 0) {
    return targetCategory
  }
  const categoriesWithSamples = (['lakas', 'haz', 'telek'] as const).filter((cat) => sampleCounts[cat] > 0)
  if (categoriesWithSamples.length === 1) {
    return categoriesWithSamples[0]
  }
  return targetCategory ?? null
}

interface OverallMedianParams {
  activeCategory: 'lakas' | 'haz' | 'telek' | null
  categoryMedianMap: Record<'lakas' | 'haz' | 'telek', number | null>
  residentialMedian: number | null
  plotMedian: number | null
  residentialSampleSize: number
  sqmPrices: number[]
}

function resolveOverallMedianPricePerSqm(params: OverallMedianParams): number {
  const { activeCategory, categoryMedianMap, residentialMedian, plotMedian, residentialSampleSize, sqmPrices } =
    params

  if (activeCategory && categoryMedianMap[activeCategory] !== null && categoryMedianMap[activeCategory] !== undefined) {
    return categoryMedianMap[activeCategory]!
  }
  if (residentialMedian !== null && residentialMedian !== undefined) {
    return residentialMedian
  }
  if (plotMedian !== null && plotMedian !== undefined && residentialSampleSize === 0) {
    return plotMedian
  }
  if (sqmPrices.length >= 5) {
    return Math.round(calculateMedian(sqmPrices))
  }
  return 0
}

interface KpiCalculationParams {
  totalCount: number
  prices: number[]
  sqmPrices: number[]
  lakasSqmPrices: number[]
  hazSqmPrices: number[]
  telekSqmPrices: number[]
  residentialSqmPrices: number[]
  plotSqmPrices: number[]
  targetCategory: 'lakas' | 'haz' | 'telek' | null
  discountedCount: number
  newCount: number
  detectedBargainsCount: number
}

function calculateKpiStats(params: KpiCalculationParams): MarketAnalyticsData['kpi'] {
  const {
    totalCount,
    prices,
    sqmPrices,
    lakasSqmPrices,
    hazSqmPrices,
    telekSqmPrices,
    residentialSqmPrices,
    plotSqmPrices,
    targetCategory,
    discountedCount,
    newCount,
    detectedBargainsCount,
  } = params

  const lakasSampleSize = lakasSqmPrices.length
  const hazSampleSize = hazSqmPrices.length
  const telekSampleSize = telekSqmPrices.length
  const residentialSampleSize = residentialSqmPrices.length
  const plotSampleSize = plotSqmPrices.length

  const medianIfValid = (arr: number[]) => (arr.length >= 5 ? Math.round(calculateMedian(arr)) : null)
  const lakasMedianPricePerSqm = medianIfValid(lakasSqmPrices)
  const hazMedianPricePerSqm = medianIfValid(hazSqmPrices)
  const telekMedianPricePerSqm = medianIfValid(telekSqmPrices)
  const residentialMedianPricePerSqm = medianIfValid(residentialSqmPrices)
  const plotMedianPricePerSqm = medianIfValid(plotSqmPrices)

  const sampleCounts: Record<'lakas' | 'haz' | 'telek', number> = {
    lakas: lakasSampleSize,
    haz: hazSampleSize,
    telek: telekSampleSize,
  }

  const activeCategory = resolveActiveCategory(targetCategory, sampleCounts)
  const insufficientData = activeCategory ? sampleCounts[activeCategory] < 5 : totalCount < 5

  const avgPriceMillion = prices.length > 0 ? prices.reduce((a, b) => a + b, 0) / prices.length : 0
  const medianPriceMillion = calculateMedian(prices)
  const minPriceMillion = prices.length > 0 ? Math.min(...prices) : 0
  const maxPriceMillion = prices.length > 0 ? Math.max(...prices) : 0
  const priceStdDev = calculateStdDev(prices, avgPriceMillion)

  const avgPricePerSqm =
    sqmPrices.length > 0 ? Math.round(sqmPrices.reduce((a, b) => a + b, 0) / sqmPrices.length) : 0

  const categoryMedianMap: Record<'lakas' | 'haz' | 'telek', number | null> = {
    lakas: lakasMedianPricePerSqm,
    haz: hazMedianPricePerSqm,
    telek: telekMedianPricePerSqm,
  }

  const medianPricePerSqm = resolveOverallMedianPricePerSqm({
    activeCategory,
    categoryMedianMap,
    residentialMedian: residentialMedianPricePerSqm,
    plotMedian: plotMedianPricePerSqm,
    residentialSampleSize,
    sqmPrices,
  })

  const minPricePerSqm = sqmPrices.length > 0 ? Math.min(...sqmPrices) : 0
  const maxPricePerSqm = sqmPrices.length > 0 ? Math.max(...sqmPrices) : 0
  const sqmPriceStdDev = Math.round(calculateStdDev(sqmPrices, avgPricePerSqm))

  return {
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
  }
}

export function useMarketAnalytics(
  items: IngatlanItem[],
  targetPropertyTypeOrOptions?: string | null | { targetPropertyType?: string | null },
): MarketAnalyticsData {
  const targetPropertyType =
    typeof targetPropertyTypeOrOptions === 'string'
      ? targetPropertyTypeOrOptions
      : targetPropertyTypeOrOptions?.targetPropertyType ?? null

  return useMemo(() => {
    const targetCategory = normalizeCategoryType(targetPropertyType)
    if (!items || items.length === 0) {
      return createEmptyMarketAnalyticsData(targetCategory)
    }

    const agg = aggregateItems(items)
    const cityAvgSqmMap = new Map<string, number>()
    const cityComparison = buildCityComparison(agg.cityMap, cityAvgSqmMap)

    const avgPricePerSqm =
      agg.sqmPrices.length > 0 ? Math.round(agg.sqmPrices.reduce((a, b) => a + b, 0) / agg.sqmPrices.length) : 0

    const { scatterData, belowMarketDeals, detectedBargainsCount } = buildScatterAndBargainData(
      items,
      agg.segmentMap,
      cityAvgSqmMap,
      avgPricePerSqm,
    )

    const topPriceDrops = items
      .filter(hasPriceDrop)
      .sort((a, b) => Math.abs(b.priceDelta ?? 0) - Math.abs(a.priceDelta ?? 0))

    const kpi = calculateKpiStats({
      totalCount: items.length,
      prices: agg.prices,
      sqmPrices: agg.sqmPrices,
      lakasSqmPrices: agg.lakasSqmPrices,
      hazSqmPrices: agg.hazSqmPrices,
      telekSqmPrices: agg.telekSqmPrices,
      residentialSqmPrices: agg.residentialSqmPrices,
      plotSqmPrices: agg.plotSqmPrices,
      targetCategory,
      discountedCount: agg.discountedCount,
      newCount: agg.newCount,
      detectedBargainsCount,
    })

    const distribution = calculateDistributionStats(
      items,
      agg.prices,
      agg.sqmPrices,
      kpi.medianPriceMillion,
      kpi.medianPricePerSqm,
    )

    return {
      totalCount: items.length,
      kpi,
      distribution,
      cityComparison,
      scatterData,
      bargains: {
        topPriceDrops,
        belowMarketDeals: belowMarketDeals.slice(0, 10),
      },
    }
  }, [items, targetPropertyType])
}
