import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CitySelector } from '../components/filters/CitySelector'
import * as useCitiesModule from '../hooks/useCities'

vi.mock('../hooks/useCities')

describe('CitySelector component', () => {
  it('renders correctly and handles object-based predefined cities from backend without crash', () => {
    vi.mocked(useCitiesModule.useCities).mockReturnValue({
      data: {
        predefined: [
          { key: 'debrecen', name: 'Debrecen', county: 'Hajdú-Bihar', description: 'Székhely' },
          { key: 'budapest', name: 'Budapest', county: 'Pest', description: 'Főváros' },
        ],
        presets: {},
        trackedCities: [
          { key: 'debrecen', name: 'Debrecen', count: 42 },
        ],
      },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useCitiesModule.useCities>)

    const handleAdd = vi.fn()
    const handleRemove = vi.fn()
    const handleSet = vi.fn()

    render(
      <CitySelector
        selectedCities={['Debrecen']}
        onAddCity={handleAdd}
        onRemoveCity={handleRemove}
        onSetCities={handleSet}
      />
    )

    // Ensure selected city chip is rendered
    expect(screen.getByText('Debrecen')).toBeTruthy()

    // Open dropdown by focusing the input
    const input = screen.getByPlaceholderText('További város hozzáadása...')
    fireEvent.focus(input)

    // Budapest should appear in filtered dropdown
    expect(screen.getByText('Budapest')).toBeTruthy()

    // Click Budapest to select
    fireEvent.click(screen.getByText('Budapest'))
    expect(handleAdd).toHaveBeenCalledWith('Budapest')
  })

  it('handles string-based predefined cities gracefully', () => {
    vi.mocked(useCitiesModule.useCities).mockReturnValue({
      data: {
        predefined: ['Szeged', 'Pécs'],
        presets: {},
        trackedCities: {},
      },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useCitiesModule.useCities>)

    const handleAdd = vi.fn()
    render(
      <CitySelector
        selectedCities={[]}
        onAddCity={handleAdd}
        onRemoveCity={vi.fn()}
        onSetCities={vi.fn()}
      />
    )

    const input = screen.getByPlaceholderText('Válassz vagy írj be egy várost...')
    fireEvent.focus(input)
    expect(screen.getByText('Szeged')).toBeTruthy()
  })
})
