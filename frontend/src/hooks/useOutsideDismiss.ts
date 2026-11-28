import { useEffect, type RefObject } from 'react'

export function useOutsideDismiss(
  containerRef: RefObject<HTMLElement | null>,
  isOpen: boolean,
  onClose: () => void,
  listenEscape = false,
  extraRefs?: ReadonlyArray<RefObject<HTMLElement | null>>,
): void {
  useEffect(() => {
    if (!isOpen) return

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node
      const insideContainer = Boolean(containerRef.current?.contains(target))
      const insideExtra = Boolean(extraRefs?.some((ref) => ref.current?.contains(target)))
      if (!insideContainer && !insideExtra) {
        onClose()
      }
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    if (listenEscape) {
      document.addEventListener('keydown', handleKeyDown)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      if (listenEscape) {
        document.removeEventListener('keydown', handleKeyDown)
      }
    }
  }, [containerRef, extraRefs, isOpen, onClose, listenEscape])
}
