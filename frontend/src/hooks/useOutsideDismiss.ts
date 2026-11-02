import { useEffect, type RefObject } from 'react'

export function useOutsideDismiss(
  containerRef: RefObject<HTMLElement | null>,
  isOpen: boolean,
  onClose: () => void,
  listenEscape = false,
): void {
  useEffect(() => {
    if (!isOpen) return

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
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
  }, [containerRef, isOpen, onClose, listenEscape])
}
