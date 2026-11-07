import { test, expect } from '@playwright/test'
import {
  setupStandardApiMocks,
  createMockSearchResponse,
  SAMPLE_PROPERTIES,
} from './helpers/mockApi'
import type { SearchCriteria } from '../src/types/api'

let pageErrors: Error[] = []
let consoleErrors: string[] = []

test.describe('Ingatlan Kereső - Search, Filter & Sort E2E Flow Suite (#377)', () => {
  test.beforeEach(async ({ page }) => {
    pageErrors = []
    consoleErrors = []

    page.on('pageerror', (err) => {
      console.error('[Browser PageError]:', err.message)
      pageErrors.push(err)
    })

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        console.error('[Browser ConsoleError]:', msg.text())
        consoleErrors.push(msg.text())
      }
    })
  })

  test.afterEach(async () => {
    expect(pageErrors, `Unhandled page errors:\n${pageErrors.map((e) => e.message).join('\n')}`).toHaveLength(0)
    const fatalErrors = consoleErrors.filter((e) => !e.includes('favicon') && !e.includes('404') && !e.includes('500'))
    expect(fatalErrors, `Fatal console errors:\n${fatalErrors.join('\n')}`).toHaveLength(0)
  })

  test('1. Search request parameter contract: criteria form inputs map correctly to POST /api/search body', async ({ page }) => {
    let capturedCriteria: SearchCriteria | null = null

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route, criteria) => {
        capturedCriteria = criteria
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createMockSearchResponse()),
        })
      },
    })

    await page.goto('/')

    // Add city "Szeged" via city input
    const cityInput = page.getByPlaceholder(/Válassz vagy írj be egy várost|További város hozzáadása/i)
    await cityInput.fill('Szeged')
    await cityInput.press('Enter')

    // Switch property type to "Ház"
    const houseTypeBtn = page.getByRole('button', { name: 'Ház', exact: true })
    await houseTypeBtn.click()

    // Toggle off Duna House portal
    const dhToggle = page.getByRole('button', { name: /Duna House/i })
    await dhToggle.click()

    // Trigger search
    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })
    await searchBtn.click()

    // Wait for cards to appear
    await expect(page.getByTestId('property-card').first()).toBeVisible()

    // Verify contract parameters sent to the backend
    expect(capturedCriteria).not.toBeNull()
    expect(capturedCriteria!.cities).toContain('Szeged')
    expect(capturedCriteria!.propertyType?.toLowerCase()).toBe('haz')
    expect(capturedCriteria!.portals).not.toContain('dh')
  })

  test('2. Loading state: shows disabled loading button and skeleton cards while request is in flight', async ({ page }) => {
    let fulfillResponse!: () => void
    const responsePromise = new Promise<void>((resolve) => {
      fulfillResponse = resolve
    })

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        await responsePromise
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createMockSearchResponse()),
        })
      },
    })

    await page.goto('/')

    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })
    await searchBtn.click()

    // Loading button state
    await expect(page.getByRole('button', { name: /Keresés folyamatban/i })).toBeDisabled()

    // Skeleton placeholders visible in grid
    const skeletons = page.getByTestId('listing-skeleton')
    await expect(skeletons.first()).toBeVisible()
    expect(await skeletons.count()).toBeGreaterThanOrEqual(1)

    // Complete the search response
    fulfillResponse()

    // Skeletons disappear, real cards appear, button returns to normal
    await expect(page.getByTestId('property-card').first()).toBeVisible()
    await expect(skeletons).toHaveCount(0)
    await expect(page.getByRole('button', { name: /Ingatlanok Keresése/i })).toBeEnabled()
  })

  test('3. Sorting operations: reorders property cards correctly (price_asc, price_desc, area_desc, bargain_desc)', async ({ page }) => {
    await setupStandardApiMocks(page)
    await page.goto('/')

    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })
    await searchBtn.click()

    const cards = page.getByTestId('property-card')
    await expect(cards).toHaveCount(3)

    // 1. Default sort is price_asc: 45 M Ft, 85 M Ft, 130 M Ft
    await expect(cards.nth(0)).toContainText('45 M Ft')
    await expect(cards.nth(1)).toContainText('85 M Ft')
    await expect(cards.nth(2)).toContainText('130 M Ft')

    const sortSelect = page.getByLabel('Rendezés')

    // 2. Sort by price_desc: 130 M Ft, 85 M Ft, 45 M Ft
    await sortSelect.selectOption('price_desc')
    await expect(cards.nth(0)).toContainText('130 M Ft')
    await expect(cards.nth(1)).toContainText('85 M Ft')
    await expect(cards.nth(2)).toContainText('45 M Ft')

    // 3. Sort by area_desc: 120 m² (85 M), 80 m² (130 M), 50 m² (45 M)
    await sortSelect.selectOption('area_desc')
    await expect(cards.nth(0)).toContainText('120 m²')
    await expect(cards.nth(1)).toContainText('80 m²')
    await expect(cards.nth(2)).toContainText('50 m²')

    // 4. Sort by bargain_desc: P1 (25% below median) is first
    await sortSelect.selectOption('bargain_desc')
    await expect(cards.nth(0)).toContainText('45 M Ft')
  })

  test('4. In-memory filtering & combination: quick filters and search input refine the list reactively', async ({ page }) => {
    await setupStandardApiMocks(page)
    await page.goto('/')

    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    const cards = page.getByTestId('property-card')
    await expect(cards).toHaveCount(3)

    // Filter by "Csak új találatok" (P1 and P3 are new, P2 is not)
    const onlyNewBtn = page.getByTestId('filter-chips-row').getByRole('button', { name: /Csak új találatok/i })
    await onlyNewBtn.click()

    await expect(cards).toHaveCount(2)
    await expect(cards.nth(0)).toContainText('45 M Ft')
    await expect(cards.nth(1)).toContainText('130 M Ft')

    // Combine with sort price_desc
    const sortSelect = page.getByLabel('Rendezés')
    await sortSelect.selectOption('price_desc')
    await expect(cards.nth(0)).toContainText('130 M Ft')
    await expect(cards.nth(1)).toContainText('45 M Ft')

    // Toggle off "Csak új találatok"
    await onlyNewBtn.click()
    await expect(cards).toHaveCount(3)

    // Quick search by text keyword: "nagypolgári" matches only P2
    const quickSearchInput = page.getByPlaceholder(/Keresés a találatok között/i)
    await quickSearchInput.fill('nagypolgári')
    await expect(cards).toHaveCount(1)
    await expect(cards.first()).toContainText('Belvárosi tágas nagypolgári lakás')

    // Clear search keyword
    await quickSearchInput.fill('')
    await expect(cards).toHaveCount(3)
  })

  test('5. Empty results state: renders friendly empty feedback when search returns 0 items', async ({ page }) => {
    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createMockSearchResponse([])),
        })
      },
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()

    // Assert empty state title and description
    await expect(page.getByText('Nem található ingatlan a megadott feltételekkel')).toBeVisible()
    await expect(page.getByText(/Próbáld meg módosítani a gyorskereső kifejezést/i)).toBeVisible()

    // Verify 0 property cards
    await expect(page.getByTestId('property-card')).toHaveCount(0)
    await expect(page.getByTestId('listing-skeleton')).toHaveCount(0)
  })

  test('6. API error state: renders accessible error banner with retry button on server failure', async ({ page }) => {
    let attemptCount = 0

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        attemptCount++
        if (attemptCount === 1) {
          await route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({ error: 'Belső szerverhiba történt a keresés közben' }),
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse()),
          })
        }
      },
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()

    // Error banner is displayed
    const errorBanner = page.getByTestId('search-error-banner')
    await expect(errorBanner).toBeVisible()
    await expect(errorBanner).toHaveAttribute('role', 'alert')
    await expect(errorBanner).toContainText('Hiba történt a keresés során')
    await expect(errorBanner).toContainText('Belső szerverhiba történt a keresés közben')

    // Click retry button in error banner
    const retryBtn = errorBanner.getByRole('button', { name: /Újrapróbálás/i })
    await retryBtn.click()

    // Second attempt succeeds
    await expect(errorBanner).not.toBeVisible()
    await expect(page.getByTestId('property-card').first()).toBeVisible()
  })

  test('7. Partial portal failure: renders successful items while displaying partial failure warning banner', async ({ page }) => {
    const singlePortalItem = SAMPLE_PROPERTIES.slice(0, 1) // Only Duna House item

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            createMockSearchResponse(singlePortalItem, {
              portalStatuses: [
                { portalId: 'dh', portalName: 'Duna House', status: 'SUCCESS', latencyMs: 220, foundCount: 1 },
                { portalId: 'oc', portalName: 'Otthon Centrum', status: 'BOT_PROTECTED', latencyMs: 400, foundCount: 0, fallbackUrl: 'https://oc.hu' },
                { portalId: 'jofogas', portalName: 'Jófogás', status: 'FAILED', latencyMs: 500, foundCount: 0, message: 'Kapcsolódási hiba' },
              ],
            }),
          ),
        })
      },
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()

    // 1. Successful items are displayed without disruption
    const cards = page.getByTestId('property-card')
    await expect(cards).toHaveCount(1)
    await expect(cards.first()).toContainText('Duna House')

    // 2. Partial portal warning banner is visible
    const warningBanner = page.getByTestId('partial-portal-warning-banner')
    await expect(warningBanner).toBeVisible()
    await expect(warningBanner).toContainText('Otthon Centrum, Jófogás')

    // 3. Open Diagnostics from warning banner
    const diagBtn = warningBanner.getByRole('button', { name: /Diagnosztika megtekintése/i })
    await diagBtn.click()

    await expect(page.getByText('Portál Diagnosztika & Állapotjelentés')).toBeVisible()
    await expect(page.getByTestId('portal-status-card-oc')).toBeVisible()
    await expect(page.getByTestId('portal-status-card-jofogas')).toBeVisible()
  })

  test('8. Search modification: updating search clears old results and displays only new matching results', async ({ page }) => {
    let searchStep = 1

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        if (searchStep === 1) {
          // Debrecen results
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse(SAMPLE_PROPERTIES.slice(0, 2))),
          })
        } else {
          // Szeged results
          const szegedProperty = {
            ...SAMPLE_PROPERTIES[2],
            id: 'szeged-404',
            title: 'Szegedi Tisza-parti új ház',
            city: 'Szeged',
            location: 'Szeged, Belváros',
            price: '99 M Ft',
          }
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse([szegedProperty])),
          })
        }
      },
    })

    await page.goto('/')
    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })

    // Step 1: Initial search
    await searchBtn.click()
    const cards = page.getByTestId('property-card')
    await expect(cards).toHaveCount(2)
    await expect(page.getByText('Nagyerdő csendes modern lakás Debrecenben')).toBeVisible()

    // Step 2: Modify search to Szeged
    searchStep = 2
    const cityInput = page.getByPlaceholder(/Válassz vagy írj be egy várost|További város hozzáadása/i)
    await cityInput.fill('Szeged')
    await cityInput.press('Enter')

    await searchBtn.click()

    // Verify only the Szeged result is shown, Debrecen result is cleared
    await expect(cards).toHaveCount(1)
    await expect(page.getByText('Szegedi Tisza-parti új ház')).toBeVisible()
    await expect(page.getByText('Nagyerdő csendes modern lakás Debrecenben')).not.toBeVisible()
  })
})
