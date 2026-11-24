export interface Session {
  accessToken: string
  refreshToken: string
}

export interface User {
  id: string
  email: string
  username: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalPages: number
  totalElements: number
}

export type OrganizationRole = 'OWNER' | 'ADMIN' | 'USER'
export type ProjectStatus = 'ACTIVE' | 'FINISHED' | 'ARCHIVED'
export type IssueType = 'EPIC' | 'STORY' | 'TASK' | 'BUG'
export type IssueStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type IssuePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

interface NamedRecord {
  id: string
  title: string
  description: string | null
}

export interface ProjectStats {
  projectId: string
  issuesByStatus: Record<IssueStatus, number>
  totalIssues: number
  openIssues: number
  openBugs: number
  assignedToMe: number
  membersCount: number
  members: string[]
}

export interface OrganizationOverview {
  organizationId: string
  projectsCount: number
  membersCount: number
  openIssues: number
  assignedToMe: number
  projects: ProjectStats[]
}

export interface ProjectActivity extends Omit<IssueEvent, 'id'> {
  id: string
  issueId: string
  issueTitle: string
  issueType: IssueType
}

export interface Organization extends NamedRecord {
  role: OrganizationRole
  memberId: string
}

export interface Project extends NamedRecord {
  organizationId: string
  status: ProjectStatus
}

export interface Issue extends NamedRecord {
  createdAt: string
  updatedAt: string
  organizationId: string
  projectId: string
  type: IssueType
  status: IssueStatus
  priority: IssuePriority
  assigneeUsername: string | null
  assigneeId: string | null
  parentId: string | null
  reporterId: string
  reporterUsername: string
  severity: string | null
  stepsToReproduce: string | null
  storyPoints: number | null
}

export type ProjectMemberRole = 'ADMIN' | 'EXTENDED' | 'USER'

export type IssueRef = { organizationId: string; projectId: string; id: string }

export interface ProjectMember {
  id: string
  organizationMemberId: string
  email: string
  username: string
  role: ProjectMemberRole
}

export interface OrganizationMember {
  id: string
  email: string
  username: string
  role: OrganizationRole
}

export interface Comment {
  id: string
  authorId: string
  authorUsername: string
  text: string
  createdAt: string
  editedAt: string | null
}

export interface IssueEvent {
  id: string
  type: 'CREATED' | 'FIELD_CHANGED' | 'STATUS_CHANGED' | 'ASSIGNEE_CHANGED' | 'PARENT_CHANGED'
  field: string | null
  oldValue: string | null
  newValue: string | null
  actorUsername: string
  createdAt: string
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  signal?: AbortSignal
}

const sessionKey = 'tasktracker.session'
const listeners = new Set<() => void>()

function readSession(): Session | null {
  try {
    const saved = JSON.parse(sessionStorage.getItem(sessionKey) ?? 'null')
    return typeof saved?.accessToken === 'string' && typeof saved.refreshToken === 'string'
      ? { accessToken: saved.accessToken, refreshToken: saved.refreshToken }
      : null
  } catch {
    return null
  }
}

let session = readSession()
let sessionGeneration = 0
let refreshing: Promise<Session> | undefined

function saveSession(next: Session | null) {
  session = next
  try {
    if (next) sessionStorage.setItem(sessionKey, JSON.stringify(next))
    else sessionStorage.removeItem(sessionKey)
  } catch {
    // The current session still works in memory when storage is unavailable.
  }
  listeners.forEach(notify => notify())
}

export const hasSession = () => session !== null
export function subscribeSession(notify: () => void) {
  listeners.add(notify)
  return () => {
    listeners.delete(notify)
  }
}

function friendlyForbidden(detail: string) {
  if (detail.startsWith('Access denied to project'))
    return 'You are not a member of this project. Ask an organization owner or admin to add you.'
  if (detail.startsWith('Access denied to organization')) return 'You are not a member of this organization anymore.'
  return detail
}

