import type { ChangeEvent, FC } from 'react'
import { Minus, Plus } from 'lucide-react'

interface PriceStepperProps {
  label: string
  value: number
  onChange: (val: number) => void
  min?: number
  max?: number
  step?: number
}

export const PriceStepper: FC<PriceStepperProps> = ({
  label,
  value,
  onChange,
  min = 0,
  max = 1000,
  step = 1,
}) => {
  const handleDecrement = () => {
    onChange(Math.max(min, value - step))
  }

  const handleIncrement = () => {
    onChange(Math.min(max, value + step))
  }

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const num = Number(e.target.value)
    if (!isNaN(num)) {
      onChange(Math.max(min, Math.min(max, num)))
    }
  }

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-semibold text-[var(--text-secondary)]">
        {label}
      </label>
      <div className="flex h-10 items-center overflow-hidden rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] shadow-inner transition-colors focus-within:border-blue-500">
        <button
          type="button"
          onClick={handleDecrement}
          aria-label={`Csökkentés: ${label}`}
          className="flex h-full w-9 items-center justify-center text-[var(--text-secondary)] transition-colors hover:bg-black/10 hover:text-[var(--text-primary)] active:bg-black/20"
        >
          <Minus className="h-3.5 w-3.5" />
        </button>

        <div className="relative flex flex-1 items-center justify-center">
          <input
            type="number"
            value={value}
            min={min}
            max={max}
            step={step}
            onChange={handleChange}
            className="w-full bg-transparent text-center font-bold text-[var(--text-primary)] focus:outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
          />
          <span className="pointer-events-none absolute right-2 text-xs font-medium text-[var(--text-secondary)]">
            M Ft
          </span>
        </div>

        <button
          type="button"
          onClick={handleIncrement}
          aria-label={`Növelés: ${label}`}
          className="flex h-full w-9 items-center justify-center text-[var(--text-secondary)] transition-colors hover:bg-black/10 hover:text-[var(--text-primary)] active:bg-black/20"
        >
          <Plus className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  )
}
