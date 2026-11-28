import { useState, useEffect, type FC, type FormEvent, type MouseEvent, type KeyboardEvent } from 'react'
import {
  Bookmark,
  BookmarkPlus,
  Loader2,
} from 'lucide-react'
import {
  usePresets,
  useCreatePreset,
  useUpdatePreset,
  useDeletePreset,
  useApplyPreset,
} from '../../hooks/usePresets'
import { useFilterStore } from '../../store/useFilterStore'
import type { SearchPreset } from '../../types/api'
import {
  PresetFeedbackBanners,
  PresetEditForm,
  PresetChip,
  PresetCreateForm,
} from './SearchPresetsBarParts'

interface SearchPresetsBarProps {
  onSearch: () => void
}

function generateDefaultName(criteria: ReturnType<typeof useFilterStore.getState>['criteria']): string {
  const city = criteria.cities && criteria.cities.length > 0 ? criteria.cities.join(', ') : criteria.city || 'Keresés'
  const typeMap: Record<string, string> = {
    HAZ: 'Ház',
    LAKAS: 'Lakás',
    TELEK: 'Telek',
    haz: 'Ház',
    lakas: 'Lakás',
    telek: 'Telek',
  }
  const propType = criteria.propertyType || ''
  const typeStr = Object.prototype.hasOwnProperty.call(typeMap, propType)
    ? (typeMap[propType] ?? propType)
    : propType
  return `${city} ${typeStr} ${criteria.minPriceMillion}-${criteria.maxPriceMillion}M`
}

