import { ArrowRight, Building2, CalendarDays, CircleCheck, Clock3, Folder, FolderOpen, Sparkles, UserCheck, Users } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import {
  api,
  type Issue,
  type IssueRef,
  type IssueStatus,
  type Organization,
  type OrganizationOverview,
  type Project,
  type ProjectActivity,
  type TimesheetEntry,
  type User,
} from './api'
import { addDays, datesBetween, formatDay, formatHours, startOfWeek, today } from './time'
import { Avatar, PriorityIcon, StatusBar } from './bits'
import { describeActivity, formatDate, formatRelative, noun, organizationRoles, plural, priorityRank, statuses, types } from './labels'
import { useAllRecentProjects } from './recent'
import { useResource } from './hooks'
import { ErrorNotice, Loading } from './ui'

type OrganizationData = { organization: Organization; overview: OrganizationOverview; projects: Project[] }
type HomeData = {
  issues: Issue[]
  organizations: OrganizationData[]
  activity: (ProjectActivity & { project: Project })[]
  week: TimesheetEntry[]
}

const dayFormatter = new Intl.DateTimeFormat('en', { weekday: 'long', month: 'long', day: 'numeric' })

export function Dashboard({
  user,
  organizations,
  openIssue,
  openOrganization,
  openMyIssues,
  openProject,
  openProjectIssue,
  openTimesheet,
}: {
  openTimesheet: () => void
  user?: User
  organizations: Organization[]
  openIssue: (issue: IssueRef) => void
  openOrganization: (id: string) => void
  openMyIssues: () => void
  openProject: (organizationId: string, projectId: string) => void
  openProjectIssue: (organizationId: string, projectId: string, issueId: string) => void
}) {
  const recent = useAllRecentProjects()
  const key = organizations.map(item => item.id).join(',')
  const recentKey = recent.map(item => item.id).join(',')
  const data = useResource(
    useCallback(
      async (signal: AbortSignal): Promise<HomeData> => {
        const ids = key.split(',').filter(Boolean)
        const weekStart = startOfWeek(today())
        const [issues, perOrganization, week] = await Promise.all([
          api.myIssues(signal),
          Promise.all(
            ids.map(async id => {
              const [overview, projects] = await Promise.all([api.organizationOverview(id, signal), api.allProjects(id, signal)])
              return { id, overview, projects }
            }),
          ),
          api.timesheet(weekStart, addDays(weekStart, 6), signal).catch(() => [] as TimesheetEntry[]),
        ])
        const orgData = perOrganization.flatMap(({ id, overview, projects }) => {
          const organization = organizations.find(item => item.id === id)
          return organization ? [{ organization, overview, projects }] : []
        })
        const accessible = new Map(
          orgData.flatMap(({ overview, projects }) =>
            projects
              .filter(project => overview.projects.some(stats => stats.projectId === project.id))
              .map(project => [project.id, project] as const),
          ),
        )
        const recentIds = recentKey.split(',').filter(id => accessible.has(id))
        const feedProjects = [...recentIds, ...[...accessible.keys()].filter(id => !recentIds.includes(id))]
          .slice(0, 3)
          .map(id => accessible.get(id)!)
        const feeds = await Promise.all(
          feedProjects.map(async project => (await api.projectActivity(project, 0, signal, 8)).content.map(item => ({ ...item, project }))),
        )
        const activity = feeds
          .flat()
          .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
          .slice(0, 8)
        return { issues, organizations: orgData, activity, week }
      },
      [key, recentKey, organizations],
    ),
  )

  const issues = useMemo(() => data.value?.issues ?? [], [data.value])
  const counts = useMemo(() => {
    const result: Record<IssueStatus, number> = { TODO: 0, IN_PROGRESS: 0, IN_REVIEW: 0, DONE: 0 }
    issues.forEach(issue => {
      result[issue.status]++
    })
    return result
  }, [issues])
  const open = issues.filter(issue => issue.status !== 'DONE')
  const focus = [...open]
    .sort((a, b) => priorityRank[a.priority] - priorityRank[b.priority] || Date.parse(b.updatedAt) - Date.parse(a.updatedAt))
    .slice(0, 5)
  const allProjects = new Map(
    (data.value?.organizations ?? []).flatMap(org => org.projects.map(project => [project.id, { project, org }] as const)),
  )
  const projectTitle = (issue: Issue) => allProjects.get(issue.projectId)?.project.title ?? 'Project'
  const jumpBack = recent.filter(item => allProjects.has(item.id)).slice(0, 4)
  const totalProjects = (data.value?.organizations ?? []).reduce((sum, org) => sum + org.overview.projects.length, 0)
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'
  const summary = !data.value
    ? 'Loading your workspace…'
    : open.length > 0
      ? `You have ${plural(open.length, 'open issue')}${counts.IN_REVIEW ? `, ${counts.IN_REVIEW} waiting for review` : ''}.`
      : totalProjects > 0
        ? `Nothing is assigned to you right now. Here is what is happening in your ${noun(totalProjects, 'project')}.`
        : organizations.length
          ? 'Create a project to start planning work.'
          : 'Create or join an organization to get started.'

  return (
    <div className="home">
      <section className="home-hero">
        <div>
          <p className="home-date">
            <CalendarDays size={15} />
            {dayFormatter.format(new Date())}
          </p>
          <h1>
            {greeting}
            {user ? `, ${user.username}` : ''}
          </h1>
          <p className="home-summary">{summary}</p>
        </div>
        <div className="row">
          {organizations[0] && (
            <button className="button" onClick={() => openOrganization(organizations[0].id)}>
              <Folder size={16} />
              Your projects
            </button>
          )}
          <button className="button primary" onClick={openMyIssues}>
            All my issues
            <ArrowRight size={16} />
          </button>
        </div>
      </section>
      <ErrorNotice text={data.error} retry={data.retry} />
      {data.loading && !data.value && <Loading />}
      {data.value && (
        <>
          <div className="home-grid">
            <section className="panel">
              <div className="panel-heading">
                <h2>Your work</h2>
                <span className="muted">{plural(issues.length, 'issue')} assigned to you</span>
              </div>
              <div className="work-summary">
                {(Object.keys(statuses) as IssueStatus[]).map(status => (
                  <div key={status} className="work-count" data-status={status}>
                    <strong>{counts[status]}</strong>
                    <span>
                      <span className="status-dot" data-status={status} />
                      {statuses[status]}
                    </span>
                  </div>
                ))}
              </div>
              <StatusBar counts={counts} total={issues.length} />
              <WeekTime entries={data.value.week} openTimesheet={openTimesheet} />
              <div className="panel-heading sub-heading">
                <h3>In focus</h3>
                <span className="muted">Open issues by priority</span>
              </div>
              {focus.length === 0 ? (
                <div className="calm-state">
                  <span className="calm-icon">
                    <Sparkles size={18} />
                  </span>
                  <span>
                    <strong>{issues.length ? 'Everything assigned to you is done' : 'Your plate is clear'}</strong>
                    <small>
                      {issues.length
                        ? 'Nice work. New issues assigned to you will show up here.'
                        : 'When someone assigns you an issue, it will show up here.'}
                    </small>
                  </span>
                </div>
              ) : (
                <ul className="focus-list">
                  {focus.map(issue => (
                    <li key={issue.id}>
                      <button type="button" className="focus-item" onClick={() => openIssue(issue)}>
                        <PriorityIcon priority={issue.priority} />
                        <span className="focus-text">
                          <strong>{issue.title}</strong>
                          <small>
                            {types[issue.type]} · {projectTitle(issue)}
                          </small>
                        </span>
                        <span className="issue-state">
                          <span className="status-dot" data-status={issue.status} />
                          {statuses[issue.status]}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
              {open.length > focus.length && (
                <button className="text-button" onClick={openMyIssues}>
                  {open.length - focus.length} more open {noun(open.length - focus.length, 'issue')}
                </button>
              )}
            </section>
            <section className="panel">
              <div className="panel-heading">
                <h2>Jump back in</h2>
                <Clock3 size={17} className="muted" />
              </div>
              {jumpBack.length === 0 ? (
                <p className="muted">Projects you open will appear here for quick access.</p>
              ) : (
                <ul className="jump-list">
                  {jumpBack.map(item => {
                    const entry = allProjects.get(item.id)!
                    const stats = entry.org.overview.projects.find(project => project.projectId === item.id)
                    return (
                      <li key={item.id}>
                        <button type="button" className="jump-item" onClick={() => openProject(item.organizationId, item.id)}>
                          <span className="record-icon small-icon">
                            <FolderOpen size={16} />
                          </span>
                          <span className="focus-text">
                            <strong>{entry.project.title}</strong>
                            <small>{entry.org.organization.title}</small>
                          </span>
                          {stats && (
                            <span className="jump-progress">
                              <StatusBar counts={stats.issuesByStatus} total={stats.totalIssues} />
                              <small>{stats.openIssues} open</small>
                            </span>
                          )}
                        </button>
                      </li>
                    )
                  })}
                </ul>
              )}
            </section>
          </div>
          {data.value.organizations.length > 0 && (
            <section className="home-section">
              <div className="section-heading">
                <h2>Your organizations</h2>
              </div>
              <div className="org-overview-grid">
                {data.value.organizations.map(({ organization, overview, projects }) => (
                  <article key={organization.id} className="panel org-overview">
                    <header className="org-overview-head">
                      <span className="record-icon">
                        <Building2 size={18} />
                      </span>
                      <span className="focus-text">
                        <strong>{organization.title}</strong>
                        <small>{organizationRoles[organization.role]}</small>
                      </span>
                      <button className="button" onClick={() => openOrganization(organization.id)}>
                        Open
                        <ArrowRight size={15} />
                      </button>
                    </header>
                    <div className="org-overview-stats">
                      <span>
                        <Folder size={14} />
                        {plural(overview.projectsCount, 'project')}
                      </span>
                      <span>
                        <Users size={14} />
                        {plural(overview.membersCount, 'member')}
                      </span>
                      <span>
                        <CircleCheck size={14} />
                        {overview.openIssues} open
                      </span>
                      <span data-highlight={overview.assignedToMe > 0 || undefined}>
                        <UserCheck size={14} />
                        {overview.assignedToMe} on you
                      </span>
                    </div>
                    {overview.projects.length === 0 ? (
                      <p className="muted small-text">
                        {overview.projectsCount ? 'You are not a member of any project here yet.' : 'No projects yet.'}
                      </p>
                    ) : (
                      <ul className="org-project-list">
                        {overview.projects.slice(0, 4).map(stats => {
                          const project = projects.find(item => item.id === stats.projectId)
                          return (
                            project && (
                              <li key={stats.projectId}>
                                <button type="button" className="org-project" onClick={() => openProject(organization.id, project.id)}>
                                  <span className="focus-text">
                                    <strong>{project.title}</strong>
                                    <small>
                                      {stats.issuesByStatus.DONE} of {stats.totalIssues} done
                                    </small>
                                  </span>
                                  <StatusBar counts={stats.issuesByStatus} total={stats.totalIssues} />
                                </button>
                              </li>
                            )
                          )
                        })}
                      </ul>
                    )}
                  </article>
                ))}
              </div>
            </section>
          )}
          {data.value.activity.length > 0 && (
            <section className="panel">
              <div className="panel-heading">
                <h2>Latest activity</h2>
                <span className="muted">From your projects</span>
              </div>
              <ul className="activity-feed">
                {data.value.activity.map(item => (
                  <li key={item.id}>
                    <Avatar name={item.actorUsername} size="tiny" />
                    <span className="activity-text">
                      <strong>{item.actorUsername}</strong> {describeActivity(item)[0]}{' '}
                      <button
                        type="button"
                        className="text-link"
                        onClick={() => openProjectIssue(item.project.organizationId, item.project.id, item.issueId)}>
                        {item.issueTitle}
                      </button>
                      {describeActivity(item)[1] && <> {describeActivity(item)[1]}</>}
                      <small className="activity-project"> · {item.project.title}</small>
                    </span>
                    <time dateTime={item.createdAt} title={formatDate(item.createdAt)}>
                      {formatRelative(item.createdAt)}
                    </time>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  )
}

function WeekTime({ entries, openTimesheet }: { entries: TimesheetEntry[]; openTimesheet: () => void }) {
  const start = startOfWeek(today())
  const days = datesBetween(start, addDays(start, 6))
  const perDay = days.map(day => entries.filter(entry => entry.workDate === day).reduce((sum, entry) => sum + entry.minutes, 0))
  const total = perDay.reduce((sum, value) => sum + value, 0)
  const peak = Math.max(8 * 60, ...perDay)
  return (
    <div className="week-time">
      <div>
        <span className="muted small-text">Logged this week</span>
        <strong>{formatHours(total)}</strong>
      </div>
      <div className="week-bars" aria-hidden="true">
        {days.map((day, index) => (
          <span key={day} title={`${formatDay(day)}: ${formatHours(perDay[index])}`} data-today={day === today() || undefined}>
            <span style={{ height: `${Math.round((perDay[index] / peak) * 100)}%` }} />
            <small>{formatDay(day, { weekday: 'narrow' })}</small>
          </span>
        ))}
      </div>
      <button type="button" className="text-button" onClick={openTimesheet}>
        Timesheet
        <ArrowRight size={14} />
      </button>
    </div>
  )
}
