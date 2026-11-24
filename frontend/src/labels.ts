import type {
  IssueEvent,
  IssuePriority,
  IssueStatus,
  IssueType,
  Metadata,
  OrganizationRole,
  ProjectActivity,
  ProjectMemberRole,
  ProjectStatus,
} from './api'

export const organizationRoles: Record<OrganizationRole, string> = { OWNER: 'Owner', ADMIN: 'Administrator', USER: 'Member' }
export const projectRoles: Record<ProjectMemberRole, string> = { ADMIN: 'Admin', EXTENDED: 'Extended', USER: 'User' }
export const projectStatuses: Record<ProjectStatus, string> = { ACTIVE: 'Active', FINISHED: 'Completed', ARCHIVED: 'Archived' }

export function readMetadata(data: FormData): Metadata {
  return { title: String(data.get('title')).trim(), description: String(data.get('description')).trim() }
}

export const statuses: Record<IssueStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  IN_REVIEW: 'In review',
  DONE: 'Done',
}
export const workflow: Record<IssueStatus, IssueStatus[]> = {
  TODO: ['IN_PROGRESS'],
  IN_PROGRESS: ['TODO', 'IN_REVIEW'],
  IN_REVIEW: ['IN_PROGRESS', 'DONE'],
  DONE: ['IN_PROGRESS'],
}
export const priorities: Record<IssuePriority, string> = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }
export const priorityRank: Record<IssuePriority, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }
export const types: Record<IssueType, string> = { TASK: 'Task', STORY: 'Story', BUG: 'Bug', EPIC: 'Epic' }
export const severities: Record<string, string> = { MINOR: 'Minor', MAJOR: 'Major', CRITICAL: 'Critical', BLOCKER: 'Blocker' }
export const eventNames: Record<IssueEvent['type'], string> = {
  CREATED: 'Issue created',
  FIELD_CHANGED: 'Field updated',
  STATUS_CHANGED: 'Status changed',
  ASSIGNEE_CHANGED: 'Assignee changed',
  PARENT_CHANGED: 'Epic changed',
}
export const fieldNames: Record<string, string> = {
  title: 'Title',
  description: 'Description',
  priority: 'Priority',
  storyPoints: 'Story points',
  severity: 'Severity',
  stepsToReproduce: 'Steps to reproduce',
  status: 'Status',
  assignee: 'Assignee',
  parent: 'Epic',
}

const valueLabels: Record<string, Record<string, string>> = {
  priority: priorities,
  severity: severities,
  status: statuses,
}

export function fieldValue(field: string | null, value: string) {
  return (field && valueLabels[field]?.[value]) ?? value
}

const dateTime = new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' })
const shortDate = new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric' })
const relative = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })

export const formatDate = (value: string) => dateTime.format(new Date(value))

export function formatRelative(value: string) {
  const seconds = (new Date(value).getTime() - Date.now()) / 1000
  const units: [Intl.RelativeTimeFormatUnit, number][] = [
    ['day', 86400],
    ['hour', 3600],
    ['minute', 60],
  ]
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) {
      return Math.abs(seconds) >= 7 * 86400 ? shortDate.format(new Date(value)) : relative.format(Math.round(seconds / size), unit)
    }
  }
  return 'just now'
}

export function describeActivity(item: ProjectActivity): [string, string] {
  if (item.type === 'CREATED') return [`created ${types[item.issueType].toLowerCase()}`, '']
  if (item.type === 'FIELD_CHANGED' && item.field) return [`changed ${(fieldNames[item.field] ?? item.field).toLowerCase()} of`, '']
  if (item.type === 'STATUS_CHANGED' && item.newValue) return ['moved', `to ${fieldValue('status', item.newValue)}`]
  if (item.type === 'ASSIGNEE_CHANGED') return item.newValue ? ['assigned', `to ${item.newValue}`] : ['removed the assignee of', '']
  return ['changed the epic of', '']
}

export const toOptions = (entries: Record<string, string>) => Object.entries(entries).map(([value, label]) => ({ value, label }))

export const noun = (count: number, one: string, many = `${one}s`) => (count === 1 ? one : many)
export const plural = (count: number, one: string, many = `${one}s`) => `${count} ${noun(count, one, many)}`

export function initials(name: string) {
  const parts = name.split(/[\s._-]+/).filter(Boolean)
  if (parts.length < 2) return name.slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

export function avatarHue(name: string) {
  let hash = 0
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) % 360
  return hash
}
