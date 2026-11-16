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
})
