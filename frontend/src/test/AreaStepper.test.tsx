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

  it('allows arbitrary numeric manual input without mid-typing clamping', () => {
    const handleChange = vi.fn()
    render(<AreaStepper label="Maximális alapterület" value={120} onChange={handleChange} min={1} />)
    const input = screen.getByDisplayValue('120')
    fireEvent.change(input, { target: { value: '2' } })
    expect(handleChange).toHaveBeenCalledWith(2)
    fireEvent.change(input, { target: { value: '200' } })
    expect(handleChange).toHaveBeenCalledWith(200)
  })

  it('allows temporarily clearing input while typing and restores on blur if left empty', () => {
    const handleChange = vi.fn()
    render(<AreaStepper label="Maximális alapterület" value={150} onChange={handleChange} />)
    const input = screen.getByDisplayValue('150') as HTMLInputElement
    fireEvent.change(input, { target: { value: '' } })
    expect(input.value).toBe('')
    expect(handleChange).not.toHaveBeenCalled()

    fireEvent.blur(input)
    expect(input.value).toBe('150')
  })

  it('respects min and max bounds on stepper buttons', () => {
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
