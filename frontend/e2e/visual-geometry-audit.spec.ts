import { test, expect } from '@playwright/test'
import type { Locator, Page } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import {
  setupStandardApiMocks,
  createMockSearchResponse,
  SAMPLE_PROPERTIES,
} from './helpers/mockApi'
import type { IngatlanItem } from '../src/types/property'
import type { MarketSegmentStatsDto } from '../src/types/api'

interface RectBox {
  x: number
  y: number
  width: number
  height: number
}

interface ViewportSpec {
  width: number
  height: number
}

const DESKTOP_VIEWPORTS: readonly ViewportSpec[] = [
  { width: 1280, height: 800 },
  { width: 1440, height: 900 },
  { width: 1920, height: 1080 },
]

const SCREENSHOT_DIR = path.resolve(process.cwd(), 'test-results', 'visual-audit')

let pageErrors: Error[] = []
let consoleErrors: string[] = []

function buildRichDataset(): IngatlanItem[] {
  const base = SAMPLE_PROPERTIES[0]!
  return [
    {
      ...base,
      id: 'rich-1',
      portal: 'Duna House',
      url: 'https://dh.hu/ingatlan/rich-1',
      title: 'Nagyerdő panorámás felújított lakás',
      price: '48 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Nagyerdő',
      propertyType: 'LAKAS',
      isNew: true,
      isBargain: true,
      percentageBelowMedian: 24,
      referenceSampleSize: 8,
      referenceMedianPricePerSquareMeter: 1_125_000,
      priceDelta: -4_000_000,
      priceChangePercent: -7.7,
      imageUrl: 'https://images.example.invalid/rich-1.jpg',
      sourcesCount: 2,
      sourceListings: [
        { portal: 'Duna House', url: 'https://dh.hu/ingatlan/rich-1', priceHuf: 48_000_000, status: 'ACTIVE' },
        { portal: 'Otthon Centrum', url: 'https://oc.hu/ingatlan/rich-1-oc', priceHuf: 51_000_000, status: 'ACTIVE' },
      ],
      priceInfo: { amountMillion: 48, amountHuf: 48_000_000, rawText: '48 M Ft' },
      detailsInfo: { areaSquareMeters: 56, roomCount: 2, pricePerSquareMeter: 857_143 },
    },
    {
      ...base,
      id: 'rich-2',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/ingatlan/rich-2',
      title: 'Belvárosi tégla építésű világos lakás',
      price: '62 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Belváros',
      propertyType: 'LAKAS',
      isNew: false,
      isBargain: false,
      priceDelta: -3_000_000,
      priceChangePercent: -4.6,
      imageUrl: 'https://images.example.invalid/rich-2.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 62, amountHuf: 62_000_000, rawText: '62 M Ft' },
      detailsInfo: { areaSquareMeters: 68, roomCount: 3, pricePerSquareMeter: 911_765 },
    },
    {
      ...base,
      id: 'rich-3',
      portal: 'Jófogás',
      url: 'https://jofogas.hu/ingatlan/rich-3',
      title: 'Egyetemvárosi csendes tégla lakás erkéllyel',
      price: '74 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Egyetemváros',
      propertyType: 'LAKAS',
      isNew: true,
      isBargain: false,
      newConstruction: true,
      imageUrl: 'https://images.example.invalid/rich-3.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 74, amountHuf: 74_000_000, rawText: '74 M Ft' },
      detailsInfo: { areaSquareMeters: 74, roomCount: 3, pricePerSquareMeter: 1_000_000 },
    },
    {
      ...base,
      id: 'rich-4',
      portal: 'Költözzbe',
      url: 'https://koltozzbe.hu/ingatlan/rich-4',
      title: 'Tócóskerti napfényes erkélyes lakás',
      price: '54 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Tócóskert',
      propertyType: 'LAKAS',
      isNew: false,
      isBargain: true,
      percentageBelowMedian: 21,
      referenceSampleSize: 8,
      referenceMedianPricePerSquareMeter: 1_068_000,
      imageUrl: '',
      sourcesCount: 1,
      priceInfo: { amountMillion: 54, amountHuf: 54_000_000, rawText: '54 M Ft' },
      detailsInfo: { areaSquareMeters: 64, roomCount: 2, pricePerSquareMeter: 843_750 },
    },
    {
      ...base,
      id: 'rich-5',
      portal: 'Ingatlanbazár',
      url: 'https://ingatlanbazar.hu/ingatlan/rich-5',
      title: 'Sestakerti parkra néző modern otthon',
      price: '88 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Sestakert',
      propertyType: 'LAKAS',
      isNew: true,
      isBargain: false,
      imageUrl: 'https://images.example.invalid/rich-5.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 88, amountHuf: 88_000_000, rawText: '88 M Ft' },
      detailsInfo: { areaSquareMeters: 82, roomCount: 3, pricePerSquareMeter: 1_073_171 },
    },
    {
      ...base,
      id: 'rich-6',
      portal: 'Duna House',
      url: 'https://dh.hu/ingatlan/rich-6',
      title: 'Szegedi Tisza-parti tégla lakás garázzsal',
      price: '69 M Ft',
      city: 'Szeged',
      location: 'Szeged, Belváros',
      propertyType: 'LAKAS',
      isNew: false,
      isBargain: true,
      percentageBelowMedian: 22,
      referenceSampleSize: 8,
      referenceMedianPricePerSquareMeter: 1_134_000,
      priceDelta: -6_000_000,
      priceChangePercent: -8.0,
      imageUrl: 'https://images.example.invalid/rich-6.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 69, amountHuf: 69_000_000, rawText: '69 M Ft' },
      detailsInfo: { areaSquareMeters: 78, roomCount: 3, pricePerSquareMeter: 884_615 },
    },
    {
      ...base,
      id: 'rich-7',
      portal: 'Otthon Centrum',
      url: 'https://oc.hu/ingatlan/rich-7',
      title: 'Szegedi újszegedi kertkapcsolatos otthon',
      price: '96 M Ft',
      city: 'Szeged',
      location: 'Szeged, Újszeged',
      propertyType: 'LAKAS',
      isNew: true,
      isBargain: false,
      imageUrl: 'https://images.example.invalid/rich-7.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 96, amountHuf: 96_000_000, rawText: '96 M Ft' },
      detailsInfo: { areaSquareMeters: 92, roomCount: 4, pricePerSquareMeter: 1_043_478 },
    },
    {
      ...base,
      id: 'rich-8',
      portal: 'Ingatlan.com',
      url: 'https://ingatlan.com/rich-8',
      title: 'Péterfia utcai polgári galériás lakás',
      price: '79 M Ft',
      city: 'Debrecen',
      location: 'Debrecen, Belváros',
      propertyType: 'LAKAS',
      isNew: false,
      isBargain: false,
      imageUrl: 'https://images.example.invalid/rich-8.jpg',
      sourcesCount: 1,
      priceInfo: { amountMillion: 79, amountHuf: 79_000_000, rawText: '79 M Ft' },
      detailsInfo: { areaSquareMeters: 80, roomCount: 3, pricePerSquareMeter: 987_500 },
    },
  ]
}

