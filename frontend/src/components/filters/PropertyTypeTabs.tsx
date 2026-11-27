import type { FC } from 'react'
import type { PropertyType } from '../../types/property'
import { cn } from '../../utils/cn'

interface PropertyTypeTabsProps {
  value: PropertyType | string
  onChange: (type: PropertyType) => void
}

const TYPES: Array<{ id: PropertyType; label: string }> = [
  { id: 'LAKAS', label: 'Lakás' },
  { id: 'HAZ', label: 'Ház' },
  { id: 'TELEK', label: 'Telek' },
]

export const PropertyTypeTabs: FC<PropertyTypeTabsProps> = ({ value, onChange }) => {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-xs font-bold text-[var(--text-primary)]">
        Ingatlan Típusa
      </label>
      <div className="grid grid-cols-3 gap-1.5">
        {TYPES.map((t) => {
          const isActive = (value || '').toUpperCase() === t.id
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => onChange(t.id)}
              className={cn(
                'flex h-9 items-center justify-center rounded-lg px-2 text-xs font-bold transition-all duration-150 cursor-pointer',
                isActive
                  ? 'bg-[#0071e3] text-white shadow-xs'
                  : 'border border-[var(--border-color)] bg-white/75 text-[var(--text-primary)] hover:border-[#0071e3]/50 hover:text-[#0071e3]'
              )}
            >
              {t.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}
