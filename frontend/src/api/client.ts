export class ApiError extends Error {
  constructor(public status: number, message: string, public data?: unknown) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function apiClient<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  
  const headers = new Headers(options.headers || {})
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`
    let errorData = null
    try {
      errorData = await response.json()
      if (errorData && typeof errorData === 'object' && 'error' in errorData) {
        errorMsg = String(errorData.error)
      }
    } catch {
      // not JSON
    }
    throw new ApiError(response.status, errorMsg, errorData)
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return {} as T
  }

  const contentType = response.headers.get('content-type')
  if (contentType && contentType.includes('application/json')) {
    return (await response.json()) as T
  }

  return (await response.text()) as unknown as T
}
