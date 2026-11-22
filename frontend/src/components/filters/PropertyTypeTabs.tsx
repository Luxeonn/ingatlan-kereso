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
    <div className="flex flex-col gap-2.5 h-full justify-between">
      <label className="text-sm font-bold text-[var(--text-primary)]">
        Ingatlan Típusa
      </label>
      <div className="grid grid-cols-3 gap-2">
        {TYPES.map((t) => {
          const isActive = (value || '').toUpperCase() === t.id
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => onChange(t.id)}
              className={cn(
                'flex h-10 items-center justify-center rounded-lg px-3 text-xs font-bold transition-all duration-150 cursor-pointer',
                isActive
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'border border-[var(--border-color)] bg-[var(--bg-card)] text-[var(--text-primary)] hover:border-blue-500/50 hover:text-blue-600'
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
