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

  it('renders dropdown via document.body portal and supports outside mousedown and Escape dismissal', () => {
    vi.mocked(useCitiesModule.useCities).mockReturnValue({
      data: {
        predefined: ['Debrecen', 'Budapest', 'Szeged'],
        presets: {},
        trackedCities: {},
      },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useCitiesModule.useCities>)

    const handleAdd = vi.fn()
    const { container } = render(
      <div>
        <div data-testid="outside-target">Outside</div>
        <CitySelector
          selectedCities={[]}
          onAddCity={handleAdd}
          onRemoveCity={vi.fn()}
          onSetCities={vi.fn()}
        />
      </div>,
    )

    const input = screen.getByPlaceholderText('Válassz vagy írj be egy várost...')
    fireEvent.focus(input)

    const dropdown = screen.getByTestId('city-selector-dropdown')
    expect(dropdown.parentElement).toBe(document.body)
    expect(container.contains(dropdown)).toBe(false)

    // Clicking inside portaled dropdown must NOT dismiss before selection
    fireEvent.mouseDown(screen.getByText('Budapest'))
    fireEvent.click(screen.getByText('Budapest'))
    expect(handleAdd).toHaveBeenCalledWith('Budapest')
    expect(screen.queryByTestId('city-selector-dropdown')).toBeNull()

    // Re-open and dismiss with outside mousedown
    fireEvent.focus(input)
    expect(screen.getByTestId('city-selector-dropdown')).toBeTruthy()
    fireEvent.mouseDown(screen.getByTestId('outside-target'))
    expect(screen.queryByTestId('city-selector-dropdown')).toBeNull()

    // Re-open and dismiss with Escape
    fireEvent.focus(input)
    expect(screen.getByTestId('city-selector-dropdown')).toBeTruthy()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByTestId('city-selector-dropdown')).toBeNull()
  })

  it('auto-closes portaled dropdown when anchor scrolls outside scroll container or below sticky header', () => {
    vi.mocked(useCitiesModule.useCities).mockReturnValue({
      data: {
        predefined: ['Debrecen', 'Budapest', 'Szeged'],
        presets: {},
        trackedCities: {},
      },
      isLoading: false,
      isError: false,
    } as unknown as ReturnType<typeof useCitiesModule.useCities>)

    const { container } = render(
      <div className="overflow-y-auto" data-testid="sidebar-scroll-container">
        <CitySelector
          selectedCities={[]}
          onAddCity={vi.fn()}
          onRemoveCity={vi.fn()}
          onSetCities={vi.fn()}
        />
      </div>,
    )

    const scrollContainer = screen.getByTestId('sidebar-scroll-container')
    const input = screen.getByPlaceholderText('Válassz vagy írj be egy várost...')
    const anchorEl = input.parentElement?.parentElement as HTMLElement

    vi.spyOn(scrollContainer, 'getBoundingClientRect').mockReturnValue({
      top: 76,
      bottom: 400,
      left: 0,
      right: 304,
      width: 304,
      height: 324,
      x: 0,
      y: 76,
      toJSON: () => ({}),
    })

    const anchorRectSpy = vi.spyOn(anchorEl, 'getBoundingClientRect').mockReturnValue({
      top: 120,
      bottom: 158,
      left: 24,
      right: 280,
      width: 256,
      height: 38,
      x: 24,
      y: 120,
      toJSON: () => ({}),
    })

    fireEvent.focus(input)
    expect(screen.getByTestId('city-selector-dropdown')).toBeTruthy()
    expect(container.contains(screen.getByTestId('city-selector-dropdown'))).toBe(false)

    // Scroll anchor above scroll container top bound -> should auto-close
    anchorRectSpy.mockReturnValue({
      top: 40,
      bottom: 78,
      left: 24,
      right: 280,
      width: 256,
      height: 38,
      x: 24,
      y: 40,
      toJSON: () => ({}),
    })

    fireEvent.scroll(scrollContainer)
    expect(screen.queryByTestId('city-selector-dropdown')).toBeNull()
  })
})
