import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PortalToggleGroup } from '../components/filters/PortalToggleGroup'
import * as usePortalsModule from '../hooks/usePortals'

vi.mock('../hooks/usePortals')

describe('PortalToggleGroup component', () => {
  const mockPortals = [
    { id: 'dh', name: 'Duna House', brandColor: '#1d4ed8', homepageUrl: 'https://dh.hu', enabledByDefault: true, supportedTypes: ['lakas', 'haz', 'telek'], directListingSupported: true, savedListingCount: 21 },
    { id: 'oc', name: 'Otthon Centrum', brandColor: '#ea580c', homepageUrl: 'https://oc.hu', enabledByDefault: true, supportedTypes: ['lakas', 'haz', 'telek'], directListingSupported: true, savedListingCount: 0 },
  ]

  it('renders portal toggle buttons without misleading savedListingCount badge', () => {
    vi.mocked(usePortalsModule.usePortals).mockReturnValue({
      data: mockPortals,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof usePortalsModule.usePortals>)

    render(
      <PortalToggleGroup
        selectedPortals={['dh']}
        onToggle={() => {}}
        onSelectAll={() => {}}
      />
    )

    // Portal names are visible
    expect(screen.getByText('Duna House')).toBeTruthy()
    expect(screen.getByText('Otthon Centrum')).toBeTruthy()

    // The misleading count badge '21' must NOT be rendered next to the portal button
    expect(screen.queryByText('21')).toBeNull()
  })

  it('triggers onToggle when a portal button is clicked', () => {
    const handleToggle = vi.fn()
    vi.mocked(usePortalsModule.usePortals).mockReturnValue({
      data: mockPortals,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof usePortalsModule.usePortals>)

    render(
      <PortalToggleGroup
        selectedPortals={['dh']}
        onToggle={handleToggle}
        onSelectAll={() => {}}
      />
    )

    const ocButton = screen.getByRole('button', { name: /Otthon Centrum/i })
    fireEvent.click(ocButton)
    expect(handleToggle).toHaveBeenCalledWith('oc')
  })

  it('toggles all portals via header action button', () => {
    const handleSelectAll = vi.fn()
    vi.mocked(usePortalsModule.usePortals).mockReturnValue({
      data: mockPortals,
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof usePortalsModule.usePortals>)

    const { rerender } = render(
      <PortalToggleGroup
        selectedPortals={['dh']}
        onToggle={() => {}}
        onSelectAll={handleSelectAll}
      />
    )

    const selectAllBtn = screen.getByRole('button', { name: /Mind kijelöl/i })
    fireEvent.click(selectAllBtn)
    expect(handleSelectAll).toHaveBeenCalledWith(['dh', 'oc'])

    rerender(
      <PortalToggleGroup
        selectedPortals={['dh', 'oc']}
        onToggle={() => {}}
        onSelectAll={handleSelectAll}
      />
    )

    const deselectAllBtn = screen.getByRole('button', { name: /Összes törlése/i })
    fireEvent.click(deselectAllBtn)
    expect(handleSelectAll).toHaveBeenCalledWith([])
  })
})
