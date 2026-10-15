export function formatPriceMillion(million?: number | null): string {
  if (million == null || isNaN(million)) return 'N/A'
  return `${Number(million.toFixed(1))} M Ft`
}

export function formatPriceFt(amountFt?: number | null): string {
  if (amountFt == null || isNaN(amountFt)) return 'Ár a hirdetésben'
  return new Intl.NumberFormat('hu-HU').format(amountFt) + ' Ft'
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
  const key = portalId.toLowerCase().replace(/[^a-z]/g, '')
  for (const [pKey, style] of Object.entries(PORTAL_COLORS)) {
    if (key.includes(pKey)) return style
  }
  return { bg: 'bg-blue-500/10 text-blue-400 border-blue-500/30', text: 'text-blue-400', border: 'border-blue-500/40' }
}
