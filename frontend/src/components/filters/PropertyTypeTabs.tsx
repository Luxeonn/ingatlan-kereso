import type { FC } from 'react'
import type { PropertyType } from '../../types/property'
import { cn } from '../../utils/cn'

export type FilterPropertyType = 'LAKAS' | 'HAZ' | 'TELEK' | 'lakas' | 'haz' | 'telek'

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
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-semibold text-[var(--text-secondary)]">
        Ingatlan Típusa
      </label>
      <div className="flex flex-wrap gap-1.5">
        {TYPES.map((t) => {
          const isActive = (value || '').toUpperCase() === t.id
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => onChange(t.id)}
              className={cn(
                'rounded-lg px-3 py-1.5 text-xs font-semibold transition-all duration-150',
                isActive
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'border border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:border-blue-500/50 hover:text-[var(--text-primary)]'
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
