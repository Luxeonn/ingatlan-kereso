import { test, expect } from '@playwright/test'
import {
  setupStandardApiMocks,
  createMockSearchResponse,
  SAMPLE_PROPERTIES,
} from './helpers/mockApi'
import type { IngatlanItem } from '../src/types/property'

let pageErrors: Error[] = []
let consoleErrors: string[] = []

function buildManyListings(count: number): IngatlanItem[] {
  const template = SAMPLE_PROPERTIES[0]!
  return Array.from({ length: count }, (_, i) => ({
    ...template,
    id: `item-${i}`,
    portal: 'Duna House',
    url: `https://dh.hu/ingatlan/item-${i}`,
    title: `Eladó lakás #${i}`,
    price: `${30 + i} M Ft`,
    isNew: true,
    viewed: false,
    priceInfo: { amountMillion: 30 + i, amountHuf: (30 + i) * 1_000_000, rawText: `${30 + i} M Ft` },
    detailsInfo: { areaSquareMeters: 60, roomCount: 2, pricePerSquareMeter: 500_000 + i * 10_000 },
  }))
}

test.describe('Ingatlan Kereső - Scroll Position Preservation (#459)', () => {
  test.beforeEach(async ({ page }) => {
    pageErrors = []
    consoleErrors = []

    page.on('pageerror', (err) => {
      pageErrors.push(err)
    })

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text())
      }
    })
  })

  test.afterEach(async () => {
    expect(pageErrors).toHaveLength(0)
    const fatalErrors = consoleErrors.filter(
      (e) => !e.includes('favicon') && !e.includes('404') && !e.includes('500'),
    )
    expect(fatalErrors).toHaveLength(0)
  })

  test('preserves scroll position (<= 5px drift) after opening 25th+ listing in new tab and returning', async ({
    page,
    context,
  }) => {
    const manyItems = buildManyListings(42)

    await context.route('https://dh.hu/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<html><body><h1>Mock External Listing</h1></body></html>',
      })
    })

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(createMockSearchResponse(manyItems)),
        })
      },
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card').first()).toBeVisible()

    // Scroll down progressively until the 26th item (#25) is loaded and in the viewport
    const targetTitle = page.getByRole('heading', { name: 'Eladó lakás #25', exact: true })
    for (let step = 0; step < 12; step++) {
      if ((await targetTitle.count()) > 0 && (await targetTitle.isVisible())) {
        break
      }
      await page.evaluate(() => window.scrollBy(0, 700))
      await page.waitForTimeout(150)
    }

    await expect(targetTitle).toBeVisible()
    await targetTitle.evaluate((el) => el.scrollIntoView({ block: 'center', behavior: 'instant' }))
    await page.waitForTimeout(150)

    const scrollBefore = await page.evaluate(() => window.scrollY)
    expect(scrollBefore).toBeGreaterThan(300)

    const [popup] = await Promise.all([
      context.waitForEvent('page'),
      targetTitle.click(),
    ])
    await popup.close()
    await page.bringToFront()
    await page.waitForTimeout(200)

    const scrollAfter = await page.evaluate(() => window.scrollY)
    expect(Math.abs(scrollAfter - scrollBefore)).toBeLessThanOrEqual(5)
    await expect(targetTitle).toBeInViewport()
  })
})
