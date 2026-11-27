import { test, expect } from '@playwright/test'
import { setupStandardApiMocks, createMockSearchResponse, SAMPLE_PROPERTIES } from './helpers/mockApi'
import type { IngatlanItem } from '../src/types/property'

test.describe('Manual Dedup Overrides E2E (#461)', () => {
  test('kijelölés -> összevonás -> újrakeresés után is egy kártya; forrás kivétele -> újrakeresés után is külön', async ({
    page,
  }) => {
    const item1: IngatlanItem = {
      ...SAMPLE_PROPERTIES[0]!,
      id: 'e2e-1',
      portal: 'Duna House',
      url: 'https://dh.hu/e2e-1',
      title: 'Első lakás Debrecen',
      sourcesCount: 1,
      sourceListings: [{ portal: 'Duna House', url: 'https://dh.hu/e2e-1', priceHuf: 45_000_000 }],
    }
    const item2: IngatlanItem = {
      ...SAMPLE_PROPERTIES[0]!,
      id: 'e2e-2',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/e2e-2',
      title: 'Második lakás Debrecen',
      sourcesCount: 1,
      sourceListings: [{ portal: 'Otthon Centrum', url: 'https://oc.hu/e2e-2', priceHuf: 46_000_000 }],
    }

    let isMerged = false
    let isSplit = false

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        if (isMerged && !isSplit) {
          const mergedItem: IngatlanItem = {
            ...item1,
            manuallyMerged: true,
            sourcesCount: 2,
            sourceListings: [...(item1.sourceListings ?? []), ...(item2.sourceListings ?? [])],
          }
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse([mergedItem])),
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(
              createMockSearchResponse([
                { ...item1, manuallySplit: isSplit },
                { ...item2, manuallySplit: isSplit },
              ]),
            ),
          })
        }
      },
    })

    await page.route('**/api/dedup/merge', async (route) => {
      isMerged = true
      isSplit = false
      const mergedItem: IngatlanItem = {
        ...item1,
        manuallyMerged: true,
        sourcesCount: 2,
        sourceListings: [...(item1.sourceListings ?? []), ...(item2.sourceListings ?? [])],
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [mergedItem],
          overrides: [],
          message: 'Hirdetések sikeresen összevonva',
        }),
      })
    })

    await page.route('**/api/dedup/split', async (route) => {
      isMerged = false
      isSplit = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [
            { ...item1, manuallySplit: true },
            { ...item2, manuallySplit: true },
          ],
          overrides: [],
          message: 'Forrás sikeresen leválasztva',
        }),
      })
    })

    await page.route('**/api/dedup/overrides**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(2)

    // Ctrl+click both cards
    const cards = page.getByTestId('property-card')
    await cards.nth(0).click({ modifiers: ['Control'] })
    await cards.nth(1).click({ modifiers: ['Control'] })

    await expect(page.getByTestId('selection-count-label')).toHaveText('2 kijelölve')
    await page.getByTestId('merge-selected-button').click()

    // Merged into 1 card
    await expect(page.getByTestId('property-card')).toHaveCount(1)

    // Re-search -> still 1 card
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(1)

    // Open drawer and split source
    await page.getByTestId('sources-count-badge').click()
    await page.getByTestId('split-source-button').first().click()
    await page.getByTestId('confirm-split-button').click()

    await expect(page.getByTestId('property-card')).toHaveCount(2)

    // Close details modal before re-searching
    await page.getByTestId('property-details-modal-close').click()

    // Re-search -> still 2 separate cards
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(2)
  })

  test('görgetés közben látható lebegő összevonás gomb és automatikus keresésfrissítés összevonás után (#487)', async ({
    page,
  }) => {
    const items: IngatlanItem[] = Array.from({ length: 12 }, (_, idx) => ({
      ...SAMPLE_PROPERTIES[0]!,
      id: `scroll-${idx}`,
      portal: idx % 2 === 0 ? 'Duna House' : 'Otthon Centrum',
      url: `https://dh.hu/scroll-${idx}`,
      title: `Görgethető lakás #${idx + 1}`,
      sourcesCount: 1,
      sourceListings: [
        {
          portal: idx % 2 === 0 ? 'Duna House' : 'Otthon Centrum',
          url: `https://dh.hu/scroll-${idx}`,
          priceHuf: 40_000_000 + idx * 1_000_000,
        },
      ],
    }))

    let isMerged = false
    let searchRequestCount = 0

    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        searchRequestCount += 1
        if (isMerged) {
          const mergedItem: IngatlanItem = {
            ...items[10]!,
            manuallyMerged: true,
            sourcesCount: 2,
            sourceListings: [
              ...(items[10]!.sourceListings ?? []),
              ...(items[11]!.sourceListings ?? []),
            ],
          }
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse([...items.slice(0, 10), mergedItem])),
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse(items)),
          })
        }
      },
    })

    await page.route('**/api/dedup/merge', async (route) => {
      isMerged = true
      const mergedItem: IngatlanItem = {
        ...items[10]!,
        manuallyMerged: true,
        sourcesCount: 2,
        sourceListings: [
          ...(items[10]!.sourceListings ?? []),
          ...(items[11]!.sourceListings ?? []),
        ],
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [mergedItem],
          overrides: [],
          message: 'Hirdetések sikeresen összevonva',
        }),
      })
    })

    await page.route('**/api/dedup/overrides**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    })

    await page.goto('/')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('tab-listings')).toContainText(/12/)
    expect(searchRequestCount).toBe(1)

    // Scroll down to bottom cards so the top BulkActionBar is scrolled above viewport
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
    const card11 = page.getByTestId('property-card').filter({ hasText: 'Görgethető lakás #11' })
    const card12 = page.getByTestId('property-card').filter({ hasText: 'Görgethető lakás #12' })
    await expect(card11).toBeVisible()
    await expect(card12).toBeVisible()

    await card11.click({ modifiers: ['Control'] })
    await card12.click({ modifiers: ['Control'] })

    const actionBar = page.getByTestId('selection-action-bar')
    const mergeBtn = page.getByTestId('merge-selected-button')
    await expect(actionBar).toBeInViewport()
    await expect(mergeBtn).toBeInViewport()
    await expect(page.getByTestId('selection-count-label')).toHaveText('2 kijelölve')

    // Click merge while still scrolled down -> merge + automatic follow-up /api/search fires
    await mergeBtn.click()
    await expect.poll(() => searchRequestCount).toBe(2)
    await expect(page.getByTestId('tab-listings')).toContainText(/11/)
    await expect(
      page.getByTestId('listings-panel').getByText('11 ingatlan (12 hirdetés)'),
    ).toBeVisible()
    await expect(actionBar).toHaveCount(0)
  })

  test('Kuka (Globális Elrejtettek) nézetben két kártya összevonása azonnal 1 többforrású kártyává áll össze és újratöltés után is megmarad (#500)', async ({
    page,
  }) => {
    const hidden1: IngatlanItem = {
      ...SAMPLE_PROPERTIES[0]!,
      id: 'h-e2e-1',
      portal: 'Duna House',
      url: 'https://dh.hu/h-e2e-1',
      title: 'Kukázott ház forrás 1',
      city: 'Debrecen',
      hidden: true,
      sourcesCount: 1,
      sourceListings: [{ portal: 'Duna House', url: 'https://dh.hu/h-e2e-1', priceHuf: 87_500_000 }],
    }
    const hidden2: IngatlanItem = {
      ...SAMPLE_PROPERTIES[0]!,
      id: 'h-e2e-2',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/h-e2e-2',
      title: 'Kukázott ház forrás 2',
      city: 'Debrecen',
      hidden: true,
      sourcesCount: 1,
      sourceListings: [{ portal: 'Otthon Centrum', url: 'https://oc.hu/h-e2e-2', priceHuf: 89_000_000 }],
    }

    let isHiddenMerged = false
    let hiddenFetchCount = 0

    await setupStandardApiMocks(page)
    await page.route('**/api/properties/discounted**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    })
    await page.route('**/api/properties/hidden', async (route) => {
      hiddenFetchCount += 1
      if (isHiddenMerged) {
        const mergedHidden: IngatlanItem = {
          ...hidden1,
          manuallyMerged: true,
          sourcesCount: 2,
          sourceListings: [...(hidden1.sourceListings ?? []), ...(hidden2.sourceListings ?? [])],
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([mergedHidden]),
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([hidden1, hidden2]),
        })
      }
    })

    await page.route('**/api/dedup/merge', async (route) => {
      isHiddenMerged = true
      const mergedHidden: IngatlanItem = {
        ...hidden1,
        manuallyMerged: true,
        sourcesCount: 2,
        sourceListings: [...(hidden1.sourceListings ?? []), ...(hidden2.sourceListings ?? [])],
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [mergedHidden],
          overrides: [],
          message: 'Hirdetések sikeresen összevonva',
        }),
      })
    })

    await page.route('**/api/dedup/overrides**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    })

    await page.goto('/')
    await page.getByTestId('tab-archive').click()
    await page.getByTestId('archive-mode-hidden').click()

    await expect(page.getByTestId('archive-mode-hidden')).toContainText('2')
    await expect(page.getByTestId('property-card')).toHaveCount(2)

    const cards = page.getByTestId('property-card')
    await cards.nth(0).click({ modifiers: ['Control'] })
    await cards.nth(1).click({ modifiers: ['Control'] })
    await page.getByTestId('merge-selected-button').click()

    await expect.poll(() => hiddenFetchCount).toBeGreaterThanOrEqual(2)
    await expect(page.getByTestId('property-card')).toHaveCount(1)
    await expect(page.getByTestId('archive-mode-hidden')).toContainText('1')
    await expect(page.getByTestId('sources-count-badge')).toContainText('2')

    // Page reload persists merged trash state
    await page.reload()
    await page.getByTestId('tab-archive').click()
    await page.getByTestId('archive-mode-hidden').click()
    await expect(page.getByTestId('property-card')).toHaveCount(1)
    await expect(page.getByTestId('archive-mode-hidden')).toContainText('1')
  })
})
