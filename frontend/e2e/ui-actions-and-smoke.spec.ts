import { test, expect } from '@playwright/test'
import type { Locator } from '@playwright/test'
import { setupStandardApiMocks, createMockSearchResponse } from './helpers/mockApi'
import type { IngatlanItem } from '../src/types/property'

let pageErrors: Error[] = []
let consoleErrors: string[] = []

const SMOKE_SEARCH_ITEM: IngatlanItem = {
  id: 'dh-101',
  portal: 'Duna House',
  url: 'https://dh.hu/ingatlan/dh-101',
  title: 'Új építésű nappali + 3 szobás lakás Debrecenben',
  price: '89 M Ft',
  location: 'Debrecen, Nagyerdő',
  city: 'Debrecen',
  propertyType: 'LAKAS',
  isNew: true,
  viewed: false,
  newConstruction: true,
  imageUrl: '',
  sourcesCount: 1,
  priceInfo: { amountMillion: 89, amountHuf: 89_000_000, rawText: '89 M Ft' },
  detailsInfo: { areaSquareMeters: 95, roomCount: 4, pricePerSquareMeter: 936_842 },
}

const SMOKE_ARCHIVE_ITEM: IngatlanItem = {
  id: 'arch-1',
  portal: 'Otthon Centrum',
  url: 'https://oc.hu/ingatlanok/arch-1',
  title: 'Archív belvárosi polgári lakás',
  price: '68 M Ft',
  location: 'Debrecen',
  city: 'Debrecen',
  propertyType: 'LAKAS',
  isNew: false,
  viewed: false,
  newConstruction: false,
  sourcesCount: 1,
  priceInfo: { amountMillion: 68, amountHuf: 68_000_000, rawText: '68 M Ft' },
  detailsInfo: { areaSquareMeters: 74, roomCount: 3, pricePerSquareMeter: 918_918 },
}

async function assertTypeTabActive(activeTab: Locator, inactiveTab: Locator): Promise<void> {
  const ariaPressed = await activeTab.getAttribute('aria-pressed')
  const ariaSelected = await activeTab.getAttribute('aria-selected')
  if (ariaPressed !== null) {
    expect(ariaPressed).toBe('true')
    return
  }
  if (ariaSelected !== null) {
    expect(ariaSelected).toBe('true')
    return
  }
  const activeBg = await activeTab.evaluate((el) => getComputedStyle(el).backgroundColor)
  const inactiveBg = await inactiveTab.evaluate((el) => getComputedStyle(el).backgroundColor)
  expect(activeBg).not.toBe(inactiveBg)
}

