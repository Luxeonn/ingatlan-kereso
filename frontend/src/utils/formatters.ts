import {
  Building2,
  Home,
  Trees,
  Warehouse,
  Factory,
  HelpCircle,
  type LucideIcon,
} from 'lucide-react'
import type { CanonicalPropertyType } from '../types/property'

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

export function normalizePropertyType(rawType?: string | null): CanonicalPropertyType {
  if (!rawType || typeof rawType !== 'string') return 'EGYEB_ISMERETLEN'

  const normalized = rawType.trim().toUpperCase()
  if (normalized in PROPERTY_TYPE_CONFIGS) {
    return normalized as CanonicalPropertyType
  }

  const lower = rawType.trim().toLowerCase()
  if (
    lower === 'lakas' ||
    lower === 'lakás' ||
    lower === 'apartment' ||
    lower === 'flat' ||
    lower.includes('lakas') ||
    lower.includes('lakás') ||
    lower.includes('tarsashaz') ||
    lower.includes('társasház') ||
    lower.includes('panel')
  ) {
    return 'LAKAS'
  }

  if (
    lower === 'haz' ||
    lower === 'ház' ||
    lower === 'house' ||
    lower.includes('haz') ||
    lower.includes('ház') ||
    lower.includes('csaladi') ||
    lower.includes('családi') ||
    lower.includes('ikerhaz') ||
    lower.includes('sorhaz') ||
    lower.includes('villa')
  ) {
    return 'HAZ'
  }

  if (
    lower === 'telek' ||
    lower === 'land' ||
    lower === 'plot' ||
    lower.includes('telek') ||
    lower.includes('epitesi') ||
    lower.includes('építési') ||
    lower.includes('zartkert') ||
    lower.includes('szanto')
  ) {
    return 'TELEK'
  }

  if (
    lower === 'garazs' ||
    lower === 'garázs' ||
    lower === 'garage' ||
    lower.includes('garazs') ||
    lower.includes('garázs') ||
    lower.includes('parkolo') ||
    lower.includes('parkoló') ||
    lower.includes('beallo')
  ) {
    return 'GARAZS'
  }

  if (
    lower === 'ipari' ||
    lower === 'kereskedelmi' ||
    lower === 'commercial' ||
    lower === 'industrial' ||
    lower.includes('ipari') ||
    lower.includes('kereskedelmi') ||
    lower.includes('iroda') ||
    lower.includes('raktar') ||
    lower.includes('raktár') ||
    lower.includes('telephely')
  ) {
    return 'IPARI_KERESKEDELMI'
  }

  return 'EGYEB_ISMERETLEN'
}

export function getPropertyTypeConfig(rawType?: string | null): PropertyTypeConfig {
  const key = normalizePropertyType(rawType)
  return PROPERTY_TYPE_CONFIGS[key]
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

export const PORTAL_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  dh: { bg: 'bg-orange-500/10 text-orange-400 border-orange-500/30', text: 'text-orange-400', border: 'border-orange-500/40' },
  oc: { bg: 'bg-red-500/10 text-red-400 border-red-500/30', text: 'text-red-400', border: 'border-red-500/40' },
  jofogas: { bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30', text: 'text-emerald-400', border: 'border-emerald-500/40' },
  ingatlanbazar: { bg: 'bg-purple-500/10 text-purple-400 border-purple-500/30', text: 'text-purple-400', border: 'border-purple-500/40' },
  koltozzbe: { bg: 'bg-teal-500/10 text-teal-400 border-teal-500/30', text: 'text-teal-400', border: 'border-teal-500/40' },
  ingatlancom: { bg: 'bg-sky-500/10 text-sky-400 border-sky-500/30', text: 'text-sky-400', border: 'border-sky-500/40' },
}

export function getPortalStyle(portalId: string) {
  const key = (portalId || '').toLowerCase().replace(/[^a-z]/g, '')
  for (const [pKey, style] of Object.entries(PORTAL_COLORS)) {
    if (key.includes(pKey)) return style
  }
  return { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/30', text: 'text-blue-400', border: 'border-blue-500/40' }
}

export function detectPortalFromUrl(url: string): { portalId: string; portalName: string } {
  try {
    const parsed = new URL(url)
    const hostname = parsed.hostname.toLowerCase()
    if (hostname === 'dh.hu' || hostname.endsWith('.dh.hu')) {
      return { portalId: 'dh', portalName: 'Duna House' }
    }
    if (hostname === 'oc.hu' || hostname.endsWith('.oc.hu')) {
      return { portalId: 'oc', portalName: 'Otthon Centrum' }
    }
    if (hostname === 'jofogas.hu' || hostname.endsWith('.jofogas.hu')) {
      return { portalId: 'jofogas', portalName: 'Jófogás' }
    }
    if (hostname === 'ingatlanbazar.hu' || hostname.endsWith('.ingatlanbazar.hu')) {
      return { portalId: 'ingatlanbazar', portalName: 'Ingatlanbazár' }
    }
    if (hostname === 'koltozzbe.hu' || hostname.endsWith('.koltozzbe.hu')) {
      return { portalId: 'koltozzbe', portalName: 'Költözzbe' }
    }
    if (hostname === 'ingatlan.com' || hostname.endsWith('.ingatlan.com')) {
      return { portalId: 'ingatlancom', portalName: 'Ingatlan.com' }
    }
  } catch {
    // Ignore URL parse error and fall back to default
  }
  return { portalId: 'egyeb', portalName: 'Hirdetés' }
}