function friendlyNotFound(detail: string) {
  const match = /^(\w[\w ]*?) with ID \S+ not found$/.exec(detail)
  return match ? `This ${match[1].toLowerCase()} does not exist or was deleted.` : detail
}

async function send<T>(path: string, options: RequestOptions = {}, token?: string): Promise<T> {
  const { method = 'GET', body, signal } = options
  const headers = new Headers({ Accept: 'application/json, application/problem+json', 'Accept-Language': 'en' })
  if (body !== undefined) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
    cache: 'no-store',
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    const fallback = response.status === 403 ? 'You don’t have access to this section.' : `API error (${response.status}).`
    const detail = typeof problem?.detail === 'string' ? problem.detail : fallback
    throw new ApiError(
      response.status,
      response.status === 403 ? friendlyForbidden(detail) : response.status === 404 ? friendlyNotFound(detail) : detail,
    )
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

async function request<T>(path: string, options?: RequestOptions): Promise<T> {
  const generation = sessionGeneration
  const initial = session
  if (!initial) throw new ApiError(401, 'Sign in to your account.')
  try {
    return await send<T>(path, options, initial.accessToken)
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401 || generation !== sessionGeneration || !session) throw error
    options?.signal?.throwIfAborted()
    if (session === initial && !refreshing) {
      const renewal = send<Session>('/auth/refresh', { method: 'POST', body: { refreshToken: initial.refreshToken } })
        .then(next => {
          if (sessionGeneration === generation) saveSession(next)
          return next
        })
        .catch(error => {
          if (sessionGeneration === generation && error instanceof ApiError && error.status === 401) {
            sessionGeneration++
            saveSession(null)
          }
          throw error
        })
        .finally(() => {
          if (refreshing === renewal) refreshing = undefined
        })
      refreshing = renewal
    }
    const latest = session === initial ? await refreshing : session
    if (generation !== sessionGeneration || !latest) throw error
    return send<T>(path, options, latest.accessToken)
  }
}

export async function signIn(email: string, password: string) {
  const next = await send<Session>('/auth/login', { method: 'POST', body: { email, password } })
  sessionGeneration++
  refreshing = undefined
  saveSession(next)
}

export async function signOut() {
  const initial = session
  const pending = refreshing
  clearSession()
  const latest = (await pending?.catch(() => null)) ?? initial
  if (latest) await send<void>('/auth/logout', { method: 'POST', body: { refreshToken: latest.refreshToken } })
}

function clearSession() {
  sessionGeneration++
  refreshing = undefined
  saveSession(null)
}

export async function changePassword(currentPassword: string, newPassword: string) {
  const generation = sessionGeneration
  await request<void>('/users/me/password', { method: 'PUT', body: { currentPassword, newPassword } })
  if (generation === sessionGeneration) clearSession()
}

export const register = (body: { email: string; password: string; username: string }) =>
  send<User>('/auth/register', { method: 'POST', body })

export const getProfile = (signal?: AbortSignal) => request<User>('/auth/me', { signal })

const orgPath = (id: string) => '/organizations/' + encodeURIComponent(id)
const projectPath = (orgId: string, projectId: string) => orgPath(orgId) + '/projects/' + encodeURIComponent(projectId)
const issuePath = (issue: IssueRef) => projectPath(issue.organizationId, issue.projectId) + '/issues/' + encodeURIComponent(issue.id)

export type Metadata = { title: string; description: string }
export type IssueFilters = {
  q?: string
  type?: string
  status?: string
  assigneeId?: string
  parentId?: string
  sortBy?: string
  direction?: string
}
export type IssueFields = {
  title: string
  description?: string
  priority: IssuePriority
  severity?: string
  stepsToReproduce?: string
  storyPoints?: number
}

