import type { IngatlanItem } from '../types/property'
import { normalizePropertyType } from './formatters'

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

export interface HistogramOptions {
  items: IngatlanItem[]
  minVal: number
  maxVal: number
  step: number
  numBins: number
  getValue: (item: IngatlanItem) => number | undefined
  formatLabel: (binMin: number, binMax: number) => string
  roundAvg: (avg: number) => number
}

export function calculateMedian(arr: number[]): number {
  if (arr.length === 0) return 0
  const sorted = [...arr].sort((a, b) => a - b)
  const mid = Math.floor(sorted.length / 2)
  return sorted.length % 2 !== 0 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2
}

export function calculatePercentile(arr: number[], p: number): number {
  if (arr.length === 0) return 0
  const sorted = [...arr].sort((a, b) => a - b)
  const index = (sorted.length - 1) * p
  const lower = Math.floor(index)
  const upper = Math.ceil(index)
  const weight = index - lower
  return lower === upper ? sorted[lower] : sorted[lower] * (1 - weight) + sorted[upper] * weight
}

export function calculateStdDev(arr: number[], mean: number): number {
  if (arr.length <= 1) return 0
  const sumSquaredDiffs = arr.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0)
  return Math.sqrt(sumSquaredDiffs / (arr.length - 1))
}

export function hasPriceDrop(item: IngatlanItem): boolean {
  const isDeltaNeg = item.priceDelta !== null && item.priceDelta !== undefined && item.priceDelta < 0
  const isPctNeg =
    item.priceChangePercent !== null && item.priceChangePercent !== undefined && item.priceChangePercent < 0
  return isDeltaNeg || isPctNeg
}

export function buildHistogramBins(options: HistogramOptions): HistogramBin[] {
  const { items, minVal, maxVal, step, numBins, getValue, formatLabel, roundAvg } = options
  const bins: HistogramBin[] = []
  const start = Math.floor(minVal / step) * step
  const totalCount = items.length

  for (let i = 0; i < numBins; i++) {
    const binMin = start + i * step
    const binMax = binMin + step
    const inBin = items.filter((it) => {
      const val = getValue(it)
      if (val === null || val === undefined) return false
      return i === numBins - 1 ? val >= binMin && val <= binMax : val >= binMin && val < binMax
    })

    if (inBin.length > 0 || (binMin <= maxVal && binMax >= minVal)) {
      const sum = inBin.reduce((acc, it) => acc + (getValue(it) || 0), 0)
      bins.push({
        rangeLabel: formatLabel(binMin, binMax),
        min: binMin,
        max: binMax,
        count: inBin.length,
        newCount: inBin.filter((it) => it.isNew).length,
        avgVal: inBin.length > 0 ? roundAvg(sum / inBin.length) : 0,
        percentage: totalCount > 0 ? Math.round((inBin.length / totalCount) * 100) : 0,
      })
    }
  }
  return bins
}

export function normalizeCategoryType(type?: string | null): 'lakas' | 'haz' | 'telek' | null {
  if (!type) return null
  const canonical = normalizePropertyType(type)
  if (canonical === 'LAKAS') return 'lakas'
  if (canonical === 'HAZ') return 'haz'
  if (canonical === 'TELEK') return 'telek'
  return null
}

export function createEmptyMarketAnalyticsData(
  targetCategory: 'lakas' | 'haz' | 'telek' | null,
): MarketAnalyticsData {
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

export function calculateDistributionStats(
  items: IngatlanItem[],
  prices: number[],
  sqmPrices: number[],
  medianPriceMillion: number,
  medianPricePerSqm: number,
): MarketAnalyticsData['distribution'] {
  const priceP25 = calculatePercentile(prices, 0.25)
  const priceP50 = medianPriceMillion
  const priceP75 = calculatePercentile(prices, 0.75)

  const sqmPriceP25 = calculatePercentile(sqmPrices, 0.25)
  const sqmPriceP50 = medianPricePerSqm
  const sqmPriceP75 = calculatePercentile(sqmPrices, 0.75)

  const minPriceMillion = prices.length > 0 ? Math.min(...prices) : 0
  const maxPriceMillion = prices.length > 0 ? Math.max(...prices) : 0
  const priceRange = maxPriceMillion - minPriceMillion
  const priceNumBins = Math.min(8, Math.max(4, Math.ceil(priceRange / 5)))
  const priceStep = priceRange > 0 ? Math.ceil((priceRange / priceNumBins) * 2) / 2 || 5 : 5
  const priceBins =
    prices.length > 0
      ? buildHistogramBins({
          items,
          minVal: minPriceMillion,
          maxVal: maxPriceMillion,
          step: priceStep,
          numBins: priceNumBins,
          getValue: (it) => it.priceInfo?.amountMillion,
          formatLabel: (binMin, binMax) => `${binMin.toFixed(0)} - ${binMax.toFixed(0)} M Ft`,
          roundAvg: (avg) => Math.round(avg * 10) / 10,
        })
      : []

  const minPricePerSqm = sqmPrices.length > 0 ? Math.min(...sqmPrices) : 0
  const maxPricePerSqm = sqmPrices.length > 0 ? Math.max(...sqmPrices) : 0
  const sqmRange = maxPricePerSqm - minPricePerSqm
  const sqmNumBins = Math.min(8, Math.max(4, Math.ceil(sqmRange / 100_000)))
  const sqmStep = sqmRange > 0 ? Math.ceil(sqmRange / sqmNumBins / 50_000) * 50_000 || 100_000 : 100_000
  const sqmPriceBins =
    sqmPrices.length > 0
      ? buildHistogramBins({
          items,
          minVal: minPricePerSqm,
          maxVal: maxPricePerSqm,
          step: sqmStep,
          numBins: sqmNumBins,
          getValue: (it) => it.detailsInfo?.pricePerSquareMeter,
          formatLabel: (binMin, binMax) => `${Math.round(binMin / 1000)}k - ${Math.round(binMax / 1000)}k`,
          roundAvg: (avg) => Math.round(avg),
        })
      : []

  return {
    priceBins,
    sqmPriceBins,
    priceP25: Math.round(priceP25 * 10) / 10,
    priceP50: Math.round(priceP50 * 10) / 10,
    priceP75: Math.round(priceP75 * 10) / 10,
    sqmPriceP25: Math.round(sqmPriceP25),
    sqmPriceP50: Math.round(sqmPriceP50),
    sqmPriceP75: Math.round(sqmPriceP75),
  }
}
