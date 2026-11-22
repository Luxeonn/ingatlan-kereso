import type { CrossPortalSpreadInfo, IngatlanItem } from '../types/property'
import { detectPortalFromUrl } from './formatters'

const MAX_ENDPOINT_SEGMENT_LENGTH = 20
const TAIL_KEEP_LENGTH = 16

function extractTailSegment(url: string): string {
  try {
    const parsed = new URL(url)
    const segments = parsed.pathname.split('/').filter(Boolean)
    if (segments.length > 0) {
      const last = segments[segments.length - 1]!.replace(/\.html?$/i, '')
      if (last.length > 0) {
        return last
      }
    }
    if (parsed.search.length > 1) {
      return parsed.search.slice(1)
    }
    return parsed.hostname
  } catch {
    const fallback = url.split('/').filter(Boolean).pop() ?? url
    return fallback.replace(/\.html?$/i, '')
  }
}

export function formatOverrideEndpointLabel(url: string): string {
  if (!url) return ''
  const { portalName } = detectPortalFromUrl(url)
  const rawSegment = extractTailSegment(url)
  if (!rawSegment) return portalName
  const shortSegment =
    rawSegment.length > MAX_ENDPOINT_SEGMENT_LENGTH
      ? `…${rawSegment.slice(-TAIL_KEEP_LENGTH)}`
      : rawSegment
  return `${portalName} #${shortSegment}`
}

export function getCrossPortalSpread(item: IngatlanItem): CrossPortalSpreadInfo | null {
  const activeWithPrice = (item.sourceListings ?? []).filter(
    (s) =>
      (!s.status || s.status.toUpperCase() === 'ACTIVE') &&
      typeof s.priceHuf === 'number' &&
      !Number.isNaN(s.priceHuf) &&
      s.priceHuf > 0,
  )
  if (activeWithPrice.length < 2) return null

  let minActivePriceHuf = Infinity
  let maxActivePriceHuf = -Infinity
  let cheapestPortal = ''

  for (const src of activeWithPrice) {
    const price = src.priceHuf!
    if (price < minActivePriceHuf) {
      minActivePriceHuf = price
      cheapestPortal = src.portal || (src.url ? detectPortalFromUrl(src.url).portalName : 'Forrás')
    }
    if (price > maxActivePriceHuf) {
      maxActivePriceHuf = price
    }
  }

  const spreadAmountHuf = maxActivePriceHuf - minActivePriceHuf
  if (spreadAmountHuf <= 0 || !Number.isFinite(spreadAmountHuf)) {
    return null
  }

  const spreadMillion = Number((spreadAmountHuf / 1_000_000).toFixed(1))
  const spreadPercent = Math.round((spreadAmountHuf / maxActivePriceHuf) * 100)

  return {
    activeSourcesCount: activeWithPrice.length,
    minActivePriceHuf,
    maxActivePriceHuf,
    spreadAmountHuf,
    spreadMillion,
    spreadPercent,
    cheapestPortal,
  }
}
