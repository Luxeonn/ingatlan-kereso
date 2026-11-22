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
