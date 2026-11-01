import { describe, expect, it } from 'vitest'
import {
  createMockIngatlanItem,
  createMockMarketStats,
  createMockPortalStatus,
} from './testFixtures'

describe('testFixtures Object Mother factories', () => {
  it('builds valid default and overridden IngatlanItem instances', () => {
    const def = createMockIngatlanItem()
    expect(def.id).toBe('mock-1')
    expect(def.city).toBe('Debrecen')
    expect(def.priceInfo?.amountMillion).toBe(45)

    const custom = createMockIngatlanItem({ id: 'custom-9', city: 'Szeged', isBargain: true })
    expect(custom.id).toBe('custom-9')
    expect(custom.city).toBe('Szeged')
    expect(custom.isBargain).toBe(true)
  })

  it('builds valid MarketStatsDto and PortalStatusDto instances', () => {
    const stats = createMockMarketStats({ bargainCount: 4 })
    expect(stats.bargainCount).toBe(4)
    expect(stats.insufficientData).toBe(false)

    const status = createMockPortalStatus({ portalId: 'oc', foundCount: 5 })
    expect(status.portalId).toBe('oc')
    expect(status.foundCount).toBe(5)
  })
})
