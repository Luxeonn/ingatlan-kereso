import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DiagnosticsModal } from '../components/modals/DiagnosticsModal'
import { useUiStore } from '../store/useUiStore'
import { createMockPortalStatus } from './testFixtures'
import * as formatters from '../utils/formatters'

describe('DiagnosticsModal (#360)', () => {
  beforeEach(() => {
    useUiStore.setState({ isDiagnosticsModalOpen: true })
    vi.restoreAllMocks()
  })

  it('always renders and opens fallbackUrl button even when status is SUCCESS and foundCount > 0 (#360)', () => {
    const openExternalUrlSpy = vi.spyOn(formatters, 'openExternalUrl').mockImplementation(() => true)
    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    })

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <DiagnosticsModal
          portalStatuses={[
            createMockPortalStatus({
              portalId: 'dh',
              portalName: 'Duna House',
              status: 'SUCCESS',
              latencyMs: 410,
              foundCount: 15,
              fallbackUrl: 'https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen',
              message: 'Sikeres letöltés (15 találat)',
            }),
            createMockPortalStatus({
              portalId: 'oc',
              portalName: 'Otthon Centrum',
              status: 'EMPTY',
              latencyMs: 280,
              foundCount: 0,
              fallbackUrl: 'https://www.oc.hu/ingatlanok/lista/elhelyezkedes:debrecen',
              message: '0 találat a megadott szűrőkre',
            }),
            createMockPortalStatus({
              portalId: 'ingatlancom',
              portalName: 'Ingatlan.com',
              status: 'BOT_PROTECTED',
              latencyMs: 190,
              foundCount: 0,
              fallbackUrl: 'https://ingatlan.com/lista/elado+haz+debrecen',
              message: 'Cloudflare WAF detektálva',
            }),
          ]}
        />
      </QueryClientProvider>,
    )

    const dhLinkBtn = screen.getByTestId('fallback-link-dh')
    expect(dhLinkBtn).toBeTruthy()
    expect(dhLinkBtn.textContent).toContain('Keresés megnyitása itt: Duna House')
    expect(screen.getByText('https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen')).toBeTruthy()

    fireEvent.click(dhLinkBtn)
    expect(openExternalUrlSpy).toHaveBeenCalledWith(
      'https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen',
    )

    const ocLinkBtn = screen.getByTestId('fallback-link-oc')
    expect(ocLinkBtn).toBeTruthy()
    fireEvent.click(ocLinkBtn)
    expect(openExternalUrlSpy).toHaveBeenCalledWith(
      'https://www.oc.hu/ingatlanok/lista/elhelyezkedes:debrecen',
    )

    const icLinkBtn = screen.getByTestId('fallback-link-ingatlancom')
    expect(icLinkBtn).toBeTruthy()
    fireEvent.click(icLinkBtn)
    expect(openExternalUrlSpy).toHaveBeenCalledWith('https://ingatlan.com/lista/elado+haz+debrecen')

    const dhMessage = screen.getByTestId('portal-message-dh')
    expect(dhMessage.className).toContain('--status-bargain-bg')
    expect(dhMessage.className).not.toContain('--status-danger-bg')

    const ocMessage = screen.getByTestId('portal-message-oc')
    expect(ocMessage.className).toContain('bg-slate-100')
    expect(ocMessage.className).not.toContain('--status-danger-bg')

    const exportBtn = screen.getByRole('button', { name: /Export/i })
    fireEvent.click(exportBtn)
    expect(writeTextMock).toHaveBeenCalledTimes(1)
    expect(writeTextMock.mock.calls[0]![0]).toContain(
      '[DH] Duna House: SUCCESS (410ms, 15 találat) - Infó: Sikeres letöltés (15 találat) - Fallback: https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen',
    )
  })

  it('closes on outside backdrop click and Escape key', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <DiagnosticsModal portalStatuses={[]} />
      </QueryClientProvider>,
    )

    expect(screen.getByTestId('diagnostics-modal')).toBeTruthy()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(false)

    act(() => {
      useUiStore.setState({ isDiagnosticsModalOpen: true })
    })
    fireEvent.mouseDown(document.body)
    expect(useUiStore.getState().isDiagnosticsModalOpen).toBe(false)
  })
})
