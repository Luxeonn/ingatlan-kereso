import type { FC } from 'react'
import { Sparkles } from 'lucide-react'
import { useCities } from '../../hooks/useCities'

interface PresetButtonsProps {
  onSelectPreset: (cities: string[]) => void
}

export const PresetButtons: FC<PresetButtonsProps> = ({ onSelectPreset }) => {
  const { data: citiesData } = useCities()

  const presets = citiesData?.presets || {
    'Debrecen és agglomeráció': ['Debrecen', 'Mikepércs', 'Hajdúsámson', 'Bocskaikert', 'Józsa', 'Ebes'],
    'Budapest és környéke': ['Budapest', 'Budaörs', 'Szentendre', 'Dunakeszi', 'Gödöllő', 'Érd'],
    'Egyetemvárosok': ['Debrecen', 'Szeged', 'Pécs', 'Győr', 'Miskolc'],
  }

  return (
    <div className="flex flex-wrap items-center gap-1.5 pt-1">
      <span className="flex items-center gap-1 text-[11px] font-semibold text-[var(--text-secondary)]">
        <Sparkles className="h-3 w-3 text-amber-400" /> Gyorssablonok:
      </span>
      {Object.entries(presets).map(([name, cityList]) => (
        <button
          key={name}
          type="button"
          onClick={() => onSelectPreset(cityList)}
          className="rounded-md border border-dashed border-[var(--border-color)] bg-[var(--bg-card)] px-2 py-0.5 text-[11px] font-medium text-[var(--text-secondary)] transition-colors hover:border-blue-500 hover:text-blue-400 hover:bg-blue-500/5"
        >
          {name}
        </button>
      ))}
    </div>
  )
}
