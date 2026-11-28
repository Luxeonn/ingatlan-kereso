import type { MouseEvent } from 'react'
import type { IngatlanItem } from '../../types/property'
import { useUiStore } from '../../store/useUiStore'

export function getCardBorderClass(
  isSelected: boolean,
  isStarred: boolean,
  isViewed?: boolean,
  isMultiSource?: boolean,
): string {
  if (isSelected) {
    return 'border-[#0071e3] bg-[#0071e3]/[0.06] ring-2 ring-[#0071e3]/35 shadow-lg'
  }
  if (isStarred) {
    return 'border-amber-500/50 ring-1 ring-amber-500/30 shadow-[0_0_15px_-3px_rgba(245,158,11,0.15)] hover:border-amber-500/70 hover:ring-amber-500/50'
  }
  if (isViewed) {
    if (isMultiSource) {
      return 'border-yellow-400/60 opacity-70 grayscale-[15%]'
    }
    return 'opacity-70 grayscale-[15%]'
  }
  if (isMultiSource) {
    return 'border-yellow-400 ring-1 ring-yellow-400/40 shadow-[0_0_12px_-3px_rgba(234,179,8,0.18)] hover:border-yellow-400 hover:ring-yellow-400/60'
  }
  return 'hover:border-[#0071e3]/35'
}

export function getPortalIdentifier(item: IngatlanItem): string {
  if (item.portal) return item.portal
  return item.source?.portalId ?? ''
}

export function getSourcesCount(item: IngatlanItem): number {
  return item.sourcesCount ?? (item.sourceListings?.length || 1)
}

export function isSelectionModifier(e?: MouseEvent<HTMLElement>): boolean {
  return Boolean(e && (e.ctrlKey || e.metaKey))
}

export function useCardSelection(url: string | undefined) {
  const { selectedCardUrls, toggleCardSelection } = useUiStore()
  const isSelected = Boolean(url && selectedCardUrls.includes(url))

  const toggleFromEvent = (e: MouseEvent<HTMLElement>) => {
    e.preventDefault()
    e.stopPropagation()
    if (url) toggleCardSelection(url)
  }

  return { isSelected, toggleFromEvent }
}
