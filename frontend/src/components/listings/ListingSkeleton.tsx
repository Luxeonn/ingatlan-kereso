import type { FC } from 'react'

export const ListingSkeleton: FC = () => {
  return (
    <div className="flex flex-col overflow-hidden rounded-2xl border border-[var(--border-color)] bg-[var(--bg-card)] p-4 shadow-lg animate-shimmer">
      {/* Header shimmer */}
      <div className="flex items-center justify-between pb-3">
        <div className="h-6 w-20 rounded-md bg-white/5" />
        <div className="h-6 w-24 rounded-md bg-white/5" />
      </div>

      {/* Title shimmer */}
      <div className="mt-2 h-5 w-3/4 rounded-md bg-white/5" />
      <div className="mt-2 h-4 w-1/2 rounded-md bg-white/5" />

      {/* Details chips shimmer */}
      <div className="mt-4 flex gap-2">
        <div className="h-6 w-16 rounded-md bg-white/5" />
        <div className="h-6 w-16 rounded-md bg-white/5" />
        <div className="h-6 w-24 rounded-md bg-white/5" />
      </div>

      {/* Footer shimmer */}
      <div className="mt-6 flex items-center justify-between border-t border-[var(--border-color)] pt-3">
        <div className="h-4 w-28 rounded-md bg-white/5" />
        <div className="h-8 w-24 rounded-lg bg-white/5" />
      </div>
    </div>
  )
}
