import type { FC } from 'react'

export const ListingSkeleton: FC = () => {
  return (
    <div
      data-testid="listing-skeleton"
      className="flex flex-col overflow-hidden rounded-[20px] glass-surface p-4 animate-shimmer"
    >
      {/* Cover image shimmer */}
      <div className="mb-3 aspect-[16/10] w-full rounded-[14px] bg-[#0b1220]/[0.07]" />

      {/* Header shimmer */}
      <div className="flex items-center justify-between pb-2">
        <div className="h-5 w-20 rounded-full bg-[#0b1220]/[0.07]" />
        <div className="h-5 w-24 rounded-full bg-[#0b1220]/[0.07]" />
      </div>

      {/* Title shimmer */}
      <div className="mt-1.5 h-5 w-3/4 rounded-md bg-[#0b1220]/[0.07]" />
      <div className="mt-2 h-4 w-1/2 rounded-md bg-[#0b1220]/[0.07]" />

      {/* Details chips shimmer */}
      <div className="mt-3.5 flex gap-2">
        <div className="h-6 w-16 rounded-full bg-[#0b1220]/[0.07]" />
        <div className="h-6 w-16 rounded-full bg-[#0b1220]/[0.07]" />
        <div className="h-6 w-24 rounded-full bg-[#0b1220]/[0.07]" />
      </div>

      {/* Footer shimmer */}
      <div className="mt-4 flex items-center justify-between border-t border-[var(--border-hairline)] pt-3">
        <div className="h-5 w-28 rounded-md bg-[#0b1220]/[0.07]" />
        <div className="h-7 w-20 rounded-full bg-[#0b1220]/[0.07]" />
      </div>
    </div>
  )
}
