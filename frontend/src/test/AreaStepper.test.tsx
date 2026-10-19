import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { AreaStepper } from '../components/filters/AreaStepper'

describe('AreaStepper component', () => {
  it('renders label and current value', () => {
    render(<AreaStepper label="Minimális alapterület" value={90} onChange={() => {}} />)
    expect(screen.getByText('Minimális alapterület')).toBeTruthy()
    expect(screen.getByDisplayValue('90')).toBeTruthy()
    expect(screen.getByText('m²')).toBeTruthy()
  })

  it('increments value on plus button click with default 1 m² step', () => {
    const handleChange = vi.fn()
    render(<AreaStepper label="Minimális alapterület" value={63} onChange={handleChange} />)
    const plusBtn = screen.getByLabelText('Növelés: Minimális alapterület')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(64)
  })

  it('decrements value on minus button click with default 1 m² step', () => {
    const handleChange = vi.fn()
    render(<AreaStepper label="Minimális alapterület" value={63} onChange={handleChange} />)
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális alapterület')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(62)
  })

  it('allows arbitrary numeric manual input', () => {
    const handleChange = vi.fn()
    render(<AreaStepper label="Minimális alapterület" value={50} onChange={handleChange} />)
    const input = screen.getByDisplayValue('50')
    fireEvent.change(input, { target: { value: '77' } })
    expect(handleChange).toHaveBeenCalledWith(77)
  })

  it('respects min and max bounds', () => {
    const handleChange = vi.fn()
    const { rerender } = render(
      <AreaStepper label="Minimális alapterület" value={1} onChange={handleChange} min={1} max={500} step={1} />
    )
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális alapterület')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(1)

    rerender(
      <AreaStepper label="Minimális alapterület" value={500} onChange={handleChange} min={1} max={500} step={1} />
    )
    const plusBtn = screen.getByLabelText('Növelés: Minimális alapterület')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(500)
  })
})
