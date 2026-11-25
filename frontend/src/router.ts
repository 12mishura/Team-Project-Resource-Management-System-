import { useSyncExternalStore } from 'react'
import type { IssueRef } from './api'

export type OrgSection = 'projects' | 'members' | 'workload' | 'settings'
export type ProjectTab = 'board' | 'issues' | 'overview' | 'workload' | 'members' | 'settings'

export type Route =
  | { name: 'home' }
  | { name: 'organizations' }
  | { name: 'profile' }
  | { name: 'timesheet' }
  | { name: 'mine'; issue?: IssueRef }
  | { name: 'org'; orgId: string; section: OrgSection }
  | { name: 'project'; orgId: string; projectId: string; tab: ProjectTab; issueId?: string }

const orgSections: OrgSection[] = ['projects', 'members', 'workload', 'settings']
const projectTabs: ProjectTab[] = ['board', 'issues', 'overview', 'workload', 'members', 'settings']

const listeners = new Set<() => void>()
const notify = () => listeners.forEach(listener => listener())
window.addEventListener('popstate', notify)

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

const currentLocation = () => window.location.pathname + window.location.search

export function parseRoute(location: string): Route {
  const [path, query = ''] = location.split('?')
  const params = new URLSearchParams(query)
  const parts = path
    .split('/')
    .filter(Boolean)
    .map(part => {
      try {
        return decodeURIComponent(part)
      } catch {
        return ''
      }
    })
  const [section, first, second, third, fourth, fifth] = parts
  const issueId = params.get('issue') || undefined

  if (parts.length === 0) return { name: 'home' }
  if (section === 'profile' && parts.length === 1) return { name: 'profile' }
  if (section === 'timesheet' && parts.length === 1) return { name: 'timesheet' }
  if (section === 'organizations' && parts.length === 1) return { name: 'organizations' }
  if (section === 'my-issues') {
    if (parts.length === 1) return { name: 'mine' }
    if (parts.length === 4 && parts.every(Boolean)) return { name: 'mine', issue: { organizationId: first, projectId: second, id: third } }
  }
  if (section === 'o' && first) {
    if (parts.length === 2) return { name: 'org', orgId: first, section: 'projects' }
    if (parts.length === 3 && orgSections.includes(second as OrgSection))
      return { name: 'org', orgId: first, section: second as OrgSection }
    if (second === 'p' && third) {
      if (parts.length === 4) return { name: 'project', orgId: first, projectId: third, tab: 'board', issueId }
      if (parts.length === 5 && projectTabs.includes(fourth as ProjectTab)) {
        return { name: 'project', orgId: first, projectId: third, tab: fourth as ProjectTab, issueId }
      }
    }
  }
  if (section === 'organizations' && first) {
    if (parts.length === 2 || (second === 'projects' && parts.length === 3)) return { name: 'org', orgId: first, section: 'projects' }
    if (second === 'projects' && third && parts.length === 4) return { name: 'project', orgId: first, projectId: third, tab: 'board' }
    if (second === 'projects' && third && fourth === 'issues' && fifth && parts.length === 6) {
      return { name: 'project', orgId: first, projectId: third, tab: 'board', issueId: fifth }
    }
  }
  return { name: 'home' }
}

export function routePath(route: Route): string {
  const segment = (...values: string[]) => '/' + values.map(encodeURIComponent).join('/')
  switch (route.name) {
    case 'home':
      return '/'
    case 'organizations':
      return '/organizations'
    case 'profile':
      return '/profile'
    case 'timesheet':
      return '/timesheet'
    case 'mine':
      return route.issue ? segment('my-issues', route.issue.organizationId, route.issue.projectId, route.issue.id) : '/my-issues'
    case 'org':
      return route.section === 'projects' ? segment('o', route.orgId) : segment('o', route.orgId, route.section)
    case 'project': {
      const base = segment('o', route.orgId, 'p', route.projectId, route.tab)
      return route.issueId ? `${base}?issue=${encodeURIComponent(route.issueId)}` : base
    }
  }
}

export function navigate(route: Route, options: { replace?: boolean } = {}) {
  const path = routePath(route)
  if (path === currentLocation()) return
  if (options.replace) window.history.replaceState(null, '', path)
  else window.history.pushState(null, '', path)
  notify()
}

export function href(route: Route) {
  return routePath(route)
}

export function useRoute(): Route {
  return parseRoute(useSyncExternalStore(subscribe, currentLocation))
}

export function isCurrent(path: string) {
  return path === currentLocation()
}
