import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SearchPresetsBar } from '../components/filters/SearchPresetsBar'
import * as usePresetsModule from '../hooks/usePresets'
import { useFilterStore } from '../store/useFilterStore'
import type { SearchPreset } from '../types/api'

vi.mock('../hooks/usePresets')

describe('SearchPresetsBar component', () => {
  const mockCreateMutate = vi.fn()
  const mockUpdateMutate = vi.fn()
  const mockDeleteMutate = vi.fn()
  const mockApplyMutate = vi.fn()

  const mockPresets: SearchPreset[] = [
    {
      id: 1,
      name: 'Hajdúszoboszló Ház 70-120M',
      criteria: {
        city: 'Hajdúszoboszló',
        cities: ['Hajdúszoboszló'],
        propertyType: 'haz',
        minPriceMillion: 70,
        maxPriceMillion: 120,
        minArea: 90,
        maxArea: 200,
        portals: ['dh', 'oc'],
        autoOpenBrowser: false,
        excludeViewed: false,
        onlyDiscounted: false,
        onlyNewConstruction: false,
      },
      createdAt: '2026-11-06T12:00:00Z',
      updatedAt: '2026-11-06T12:00:00Z',
      lastUsedAt: null,
    },
    {
      id: 2,
      name: 'Debrecen Lakás 40-75M',
      criteria: {
        city: 'Debrecen',
        cities: ['Debrecen'],
        propertyType: 'lakas',
        minPriceMillion: 40,
        maxPriceMillion: 75,
        minArea: 40,
        maxArea: 80,
        portals: ['dh', 'oc', 'jofogas'],
        autoOpenBrowser: false,
        excludeViewed: false,
        onlyDiscounted: false,
        onlyNewConstruction: false,
      },
      createdAt: '2026-11-06T11:00:00Z',
      updatedAt: '2026-11-06T11:00:00Z',
      lastUsedAt: null,
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(usePresetsModule.useCreatePreset).mockReturnValue({
      mutate: mockCreateMutate,
      isPending: false,
    } as unknown as ReturnType<typeof usePresetsModule.useCreatePreset>)
    vi.mocked(usePresetsModule.useUpdatePreset).mockReturnValue({
      mutate: mockUpdateMutate,
      isPending: false,
    } as unknown as ReturnType<typeof usePresetsModule.useUpdatePreset>)
    vi.mocked(usePresetsModule.useDeletePreset).mockReturnValue({
      mutate: mockDeleteMutate,
      isPending: false,
    } as unknown as ReturnType<typeof usePresetsModule.useDeletePreset>)
    vi.mocked(usePresetsModule.useApplyPreset).mockReturnValue({
      mutate: mockApplyMutate,
      isPending: false,
    } as unknown as ReturnType<typeof usePresetsModule.useApplyPreset>)
  })

  it('renders empty message when no presets exist', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)
    expect(screen.getByText('Nincsenek mentett sablonok.')).toBeTruthy()
    expect(screen.getByText('Mentés sablonként')).toBeTruthy()
  })

  it('renders preset chips and selecting one updates store criteria and triggers search', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    const handleSearch = vi.fn()
    render(<SearchPresetsBar onSearch={handleSearch} />)

    expect(screen.getByText('Hajdúszoboszló Ház 70-120M')).toBeTruthy()
    expect(screen.getByText('Debrecen Lakás 40-75M')).toBeTruthy()

    // Click on the second preset chip
    fireEvent.click(screen.getByTestId('preset-chip-2'))

    expect(handleSearch).toHaveBeenCalledTimes(1)
    expect(mockApplyMutate).toHaveBeenCalledWith(2)

    // Store should have updated criteria
    const currentCriteria = useFilterStore.getState().criteria
    expect(currentCriteria.city).toBe('Debrecen')
    expect(currentCriteria.propertyType).toBe('lakas')
    expect(currentCriteria.minPriceMillion).toBe(40)
    expect(currentCriteria.maxPriceMillion).toBe(75)
  })

  it('opens create input form and submits a new preset', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    fireEvent.click(screen.getByTestId('save-preset-button'))

    const input = screen.getByPlaceholderText('Sablon neve')
    expect(input).toBeTruthy()

    fireEvent.change(input, { target: { value: 'Balaton Nyaraló' } })
    fireEvent.click(screen.getByTitle('Mentés sablonként'))

    expect(mockCreateMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Balaton Nyaraló',
      }),
      expect.anything(),
    )
  })

  it('allows renaming a preset inline', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    // Click edit button on the first preset
    const editButtons = screen.getAllByTitle('Sablon átnevezése')
    fireEvent.click(editButtons[0])

    const editInput = screen.getByDisplayValue('Hajdúszoboszló Ház 70-120M')
    expect(editInput).toBeTruthy()

    fireEvent.change(editInput, { target: { value: 'Szoboszló Prémium Házak' } })
    fireEvent.click(screen.getByTitle('Mentés'))

    expect(mockUpdateMutate).toHaveBeenCalledWith(
      { id: 1, preset: { name: 'Szoboszló Prémium Házak' } },
      expect.anything(),
    )
  })

  it('allows deleting a preset', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    const deleteButtons = screen.getAllByTitle('Sablon törlése')
    fireEvent.click(deleteButtons[0])

    expect(mockDeleteMutate).toHaveBeenCalledWith(1, expect.anything())
  })

  it('displays error alert and preserves input when preset creation fails (#394)', () => {
    mockCreateMutate.mockImplementation((_payload, options) => {
      options?.onError?.(new Error('Hálózati hiba a sablon mentésekor.'))
    })

    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    fireEvent.click(screen.getByTestId('save-preset-button'))
    const input = screen.getByPlaceholderText('Sablon neve') as HTMLInputElement
    fireEvent.change(input, { target: { value: 'Egyedi Sablon' } })
    fireEvent.click(screen.getByTitle('Mentés sablonként'))

    expect(mockCreateMutate).toHaveBeenCalledTimes(1)
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('Hálózati hiba a sablon mentésekor.')
    // Input should remain open with value preserved so user can retry
    expect(screen.getByPlaceholderText('Sablon neve')).toBeTruthy()
    expect((screen.getByPlaceholderText('Sablon neve') as HTMLInputElement).value).toBe('Egyedi Sablon')
  })

  it('displays validation error when attempting to save empty name (#394)', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    fireEvent.click(screen.getByTestId('save-preset-button'))
    const input = screen.getByPlaceholderText('Sablon neve')
    fireEvent.change(input, { target: { value: '   ' } })
    fireEvent.click(screen.getByTitle('Mentés sablonként'))

    expect(mockCreateMutate).not.toHaveBeenCalled()
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('A sablon neve nem lehet üres.')
  })

  it('displays success feedback and closes form when preset creation succeeds (#394)', () => {
    mockCreateMutate.mockImplementation((payload, options) => {
      options?.onSuccess?.({
        id: 99,
        name: payload.name,
        criteria: payload.criteria,
        createdAt: '2026-11-08T12:00:00Z',
        updatedAt: '2026-11-08T12:00:00Z',
        lastUsedAt: null,
      })
    })

    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    fireEvent.click(screen.getByTestId('save-preset-button'))
    const input = screen.getByPlaceholderText('Sablon neve')
    fireEvent.change(input, { target: { value: 'Új Mentett Sablon' } })
    fireEvent.click(screen.getByTitle('Mentés sablonként'))

    expect(mockCreateMutate).toHaveBeenCalledTimes(1)
    const status = screen.getByRole('status')
    expect(status).toBeTruthy()
    expect(status.textContent).toContain('Új Mentett Sablon')
    expect(screen.queryByPlaceholderText('Sablon neve')).toBeNull()
  })

  it('displays error alert when preset update fails (#394)', () => {
    mockUpdateMutate.mockImplementation((_payload, options) => {
      options?.onError?.(new Error('Nem sikerült módosítani a sablont.'))
    })

    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    const editButtons = screen.getAllByTitle('Sablon átnevezése')
    fireEvent.click(editButtons[0])

    const editInput = screen.getByDisplayValue('Hajdúszoboszló Ház 70-120M')
    fireEvent.change(editInput, { target: { value: 'Módosított Név' } })
    fireEvent.click(screen.getByTitle('Mentés'))

    expect(mockUpdateMutate).toHaveBeenCalledTimes(1)
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('Nem sikerült módosítani a sablont.')
  })

  it('displays error alert when preset deletion fails (#394)', () => {
    mockDeleteMutate.mockImplementation((_id, options) => {
      options?.onError?.(new Error('Nem sikerült törölni a sablont.'))
    })

    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: mockPresets,
      isLoading: false,
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    const deleteButtons = screen.getAllByTitle('Sablon törlése')
    fireEvent.click(deleteButtons[0])

    expect(mockDeleteMutate).toHaveBeenCalledTimes(1)
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('Nem sikerült törölni a sablont.')
  })

  it('displays query error message when fetching presets fails (#394)', () => {
    vi.mocked(usePresetsModule.usePresets).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('Backend hiba a sablonok lekérésekor'),
    } as unknown as ReturnType<typeof usePresetsModule.usePresets>)

    render(<SearchPresetsBar onSearch={vi.fn()} />)

    expect(screen.getByText('Nem sikerült betölteni a keresési sablonokat.')).toBeTruthy()
  })
})
