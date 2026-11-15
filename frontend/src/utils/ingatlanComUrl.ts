import type { CanonicalPropertyType } from '../types/property'

const TYPE_SLUG_MAP: Record<CanonicalPropertyType, string> = {
  HAZ: 'haz',
  LAKAS: 'lakas',
  TELEK: 'telek',
  GARAZS: 'garazs',
  IPARI_KERESKEDELMI: 'ingatlan',
  EGYEB_ISMERETLEN: 'ingatlan',
}

function toAsciiSlug(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

function buildRangeSlug(minVal: number | undefined, maxVal: number | undefined, unit: string): string {
  const hasMin = typeof minVal === 'number' && minVal > 0
  const hasMax = typeof maxVal === 'number' && maxVal > 0 && maxVal < 9999
  if (hasMin && hasMax) return `${Math.round(minVal)}-${Math.round(maxVal)}-${unit}`
  if (hasMin) return `${Math.round(minVal)}-${unit}-tol`
  if (hasMax) return `${Math.round(maxVal)}-${unit}-ig`
  return ''
}

export function buildIngatlanComSearchUrl(
  params: {
    city?: string
    propertyType?: CanonicalPropertyType
    minPriceMillion?: number
    maxPriceMillion?: number
    minArea?: number
    maxArea?: number
  },
  page = 1,
): string {
  const citySlug = params.city ? toAsciiSlug(params.city) || 'debrecen' : 'debrecen'
  const typeSlug = params.propertyType ? TYPE_SLUG_MAP[params.propertyType] : 'lakas'
  const segments = ['elado', typeSlug]

  const areaSlug = buildRangeSlug(params.minArea, params.maxArea, 'm2')
  if (areaSlug) segments.push(areaSlug)

  const priceSlug = buildRangeSlug(params.minPriceMillion, params.maxPriceMillion, 'mFt')
  if (priceSlug) segments.push(priceSlug)

  segments.push(citySlug)
  const baseUrl = `https://ingatlan.com/lista/${segments.join('+')}`
  return page > 1 ? `${baseUrl}?page=${page}` : baseUrl
}
