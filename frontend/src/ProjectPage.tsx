import { Bug, ChartColumn, CircleCheck, Columns3, List, ListTodo, Lock, Settings, Trash2, Users } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import {
  api,
  type Issue,
  type IssueRef,
  type IssueStatus,
  type Metadata,
  type Organization,
  type Project,
  type ProjectStatus,
  type User,
} from './api'
import { confirmAction, toast } from './feedbackStore'
import { Avatar, PriorityIcon, StatusBar } from './bits'
import { Issues } from './Issues'
import { describeActivity, formatDate, formatRelative, noun, plural, projectStatuses, readMetadata, statuses, toOptions } from './labels'
import { MetadataFields } from './forms'
import { ProjectMembers } from './ProjectMembers'
import { rememberProject } from './recent'
import { Select } from './Select'
import type { ProjectTab } from './router'
import { useAction, useResource } from './hooks'
import { Empty, ErrorNotice, Loading, Pagination } from './ui'

const tabIcons: Record<ProjectTab, ReactNode> = {
  board: <Columns3 size={16} />,
  issues: <List size={16} />,
  overview: <ChartColumn size={16} />,
  members: <Users size={16} />,
  settings: <Settings size={16} />,
}
const tabLabels: Record<ProjectTab, string> = {
  board: 'Board',
  issues: 'Issues',
  overview: 'Overview',
  members: 'Members',
  settings: 'Settings',
}

export function ProjectPage({
  organization,
  projectId,
  tab,
  issueId,
  user,
  onTab,
  onSelectIssue,
  onDeleted,
}: {
  organization: Organization
  projectId: string
  tab: ProjectTab
  issueId?: string
  user?: User
  onTab: (tab: ProjectTab, issueId?: string) => void
  onSelectIssue: (issueId?: string) => void
  onDeleted: () => void
}) {
  const detail = useResource(
    useCallback((signal: AbortSignal) => api.project(organization.id, projectId, signal), [organization.id, projectId]),
  )
  const current = detail.value
  const canManage = organization.role !== 'USER'
  const roster = useResource(
    useCallback(
      (signal: AbortSignal) => (!canManage && current ? api.projectMembers(current, signal) : Promise.resolve(null)),
      [canManage, current],
    ),
  )
  const ownMembership = user ? roster.value?.find(member => member.email === user.email) : undefined
  const noAccess = Boolean(!canManage && user && roster.value && !ownMembership)
  const projectAdmin = canManage || ownMembership?.role === 'ADMIN'
  const tabs = noAccess ? [] : (Object.keys(tabLabels) as ProjectTab[]).filter(item => item !== 'settings' || projectAdmin)

  useEffect(() => {
    if (current) rememberProject({ id: current.id, organizationId: current.organizationId, title: current.title })
  }, [current])

  if (!current)
    return (
      <>
        <ErrorNotice text={detail.error} retry={detail.retry} />
        {detail.loading && <Loading />}
      </>
    )

  const heading = (action: ReactNode) => (
    <>
      <header className="page-heading project-heading">
        <div>
          <h1>{current.title}</h1>
          <p>{current.description || 'No description'}</p>
        </div>
        <div className="row">
          {current.status !== 'ACTIVE' && (
            <span className="badge" data-project-status={current.status}>
              {projectStatuses[current.status]}
            </span>
          )}
          {action}
        </div>
      </header>
      {tabs.length > 0 && (
        <nav className="tabs" aria-label="Project sections">
          {tabs.map(item => (
            <button key={item} type="button" className="tab" aria-current={item === tab ? 'page' : undefined} onClick={() => onTab(item)}>
              {tabIcons[item]}
              <span>{tabLabels[item]}</span>
            </button>
          ))}
        </nav>
      )}
    </>
  )

  if (noAccess)
    return (
      <>
        {heading(null)}
        <Empty title="You are not a member of this project" icon={<Lock size={22} />}>
          Ask an organization owner or admin to add you to the project.
        </Empty>
      </>
    )

  const selected = issueId ? { organizationId: current.organizationId, projectId: current.id, id: issueId } : undefined

  if (tab === 'board' || tab === 'issues') {
    return (
      <Issues
        key={tab}
        project={current}
        organizations={[organization]}
        user={user}
        layout={tab === 'board' ? 'board' : 'list'}
        selected={selected}
        onSelect={issue => onSelectIssue(issue?.id)}
        heading={heading}
      />
    )
  }

  return (
    <>
      {heading(null)}
      {tab === 'overview' && <ProjectOverview project={current} openIssue={issue => onTab('issues', issue.id)} />}
      {tab === 'members' && (
        <ProjectMembers project={current} canManage={projectAdmin} selfId={canManage ? undefined : ownMembership?.id} />
      )}
      {tab === 'settings' &&
        (projectAdmin ? (
          <ProjectSettings project={current} organization={organization} onSaved={detail.setValue} onDeleted={onDeleted} />
        ) : (
          <Empty title="Only project admins and organization owners or admins can change project settings" icon={<Settings size={22} />} />
        ))}
    </>
  )
}

