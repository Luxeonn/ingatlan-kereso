import { test, expect } from '@playwright/test'

let pageErrors: Error[] = []
let consoleErrors: string[] = []

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

    // Verify Title & Header Brand
    await expect(page).toHaveTitle(/Ingatlan Kereső Dashboard/i)
    await expect(page.getByText('Ingatlan Kereső')).toBeVisible()
    await expect(page.getByText('SPA')).toBeVisible()

    // Test Diagnostics Modal
    const diagBtn = page.getByRole('button', { name: /Diagnosztika/i })
    await expect(diagBtn).toBeVisible()
    await diagBtn.click()
    await expect(page.getByText('Rendszer- és Portáldiagnosztika')).toBeVisible()

    // Close Diagnostics Modal
    const closeDiagBtn = page.getByRole('button', { name: /Bezárás/i }).first()
    await closeDiagBtn.click()
    await expect(page.getByText('Rendszer- és Portáldiagnosztika')).not.toBeVisible()

    // Test Log Viewer Modal
    const logBtn = page.getByRole('button', { name: /Napló/i })
    await expect(logBtn).toBeVisible()
    await logBtn.click()
    await expect(page.getByText('Rendszernapló & Hibakeresés')).toBeVisible()

    // Close Log Modal
    const closeLogBtn = page.getByRole('button', { name: /Bezárás/i }).first()
    await closeLogBtn.click()
    await expect(page.getByText('Rendszernapló & Hibakeresés')).not.toBeVisible()
  })

  test('2. Filter Panel, Steppers, Type Tabs and Portal Toggles interact cleanly', async ({ page }) => {
    await page.goto('/')

    // Verify Filter Section
    await expect(page.getByText('Keresési Paraméterek & Szűrők')).toBeVisible()

    // City Selector & Preset Buttons
    const debChip = page.getByText('Debrecen', { exact: true })
    await expect(debChip).toBeVisible()

    // Click a preset button (e.g. Főbb egyetemvárosok)
    const uniPresetBtn = page.getByRole('button', { name: /Főbb egyetemvárosok/i })
    if (await uniPresetBtn.isVisible()) {
      await uniPresetBtn.click()
      // Should now include Budapest or Szeged
      await expect(page.getByText('Szeged', { exact: true })).toBeVisible()
    }

    // Property Type Tabs
    const houseTab = page.getByRole('button', { name: 'Ház' })
    await houseTab.click()
    await expect(houseTab).toHaveClass(/bg-blue-600/)

    const flatTab = page.getByRole('button', { name: 'Lakás' })
    await flatTab.click()
    await expect(flatTab).toHaveClass(/bg-blue-600/)

    // Price Stepper buttons
    const minPriceIncBtn = page.getByLabel('Növelés: Minimális ár')
    await expect(minPriceIncBtn).toBeVisible()
    await minPriceIncBtn.click()

    const maxPriceDecBtn = page.getByLabel('Csökkentés: Maximális ár')
    await expect(maxPriceDecBtn).toBeVisible()
    await maxPriceDecBtn.click()

    // Portal Toggles
    const dhToggle = page.getByRole('button', { name: /Duna House/i })
    await expect(dhToggle).toBeVisible()
    await dhToggle.click() // toggle off
    await dhToggle.click() // toggle back on

    // Quick Toggles
    const onlyNewToggle = page.getByRole('button', { name: /Csak új találatok/i })
    await expect(onlyNewToggle).toBeVisible()
    await onlyNewToggle.click()
  })

  test('3. Search execution and Dashboard view switching triggers smoothly', async ({ page }) => {
    await page.goto('/')

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

    // Switch between Listings and Analytics tab
    const analyticsTabBtn = page.getByRole('button', { name: /Piaci Elemzés és Árdiagramok/i })
    await expect(analyticsTabBtn).toBeVisible()
    await analyticsTabBtn.click()
    await expect(analyticsTabBtn).toHaveClass(/bg-indigo-600/)

    const listingsTabBtn = page.getByRole('button', { name: /Hirdetések/i })
    await expect(listingsTabBtn).toBeVisible()
    await listingsTabBtn.click()
    await expect(listingsTabBtn).toHaveClass(/bg-blue-600/)
  })
})
