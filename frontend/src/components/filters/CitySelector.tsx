import {
  useState,
  useRef,
  useMemo,
  useCallback,
  useEffect,
  type FC,
  type KeyboardEvent,
} from 'react'
import { createPortal } from 'react-dom'
import { MapPin, X, ChevronDown } from 'lucide-react'
import { useCities } from '../../hooks/useCities'
import { useOutsideDismiss } from '../../hooks/useOutsideDismiss'
import { PresetButtons } from './PresetButtons'

interface CitySelectorProps {
  selectedCities: string[]
  onAddCity: (city: string) => void
  onRemoveCity: (city: string) => void
  onSetCities: (cities: string[]) => void
}

interface DropdownPosition {
  top: number
  left: number
  width: number
}

const DEFAULT_CITIES = [
  'Debrecen', 'Budapest', 'Szeged', 'Miskolc', 'Pécs', 'Győr', 'Nyíregyháza', 'Kecskemét',
  'Székesfehérvár', 'Szombathely', 'Szolnok', 'Tatabánya', 'Kaposvár', 'Békéscsaba', 'Zalaegerszeg',
  'Veszprém', 'Eger', 'Érd', 'Sopron', 'Mikepércs', 'Hajdúsámson', 'Bocskaikert', 'Józsa', 'Ebes',
]