function ProjectOverview({ project, openIssue }: { project: Project; openIssue: (issue: IssueRef) => void }) {
  const data = useResource(useCallback((signal: AbortSignal) => api.allIssues(project, {}, signal), [project]))
  const issues = useMemo(() => data.value?.content ?? [], [data.value])
  const summary = useMemo(() => {
    const counts: Record<IssueStatus, number> = { TODO: 0, IN_PROGRESS: 0, IN_REVIEW: 0, DONE: 0 }
    issues.forEach(issue => {
      counts[issue.status]++
    })
    const open = issues.filter(issue => issue.status !== 'DONE')
    const workload = new Map<string, Issue[]>()
    open.forEach(issue => {
      const key = issue.assigneeUsername ?? ''
      workload.set(key, [...(workload.get(key) ?? []), issue])
    })
    const people = [...workload.entries()].sort((a, b) => b[1].length - a[1].length || a[0].localeCompare(b[0]))
    const epics = issues
      .filter(issue => issue.type === 'EPIC')
      .map(epic => {
        const children = issues.filter(issue => issue.parentId === epic.id)
        return { epic, total: children.length, done: children.filter(child => child.status === 'DONE').length }
      })
    const bugs = open.filter(issue => issue.type === 'BUG')
    return { counts, open, people, epics, bugs }
  }, [issues])
  const maxLoad = Math.max(1, ...summary.people.map(([, items]) => items.length))
  const percent = issues.length ? Math.round((summary.counts.DONE / issues.length) * 100) : 0

  if (data.error) return <ErrorNotice text={data.error} retry={data.retry} />
  if (!data.value) return <Loading />

  return (
    <div className="overview">
      <section className="stat-grid" aria-label="Project summary">
        <div className="stat-tile">
          <span className="stat-icon">
            <ListTodo size={18} />
          </span>
          <strong>{issues.length}</strong>
          <span>{noun(issues.length, 'issue')}</span>
        </div>
        <div className="stat-tile" data-status="IN_PROGRESS">
          <span className="stat-icon">
            <ChartColumn size={18} />
          </span>
          <strong>{summary.open.length}</strong>
          <span>open</span>
        </div>
        <div className="stat-tile" data-status="DONE">
          <span className="stat-icon">
            <CircleCheck size={18} />
          </span>
          <strong>{percent}%</strong>
          <span>done</span>
        </div>
        <div className="stat-tile" data-alert={summary.bugs.length > 0 || undefined}>
          <span className="stat-icon">
            <Bug size={18} />
          </span>
          <strong>{summary.bugs.length}</strong>
          <span>open {noun(summary.bugs.length, 'bug')}</span>
        </div>
      </section>
      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>Progress</h2>
            <span className="muted">
              {summary.counts.DONE} of {issues.length} done
            </span>
          </div>
          <StatusBar counts={summary.counts} total={issues.length} />
          <ul className="status-legend">
            {(Object.keys(statuses) as IssueStatus[]).map(status => (
              <li key={status}>
                <span className="status-dot" data-status={status} />
                {statuses[status]}
                <strong>{summary.counts[status]}</strong>
              </li>
            ))}
          </ul>
          <div className="panel-heading sub-heading">
            <h3>Epics</h3>
          </div>
          {summary.epics.length === 0 ? (
            <p className="muted">No epics in this project yet.</p>
          ) : (
            <ul className="epic-progress-list">
              {summary.epics.map(({ epic, total, done }) => (
                <li key={epic.id}>
                  <button type="button" className="row-link" onClick={() => openIssue(epic)}>
                    <span className="issue-title">
                      {epic.title}
                      <small>{total ? `${done} of ${plural(total, 'issue')} done` : 'No linked issues'}</small>
                    </span>
                  </button>
                  <span className="progress mini-progress">
                    <span data-status="DONE" style={{ width: `${total ? (done / total) * 100 : 0}%` }} />
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>Who has what</h2>
            <span className="muted">Open issues per person</span>
          </div>
          {summary.people.length === 0 ? (
            <p className="muted">No open issues.</p>
          ) : (
            <ul className="workload-list">
              {summary.people.map(([name, items]) => (
                <li key={name || 'unassigned'}>
                  <span className="assignee-cell">
                    <Avatar name={name || null} size="tiny" />
                    {name || 'Unassigned'}
                  </span>
                  <span className="workload-bar">
                    <span style={{ width: `${(items.length / maxLoad) * 100}%` }} data-unassigned={!name || undefined} />
                  </span>
                  <strong>{items.length}</strong>
                </li>
              ))}
            </ul>
          )}
          {summary.bugs.length > 0 && (
            <>
              <div className="panel-heading sub-heading">
                <h3>Open bugs</h3>
              </div>
              <ul className="focus-list">
                {summary.bugs.slice(0, 4).map(bug => (
                  <li key={bug.id}>
                    <button type="button" className="focus-item" onClick={() => openIssue(bug)}>
                      <PriorityIcon priority={bug.priority} />
                      <span className="focus-text">
                        <strong>{bug.title}</strong>
                        <small>{bug.assigneeUsername ?? 'Unassigned'}</small>
                      </span>
                      <span className="issue-state">
                        <span className="status-dot" data-status={bug.status} />
                        {statuses[bug.status]}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>
      <ProjectActivity project={project} openIssue={openIssue} />
    </div>
  )
}

function ProjectActivity({ project, openIssue }: { project: Project; openIssue: (issue: IssueRef) => void }) {
  const [page, setPage] = useState(0)
  const activity = useResource(useCallback((signal: AbortSignal) => api.projectActivity(project, page, signal, 10), [project, page]))
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Recent activity</h2>
      </div>
      <ErrorNotice text={activity.error} retry={activity.retry} />
      {activity.loading && !activity.value && <Loading />}
      {activity.value?.content.length === 0 && <p className="muted">Nothing has happened in this project yet.</p>}
      <ul className="activity-feed">
        {activity.value?.content.map(item => (
          <li key={item.id}>
            <Avatar name={item.actorUsername} size="tiny" />
            <span className="activity-text">
              <strong>{item.actorUsername}</strong> {describeActivity(item)[0]}{' '}
              <button
                type="button"
                className="text-link"
                onClick={() => openIssue({ organizationId: project.organizationId, projectId: project.id, id: item.issueId })}>
                {item.issueTitle}
              </button>
              {describeActivity(item)[1] && <> {describeActivity(item)[1]}</>}
            </span>
            <time dateTime={item.createdAt} title={formatDate(item.createdAt)}>
              {formatRelative(item.createdAt)}
            </time>
          </li>
        ))}
      </ul>
      <Pagination data={activity.value} onChange={setPage} />
    </section>
  )
}

function ProjectSettings({
  project,
  organization,
  onSaved,
  onDeleted,
}: {
  project: Project
  organization: Organization
  onSaved: (project: Project) => void
  onDeleted: () => void
}) {
  const saveAction = useAction()
  const statusAction = useAction()
  const deleteAction = useAction()

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = readMetadata(new FormData(event.currentTarget))
    const changes: Partial<Metadata> = {}
    if (fields.title !== project.title) changes.title = fields.title
    if (fields.description !== (project.description ?? '')) changes.description = fields.description
    if (!Object.keys(changes).length) return
    void saveAction.run(async () => {
      onSaved(await api.updateProject(project, changes))
      toast('Project updated')
    })
  }

  function changeStatus(status: ProjectStatus) {
    void statusAction.run(async () => {
      onSaved(await api.changeProjectStatus(project, status))
      toast(`Project marked as ${projectStatuses[status].toLowerCase()}`)
    })
  }

  async function remove() {
    if (
      !(await confirmAction({
        title: 'Delete project?',
        confirmLabel: 'Delete project',
        danger: true,
        requireText: project.title,
        message: `${project.title} will be permanently deleted with all its issues, comments and members.`,
      }))
    )
      return
    void deleteAction.run(async () => {
      await api.deleteProject(project)
      toast(`${project.title} deleted`)
      onDeleted()
    })
  }

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel-heading">
          <h2>Details</h2>
        </div>
        <form key={project.title + (project.description ?? '')} className="form settings-form" onSubmit={save}>
          <MetadataFields initial={project} />
          <ErrorNotice text={saveAction.error} />
          <button className="button primary" disabled={saveAction.busy}>
            {saveAction.busy ? 'Saving…' : 'Save changes'}
          </button>
        </form>
      </section>
      <section className="panel">
        <div className="panel-heading">
          <h2>Status</h2>
          <span className="muted">Archived projects are read-only</span>
        </div>
        <div className="settings-form form">
          <label>
            Project status
            <Select
              value={project.status}
              disabled={statusAction.busy}
              onChange={next => changeStatus(next as ProjectStatus)}
              options={toOptions(projectStatuses)}
            />
          </label>
          <ErrorNotice text={statusAction.error} />
        </div>
      </section>
      {organization.role === 'OWNER' && (
        <section className="panel danger-panel">
          <div className="panel-heading">
            <h2>Danger zone</h2>
          </div>
          <ErrorNotice text={deleteAction.error} />
          <div className="danger-row">
            <span>
              <strong>Delete project</strong>
              <small>Deletes the project with all its issues, comments and members.</small>
            </span>
            <button className="button danger" disabled={deleteAction.busy} onClick={() => void remove()}>
              <Trash2 size={15} />
              Delete project
            </button>
          </div>
        </section>
      )}
    </div>
  )
}
