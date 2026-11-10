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
      className="mb-3 flex flex-wrap items-center gap-2 border-b border-[var(--border-color)] pb-3 text-xs"
    >
      <div className="flex items-center gap-1.5 font-semibold text-[var(--text-secondary)]">
        <Bookmark className="h-3.5 w-3.5 text-blue-400" />
        <span>Keresési sablonok:</span>
      </div>

      {isLoading && (
        <div className="flex items-center gap-1 text-[var(--text-secondary)]">
          <Loader2 className="h-3.5 w-3.5 animate-spin" />
          <span>Betöltés...</span>
        </div>
      )}

      {isError && (
        <span role="alert" className="text-rose-400 italic">
          Nem sikerült betölteni a keresési sablonokat.
        </span>
      )}

      {!isLoading && !isError && presets.length === 0 && !isCreating && (
        <span className="text-[var(--text-secondary)] italic">
          Nincsenek mentett sablonok.
        </span>
      )}

      {/* Preset Chips */}
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

      {/* Inline Create Form */}
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
          className="inline-flex items-center gap-1 rounded-lg border border-dashed border-[var(--border-color)] bg-transparent px-2.5 py-1 font-semibold text-blue-400 transition-colors hover:border-blue-400 hover:bg-blue-500/10 cursor-pointer"
        >
          <BookmarkPlus className="h-3.5 w-3.5" />
          <span>Mentés sablonként</span>
        </button>
      )}

      {/* Inline Status and Error Feedback Banners */}
      <PresetFeedbackBanners
        successMessage={successMessage}
        errorMessage={errorMessage}
        onClearSuccess={() => setSuccessMessage(null)}
        onClearError={() => setErrorMessage(null)}
      />
    </div>
  )
}
