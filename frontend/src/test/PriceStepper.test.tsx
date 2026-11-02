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

  it('increments value on plus button click with default 1 M Ft step', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Minimális ár" value={73} onChange={handleChange} />)
    const plusBtn = screen.getByLabelText('Növelés: Minimális ár')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(74)
  })

  it('decrements value on minus button click with default 1 M Ft step', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Minimális ár" value={73} onChange={handleChange} />)
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális ár')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(72)
  })

  it('allows arbitrary numeric manual input without mid-typing clamping', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Maximális ár" value={120} onChange={handleChange} min={0} />)
    const input = screen.getByDisplayValue('120')
    fireEvent.change(input, { target: { value: '2' } })
    expect(handleChange).toHaveBeenCalledWith(2)
    fireEvent.change(input, { target: { value: '200' } })
    expect(handleChange).toHaveBeenCalledWith(200)
  })

  it('allows temporarily clearing input while typing and restores on blur if left empty', () => {
    const handleChange = vi.fn()
    render(<PriceStepper label="Maximális ár" value={120} onChange={handleChange} />)
    const input = screen.getByDisplayValue('120') as HTMLInputElement
    fireEvent.change(input, { target: { value: '' } })
    expect(input.value).toBe('')
    expect(handleChange).not.toHaveBeenCalled()

    fireEvent.blur(input)
    expect(input.value).toBe('120')
  })

  it('respects min and max bounds on stepper buttons', () => {
    const handleChange = vi.fn()
    const { rerender } = render(
      <PriceStepper label="Minimális ár" value={10} onChange={handleChange} min={10} max={100} step={1} />
    )
    const minusBtn = screen.getByLabelText('Csökkentés: Minimális ár')
    fireEvent.click(minusBtn)
    expect(handleChange).toHaveBeenCalledWith(10)

    rerender(
      <PriceStepper label="Minimális ár" value={100} onChange={handleChange} min={10} max={100} step={1} />
    )
    const plusBtn = screen.getByLabelText('Növelés: Minimális ár')
    fireEvent.click(plusBtn)
    expect(handleChange).toHaveBeenCalledWith(100)
  })
})
