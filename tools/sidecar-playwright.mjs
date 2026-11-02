#!/usr/bin/env node
/**
 * Local Node.js + Playwright (--headless=new) fallback sidecar for Ingatlan.com
 * Used automatically by SidecarCliExecutor when Python + curl_cffi is not installed locally.
 */

import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const BASE_URL = 'https://ingatlan.com'

function parseArgs(argv) {
  const args = {
    portal: 'ingatlancom',
    city: 'debrecen',
    type: 'lakas',
    minPrice: null,
    maxPrice: null,
    minArea: null,
    maxArea: null,
    maxPages: 1,
    help: false,
  }
  for (let i = 2; i < argv.length; i++) {
    const arg = argv[i]
    if (arg === '--help' || arg === '-h') args.help = true
    else if (arg === '--portal' && argv[i + 1]) args.portal = argv[++i]
    else if (arg === '--city' && argv[i + 1]) args.city = argv[++i]
    else if (arg === '--type' && argv[i + 1]) args.type = argv[++i]
    else if (arg === '--min-price' && argv[i + 1]) args.minPrice = Number.parseInt(argv[++i], 10)
    else if (arg === '--max-price' && argv[i + 1]) args.maxPrice = Number.parseInt(argv[++i], 10)
    else if (arg === '--min-area' && argv[i + 1]) args.minArea = Number.parseInt(argv[++i], 10)
    else if (arg === '--max-area' && argv[i + 1]) args.maxArea = Number.parseInt(argv[++i], 10)
    else if (arg === '--max-pages' && argv[i + 1]) args.maxPages = Number.parseInt(argv[++i], 10)
  }
  return args
}

function toSlug(text) {
  if (!text) return 'debrecen'
  return text
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, '-')
}

function buildRangeSlug(minVal, maxVal, unit) {
  const min = Number.isFinite(minVal) && minVal > 0 ? minVal : null
  const max = Number.isFinite(maxVal) && maxVal > 0 && maxVal < 9999 ? maxVal : null
  if (min !== null && max !== null) return `${min}-${max}-${unit}`
  if (min !== null) return `${min}-${unit}-tol`
  if (max !== null) return `${max}-${unit}-ig`
  return ''
}

function buildSearchUrl(args, page = 1) {
  const citySlug = toSlug(args.city) || 'debrecen'
  const typeMap = {
    haz: 'haz',
    garazs: 'garazs',
    telek: 'telek',
    nyaralo: 'nyaralo',
    iroda: 'iroda',
    osszes: 'ingatlan',
  }
  const typeSlug = typeMap[(args.type || 'lakas').toLowerCase().trim()] || 'lakas'
  const segments = ['elado', typeSlug]

  const areaSlug = buildRangeSlug(args.minArea, args.maxArea, 'm2')
  if (areaSlug) segments.push(areaSlug)

  const priceSlug = buildRangeSlug(args.minPrice, args.maxPrice, 'mFt')
  if (priceSlug) segments.push(priceSlug)

  segments.push(citySlug)
  if (page === 2) segments.push('ar-szerint')
  else if (page >= 3) segments.push('m2-ar-szerint')
  return `${BASE_URL}/lista/${segments.join('+')}`
}

async function loadPlaywright() {
  const playwrightPath = path.resolve(__dirname, '..', 'frontend', 'node_modules', 'playwright', 'index.mjs')
  return import(pathToFileURL(playwrightPath).href)
}

