import { useSyncExternalStore } from 'react'

export type RecentProject = { id: string; organizationId: string; title: string }

const storageKey = 'tasktracker.recentProjects'
const limit = 5
const listeners = new Set<() => void>()

function read(): RecentProject[] {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) ?? '[]')
    return Array.isArray(saved)
      ? saved.filter(item => typeof item?.id === 'string' && typeof item.organizationId === 'string' && typeof item.title === 'string')
      : []
  } catch {
    return []
  }
}

let recent = read()

function save(next: RecentProject[]) {
  recent = next
  try {
    localStorage.setItem(storageKey, JSON.stringify(next))
  } catch {
    return
  } finally {
    listeners.forEach(listener => listener())
  }
}

export function rememberProject(project: RecentProject) {
  const current = recent[0]
  if (current && current.id === project.id && current.title === project.title) return
  save([project, ...recent.filter(item => item.id !== project.id)].slice(0, limit))
}

export function forgetProject(id: string) {
  if (recent.some(item => item.id === id)) save(recent.filter(item => item.id !== id))
}

export function forgetOrganization(organizationId: string) {
  if (recent.some(item => item.organizationId === organizationId)) save(recent.filter(item => item.organizationId !== organizationId))
}

export function useAllRecentProjects() {
  return useSyncExternalStore(subscribe, () => recent)
}

export function useRecentProjects(organizationId: string) {
  const all = useSyncExternalStore(subscribe, () => recent)
  return all.filter(item => item.organizationId === organizationId)
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

const lastOrgKey = 'tasktracker.lastOrganization'

export function readLastOrganization() {
  try {
    return localStorage.getItem(lastOrgKey) ?? ''
  } catch {
    return ''
  }
}

export function saveLastOrganization(id: string) {
  try {
    if (id) localStorage.setItem(lastOrgKey, id)
    else localStorage.removeItem(lastOrgKey)
  } catch {
    return
  }
}

const ownerKey = 'tasktracker.recentOwner'

export function bindRecentToUser(userId: string) {
  try {
    if (localStorage.getItem(ownerKey) === userId) return
    localStorage.setItem(ownerKey, userId)
    localStorage.removeItem(lastOrgKey)
  } catch {
    return
  }
  save([])
}
