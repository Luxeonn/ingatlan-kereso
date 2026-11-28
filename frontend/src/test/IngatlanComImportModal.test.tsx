import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { IngatlanComImportModal } from '../components/modals/IngatlanComImportModal'
import { PartialPortalWarningBanner } from '../components/layout/StatusBanners'
import {
  buildIngatlanComSearchUrl,
  extractIngatlanComFetchedPages,
  findPartialIngatlanComStatus,
} from '../utils/ingatlanComUrl'
import { useUiStore } from '../store/useUiStore'
import { useFilterStore, DEFAULT_PORTALS } from '../store/useFilterStore'
import * as portalApi from '../api/portalApi'
import * as formatters from '../utils/formatters'
import { createMockPortalStatus } from './testFixtures'

describe('Ingatlan.com HTML import & URL builder (#455)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    useUiStore.setState({ isIngatlanComImportModalOpen: true })
    useFilterStore.getState().setCriteria({
      cities: ['Hajdúszoboszló'],
      propertyType: 'HAZ',
      minPriceMillion: 60,
      maxPriceMillion: 130,
      minArea: 95,
      maxArea: 200,
      portals: [...DEFAULT_PORTALS],
    })
  })

  it('builds exact Ingatlan.com paginated search URLs from filter criteria', () => {
    const page1 = buildIngatlanComSearchUrl(
      {
        city: 'Hajdúszoboszló',
        propertyType: 'HAZ',
        minPriceMillion: 60,
        maxPriceMillion: 130,
        minArea: 95,
        maxArea: 200,
      },
      1,
    )
    expect(page1).toBe(
      'https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo',
    )

    const page3 = buildIngatlanComSearchUrl(
      {
        city: 'Hajdúszoboszló',
        propertyType: 'HAZ',
        minPriceMillion: 60,
        maxPriceMillion: 130,
        minArea: 95,
        maxArea: 200,
      },
      3,
    )
    expect(page3).toBe(
      'https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo?page=3',
    )
  })

  it('submits pasted HTML, shows summary and invokes onImportSuccess', async () => {
    const importSpy = vi.spyOn(portalApi, 'importIngatlanComHtml').mockResolvedValue({
      parsedCount: 5,
      savedCount: 5,
      newlyAddedCount: 4,
      duplicateCount: 1,
      city: 'Hajdúszoboszló',
      items: [],
    })
    const openExternalUrlSpy = vi.spyOn(formatters, 'openExternalUrl').mockImplementation(() => true)
    const onImportSuccess = vi.fn()

    render(<IngatlanComImportModal onImportSuccess={onImportSuccess} />)

    fireEvent.click(screen.getByTestId('open-ingatlancom-page-2'))
    expect(openExternalUrlSpy).toHaveBeenCalledWith(
      'https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo?page=2',
    )

    fireEvent.change(screen.getByTestId('ingatlancom-html-textarea'), {
      target: { value: '<a href="/39990001" class="listing-card">Teszt</a>' },
    })
    fireEvent.click(screen.getByTestId('submit-ingatlancom-import'))

    await waitFor(() => {
      expect(importSpy).toHaveBeenCalledTimes(1)
      expect(screen.getByTestId('ingatlancom-import-summary').textContent).toContain('4 új')
    })
    expect(onImportSuccess).toHaveBeenCalledTimes(1)
  })

  it('shows import & enable-all-portals quick actions on PartialPortalWarningBanner', () => {
    const onOpenDiagnostics = vi.fn()
    const onOpenImportModal = vi.fn()
    const onEnableAllPortals = vi.fn()

    render(
      <PartialPortalWarningBanner
        failures={[]}
        hasIngatlanComPartial={true}
        showEnableAllPortals={true}
        onOpenDiagnostics={onOpenDiagnostics}
        onOpenImportModal={onOpenImportModal}
        onEnableAllPortals={onEnableAllPortals}
      />,
    )

    fireEvent.click(screen.getByTestId('open-ingatlancom-import-btn'))
    expect(onOpenImportModal).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByTestId('enable-all-portals-rerun-btn'))
    expect(onEnableAllPortals).toHaveBeenCalledTimes(1)

    render(
      <PartialPortalWarningBanner
        failures={[createMockPortalStatus({ portalId: 'ingatlancom', portalName: 'Ingatlan.com', status: 'BOT_PROTECTED' })]}
        hasIngatlanComPartial={true}
        onOpenDiagnostics={onOpenDiagnostics}
        onOpenImportModal={onOpenImportModal}
      />,
    )
    expect(screen.getAllByTestId('open-ingatlancom-import-btn').length).toBeGreaterThanOrEqual(1)
  })

  it('suggests next pages (6, 7, 8) and shows accurate partial message when 5 pages succeeded', () => {
    const partialStatus = createMockPortalStatus({
      portalId: 'ingatlancom',
      portalName: 'Ingatlan.com',
      status: 'SUCCESS',
      message: 'Részleges találati lista (5 oldal): a Cloudflare WAF korlátozta a további oldalakat',
      technicalDetails: 'Strategy: SIDECAR | pages=5, stopReason=INTERMEDIATE_PAGE_RATE_LIMITED',
    })

    const found = findPartialIngatlanComStatus([partialStatus])
    expect(found?.portalId).toBe('ingatlancom')
    const fetchedPages = extractIngatlanComFetchedPages(found)
    expect(fetchedPages).toBe(5)

    render(
      <>
        <PartialPortalWarningBanner
          failures={[]}
          hasIngatlanComPartial={true}
          ingatlanComMessage={found?.message}
          onOpenDiagnostics={vi.fn()}
          onOpenImportModal={vi.fn()}
        />
        <IngatlanComImportModal fetchedPages={fetchedPages} />
      </>,
    )

    expect(screen.getByTestId('partial-portal-warning-banner').textContent).toContain('Részleges találati lista (5 oldal)')
    expect(screen.getByTestId('open-ingatlancom-page-6')).toBeTruthy()
    expect(screen.getByTestId('open-ingatlancom-page-7')).toBeTruthy()
    expect(screen.getByTestId('open-ingatlancom-page-8')).toBeTruthy()
  })

  it('closes on outside click and Escape key', () => {
    render(<IngatlanComImportModal />)
    expect(screen.getByTestId('ingatlancom-import-modal')).toBeTruthy()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(useUiStore.getState().isIngatlanComImportModalOpen).toBe(false)

    act(() => {
      useUiStore.setState({ isIngatlanComImportModalOpen: true })
    })
    fireEvent.mouseDown(document.body)
    expect(useUiStore.getState().isIngatlanComImportModalOpen).toBe(false)
  })
})
