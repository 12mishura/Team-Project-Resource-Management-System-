import { ArrowRight, Building2, ClipboardList, Folder, FolderOpen, LogOut, Pencil, Plus, Trash2, Users } from 'lucide-react'
import { useCallback, useEffect, useState, useSyncExternalStore, type FormEvent } from 'react'
import { api, getProfile, hasSession, signOut, subscribeSession, type Organization, type Project, type User } from './api'
import { Auth } from './Auth'
import { Issues } from './Issues'
import { OrganizationPicker, organizationRoles } from './OrganizationPicker'
import { ProjectMembers } from './ProjectMembers'
import { navigate, routePath, useRoute } from './router'
import { MetadataEditor, MetadataFields, OrganizationProfile, UserProfile, readMetadata } from './Profiles'
import { Brand, Empty, ErrorNotice, Loading, Modal, Pagination, useAction, useResource } from './ui'

export const projectStatuses = { ACTIVE: 'Active', FINISHED: 'Completed', ARCHIVED: 'Archived' }
const canManage = (organization: Organization) => organization.role !== 'USER'

export function App() {
  const active = useSyncExternalStore(subscribeSession, hasSession)
  const [logoutWarning, setLogoutWarning] = useState('')
  const [passwordChanged, setPasswordChanged] = useState(false)
  useEffect(() => { if (active) setPasswordChanged(false) }, [active])
  if (!active) return <Auth logoutWarning={logoutWarning} passwordChanged={passwordChanged} />
  return <Workspace onPasswordChanged={() => { setLogoutWarning(''); setPasswordChanged(true) }} logout={() => {
    setPasswordChanged(false)
    setLogoutWarning('')
    void signOut().catch(() => setLogoutWarning('You signed out locally, but the server could not confirm that the session was revoked.'))
  }} />
}

