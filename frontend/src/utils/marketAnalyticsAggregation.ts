import type { IngatlanItem } from '../types/property'
import { getEffectiveSqmPrice } from './formatters'
import { isListingNew } from './listingFilters'
import {
  type BargainItem,
  type CityStat,
  type ScatterPoint,
  type AnalyticsCategory,
  calculateMedian,
  hasPriceDrop,
  normalizeCategoryType,
} from './marketAnalyticsMath'

export interface ItemAggregation {
  prices: number[]
  sqmPrices: number[]
  lakasSqmPrices: number[]
  hazSqmPrices: number[]
  telekSqmPrices: number[]
  residentialSqmPrices: number[]
  plotSqmPrices: number[]
  cityMap: Map<string, IngatlanItem[]>
  segmentMap: Map<string, number[]>
  discountedCount: number
  newCount: number
}

function categorizeSqmPrice(
  category: AnalyticsCategory | null,
  sqPrice: number,
  agg: ItemAggregation,
): void {
  if (category === 'lakas') {
    agg.lakasSqmPrices.push(sqPrice)
    agg.residentialSqmPrices.push(sqPrice)
  } else if (category === 'haz') {
    agg.hazSqmPrices.push(sqPrice)
    agg.residentialSqmPrices.push(sqPrice)
  } else if (category === 'telek') {
    agg.telekSqmPrices.push(sqPrice)
    agg.plotSqmPrices.push(sqPrice)
  }
}

function getOrCreateArray<K, V>(map: Map<K, V[]>, key: K): V[] {
  let list = map.get(key)
  if (!list) {
    list = []
    map.set(key, list)
  }
  return list
}

function recordCityAndSegment(
  cityName: string,
  rawType: string,
  sqPrice: number,
  item: IngatlanItem,
  agg: ItemAggregation,
): void {
  const bucketKey = `${cityName.toLowerCase()}#${rawType}`
  getOrCreateArray(agg.segmentMap, bucketKey).push(sqPrice)
  getOrCreateArray(agg.cityMap, cityName).push(item)
}

export function aggregateItems(items: IngatlanItem[]): ItemAggregation {
  const agg: ItemAggregation = {
    prices: [],
    sqmPrices: [],
    lakasSqmPrices: [],
    hazSqmPrices: [],
    telekSqmPrices: [],
    residentialSqmPrices: [],
    plotSqmPrices: [],
    cityMap: new Map(),
    segmentMap: new Map(),
    discountedCount: 0,
    newCount: 0,
  }

  items.forEach((item) => {
    if (isListingNew(item)) agg.newCount++
    if (hasPriceDrop(item)) agg.discountedCount++

    const pMillion = item.priceInfo?.amountMillion
    if (pMillion !== null && pMillion !== undefined && pMillion > 0) {
      agg.prices.push(pMillion)
    }

    const sqPrice = getEffectiveSqmPrice(item)
    const cityName = item.city || item.location || 'Ismeretlen'
    const rawType = (item.propertyType || '').toUpperCase()

    if (sqPrice !== null && sqPrice !== undefined && sqPrice > 0) {
      agg.sqmPrices.push(sqPrice)
      const category = normalizeCategoryType(item.propertyType)
      categorizeSqmPrice(category, sqPrice, agg)
      recordCityAndSegment(cityName, rawType, sqPrice, item, agg)
    } else {
      getOrCreateArray(agg.cityMap, cityName).push(item)
    }
  })

  return agg
}

