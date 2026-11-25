import { describe, expect, it } from 'vitest'
import { parseRoute, routePath, type Route } from './router'

const ORG = 'org-1'
const PROJECT = 'project-1'

describe('parseRoute', () => {
  it.each([
    ['/', { name: 'home' }],
    ['/organizations', { name: 'organizations' }],
    ['/profile', { name: 'profile' }],
    ['/my-issues', { name: 'mine' }],
    ['/o/org-1', { name: 'org', orgId: ORG, section: 'projects' }],
    ['/o/org-1/members', { name: 'org', orgId: ORG, section: 'members' }],
    ['/o/org-1/settings', { name: 'org', orgId: ORG, section: 'settings' }],
    ['/o/org-1/p/project-1', { name: 'project', orgId: ORG, projectId: PROJECT, tab: 'board' }],
    ['/o/org-1/p/project-1/overview', { name: 'project', orgId: ORG, projectId: PROJECT, tab: 'overview' }],
  ] as [string, Route][])('reads %s', (path, route) => {
    expect(parseRoute(path)).toEqual(route)
  })

  it('reads the open issue from the query string', () => {
    expect(parseRoute('/o/org-1/p/project-1/issues?issue=issue-9')).toEqual({
      name: 'project',
      orgId: ORG,
      projectId: PROJECT,
      tab: 'issues',
      issueId: 'issue-9',
    })
  })

  it('reads an issue opened from my issues', () => {
    expect(parseRoute('/my-issues/org-1/project-1/issue-9')).toEqual({
      name: 'mine',
      issue: { organizationId: ORG, projectId: PROJECT, id: 'issue-9' },
    })
  })

  it('maps legacy links to the new routes', () => {
    expect(parseRoute('/organizations/org-1/projects')).toEqual({ name: 'org', orgId: ORG, section: 'projects' })
    expect(parseRoute('/organizations/org-1/projects/project-1/issues/issue-9')).toEqual({
      name: 'project',
      orgId: ORG,
      projectId: PROJECT,
      tab: 'board',
      issueId: 'issue-9',
    })
  })

  it('falls back to home for unknown paths and tabs', () => {
    expect(parseRoute('/nope')).toEqual({ name: 'home' })
    expect(parseRoute('/o/org-1/p/project-1/unknown')).toEqual({ name: 'home' })
  })
})

describe('routePath', () => {
  it('round-trips every route', () => {
    const routes: Route[] = [
      { name: 'home' },
      { name: 'organizations' },
      { name: 'profile' },
      { name: 'mine' },
      { name: 'mine', issue: { organizationId: ORG, projectId: PROJECT, id: 'issue-9' } },
      { name: 'org', orgId: ORG, section: 'projects' },
      { name: 'org', orgId: ORG, section: 'members' },
      { name: 'project', orgId: ORG, projectId: PROJECT, tab: 'issues', issueId: 'issue-9' },
    ]
    for (const route of routes) expect(parseRoute(routePath(route))).toEqual(route)
  })

  it('encodes unsafe characters', () => {
    expect(routePath({ name: 'org', orgId: 'a b', section: 'members' })).toBe('/o/a%20b/members')
  })
})