function buildGeometryDataset(): IngatlanItem[] {
  const baseItems = buildRichDataset()
  const extraItems: IngatlanItem[] = Array.from({ length: 10 }, (_, idx) => {
    const template = baseItems[idx % baseItems.length]!
    const n = idx + 9
    return {
      ...template,
      id: `rich-${n}`,
      url: `${template.url}-extra-${n}`,
      title: `${template.title} (#${n})`,
      sourcesCount: 1,
      sourceListings: undefined,
    }
  })
  return [...baseItems, ...extraItems]
}

const RICH_SEGMENTS: MarketSegmentStatsDto[] = [
  {
    city: 'Debrecen',
    propertyType: 'LAKAS',
    propertyTypeName: 'Lakás',
    residential: true,
    plot: false,
    sampleSize: 6,
    insufficientData: false,
    medianPricePerM2: 950_000,
    averagePricePerM2: 962_138,
    medianPriceMillion: 68,
    averagePriceMillion: 67.5,
    minPriceMillion: 48,
    maxPriceMillion: 88,
    bargainCount: 2,
  },
  {
    city: 'Szeged',
    propertyType: 'LAKAS',
    propertyTypeName: 'Lakás',
    residential: true,
    plot: false,
    sampleSize: 2,
    insufficientData: true,
    medianPricePerM2: 964_046,
    averagePricePerM2: 964_046,
    medianPriceMillion: 82.5,
    averagePriceMillion: 82.5,
    minPriceMillion: 69,
    maxPriceMillion: 96,
    bargainCount: 1,
  },
]

function rectsOverlap(a: RectBox, b: RectBox): boolean {
  const tolerance = 1
  const separatedX = a.x + a.width <= b.x + tolerance || b.x + b.width <= a.x + tolerance
  const separatedY = a.y + a.height <= b.y + tolerance || b.y + b.height <= a.y + tolerance
  return !(separatedX || separatedY)
}

function assertZeroRectOverlap(a: RectBox, b: RectBox, labelA: string, labelB: string): void {
  expect(
    rectsOverlap(a, b),
    `Expected 0px overlap between ${labelA} (${JSON.stringify(a)}) and ${labelB} (${JSON.stringify(b)})`,
  ).toBe(false)
}

function assertInsideViewport(rect: RectBox, viewport: ViewportSpec, label: string): void {
  expect(rect.x, `${label} left edge out of viewport`).toBeGreaterThanOrEqual(-1)
  expect(rect.y, `${label} top edge out of viewport`).toBeGreaterThanOrEqual(-1)
  expect(rect.x + rect.width, `${label} right edge overflows viewport`).toBeLessThanOrEqual(
    viewport.width + 1,
  )
  expect(rect.y + rect.height, `${label} bottom edge overflows viewport`).toBeLessThanOrEqual(
    viewport.height + 1,
  )
}

async function assertNoHorizontalOverflow(page: Page, contextLabel: string): Promise<void> {
  const dims = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    innerWidth: window.innerWidth,
  }))
  expect(
    dims.scrollWidth,
    `Horizontal overflow detected in ${contextLabel}: scrollWidth=${dims.scrollWidth} > innerWidth=${dims.innerWidth}`,
  ).toBeLessThanOrEqual(dims.innerWidth)
}

async function collectPairwiseNonOverlappingBoxes(
  locator: Locator,
  maxItems: number,
  groupName: string,
): Promise<void> {
  const count = Math.min(await locator.count(), maxItems)
  const boxes: Array<{ idx: number; box: RectBox }> = []
  for (let i = 0; i < count; i += 1) {
    const item = locator.nth(i)
    if (await item.isVisible()) {
      const box = await item.boundingBox()
      if (box) {
        boxes.push({ idx: i, box })
      }
    }
  }
  for (let i = 0; i < boxes.length; i += 1) {
    for (let j = i + 1; j < boxes.length; j += 1) {
      const a = boxes[i]
      const b = boxes[j]
      if (a && b) {
        assertZeroRectOverlap(a.box, b.box, `${groupName}[${a.idx}]`, `${groupName}[${b.idx}]`)
      }
    }
  }
}

