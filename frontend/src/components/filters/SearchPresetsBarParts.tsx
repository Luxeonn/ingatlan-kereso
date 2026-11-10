import type { FC, FormEvent, MouseEvent, KeyboardEvent } from 'react'
import {
  Check,
  Pencil,
  Trash2,
  X,
  AlertCircle,
} from 'lucide-react'
import type { SearchPreset } from '../../types/api'

interface PresetFeedbackBannersProps {
  successMessage: string | null
  errorMessage: string | null
  onClearSuccess: () => void
  onClearError: () => void
}

export const PresetFeedbackBanners: FC<PresetFeedbackBannersProps> = ({
  successMessage,
  errorMessage,
  onClearSuccess,
  onClearError,
}) => {
  if (!successMessage && !errorMessage) return null

  return (
    <>
      {successMessage && (
        <div
          role="status"
          data-testid="preset-success-banner"
          className="inline-flex items-center gap-1 rounded-md border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-400 animate-in fade-in duration-150"
        >
          <Check className="h-3 w-3 shrink-0" />
          <span>{successMessage}</span>
          <button
            type="button"
            onClick={onClearSuccess}
            title="Értesítés bezárása"
            className="ml-1 text-emerald-400 hover:text-emerald-200 cursor-pointer"
          >
            <X className="h-3 w-3" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div
          role="alert"
          data-testid="preset-error-banner"
          className="inline-flex items-center gap-1 rounded-md border border-rose-500/30 bg-rose-500/10 px-2 py-0.5 text-xs font-medium text-rose-400 animate-in fade-in duration-150"
        >
          <AlertCircle className="h-3 w-3 shrink-0" />
          <span>{errorMessage}</span>
          <button
            type="button"
            onClick={onClearError}
            title="Hibaüzenet bezárása"
            className="ml-1 text-rose-400 hover:text-rose-200 cursor-pointer"
          >
            <X className="h-3 w-3" />
          </button>
        </div>
      )}
    </>
  )
}

interface PresetEditFormProps {
  presetId: number
  editingName: string
  isPending: boolean
  onNameChange: (val: string) => void
  onSave: (e: FormEvent | KeyboardEvent, id: number) => void
  onCancel: (e?: MouseEvent | KeyboardEvent) => void
}

export const PresetEditForm: FC<PresetEditFormProps> = ({
  presetId,
  editingName,
  isPending,
  onNameChange,
  onSave,
  onCancel,
}) => (
  <form
    onSubmit={(e) => onSave(e, presetId)}
    className="inline-flex items-center gap-1 rounded-lg border border-blue-500 bg-[var(--bg-input)] px-2 py-1"
  >
    <input
      type="text"
      value={editingName}
      onChange={(e) => onNameChange(e.target.value)}
      onKeyDown={(e) => {
        if (e.key === 'Enter') {
          onSave(e, presetId)
        } else if (e.key === 'Escape') {
          onCancel(e)
        }
      }}
      autoFocus
      className="w-36 bg-transparent text-xs text-[var(--text-primary)] focus:outline-none"
    />
    <button
      type="submit"
      title="Mentés"
      disabled={isPending}
      className="text-emerald-400 hover:text-emerald-300 disabled:opacity-40 cursor-pointer"
    >
      <Check className="h-3.5 w-3.5" />
    </button>
    <button
      type="button"
      onClick={onCancel}
      title="Mégse"
      className="text-[var(--text-secondary)] hover:text-[var(--text-primary)] cursor-pointer"
    >
      <X className="h-3.5 w-3.5" />
    </button>
  </form>
)

interface PresetChipProps {
  preset: SearchPreset
  isActive: boolean
  onSelect: (preset: SearchPreset) => void
  onStartEdit: (e: MouseEvent, preset: SearchPreset) => void
  onDelete: (e: MouseEvent, id: number) => void
}

export const PresetChip: FC<PresetChipProps> = ({
  preset,
  isActive,
  onSelect,
  onStartEdit,
  onDelete,
}) => (
  <div
    data-testid={`preset-chip-${preset.id}`}
    onClick={() => onSelect(preset)}
    role="button"
    tabIndex={0}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        onSelect(preset)
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
        onClick={(e) => onStartEdit(e, preset)}
        title="Sablon átnevezése"
        className="hover:text-blue-400 cursor-pointer"
      >
        <Pencil className="h-3 w-3" />
      </button>
      <button
        type="button"
        onClick={(e) => onDelete(e, preset.id)}
        title="Sablon törlése"
        className="hover:text-rose-400 cursor-pointer"
      >
        <Trash2 className="h-3 w-3" />
      </button>
    </div>
  </div>
)

interface PresetCreateFormProps {
  name: string
  isPending: boolean
  onNameChange: (val: string) => void
  onSave: (e?: FormEvent | KeyboardEvent) => void
  onCancel: () => void
}

export const PresetCreateForm: FC<PresetCreateFormProps> = ({
  name,
  isPending,
  onNameChange,
  onSave,
  onCancel,
}) => (
  <form
    onSubmit={onSave}
    className="inline-flex items-center gap-1.5 rounded-lg border border-blue-500 bg-[var(--bg-input)] px-2 py-1 shadow-md"
  >
    <input
      type="text"
      value={name}
      onChange={(e) => onNameChange(e.target.value)}
      onKeyDown={(e) => {
        if (e.key === 'Enter') {
          onSave(e)
        } else if (e.key === 'Escape') {
          onCancel()
        }
      }}
      placeholder="Sablon neve"
      autoFocus
      className="w-44 bg-transparent text-xs text-[var(--text-primary)] focus:outline-none"
    />
    <button
      type="submit"
      disabled={isPending}
      title="Mentés sablonként"
      className="flex items-center gap-0.5 rounded px-1.5 py-0.5 font-bold text-emerald-400 hover:bg-emerald-500/10 disabled:opacity-40 cursor-pointer"
    >
      <Check className="h-3.5 w-3.5" />
      <span>Mentés</span>
    </button>
    <button
      type="button"
      onClick={onCancel}
      title="Mégse"
      className="rounded p-0.5 text-[var(--text-secondary)] hover:bg-[var(--bg-hover)] hover:text-[var(--text-primary)] cursor-pointer"
    >
      <X className="h-3.5 w-3.5" />
    </button>
  </form>
)