function Workspace({ logout, onPasswordChanged }: { logout: () => void; onPasswordChanged: () => void }) {
  const profile = useResource(getProfile)
  const organizations = useResource(api.organizations)
  const route = useRoute()
  const view = route.name
  const routeOrgId = 'orgId' in route ? route.orgId : ''
  const [lastOrgId, setLastOrgId] = useState(routeOrgId)
  const [lastProject, setLastProject] = useState<Project | null>(null)
  const [form, setForm] = useState<'organization' | 'join' | 'project' | null>(null)
  const orgId = routeOrgId || lastOrgId
  const organization = organizations.value?.find(item => item.id === orgId)
  const missingOrganization = Boolean(routeOrgId && organizations.value && !organization)
  const boardProject = route.name === 'board' && lastProject?.id === route.projectId ? lastProject : null
  const navProject = lastProject && lastProject.organizationId === orgId ? lastProject : null

  useEffect(() => {
    if (routePath(route) !== window.location.pathname) navigate(route, { replace: true })
  })
  useEffect(() => {
    if (routeOrgId) setLastOrgId(routeOrgId)
  }, [routeOrgId])

  function openOrganization(id: string) {
    navigate({ name: 'projects', orgId: id })
  }

  function openProject(next: Project) {
    setLastProject(next)
    navigate({ name: 'board', orgId: next.organizationId, projectId: next.id })
  }

  function go(key: typeof nav[number]['key']) {
    if (key === 'organizations' || key === 'mine') navigate({ name: key })
    else if (key === 'board' && navProject) navigate({ name: 'board', orgId, projectId: navProject.id })
    else if (key === 'organization' || key === 'projects') navigate({ name: key, orgId })
  }

  const nav = [
    { key: 'organizations', label: 'Organizations', icon: Building2, visible: true },
    { key: 'organization', label: 'Organization profile', icon: Building2, visible: Boolean(organization) },
    { key: 'projects', label: 'Projects', icon: Folder, visible: Boolean(organization) },
    { key: 'board', label: navProject?.title ?? '', icon: FolderOpen, visible: Boolean(organization && navProject) },
    { key: 'mine', label: 'My issues', icon: ClipboardList, visible: true },
  ] as const

  return <div className="workspace">
    <aside className="sidebar">
      <Brand />
      {organizations.value && organizations.value.length > 0 &&
        <OrganizationPicker organizations={organizations.value} selectedId={organization ? orgId : ''} onSelect={openOrganization} />}
      <nav className="navigation" aria-label="Main navigation">
        {nav.filter(item => item.visible).map(({ key, label, icon: Icon }) =>
          <button key={key} aria-current={view === key ? 'page' : undefined} onClick={() => go(key)}>
            <Icon size={17} /><span>{label}</span>
          </button>)}
      </nav>
      <div className="account">
        <button type="button" className="account-profile" aria-label="My profile" aria-current={view === 'profile' ? 'page' : undefined} onClick={() => navigate({ name: 'profile' })}>
          <span className="avatar" aria-hidden="true">{profile.value?.username.slice(0, 2).toUpperCase() ?? '—'}</span>
          <span className="account-name">{profile.value?.username ?? 'Account'}<small>My profile</small></span>
        </button>
        <button type="button" className="icon-button" title="Sign out" aria-label="Sign out" onClick={logout}><LogOut size={17} /></button>
      </div>
    </aside>
    <main className="main">
      <div className="breadcrumb">TaskTracker <span>/</span> {view === 'mine' || view === 'profile' ? 'Personal' : organization?.title ?? 'Workspace'}
        {view === 'board' && boardProject && <><span>/</span> {boardProject.title}</>}
      </div>
      <ErrorNotice text={profile.error} retry={profile.retry} />
      <ErrorNotice text={organizations.error} retry={organizations.retry} />
      {view === 'organizations' && <>
        <header className="page-heading">
          <div><h1>Organizations</h1><p>Choose a workspace</p></div>
          <div className="row">
            <button className="button" onClick={() => setForm('join')}>Join with a code</button>
            <button className="button primary" onClick={() => setForm('organization')}><Plus size={17} />Create organization</button>
          </div>
        </header>
        {organizations.loading && <Loading />}
        {organizations.value?.length === 0 && <Empty title="No organizations yet">Create an organization or use an invite code to join a team.</Empty>}
        <div className="card-grid">{organizations.value?.map(item =>
          <button key={item.id} className="record-card" onClick={() => openOrganization(item.id)}>
            <span className="record-icon"><Building2 size={20} /></span>
            <span className="badge">{organizationRoles[item.role]}</span>
            <strong>{item.title}</strong>
            <span className="card-description">{item.description || 'No description'}</span>
            <span className="card-link">Open <ArrowRight size={16} /></span>
          </button>)}</div>
      </>}
      {routeOrgId && !organization && organizations.loading && <Loading />}
      {missingOrganization && <Empty title="Organization not found">
        It may have been deleted, or you are no longer a member.{' '}
        <button className="button" onClick={() => navigate({ name: 'organizations' })}>Go to organizations</button>
      </Empty>}
      {view === 'profile' && (profile.value ? <UserProfile user={profile.value} onSaved={profile.setValue} onPasswordChanged={onPasswordChanged} /> : profile.loading && <Loading />)}
      {route.name === 'organization' && organization && <OrganizationProfile key={organization.id}
        organization={organization} openProjects={() => navigate({ name: 'projects', orgId: organization.id })}
        onMembershipChanged={organizations.reload}
        onLeft={() => { setLastOrgId(''); setLastProject(null); navigate({ name: 'organizations' }); organizations.reload() }}
        onSaved={updated => organizations.setValue(current => current?.map(item => item.id === updated.id ? updated : item))} />}
      {route.name === 'projects' && organization && <Projects key={organization.id}
        organization={organization} onOpen={openProject} onCreate={() => setForm('project')}
        openProfile={() => navigate({ name: 'organization', orgId: organization.id })} />}
      {route.name === 'board' && organization && <ProjectBoard key={route.projectId} projectId={route.projectId} organization={organization}
        user={profile.value} selectedIssueId={route.issueId}
        onSelectIssue={issueId => navigate({ name: 'board', orgId: route.orgId, projectId: route.projectId, issueId })}
        onLoaded={setLastProject}
        onDeleted={() => { setLastProject(null); navigate({ name: 'projects', orgId: organization.id }) }} />}
      {route.name === 'mine' && <Issues key="mine" project={null} organizations={organizations.value ?? []} user={profile.value}
        selected={route.issue} onSelect={issue => navigate({ name: 'mine', issue })} />}
      {form && <MetadataForm kind={form} organization={organization} close={() => setForm(null)} onSaved={result => {
        if (form === 'project' && result) {
          openProject(result)
        } else {
          organizations.reload()
        }
        setForm(null)
      }} />}
    </main>
  </div>
}

