import { apiClient } from './client'
import { LogsResponse } from '../types/api'

export async function getLogs(limit = 100, level?: string): Promise<LogsResponse> {
  const params = new URLSearchParams()
  params.set('limit', String(limit))
  if (level && level !== 'ALL') {
    params.set('level', level)
  }
  return apiClient<LogsResponse>(`/api/logs?${params.toString()}`)
}

export async function clearLogs(): Promise<{ success: boolean; message: string }> {
  return apiClient<{ success: boolean; message: string }>('/api/logs/clear', {
    method: 'POST',
  })
}