test.describe('Ingatlan Kereső - UI Action & Endpoint Smoke Suite', () => {
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

    // If running in isolated frontend mode (e.g. Vite Preview without live Spring Boot backend),
    // provide realistic OpenAPI-typed mock responses for all REST API endpoints.
    const isLiveBackend = Boolean(process.env['PLAYWRIGHT_BASE_URL'])
    if (!isLiveBackend) {
      await setupStandardApiMocks(page, {
        historyItems: [SMOKE_ARCHIVE_ITEM],
        onSearchRequest: async (route) => {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(
              createMockSearchResponse([SMOKE_SEARCH_ITEM], {
                portalStatuses: [
                  {
                    portalId: 'dh',
                    portalName: 'Duna House',
                    status: 'SUCCESS',
                    latencyMs: 250,
                    foundCount: 1,
                  },
                ],
                marketStats: {
                  averagePriceMillion: 89,
                  averagePricePerM2: 936_842,
                  bargainCount: 1,
                  medianPricePerM2: 936_842,
                  minPriceMillion: 89,
                  maxPriceMillion: 89,
                  newRatePercent: 100,
                  cityBreakdown: { Debrecen: 1 },
                },
              }),
            ),
          })
        },
      })
    }
  })

  test.afterEach(async () => {
    // 0-Crash Resilience check: fail if ANY unhandled JS exception occurred
    expect(pageErrors, `Unhandled page errors detected:\n${pageErrors.map((e) => e.message).join('\n')}`).toHaveLength(0)
    // Check that no fatal console errors occurred
    const fatalErrors = consoleErrors.filter((e) => !e.includes('favicon') && !e.includes('404'))
    expect(fatalErrors, `Fatal console errors detected:\n${fatalErrors.join('\n')}`).toHaveLength(0)
  })

  test('1. Core Layout & Diagnostic Modals render without runtime exceptions', async ({ page }) => {
    await page.goto('/')

    // Verify Title & Header Brand / Navigation landmarks
    await expect(page).toHaveTitle(/Ingatlan Kereső Dashboard/i)
    await expect(page.getByText(/Ingatlan Kereső/i).first()).toBeVisible()
    await expect(page.getByRole('banner')).toBeVisible()
    await expect(page.getByTestId('listings-panel-header')).toBeVisible()

    // Test Diagnostics Modal
    const diagBtn = page
      .getByRole('button', { name: /Diagnosztika/i })
      .or(page.locator('header button:has([data-testid="header-portal-health-count"])'))
      .first()
    await expect(diagBtn).toBeVisible()
    await diagBtn.click()
    await expect(page.getByText('Portál Diagnosztika & Állapotjelentés')).toBeVisible()

    // Close Diagnostics Modal
    const closeDiagBtn = page.getByRole('button', { name: /Bezárás/i }).first()
    await closeDiagBtn.click()
    await expect(page.getByText('Portál Diagnosztika & Állapotjelentés')).not.toBeVisible()
  })

  test('2. Filter Panel, Steppers, Type Tabs and Portal Toggles interact cleanly', async ({ page }) => {
    await page.goto('/')

    // Verify Filter Section
    const searchPanel = page.getByTestId('search-panel-container')
    await expect(searchPanel).toBeVisible()

    // City Selector & Preset Buttons
    const debChip = searchPanel.getByText('Debrecen', { exact: true }).first()
    await expect(debChip).toBeVisible()

    // Click a preset button (Egyetemvárosok or Főbb egyetemvárosok)
    const uniPresetBtn = searchPanel
      .getByRole('button', { name: /Egyetemvárosok|Főbb egyetemvárosok/i })
      .first()
    await expect(uniPresetBtn).toBeVisible()
    await uniPresetBtn.click()
    await expect(searchPanel.getByText('Szeged', { exact: true }).first()).toBeVisible()

    // Property Type Tabs
    const houseTab = searchPanel.getByRole('button', { name: 'Ház', exact: true })
    const flatTab = searchPanel.getByRole('button', { name: 'Lakás', exact: true })
    await houseTab.click()
    await assertTypeTabActive(houseTab, flatTab)

    await flatTab.click()
    await assertTypeTabActive(flatTab, houseTab)

    // Price Stepper buttons
    const minPriceIncBtn = page.getByLabel('Növelés: Minimális ár')
    await expect(minPriceIncBtn).toBeVisible()
    await minPriceIncBtn.click()

    const maxPriceDecBtn = page.getByLabel('Csökkentés: Maximális ár')
    await expect(maxPriceDecBtn).toBeVisible()
    await maxPriceDecBtn.click()

    // Portal Toggles
    const dhToggle = searchPanel.getByRole('button', { name: /Duna House/i })
    await expect(dhToggle).toBeVisible()
    await dhToggle.click() // toggle off
    await dhToggle.click() // toggle back on

    // Collapse filter panel via TopNavBar sidebar-toggle-btn and verify summary bar (#157, #535)
    const sidebarToggleBtn = page.getByTestId('sidebar-toggle-btn')
    await expect(sidebarToggleBtn).toHaveAttribute('aria-expanded', 'true')
    await sidebarToggleBtn.click()
    await expect(sidebarToggleBtn).toHaveAttribute('aria-expanded', 'false')
    await expect(page.getByTestId('collapsed-filter-summary')).toBeVisible()
    await sidebarToggleBtn.click()
    await expect(sidebarToggleBtn).toHaveAttribute('aria-expanded', 'true')
    await expect(page.getByTestId('collapsed-filter-summary')).not.toBeVisible()
  })

  test('3. Search execution and Dashboard view switching triggers smoothly', async ({ page }) => {
    await page.goto('/')

    // Verify Listings Header does not contain duplicate Sort and ExcludeViewed controls (#359)
    const headerBar = page.getByTestId('listings-panel-header')
    await expect(headerBar).toBeVisible()
    await expect(headerBar.getByRole('button', { name: /Megtekintettek rejtése/i })).toHaveCount(0)
    await expect(headerBar.getByLabel('Rendezés')).toHaveCount(0)

    // Trigger Search
    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })
    await expect(searchBtn).toBeVisible()

    // Start search and wait for network response
    const searchPromise = page.waitForResponse(
      (resp) => resp.url().includes('/api/search') && (resp.status() === 200 || resp.status() === 304),
      { timeout: 20_000 }
    ).catch(() => null)

    await searchBtn.click()
    await searchPromise

    // Verify BulkActionBar 3-tier structure & customizable filter chips (#303, #359)
    const filterChipsRow = page.getByTestId('filter-chips-row')
    await expect(filterChipsRow).toBeVisible()
    const onlyNewToggle = filterChipsRow.getByRole('button', { name: /Új találatok/i })
    await expect(onlyNewToggle).toBeVisible()
    await onlyNewToggle.click()
    await onlyNewToggle.click() // toggle back off

    const bargainToggle = filterChipsRow.getByTestId('bargain-filter-toggle')
    await expect(bargainToggle).toBeVisible()
    const hiddenToggle = filterChipsRow.getByTestId('hidden-filter-toggle')
    await expect(hiddenToggle).toBeVisible()
    const filterSelectorBtn = filterChipsRow.getByTestId('quick-filter-selector-btn')
    await expect(filterSelectorBtn).toBeVisible()

    // Verify interactive MarketStatsBar filter cards
    const statsBar = page.getByTestId('market-stats-bar')
    await expect(statsBar).toBeVisible()
    const bargainCard = statsBar.getByTestId('kpi-filter-bargain')
    await bargainCard.click()
    await expect(bargainCard).toHaveAttribute('aria-pressed', 'true')
    await expect(headerBar.getByTestId('tab-listings')).toHaveAttribute('aria-selected', 'true')
    const allCard = statsBar.getByTestId('kpi-filter-all')
    await allCard.click()
    await expect(allCard).toHaveAttribute('aria-pressed', 'true')

    // Switch between TopNavBar Favorites and Listings views (#535)
    const listingsPanel = page.getByTestId('listings-panel')
    await expect(listingsPanel.getByTestId('subview-archive')).toHaveCount(0)

    const favoritesTabBtn = headerBar.getByTestId('subview-favorites')
    await expect(favoritesTabBtn).toBeVisible()
    await favoritesTabBtn.click()
    await expect(page.getByText('Még nincsenek kedvencnek jelölt ingatlanok')).toBeVisible()

    const talalatokTabBtn = headerBar.getByTestId('tab-listings')
    await expect(talalatokTabBtn).toBeVisible()
    await talalatokTabBtn.click()
    await expect(page.getByText('Új építésű nappali + 3 szobás lakás Debrecenben')).toBeVisible()

    // Switch to Piaci Elemzés top tab
    const analyticsTabBtn = headerBar.getByTestId('tab-analytics')
    await expect(analyticsTabBtn).toBeVisible()
    await analyticsTabBtn.click()
    await expect(analyticsTabBtn).toHaveAttribute('aria-selected', 'true')
    await expect(talalatokTabBtn).toHaveAttribute('aria-selected', 'false')
    await expect(page.getByTestId('market-analytics-panel')).toBeVisible()

    // Switch back to Találatok top tab
    await talalatokTabBtn.click()
    await expect(talalatokTabBtn).toHaveAttribute('aria-selected', 'true')
    await expect(analyticsTabBtn).toHaveAttribute('aria-selected', 'false')
    await expect(page.getByTestId('listings-panel')).toBeVisible()
  })
})
