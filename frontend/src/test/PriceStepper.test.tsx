import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PriceStepper } from '../components/filters/PriceStepper'

describe('PriceStepper component', () => {
  it('renders label and current value', () => {
    render(<PriceStepper label="Minimális ár" value={70} onChange={() => {}} />)
    expect(screen.getByText('Minimális ár')).toBeTruthy()
    expect(screen.getByDisplayValue('70')).toBeTruthy()
    expect(screen.getByText('M Ft')).toBeTruthy()
  })

  it('increments value on plus button click', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Minimális ár" value={70} onChange={handleChange} step={5} />)
    const plusBtn = screen.getByLabelText('Növelés: Minimális ár')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(75)
  })

  it('decrements value on minus button click', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Minimális ár" value={70} onChange={handleChange} step={5} />)
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális ár')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(65)
  })

  it('respects min and max bounds', () => {
    const handleChange = vi.fn()
    const { rerender } = render(
      <PriceStepper label="Minimális ár" value={10} onChange={handleChange} min={10} max={100} step={5} />
    )
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális ár')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(10)

    rerender(
      <PriceStepper label="Minimális ár" value={100} onChange={handleChange} min={10} max={100} step={5} />
    )
    const plusBtn = screen.getByLabelText('Növelés: Minimális ár')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(100)
  })
})
