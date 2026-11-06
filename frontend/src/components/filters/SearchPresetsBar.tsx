import { useState, type FC, type FormEvent, type MouseEvent } from 'react'
import {
  Bookmark,
  BookmarkPlus,
  Check,
  Pencil,
  Trash2,
  X,
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

interface SearchPresetsBarProps {
  onSearch: () => void
}

function generateDefaultName(criteria: ReturnType<typeof useFilterStore.getState>['criteria']): string {
  const city = criteria.cities && criteria.cities.length > 0 ? criteria.cities.join(', ') : criteria.city || 'Keresés'
  const typeMap: Record<string, string> = { haz: 'Ház', lakas: 'Lakás', telek: 'Telek' }
  const typeStr = typeMap[criteria.propertyType] || criteria.propertyType
  return `${city} ${typeStr} ${criteria.minPriceMillion}-${criteria.maxPriceMillion}M`
}

export const SearchPresetsBar: FC<SearchPresetsBarProps> = ({ onSearch }) => {
  const { criteria, setCriteria } = useFilterStore()
  const { data, isLoading } = usePresets()
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

  const handleStartCreate = () => {
    setNewPresetName(generateDefaultName(criteria))
    setIsCreating(true)
    setEditingPresetId(null)
  }

  const handleCancelCreate = () => {
    setIsCreating(false)
    setNewPresetName('')
  }

  const handleSaveNew = (e: FormEvent) => {
    e.preventDefault()
    const trimmed = newPresetName.trim()
    if (!trimmed) return
    createMutation.mutate(
      { name: trimmed, criteria },
      {
        onSuccess: (saved) => {
          setIsCreating(false)
          setNewPresetName('')
          if (saved && typeof saved.id === 'number') {
            setActivePresetId(saved.id)
          }
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
  }

  const handleCancelEdit = (e: MouseEvent) => {
    e.stopPropagation()
    setEditingPresetId(null)
    setEditingName('')
  }

  const handleSaveEdit = (e: FormEvent, id: number) => {
    e.preventDefault()
    e.stopPropagation()
    const trimmed = editingName.trim()
    if (!trimmed) return
    updateMutation.mutate(
      { id, preset: { name: trimmed } },
      {
        onSuccess: () => {
          setEditingPresetId(null)
          setEditingName('')
        },
      },
    )
  }

  const handleDelete = (e: MouseEvent, id: number) => {
    e.stopPropagation()
    deleteMutation.mutate(id, {
      onSuccess: () => {
        if (activePresetId === id) {
          setActivePresetId(null)
        }
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

      {!isLoading && presets.length === 0 && !isCreating && (
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
            <form
              key={preset.id}
              onSubmit={(e) => handleSaveEdit(e, preset.id)}
              className="inline-flex items-center gap-1 rounded-lg border border-blue-500 bg-[var(--bg-input)] px-2 py-1"
            >
              <input
                type="text"
                value={editingName}
                onChange={(e) => setEditingName(e.target.value)}
                autoFocus
                className="w-36 bg-transparent text-xs text-[var(--text-primary)] focus:outline-none"
              />
              <button
                type="submit"
                title="Mentés"
                disabled={updateMutation.isPending}
                className="text-emerald-400 hover:text-emerald-300"
              >
                <Check className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                onClick={handleCancelEdit}
                title="Mégse"
                className="text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </form>
          )
        }

        return (
          <div
            key={preset.id}
            data-testid={`preset-chip-${preset.id}`}
            onClick={() => handleSelectPreset(preset)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                handleSelectPreset(preset)
              }
            }}
            className={`group inline-flex cursor-pointer items-center gap-1.5 rounded-lg border px-2.5 py-1 font-medium transition-all ${
              isActive
                ? 'border-blue-500 bg-blue-500/15 text-blue-400 shadow-sm'
                : 'border-[var(--border-color)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:border-blue-400/50 hover:text-[var(--text-primary)]'
            }`}
          >
            <span>{preset.name}</span>
            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                type="button"
                onClick={(e) => handleStartEdit(e, preset)}
                title="Sablon átnevezése"
                className="hover:text-blue-400"
              >
                <Pencil className="h-3 w-3" />
              </button>
              <button
                type="button"
                onClick={(e) => handleDelete(e, preset.id)}
                title="Sablon törlése"
                className="hover:text-rose-400"
              >
                <Trash2 className="h-3 w-3" />
              </button>
            </div>
          </div>
        )
      })}

      {/* Inline Create Form */}
      {isCreating ? (
        <form
          onSubmit={handleSaveNew}
          className="inline-flex items-center gap-1.5 rounded-lg border border-blue-500 bg-[var(--bg-input)] px-2 py-1 shadow-md"
        >
          <input
            type="text"
            value={newPresetName}
            onChange={(e) => setNewPresetName(e.target.value)}
            placeholder="Sablon neve"
            autoFocus
            className="w-44 bg-transparent text-xs text-[var(--text-primary)] focus:outline-none"
          />
          <button
            type="submit"
            disabled={createMutation.isPending || !newPresetName.trim()}
            title="Mentés sablonként"
            className="flex items-center gap-0.5 rounded px-1.5 py-0.5 font-bold text-emerald-400 hover:bg-emerald-500/10 disabled:opacity-40"
          >
            <Check className="h-3.5 w-3.5" />
            <span>Mentés</span>
          </button>
          <button
            type="button"
            onClick={handleCancelCreate}
            title="Mégse"
            className="rounded p-0.5 text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)]"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </form>
      ) : (
        <button
          type="button"
          onClick={handleStartCreate}
          data-testid="save-preset-button"
          className="inline-flex items-center gap-1 rounded-lg border border-dashed border-[var(--border-color)] bg-transparent px-2.5 py-1 font-semibold text-blue-400 transition-colors hover:border-blue-400 hover:bg-blue-500/10"
        >
          <BookmarkPlus className="h-3.5 w-3.5" />
          <span>Mentés sablonként</span>
        </button>
      )}
    </div>
  )
}
