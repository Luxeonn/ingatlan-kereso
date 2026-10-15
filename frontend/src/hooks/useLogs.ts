import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getLogs, clearLogs } from '../api/logApi'

export function useLogs(limit = 100, level?: string, enabled = false) {
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['logs', limit, level],
    queryFn: () => getLogs(limit, level),
    enabled,
    refetchInterval: enabled ? 3000 : false, // 3 másodpercenként frissül ha nyitva a modal
  })

  const clearMutation = useMutation({
    mutationFn: clearLogs,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['logs'] })
    },
  })

  return {
    ...query,
    clearLogs: clearMutation.mutate,
    isClearing: clearMutation.isPending,
  }
}
