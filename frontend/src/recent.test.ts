import { describe, expect, it } from 'vitest'
import { bindRecentToUser, readLastOrganization, rememberProject, saveLastOrganization, useAllRecentProjects } from './recent'
import { renderHook } from '@testing-library/react'

describe('recent projects', () => {
  it('are kept for the same user and cleared when another user signs in', () => {
    bindRecentToUser('alice')
    rememberProject({ id: 'p1', organizationId: 'o1', title: 'Billing' })
    saveLastOrganization('o1')

    bindRecentToUser('alice')
    expect(renderHook(() => useAllRecentProjects()).result.current).toHaveLength(1)
    expect(readLastOrganization()).toBe('o1')

    bindRecentToUser('bob')
    expect(renderHook(() => useAllRecentProjects()).result.current).toHaveLength(0)
    expect(readLastOrganization()).toBe('')
  })
})
