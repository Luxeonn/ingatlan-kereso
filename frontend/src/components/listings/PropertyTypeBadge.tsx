import type { FC } from 'react'
import { getPropertyTypeConfig } from '../../utils/formatters'
import { cn } from '../../utils/cn'

export interface PropertyTypeBadgeProps {
  propertyType?: string | null
  className?: string
  showIcon?: boolean
}

export const PropertyTypeBadge: FC<PropertyTypeBadgeProps> = ({
  propertyType,
  className,
  showIcon = true,
}) => {
  const config = getPropertyTypeConfig(propertyType)
  const Icon = config.icon

  return (
    <span
      data-testid="property-type-badge"
      data-property-type={config.key}
      role="status"
      aria-label={`Ingatlan típusa: ${config.label}`}
      title={`Ingatlan típusa: ${config.label}`}
      className={cn(
        'inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-bold border tracking-wider transition-colors',
        config.colorClasses.badge,
        className
      )}
    >
      {showIcon && (
        <Icon className={cn('h-3 w-3 shrink-0', config.colorClasses.icon)} aria-hidden="true" />
      )}
      <span>{config.label}</span>
    </span>
  )
}
