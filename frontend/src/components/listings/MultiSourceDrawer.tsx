import type { FC } from 'react'
import {
  PropertyDetailsDrawer,
  type PropertyDetailsDrawerProps,
} from './PropertyDetailsDrawer'

/**
 * Backward compatibility alias for PropertyDetailsDrawer (#260, #261, #270).
 */
export const MultiSourceDrawer: FC<PropertyDetailsDrawerProps> = (props) => {
  return <PropertyDetailsDrawer {...props} />
}