function Projects({ organization, onOpen, onCreate, openProfile }: {
  organization: Organization; onOpen: (project: Project) => void; onCreate: () => void; openProfile: () => void
}) {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const load = useCallback((signal: AbortSignal) => api.projects(organization.id, page, status, signal), [organization.id, page, status])
  const data = useResource(load)
  return <>
    <header className="page-heading"><div><h1>Projects</h1><p>{organization.title}</p></div>
      <div className="row">
        <button className="button" onClick={openProfile}>Organization profile</button>
        {canManage(organization) && <button className="button primary" onClick={onCreate}><Plus size={17} />Create project</button>}
      </div>
    </header>
    <div className="toolbar">
      <select aria-label="Project status" value={status} onChange={event => { setStatus(event.target.value); setPage(0) }}>
        <option value="">All statuses</option>
        {Object.entries(projectStatuses).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
      </select>
    </div>
    <ErrorNotice text={data.error} retry={data.retry} />
    {data.loading ? <Loading /> : <>
      {data.value?.content.length === 0 && <Empty title={status ? 'No matching projects' : 'No projects yet'} />}
      <div className="card-grid">{data.value?.content.map(item => <button key={item.id} className="record-card" onClick={() => onOpen(item)}>
        <span className="record-icon"><Folder size={20} /></span>
        <span className={`badge ${item.status === 'ACTIVE' ? 'blue' : ''}`}>{projectStatuses[item.status]}</span>
        <strong>{item.title}</strong><span className="card-description">{item.description || 'No description'}</span>
        <span className="card-link">Open <ArrowRight size={16} /></span>
      </button>)}</div>
      <Pagination data={data.value} onChange={setPage} />
    </>}
  </>
}

function ProjectBoard({ projectId, organization, user, selectedIssueId, onSelectIssue, onLoaded, onDeleted }: {
  projectId: string
  organization: Organization
  user?: User
  selectedIssueId?: string
  onSelectIssue: (issueId?: string) => void
  onLoaded: (project: Project) => void
  onDeleted: () => void
}) {
  const load = useCallback((signal: AbortSignal) => api.project(organization.id, projectId, signal), [organization.id, projectId])
  const detail = useResource(load)
  const [editing, setEditing] = useState(false)
  const [managingMembers, setManagingMembers] = useState(false)
  const statusAction = useAction()
  const current = detail.value

  useEffect(() => {
    if (current) onLoaded(current)
  }, [current, onLoaded])

  function deleteProject(target: Project) {
    const typed = window.prompt(`This permanently deletes ${target.title} with all its issues, comments and members. Type the project title to confirm.`)
    if (typed === null) return
    if (typed.trim() !== target.title) {
      window.alert('The title does not match. Nothing was deleted.')
      return
    }
    void statusAction.run(async () => {
      await api.deleteProject(target)
      onDeleted()
    })
  }

  return <>
    <ErrorNotice text={detail.error} retry={detail.retry} />
    {detail.loading && !current && <Loading />}
    {current && <Issues project={current} organizations={[organization]} user={user}
      selected={selectedIssueId ? { organizationId: current.organizationId, projectId: current.id, id: selectedIssueId } : undefined}
      onSelect={issue => onSelectIssue(issue?.id)}
      projectControls={canManage(organization) && <>
      <button className="button" onClick={() => setManagingMembers(true)}><Users size={15} />Members</button>
      <button className="button" onClick={() => setEditing(true)} disabled={statusAction.busy}><Pencil size={15} />Edit project</button>
      <select aria-label="Change project status" value={current.status} disabled={statusAction.busy} onChange={event => {
        const status = event.target.value as Project['status']
        void statusAction.run(async () => detail.setValue(await api.changeProjectStatus(current, status)))
      }}>
        {Object.entries(projectStatuses).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
      </select>
      {organization.role === 'OWNER' && <button className="button danger" disabled={statusAction.busy} onClick={() => deleteProject(current)}><Trash2 size={15} />Delete project</button>}
      </>} projectError={statusAction.error} />}
    {current && editing && <MetadataEditor title="Edit project" initial={current} close={() => setEditing(false)}
      save={async changes => detail.setValue(await api.updateProject(current, changes))} />}
    {current && managingMembers && <ProjectMembers project={current} close={() => setManagingMembers(false)} />}
  </>
}

function MetadataForm({ kind, organization, close, onSaved }: {
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
  return <Modal title={title} close={() => { if (!busy) close() }}>
    <form className="form" onSubmit={submit}>
      {kind === 'join' ? <label>Invite code<input required name="code" maxLength={9} /></label> : <MetadataFields />}
      <ErrorNotice text={error} />
      <button className="button primary" disabled={busy}>{busy ? 'Please wait…' : kind === 'join' ? 'Join' : 'Create'}</button>
    </form>
  </Modal>
}
