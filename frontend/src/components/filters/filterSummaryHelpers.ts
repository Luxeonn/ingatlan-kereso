import type { CanonicalPropertyType } from '../../types/property'

export type FilterPropertyType = CanonicalPropertyType | 'lakas' | 'haz' | 'telek'

const PROPERTY_TYPE_LABELS: Record<FilterPropertyType, string> = {
  HAZ: 'Családi ház',
  LAKAS: 'Lakás',
  TELEK: 'Telek',
  GARAZS: 'Garázs',
  IPARI_KERESKEDELMI: 'Ipari / Kereskedelmi',
  EGYEB_ISMERETLEN: 'Egyéb',
  haz: 'Családi ház',
  lakas: 'Lakás',
  telek: 'Telek',
}

export function resolveCitiesSummary(cities?: string[], defaultCity?: string): string {
  if (cities && cities.length > 0) return cities.join(', ')
  return defaultCity || 'Hajdúszoboszló'
}

export function resolveTypeLabel(propertyType?: string): string {
  const key = (propertyType || 'haz').toLowerCase()
  if (Object.prototype.hasOwnProperty.call(PROPERTY_TYPE_LABELS, key)) {
    return PROPERTY_TYPE_LABELS[key as FilterPropertyType]
  }
  return propertyType || 'Családi ház'
}
