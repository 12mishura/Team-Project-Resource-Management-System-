import { Columns3, GripVertical, List, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { useCallback, useDeferredValue, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import {
  api, type Comment, type Issue, type IssueRef, type IssueEvent, type IssueFields, type IssuePriority, type IssueStatus, type IssueType,
  type Organization, type Project, type User,
} from './api'
import { Empty, ErrorNotice, Loading, Modal, Pagination, useAction, useResource } from './ui'

const statuses: Record<IssueStatus, string> = {
  TODO: 'To do', IN_PROGRESS: 'In progress', IN_REVIEW: 'In review', DONE: 'Done',
}
const workflow: Record<IssueStatus, IssueStatus[]> = {
  TODO: ['IN_PROGRESS'], IN_PROGRESS: ['TODO', 'IN_REVIEW'], IN_REVIEW: ['IN_PROGRESS', 'DONE'], DONE: ['IN_PROGRESS'],
}
const priorities: Record<IssuePriority, string> = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }
const types: Record<IssueType, string> = { TASK: 'Task', STORY: 'Story', BUG: 'Bug', EPIC: 'Epic' }
const eventNames: Record<IssueEvent['type'], string> = {
  CREATED: 'Issue created', FIELD_CHANGED: 'Field updated', STATUS_CHANGED: 'Status changed',
  ASSIGNEE_CHANGED: 'Assignee changed', PARENT_CHANGED: 'Epic changed',
}

function useProjectAccess(project: Project | null, organization?: Organization, user?: User, includeRoster = false) {
  const privileged = organization?.role === 'OWNER' || organization?.role === 'ADMIN'
  const load = useCallback(async (signal: AbortSignal) => {
    if (!project || !user || (privileged && !includeRoster)) return []
    return api.projectMembers(project, signal)
  }, [project, privileged, user, includeRoster])
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

function canChangeStatus(issue: Issue, access: ReturnType<typeof useProjectAccess>, user?: User) {
  return access.mayView && (access.extended || user?.id === issue.reporterId
    || Boolean(issue.assigneeId && issue.assigneeId === access.member?.id))
}

export function Issues({ project, organizations, user, selected, onSelect, projectControls, projectError = '' }: {
  project: Project | null; organizations: Organization[]; user?: User; selected?: IssueRef; onSelect: (issue?: IssueRef) => void
  projectControls?: ReactNode; projectError?: string
}) {
  const [layout, setLayout] = useState<'board' | 'list'>('board')
  const [query, setQuery] = useState('')
  const [type, setType] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const [moveMessage, setMoveMessage] = useState('')
  const move = useAction()
  const deferredQuery = useDeferredValue(query.trim())
  const organization = organizations.find(item => item.id === project?.organizationId)
  const access = useProjectAccess(project, organization, user)
  const wholeBoard = Boolean(project) && layout === 'board'
  const load = useCallback((signal: AbortSignal) => {
    const filters = { q: deferredQuery, type, status }
    return project && wholeBoard ? api.allIssues(project, filters, signal) : api.issues(project, page, filters, signal)
  }, [project, wholeBoard, page, deferredQuery, type, status])
  const issues = useResource(load)
  const content = issues.value?.content ?? []
  const archived = project?.status === 'ARCHIVED'
  const noAccess = Boolean(project && access.roster.value && !access.mayView)

  function moveIssue(issue: Issue, next: IssueStatus) {
    if (archived || !canChangeStatus(issue, access, user) || !workflow[issue.status].includes(next)) return
    setMoveMessage('')
    void move.run(async () => {
      const updated = await api.changeStatus(issue, next)
      if (status) {
        if (!wholeBoard && page > 0 && content.length === 1) setPage(page - 1)
        else issues.reload()
      } else {
        issues.setValue(current => current && {
          ...current, content: current.content.map(item => item.id === updated.id ? updated : item),
        })
      }
      setMoveMessage(`${updated.title} moved to ${statuses[updated.status]}.`)
    })
  }

  return <>
    <header className="page-heading">
      <div><h1>{project?.title ?? 'My issues'}</h1><p>{project ? project.description : 'Issues assigned to you across all organizations'}</p></div>
      <div className="row">
        {archived && <span className="badge">Archived · issues are read-only</span>}
        {projectControls}
        {project && !archived && access.mayView && <button className="button primary" onClick={() => setCreating(true)}><Plus size={17} />Create issue</button>}
      </div>
    </header>
    <ErrorNotice text={projectError} />
    {!noAccess && <div className="toolbar issue-toolbar">
      {project && <div className="segmented" aria-label="Issue view">
        <button aria-pressed={layout === 'board'} onClick={() => { setLayout('board'); setPage(0) }}><Columns3 size={17} />Board</button>
        <button aria-pressed={layout === 'list'} onClick={() => { setLayout('list'); setPage(0) }}><List size={17} />List</button>
      </div>}
      <div className="row filters">
        <label className="search"><Search size={16} /><input type="search" aria-label="Search issues by title" placeholder="Search issues" value={query} disabled={move.busy}
          onChange={event => { setQuery(event.target.value); setPage(0) }} /></label>
        <select aria-label="Issue type" value={type} disabled={move.busy} onChange={event => { setType(event.target.value); setPage(0) }}>
          <option value="">All types</option>{Object.entries(types).map(([key, label]) => <option value={key} key={key}>{label}</option>)}
        </select>
        <select aria-label="Issue status" value={status} disabled={move.busy} onChange={event => { setStatus(event.target.value); setPage(0) }}>
          <option value="">All statuses</option>{Object.entries(statuses).map(([key, label]) => <option value={key} key={key}>{label}</option>)}
        </select>
      </div>
    </div>}
    {noAccess && <Empty title="You are not a member of this project">Ask an organization owner or admin to add you to the project.</Empty>}
    {!noAccess && <ErrorNotice text={issues.error || access.roster.error} retry={issues.error ? issues.retry : access.roster.retry} />}
    <ErrorNotice text={move.error} />
    <div className="sr-only" role="status">{moveMessage}</div>
    {noAccess ? null : issues.loading ? <Loading /> : issues.value && <>
      {content.length === 0 ? <Empty title={query || type || status ? 'No matching issues' : 'No issues yet'} /> :
        project && layout === 'board' ? <IssueBoard issues={content} busy={move.busy}
          canMove={issue => !archived && canChangeStatus(issue, access, user)}
          onOpen={onSelect} onMove={moveIssue} /> : <div className="issue-list">
          {content.map(item => <button className="issue-row" key={item.id} onClick={() => onSelect(item)}>
            <span className="badge issue-type" data-type={item.type}>{types[item.type]}</span><strong>{item.title}
              {!project && <small>{organizations.find(org => org.id === item.organizationId)?.title}</small>}</strong>
            <span className="issue-state"><span className="status-dot" data-status={item.status} />{statuses[item.status]}</span>
          </button>)}
        </div>}
      <div className="list-footer"><span>{issues.value.totalElements} {issues.value.totalElements === 1 ? 'matching issue' : 'matching issues'}</span><Pagination data={issues.value} onChange={setPage} disabled={move.busy} /></div>
    </>}
    {creating && project && <IssueEditor project={project} extended={access.extended} close={() => setCreating(false)} saved={issue => {
      setCreating(false)
      issues.reload()
      onSelect(issue)
    }} />}
    {selected && <IssueDetail key={selected.id} issue={selected} project={project} user={user}
      organization={organizations.find(item => item.id === selected.organizationId)}
      close={() => onSelect(undefined)} changed={issues.reload} />}
  </>
}

function IssueBoard({ issues, busy, canMove, onOpen, onMove }: {
  issues: Issue[]
  busy: boolean
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

  return <div className="board" aria-busy={busy}>
    {(Object.entries(statuses) as [IssueStatus, string][]).map(([status, label]) => {
      const column = issues.filter(issue => issue.status === status)
      const acceptsDrop = Boolean(dragged && canMove(dragged) && workflow[dragged.status].includes(status)) && !busy
      return <section className="lane" key={status} aria-label={label}
        data-drop={acceptsDrop ? (target === status ? 'active' : 'available') : undefined}
        onDragOver={event => {
          if (!acceptsDrop) return
          event.preventDefault()
          event.dataTransfer.dropEffect = 'move'
          setTarget(status)
        }}
        onDragLeave={event => {
          if (!(event.relatedTarget instanceof Node && event.currentTarget.contains(event.relatedTarget))) {
            setTarget(current => current === status ? undefined : current)
          }
        }}
        onDrop={event => {
          if (!acceptsDrop || !dragged) return
          event.preventDefault()
          clearDrag()
          onMove(dragged, status)
        }}>
        <h2><span className="status-dot" data-status={status} />{label}<span className="count">{column.length}</span></h2>
        {column.map(issue => {
          const movable = canMove(issue)
          return <button className="ticket" key={issue.id} disabled={busy}
            draggable={movable && !busy} data-dragging={draggedId === issue.id || undefined}
            onClick={() => onOpen(issue)}
            onDragStart={event => {
              if (!movable || busy) { event.preventDefault(); return }
              event.dataTransfer.effectAllowed = 'move'
              event.dataTransfer.setData('text/plain', issue.id)
              setDraggedId(issue.id)
            }}
            onDragEnd={clearDrag}>
            <span className="ticket-heading"><span className="badge issue-type" data-type={issue.type}>{types[issue.type]}</span>
              {movable && <GripVertical className="ticket-grip" size={15} aria-hidden="true" />}
            </span>
            <strong>{issue.title}</strong>
            <span className="ticket-meta">{priorities[issue.priority]}<span>{issue.assigneeUsername ?? 'Unassigned'}</span></span>
          </button>
        })}
        {column.length === 0 && <p className="lane-empty">{acceptsDrop ? 'Drop here' : 'No issues'}</p>}
      </section>
    })}
  </div>
}

function IssueEditor({ project, issue, extended, close, saved }: {
  project: Project; issue?: Issue; extended: boolean; close: () => void; saved: (issue: Issue) => void
}) {
  const [type, setType] = useState<IssueType>(issue?.type ?? 'TASK')
  const action = useAction()
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const points = fields.get('points')
    const values: IssueFields = {
      title: String(fields.get('title')).trim(),
      description: String(fields.get('description')),
      priority: fields.get('priority') as IssuePriority,
      ...(type === 'BUG' ? {
        severity: String(fields.get('severity')),
        stepsToReproduce: String(fields.get('steps')),
      } : {}),
      ...(type === 'STORY' && points ? { storyPoints: Number(points) } : {}),
    }
    if (!issue) {
      void action.run(async () => saved(await api.createIssue(project, {
        ...values, type, description: values.description?.trim() || undefined,
        stepsToReproduce: values.stepsToReproduce?.trim() || undefined,
      })))
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
    if (type === 'STORY' && values.storyPoints !== undefined && values.storyPoints !== issue.storyPoints) changes.storyPoints = values.storyPoints
    if (!Object.keys(changes).length) { close(); return }
    void action.run(async () => saved(await api.updateIssue(issue, changes)))
  }
  return <Modal title={issue ? 'Edit issue' : 'Create issue'} close={() => { if (!action.busy) close() }}>
    <form className="form" onSubmit={submit}>
      <label>Title<input name="title" defaultValue={issue?.title} minLength={3} maxLength={200} required /></label>
      <div className="field-grid">
        {issue ? <div className="detail-field">Type<strong>{types[type]}</strong></div> :
        <label>Type<select value={type} onChange={event => setType(event.target.value as IssueType)}>
          {Object.entries(types).filter(([key]) => extended || key !== 'EPIC').map(([key, label]) => <option key={key} value={key}>{label}</option>)}
        </select></label>}
        <label>Priority<select name="priority" defaultValue={issue?.priority ?? 'MEDIUM'}>{Object.entries(priorities).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
        {type === 'STORY' && <label>Story points<input type="number" name="points" min={0} max={100} defaultValue={issue?.storyPoints ?? ''}
          required={issue?.storyPoints != null} /></label>}
        {type === 'BUG' && <label>Severity<select name="severity" defaultValue={issue?.severity ?? 'MINOR'}>{['MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'].map(level => <option key={level}>{level}</option>)}</select></label>}
      </div>
      <label>Description<textarea name="description" defaultValue={issue?.description ?? ''} maxLength={5000} rows={3} /></label>
      {type === 'BUG' && <label>Steps to reproduce<textarea name="steps" defaultValue={issue?.stepsToReproduce ?? ''} maxLength={5000} rows={3} /></label>}
      <ErrorNotice text={action.error} />
      <div className="row"><button className="button primary" disabled={action.busy}>{action.busy ? 'Saving…' : issue ? 'Save changes' : 'Create issue'}</button>
        <button type="button" className="button" disabled={action.busy} onClick={close}>Cancel</button></div>
    </form>
  </Modal>
}

function IssueDetail({ issue, project, organization, user, close, changed }: {
  issue: IssueRef; project: Project | null; organization?: Organization; user?: User; close: () => void; changed: () => void
}) {
  const { organizationId, projectId, id } = issue
  const load = useCallback(async (signal: AbortSignal) => {
    const ref = { organizationId, projectId, id }
    const [record, issueProject] = await Promise.all([
      api.issue(ref, signal),
      project ? Promise.resolve(project) : api.project(organizationId, projectId, signal),
    ])
    return { record, project: issueProject }
  }, [organizationId, projectId, id, project])
  const detail = useResource(load)
  if (!detail.value) {
    return <Modal title="Issue" close={close}>
      <ErrorNotice text={detail.error} retry={detail.retry} />
      {detail.loading && <Loading />}
    </Modal>
  }
  return <IssueDetailBody record={detail.value.record} issueProject={detail.value.project} organization={organization} user={user}
    close={close} changed={changed} setRecord={record => detail.setValue(value => value && { ...value, record })} />
}

function IssueDetailBody({ record, issueProject, organization, user, close, changed, setRecord }: {
  record: Issue; issueProject: Project; organization?: Organization; user?: User
  close: () => void; changed: () => void; setRecord: (record: Issue) => void
}) {
  const [tab, setTab] = useState<'comments' | 'history'>('comments')
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
  const epics = useResource(useCallback(signal => canChangeParent && issueProject
    ? api.epics(issueProject, signal) : Promise.resolve([]), [canChangeParent, issueProject]))

  function apply(updated: Issue) {
    setRecord(updated)
    setRevision(version => version + 1)
    changed()
  }

  function deleteIssue() {
    const children = record.type === 'EPIC' ? ' Issues linked to this epic will stay, but without an epic.' : ''
    if (!window.confirm(`Delete "${record.title}" with its comments and history? This cannot be undone.${children}`)) return
    void changes.run(async () => {
      await api.deleteIssue(record)
      changed()
      close()
    })
  }

  if (editing && canEdit) {
    return <IssueEditor project={issueProject} issue={record} extended={access.extended}
      close={() => setEditing(false)} saved={updated => { apply(updated); setEditing(false) }} />
  }

  return <Modal title={record.title} close={() => { if (!changes.busy) close() }}>
    <div className="issue-detail">
      <div className="row"><span className="badge issue-type" data-type={record.type}>{types[record.type]}</span>
        <span className="badge">{priorities[record.priority]} priority</span>
        {issueProject.status === 'ARCHIVED' && <span className="badge">Archived</span>}
        {canEdit && <button className="button" disabled={changes.busy} onClick={() => setEditing(true)}><Pencil size={15} />Edit issue</button>}
        {canDelete && <button className="button danger" disabled={changes.busy} onClick={deleteIssue}><Trash2 size={15} />Delete</button>}
      </div>
      {revision > 0 && <p role="status" className="success-message">Issue updated.</p>}
      {record.description && <p>{record.description}</p>}
      <div className="field-grid">
        <label>Status<select value={record.status} disabled={!canChange || changes.busy} onChange={event => {
          const next = event.target.value as IssueStatus
          void changes.run(async () => apply(await api.changeStatus(record, next)))
        }}>{[record.status, ...workflow[record.status]].map(key => <option key={key} value={key}>{statuses[key]}</option>)}</select></label>
        {canAssign && assignees ? <label>Assignee<select value={record.assigneeId ?? ''} disabled={changes.busy} onChange={event => {
          const member = event.target.value
          void changes.run(async () => apply(await api.assignee(record, member)))
        }}>
          <option value="">Unassigned</option>
          {record.assigneeId && !assignees.some(member => member.id === record.assigneeId) &&
            <option value={record.assigneeId}>{record.assigneeUsername}</option>}
          {assignees.map(member => <option key={member.id} value={member.id}>{member.username}</option>)}
        </select></label> : <div className="detail-field">Assignee<strong>{record.assigneeUsername ?? 'Unassigned'}</strong></div>}
        {canChangeParent && <label>Epic<select value={record.parentId ?? ''} disabled={changes.busy || epics.loading || Boolean(epics.error)} onChange={event => {
          const parentId = event.target.value
          void changes.run(async () => apply(await api.parent(record, parentId)))
        }}>
          <option value="">No epic</option>
          {record.parentId && !epics.value?.some(epic => epic.id === record.parentId) && <option value={record.parentId}>Current epic</option>}
          {epics.value?.map(epic => <option key={epic.id} value={epic.id}>{epic.title}</option>)}
        </select></label>}
        {record.storyPoints !== null && <div className="detail-field">Estimate<strong>{record.storyPoints} SP</strong></div>}
        {record.severity && <div className="detail-field">Severity<strong>{record.severity}</strong></div>}
      </div>
      {record.stepsToReproduce && <div className="detail-field">Steps to reproduce<p>{record.stepsToReproduce}</p></div>}
      <ErrorNotice text={changes.error} />
      <ErrorNotice text={access.roster.error} retry={access.roster.retry} />
      <ErrorNotice text={epics.error} retry={epics.retry} />
      <div className="segmented" aria-label="Issue activity">
        <button aria-pressed={tab === 'comments'} onClick={() => setTab('comments')}>Comments</button>
        <button aria-pressed={tab === 'history'} onClick={() => setTab('history')}>History</button>
      </div>
      {tab === 'comments' ? <Comments issue={ref} mutable={mutable} userId={user?.id} admin={access.admin} /> : <History key={revision} issue={ref} project={issueProject} />}
    </div>
  </Modal>
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
    if (next === item.text) { setEditingId(''); return }
    void itemAction.run(async () => {
      const updated = await api.editComment(issue, item, next)
      comments.setValue(data => data && { ...data, content: data.content.map(entry => entry.id === updated.id ? updated : entry) })
      setEditingId('')
    })
  }

  function remove(item: Comment) {
    if (!window.confirm('Delete this comment? This cannot be undone.')) return
    void itemAction.run(async () => {
      await api.deleteComment(issue, item)
      if (page > 0 && comments.value?.content.length === 1) setPage(page - 1)
      else comments.reload()
    })
  }

  return <section className="activity">
    <ErrorNotice text={comments.error} retry={comments.retry} />
    <ErrorNotice text={itemAction.error} />
    {comments.loading ? <Loading /> : <>
      {comments.value?.content.length === 0 && <p className="muted">No comments yet</p>}
      {comments.value?.content.map(item => {
        const own = item.authorId === userId
        return <article key={item.id}>
          <header><strong>{item.authorUsername}</strong>
            <span className="row comment-meta"><time dateTime={item.createdAt}>{formatDate(item.createdAt)}</time>
              {mutable && editingId !== item.id && own && <button type="button" className="icon-button small" title="Edit comment"
                aria-label="Edit comment" disabled={itemAction.busy} onClick={() => startEditing(item)}><Pencil size={14} /></button>}
              {mutable && editingId !== item.id && (own || admin) && <button type="button" className="icon-button small" title="Delete comment"
                aria-label="Delete comment" disabled={itemAction.busy} onClick={() => remove(item)}><Trash2 size={14} /></button>}
            </span>
          </header>
          {editingId === item.id ? <form className="form" onSubmit={event => saveEdit(event, item)}>
            <label>Edit comment<textarea required autoFocus value={draft} onChange={event => setDraft(event.target.value)} maxLength={5000} rows={3} /></label>
            <div className="row">
              <button className="button primary" disabled={itemAction.busy || !draft.trim()}>Save</button>
              <button type="button" className="button" disabled={itemAction.busy} onClick={() => setEditingId('')}>Cancel</button>
            </div>
          </form> : <><p>{item.text}</p>{item.editedAt && <small title={formatDate(item.editedAt)}>Edited</small>}</>}
        </article>
      })}
      <Pagination data={comments.value} onChange={setPage} disabled={itemAction.busy} />
    </>}
    {mutable && <form className="form" onSubmit={event => {
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
      <label>Comment<textarea required value={text} onChange={event => setText(event.target.value)} maxLength={5000} rows={2} /></label>
      <ErrorNotice text={action.error} />
      <button className="button primary" disabled={action.busy || !text.trim()}>Post comment</button>
    </form>}
  </section>
}

const dateFormatter = new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' })
const formatDate = (value: string) => dateFormatter.format(new Date(value))

function History({ issue, project }: { issue: IssueRef; project: Project | null }) {
  const [page, setPage] = useState(0)
  const load = useCallback((signal: AbortSignal) => api.history(issue, page, signal), [issue, page])
  const history = useResource(load)
  const needsEpics = Boolean(history.value?.content.some(item => item.type === 'PARENT_CHANGED'))
  const epics = useResource(useCallback((signal: AbortSignal) => needsEpics && project
    ? api.epics(project, signal) : Promise.resolve([]), [needsEpics, project]))
  const show = (item: IssueEvent, value: string | null) => {
    if (value === null) return item.type === 'PARENT_CHANGED' ? 'No epic' : '—'
    if (item.type !== 'PARENT_CHANGED') return value
    if (epics.loading) return '…'
    return epics.value?.find(epic => epic.id === value)?.title ?? 'Deleted epic'
  }
  return <section className="activity">
    <ErrorNotice text={history.error} retry={history.retry} />
    {history.loading ? <Loading /> : <>
      {history.value?.content.length === 0 && <p className="muted">No activity yet</p>}
      {history.value?.content.map(item => <article key={item.id}>
        <header><strong>{eventNames[item.type]}</strong><time dateTime={item.createdAt}>{formatDate(item.createdAt)}</time></header>
        <p>{item.actorUsername}{item.field ? ` · ${item.field}` : ''}</p>
        {(item.oldValue || item.newValue) && <small>{show(item, item.oldValue)} → {show(item, item.newValue)}</small>}
      </article>)}
      <Pagination data={history.value} onChange={setPage} />
    </>}
  </section>
}
