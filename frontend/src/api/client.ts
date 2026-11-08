import createClient from 'openapi-fetch'
import type { paths } from '../types/generated-api'

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public data?: unknown,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const defaultBaseUrl =
  typeof window !== 'undefined' && window.location?.origin
    ? window.location.origin
    : 'http://localhost:8080'

/**
 * Single source of truth typed OpenAPI client.
 * Endpoint routes, HTTP methods, query params, request bodies, and responses
 * are automatically derived from openapi/api-spec.yaml via generated-api.ts paths.
 */
export const fetchClient = createClient<paths>({
  baseUrl: defaultBaseUrl,
  fetch: (request: Request) => globalThis.fetch(request),
})

export type FetchResponse<T> = Promise<{
  data?: T
  error?: unknown
  response: Response
}>

/**
 * Strictly typed unwrap helper for openapi-fetch calls.
 * Caller cannot supply an arbitrary generic response type;
 * the returned data type is strictly derived from the endpoint schema.
 */
export async function unwrapApiResponse<T>(fetchPromise: FetchResponse<T>): Promise<T> {
  const { data, error, response } = await fetchPromise

  if (error || !response.ok || data === undefined) {
    let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`
    if (error && typeof error === 'object') {
      const errObj = error as Record<string, unknown>
      if ('error' in errObj && errObj.error) {
        errorMsg = String(errObj.error)
      } else if ('message' in errObj && errObj.message) {
        errorMsg = String(errObj.message)
      }
    }
    throw new ApiError(response.status, errorMsg, error)
  }

  return data
}
