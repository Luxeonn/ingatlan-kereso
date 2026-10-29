import {
  Building2,
  Home,
  Trees,
  Warehouse,
  Factory,
  HelpCircle,
  ShieldCheck,
  ShieldAlert,
  ShieldX,
  type LucideIcon,
} from 'lucide-react'
import type { CanonicalPropertyType, IngatlanItem } from '../types/property'

export interface PropertyTypeConfig {
  key: CanonicalPropertyType
  label: string
  icon: LucideIcon
  colorClasses: {
    badge: string
    icon: string
  }
}

export const PROPERTY_TYPE_CONFIGS: Record<CanonicalPropertyType, PropertyTypeConfig> = {
  LAKAS: {
    key: 'LAKAS',
    label: 'Lakás',
    icon: Building2,
    colorClasses: {
      badge: 'bg-indigo-500/15 text-indigo-400 border-indigo-500/30',
      icon: 'text-indigo-400',
    },
  },
  HAZ: {
    key: 'HAZ',
    label: 'Ház',
    icon: Home,
    colorClasses: {
      badge: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
      icon: 'text-amber-400',
    },
  },
  TELEK: {
    key: 'TELEK',
    label: 'Telek',
    icon: Trees,
    colorClasses: {
      badge: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
      icon: 'text-emerald-400',
    },
  },
  GARAZS: {
    key: 'GARAZS',
    label: 'Garázs',
    icon: Warehouse,
    colorClasses: {
      badge: 'bg-cyan-500/15 text-cyan-400 border-cyan-500/30',
      icon: 'text-cyan-400',
    },
  },
  IPARI_KERESKEDELMI: {
    key: 'IPARI_KERESKEDELMI',
    label: 'Ipari / Kereskedelmi',
    icon: Factory,
    colorClasses: {
      badge: 'bg-purple-500/15 text-purple-400 border-purple-500/30',
      icon: 'text-purple-400',
    },
  },
  EGYEB_ISMERETLEN: {
    key: 'EGYEB_ISMERETLEN',
    label: 'Egyéb',
    icon: HelpCircle,
    colorClasses: {
      badge: 'bg-slate-500/15 text-slate-400 border-slate-500/30',
      icon: 'text-slate-400',
    },
  },
}

const PROPERTY_TYPE_MATCHERS: Array<{ type: CanonicalPropertyType; keywords: string[] }> = [
  { type: 'LAKAS', keywords: ['lakas', 'lakás', 'apartment', 'flat', 'tarsashaz', 'társasház', 'panel'] },
  { type: 'HAZ', keywords: ['haz', 'ház', 'house', 'csaladi', 'családi', 'ikerhaz', 'sorhaz', 'villa'] },
  { type: 'TELEK', keywords: ['telek', 'land', 'plot', 'epitesi', 'építési', 'zartkert', 'szanto'] },
  { type: 'GARAZS', keywords: ['garazs', 'garázs', 'garage', 'parkolo', 'parkoló', 'beallo'] },
  {
    type: 'IPARI_KERESKEDELMI',
    keywords: ['ipari', 'kereskedelmi', 'commercial', 'industrial', 'iroda', 'raktar', 'raktár', 'telephely'],
  },
]

export function normalizePropertyType(rawType?: string | null): CanonicalPropertyType {
  if (!rawType || typeof rawType !== 'string') return 'EGYEB_ISMERETLEN'

  const normalized = rawType.trim().toUpperCase()
  if (normalized in PROPERTY_TYPE_CONFIGS) {
    return normalized as CanonicalPropertyType
  }

  const lower = rawType.trim().toLowerCase()
  for (const { type, keywords } of PROPERTY_TYPE_MATCHERS) {
    if (keywords.some((kw) => lower === kw || lower.includes(kw))) {
      return type
    }
  }

  return 'EGYEB_ISMERETLEN'
}

export function getPropertyTypeConfig(rawType?: string | null): PropertyTypeConfig {
  return PROPERTY_TYPE_CONFIGS[normalizePropertyType(rawType)]
}

export type QualityTierKey = 'EXCELLENT' | 'ACCEPTABLE' | 'SUSPICIOUS'

export interface QualityConfig {
  key: QualityTierKey
  ratingLabel: string
  icon: LucideIcon
  buttonClasses: string
  badgeClass: string
  textClass: string
  pillClass: string
  popoverBorderClass: string
  headerTextClass: string
  barClass: string
}