async function listAll<T>(path: string, signal?: AbortSignal, filters: Record<string, string | undefined> = {}): Promise<T[]> {
  const first = await list<T>(path, 0, filters, signal, 100)
  const all = [...first.content]
  for (let page = 1; page < first.totalPages; page++) {
    const next = await list<T>(path, page, filters, signal, 100)
    all.push(...next.content)
  }
  return all
}

function list<T>(path: string, page: number, filters: Record<string, string | undefined>, signal?: AbortSignal, size = 20) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value)
  })
  return request<Page<T>>(`${path}?${params}`, { signal })
}

export const api = {
  updateProfile: (username: string) => request<User>('/users/me', { method: 'PATCH', body: { username } }),
  organizations: (signal?: AbortSignal) => listAll<Organization>('/organizations', signal),
  organization: (id: string, signal?: AbortSignal) => request<NamedRecord>(orgPath(id), { signal }),
  organizationOverview: (id: string, signal?: AbortSignal) => request<OrganizationOverview>(orgPath(id) + '/overview', { signal }),
  updateOrganization: (id: string, body: Partial<Metadata>) => request<NamedRecord>(orgPath(id), { method: 'PATCH', body }),
  organizationMembers: (id: string, page: number, signal?: AbortSignal) =>
    list<OrganizationMember>(orgPath(id) + '/members', page, {}, signal),
  updateOrganizationMember: (id: string, memberId: string, role: OrganizationRole) =>
    request<OrganizationMember>(orgPath(id) + '/members/' + encodeURIComponent(memberId), { method: 'PATCH', body: { role } }),
  removeOrganizationMember: (id: string, memberId: string) =>
    request<void>(orgPath(id) + '/members/' + encodeURIComponent(memberId), { method: 'DELETE' }),
  leaveOrganization: (id: string) => request<void>(orgPath(id) + '/members/me', { method: 'DELETE' }),
  transferOwnership: (id: string, memberId: string) =>
    request<OrganizationMember>(orgPath(id) + '/transfer-ownership', { method: 'POST', body: { memberId } }),
  deleteOrganization: (id: string) => request<void>(orgPath(id), { method: 'DELETE' }),
  regenerateInviteCode: (id: string) => request<{ inviteCode: string }>(orgPath(id) + '/invite-code/regenerate', { method: 'POST' }),
  inviteCode: (id: string, signal?: AbortSignal) => request<{ inviteCode: string }>(orgPath(id) + '/invite-code', { signal }),
  createOrganization: (body: Metadata) => request<NamedRecord>('/organizations', { method: 'POST', body }),
  joinOrganization: (inviteCode: string) => request('/organizations/join', { method: 'POST', body: { inviteCode } }),
  projects: (id: string, page: number, status: string, signal?: AbortSignal) =>
    list<Project>(orgPath(id) + '/projects', page, { status }, signal),
  myIssues: (signal?: AbortSignal) => listAll<Issue>('/users/me/issues', signal),
  allProjects: (id: string, signal?: AbortSignal) => listAll<Project>(orgPath(id) + '/projects', signal),
  project: (orgId: string, projectId: string, signal?: AbortSignal) => request<Project>(projectPath(orgId, projectId), { signal }),
  createProject: (id: string, body: Metadata) => request<Project>(orgPath(id) + '/projects', { method: 'POST', body }),
  updateProject: (project: Project, body: Partial<Metadata>) =>
    request<Project>(projectPath(project.organizationId, project.id), { method: 'PATCH', body }),
  deleteProject: (project: Project) => request<void>(projectPath(project.organizationId, project.id), { method: 'DELETE' }),
  changeProjectStatus: (project: Project, status: ProjectStatus) =>
    request<Project>(projectPath(project.organizationId, project.id) + '/status', { method: 'PATCH', body: { status } }),
  allOrganizationMembers: (id: string, signal?: AbortSignal) => listAll<OrganizationMember>(orgPath(id) + '/members', signal),
  projectMembers: (project: Project, signal?: AbortSignal) =>
    listAll<ProjectMember>(projectPath(project.organizationId, project.id) + '/members', signal),
  addProjectMember: (project: Project, organizationMemberId: string, role: ProjectMemberRole) =>
    request<ProjectMember>(projectPath(project.organizationId, project.id) + '/members', {
      method: 'POST',
      body: { organizationMemberId, role },
    }),
  updateProjectMember: (project: Project, member: ProjectMember, role: ProjectMemberRole) =>
    request<ProjectMember>(projectPath(project.organizationId, project.id) + '/members/' + encodeURIComponent(member.id), {
      method: 'PATCH',
      body: { role },
    }),
  removeProjectMember: (project: Project, member: ProjectMember) =>
    request<void>(projectPath(project.organizationId, project.id) + '/members/' + encodeURIComponent(member.id), { method: 'DELETE' }),
  allIssues: async (project: Project, filters: IssueFilters, signal?: AbortSignal): Promise<Page<Issue>> => {
    const content = await listAll<Issue>(projectPath(project.organizationId, project.id) + '/issues', signal, filters)
    return { content, page: 0, size: content.length, totalPages: 1, totalElements: content.length }
  },
  issues: (project: Project | null, page: number, filters: IssueFilters, signal?: AbortSignal) =>
    list<Issue>(project ? projectPath(project.organizationId, project.id) + '/issues' : '/users/me/issues', page, filters, signal),
  issue: (issue: IssueRef, signal?: AbortSignal) => request<Issue>(issuePath(issue), { signal }),
  createIssue: (project: Project, body: IssueFields & { type: IssueType; assigneeId?: string; parentId?: string }) =>
    request<Issue>(projectPath(project.organizationId, project.id) + '/issues', { method: 'POST', body }),
  updateIssue: (issue: Issue, body: Partial<IssueFields>) => request<Issue>(issuePath(issue), { method: 'PATCH', body }),
  deleteIssue: (issue: Issue) => request<void>(issuePath(issue), { method: 'DELETE' }),
  changeStatus: (issue: Issue, status: IssueStatus) => request<Issue>(issuePath(issue) + '/status', { method: 'PATCH', body: { status } }),
  assignToMe: (issue: Issue) => request<Issue>(issuePath(issue) + '/assignee/me', { method: 'PATCH' }),
  assignee: (issue: Issue, projectMemberId: string) =>
    request<Issue>(issuePath(issue) + '/assignee', projectMemberId ? { method: 'PATCH', body: { projectMemberId } } : { method: 'DELETE' }),
  epics: (project: Project, signal?: AbortSignal) =>
    listAll<Issue>(projectPath(project.organizationId, project.id) + '/issues', signal, { type: 'EPIC' }),
  parent: (issue: Issue, parentId: string) =>
    request<Issue>(issuePath(issue) + '/parent', parentId ? { method: 'PATCH', body: { parentId } } : { method: 'DELETE' }),
  comments: (issue: IssueRef, page: number, signal?: AbortSignal) => list<Comment>(issuePath(issue) + '/comments', page, {}, signal),
  comment: (issue: IssueRef, text: string) => request<Comment>(issuePath(issue) + '/comments', { method: 'POST', body: { text } }),
  editComment: (issue: IssueRef, comment: Comment, text: string) =>
    request<Comment>(issuePath(issue) + '/comments/' + encodeURIComponent(comment.id), { method: 'PATCH', body: { text } }),
  deleteComment: (issue: IssueRef, comment: Comment) =>
    request<void>(issuePath(issue) + '/comments/' + encodeURIComponent(comment.id), { method: 'DELETE' }),
  projectActivity: (project: Project, page: number, signal?: AbortSignal, size = 20) =>
    list<ProjectActivity>(projectPath(project.organizationId, project.id) + '/activity', page, {}, signal, size),
  history: (issue: IssueRef, page: number, signal?: AbortSignal) => list<IssueEvent>(issuePath(issue) + '/history', page, {}, signal),
}
