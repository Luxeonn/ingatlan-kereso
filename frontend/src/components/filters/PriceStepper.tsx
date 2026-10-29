import type { FC } from 'react'
import { NumericStepper, type NumericStepperProps } from './NumericStepper'

type PriceStepperProps = Omit<NumericStepperProps, 'unit'>

export const PriceStepper: FC<PriceStepperProps> = ({ min = 0, ...props }) => (
  <NumericStepper {...props} min={min} unit="M Ft" />
)