export function buildCityComparison(
  cityMap: Map<string, IngatlanItem[]>,
  cityAvgSqmMap: Map<string, number>,
): CityStat[] {
  const cityComparison: CityStat[] = []

  cityMap.forEach((cityItems, cityName) => {
    const cityPrices = cityItems
      .map((it) => it.priceInfo?.amountMillion)
      .filter((p): p is number => p !== null && p !== undefined && p > 0)
    const citySqmPrices = cityItems
      .map((it) => getEffectiveSqmPrice(it))
      .filter((p): p is number => p !== null && p !== undefined && p > 0)

    const cityAvgPrice = cityPrices.length > 0 ? cityPrices.reduce((a, b) => a + b, 0) / cityPrices.length : 0
    const cityMedianPrice = calculateMedian(cityPrices)
    const cityAvgSqm =
      citySqmPrices.length > 0 ? Math.round(citySqmPrices.reduce((a, b) => a + b, 0) / citySqmPrices.length) : 0
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
  return cityComparison
}

function checkBargainStatus(
  isExplicitBargain: boolean,
  sqmPrice: number,
  bucketMedian: number,
  cityAvg: number,
): boolean {
  if (isExplicitBargain) return true
  if (sqmPrice <= 0) return false
  if (bucketMedian > 0) return sqmPrice <= bucketMedian * 0.8
  if (cityAvg > 0) return sqmPrice <= cityAvg * 0.85
  return false
}

function resolvePointSegment(
  isBargain: boolean,
  isPriceDrop: boolean,
  refPrice: number,
  sqmPrice: number,
): 'bargain' | 'fair' | 'premium' {
  if (isBargain || isPriceDrop) return 'bargain'
  if (refPrice > 0 && sqmPrice > refPrice * 1.25) return 'premium'
  return 'fair'
}

function getValidAreaAndPrice(item: IngatlanItem): { area: number; price: number } | null {
  const area = item.detailsInfo?.areaSquareMeters
  const price = item.priceInfo?.amountMillion
  if (area && area > 0 && price && price > 0) {
    return { area, price }
  }
  return null
}

function getReferenceSqmPrice(
  city: string,
  rawType: string,
  segmentMap: Map<string, number[]>,
  cityAvgSqmMap: Map<string, number>,
  avgPricePerSqm: number,
): { bucketMedian: number; refPrice: number } {
  const bucketKey = `${city.toLowerCase()}#${rawType}`
  const bucketPrices = segmentMap.get(bucketKey) || []
  const bucketMedian = bucketPrices.length >= 5 ? calculateMedian(bucketPrices) : 0
  const cityAvg = cityAvgSqmMap.get(city) || avgPricePerSqm
  const refPrice = bucketMedian > 0 ? bucketMedian : cityAvg
  return { bucketMedian, refPrice }
}

interface ScatterEvaluationResult {
  point: ScatterPoint
  deal?: BargainItem
  isBargainOrPriceDrop: boolean
}

function evaluateScatterPoint(
  item: IngatlanItem,
  segmentMap: Map<string, number[]>,
  cityAvgSqmMap: Map<string, number>,
  avgPricePerSqm: number,
): ScatterEvaluationResult | null {
  const dimensions = getValidAreaAndPrice(item)
  if (!dimensions) {
    return null
  }

  const { area, price } = dimensions
  const sqmPrice = getEffectiveSqmPrice(item) || 0
  const city = item.city || item.location || 'Ismeretlen'
  const rawType = (item.propertyType || '').toUpperCase()

  const { bucketMedian, refPrice } = getReferenceSqmPrice(
    city,
    rawType,
    segmentMap,
    cityAvgSqmMap,
    avgPricePerSqm,
  )

  const isBargain = checkBargainStatus(Boolean(item.isBargain), sqmPrice, bucketMedian, refPrice)
  const isPriceDrop = hasPriceDrop(item)
  const segment = resolvePointSegment(isBargain, isPriceDrop, refPrice, sqmPrice)

  const point: ScatterPoint = {
    id: item.id || item.url,
    title: item.title || 'Ingatlan',
    city,
    portal: item.portal,
    url: item.url,
    area,
    price,
    sqmPrice,
    isNew: isListingNew(item),
    isDiscounted: isPriceDrop,
    priceDelta: item.priceDelta,
    priceChangePercent: item.priceChangePercent,
    isBargain,
    segment,
  }

  let deal: BargainItem | undefined
  if (isBargain && refPrice > 0) {
    const diff = refPrice - sqmPrice
    deal = {
      item,
      cityAvgSqmPrice: Math.round(refPrice),
      discountPercent: Math.round((diff / refPrice) * 100),
      sqmDiffFt: diff,
    }
  }

  return {
    point,
    deal,
    isBargainOrPriceDrop: isBargain || isPriceDrop,
  }
}

export interface ScatterAndBargainData {
  scatterData: ScatterPoint[]
  belowMarketDeals: BargainItem[]
  detectedBargainsCount: number
}

export function buildScatterAndBargainData(
  items: IngatlanItem[],
  segmentMap: Map<string, number[]>,
  cityAvgSqmMap: Map<string, number>,
  avgPricePerSqm: number,
): ScatterAndBargainData {
  const scatterData: ScatterPoint[] = []
  const belowMarketDeals: BargainItem[] = []
  let detectedBargainsCount = 0

  items.forEach((item) => {
    const evaluated = evaluateScatterPoint(item, segmentMap, cityAvgSqmMap, avgPricePerSqm)
    if (!evaluated) return
    scatterData.push(evaluated.point)
    if (evaluated.deal) {
      belowMarketDeals.push(evaluated.deal)
    }
    if (evaluated.isBargainOrPriceDrop) {
      detectedBargainsCount++
    }
  })

  belowMarketDeals.sort((a, b) => b.discountPercent - a.discountPercent)

  return {
    scatterData,
    belowMarketDeals,
    detectedBargainsCount,
  }
}