export const SearchPresetsBar: FC<SearchPresetsBarProps> = ({ onSearch }) => {
  const { criteria, setCriteria } = useFilterStore()
  const { data, isLoading, isError } = usePresets()
  const presets = Array.isArray(data) ? data : []
  const createMutation = useCreatePreset()
  const updateMutation = useUpdatePreset()
  const deleteMutation = useDeletePreset()
  const applyMutation = useApplyPreset()

  const [activePresetId, setActivePresetId] = useState<number | null>(null)
  const [isCreating, setIsCreating] = useState(false)
  const [newPresetName, setNewPresetName] = useState('')
  const [editingPresetId, setEditingPresetId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!successMessage) return
    const timer = setTimeout(() => {
      setSuccessMessage(null)
    }, 4000)
    return () => clearTimeout(timer)
  }, [successMessage])

  const handleStartCreate = () => {
    setNewPresetName(generateDefaultName(criteria))
    setIsCreating(true)
    setEditingPresetId(null)
    setErrorMessage(null)
  }

  const handleCancelCreate = () => {
    setIsCreating(false)
    setNewPresetName('')
    setErrorMessage(null)
  }

  const handleSaveNew = (e?: FormEvent | KeyboardEvent) => {
    if (e) {
      e.preventDefault()
      e.stopPropagation()
    }
    const trimmed = newPresetName.trim()
    if (!trimmed) {
      setErrorMessage('A sablon neve nem lehet üres.')
      return
    }
    setErrorMessage(null)
    createMutation.mutate(
      { name: trimmed, criteria },
      {
        onSuccess: (saved) => {
          setIsCreating(false)
          setNewPresetName('')
          setErrorMessage(null)
          const name = saved?.name || trimmed
          setSuccessMessage(`Sablon sikeresen elmentve: "${name}"`)
          if (saved && typeof saved.id === 'number') {
            setActivePresetId(saved.id)
          }
        },
        onError: (err: unknown) => {
          const msg = err instanceof Error ? err.message : 'Nem sikerült menteni a keresési sablont.'
          setErrorMessage(msg)
        },
      },
    )
  }

  const handleSelectPreset = (preset: SearchPreset) => {
    if (!preset.criteria) return
    setActivePresetId(preset.id)
    setCriteria(preset.criteria)
    applyMutation.mutate(preset.id)
    onSearch()
  }

  const handleStartEdit = (e: MouseEvent, preset: SearchPreset) => {
    e.stopPropagation()
    setEditingPresetId(preset.id)
    setEditingName(preset.name)
    setIsCreating(false)
    setErrorMessage(null)
  }

  const handleCancelEdit = (e?: MouseEvent | KeyboardEvent) => {
    if (e) e.stopPropagation()
    setEditingPresetId(null)
    setEditingName('')
    setErrorMessage(null)
  }

  const handleSaveEdit = (e: FormEvent | KeyboardEvent, id: number) => {
    e.preventDefault()
    e.stopPropagation()
    const trimmed = editingName.trim()
    if (!trimmed) {
      setErrorMessage('A sablon neve nem lehet üres.')
      return
    }
    setErrorMessage(null)
    updateMutation.mutate(
      { id, preset: { name: trimmed } },
      {
        onSuccess: () => {
          setEditingPresetId(null)
          setEditingName('')
          setErrorMessage(null)
          setSuccessMessage(`Sablon sikeresen módosítva: "${trimmed}"`)
        },
        onError: (err: unknown) => {
          const msg = err instanceof Error ? err.message : 'Nem sikerült módosítani a sablont.'
          setErrorMessage(msg)
        },
      },
    )
  }

  const handleDelete = (e: MouseEvent, id: number) => {
    e.stopPropagation()
    setErrorMessage(null)
    deleteMutation.mutate(id, {
      onSuccess: () => {
        if (activePresetId === id) {
          setActivePresetId(null)
        }
        setErrorMessage(null)
        setSuccessMessage('Sablon sikeresen törölve.')
      },
      onError: (err: unknown) => {
        const msg = err instanceof Error ? err.message : 'Nem sikerült törölni a sablont.'
        setErrorMessage(msg)
      },
    })
  }

  return (
    <div
      data-testid="search-presets-bar"
      className="mt-2.5 rounded-xl border border-[var(--border-hairline)] bg-white/55 p-2.5"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--border-hairline)] pb-2">
        <div className="flex items-center gap-1.5">
          <Bookmark className="h-3.5 w-3.5 text-[#0071e3]" />
          <span className="text-xs font-bold text-[var(--text-primary)]">Keresési sablonok</span>
          {presets.length > 0 && (
            <span className="rounded-full bg-white/80 border border-[var(--border-hairline)] px-1.5 py-0.5 text-[10px] font-bold text-[var(--text-secondary)] tabular-nums">
              {presets.length}
            </span>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-1.5 w-full justify-end">
          <PresetFeedbackBanners
            successMessage={successMessage}
            errorMessage={errorMessage}
            onClearSuccess={() => setSuccessMessage(null)}
            onClearError={() => setErrorMessage(null)}
          />

          {isCreating ? (
            <PresetCreateForm
              name={newPresetName}
              isPending={createMutation.isPending}
              onNameChange={(val) => {
                setNewPresetName(val)
                if (errorMessage) setErrorMessage(null)
              }}
              onSave={handleSaveNew}
              onCancel={handleCancelCreate}
            />
          ) : (
            <button
              type="button"
              onClick={handleStartCreate}
              data-testid="save-preset-button"
              className="inline-flex items-center gap-1 rounded-lg border border-[#0071e3]/30 bg-[#0071e3]/10 px-2.5 py-1 text-[11px] font-bold text-[#005bb5] shadow-xs transition-all hover:bg-[#0071e3] hover:text-white cursor-pointer"
            >
              <BookmarkPlus className="h-3 w-3" />
              <span>Mentés sablonként</span>
            </button>
          )}
        </div>
      </div>

      <div className="flex max-h-28 flex-wrap items-center justify-center gap-1.5 overflow-y-auto pt-2 text-xs">
        {isLoading && (
          <div className="flex items-center gap-1.5 text-[var(--text-secondary)]">
            <Loader2 className="h-3.5 w-3.5 animate-spin text-blue-600" />
            <span>Betöltés...</span>
          </div>
        )}

        {isError && (
          <span role="alert" className="font-medium text-rose-700">
            Nem sikerült betölteni a keresési sablonokat.
          </span>
        )}

        {!isLoading && !isError && presets.length === 0 && !isCreating && (
          <span className="text-[var(--text-secondary)] italic">
            Nincsenek mentett sablonok.
          </span>
        )}

        {presets.map((preset) => {
          const isActive = activePresetId === preset.id
          const isEditing = editingPresetId === preset.id

          if (isEditing) {
            return (
              <PresetEditForm
                key={preset.id}
                presetId={preset.id}
                editingName={editingName}
                isPending={updateMutation.isPending}
                onNameChange={(val) => {
                  setEditingName(val)
                  if (errorMessage) setErrorMessage(null)
                }}
                onSave={handleSaveEdit}
                onCancel={handleCancelEdit}
              />
            )
          }

          return (
            <PresetChip
              key={preset.id}
              preset={preset}
              isActive={isActive}
              onSelect={handleSelectPreset}
              onStartEdit={handleStartEdit}
              onDelete={handleDelete}
            />
          )
        })}
      </div>
    </div>
  )
}
