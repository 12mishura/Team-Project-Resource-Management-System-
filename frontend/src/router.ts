import { useSyncExternalStore } from 'react'
import type { IssueRef } from './api'

export type Route =
  | { name: 'organizations' }
  | { name: 'profile' }
  | { name: 'mine'; issue?: IssueRef }
  | { name: 'organization'; orgId: string }
  | { name: 'projects'; orgId: string }
  | { name: 'board'; orgId: string; projectId: string; issueId?: string }

const listeners = new Set<() => void>()
const notify = () => listeners.forEach(listener => listener())
window.addEventListener('popstate', notify)

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

const currentPath = () => window.location.pathname

export function parseRoute(path: string): Route {
  const parts = path.split('/').filter(Boolean).map(part => {
    try { return decodeURIComponent(part) } catch { return '' }
  })
  const [section, orgId, child, projectId, issues, issueId] = parts
  if (section === 'profile' && parts.length === 1) return { name: 'profile' }
  if (section === 'my-issues') {
    if (parts.length === 1) return { name: 'mine' }
    if (parts.length === 4 && parts.every(Boolean)) return { name: 'mine', issue: { organizationId: parts[1], projectId: parts[2], id: parts[3] } }
  }
  if (section === 'organizations' && orgId) {
    if (parts.length === 2) return { name: 'organization', orgId }
    if (child === 'projects' && parts.length === 3) return { name: 'projects', orgId }
    if (child === 'projects' && projectId && parts.length === 4) return { name: 'board', orgId, projectId }
    if (child === 'projects' && projectId && issues === 'issues' && issueId && parts.length === 6) {
      return { name: 'board', orgId, projectId, issueId }
    }
  }
  return { name: 'organizations' }
}

export function routePath(route: Route): string {
  const segment = (...values: string[]) => '/' + values.map(encodeURIComponent).join('/')
  switch (route.name) {
    case 'organizations': return '/organizations'
    case 'profile': return '/profile'
    case 'mine': return route.issue
      ? segment('my-issues', route.issue.organizationId, route.issue.projectId, route.issue.id)
      : '/my-issues'
    case 'organization': return segment('organizations', route.orgId)
    case 'projects': return segment('organizations', route.orgId, 'projects')
    case 'board': return route.issueId
      ? segment('organizations', route.orgId, 'projects', route.projectId, 'issues', route.issueId)
      : segment('organizations', route.orgId, 'projects', route.projectId)
  }
}

export function navigate(route: Route, options: { replace?: boolean } = {}) {
  const path = routePath(route)
  if (path === currentPath()) return
  if (options.replace) window.history.replaceState(null, '', path)
  else window.history.pushState(null, '', path)
  notify()
}

export function useRoute(): Route {
  const path = useSyncExternalStore(subscribe, currentPath)
  return parseRoute(path)
}