export function clampQualityScore(score?: number | null): number | null {
  if (score == null || isNaN(score)) return null
  return Math.max(0, Math.min(100, Math.round(score)))
}

export function getQualityConfig(score: number): QualityConfig {
  if (score >= 85) {
    return {
      key: 'EXCELLENT',
      ratingLabel: 'Kiváló adatminőség',
      icon: ShieldCheck,
      buttonClasses:
        'bg-emerald-500/15 border-emerald-500/30 hover:bg-emerald-500/25 text-emerald-400 focus:ring-emerald-400/50',
      badgeClass: 'bg-emerald-500/15 border-emerald-500/30 text-emerald-400',
      textClass: 'text-emerald-400',
      pillClass: 'bg-emerald-500/25 text-emerald-300',
      popoverBorderClass: 'border-emerald-500/30',
      headerTextClass: 'text-emerald-400',
      barClass: 'bg-emerald-400',
    }
  }
  if (score >= 70) {
    return {
      key: 'ACCEPTABLE',
      ratingLabel: 'Elfogadható adatminőség',
      icon: ShieldAlert,
      buttonClasses:
        'bg-amber-500/15 border-amber-500/30 hover:bg-amber-500/25 text-amber-400 focus:ring-amber-400/50',
      badgeClass: 'bg-amber-500/15 border-amber-500/30 text-amber-400',
      textClass: 'text-amber-400',
      pillClass: 'bg-amber-500/25 text-amber-300',
      popoverBorderClass: 'border-amber-500/30',
      headerTextClass: 'text-amber-400',
      barClass: 'bg-amber-400',
    }
  }
  return {
    key: 'SUSPICIOUS',
    ratingLabel: 'Gyanús / hiányos adat',
    icon: ShieldX,
    buttonClasses:
      'bg-rose-500/15 border-rose-500/30 hover:bg-rose-500/25 text-rose-400 focus:ring-rose-400/50',
    badgeClass: 'bg-rose-500/15 border-rose-500/30 text-rose-400',
    textClass: 'text-rose-400',
    pillClass: 'bg-rose-500/25 text-rose-300',
    popoverBorderClass: 'border-rose-500/30',
    headerTextClass: 'text-rose-400',
    barClass: 'bg-rose-500',
  }
}

export function getEffectivePrice(item: IngatlanItem): { priceFt?: number; priceMillion?: number } {
  const activeSources = (item.sourceListings || []).filter(
    (s) => !s.status || s.status.toUpperCase() === 'ACTIVE',
  )
  const lowestActivePriceFt =
    activeSources.length > 0
      ? activeSources.reduce((min, s) => {
          if (s.priceHuf != null && s.priceHuf > 0) {
            return min == null || s.priceHuf < min ? s.priceHuf : min
          }
          return min
        }, null as number | null)
      : null

  const priceFt = lowestActivePriceFt ?? item.priceInfo?.amountHuf
  const priceMillion = priceFt != null ? priceFt / 1_000_000 : item.priceInfo?.amountMillion
  return { priceFt, priceMillion }
}

export function getEffectiveSqmPrice(item: IngatlanItem): number | null {
  if (item.detailsInfo?.pricePerSquareMeter != null && item.detailsInfo.pricePerSquareMeter > 0) {
    return item.detailsInfo.pricePerSquareMeter
  }
  const area = item.detailsInfo?.areaSquareMeters
  if (area && area > 0) {
    if (item.priceInfo?.amountHuf && item.priceInfo.amountHuf > 0) {
      return Math.round(item.priceInfo.amountHuf / area)
    }
    if (item.priceInfo?.amountMillion && item.priceInfo.amountMillion > 0) {
      return Math.round((item.priceInfo.amountMillion * 1_000_000) / area)
    }
  }
  return null
}