function assertPairwiseNamedBoxes(entries: Array<{ label: string; box: RectBox | null }>): void {
  const present = entries.filter((entry): entry is { label: string; box: RectBox } => entry.box !== null)
  for (let i = 0; i < present.length; i += 1) {
    for (let j = i + 1; j < present.length; j += 1) {
      assertZeroRectOverlap(present[i]!.box, present[j]!.box, present[i]!.label, present[j]!.label)
    }
  }
}

async function assertBackdropBlurEnabled(locator: Locator, label: string): Promise<void> {
  await expect(locator).toBeVisible()
  const hasBlur = await locator.evaluate((el) => {
    const style = getComputedStyle(el)
    const standard = style.getPropertyValue('backdrop-filter')
    const webkit = style.getPropertyValue('-webkit-backdrop-filter')
    return (standard !== '' && standard !== 'none') || (webkit !== '' && webkit !== 'none')
  })
  expect(hasBlur, `Expected Liquid Glass backdrop-filter on ${label}`).toBe(true)
}

async function assertFallbackAndImageAssets(page: Page): Promise<void> {
  const fallbackSvgs = page.locator('[data-testid="property-image-fallback"] svg')
  expect(await fallbackSvgs.count()).toBeGreaterThan(0)
  await expect(fallbackSvgs.first()).toBeVisible()

  const loadedImgs = page.locator('[data-testid="property-thumbnail"] img')
  const imgCount = Math.min(await loadedImgs.count(), 4)
  expect(imgCount).toBeGreaterThan(0)
  for (let i = 0; i < imgCount; i += 1) {
    const ok = await loadedImgs.nth(i).evaluate((node) => {
      const img = node as HTMLImageElement
      return img.complete && img.naturalWidth > 0
    })
    expect(ok, `Mocked thumbnail img[${i}] failed to load cleanly`).toBe(true)
  }
}

async function assertLightThemeAndLiquidGlassRubric(page: Page): Promise<void> {
  await expect(page.locator('html')).not.toHaveClass(/dark/)

  const luminance = await page.evaluate(() => {
    const bg = getComputedStyle(document.body).backgroundColor
    const match = bg.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
    if (!match) return 1
    const r = Number(match[1]) / 255
    const g = Number(match[2]) / 255
    const b = Number(match[3]) / 255
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  })
  expect(luminance, 'Body canvas must remain light-theme').toBeGreaterThan(0.7)

  await expect(page.locator('.pulse-title')).toHaveCount(0)

  await assertBackdropBlurEnabled(page.locator('header').first(), 'TopNavBar')
  await assertBackdropBlurEnabled(page.getByTestId('search-panel-container'), 'FilterSidebar')
  await assertBackdropBlurEnabled(page.getByTestId('property-card').first(), 'PropertyCard')

  const tabularCount = await page.locator('.tabular-nums').count()
  expect(tabularCount).toBeGreaterThan(0)
  const firstTabularNumeric = await page.locator('.tabular-nums').first().evaluate((el) => {
    return getComputedStyle(el).fontVariantNumeric
  })
  expect(firstTabularNumeric).toContain('tabular-nums')

  const cards = page.getByTestId('property-card')
  const cardCount = Math.min(await cards.count(), 4)
  for (let i = 0; i < cardCount; i += 1) {
    const card = cards.nth(i)
    const topBadgeCount = await card.evaluate((cardEl) => {
      const thumb = cardEl.querySelector('[data-testid="property-thumbnail"]')
      if (!thumb) return 0
      const thumbRect = thumb.getBoundingClientRect()
      const fallbackContainer = thumb.querySelector('[data-testid="property-image-fallback"]')
      const badgeCandidates = Array.from(
        thumb.querySelectorAll(
          '[data-testid="bargain-badge-button"], [data-testid="sources-count-badge"], [data-testid="new-construction-badge"], span, button',
        ),
      )
      const topBadges = badgeCandidates.filter((node) => {
        if (node.getAttribute('data-testid') === 'property-portal-badge') return false
        if (fallbackContainer && fallbackContainer.contains(node)) return false
        const rect = node.getBoundingClientRect()
        const isTopHalf = rect.top < thumbRect.top + thumbRect.height / 2
        const isSmallCapsule =
          rect.height > 10 && rect.height <= 40 && rect.width > 12 && rect.width < thumbRect.width
        if (!isTopHalf || !isSmallCapsule) return false
        const hasBadgeAncestor = badgeCandidates.some(
          (other) => other !== node && other.contains(node) && other.getBoundingClientRect().height <= 40,
        )
        return !hasBadgeAncestor
      })
      return topBadges.length
    })
    expect(topBadgeCount, `Card ${i} exceeded max 2 top glass capsule badges`).toBeLessThanOrEqual(2)
  }
}

async function maybeCaptureAuditScreenshot(page: Page, filename: string): Promise<void> {
  if (process.env['VISUAL_AUDIT_SCREENSHOTS'] !== '1') return
  await mkdir(SCREENSHOT_DIR, { recursive: true })
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, filename), fullPage: false })
}

