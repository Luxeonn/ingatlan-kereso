import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_MAX_PAGES,
  DEFAULT_TIMEOUT_MS,
  buildSearchUrl,
  hasPaginationWithoutNextPage,
  parseArgs,
  resolveChromiumExecutablePath,
  scrapePages,
} from './sidecar-playwright.mjs'

function makeItems(pageNum, count) {
  const startId = (pageNum - 1) * 20 + 1
  return Array.from({ length: count }, (_, idx) => {
    const id = String(1000000 + startId + idx)
    return {
      id,
      url: `https://ingatlan.com/${id}`,
      title: `Hajdúszoboszló ház ${id}`,
      priceMillion: 80 + idx * 0.5,
      priceRaw: `${80 + idx * 0.5} M Ft`,
      areaSquareMeters: 120,
      roomCount: 4,
      imageUrl: null,
    }
  })
}

test('parseArgs uses unified defaults and parses pagination flags', () => {
  const defaults = parseArgs(['node', 'sidecar-playwright.mjs'])
  assert.equal(defaults.maxPages, DEFAULT_MAX_PAGES)
  assert.equal(defaults.maxPages, 20)
  assert.equal(defaults.timeoutMs, DEFAULT_TIMEOUT_MS)
  assert.equal(defaults.timeoutMs, 125000)
  const exePath = resolveChromiumExecutablePath()
  assert.ok(exePath === undefined || typeof exePath === 'string')

  const custom = parseArgs([
    'node',
    'sidecar-playwright.mjs',
    '--city',
    'Hajdúszoboszló',
    '--type',
    'haz',
    '--min-price',
    '60',
    '--max-price',
    '130',
    '--min-area',
    '95',
    '--max-pages',
    '15',
    '--timeout-ms',
    '50000',
  ])
  assert.equal(custom.maxPages, 15)
  assert.equal(custom.timeoutMs, 50000)
})

test('buildSearchUrl uses ?page=N query pagination without sort-order segments', () => {
  const args = {
    city: 'Hajdúszoboszló',
    type: 'haz',
    minPrice: 60,
    maxPrice: 130,
    minArea: 95,
    maxArea: 200,
  }
  const base = 'https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo'
  assert.equal(buildSearchUrl(args, 1), base)
  assert.equal(buildSearchUrl(args, 2), `${base}?page=2`)
  assert.equal(buildSearchUrl(args, 6), `${base}?page=6`)
  assert.ok(!buildSearchUrl(args, 2).includes('ar-szerint'))
  assert.ok(!buildSearchUrl(args, 3).includes('m2-ar-szerint'))
})

test('hasPaginationWithoutNextPage detects final page accurately', () => {
  assert.equal(hasPaginationWithoutNextPage(['/lista/x?page=2'], 1, 20), false)
  assert.equal(hasPaginationWithoutNextPage(['/lista/x?page=5'], 6, 2), true)
  assert.equal(hasPaginationWithoutNextPage([], 1, 8), true)
  assert.equal(hasPaginationWithoutNextPage([], 1, 20), false)
})

test('scrapePages fetches all 6 pages (102 listings) and stops with NO_MORE_PAGES', async () => {
  const requestedUrls = []
  const args = {
    portal: 'ingatlancom',
    city: 'Hajdúszoboszló',
    type: 'haz',
    minPrice: 60,
    maxPrice: 130,
    minArea: 95,
    maxArea: 200,
    maxPages: 20,
    timeoutMs: 85000,
  }

  const result = await scrapePages(
    args,
    async (url, pageNum) => {
      requestedUrls.push(url)
      if (pageNum < 6) {
        return {
          status: 200,
          items: makeItems(pageNum, 20),
          pageLinks: [`/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo?page=${pageNum + 1}`],
        }
      }
      if (pageNum === 6) {
        return {
          status: 200,
          items: makeItems(6, 2),
          pageLinks: ['/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo?page=5'],
        }
      }
      throw new Error(`Unexpected page requested: ${pageNum}`)
    },
    { delayMs: 0 }
  )

  assert.equal(result.status, 'SUCCESS')
  assert.equal(result.items.length, 102)
  assert.equal(result.pagesProcessed, 6)
  assert.equal(result.stopReason, 'NO_MORE_PAGES')
  assert.equal(requestedUrls.length, 6)
  assert.ok(requestedUrls[1].endsWith('?page=2'))
  assert.ok(requestedUrls[5].endsWith('?page=6'))
})

test('scrapePages preserves partial items on intermediate 429 rate limit', async () => {
  const args = {
    portal: 'ingatlancom',
    city: 'Hajdúszoboszló',
    type: 'haz',
    maxPages: 20,
    timeoutMs: 85000,
  }

  const result = await scrapePages(
    args,
    async (_url, pageNum) => {
      if (pageNum <= 2) {
        return {
          status: 200,
          items: makeItems(pageNum, 20),
          pageLinks: [`/lista/x?page=${pageNum + 1}`],
        }
      }
      return { status: 429, items: [], pageLinks: [] }
    },
    { delayMs: 0 }
  )

  assert.equal(result.status, 'SUCCESS')
  assert.equal(result.items.length, 40)
  assert.equal(result.pagesProcessed, 2)
  assert.equal(result.stopReason, 'INTERMEDIATE_PAGE_RATE_LIMITED')
})

test('scrapePages stops on repeated page content and respects time budget', async () => {
  const repeated = await scrapePages(
    { portal: 'ingatlancom', city: 'Debrecen', type: 'lakas', maxPages: 20, timeoutMs: 85000 },
    async () => ({
      status: 200,
      items: makeItems(1, 20),
      pageLinks: ['/lista/x?page=2'],
    }),
    { delayMs: 0 }
  )
  assert.equal(repeated.status, 'SUCCESS')
  assert.equal(repeated.items.length, 20)
  assert.equal(repeated.stopReason, 'REPEATED_PAGE_CONTENT')

  let tick = 0
  const timedOut = await scrapePages(
    { portal: 'ingatlancom', city: 'Debrecen', type: 'lakas', maxPages: 20, timeoutMs: 5000 },
    async (_url, pageNum) => ({
      status: 200,
      items: makeItems(pageNum, 20),
      pageLinks: [`/lista/x?page=${pageNum + 1}`],
    }),
    {
      nowFn: () => {
        const current = tick
        tick += 6000
        return current
      },
      delayMs: 0,
    }
  )
  assert.equal(timedOut.status, 'SUCCESS')
  assert.equal(timedOut.items.length, 20)
  assert.equal(timedOut.stopReason, 'TIME_BUDGET_REACHED')
})
