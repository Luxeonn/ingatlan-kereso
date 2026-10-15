import { useState, useRef, useEffect, type FC, type KeyboardEvent } from 'react'
import { MapPin, X, ChevronDown } from 'lucide-react'
import { useCities } from '../../hooks/useCities'
import { PresetButtons } from './PresetButtons'

interface CitySelectorProps {
  selectedCities: string[]
  onAddCity: (city: string) => void
  onRemoveCity: (city: string) => void
  onSetCities: (cities: string[]) => void
}

export const CitySelector: FC<CitySelectorProps> = ({
  selectedCities,
  onAddCity,
  onRemoveCity,
  onSetCities,
}) => {
  const { data: citiesData } = useCities()
  const [query, setQuery] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const allCities = citiesData?.predefined || [
    'Debrecen', 'Budapest', 'Szeged', 'Miskolc', 'Pécs', 'Győr', 'Nyíregyháza', 'Kecskemét',
    'Székesfehérvár', 'Szombathely', 'Szolnok', 'Tatabánya', 'Kaposvár', 'Békéscsaba', 'Zalaegerszeg',
    'Veszprém', 'Eger', 'Érd', 'Sopron', 'Mikepércs', 'Hajdúsámson', 'Bocskaikert', 'Józsa', 'Ebes'
  ]

  const filteredCities = allCities.filter((city) => {
    const matches = city.toLowerCase().includes(query.toLowerCase())
    const notSelected = !selectedCities.includes(city)
    return matches && notSelected
  })

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSelect = (city: string) => {
    onAddCity(city)
    setQuery('')
    setIsOpen(false)
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      if (filteredCities.length > 0) {
        handleSelect(filteredCities[0])
      } else if (query.trim()) {
        handleSelect(query.trim())
      }
    }
  }

  return (
    <div className="flex flex-col gap-1.5" ref={containerRef}>
      <label className="text-xs font-semibold text-[var(--text-secondary)]">
        Keresési Települések (Többvárosos Szűrés)
      </label>

      {/* Chips box & input */}
      <div
        onClick={() => setIsOpen(true)}
        className="relative flex min-h-[42px] flex-wrap items-center gap-1.5 rounded-lg border border-[var(--border-color)] bg-[var(--bg-input)] p-1.5 transition-colors focus-within:border-blue-500"
      >
        <MapPin className="ml-1 h-4 w-4 text-blue-400 shrink-0" />

        {/* Selected chips */}
        {selectedCities.map((city) => (
          <span
            key={city}
            className="flex items-center gap-1 rounded-md bg-blue-500/15 px-2 py-0.5 text-xs font-semibold text-blue-400 border border-blue-500/30"
          >
            {city}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation()
                onRemoveCity(city)
              }}
              className="hover:text-red-400 transition-colors"
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        ))}

        {/* Text input */}
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setIsOpen(true)
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder={selectedCities.length === 0 ? 'Válassz vagy írj be egy várost...' : 'További város hozzáadása...'}
          className="flex-1 min-w-[140px] bg-transparent text-xs text-[var(--text-primary)] placeholder-[var(--text-secondary)] focus:outline-none"
        />

        <ChevronDown className="mr-1 h-4 w-4 text-[var(--text-secondary)] shrink-0 cursor-pointer" />

        {/* Dropdown menu */}
        {isOpen && filteredCities.length > 0 && (
          <div className="absolute left-0 top-full z-50 mt-1 max-h-56 w-full overflow-y-auto rounded-lg border border-[var(--border-color)] bg-[var(--bg-card)] p-1 shadow-2xl backdrop-blur-md">
            {filteredCities.map((city) => (
              <button
                key={city}
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  handleSelect(city)
                }}
                className="flex w-full items-center justify-between rounded-md px-3 py-1.5 text-left text-xs font-medium text-[var(--text-primary)] hover:bg-blue-600 hover:text-white transition-colors"
              >
                <span>{city}</span>
                {citiesData?.trackedCities?.[city] != null && (
                  <span className="text-[10px] opacity-75">
                    {citiesData.trackedCities[city]} találat
                  </span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Presets */}
      <PresetButtons onSelectPreset={onSetCities} />
    </div>
  )
}