async function assertAnalyticsGeometryAndGlass(page: Page, vp: ViewportSpec): Promise<void> {
  const panel = page.getByTestId('market-analytics-panel')
  await expect(panel).toBeVisible()
  await assertNoHorizontalOverflow(page, `${vp.width}x${vp.height} analytics`)

  const analyticsBlocks = [
    { label: 'kpi-card-lakas', box: await panel.getByTestId('kpi-card-lakas').boundingBox() },
    { label: 'kpi-card-avg-price', box: await panel.getByTestId('kpi-card-avg-price').boundingBox() },
    { label: 'kpi-card-total-samples', box: await panel.getByTestId('kpi-card-total-samples').boundingBox() },
    { label: 'kpi-card-price-drops', box: await panel.getByTestId('kpi-card-price-drops').boundingBox() },
    { label: 'market-segment-matrix-table', box: await panel.getByTestId('market-segment-matrix-table').boundingBox() },
    { label: 'top-price-drops-section', box: await panel.getByTestId('top-price-drops-section').boundingBox() },
  ]
  const chartCount = await panel.locator('.recharts-responsive-container').count()
  for (let i = 0; i < chartCount; i += 1) {
    const chartBox = await panel.locator('.recharts-responsive-container').nth(i).boundingBox()
    analyticsBlocks.push({ label: `analytics-chart-${i}`, box: chartBox })
  }
  assertPairwiseNamedBoxes(analyticsBlocks)

  await assertBackdropBlurEnabled(panel.getByTestId('kpi-card-lakas'), 'AnalyticsKpiCard')
  await assertBackdropBlurEnabled(panel.getByTestId('market-segment-matrix-table'), 'SegmentMatrixTable')

  const analyticsTabularCount = await panel.locator('.tabular-nums').count()
  expect(analyticsTabularCount).toBeGreaterThan(0)
}

async function runDesktopGeometryPass(page: Page, vp: ViewportSpec): Promise<void> {
  await page.setViewportSize(vp)
  await page.goto('/')

  await expect(page.getByTestId('header-db-telemetry-pill')).toContainText('DB')
  await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
  await expect(page.getByTestId('property-card').first()).toBeVisible()

  await assertNoHorizontalOverflow(page, `${vp.width}x${vp.height} sidebar-open`)

  const bulkActionBar = page.locator('main div:has(> [data-testid="filter-chips-row"])').first()
  const headerBox = await page.locator('header').first().boundingBox()
  const sidebarBox = await page.getByTestId('search-panel-container').boundingBox()
  const statsBox = await page.getByTestId('market-stats-bar').boundingBox()
  const bulkBox = await bulkActionBar.boundingBox()
  const mainBoxOpen = await page.locator('main').boundingBox()
  const firstCardBox = await page.getByTestId('property-card').first().boundingBox()

  expect(headerBox).not.toBeNull()
  expect(sidebarBox).not.toBeNull()
  expect(mainBoxOpen).not.toBeNull()
  expect(firstCardBox).not.toBeNull()

  assertPairwiseNamedBoxes([
    { label: 'TopNavBar', box: headerBox },
    { label: 'FilterSidebar', box: sidebarBox },
    { label: 'MainWorkspace', box: mainBoxOpen },
  ])
  assertPairwiseNamedBoxes([
    { label: 'TopNavBar', box: headerBox },
    { label: 'MarketStatsBar', box: statsBox },
    { label: 'BulkActionBar', box: bulkBox },
    { label: 'FirstPropertyCard', box: firstCardBox },
  ])

  await collectPairwiseNonOverlappingBoxes(page.getByTestId('property-card'), 9, 'PropertyCard')
  await assertLightThemeAndLiquidGlassRubric(page)
  await assertFallbackAndImageAssets(page)
  await maybeCaptureAuditScreenshot(page, `desktop-${vp.width}x${vp.height}-open.png`)

  const sidebarToggleBtn = page.getByTestId('sidebar-toggle-btn')
  await expect(sidebarToggleBtn).toHaveAttribute('aria-expanded', 'true')
  await sidebarToggleBtn.click()
  await expect(sidebarToggleBtn).toHaveAttribute('aria-expanded', 'false')
  const collapsedSummary = page.getByTestId('collapsed-filter-summary')
  await expect(collapsedSummary).toBeVisible()
  await expect(collapsedSummary.locator('div > button')).toHaveCount(5)
  await assertNoHorizontalOverflow(page, `${vp.width}x${vp.height} sidebar-collapsed`)

  const collapsedSummaryBox = await collapsedSummary.boundingBox()
  const headerBoxCollapsed = await page.locator('header').first().boundingBox()
  const statsBoxCollapsed = await page.getByTestId('market-stats-bar').boundingBox()
  const bulkBoxCollapsed = await bulkActionBar.boundingBox()
  const firstCardBoxCollapsed = await page.getByTestId('property-card').first().boundingBox()
  assertPairwiseNamedBoxes([
    { label: 'TopNavBar', box: headerBoxCollapsed },
    { label: 'CollapsedFilterSummary', box: collapsedSummaryBox },
    { label: 'MarketStatsBar', box: statsBoxCollapsed },
    { label: 'BulkActionBar', box: bulkBoxCollapsed },
    { label: 'FirstPropertyCardCollapsed', box: firstCardBoxCollapsed },
  ])

  const mainBoxCollapsed = await page.locator('main').boundingBox()
  expect(mainBoxCollapsed).not.toBeNull()
  if (mainBoxOpen && mainBoxCollapsed) {
    expect(mainBoxCollapsed.width).toBeGreaterThan(mainBoxOpen.width + 150)
  }
  await collectPairwiseNonOverlappingBoxes(page.getByTestId('property-card'), 9, 'PropertyCardCollapsed')
  await maybeCaptureAuditScreenshot(page, `desktop-${vp.width}x${vp.height}-collapsed.png`)

  await page.evaluate(() => window.scrollTo(0, 900))
  const scrolledY = await page.evaluate(() => window.scrollY)
  expect(scrolledY, `Window failed to scroll at ${vp.width}x${vp.height} (scroll trap check)`).toBeGreaterThan(100)
  const stickyHeaderBox = await page.locator('header').first().boundingBox()
  expect(stickyHeaderBox).not.toBeNull()
  if (stickyHeaderBox) {
    expect(stickyHeaderBox.y).toBeLessThanOrEqual(2)
    expect(stickyHeaderBox.y).toBeGreaterThanOrEqual(-1)
  }
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.evaluate(
    () => new Promise<void>((resolve) => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))),
  )

  const cards = page.getByTestId('property-card')
  await cards.nth(0).click({ modifiers: ['Control'] })
  await cards.nth(1).click({ modifiers: ['Control'] })
  await cards.nth(2).getByTestId('hide-property-button').click()

  const selectionBar = page.getByTestId('selection-action-bar')
  const undoToast = page.getByTestId('undo-hide-toast')
  await expect(selectionBar).toBeVisible()
  await expect(undoToast).toBeVisible()
  await assertBackdropBlurEnabled(selectionBar, 'FloatingSelectionBar')
  await assertBackdropBlurEnabled(undoToast, 'GlobalUndoToast')

  const selectionBox = await selectionBar.boundingBox()
  const toastBox = await undoToast.boundingBox()
  const activeHeaderBox = await page.locator('header').first().boundingBox()
  if (selectionBox && toastBox && activeHeaderBox) {
    assertInsideViewport(selectionBox, vp, 'FloatingSelectionBar')
    assertInsideViewport(toastBox, vp, 'GlobalUndoToast')
    assertZeroRectOverlap(selectionBox, toastBox, 'FloatingSelectionBar', 'GlobalUndoToast')
    assertZeroRectOverlap(activeHeaderBox, selectionBox, 'TopNavBar', 'FloatingSelectionBar')
    assertZeroRectOverlap(activeHeaderBox, toastBox, 'TopNavBar', 'GlobalUndoToast')
  }

  await undoToast.getByRole('button', { name: /Visszavonás/i }).click()
  await page.getByTestId('clear-selection-button').click()

  await page.getByTestId('sources-count-badge').first().click()
  const drawerModal = page.getByTestId('property-details-modal')
  await expect(drawerModal).toBeVisible()
  const drawerBox = await drawerModal.boundingBox()
  if (drawerBox) {
    assertInsideViewport(drawerBox, vp, 'PropertyDetailsDrawerModal')
  }
  await assertNoHorizontalOverflow(page, `${vp.width}x${vp.height} drawer-open`)
  await page.getByTestId('property-details-modal-close').click()

  await page.getByTestId('tab-analytics').click()
  await assertAnalyticsGeometryAndGlass(page, vp)
  await maybeCaptureAuditScreenshot(page, `desktop-${vp.width}x${vp.height}-analytics.png`)

  if (vp.width === 1280) {
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.getByTestId('tab-listings').click()
    await page
      .getByTestId('collapsed-filter-summary')
      .getByRole('button', { name: /Szűrőpanel megnyitása|Szűrők módosítása|Kibontás/i })
      .click()
    await expect(page.getByTestId('sidebar-toggle-btn')).toHaveAttribute('aria-expanded', 'true')
    await expect(page.getByTestId('search-panel-container')).toBeVisible()
    await assertNoHorizontalOverflow(page, '1280x800 reduced-motion')
    await page.emulateMedia({ reducedMotion: 'no-preference' })
  }
}

