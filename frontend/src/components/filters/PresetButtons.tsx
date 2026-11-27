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
    <div className="flex flex-wrap items-center gap-1.5 border-t border-[var(--border-hairline)] pt-2.5">
      <span className="flex items-center gap-1 text-[11px] font-bold text-[var(--text-primary)] shrink-0">
        <Sparkles className="h-3 w-3 text-amber-700" /> Gyorssablonok:
      </span>
      {Object.entries(presets).map(([name, cityList]) => (
        <button
          key={name}
          type="button"
          onClick={() => onSelectPreset(cityList)}
          className="inline-flex flex-1 min-w-[110px] items-center justify-center rounded-lg border border-dashed border-[var(--border-strong)] bg-white/75 px-2 py-1 text-[11px] font-semibold text-center text-[var(--text-primary)] transition-colors hover:border-[#0071e3] hover:text-[#0071e3] hover:bg-[#0071e3]/5 cursor-pointer"
        >
          {name}
        </button>
      ))}
    </div>
  )
}
