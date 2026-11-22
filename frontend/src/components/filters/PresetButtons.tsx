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
    <div className="flex flex-wrap items-center gap-2 border-t border-[var(--border-hairline)] pt-3">
      <span className="flex items-center gap-1.5 text-xs font-bold text-[var(--text-primary)]">
        <Sparkles className="h-3.5 w-3.5 text-amber-700" /> Gyorssablonok:
      </span>
      {Object.entries(presets).map(([name, cityList]) => (
        <button
          key={name}
          type="button"
          onClick={() => onSelectPreset(cityList)}
          className="rounded-lg border border-dashed border-[var(--border-strong)] bg-[var(--bg-card)] px-2.5 py-1 text-xs font-semibold text-[var(--text-primary)] transition-colors hover:border-blue-500 hover:text-blue-600 hover:bg-blue-500/5 cursor-pointer"
        >
          {name}
        </button>
      ))}
    </div>
  )
}