export function openExternalUrl(url?: string | null): void {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

export function formatPriceMillion(million?: number | null): string {
  if (million == null || isNaN(million)) return 'N/A'
  return `${Number(million.toFixed(1))} M Ft`
}

export function formatPriceFt(amountHuf?: number | null): string {
  if (amountHuf == null || isNaN(amountHuf)) return 'Ár a hirdetésben'
  return new Intl.NumberFormat('hu-HU').format(amountHuf) + ' Ft'
}

export function formatPricePerSqm(ftPerM2?: number | null): string {
  if (ftPerM2 == null || isNaN(ftPerM2) || ftPerM2 <= 0) return 'N/A'
  return new Intl.NumberFormat('hu-HU').format(Math.round(ftPerM2)) + ' Ft/m²'
}

export function formatArea(sqm?: number | null): string {
  if (sqm == null || isNaN(sqm) || sqm <= 0) return 'N/A'
  return `${Math.round(sqm)} m²`
}

export function formatRooms(rooms?: number | null): string {
  if (rooms == null || isNaN(rooms) || rooms <= 0) return ''
  return `${rooms} szoba`
}

export function formatDate(dateStr?: string | null): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('hu-HU', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

export const PORTAL_CHIPS = [
  { id: 'all', label: 'Összes' },
  { id: 'dh', label: 'Duna House' },
  { id: 'oc', label: 'Otthon Centrum' },
  { id: 'jofogas', label: 'Jófogás' },
  { id: 'ingatlancom', label: 'Ingatlan.com' },
  { id: 'ingatlanbazar', label: 'Ingatlanbazár' },
  { id: 'koltozzbe', label: 'Költözzbe' },
] as const

const PORTAL_DOMAINS: Array<{ domain: string; portalId: string; portalName: string }> = [
  { domain: 'dh.hu', portalId: 'dh', portalName: 'Duna House' },
  { domain: 'oc.hu', portalId: 'oc', portalName: 'Otthon Centrum' },
  { domain: 'jofogas.hu', portalId: 'jofogas', portalName: 'Jófogás' },
  { domain: 'ingatlanbazar.hu', portalId: 'ingatlanbazar', portalName: 'Ingatlanbazár' },
  { domain: 'koltozzbe.hu', portalId: 'koltozzbe', portalName: 'Költözzbe' },
  { domain: 'ingatlan.com', portalId: 'ingatlancom', portalName: 'Ingatlan.com' },
]

export const PORTAL_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  dh: { bg: 'bg-orange-500/10 text-orange-400 border-orange-500/30', text: 'text-orange-400', border: 'border-orange-500/40' },
  oc: { bg: 'bg-red-500/10 text-red-400 border-red-500/30', text: 'text-red-400', border: 'border-red-500/40' },
  jofogas: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30', text: 'text-emerald-400', border: 'border-emerald-500/40' },
  ingatlanbazar: { bg: 'bg-purple-500/10 text-purple-400 border-purple-500/30', text: 'text-purple-400', border: 'border-purple-500/40' },
  koltozzbe: { bg: 'bg-teal-500/10 text-teal-400 border-teal-500/30', text: 'text-teal-400', border: 'border-teal-500/40' },
  ingatlancom: { bg: 'bg-sky-500/10 text-sky-400 border-sky-500/30', text: 'text-sky-400', border: 'border-sky-500/40' },
}

export function normalizePortalKey(portalOrId?: string | null): string {
  const cleaned = (portalOrId || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '')
  if (cleaned.includes('dh') || cleaned.includes('duna')) return 'dh'
  if (cleaned.includes('oc') || cleaned.includes('otthon')) return 'oc'
  if (cleaned.includes('jofogas')) return 'jofogas'
  if (cleaned.includes('ingatlanbazar') || cleaned.includes('bazar')) return 'ingatlanbazar'
  if (cleaned.includes('koltozzbe')) return 'koltozzbe'
  if (cleaned.includes('ingatlancom')) return 'ingatlancom'
  return cleaned
}

export function getPortalStyle(portalId: string) {
  const key = normalizePortalKey(portalId)
  if (key in PORTAL_COLORS) {
    return PORTAL_COLORS[key]
  }
  return { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/30', text: 'text-blue-400', border: 'border-blue-500/40' }
}

export function detectPortalFromUrl(url: string): { portalId: string; portalName: string } {
  try {
    const hostname = new URL(url).hostname.toLowerCase()
    const match = PORTAL_DOMAINS.find(
      ({ domain }) => hostname === domain || hostname.endsWith(`.${domain}`),
    )
    if (match) {
      return { portalId: match.portalId, portalName: match.portalName }
    }
  } catch {
    // Ignore URL parse error and fall back to default
  }
  return { portalId: 'egyeb', portalName: 'Hirdetés' }
}