function extractCityName(item: unknown): string {
  if (typeof item === 'string') return item.trim()
  if (item && typeof item === 'object') {
    const rec = item as { name?: string; key?: string }
    return (rec.name || rec.key || '').trim()
  }
  return ''
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
  const [dropdownPos, setDropdownPos] = useState<DropdownPosition>({
    top: 0,
    left: 0,
    width: 268,
  })
  const containerRef = useRef<HTMLDivElement>(null)
  const anchorRef = useRef<HTMLDivElement>(null)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const extraRefs = useMemo(() => [dropdownRef], [])
  const closeDropdown = useCallback(() => setIsOpen(false), [])

  useOutsideDismiss(containerRef, isOpen, closeDropdown, true, extraRefs)

  const updateDropdownPosition = useCallback(() => {
    const anchor = anchorRef.current
    if (!anchor) return
    const rect = anchor.getBoundingClientRect()
    if (rect.height > 0 && rect.width > 0) {
      if (rect.bottom < 56) {
        setIsOpen(false)
        return
      }
      const scrollContainer = anchor.closest('.overflow-y-auto')
      if (scrollContainer) {
        const scrollRect = scrollContainer.getBoundingClientRect()
        if (
          scrollRect.height > 0 &&
          (rect.bottom <= scrollRect.top + 4 || rect.top >= scrollRect.bottom - 4)
        ) {
          setIsOpen(false)
          return
        }
      }
    }
    setDropdownPos({
      top: rect.bottom + 4,
      left: rect.left,
      width: rect.width > 0 ? rect.width : 268,
    })
  }, [])

  useEffect(() => {
    if (!isOpen) return
    updateDropdownPosition()
    window.addEventListener('resize', updateDropdownPosition)
    window.addEventListener('scroll', updateDropdownPosition, true)
    return () => {
      window.removeEventListener('resize', updateDropdownPosition)
      window.removeEventListener('scroll', updateDropdownPosition, true)
    }
  }, [isOpen, selectedCities.length, updateDropdownPosition])

  const cityMetaMap = useMemo(() => {
    const map = new Map<string, { county?: string; description?: string }>()
    if (Array.isArray(citiesData?.predefined)) {
      for (const item of citiesData.predefined) {
        if (item && typeof item === 'object') {
          const name = (item.name || item.key || '').trim()
          if (name) {
            map.set(name.toLowerCase(), {
              county: item.county,
              description: item.description,
            })
          }
        }
      }
    }
    return map
  }, [citiesData?.predefined])

  const allCities = useMemo(() => {
    if (!citiesData?.predefined || !Array.isArray(citiesData.predefined)) {
      return DEFAULT_CITIES
    }
    const extracted = citiesData.predefined
      .map((item) => extractCityName(item))
      .filter((name): name is string => Boolean(name && name.length > 0))

    return extracted.length > 0 ? extracted : DEFAULT_CITIES
  }, [citiesData?.predefined])

  const filteredCities = useMemo(() => {
    const q = query.trim().toLowerCase()
    return allCities.filter((city) => {
      const meta = cityMetaMap.get(city.toLowerCase())
      const matchesCity = !q || city.toLowerCase().includes(q)
      const matchesCounty = Boolean(q && meta?.county?.toLowerCase().includes(q))
      const notSelected = !selectedCities.includes(city)
      return (matchesCity || matchesCounty) && notSelected
    })
  }, [allCities, cityMetaMap, query, selectedCities])

  const getTrackedCount = (city: string): number | null => {
    if (!citiesData?.trackedCities) return null
    if (Array.isArray(citiesData.trackedCities)) {
      const found = citiesData.trackedCities.find(
        (t) =>
          t.name?.toLowerCase() === city.toLowerCase() ||
          t.key?.toLowerCase() === city.toLowerCase(),
      )
      return found !== null && found !== undefined && typeof found.count === 'number'
        ? found.count
        : null
    }
    if (typeof citiesData.trackedCities === 'object' && citiesData.trackedCities !== null) {
      if (Object.prototype.hasOwnProperty.call(citiesData.trackedCities, city)) {
        const val = (citiesData.trackedCities as Record<string, unknown>)[city]
        return typeof val === 'number' ? val : null
      }
    }
    return null
  }

  const handleSelect = (city: string) => {
    onAddCity(city)
    setQuery('')
    setIsOpen(false)
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      const firstCity = filteredCities[0]
      if (firstCity) {
        handleSelect(firstCity)
      } else if (query.trim()) {
        handleSelect(query.trim())
      }
    }
  }

  const dropdownNode =
    isOpen && filteredCities.length > 0 && typeof document !== 'undefined'
      ? createPortal(
          <div
            ref={dropdownRef}
            data-testid="city-selector-dropdown"
            style={{
              top: `${dropdownPos.top}px`,
              left: `${dropdownPos.left}px`,
              width: `${dropdownPos.width}px`,
            }}
            className="fixed z-[45] max-h-60 overflow-y-auto rounded-xl border border-[var(--border-strong)] glass-elevated p-1 shadow-2xl"
          >
            {filteredCities.map((city) => {
              const meta = cityMetaMap.get(city.toLowerCase())
              const count = getTrackedCount(city)
              return (
                <button
                  key={city}
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleSelect(city)
                  }}
                  className="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-1.5 text-left text-xs font-medium text-[var(--text-primary)] hover:bg-[#0071e3] hover:text-white transition-colors cursor-pointer"
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="font-semibold">{city}</span>
                    {meta?.county && (
                      <span className="rounded bg-[var(--bg-input)] px-1.5 py-0.5 text-[10px] text-[var(--text-secondary)]">
                        {meta.county}
                      </span>
                    )}
                    {meta?.description && (
                      <span className="inline truncate text-[10px] opacity-70">
                        · {meta.description}
                      </span>
                    )}
                  </div>
                  {count !== null && count !== undefined && count > 0 && (
                    <span className="text-[10px] opacity-75 tabular-nums shrink-0">
                      {count} találat
                    </span>
                  )}
                </button>
              )
            })}
          </div>,
          document.body,
        )
      : null

  return (
    <div className="flex flex-col gap-2" ref={containerRef}>
      <label className="text-xs font-bold text-[var(--text-primary)]">
        Keresési Települések
      </label>

      <div
        ref={anchorRef}
        onClick={() => setIsOpen(true)}
        className="relative flex min-h-[38px] flex-wrap items-center gap-1.5 rounded-xl border border-[var(--border-hairline)] bg-[var(--bg-input)] p-1.5 transition-colors focus-within:border-[#0071e3]"
      >
        <MapPin className="ml-1 h-3.5 w-3.5 text-[#0071e3] shrink-0" />

        {selectedCities.map((city) => (
          <span
            key={city}
            className="flex items-center gap-1 rounded-lg bg-[#0071e3]/12 px-2 py-0.5 text-[11px] font-semibold text-[#005bb5] border border-[#0071e3]/25"
          >
            {city}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation()
                onRemoveCity(city)
              }}
              className="hover:text-rose-700 transition-colors cursor-pointer"
            >
              <X className="h-3 w-3" />
            </button>
          </span>
        ))}

        <div className="flex min-w-[100px] flex-1 items-center gap-1">
          <input
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setIsOpen(true)
            }}
            onFocus={() => setIsOpen(true)}
            onKeyDown={handleKeyDown}
            placeholder={
              selectedCities.length === 0
                ? 'Válassz vagy írj be egy várost...'
                : 'További város hozzáadása...'
            }
            className="min-w-0 flex-1 bg-transparent text-xs text-[var(--text-primary)] placeholder-[var(--text-secondary)] focus:outline-none"
          />

          <ChevronDown className="mr-1 h-3.5 w-3.5 text-[var(--text-secondary)] shrink-0 cursor-pointer" />
        </div>
      </div>

      {dropdownNode}

      <PresetButtons onSelectPreset={onSetCities} />
    </div>
  )
}
