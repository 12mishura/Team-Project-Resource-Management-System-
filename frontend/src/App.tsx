import {
  ArrowRight,
  Bug,
  Building2,
  CalendarClock,
  CircleCheck,
  ClipboardList,
  Folder,
  FolderPlus,
  Gauge,
  House,
  LogOut,
  Menu as MenuIcon,
  Moon,
  Plus,
  Settings,
  Sun,
  UserCheck,
  Users,
  X,
} from 'lucide-react'
import { Fragment, lazy, Suspense, useCallback, useEffect, useState, useSyncExternalStore, type FormEvent, type ReactNode } from 'react'
import { api, getProfile, hasSession, signOut, subscribeSession, type Organization, type Project, type ProjectStats } from './api'
import { FeedbackHost } from './feedback'
import { toast } from './feedbackStore'
import { Avatar, StatusBar } from './bits'
import { MetadataFields } from './forms'
import { noun, organizationRoles, plural, projectStatuses, readMetadata, toOptions } from './labels'
import { OrganizationPicker } from './OrganizationPicker'
import {
  bindRecentToUser,
  forgetOrganization,
  forgetProject,
  readLastOrganization,
  saveLastOrganization,
  useRecentProjects,
} from './recent'
import { navigate, routePath, useRoute, type Route } from './router'
import { Select } from './Select'
import { toggleTheme, useTheme } from './theme'
import { useAction, useResource } from './hooks'
import { Brand, Empty, ErrorNotice, Loading, Modal, Pagination } from './ui'
import { ErrorBoundary } from './ErrorBoundary'

const Auth = lazy(() => import('./Auth').then(module => ({ default: module.Auth })))
const Dashboard = lazy(() => import('./Dashboard').then(module => ({ default: module.Dashboard })))
const Issues = lazy(() => import('./Issues').then(module => ({ default: module.Issues })))
const ProjectPage = lazy(() => import('./ProjectPage').then(module => ({ default: module.ProjectPage })))
const UserProfile = lazy(() => import('./Profiles').then(module => ({ default: module.UserProfile })))
const OrganizationMembers = lazy(() => import('./Profiles').then(module => ({ default: module.OrganizationMembers })))
const OrganizationSettings = lazy(() => import('./Profiles').then(module => ({ default: module.OrganizationSettings })))
const Timesheet = lazy(() => import('./Timesheet').then(module => ({ default: module.Timesheet })))
const OrganizationWorkload = lazy(() => import('./Workload').then(module => ({ default: module.OrganizationWorkload })))

const canManage = (organization: Organization) => organization.role !== 'USER'

export function App() {
  const active = useSyncExternalStore(subscribeSession, hasSession)
  const [logoutWarning, setLogoutWarning] = useState('')
  const [passwordChanged, setPasswordChanged] = useState(false)
  useEffect(() => {
    if (active) setPasswordChanged(false)
  }, [active])
  if (!active)
    return (
      <>
        <Suspense fallback={<Loading />}>
          <Auth logoutWarning={logoutWarning} passwordChanged={passwordChanged} />
        </Suspense>
        <FeedbackHost />
      </>
    )
  return (
    <>
      <FeedbackHost />
      <Workspace
        onPasswordChanged={() => {
          setLogoutWarning('')
          setPasswordChanged(true)
        }}
        logout={() => {
          setPasswordChanged(false)
          setLogoutWarning('')
          void signOut().catch(() =>
            setLogoutWarning('You signed out locally, but the server could not confirm that the session was revoked.'),
          )
        }}
      />
    </>
  )
}

type Crumb = { label: string; route?: Route }

