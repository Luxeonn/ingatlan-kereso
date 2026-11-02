import type { FC } from 'react'
import { NumericStepper, type NumericStepperProps } from './NumericStepper'

type AreaStepperProps = Omit<NumericStepperProps, 'unit'>

export const AreaStepper: FC<AreaStepperProps> = ({ min = 1, ...props }) => (
  <NumericStepper {...props} min={min} unit="m²" />
)