test.describe('Ingatlan Kereső #535 - Opaque-Box Parity (Tiers 1-4) & Visual/Geometry Audit', () => {
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
    const fatalErrors = consoleErrors.filter((e) => !e.includes('favicon'))
    expect(fatalErrors, `Console errors:\n${fatalErrors.join('\n')}`).toHaveLength(0)
  })

  test('1. Tier 1 & 2 (F2/F3): Presets (Create/Rename/Apply/Delete), Multi-City Add/Remove, Steppers & Reset', async ({
    page,
  }) => {
    await setupStandardApiMocks(page)
    await page.goto('/')

    const searchPanel = page.getByTestId('search-panel-container')

    await page.getByTestId('save-preset-button').click()
    const presetInput = page.getByPlaceholder('Sablon neve')
    await presetInput.fill('   ')
    await page.getByTitle('Mentés sablonként').click()
    await expect(page.getByTestId('preset-error-banner')).toContainText('A sablon neve nem lehet üres.')

    await presetInput.fill('Debrecen Kedvenc')
    await page.getByTitle('Mentés sablonként').click()
    await expect(page.getByTestId('preset-success-banner')).toContainText('Debrecen Kedvenc')

    const presetChip = page.locator('[data-testid^="preset-chip-"]').first()
    await expect(presetChip).toBeVisible()

    await presetChip.getByTitle('Sablon átnevezése').click()
    const renameInput = page.getByTestId('search-presets-bar').locator('form input[type="text"]')
    await renameInput.fill('Debrecen Átnevezett')
    await page.getByTestId('search-presets-bar').getByTitle('Mentés').click()
    await expect(page.getByTestId('preset-success-banner')).toContainText('Debrecen Átnevezett')

    await presetChip.click()
    await expect(page.getByTestId('property-card').first()).toBeVisible()

    await presetChip.getByTitle('Sablon törlése').click()
    await expect(page.getByTestId('preset-success-banner')).toContainText('törölve')

    const cityInput = page.getByPlaceholder(/Válassz vagy írj be egy várost|További város hozzáadása/i)
    await cityInput.click()
    const cityDropdown = page.getByTestId('city-selector-dropdown')
    await expect(cityDropdown).toBeVisible()
    const dropdownBox = await cityDropdown.boundingBox()
    expect(dropdownBox).not.toBeNull()
    if (dropdownBox) {
      assertInsideViewport(dropdownBox, { width: 1280, height: 800 }, 'CitySelectorDropdown')
    }
    const cityDropdownOption = cityDropdown.getByRole('button', { name: /Budapest/i }).first()
    await expect(cityDropdownOption).toBeVisible()
    const optionBox = await cityDropdownOption.boundingBox()
    if (optionBox) {
      assertInsideViewport(optionBox, { width: 1280, height: 800 }, 'CitySelectorOption')
    }

    await cityInput.fill('Szeged')
    await cityInput.press('Enter')
    await expect(searchPanel.getByText('Szeged', { exact: true }).first()).toBeVisible()

    await searchPanel.locator('span').filter({ hasText: /^Szeged$/ }).locator('button').click()
    await expect(searchPanel.locator('span').filter({ hasText: /^Szeged$/ })).toHaveCount(0)

    const minPriceInput = searchPanel.locator('input[type="number"]').nth(0)
    await minPriceInput.fill('200')
    await expect(page.getByTestId('search-validation-warning')).toBeVisible()
    const searchBtn = page.getByRole('button', { name: /Ingatlanok Keresése/i })
    await expect(searchBtn).toBeDisabled()
    await minPriceInput.fill('20')
    await expect(page.getByTestId('search-validation-warning')).toHaveCount(0)

    const minAreaInput = searchPanel.locator('input[type="number"]').nth(2)
    await minAreaInput.fill('0')
    await expect(page.getByTestId('search-validation-warning')).toContainText('legalább 1 m²')
    await expect(searchBtn).toBeDisabled()

    await minAreaInput.fill('200')
    await expect(page.getByTestId('search-validation-warning')).toContainText('alapterület')
    await expect(searchBtn).toBeDisabled()
    await minAreaInput.fill('30')
    await expect(page.getByTestId('search-validation-warning')).toHaveCount(0)

    await minPriceInput.fill('35')
    await page.getByTitle('Szűrők alaphelyzetbe állítása').click()
    await expect(minPriceInput).toHaveValue('70')

    await page.getByRole('button', { name: 'Összes törlése' }).click()
    await expect(page.getByText(/0\/6/)).toBeVisible()
    await expect(searchBtn).toBeDisabled()
    await page.getByRole('button', { name: 'Mind kijelöl' }).click()
    await expect(page.getByText(/6\/6/)).toBeVisible()
    await expect(searchBtn).toBeEnabled()
  })

  test('2. Tier 1, 2 & 4 (F4/F6/F8/S3): Single-Portal Chip Rule, Partial Warning, Enable All, Diagnostics -> Import Handoff', async ({
    page,
  }) => {
    let imported = false
    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        const baseItems = imported ? buildRichDataset() : SAMPLE_PROPERTIES.slice(0, 1)
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            createMockSearchResponse(baseItems, {
              executionStatus: imported ? 'SUCCESS' : 'PARTIAL_SUCCESS',
              newCitiesInitialized: ['Debrecen'],
              portalStatuses: [
                { portalId: 'dh', portalName: 'Duna House', status: 'SUCCESS', latencyMs: 190, foundCount: 1 },
                {
                  portalId: 'ingatlancom',
                  portalName: 'Ingatlan.com',
                  status: imported ? 'SUCCESS' : 'BOT_PROTECTED',
                  latencyMs: 650,
                  foundCount: imported ? 1 : 0,
                  fallbackUrl: 'https://ingatlan.com',
                  message: imported ? 'Importálva' : 'Cloudflare kihívás',
                },
              ],
            }),
          ),
        })
      },
    })

    await page.goto('/')
    const searchPanel = page.getByTestId('search-panel-container')
    await searchPanel.getByRole('button', { name: 'Összes törlése' }).click()
    await searchPanel.getByRole('button', { name: /Duna House/i }).click()
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()

    await expect(
      page.getByTestId('portal-quick-filter-chips').getByRole('button', { name: /Összes/i }),
    ).toHaveCount(0)

    await searchPanel.getByRole('button', { name: /Ingatlan\.com/i }).click()
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()

    await expect(page.getByTestId('new-cities-initialized-banner')).toBeVisible()
    const partialBanner = page.getByTestId('partial-portal-warning-banner')
    await expect(partialBanner).toBeVisible()
    await expect(partialBanner).toHaveAttribute('data-total-failure', 'false')

    const enableAllBtn = partialBanner.getByTestId('enable-all-portals-rerun-btn')
    await expect(enableAllBtn).toBeVisible()
    await enableAllBtn.click()
    await expect(searchPanel.getByText(/6\/6/)).toBeVisible()

    await expect(partialBanner.getByTestId('open-ingatlancom-import-btn')).toBeVisible()
    await partialBanner.getByRole('button', { name: /Diagnosztika megtekintése/i }).click()
    await expect(page.getByTestId('portal-status-card-ingatlancom')).toBeVisible()
    await expect(page.getByTestId('fallback-link-ingatlancom')).toBeVisible()

    await page.getByRole('button', { name: /Bezárás/i }).first().click()
    await partialBanner.getByTestId('open-ingatlancom-import-btn').click()

    const importModal = page.getByTestId('ingatlancom-import-modal')
    await expect(importModal).toBeVisible()
    await importModal.getByTestId('submit-ingatlancom-import').click()
    await expect(importModal.getByRole('alert')).toBeVisible()

    imported = true
    await importModal
      .getByTestId('ingatlancom-html-textarea')
      .fill("<a href='/34567890' class='listing-card'>Debrecen lakás</a>")
    await importModal.getByTestId('submit-ingatlancom-import').click()

    await expect(importModal.getByTestId('ingatlancom-import-summary')).toContainText('1 kártya beolvasva')
    await importModal.getByRole('button', { name: 'Bezárás', exact: true }).last().click()
    await expect(page.getByTestId('property-card')).toHaveCount(8)

    await page.locator('header button:has([data-testid="header-portal-health-count"])').click()
    const diagImportBtn = page.getByTestId('diagnostics-import-ingatlancom-btn')
    await expect(diagImportBtn).toBeVisible()
    await diagImportBtn.click()
    await expect(importModal).toBeVisible()
    await importModal.getByRole('button', { name: 'Bezárás', exact: true }).last().click()
  })

  test('3. Tier 1, 2 & 4 (F2/F4/F5/F6/F7/S5): KPI Filters, Favorites, View Modes, Archive Restore & Clear History', async ({
    page,
  }) => {
    await setupStandardApiMocks(page, {
      historyItems: [SAMPLE_PROPERTIES[1]!],
      discountedItems: [SAMPLE_PROPERTIES[0]!],
      hiddenItems: [{ ...SAMPLE_PROPERTIES[2]!, hidden: true }],
    })
    await page.goto('/')

    await expect(page.getByTestId('header-db-telemetry-pill')).toContainText('42 DB')
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(3)

    const kpiNewBtn = page.getByTestId('kpi-filter-new')
    await kpiNewBtn.click()
    await expect(kpiNewBtn).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByTestId('property-card')).toHaveCount(2)
    await page.getByTestId('kpi-filter-all').click()
    await expect(page.getByTestId('property-card')).toHaveCount(3)

    await page.getByTestId('property-card').first().getByLabel('Hozzáadás a kedvencekhez').click()
    await page.getByTestId('subview-favorites').click()
    await expect(page.getByTestId('property-card')).toHaveCount(1)
    await page.getByTestId('property-card').first().getByLabel('Eltávolítás a kedvencekből').click()
    await expect(page.getByText('Még nincsenek kedvencnek jelölt ingatlanok')).toBeVisible()

    await page.getByTestId('tab-listings').click()
    await page.getByTitle('Lista nézet').click()
    await page.getByTestId('details-button').first().click()
    await expect(page.getByTestId('multi-source-drawer')).toBeVisible()

    await page.getByTestId('view-mode-table').click()
    await expect(page.getByTestId('listings-table-view')).toBeVisible()
    await expect(page.getByTestId('listings-table-row')).toHaveCount(3)
    await page.getByTitle('Rács nézet').click()

    await page.getByTestId('quick-filter-selector-btn').click()
    const popover = page.getByTestId('quick-filter-selector-popover')
    await expect(popover).toBeVisible()
    expect(await popover.locator('input[type="checkbox"]').count()).toBeGreaterThanOrEqual(7)
    await popover.getByTestId('tri-state-bargain-only').click()
    await expect(page.getByTestId('property-card')).toHaveCount(1)
    await popover.getByTestId('tri-state-bargain-all').click()
    await page.keyboard.press('Escape')

    await page.getByTestId('portal-quick-filter-chips').getByRole('button', { name: /Duna House/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(1)
    await page.getByTestId('portal-quick-filter-chips').getByRole('button', { name: /Összes/i }).click()

    await page.getByTestId('tab-archive').click()
    await expect(page.getByTestId('archive-subview-toolbar')).toBeVisible()
    await page.getByTestId('archive-mode-discounted').click()
    await expect(page.getByTestId('archive-mode-discounted')).toHaveAttribute('aria-pressed', 'true')
    await page.getByTestId('archive-mode-hidden').click()
    await expect(page.getByTestId('archive-mode-hidden')).toHaveAttribute('aria-pressed', 'true')

    const restoreBtn = page.getByTestId('restore-property-button').first()
    await expect(restoreBtn).toBeVisible()
    await restoreBtn.click()
    await expect(page.getByTestId('property-card')).toHaveCount(0)

    await page.getByTestId('archive-mode-all').click()
    await page.getByRole('button', { name: 'Előzmények törlése' }).click()
    await page.getByRole('button', { name: 'Törlés megerősítése' }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(0)
  })

  test('4. Tier 1, 2 & 3 (F4/F6/F7/F8/S1/S4): Bargain Popover, Sparkline, Split + Undo Override, Escape & Merge', async ({
    page,
  }) => {
    const rich = buildRichDataset()
    await setupStandardApiMocks(page, { searchItems: rich })
    await page.goto('/')

    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(8)

    const bargainBtn = page.getByTestId('bargain-badge-button').first()
    await expect(bargainBtn).toBeVisible()
    await bargainBtn.click()
    await expect(page.getByTestId('bargain-popover')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('bargain-popover')).toHaveCount(0)

    await page.getByTestId('sources-count-badge').first().click()
    const detailsModal = page.getByTestId('property-details-modal')
    await expect(detailsModal).toBeVisible()
    await expect(detailsModal.getByTestId('sources-list-section')).toBeVisible()
    await expect(detailsModal.getByTestId('drawer-cross-portal-spread')).toBeVisible()
    await expect(detailsModal.getByTestId('property-timeline-section')).toBeVisible()
    await expect(detailsModal.getByTestId('price-history-sparkline')).toBeVisible()
    await expect(detailsModal.getByTestId('timeline-price-delta-row')).toBeVisible()
    await expect(detailsModal.getByTestId('timeline-unchanged-badge')).toBeVisible()

    await detailsModal.getByTestId('split-source-button').first().click()
    await detailsModal.getByTestId('confirm-split-button').click()
    await detailsModal.getByTestId('property-details-modal-close').click()
    await expect(page.getByTestId('property-card')).toHaveCount(9)

    await page.getByTestId('details-button').first().click()
    await expect(detailsModal).toBeVisible()
    const undoOverrideBtn = detailsModal.locator('[data-testid^="undo-override-"]').first()
    await expect(undoOverrideBtn).toBeVisible()
    await undoOverrideBtn.click()
    await detailsModal.getByTestId('property-details-modal-close').click()
    await expect(page.getByTestId('property-card')).toHaveCount(8)

    const cards = page.getByTestId('property-card')
    await cards.nth(0).click({ modifiers: ['Control'] })
    await expect(page.getByTestId('selection-action-bar')).toBeVisible()
    await expect(page.getByTestId('merge-selected-button')).toBeDisabled()
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('selection-action-bar')).toHaveCount(0)

    await cards.nth(0).click({ modifiers: ['Control'] })
    await cards.nth(1).click({ modifiers: ['Control'] })
    await expect(page.getByTestId('merge-selected-button')).toBeEnabled()
    await page.getByTestId('merge-selected-button').click()
    await expect(page.getByTestId('property-card')).toHaveCount(7)

    await cards.nth(0).getByTestId('hide-property-button').click()
    const undoToast = page.getByTestId('undo-hide-toast')
    await expect(undoToast).toBeVisible()
    await undoToast.getByRole('button', { name: /Visszavonás/i }).click()
    await expect(undoToast).toHaveCount(0)
  })

  test('5. Tier 1, 2 & 3 (F9/S2): MarketAnalyticsPanel Empty, Insufficient (<5) & Full Drill-Down', async ({
    page,
  }) => {
    let mode: 'empty' | 'small' | 'rich' = 'empty'
    await setupStandardApiMocks(page, {
      onSearchRequest: async (route) => {
        if (mode === 'empty') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse([])),
          })
          return
        }
        if (mode === 'small') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(createMockSearchResponse(SAMPLE_PROPERTIES.slice(0, 2))),
          })
          return
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            createMockSearchResponse(buildRichDataset(), {
              marketStats: {
                averagePriceMillion: 71,
                averagePricePerM2: 962_615,
                medianPricePerM2: 950_000,
                minPriceMillion: 48,
                maxPriceMillion: 96,
                newRatePercent: 50,
                totalSampleSize: 8,
                priceDropCount: 3,
                bargainCount: 3,
                segments: RICH_SEGMENTS,
              },
            }),
          ),
        })
      },
    })

    await page.goto('/')
    await page.getByTestId('tab-analytics').click()
    await expect(page.getByTestId('market-analytics-empty-state')).toBeVisible()

    mode = 'small'
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(2)
    await page.getByTestId('tab-analytics').click()
    await expect(page.getByTestId('insufficient-data-banner')).toBeVisible()

    mode = 'rich'
    await page.getByRole('button', { name: /Ingatlanok Keresése/i }).click()
    await expect(page.getByTestId('property-card')).toHaveCount(8)
    await page.getByTestId('tab-analytics').click()
    const panel = page.getByTestId('market-analytics-panel')
    await expect(panel.getByTestId('kpi-card-lakas')).toBeVisible()
    await expect(panel.getByTestId('kpi-card-avg-price')).toBeVisible()
    await expect(panel.getByTestId('kpi-card-total-samples')).toBeVisible()
    await expect(panel.getByTestId('kpi-card-price-drops')).toBeVisible()
    await expect(panel.getByTestId('market-segment-matrix-table')).toBeVisible()
    await expect(panel.getByTestId('top-price-drops-section')).toBeVisible()
    await expect(panel.locator('.recharts-responsive-container').first()).toBeVisible()

    await panel.getByTestId('category-help-button').click()
    await expect(panel.getByTestId('category-help-popover')).toBeVisible()

    await panel.getByRole('button', { name: /Leárazások/i }).click()
    await panel.getByRole('button', { name: /Piaci átlag alatt/i }).click()

    const searchPanel = page.getByTestId('search-panel-container')
    await searchPanel.getByRole('button', { name: 'Ház', exact: true }).click()
    await panel.getByTestId('segment-matrix-row').first().getByRole('button', { name: /Szűrés/i }).click()

    await page.getByTestId('tab-listings').click()
    await expect(searchPanel.getByText('Debrecen', { exact: true }).first()).toBeVisible()
    const lakasBtn = searchPanel.getByRole('button', { name: 'Lakás', exact: true })
    const hazBtn = searchPanel.getByRole('button', { name: 'Ház', exact: true })
    await expect(lakasBtn).toHaveCSS('background-color', 'rgb(0, 113, 227)')
    await expect(hazBtn).not.toHaveCSS('background-color', 'rgb(0, 113, 227)')
    await expect(lakasBtn).toHaveClass(/bg-\[#0071e3\]/)
    await expect(hazBtn).not.toHaveClass(/bg-\[#0071e3\]/)
  })

  for (const vp of DESKTOP_VIEWPORTS) {
    test(`6. Desktop Geometry (${vp.width}x${vp.height}) & Liquid Glass Visual Rubric`, async ({
      page,
    }) => {
      const geometryItems = buildGeometryDataset()
      await setupStandardApiMocks(page, {
        onSearchRequest: async (route) => {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(
              createMockSearchResponse(geometryItems, {
                marketStats: {
                  averagePriceMillion: 71,
                  averagePricePerM2: 962_615,
                  medianPricePerM2: 950_000,
                  minPriceMillion: 48,
                  maxPriceMillion: 96,
                  newRatePercent: 50,
                  totalSampleSize: geometryItems.length,
                  priceDropCount: 3,
                  bargainCount: 3,
                  segments: RICH_SEGMENTS,
                },
              }),
            ),
          })
        },
      })

      await runDesktopGeometryPass(page, vp)
    })
  }
})