function Workspace({ logout, onPasswordChanged }: { logout: () => void; onPasswordChanged: () => void }) {
  const profile = useResource(getProfile)
  const userId = profile.value?.id
  useEffect(() => {
    if (userId) bindRecentToUser(userId)
  }, [userId])
  const organizations = useResource(api.organizations)
  const route = useRoute()
  const routeOrgId = route.name === 'org' || route.name === 'project' ? route.orgId : ''
  const [lastOrgId, setLastOrgId] = useState(readLastOrganization)
  const [form, setForm] = useState<'organization' | 'join' | 'project' | null>(null)
  const [navOpen, setNavOpen] = useState(false)
  const list = organizations.value ?? []
  const organization = list.find(item => item.id === (routeOrgId || lastOrgId)) ?? (routeOrgId ? undefined : list[0])
  const orgId = organization?.id ?? ''
  const missingOrganization = Boolean(routeOrgId && organizations.value && !organization)
  const recent = useRecentProjects(orgId)
  const path = routePath(route)
  const { theme } = useTheme()

  useEffect(() => {
    if (path !== window.location.pathname + window.location.search) navigate(route, { replace: true })
  })
  useEffect(() => {
    if (routeOrgId && organization) {
      setLastOrgId(routeOrgId)
      saveLastOrganization(routeOrgId)
    }
  }, [routeOrgId, organization])
  useEffect(() => {
    setNavOpen(false)
  }, [path])

  const openOrganization = (id: string) => navigate({ name: 'org', orgId: id, section: 'projects' })
  const openProject = (project: Project) =>
    navigate({ name: 'project', orgId: project.organizationId, projectId: project.id, tab: 'board' })

  const navItem = (label: string, icon: ReactNode, target: Route, active: boolean) => (
    <button key={label + routePath(target)} aria-current={active ? 'page' : undefined} onClick={() => navigate(target)}>
      {icon}
      <span>{label}</span>
    </button>
  )

  const projectTitle = route.name === 'project' ? recent.find(item => item.id === route.projectId)?.title : undefined
  const orgCrumb: Crumb | undefined = organization
    ? { label: organization.title, route: { name: 'org', orgId, section: 'projects' } }
    : undefined
  const crumbs: Crumb[] =
    route.name === 'home'
      ? [{ label: 'Home' }]
      : route.name === 'mine'
        ? [{ label: 'My issues' }]
        : route.name === 'profile'
          ? [{ label: 'Your profile' }]
          : route.name === 'timesheet'
            ? [{ label: 'Timesheet' }]
            : route.name === 'organizations'
              ? [{ label: 'Organizations' }]
              : route.name === 'org'
                ? [...(orgCrumb ? [orgCrumb] : []), { label: route.section[0].toUpperCase() + route.section.slice(1) }]
                : [
                    ...(orgCrumb ? [orgCrumb, { label: 'Projects', route: orgCrumb.route }] : []),
                    {
                      label: projectTitle ?? 'Project',
                      route: { name: 'project', orgId: route.orgId, projectId: route.projectId, tab: 'board' },
                    },
                    { label: route.tab[0].toUpperCase() + route.tab.slice(1) },
                  ]

  return (
    <div className="workspace">
      <aside className="sidebar" data-open={navOpen || undefined}>
        <div className="sidebar-top">
          <Brand />
          <button
            type="button"
            className="icon-button sidebar-toggle"
            aria-expanded={navOpen}
            aria-label={navOpen ? 'Close menu' : 'Open menu'}
            onClick={() => setNavOpen(open => !open)}>
            {navOpen ? <X size={18} /> : <MenuIcon size={18} />}
          </button>
        </div>
        <div className="sidebar-body">
          <OrganizationPicker
            organizations={list}
            loading={!organizations.value}
            selectedId={orgId}
            onSelect={openOrganization}
            onShowAll={() => navigate({ name: 'organizations' })}
            onCreate={() => setForm('organization')}
            onJoin={() => setForm('join')}
          />
          <nav className="navigation" aria-label="Main navigation">
            <span className="nav-section">For you</span>
            {navItem('Home', <House size={17} />, { name: 'home' }, route.name === 'home')}
            {navItem('My issues', <ClipboardList size={17} />, { name: 'mine' }, route.name === 'mine')}
            {navItem('Timesheet', <CalendarClock size={17} />, { name: 'timesheet' }, route.name === 'timesheet')}
            {organization && (
              <>
                <span className="nav-section">{organization.title}</span>
                {navItem(
                  'Projects',
                  <Folder size={17} />,
                  { name: 'org', orgId, section: 'projects' },
                  route.name === 'org' && route.section === 'projects',
                )}
                {navItem(
                  'Members',
                  <Users size={17} />,
                  { name: 'org', orgId, section: 'members' },
                  route.name === 'org' && route.section === 'members',
                )}
                {canManage(organization) &&
                  navItem(
                    'Workload',
                    <Gauge size={17} />,
                    { name: 'org', orgId, section: 'workload' },
                    route.name === 'org' && route.section === 'workload',
                  )}
                {navItem(
                  'Settings',
                  <Settings size={17} />,
                  { name: 'org', orgId, section: 'settings' },
                  route.name === 'org' && route.section === 'settings',
                )}
              </>
            )}
            {organization && recent.length > 0 && (
              <>
                <span className="nav-section">Recent projects</span>
                {recent.map(item =>
                  navItem(
                    item.title,
                    <Folder size={17} />,
                    { name: 'project', orgId: item.organizationId, projectId: item.id, tab: 'board' },
                    route.name === 'project' && route.projectId === item.id,
                  ),
                )}
              </>
            )}
            {organizations.value && list.length === 0 && (
              <div className="nav-empty">
                <p>Create an organization or join one to start.</p>
                <button className="button primary" onClick={() => setForm('organization')}>
                  <Plus size={16} />
                  Create organization
                </button>
              </div>
            )}
          </nav>
          <div className="account">
            <button
              type="button"
              className="account-profile"
              aria-label="Your profile"
              aria-current={route.name === 'profile' ? 'page' : undefined}
              onClick={() => navigate({ name: 'profile' })}>
              <Avatar name={profile.value?.username ?? null} />
              <span className="account-name">
                {profile.value?.username ?? 'Account'}
                <small>Your profile</small>
              </span>
            </button>
            <button type="button" className="icon-button" title="Sign out" aria-label="Sign out" onClick={logout}>
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>
      <main className="main">
        <div className="topbar">
          <nav className="breadcrumb" aria-label="Breadcrumb">
            <button type="button" className="crumb-link" onClick={() => navigate({ name: 'home' })}>
              TaskTracker
            </button>
            {crumbs.map((crumb, index) => (
              <Fragment key={index}>
                <span className="crumb-separator" aria-hidden="true">
                  /
                </span>
                {crumb.route && index < crumbs.length - 1 ? (
                  <button type="button" className="crumb-link" onClick={() => navigate(crumb.route!)}>
                    {crumb.label}
                  </button>
                ) : (
                  <span className="crumb-current" aria-current="page">
                    {crumb.label}
                  </span>
                )}
              </Fragment>
            ))}
          </nav>
          <button
            type="button"
            className="icon-button theme-toggle"
            title={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            onClick={toggleTheme}>
            {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
          </button>
        </div>
        <ErrorNotice text={profile.error} retry={profile.retry} />
        <ErrorNotice text={organizations.error} retry={organizations.retry} />
        <ErrorBoundary
          key={route.name === 'project' ? routePath({ ...route, issueId: undefined }) : path}
          scope="page"
          onHome={() => navigate({ name: 'home' })}>
          <Suspense fallback={<Loading />}>
            {route.name === 'home' && (
              <Dashboard
                user={profile.value}
                organizations={list}
                openIssue={issue => navigate({ name: 'mine', issue })}
                openOrganization={openOrganization}
                openMyIssues={() => navigate({ name: 'mine' })}
                openTimesheet={() => navigate({ name: 'timesheet' })}
                openProject={(organizationId, projectId) => navigate({ name: 'project', orgId: organizationId, projectId, tab: 'board' })}
                openProjectIssue={(organizationId, projectId, issueId) =>
                  navigate({ name: 'project', orgId: organizationId, projectId, tab: 'issues', issueId })
                }
              />
            )}
            {route.name === 'mine' && (
              <Issues
                key="mine"
                project={null}
                organizations={list}
                user={profile.value}
                selected={route.issue}
                onSelect={issue => navigate({ name: 'mine', issue })}
              />
            )}
            {route.name === 'profile' &&
              (profile.value ? (
                <UserProfile
                  user={profile.value}
                  organizations={list}
                  onSaved={profile.setValue}
                  onPasswordChanged={onPasswordChanged}
                  openOrganization={openOrganization}
                />
              ) : (
                profile.loading && <Loading />
              ))}
            {route.name === 'timesheet' && (
              <Timesheet
                openIssue={issue =>
                  navigate({
                    name: 'project',
                    orgId: issue.organizationId,
                    projectId: issue.projectId,
                    tab: 'issues',
                    issueId: issue.issueId,
                  })
                }
              />
            )}
            {route.name === 'organizations' && (
              <>
                <header className="page-heading">
                  <div>
                    <h1>Organizations</h1>
                    <p>Teams you belong to</p>
                  </div>
                  <div className="row">
                    <button className="button" onClick={() => setForm('join')}>
                      Join with a code
                    </button>
                    <button className="button primary" onClick={() => setForm('organization')}>
                      <Plus size={17} />
                      Create organization
                    </button>
                  </div>
                </header>
                {organizations.loading && !organizations.value && <Loading />}
                {organizations.value?.length === 0 && (
                  <Empty
                    title="Start your first organization"
                    icon={<Building2 size={22} />}
                    action={
                      <div className="row">
                        <button className="button primary" onClick={() => setForm('organization')}>
                          <Plus size={16} />
                          Create organization
                        </button>
                        <button className="button" onClick={() => setForm('join')}>
                          Join with a code
                        </button>
                      </div>
                    }>
                    An organization is a shared space for your team: projects, members and issues live inside it.
                  </Empty>
                )}
                <div className="card-grid">
                  {list.map(item => (
                    <OrganizationCard key={item.id} organization={item} onOpen={() => openOrganization(item.id)} />
                  ))}
                </div>
              </>
            )}
            {routeOrgId && !organization && organizations.loading && <Loading />}
            {missingOrganization && (
              <Empty
                title="Organization not found"
                icon={<Building2 size={22} />}
                action={
                  <button className="button" onClick={() => navigate({ name: 'organizations' })}>
                    Go to organizations
                  </button>
                }>
                It may have been deleted, or you are no longer a member.
              </Empty>
            )}
            {route.name === 'org' && organization && route.section === 'projects' && (
              <Projects key={orgId} organization={organization} onOpen={openProject} onCreate={() => setForm('project')} />
            )}
            {route.name === 'org' && organization && route.section === 'members' && (
              <OrganizationMembers key={orgId} organization={organization} onMembershipChanged={organizations.reload} />
            )}
            {route.name === 'org' && organization && route.section === 'workload' && (
              <OrganizationWorkload key={orgId} organization={organization} />
            )}
            {route.name === 'org' && organization && route.section === 'settings' && (
              <OrganizationSettings
                key={orgId}
                organization={organization}
                onSaved={updated => organizations.setValue(current => current?.map(item => (item.id === updated.id ? updated : item)))}
                onLeft={() => {
                  forgetOrganization(orgId)
                  saveLastOrganization('')
                  setLastOrgId('')
                  navigate({ name: 'organizations' })
                  organizations.reload()
                }}
              />
            )}
            {route.name === 'project' && organization && (
              <ProjectPage
                key={route.projectId}
                organization={organization}
                projectId={route.projectId}
                tab={route.tab}
                issueId={route.issueId}
                user={profile.value}
                onTab={(tab, issueId) => navigate({ name: 'project', orgId, projectId: route.projectId, tab, issueId })}
                onSelectIssue={issueId => navigate({ name: 'project', orgId, projectId: route.projectId, tab: route.tab, issueId })}
                onDeleted={() => {
                  forgetProject(route.projectId)
                  navigate({ name: 'org', orgId, section: 'projects' })
                }}
              />
            )}
          </Suspense>
        </ErrorBoundary>
        {form && (
          <MetadataForm
            kind={form}
            organization={organization}
            close={() => setForm(null)}
            onSaved={result => {
              if (form === 'project' && result) {
                toast(`${result.title} created`)
                openProject(result)
              } else {
                toast(form === 'join' ? 'You joined the organization' : 'Organization created')
                organizations.reload()
              }
              setForm(null)
            }}
          />
        )}
      </main>
    </div>
  )
}

function MemberStack({ names, total }: { names: string[]; total: number }) {
  if (total === 0) return <span className="muted small-text">No members yet</span>
  return (
    <span className="member-stack" aria-label={plural(total, 'member')}>
      {names.map(name => (
        <Avatar key={name} name={name} size="tiny" />
      ))}
      {total > names.length && <span className="avatar tiny more-avatar">+{total - names.length}</span>}
    </span>
  )
}

function OrganizationCard({ organization, onOpen }: { organization: Organization; onOpen: () => void }) {
  const overview = useResource(useCallback((signal: AbortSignal) => api.organizationOverview(organization.id, signal), [organization.id]))
  const data = overview.value
  return (
    <button className="record-card rich-card" onClick={onOpen}>
      <span className="card-top">
        <span className="record-icon">
          <Building2 size={20} />
        </span>
        <span className="badge">{organizationRoles[organization.role]}</span>
      </span>
      <strong>{organization.title}</strong>
      <span className="card-description">{organization.description || 'No description'}</span>
      <span className="card-stats">
        <span>
          <Folder size={14} />
          {data ? plural(data.projectsCount, 'project') : '– projects'}
        </span>
        <span>
          <Users size={14} />
          {data ? plural(data.membersCount, 'member') : '– members'}
        </span>
        <span>
          <UserCheck size={14} />
          {data ? data.assignedToMe : '–'} on me
        </span>
      </span>
      <span className="card-link">
        Open <ArrowRight size={16} />
      </span>
    </button>
  )
}

function ProjectCard({ project, stats, onOpen }: { project: Project; stats?: ProjectStats; onOpen: () => void }) {
  const done = stats?.issuesByStatus.DONE ?? 0
  const percent = stats && stats.totalIssues > 0 ? Math.round((done / stats.totalIssues) * 100) : 0
  return (
    <button className="record-card rich-card" onClick={onOpen} data-restricted={!stats || undefined}>
      <span className="card-top">
        <span className="record-icon">
          <Folder size={20} />
        </span>
        <span className="badge" data-project-status={project.status}>
          {projectStatuses[project.status]}
        </span>
      </span>
      <strong>{project.title}</strong>
      <span className="card-description">{project.description || 'No description'}</span>
      {stats ? (
        <>
          <span className="card-progress">
            <span className="progress-label">
              <span>
                {done} of {stats.totalIssues} done
              </span>
              <span>{percent}%</span>
            </span>
            <StatusBar counts={stats.issuesByStatus} total={stats.totalIssues} />
          </span>
          <span className="card-stats">
            <span>
              <CircleCheck size={14} />
              {stats.openIssues} open
            </span>
            <span data-alert={stats.openBugs > 0 || undefined}>
              <Bug size={14} />
              {plural(stats.openBugs, 'bug')}
            </span>
            <span data-highlight={stats.assignedToMe > 0 || undefined}>
              <UserCheck size={14} />
              {stats.assignedToMe} on me
            </span>
          </span>
          <span className="card-footer">
            <MemberStack names={stats.members} total={stats.membersCount} />
            <span className="card-link">
              Open <ArrowRight size={16} />
            </span>
          </span>
        </>
      ) : (
        <span className="card-footer">
          <span className="muted small-text">You are not a member of this project</span>
          <span className="card-link">
            View <ArrowRight size={16} />
          </span>
        </span>
      )}
    </button>
  )
}

function Projects({
  organization,
  onOpen,
  onCreate,
}: {
  organization: Organization
  onOpen: (project: Project) => void
  onCreate: () => void
}) {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const load = useCallback((signal: AbortSignal) => api.projects(organization.id, page, status, signal), [organization.id, page, status])
  const data = useResource(load)
  const overview = useResource(useCallback((signal: AbortSignal) => api.organizationOverview(organization.id, signal), [organization.id]))
  const stats = new Map((overview.value?.projects ?? []).map(item => [item.projectId, item]))
  return (
    <>
      <header className="page-heading">
        <div>
          <h1>Projects</h1>
          <p>{organization.title}</p>
        </div>
        {canManage(organization) && (
          <button className="button primary" onClick={onCreate}>
            <Plus size={17} />
            Create project
          </button>
        )}
      </header>
      {overview.value && (
        <section className="summary-strip" aria-label="Organization summary">
          <span>
            <strong>{overview.value.projectsCount}</strong> {noun(overview.value.projectsCount, 'project')}
          </span>
          <span>
            <strong>{overview.value.membersCount}</strong> {noun(overview.value.membersCount, 'member')}
          </span>
          <span>
            <strong>{overview.value.openIssues}</strong> open {noun(overview.value.openIssues, 'issue')}
          </span>
          <span>
            <strong>{overview.value.assignedToMe}</strong> assigned to you
          </span>
        </section>
      )}
      <div className="toolbar">
        <Select
          aria-label="Project status"
          value={status}
          onChange={next => {
            setStatus(next)
            setPage(0)
          }}
          options={[{ value: '', label: 'All statuses' }, ...toOptions(projectStatuses)]}
        />
      </div>
      <ErrorNotice text={data.error} retry={data.retry} />
      {data.loading && !data.value ? (
        <Loading />
      ) : (
        <>
          {data.value?.content.length === 0 &&
            (status ? (
              <Empty title="No matching projects">Try another status filter.</Empty>
            ) : (
              <Empty
                title="Start your first project"
                icon={<FolderPlus size={22} />}
                action={
                  canManage(organization) && (
                    <button className="button primary" onClick={onCreate}>
                      <Plus size={16} />
                      Create project
                    </button>
                  )
                }>
                {canManage(organization)
                  ? 'Projects hold the board, issues and members of one piece of work.'
                  : 'An owner or admin of this organization can create the first project.'}
              </Empty>
            ))}
          <div className="card-grid">
            {data.value?.content.map(item => (
              <ProjectCard key={item.id} project={item} stats={stats.get(item.id)} onOpen={() => onOpen(item)} />
            ))}
          </div>
          <Pagination data={data.value} onChange={setPage} />
        </>
      )}
    </>
  )
}

function MetadataForm({
  kind,
  organization,
  close,
  onSaved,
}: {
  kind: 'organization' | 'join' | 'project'
  organization?: Organization
  close: () => void
  onSaved: (project?: Project) => void
}) {
  const { run, busy, error } = useAction()
  const title = { organization: 'Create organization', join: 'Join with a code', project: 'Create project' }[kind]
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const values = new FormData(event.currentTarget)
    void run(async () => {
      if (kind === 'join') {
        await api.joinOrganization(String(values.get('code')).trim())
        onSaved()
        return
      }
      const metadata = readMetadata(values)
      if (kind === 'project' && organization) onSaved(await api.createProject(organization.id, metadata))
      else if (kind === 'organization') {
        await api.createOrganization(metadata)
        onSaved()
      }
    })
  }
  return (
    <Modal
      title={title}
      close={() => {
        if (!busy) close()
      }}>
      <form className="form" onSubmit={submit}>
        {kind === 'join' ? (
          <label>
            Invite code
            <input required name="code" maxLength={9} placeholder="abcd-1234" autoFocus />
          </label>
        ) : (
          <MetadataFields />
        )}
        <ErrorNotice text={error} />
        <div className="row">
          <button className="button primary" disabled={busy}>
            {busy ? 'Please wait…' : kind === 'join' ? 'Join' : 'Create'}
          </button>
          <button type="button" className="button" disabled={busy} onClick={close}>
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  )
}
