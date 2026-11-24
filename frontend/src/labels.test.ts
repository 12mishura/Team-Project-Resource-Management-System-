import { describe, expect, it } from 'vitest'
import type { ProjectActivity } from './api'
import { describeActivity, fieldValue, formatRelative, initials, noun, plural, readMetadata, toOptions } from './labels'

const activity = (overrides: Partial<ProjectActivity>): ProjectActivity => ({
  id: 'event',
  issueId: 'issue',
  issueTitle: 'Fix login',
  issueType: 'BUG',
  type: 'CREATED',
  field: null,
  oldValue: null,
  newValue: null,
  actorUsername: 'demo_dev',
  createdAt: new Date().toISOString(),
  ...overrides,
})

describe('labels', () => {
  it('builds initials from the first and last name parts', () => {
    expect(initials('demo_dev')).toBe('DD')
    expect(initials('demo_lead')).toBe('DL')
    expect(initials('alice')).toBe('AL')
    expect(initials('Mary Jane Watson')).toBe('MW')
  })

  it('pluralizes counts', () => {
    expect(plural(1, 'bug')).toBe('1 bug')
    expect(plural(0, 'bug')).toBe('0 bugs')
    expect(plural(3, 'person', 'people')).toBe('3 people')
    expect(noun(1, 'issue')).toBe('issue')
  })

  it('shows readable values for known fields', () => {
    expect(fieldValue('priority', 'HIGH')).toBe('High')
    expect(fieldValue('status', 'IN_REVIEW')).toBe('In review')
    expect(fieldValue('title', 'IN_REVIEW')).toBe('IN_REVIEW')
  })

  it('formats recent and old dates', () => {
    expect(formatRelative(new Date().toISOString())).toBe('just now')
    expect(formatRelative(new Date(Date.now() - 3 * 3600 * 1000).toISOString())).toBe('3 hours ago')
    expect(formatRelative('2020-01-15T10:00:00Z')).toMatch(/Jan 15/)
  })

  it('describes project activity as sentence parts', () => {
    expect(describeActivity(activity({ type: 'CREATED' }))).toEqual(['created bug', ''])
    expect(describeActivity(activity({ type: 'STATUS_CHANGED', newValue: 'DONE' }))).toEqual(['moved', 'to Done'])
    expect(describeActivity(activity({ type: 'ASSIGNEE_CHANGED', newValue: 'demo_dev' }))).toEqual(['assigned', 'to demo_dev'])
    expect(describeActivity(activity({ type: 'ASSIGNEE_CHANGED', newValue: null }))).toEqual(['removed the assignee of', ''])
  })

  it('trims form metadata and builds select options', () => {
    const data = new FormData()
    data.set('title', '  Backend  ')
    data.set('description', ' text ')
    expect(readMetadata(data)).toEqual({ title: 'Backend', description: 'text' })
    expect(toOptions({ A: 'Alpha' })).toEqual([{ value: 'A', label: 'Alpha' }])
  })
})
