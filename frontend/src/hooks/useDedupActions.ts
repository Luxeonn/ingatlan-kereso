import { useState, type Dispatch, type SetStateAction } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  splitCluster as apiSplitCluster,
  mergeListings as apiMergeListings,
  deleteDedupOverride as apiDeleteDedupOverride,
} from '../api/dedupApi'
import type { DashboardResponseDto } from '../types/api'
import type { IngatlanItem } from '../types/property'
import { useUiStore } from '../store/useUiStore'

function collectItemUrls(item: IngatlanItem): string[] {
  const urls: string[] = []
  if (item.url) urls.push(item.url)
  for (const src of item.sourceListings ?? []) {
    if (src.url) urls.push(src.url)
  }
  return urls
}

export function replaceAffectedItems(
  currentItems: IngatlanItem[],
  affectedUrls: Set<string>,
  replacementItems: IngatlanItem[],
): IngatlanItem[] {
  let insertAt = -1
  const remaining: IngatlanItem[] = []
  for (const item of currentItems) {
    const overlaps = collectItemUrls(item).some((u) => affectedUrls.has(u))
    if (overlaps) {
      if (insertAt === -1) insertAt = remaining.length
    } else {
      remaining.push(item)
    }
  }
  if (insertAt === -1) {
    return [...remaining, ...replacementItems]
  }
  return [
    ...remaining.slice(0, insertAt),
    ...replacementItems,
    ...remaining.slice(insertAt),
  ]
}

function resolveErrorMessage(err: unknown): string {
  if (err instanceof Error && err.message) {
    return err.message
  }
  return 'Hiba történt a művelet során'
}

export function useDedupActions(
  setData: Dispatch<SetStateAction<DashboardResponseDto | null>>,
  setToastMessage: Dispatch<SetStateAction<string | null>>,
  onAfterMerge?: () => void,
) {
  const queryClient = useQueryClient()
  const { clearCardSelection } = useUiStore()
  const [isMerging, setIsMerging] = useState(false)

  const splitSource = async (sourceUrl: string, clusterUrls: string[]): Promise<boolean> => {
    try {
      const res = await apiSplitCluster(sourceUrl, clusterUrls)
      const affected = new Set([sourceUrl, ...clusterUrls])
      setData((prev) =>
        prev ? { ...prev, items: replaceAffectedItems(prev.items, affected, res.items) } : prev,
      )
      setToastMessage(res.message)
      queryClient.invalidateQueries({ queryKey: ['history'] })
      queryClient.invalidateQueries({ queryKey: ['properties'] })
      return true
    } catch (err) {
      setToastMessage(resolveErrorMessage(err))
      return false
    }
  }

  const mergeSelected = async (urls: string[]): Promise<boolean> => {
    setIsMerging(true)
    try {
      const res = await apiMergeListings(urls)
      const affected = new Set(urls)
      setData((prev) =>
        prev ? { ...prev, items: replaceAffectedItems(prev.items, affected, res.items) } : prev,
      )
      clearCardSelection()
      setToastMessage(res.message)
      queryClient.invalidateQueries({ queryKey: ['history'] })
      queryClient.invalidateQueries({ queryKey: ['properties'] })
      queryClient.invalidateQueries({ queryKey: ['portals'] })
      queryClient.invalidateQueries({ queryKey: ['cities'] })
      onAfterMerge?.()
      return true
    } catch (err) {
      setToastMessage(resolveErrorMessage(err))
      return false
    } finally {
      setIsMerging(false)
    }
  }

  const undoDedupOverride = async (overrideId: number): Promise<boolean> => {
    try {
      const res = await apiDeleteDedupOverride(overrideId)
      const affected = new Set<string>()
      for (const item of res.items) {
        for (const u of collectItemUrls(item)) affected.add(u)
      }
      setData((prev) =>
        prev && affected.size > 0
          ? { ...prev, items: replaceAffectedItems(prev.items, affected, res.items) }
          : prev,
      )
      setToastMessage(res.message)
      queryClient.invalidateQueries({ queryKey: ['history'] })
      queryClient.invalidateQueries({ queryKey: ['properties'] })
      return true
    } catch (err) {
      setToastMessage(resolveErrorMessage(err))
      return false
    }
  }

  return { splitSource, mergeSelected, undoDedupOverride, isMerging }
}
