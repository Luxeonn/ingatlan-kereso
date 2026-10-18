import type { FC } from 'react'
import { cn } from '../../utils/cn'

interface PropertyTypeTabsProps {
  value: string
  onChange: (type: string) => void
}

const TYPES = [
  { id: 'lakas', label: 'Lakás' },
  { id: 'haz', label: 'Ház' },
  { id: 'telek', label: 'Telek' },
]

export const PropertyTypeTabs: FC<PropertyTypeTabsProps> = ({ value, onChange }) => {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-xs font-semibold text-[var(--text-secondary)]">
        Ingatlan Típusa
      </label>
      <div className="flex flex-wrap gap-1.5">
        {TYPES.map((t) => {
          const isActive = (value || '').toLowerCase() === t.id.toLowerCase()
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
