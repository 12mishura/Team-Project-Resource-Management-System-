import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ClipboardList,
  GripVertical,
  Link2,
  Pencil,
  Plus,
  Search,
  Trash2,
  UserCheck,
  X,
} from 'lucide-react'
import { useCallback, useDeferredValue, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import {
  api,
  type Comment,
  type Issue,
  type IssueEvent,
  type IssueFields,
  type IssueFilters,
  type IssuePriority,
  type IssueRef,
  type IssueStatus,
  type IssueType,
  type Organization,
  type Project,
  type ProjectMember,
  type User,
} from './api'
import { confirmAction, toast } from './feedbackStore'
import {
  eventNames,
  fieldNames,
  fieldValue,
  formatDate,
  formatRelative,
  priorities,
  severities,
  statuses,
  toOptions,
  types,
  workflow,
} from './labels'
import { navigate, routePath } from './router'
import { Avatar, PriorityIcon } from './bits'
import { Select } from './Select'
import { useAction, useResource } from './hooks'
import { Empty, ErrorNotice, Loading, Modal, Pagination } from './ui'
import { EstimateField, LogTimeDialog, TimeSummary, WorkLogList } from './TimeTracking'
import { formatDuration, readDuration } from './time'

type SortField = 'CREATED_AT' | 'UPDATED_AT' | 'PRIORITY' | 'STATUS'
type Sort = { field: SortField; direction: 'ASC' | 'DESC' }
const defaultSort: Sort = { field: 'UPDATED_AT', direction: 'DESC' }

function useProjectAccess(project: Project | null, organization?: Organization, user?: User, includeRoster = false) {
  const privileged = organization?.role === 'OWNER' || organization?.role === 'ADMIN'
  const load = useCallback(
    async (signal: AbortSignal) => {
      if (!project || !user || (privileged && !includeRoster)) return []
      return api.projectMembers(project, signal)
    },
    [project, privileged, user, includeRoster],
  )
  const roster = useResource(load)
  const member = roster.value?.find(member => member.email === user?.email)
  return {
    roster,
    member,
    mayView: privileged || Boolean(member),
    extended: privileged || member?.role === 'EXTENDED' || member?.role === 'ADMIN',
    admin: privileged || member?.role === 'ADMIN',
  }
}

type Access = ReturnType<typeof useProjectAccess>

function canChangeStatus(issue: Issue, access: Access, user?: User) {
  return (
    access.mayView &&
    (access.extended || user?.id === issue.reporterId || Boolean(issue.assigneeId && issue.assigneeId === access.member?.id))
  )
}

function useEpics(project: Project | null, enabled = true) {
  return useResource(
    useCallback(
      (signal: AbortSignal) => (project && enabled ? api.epics(project, signal) : Promise.resolve([] as Issue[])),
      [project, enabled],
    ),
  )
}

function useProjectTitles(organizations: Organization[], enabled: boolean) {
  const key = organizations.map(item => item.id).join(',')
  return useResource(
    useCallback(
      async (signal: AbortSignal) => {
        if (!enabled) return new Map<string, string>()
        const lists = await Promise.all(
          key
            .split(',')
            .filter(Boolean)
            .map(id => api.allProjects(id, signal)),
        )
        return new Map(lists.flat().map(project => [project.id, project.title]))
      },
      [key, enabled],
    ),
  )
}

export function Issues({
  project,
  organizations,
  user,
  selected,
  onSelect,
  layout = 'list',
  heading,
}: {
  project: Project | null
  organizations: Organization[]
  user?: User
  selected?: IssueRef
  onSelect: (issue?: IssueRef) => void
  layout?: 'board' | 'list'
  heading?: (createButton: ReactNode) => ReactNode
}) {
  const [query, setQuery] = useState('')
  const [type, setType] = useState('')
  const [status, setStatus] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [parentId, setParentId] = useState('')
  const [sort, setSort] = useState<Sort>(defaultSort)
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const [moveMessage, setMoveMessage] = useState('')
  const move = useAction()
  const deferredQuery = useDeferredValue(query.trim())
  const organization = organizations.find(item => item.id === project?.organizationId)
  const access = useProjectAccess(project, organization, user, true)
  const epics = useEpics(project)
  const projectTitles = useProjectTitles(organizations, !project)
  const wholeBoard = Boolean(project) && layout === 'board'
  const load = useCallback(
    (signal: AbortSignal) => {
      const filters: IssueFilters = { q: deferredQuery, type, status, assigneeId, parentId }
      if (project && wholeBoard) return api.allIssues(project, filters, signal)
      return api.issues(project, page, { ...filters, sortBy: sort.field, direction: sort.direction }, signal)
    },
    [project, wholeBoard, page, deferredQuery, type, status, assigneeId, parentId, sort],
  )
  const issues = useResource(load)
  const content = issues.value?.content ?? []
  const archived = project?.status === 'ARCHIVED'
  const noAccess = Boolean(project && access.roster.value && !access.mayView)
  const filtered = Boolean(query || type || status || assigneeId || parentId)
  const epicTitles = useMemo(() => new Map((epics.value ?? []).map(epic => [epic.id, epic.title])), [epics.value])

  function resetPage<T>(setter: (value: T) => void) {
    return (value: T) => {
      setter(value)
      setPage(0)
    }
  }

  function clearFilters() {
    setQuery('')
    setType('')
    setStatus('')
    setAssigneeId('')
    setParentId('')
    setPage(0)
  }

  function changeSort(field: SortField) {
    setSort(current =>
      current.field === field
        ? { field, direction: current.direction === 'ASC' ? 'DESC' : 'ASC' }
        : { field, direction: field === 'STATUS' ? 'ASC' : 'DESC' },
    )
    setPage(0)
  }

  function moveIssue(issue: Issue, next: IssueStatus) {
    if (archived || !canChangeStatus(issue, access, user) || !workflow[issue.status].includes(next)) return
    setMoveMessage('')
    void move.run(async () => {
      const updated = await api.changeStatus(issue, next)
      if (status) issues.reload()
      else
        issues.setValue(
          current =>
            current && {
              ...current,
              content: current.content.map(item => (item.id === updated.id ? updated : item)),
            },
        )
      setMoveMessage(`${updated.title} moved to ${statuses[updated.status]}.`)
    })
  }

  return (
    <>
      {heading ? (
        heading(
          project && !archived && access.mayView ? (
            <button className="button primary" onClick={() => setCreating(true)}>
              <Plus size={17} />
              Create issue
            </button>
          ) : null,
        )
      ) : (
        <header className="page-heading">
          <div>
            <h1>My issues</h1>
            <p>Issues assigned to you across all organizations</p>
          </div>
        </header>
      )}
      {archived && <p className="notice info">This project is archived, so its issues are read-only.</p>}
      {!noAccess && (
        <div className="toolbar issue-toolbar">
          <div className="row filters">
            <label className="search">
              <Search size={16} />
              <input
                type="search"
                aria-label="Search issues by title"
                placeholder="Search issues"
                value={query}
                disabled={move.busy}
                onChange={event => {
                  setQuery(event.target.value)
                  setPage(0)
                }}
              />
            </label>
            <Select
              aria-label="Issue type"
              value={type}
              disabled={move.busy}
              onChange={resetPage(setType)}
              options={[{ value: '', label: 'All types' }, ...toOptions(types)]}
            />
            <Select
              aria-label="Issue status"
              value={status}
              disabled={move.busy}
              onChange={resetPage(setStatus)}
              options={[{ value: '', label: 'All statuses' }, ...toOptions(statuses)]}
            />
            {project && (
              <Select
                aria-label="Assignee"
                value={assigneeId}
                disabled={move.busy || !access.roster.value}
                onChange={resetPage(setAssigneeId)}
                options={[
                  { value: '', label: 'Anyone' },
                  ...(access.member ? [{ value: access.member.id, label: 'Assigned to me' }] : []),
                  ...(access.roster.value ?? [])
                    .filter(member => member.id !== access.member?.id)
                    .map(member => ({ value: member.id, label: member.username })),
                ]}
              />
            )}
            {project && (
              <Select
                aria-label="Epic"
                value={parentId}
                disabled={move.busy || !epics.value}
                onChange={resetPage(setParentId)}
                options={[{ value: '', label: 'Any epic' }, ...(epics.value ?? []).map(epic => ({ value: epic.id, label: epic.title }))]}
              />
            )}
            {filtered && (
              <button className="button" onClick={clearFilters}>
                <X size={15} />
                Clear
              </button>
            )}
          </div>
        </div>
      )}
      {noAccess && (
        <Empty title="You are not a member of this project">Ask an organization owner or admin to add you to the project.</Empty>
      )}
      {!noAccess && <ErrorNotice text={issues.error || access.roster.error} retry={issues.error ? issues.retry : access.roster.retry} />}
      <ErrorNotice text={move.error} />
      <div className="sr-only" role="status">
        {moveMessage}
      </div>
      {noAccess ? null : issues.loading && !issues.value ? (
        <Loading />
      ) : (
        issues.value && (
          <>
            {content.length === 0 ? (
              <Empty
                title={filtered ? 'No matching issues' : project ? 'No issues yet' : 'Nothing assigned to you'}
                icon={filtered ? <Search size={22} /> : <ClipboardList size={22} />}
                action={
                  filtered ? (
                    <button className="button" onClick={clearFilters}>
                      <X size={15} />
                      Clear filters
                    </button>
                  ) : (
                    project &&
                    !archived &&
                    access.mayView && (
                      <button className="button primary" onClick={() => setCreating(true)}>
                        <Plus size={16} />
                        Create issue
                      </button>
                    )
                  )
                }>
                {filtered
                  ? 'Try another search or clear the filters.'
                  : project
                    ? 'Create the first issue to start planning.'
                    : 'Issues assigned to you in any project will appear here.'}
              </Empty>
            ) : project && layout === 'board' ? (
              <IssueBoard
                issues={content}
                busy={move.busy}
                epicTitles={epicTitles}
                canMove={issue => !archived && canChangeStatus(issue, access, user)}
                onOpen={onSelect}
                onMove={moveIssue}
              />
            ) : (
              <IssueTable
                issues={content}
                sort={sort}
                onSort={changeSort}
                onOpen={onSelect}
                epicTitles={epicTitles}
                context={project ? undefined : { organizations, projectTitles: projectTitles.value }}
              />
            )}
            {content.length > 0 && (
              <div className="list-footer">
                <span>
                  {issues.value.totalElements} {issues.value.totalElements === 1 ? 'issue' : 'issues'}
                  {filtered ? ' match the filters' : ''}
                </span>
                <Pagination data={issues.value} onChange={setPage} disabled={move.busy} />
              </div>
            )}
          </>
        )
      )}
      {creating && project && (
        <IssueEditor
          project={project}
          access={access}
          epics={epics.value ?? []}
          close={() => setCreating(false)}
          saved={issue => {
            setCreating(false)
            issues.reload()
            epics.reload()
            toast(`${types[issue.type]} created`)
            onSelect(issue)
          }}
        />
      )}
      {selected && (
        <IssueDetail
          key={selected.id}
          issue={selected}
          project={project}
          user={user}
          organization={organizations.find(item => item.id === selected.organizationId)}
          close={() => onSelect(undefined)}
          openIssue={onSelect}
          changed={() => {
            issues.reload()
            epics.reload()
          }}
        />
      )}
    </>
  )
}

function IssueBoard({
  issues,
  busy,
  epicTitles,
  canMove,
  onOpen,
  onMove,
}: {
  issues: Issue[]
  busy: boolean
  epicTitles: Map<string, string>
  canMove: (issue: Issue) => boolean
  onOpen: (issue: Issue) => void
  onMove: (issue: Issue, status: IssueStatus) => void
}) {
  const [draggedId, setDraggedId] = useState<string>()
  const [target, setTarget] = useState<IssueStatus>()
  const dragged = issues.find(issue => issue.id === draggedId)

  function clearDrag() {
    setDraggedId(undefined)
    setTarget(undefined)
  }

  return (
    <div className="board" aria-busy={busy}>
      {(Object.entries(statuses) as [IssueStatus, string][]).map(([status, label]) => {
        const column = issues.filter(issue => issue.status === status)
        const acceptsDrop = Boolean(dragged && canMove(dragged) && workflow[dragged.status].includes(status)) && !busy
        return (
          <section
            className="lane"
            key={status}
            aria-label={label}
            data-drop={acceptsDrop ? (target === status ? 'active' : 'available') : undefined}
            onDragOver={event => {
              if (!acceptsDrop) return
              event.preventDefault()
              event.dataTransfer.dropEffect = 'move'
              setTarget(status)
            }}
            onDragLeave={event => {
              if (!(event.relatedTarget instanceof Node && event.currentTarget.contains(event.relatedTarget))) {
                setTarget(current => (current === status ? undefined : current))
              }
            }}
            onDrop={event => {
              if (!acceptsDrop || !dragged) return
              event.preventDefault()
              clearDrag()
              onMove(dragged, status)
            }}>
            <h2>
              <span className="status-dot" data-status={status} />
              {label}
              <span className="count">{column.length}</span>
            </h2>
            {column.map(issue => {
              const movable = canMove(issue)
              const epic = issue.parentId ? epicTitles.get(issue.parentId) : undefined
              return (
                <button
                  className="ticket"
                  key={issue.id}
                  disabled={busy}
                  draggable={movable && !busy}
                  data-dragging={draggedId === issue.id || undefined}
                  onClick={() => onOpen(issue)}
                  onDragStart={event => {
                    if (!movable || busy) {
                      event.preventDefault()
                      return
                    }
                    event.dataTransfer.effectAllowed = 'move'
                    event.dataTransfer.setData('text/plain', issue.id)
                    setDraggedId(issue.id)
                  }}
                  onDragEnd={clearDrag}>
                  <span className="ticket-heading">
                    <span className="badge issue-type" data-type={issue.type}>
                      {types[issue.type]}
                    </span>
                    {movable && <GripVertical className="ticket-grip" size={15} aria-hidden="true" />}
                  </span>
                  <strong>{issue.title}</strong>
                  {epic && (
                    <span className="epic-chip" title={`Epic: ${epic}`}>
                      {epic}
                    </span>
                  )}
                  <span className="ticket-footer">
                    <span className="ticket-tags">
                      <PriorityIcon priority={issue.priority} />
                      {(issue.estimateMinutes !== null || issue.spentMinutes > 0) && (
                        <span
                          className="chip time-chip"
                          data-over={(issue.estimateMinutes !== null && issue.spentMinutes > issue.estimateMinutes) || undefined}
                          title={`Logged ${formatDuration(issue.spentMinutes)} of ${formatDuration(issue.estimateMinutes, 'no estimate')}`}>
                          {formatDuration(issue.spentMinutes)} / {formatDuration(issue.estimateMinutes, '?')}
                        </span>
                      )}
                      {issue.storyPoints !== null && (
                        <span className="chip" title="Story points">
                          {issue.storyPoints} SP
                        </span>
                      )}
                      {issue.severity && (
                        <span className="chip severity" data-severity={issue.severity}>
                          {severities[issue.severity] ?? issue.severity}
                        </span>
                      )}
                    </span>
                    <Avatar name={issue.assigneeUsername} size="tiny" />
                  </span>
                </button>
              )
            })}
            {column.length === 0 && <p className="lane-empty">{acceptsDrop ? 'Drop here' : 'No issues'}</p>}
          </section>
        )
      })}
    </div>
  )
}

function IssueTable({
  issues,
  sort,
  onSort,
  onOpen,
  epicTitles,
  context,
}: {
  issues: Issue[]
  sort: Sort
  onSort: (field: SortField) => void
  onOpen: (issue: Issue) => void
  epicTitles: Map<string, string>
  context?: { organizations: Organization[]; projectTitles?: Map<string, string> }
}) {
  const header = (label: string, field?: SortField) => {
    if (!field) return <th scope="col">{label}</th>
    const active = sort.field === field
    const Icon = !active ? ArrowUpDown : sort.direction === 'ASC' ? ArrowUp : ArrowDown
    return (
      <th scope="col" aria-sort={active ? (sort.direction === 'ASC' ? 'ascending' : 'descending') : 'none'}>
        <button type="button" className="sort-button" data-active={active || undefined} onClick={() => onSort(field)}>
          {label}
          <Icon size={13} aria-hidden="true" />
        </button>
      </th>
    )
  }
  return (
    <div className="table-wrap">
      <table className="issue-table">
        <thead>
          <tr>
            {header('Issue')}
            {context && header('Project')}
            {header('Status', 'STATUS')}
            {header('Priority', 'PRIORITY')}
            {!context && header('Assignee')}
            {header('Updated', 'UPDATED_AT')}
            {header('Created', 'CREATED_AT')}
          </tr>
        </thead>
        <tbody>
          {issues.map(issue => {
            const epic = issue.parentId ? epicTitles.get(issue.parentId) : undefined
            const projectTitle = context?.projectTitles?.get(issue.projectId)
            const orgTitle = context?.organizations.find(org => org.id === issue.organizationId)?.title
            return (
              <tr key={issue.id}>
                <td className="issue-cell">
                  <button type="button" className="row-link" onClick={() => onOpen(issue)}>
                    <span className="badge issue-type" data-type={issue.type}>
                      {types[issue.type]}
                    </span>
                    <span className="issue-title">
                      {issue.title}
                      {epic && <small>Epic · {epic}</small>}
                    </span>
                  </button>
                </td>
                {context && (
                  <td>
                    <span className="cell-stack">
                      {projectTitle ?? '…'}
                      <small>{orgTitle}</small>
                    </span>
                  </td>
                )}
                <td>
                  <span className="issue-state">
                    <span className="status-dot" data-status={issue.status} />
                    {statuses[issue.status]}
                  </span>
                </td>
                <td>
                  <span className="priority-cell">
                    <PriorityIcon priority={issue.priority} />
                    {priorities[issue.priority]}
                  </span>
                </td>
                {!context && (
                  <td>
                    <span className="assignee-cell">
                      <Avatar name={issue.assigneeUsername} size="tiny" />
                      {issue.assigneeUsername ?? 'Unassigned'}
                    </span>
                  </td>
                )}
                <td>
                  <time dateTime={issue.updatedAt} title={formatDate(issue.updatedAt)}>
                    {formatRelative(issue.updatedAt)}
                  </time>
                </td>
                <td>
                  <time dateTime={issue.createdAt} title={formatDate(issue.createdAt)}>
                    {formatRelative(issue.createdAt)}
                  </time>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function IssueEditor({
  project,
  issue,
  access,
  epics,
  close,
  saved,
}: {
  project: Project
  issue?: Issue
  access: Access
  epics: Issue[]
  close: () => void
  saved: (issue: Issue) => void
}) {
  const [type, setType] = useState<IssueType>(issue?.type ?? 'TASK')
  const action = useAction()
  const assignees: ProjectMember[] = access.extended ? (access.roster.value ?? []) : access.member ? [access.member] : []
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const points = fields.get('points')
    const values: IssueFields = {
      title: String(fields.get('title')).trim(),
      description: String(fields.get('description')).trim(),
      priority: fields.get('priority') as IssuePriority,
      ...(type === 'BUG'
        ? {
            severity: String(fields.get('severity')),
            stepsToReproduce: String(fields.get('steps')).trim(),
          }
        : {}),
      ...(type === 'STORY' && points ? { storyPoints: Number(points) } : {}),
    }
    const estimate = type === 'EPIC' ? null : readDuration(fields, 'estimate')
    if (!issue) {
      const assigneeId = String(fields.get('assignee') ?? '')
      const parentId = String(fields.get('epic') ?? '')
      void action.run(async () =>
        saved(
          await api.createIssue(project, {
            ...values,
            type,
            description: values.description || undefined,
            stepsToReproduce: values.stepsToReproduce || undefined,
            assigneeId: assigneeId || undefined,
            parentId: type !== 'EPIC' && parentId ? parentId : undefined,
            estimateMinutes: estimate ?? undefined,
          }),
        ),
      )
      return
    }
    const changes: Partial<IssueFields> = {}
    if (values.title !== issue.title) changes.title = values.title
    if (values.description !== (issue.description ?? '')) changes.description = values.description
    if (values.priority !== issue.priority) changes.priority = values.priority
    if (type === 'BUG') {
      if (values.severity !== issue.severity) changes.severity = values.severity
      if (values.stepsToReproduce !== (issue.stepsToReproduce ?? '')) changes.stepsToReproduce = values.stepsToReproduce
    }
    if (type === 'STORY' && values.storyPoints !== undefined && values.storyPoints !== issue.storyPoints)
      changes.storyPoints = values.storyPoints
    if (type !== 'EPIC' && estimate !== issue.estimateMinutes) changes.estimateMinutes = estimate ?? 0
    if (!Object.keys(changes).length) {
      close()
      return
    }
    void action.run(async () => saved(await api.updateIssue(issue, changes)))
  }
  return (
    <Modal
      title={issue ? 'Edit issue' : 'Create issue'}
      close={() => {
        if (!action.busy) close()
      }}>
      <form className="form" onSubmit={submit}>
        <label>
          Title
          <input name="title" defaultValue={issue?.title} minLength={3} maxLength={200} required autoFocus />
        </label>
        <div className="field-grid">
          {issue ? (
            <div className="detail-field">
              Type<strong>{types[type]}</strong>
            </div>
          ) : (
            <label>
              Type
              <Select
                value={type}
                onChange={next => setType(next as IssueType)}
                options={toOptions(types).filter(option => access.extended || option.value !== 'EPIC')}
              />
            </label>
          )}
          <label>
            Priority
            <Select name="priority" defaultValue={issue?.priority ?? 'MEDIUM'} options={toOptions(priorities)} />
          </label>
          {!issue && (
            <label>
              Assignee
              <Select
                name="assignee"
                defaultValue=""
                disabled={assignees.length === 0}
                options={[
                  { value: '', label: 'Unassigned' },
                  ...assignees.map(member => ({
                    value: member.id,
                    label: member.id === access.member?.id ? `${member.username} (me)` : member.username,
                  })),
                ]}
              />
            </label>
          )}
          {!issue && access.extended && type !== 'EPIC' && (
            <label>
              Epic
              <Select
                name="epic"
                defaultValue=""
                options={[{ value: '', label: 'No epic' }, ...epics.map(epic => ({ value: epic.id, label: epic.title }))]}
              />
            </label>
          )}
          {type === 'STORY' && (
            <label>
              Story points
              <input
                type="number"
                name="points"
                min={0}
                max={100}
                defaultValue={issue?.storyPoints ?? ''}
                required={issue?.storyPoints != null}
              />
            </label>
          )}
          {type !== 'EPIC' && <EstimateField issue={issue} />}
          {type === 'BUG' && (
            <label>
              Severity
              <Select name="severity" defaultValue={issue?.severity ?? 'MINOR'} options={toOptions(severities)} />
            </label>
          )}
        </div>
        <label>
          Description
          <textarea name="description" defaultValue={issue?.description ?? ''} maxLength={5000} rows={3} />
        </label>
        {type === 'BUG' && (
          <label>
            Steps to reproduce
            <textarea name="steps" defaultValue={issue?.stepsToReproduce ?? ''} maxLength={5000} rows={3} />
          </label>
        )}
        <ErrorNotice text={action.error} />
        <div className="row">
          <button className="button primary" disabled={action.busy}>
            {action.busy ? 'Saving…' : issue ? 'Save changes' : 'Create issue'}
          </button>
          <button type="button" className="button" disabled={action.busy} onClick={close}>
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  )
}

function IssueDetail({
  issue,
  project,
  organization,
  user,
  close,
  openIssue,
  changed,
}: {
  issue: IssueRef
  project: Project | null
  organization?: Organization
  user?: User
  close: () => void
  openIssue: (issue: IssueRef) => void
  changed: () => void
}) {
  const { organizationId, projectId, id } = issue
  const load = useCallback(
    async (signal: AbortSignal) => {
      const ref = { organizationId, projectId, id }
      const [record, issueProject] = await Promise.all([
        api.issue(ref, signal),
        project ? Promise.resolve(project) : api.project(organizationId, projectId, signal),
      ])
      return { record, project: issueProject }
    },
    [organizationId, projectId, id, project],
  )
  const detail = useResource(load)
  if (!detail.value) {
    return (
      <Modal title="Issue" close={close}>
        <ErrorNotice text={detail.error} retry={detail.retry} />
        {detail.loading && <Loading />}
      </Modal>
    )
  }
  return (
    <IssueDetailBody
      record={detail.value.record}
      issueProject={detail.value.project}
      organization={organization}
      user={user}
      close={close}
      openIssue={openIssue}
      changed={changed}
      setRecord={record => detail.setValue(value => value && { ...value, record })}
    />
  )
}

function IssueDetailBody({
  record,
  issueProject,
  organization,
  user,
  close,
  openIssue,
  changed,
  setRecord,
}: {
  record: Issue
  issueProject: Project
  organization?: Organization
  user?: User
  close: () => void
  openIssue: (issue: IssueRef) => void
  changed: () => void
  setRecord: (record: Issue) => void
}) {
  const [tab, setTab] = useState<'comments' | 'history' | 'time'>('comments')
  const [loggingTime, setLoggingTime] = useState(false)
  const [editing, setEditing] = useState(false)
  const [revision, setRevision] = useState(0)
  const access = useProjectAccess(issueProject, organization, user, true)
  const changes = useAction()
  const { organizationId, projectId, id } = record
  const ref = useMemo(() => ({ organizationId, projectId, id }), [organizationId, projectId, id])
  const mutable = issueProject.status !== 'ARCHIVED'
  const canChange = mutable && canChangeStatus(record, access, user)
  const canEdit = mutable && access.mayView && (access.extended || user?.id === record.reporterId)
  const canAssign = mutable && (access.extended || Boolean(access.member && (!record.assigneeId || record.assigneeId === access.member.id)))
  const canChangeParent = mutable && access.extended && record.type !== 'EPIC'
  const canDelete = mutable && access.mayView && (access.admin || user?.id === record.reporterId)
  const assignees = access.extended ? access.roster.value : access.member && [access.member]
  const canTakeIssue =
    mutable && Boolean(access.member) && record.assigneeId !== access.member?.id && (access.extended || !record.assigneeId)
  const mustJoinToTake = mutable && access.admin && Boolean(access.roster.value) && !access.member
  const canLogTime = mutable && Boolean(access.member) && record.type !== 'EPIC'

  function refreshTime() {
    void changes.run(async () => apply(await api.issue(ref)))
  }
  const epics = useEpics(issueProject, canChangeParent || Boolean(record.parentId))
  const epicTitle = record.parentId ? epics.value?.find(epic => epic.id === record.parentId)?.title : undefined

  function apply(updated: Issue, message?: string) {
    setRecord(updated)
    setRevision(version => version + 1)
    changed()
    if (message) toast(message)
  }

  async function copyLink() {
    const url = window.location.origin + routePath({ name: 'project', orgId: organizationId, projectId, tab: 'issues', issueId: id })
    try {
      await navigator.clipboard.writeText(url)
      toast('Link copied')
    } catch {
      window.prompt('Copy this link', url)
    }
  }

  async function deleteIssue() {
    const children = record.type === 'EPIC' ? ' Issues linked to this epic will stay, but without an epic.' : ''
    const confirmed = await confirmAction({
      title: `Delete ${types[record.type].toLowerCase()}?`,
      message: `"${record.title}" will be deleted with its comments and history. This cannot be undone.${children}`,
      confirmLabel: 'Delete',
      danger: true,
    })
    if (!confirmed) return
    void changes.run(async () => {
      await api.deleteIssue(record)
      changed()
      toast(`${types[record.type]} deleted`)
      close()
    })
  }

  if (editing && canEdit) {
    return (
      <IssueEditor
        project={issueProject}
        issue={record}
        access={access}
        epics={[]}
        close={() => setEditing(false)}
        saved={updated => {
          apply(updated, 'Issue updated')
          setEditing(false)
        }}
      />
    )
  }

  return (
    <Modal
      title={record.title}
      close={() => {
        if (!changes.busy) close()
      }}>
      <div className="issue-detail">
        <div className="row">
          <span className="badge issue-type" data-type={record.type}>
            {types[record.type]}
          </span>
          <span className="badge priority-badge">
            <PriorityIcon priority={record.priority} />
            {priorities[record.priority]}
          </span>
          {issueProject.status === 'ARCHIVED' && <span className="badge">Archived</span>}
          <span className="detail-actions">
            <button
              type="button"
              className="icon-button"
              title="Copy link"
              aria-label="Copy link to this issue"
              onClick={() => void copyLink()}>
              <Link2 size={16} />
            </button>
            {canEdit && (
              <button className="button" disabled={changes.busy} onClick={() => setEditing(true)}>
                <Pencil size={15} />
                Edit
              </button>
            )}
            {canDelete && (
              <button className="button danger" disabled={changes.busy} onClick={() => void deleteIssue()}>
                <Trash2 size={15} />
                Delete
              </button>
            )}
          </span>
        </div>
        <p className="issue-meta">
          Created by <strong>{record.reporterUsername}</strong>{' '}
          <time dateTime={record.createdAt} title={formatDate(record.createdAt)}>
            {formatRelative(record.createdAt)}
          </time>
          {record.updatedAt !== record.createdAt && (
            <>
              {' '}
              · updated{' '}
              <time dateTime={record.updatedAt} title={formatDate(record.updatedAt)}>
                {formatRelative(record.updatedAt)}
              </time>
            </>
          )}
        </p>
        {record.description ? <p className="issue-description">{record.description}</p> : <p className="muted">No description</p>}
        <div className="field-grid">
          <label>
            Status
            <Select
              value={record.status}
              disabled={!canChange || changes.busy}
              onChange={value => {
                const next = value as IssueStatus
                void changes.run(async () => apply(await api.changeStatus(record, next), `Moved to ${statuses[next]}`))
              }}
              options={[record.status, ...workflow[record.status]].map(key => ({ value: key, label: statuses[key] }))}
            />
          </label>
          {canAssign && assignees ? (
            <label>
              Assignee
              <Select
                value={record.assigneeId ?? ''}
                disabled={changes.busy}
                onChange={member => {
                  void changes.run(async () => apply(await api.assignee(record, member), member ? 'Assignee updated' : 'Assignee removed'))
                }}
                options={[
                  { value: '', label: 'Unassigned' },
                  ...(record.assigneeId && !assignees.some(member => member.id === record.assigneeId)
                    ? [{ value: record.assigneeId, label: record.assigneeUsername ?? 'Current assignee' }]
                    : []),
                  ...assignees.map(member => ({ value: member.id, label: member.username })),
                ]}
              />
            </label>
          ) : (
            <div className="detail-field">
              Assignee
              <strong className="assignee-cell">
                <Avatar name={record.assigneeUsername} size="tiny" />
                {record.assigneeUsername ?? 'Unassigned'}
              </strong>
            </div>
          )}
          {canChangeParent ? (
            <label>
              Epic
              <Select
                value={record.parentId ?? ''}
                disabled={changes.busy || epics.loading || Boolean(epics.error)}
                onChange={parentId => {
                  void changes.run(async () => apply(await api.parent(record, parentId), parentId ? 'Epic updated' : 'Removed from epic'))
                }}
                options={[
                  { value: '', label: 'No epic' },
                  ...(record.parentId && !epics.value?.some(epic => epic.id === record.parentId)
                    ? [{ value: record.parentId, label: 'Current epic' }]
                    : []),
                  ...(epics.value ?? []).map(epic => ({ value: epic.id, label: epic.title })),
                ]}
              />
            </label>
          ) : (
            record.parentId && (
              <div className="detail-field">
                Epic
                <button
                  type="button"
                  className="text-button"
                  onClick={() => openIssue({ organizationId, projectId, id: record.parentId! })}>
                  {epicTitle ?? 'Open epic'}
                </button>
              </div>
            )
          )}
          {record.storyPoints !== null && (
            <div className="detail-field">
              Story points<strong>{record.storyPoints}</strong>
            </div>
          )}
          {record.severity && (
            <div className="detail-field">
              Severity
              <strong>
                <span className="chip severity" data-severity={record.severity}>
                  {severities[record.severity] ?? record.severity}
                </span>
              </strong>
            </div>
          )}
        </div>
        {canTakeIssue && (
          <div className="assign-me">
            <button
              type="button"
              className="button"
              disabled={changes.busy}
              onClick={() => void changes.run(async () => apply(await api.assignToMe(record), 'Assigned to you'))}>
              <UserCheck size={15} />
              Assign to me
            </button>
          </div>
        )}
        {mustJoinToTake && (
          <p className="muted small-text">To assign issues to yourself, first add yourself to this project on the Members tab.</p>
        )}
        <TimeSummary
          issue={record}
          canLog={canLogTime}
          onLog={() => setLoggingTime(true)}
          onJoin={
            mustJoinToTake && record.type !== 'EPIC'
              ? () => navigate({ name: 'project', orgId: organizationId, projectId, tab: 'members' })
              : undefined
          }
        />
        {record.stepsToReproduce && (
          <div className="detail-field">
            Steps to reproduce<p>{record.stepsToReproduce}</p>
          </div>
        )}
        {record.type === 'EPIC' && <EpicChildren key={revision} epic={record} project={issueProject} openIssue={openIssue} />}
        <ErrorNotice text={changes.error} />
        <ErrorNotice text={access.roster.error} retry={access.roster.retry} />
        <ErrorNotice text={epics.error} retry={epics.retry} />
        <div className="segmented" aria-label="Issue activity">
          <button aria-pressed={tab === 'comments'} onClick={() => setTab('comments')}>
            Comments
          </button>
          {record.type !== 'EPIC' && (
            <button aria-pressed={tab === 'time'} onClick={() => setTab('time')}>
              Work log
            </button>
          )}
          <button aria-pressed={tab === 'history'} onClick={() => setTab('history')}>
            History
          </button>
        </div>
        {tab === 'comments' ? (
          <Comments issue={ref} mutable={mutable} userId={user?.id} admin={access.admin} />
        ) : tab === 'time' && record.type !== 'EPIC' ? (
          <WorkLogList
            key={revision}
            issue={{ ...ref, title: record.title }}
            mutable={mutable}
            userId={user?.id}
            admin={access.admin}
            changed={refreshTime}
          />
        ) : (
          <History key={revision} issue={ref} project={issueProject} />
        )}
        {loggingTime && (
          <LogTimeDialog
            issue={{ ...ref, title: record.title }}
            close={() => setLoggingTime(false)}
            saved={() => {
              setLoggingTime(false)
              refreshTime()
            }}
          />
        )}
      </div>
    </Modal>
  )
}

function EpicChildren({ epic, project, openIssue }: { epic: Issue; project: Project; openIssue: (issue: IssueRef) => void }) {
  const children = useResource(
    useCallback((signal: AbortSignal) => api.allIssues(project, { parentId: epic.id }, signal), [project, epic.id]),
  )
  const items = children.value?.content ?? []
  const done = items.filter(item => item.status === 'DONE').length
  const percent = items.length ? Math.round((done / items.length) * 100) : 0
  return (
    <section className="epic-children" aria-label="Issues in this epic">
      <header>
        <h3>Issues in this epic</h3>
        {items.length > 0 && (
          <span className="muted">
            {done} of {items.length} done · {percent}%
          </span>
        )}
      </header>
      {items.length > 0 && (
        <div
          className="progress"
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={percent}
          aria-label="Epic progress">
          {(['DONE', 'IN_REVIEW', 'IN_PROGRESS'] as IssueStatus[]).map(status => {
            const count = items.filter(item => item.status === status).length
            return (
              count > 0 && (
                <span
                  key={status}
                  data-status={status}
                  style={{ width: `${(count / items.length) * 100}%` }}
                  title={`${statuses[status]}: ${count}`}
                />
              )
            )
          })}
        </div>
      )}
      <ErrorNotice text={children.error} retry={children.retry} />
      {children.loading && !children.value && <Loading />}
      {children.value && items.length === 0 && <p className="muted">No issues are linked to this epic yet.</p>}
      <ul className="child-list">
        {items.map(item => (
          <li key={item.id}>
            <button type="button" className="row-link" onClick={() => openIssue(item)}>
              <span className="badge issue-type" data-type={item.type}>
                {types[item.type]}
              </span>
              <span className="issue-title">{item.title}</span>
              <span className="issue-state">
                <span className="status-dot" data-status={item.status} />
                {statuses[item.status]}
              </span>
              <Avatar name={item.assigneeUsername} size="tiny" />
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

function Comments({ issue, mutable, userId, admin }: { issue: IssueRef; mutable: boolean; userId?: string; admin: boolean }) {
  const [page, setPage] = useState(0)
  const [text, setText] = useState('')
  const [editingId, setEditingId] = useState('')
  const [draft, setDraft] = useState('')
  const load = useCallback((signal: AbortSignal) => api.comments(issue, page, signal), [issue, page])
  const comments = useResource(load)
  const action = useAction()
  const itemAction = useAction()

  function startEditing(item: Comment) {
    setEditingId(item.id)
    setDraft(item.text)
  }

  function saveEdit(event: FormEvent<HTMLFormElement>, item: Comment) {
    event.preventDefault()
    const next = draft.trim()
    if (next === item.text) {
      setEditingId('')
      return
    }
    void itemAction.run(async () => {
      const updated = await api.editComment(issue, item, next)
      comments.setValue(data => data && { ...data, content: data.content.map(entry => (entry.id === updated.id ? updated : entry)) })
      setEditingId('')
    })
  }

  async function remove(item: Comment) {
    const confirmed = await confirmAction({
      title: 'Delete comment?',
      message: 'The comment will be deleted. This cannot be undone.',
      confirmLabel: 'Delete',
      danger: true,
    })
    if (!confirmed) return
    void itemAction.run(async () => {
      await api.deleteComment(issue, item)
      if (page > 0 && comments.value?.content.length === 1) setPage(page - 1)
      else comments.reload()
      toast('Comment deleted')
    })
  }

  return (
    <section className="activity">
      <ErrorNotice text={comments.error} retry={comments.retry} />
      <ErrorNotice text={itemAction.error} />
      {comments.loading && !comments.value ? (
        <Loading />
      ) : (
        <>
          {comments.value?.content.length === 0 && <p className="muted">No comments yet</p>}
          {comments.value?.content.map(item => {
            const own = item.authorId === userId
            return (
              <article key={item.id}>
                <header>
                  <span className="comment-author">
                    <Avatar name={item.authorUsername} size="tiny" />
                    <strong>{item.authorUsername}</strong>
                  </span>
                  <span className="row comment-meta">
                    <time dateTime={item.createdAt} title={formatDate(item.createdAt)}>
                      {formatRelative(item.createdAt)}
                    </time>
                    {mutable && editingId !== item.id && own && (
                      <button
                        type="button"
                        className="icon-button small"
                        title="Edit comment"
                        aria-label="Edit comment"
                        disabled={itemAction.busy}
                        onClick={() => startEditing(item)}>
                        <Pencil size={14} />
                      </button>
                    )}
                    {mutable && editingId !== item.id && (own || admin) && (
                      <button
                        type="button"
                        className="icon-button small"
                        title="Delete comment"
                        aria-label="Delete comment"
                        disabled={itemAction.busy}
                        onClick={() => void remove(item)}>
                        <Trash2 size={14} />
                      </button>
                    )}
                  </span>
                </header>
                {editingId === item.id ? (
                  <form className="form" onSubmit={event => saveEdit(event, item)}>
                    <label>
                      Edit comment
                      <textarea
                        required
                        autoFocus
                        value={draft}
                        onChange={event => setDraft(event.target.value)}
                        maxLength={5000}
                        rows={3}
                      />
                    </label>
                    <div className="row">
                      <button className="button primary" disabled={itemAction.busy || !draft.trim()}>
                        Save
                      </button>
                      <button type="button" className="button" disabled={itemAction.busy} onClick={() => setEditingId('')}>
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p>{item.text}</p>
                    {item.editedAt && <small title={formatDate(item.editedAt)}>Edited</small>}
                  </>
                )}
              </article>
            )
          })}
          <Pagination data={comments.value} onChange={setPage} disabled={itemAction.busy} />
        </>
      )}
      {mutable && (
        <form
          className="form"
          onSubmit={event => {
            event.preventDefault()
            void action.run(async () => {
              await api.comment(issue, text.trim())
              setText('')
              const previous = comments.value
              const lastPage = previous ? Math.floor(previous.totalElements / previous.size) : page
              if (page === lastPage) comments.reload()
              else setPage(lastPage)
            })
          }}>
          <label>
            Comment
            <textarea
              required
              value={text}
              onChange={event => setText(event.target.value)}
              maxLength={5000}
              rows={2}
              placeholder="Write a comment…"
            />
          </label>
          <ErrorNotice text={action.error} />
          <button className="button primary" disabled={action.busy || !text.trim()}>
            Post comment
          </button>
        </form>
      )}
    </section>
  )
}

function History({ issue, project }: { issue: IssueRef; project: Project | null }) {
  const [page, setPage] = useState(0)
  const load = useCallback((signal: AbortSignal) => api.history(issue, page, signal), [issue, page])
  const history = useResource(load)
  const needsEpics = Boolean(history.value?.content.some(item => item.type === 'PARENT_CHANGED'))
  const epics = useEpics(project, needsEpics)
  const show = (item: IssueEvent, value: string | null) => {
    if (value === null) return item.type === 'PARENT_CHANGED' ? 'No epic' : item.type === 'ASSIGNEE_CHANGED' ? 'Unassigned' : 'Empty'
    if (item.type !== 'PARENT_CHANGED') return fieldValue(item.field, value)
    if (epics.loading) return '…'
    return epics.value?.find(epic => epic.id === value)?.title ?? 'Deleted epic'
  }
  const title = (item: IssueEvent) =>
    item.type === 'FIELD_CHANGED' && item.field
      ? `${fieldNames[item.field] ?? item.field} changed`
      : item.type === 'CREATED' && item.newValue
        ? `Created as ${types[item.newValue as IssueType] ?? item.newValue}`
        : eventNames[item.type]
  return (
    <section className="activity">
      <ErrorNotice text={history.error} retry={history.retry} />
      {history.loading && !history.value ? (
        <Loading />
      ) : (
        <>
          {history.value?.content.length === 0 && <p className="muted">No activity yet</p>}
          {history.value?.content.map(item => (
            <article key={item.id}>
              <header>
                <strong>{title(item)}</strong>
                <time dateTime={item.createdAt} title={formatDate(item.createdAt)}>
                  {formatRelative(item.createdAt)}
                </time>
              </header>
              <p className="muted">by {item.actorUsername}</p>
              {item.type !== 'CREATED' && (item.oldValue !== null || item.newValue !== null) && (
                <p className="change-line">
                  <span className="old-value">{show(item, item.oldValue)}</span> → <span>{show(item, item.newValue)}</span>
                </p>
              )}
            </article>
          ))}
          <Pagination data={history.value} onChange={setPage} />
        </>
      )}
    </section>
  )
}
