import { useState, type ChangeEvent, type FC } from 'react'
import { Minus, Plus } from 'lucide-react'

export interface NumericStepperProps {
  label: string
  value: number
  onChange: (val: number) => void
  unit: string
  min?: number
  max?: number
  step?: number
}

export const NumericStepper: FC<NumericStepperProps> = ({
  label,
  value,
  onChange,
  unit,
  min,
  max,
  step = 1,
}) => {
  const [draftValue, setDraftValue] = useState<string | null>(null)

  const handleDecrement = () => {
    const next = min !== undefined ? Math.max(min, value - step) : value - step
    setDraftValue(null)
    onChange(next)
  }

  const handleIncrement = () => {
    const next = max !== undefined ? Math.min(max, value + step) : value + step
    setDraftValue(null)
    onChange(next)
  }

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    setDraftValue(val)
    if (val.trim() === '' || val.trim() === '-') {
      return
    }
    const num = Number(val)
    if (Number.isFinite(num)) {
      onChange(num)
    }
  }

  const handleBlur = () => {
    if (draftValue !== null && draftValue.trim() !== '' && draftValue.trim() !== '-') {
      const num = Number(draftValue)
      if (Number.isFinite(num)) {
        onChange(num)
      }
    }
    setDraftValue(null)
  }

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-[11px] font-bold text-[var(--text-primary)] truncate">
        {label}
      </label>
      <div className="flex h-9 items-center overflow-hidden rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] transition-colors focus-within:border-[#0071e3]">
        <button
          type="button"
          onClick={handleDecrement}
          aria-label={`Csökkentés: ${label}`}
          className="flex h-full w-7 shrink-0 items-center justify-center text-[var(--text-secondary)] transition-colors hover:bg-black/5 hover:text-[var(--text-primary)] active:bg-black/10 cursor-pointer"
        >
          <Minus className="h-3 w-3" />
        </button>

        <div className="relative flex min-w-0 flex-1 items-center">
          <input
            type="number"
            value={draftValue ?? String(value)}
            min={min}
            max={max}
            step={step}
            onChange={handleChange}
            onBlur={handleBlur}
            className="w-full min-w-0 bg-transparent pl-1 pr-8 text-center text-xs font-bold text-[var(--text-primary)] tabular-nums focus:outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
          />
          <span className="pointer-events-none absolute right-1.5 text-[10px] font-semibold text-[var(--text-secondary)]">
            {unit}
          </span>
        </div>

        <button
          type="button"
          onClick={handleIncrement}
          aria-label={`Növelés: ${label}`}
          className="flex h-full w-7 shrink-0 items-center justify-center text-[var(--text-secondary)] transition-colors hover:bg-black/5 hover:text-[var(--text-primary)] active:bg-black/10 cursor-pointer"
        >
          <Plus className="h-3 w-3" />
        </button>
      </div>
    </div>
  )
}