async function main() {
  const args = parseArgs(process.argv)
  if (args.help) {
    console.log('Usage: node sidecar-playwright.mjs --portal ingatlancom --city <city> --type <type>')
    process.exit(0)
  }

  const startMs = Date.now()
  let browser = null
  try {
    const { chromium } = await loadPlaywright()
    browser = await chromium.launch({
      headless: false,
      args: ['--disable-blink-features=AutomationControlled', '--headless=new'],
    })
    const context = await browser.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36',
      locale: 'hu-HU',
    })
    const page = await context.newPage()
    await page.addInitScript(() => {
      Object.defineProperty(navigator, 'webdriver', { get: () => undefined })
    })

    const allItems = []
    const seenUrls = new Set()
    const maxPages = Math.max(1, Math.min(args.maxPages || 2, 5))

    for (let pageNum = 1; pageNum <= maxPages; pageNum++) {
      const targetUrl = buildSearchUrl(args, pageNum)
      const response = await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 15000 })
      if (!response || response.status() !== 200) {
        if (pageNum === 1) {
          const status = response ? response.status() : 0
          console.log(
            JSON.stringify({
              portalId: args.portal,
              status: 'FAILED',
              items: [],
              errorMessage: `HTTP ${status} hiba`,
              elapsedMs: Date.now() - startMs,
            })
          )
          return
        }
        break
      }

      const pageItems = await page.evaluate(
        ({ baseUrl, defaultCity }) => {
          const cards = Array.from(
            document.querySelectorAll("a.listing-card, a[data-listing-id], div.listing-card, a[href*='/ingatlan/']")
          )
          return cards
            .map((card) => {
              const href =
                card.tagName === 'A'
                  ? card.getAttribute('href')
                  : card.querySelector('a[href]')?.getAttribute('href')
              if (!href) return null
              const fullUrl = href.startsWith('http')
                ? href
                : `${baseUrl}${href.startsWith('/') ? '' : '/'}${href}`
              let id = card.getAttribute('data-listing-id') || card.getAttribute('data-id') || ''
              if (!id) {
                const m = fullUrl.match(/\/(?:ingatlan\/)?(\d{6,12})(?:[/?#]|$)/)
                if (m) id = m[1]
              }
              if (!id) return null

              const cardText = (card.innerText || card.textContent || '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim()
              const priceEl = card.querySelector('.fw-bold.fs-5, .price, .listing-card__price')
              const priceTextSource = (priceEl ? priceEl.textContent : cardText).replace(/\u00a0/g, ' ').trim()

              let priceMillion = null
              let priceRaw = ''
              const mMillion = priceTextSource.match(/(\d[\d\s,.]*)\s*M\s*Ft/i) || cardText.match(/(\d[\d\s,.]*)\s*M\s*Ft/i)
              if (mMillion) {
                priceRaw = mMillion[0].trim()
                const parsed = Number.parseFloat(mMillion[1].replace(/\s+/g, '').replace(',', '.'))
                if (Number.isFinite(parsed)) priceMillion = parsed
              }

              const addrEl = card.querySelector('span.d-block.fs-7.text-gray-900, .address, .listing-card__title')
              const addrText = addrEl ? addrEl.textContent.trim() : ''
              const newLabel = card.querySelector("[data-slug='uj-epitesu'], .listing-label")
              const isNew = newLabel && newLabel.textContent.toLowerCase().includes('új építésű')
              const title = addrText
                ? `${addrText}${isNew ? ' [Új építésű]' : ''}`
                : cardText.length > 5
                ? `${cardText.slice(0, 65).trim()}...`
                : `${defaultCity} - Ingatlan (${id})`

              let areaSquareMeters = null
              const mArea =
                cardText.match(/Alapterület\s*(\d[\d,.\s]*)\s*m[²2]?/i) ||
                cardText.match(/(?<!\/\s)(\d[\d,.\s]*)\s*m[²2]/i)
              if (mArea) {
                const parsedArea = Number.parseFloat(mArea[1].replace(/\s+/g, '').replace(',', '.'))
                if (Number.isFinite(parsedArea)) areaSquareMeters = parsedArea
              }

              let roomCount = null
              const mRoomsHalf = cardText.match(/Szobák\s*(\d+)\s*\+\s*(\d+)/i) || cardText.match(/(\d+)\s*\+\s*(\d+)\s*(?:fél\s*)?szob/i)
              const mRoomsSimple = cardText.match(/Szobák\s*(\d+(?:[.,]\d+)?)/i) || cardText.match(/(\d+(?:[.,]\d+)?)\s*szob/i)
              if (mRoomsHalf) {
                roomCount = Number.parseFloat(mRoomsHalf[1]) + 0.5 * Number.parseFloat(mRoomsHalf[2])
              } else if (mRoomsSimple) {
                roomCount = Number.parseFloat(mRoomsSimple[1].replace(',', '.'))
              }

              const imgEl = card.querySelector("img.listing-card-image, img[src*='ingatlancdn.com/resize']")
              const imgSrc = imgEl ? imgEl.getAttribute('src') || imgEl.getAttribute('data-src') : null
              const imageUrl =
                imgSrc && imgSrc.startsWith('http') && !imgSrc.includes('listing-image-error') ? imgSrc : null

              return {
                id,
                url: fullUrl,
                title,
                priceMillion,
                priceRaw,
                areaSquareMeters,
                roomCount,
                imageUrl,
              }
            })
            .filter(Boolean)
        },
        { baseUrl: BASE_URL, defaultCity: args.city }
      )

      if (!pageItems.length) break
      for (const item of pageItems) {
        if (!seenUrls.has(item.url)) {
          seenUrls.add(item.url)
          allItems.push(item)
        }
      }
      if (pageItems.length < 5 && pageNum === 1) break
    }

    console.log(
      JSON.stringify({
        portalId: args.portal,
        status: 'SUCCESS',
        items: allItems,
        errorMessage: null,
        elapsedMs: Date.now() - startMs,
      })
    )
  } catch (err) {
    console.log(
      JSON.stringify({
        portalId: args.portal,
        status: 'FAILED',
        items: [],
        errorMessage: err instanceof Error ? err.message : String(err),
        elapsedMs: Date.now() - startMs,
      })
    )
  } finally {
    if (browser) {
      await browser.close().catch(() => {})
    }
  }
}

main()
