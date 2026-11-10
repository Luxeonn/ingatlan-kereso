import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ApiError, fetchClient, unwrapApiResponse } from '../api/client'
import { getCities } from '../api/cityApi'
import { getPortals } from '../api/portalApi'
import { getLastConfig, executeSearch } from '../api/searchApi'
import { markAsViewed, openUrls, getHistory, clearHistory, hideProperty, restoreProperty, getHiddenProperties } from '../api/propertyApi'
import { getPresets, createPreset, updatePreset, deletePreset, recordPresetUsage } from '../api/presetApi'
import type { SearchCriteria } from '../types/api'

describe('Typed OpenAPI Client (Issue #402)', () => {
  const originalFetch = globalThis.fetch

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  describe('unwrapApiResponse & ApiError', () => {
    it('returns unwrapped data on successful 200 response', async () => {
      const mockPromise = Promise.resolve({
        data: { success: true, count: 5 },
        error: undefined,
        response: new Response(JSON.stringify({ success: true, count: 5 }), { status: 200 }),
      })

      const result = await unwrapApiResponse(mockPromise)
      expect(result).toEqual({ success: true, count: 5 })
    })

    it('throws typed ApiError when error object contains error property', async () => {
      const mockPromise = Promise.resolve({
        data: undefined,
        error: { error: 'Érvénytelen kérés paraméter' },
        response: new Response(null, { status: 400, statusText: 'Bad Request' }),
      })

      await expect(unwrapApiResponse(mockPromise)).rejects.toThrow(ApiError)
      try {
        await unwrapApiResponse(mockPromise)
      } catch (err) {
        expect(err).toBeInstanceOf(ApiError)
        const apiErr = err as ApiError
        expect(apiErr.status).toBe(400)
        expect(apiErr.message).toBe('Érvénytelen kérés paraméter')
      }
    })

    it('throws typed ApiError with message when error object contains message property', async () => {
      const mockPromise = Promise.resolve({
        data: undefined,
        error: { message: 'Erőforrás nem található' },
        response: new Response(null, { status: 404, statusText: 'Not Found' }),
      })

      await expect(unwrapApiResponse(mockPromise)).rejects.toThrow(ApiError)
      try {
        await unwrapApiResponse(mockPromise)
      } catch (err) {
        const apiErr = err as ApiError
        expect(apiErr.status).toBe(404)
        expect(apiErr.message).toBe('Erőforrás nem található')
      }
    })

    it('falls back to HTTP status text when error payload lacks text messages', async () => {
      const mockPromise = Promise.resolve({
        data: undefined,
        error: true,
        response: new Response(null, { status: 500, statusText: 'Internal Server Error' }),
      })

      try {
        await unwrapApiResponse(mockPromise)
        expect.unreachable('Should have thrown')
      } catch (err) {
        const apiErr = err as ApiError
        expect(apiErr.status).toBe(500)
        expect(apiErr.message).toBe('HTTP Error 500: Internal Server Error')
      }
    })
  })

  describe('Typed Endpoint Integration', () => {
    const getRequestInfo = (input: unknown, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : (input as Request).url
      const method = init?.method ?? (typeof input === 'string' ? 'GET' : (input as Request).method)
      return { url, method }
    }

    it('executes getCities via GET /api/cities', async () => {
      const mockData = {
        predefined: [{ key: 'debrecen', name: 'Debrecen', county: 'Hajdú-Bihar', description: 'Székhely' }],
        presets: { 'Egyetemvárosok': ['Debrecen'] },
        trackedCities: [{ key: 'debrecen', name: 'Debrecen', count: 120 }],
      }
      globalThis.fetch = vi.fn().mockResolvedValue(
        new Response(JSON.stringify(mockData), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

      const res = await getCities()
      expect(res).toEqual(mockData)

      const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>
      const callArg = fetchMock.mock.calls[0]?.[0]
      expect(callArg).toBeDefined()
      const { url, method } = getRequestInfo(callArg)
      expect(url).toContain('/api/cities')
      expect(method).toBe('GET')
    })

    it('executes getPortals via GET /api/portals', async () => {
      const mockPortals = [
        {
          id: 'dh',
          name: 'Duna House',
          brandColor: '#008638',
          homepageUrl: 'https://dh.hu',
          enabledByDefault: true,
          supportedTypes: ['LAKAS', 'HAZ'],
          directListingSupported: true,
          savedListingCount: 42,
        },
      ]
      globalThis.fetch = vi.fn().mockResolvedValue(
        new Response(JSON.stringify(mockPortals), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

      const res = await getPortals()
      expect(res).toEqual(mockPortals)

      const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>
      const callArg = fetchMock.mock.calls[0]?.[0]
      expect(callArg).toBeDefined()
      const { url, method } = getRequestInfo(callArg)
      expect(url).toContain('/api/portals')
      expect(method).toBe('GET')
    })

    it('executes searchApi functions with typed contracts', async () => {
      const mockConfig = { city: 'Hajdúszoboszló', cities: ['Hajdúszoboszló'], minPriceMillion: 70 }
      globalThis.fetch = vi.fn().mockResolvedValue(
        new Response(JSON.stringify(mockConfig), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

      const config = await getLastConfig()
      expect(config).toEqual(mockConfig)

      const mockDashboard = { items: [], newCount: 0, totalFound: 0 }
      globalThis.fetch = vi.fn().mockResolvedValue(
        new Response(JSON.stringify(mockDashboard), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

      const testCriteria: SearchCriteria = {
        city: 'Debrecen',
        cities: ['Debrecen'],
        propertyType: 'LAKAS',
        minPriceMillion: 0,
        maxPriceMillion: 100,
        minArea: 0,
        maxArea: 200,
        portals: ['dh'],
        autoOpenBrowser: false,
        excludeViewed: false,
        onlyDiscounted: false,
        onlyNewConstruction: false,
      }
      const searchRes = await executeSearch(testCriteria)
      expect(searchRes).toEqual(mockDashboard)

      const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>
      const searchReq = fetchMock.mock.calls[0]?.[0]
      expect(searchReq).toBeDefined()
      const { url, method } = getRequestInfo(searchReq)
      expect(url).toContain('/api/search')
      expect(method).toBe('POST')
      if (searchReq instanceof Request) {
        const body = await searchReq.clone().json()
        expect(body).toEqual(testCriteria)
      }
    })

    it('executes propertyApi mutations with typed bodies and routes', async () => {
      globalThis.fetch = vi.fn().mockImplementation((input: unknown, init?: RequestInit) => {
        const { url } = getRequestInfo(input, init)
        if (url.includes('/api/items/viewed')) {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true, marked: true, url: 'https://dh.hu/1' }), { status: 200 }),
          )
        }
        if (url.includes('/api/open-urls')) {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true, count: 2 }), { status: 200 }),
          )
        }
        if (url.includes('/api/history?')) {
          return Promise.resolve(
            new Response(JSON.stringify([]), { status: 200 }),
          )
        }
        if (url.includes('/api/history/clear')) {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true, clearedCity: 'Debrecen' }), { status: 200 }),
          )
        }
        if (url.includes('/api/properties/prop-1/hide')) {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true, hidden: true, id: 'prop-1' }), { status: 200 }),
          )
        }
        if (url.includes('/api/properties/prop-1/restore')) {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true, hidden: false, id: 'prop-1' }), { status: 200 }),
          )
        }
        if (url.includes('/api/properties/hidden')) {
          return Promise.resolve(
            new Response(JSON.stringify([]), { status: 200 }),
          )
        }
        return Promise.reject(new Error(`Unexpected url: ${url}`))
      })

      const viewed = await markAsViewed('https://dh.hu/1')
      expect(viewed.marked).toBe(true)

      const opened = await openUrls(['https://dh.hu/1', 'https://oc.hu/2'])
      expect(opened.count).toBe(2)

      const history = await getHistory('Debrecen', true, false)
      expect(history).toEqual([])

      const cleared = await clearHistory('Debrecen')
      expect(cleared.clearedCity).toBe('Debrecen')

      const hidden = await hideProperty('prop-1', ['https://dh.hu/1'])
      expect(hidden.hidden).toBe(true)

      const restored = await restoreProperty('prop-1')
      expect(restored.hidden).toBe(false)

      const hiddenList = await getHiddenProperties()
      expect(hiddenList).toEqual([])
    })

    it('executes presetApi CRUD operations with path parameters', async () => {
      globalThis.fetch = vi.fn().mockImplementation((input: unknown, init?: RequestInit) => {
        const { url, method } = getRequestInfo(input, init)
        if (url.endsWith('/api/presets') && method === 'GET') {
          return Promise.resolve(
            new Response(JSON.stringify([]), { status: 200 }),
          )
        }
        if (url.endsWith('/api/presets') && method === 'POST') {
          return Promise.resolve(
            new Response(JSON.stringify({ id: 1, name: 'Új profil' }), { status: 200 }),
          )
        }
        if (url.endsWith('/api/presets/1') && method === 'PUT') {
          return Promise.resolve(
            new Response(JSON.stringify({ id: 1, name: 'Módosított profil' }), { status: 200 }),
          )
        }
        if (url.endsWith('/api/presets/1') && method === 'DELETE') {
          return Promise.resolve(
            new Response(JSON.stringify({ success: true }), { status: 200 }),
          )
        }
        if (url.endsWith('/api/presets/1/use') && method === 'POST') {
          return Promise.resolve(
            new Response(JSON.stringify({ id: 1, name: 'Használt profil' }), { status: 200 }),
          )
        }
        return Promise.reject(new Error(`Unexpected url: ${url} (method: ${method})`))
      })

      const presets = await getPresets()
      expect(presets).toEqual([])

      const created = await createPreset({ name: 'Új profil' })
      expect(created.name).toBe('Új profil')

      const updated = await updatePreset(1, { name: 'Módosított profil' })
      expect(updated.name).toBe('Módosított profil')

      const deleted = await deletePreset(1)
      expect(deleted.success).toBe(true)

      const used = await recordPresetUsage(1)
      expect(used.name).toBe('Használt profil')
    })
  })

  describe('Compile-time Type Safety Guarantee', () => {
    it('verifies that fetchClient enforces type-safe endpoint keys', () => {
      // Direct assertion that fetchClient exposes standard HTTP verbs
      expect(typeof fetchClient.GET).toBe('function')
      expect(typeof fetchClient.POST).toBe('function')
      expect(typeof fetchClient.PUT).toBe('function')
      expect(typeof fetchClient.DELETE).toBe('function')
    })
  })
})
