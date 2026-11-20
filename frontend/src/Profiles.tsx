import { Building2, Crown, LogOut, Pencil, RefreshCw, Trash2 } from 'lucide-react'
import { useCallback, useState, type FormEvent } from 'react'
import { api, changePassword, type Metadata, type Organization, type OrganizationMember, type OrganizationRole, type User } from './api'
import { organizationRoles } from './OrganizationPicker'
import { Empty, ErrorNotice, Loading, Modal, Pagination, useAction, useResource } from './ui'

type ProfileMetadata = { title: string; description: string | null }

export function MetadataFields({ initial }: { initial?: ProfileMetadata }) {
  return <>
    <label>Title<input name="title" required minLength={8} maxLength={50} defaultValue={initial?.title} /></label>
    <label>Description<textarea name="description" rows={4} maxLength={400} defaultValue={initial?.description ?? ''} /></label>
  </>
}

export function readMetadata(data: FormData): Metadata {
  return { title: String(data.get('title')).trim(), description: String(data.get('description')).trim() }
}

export function MetadataEditor({ title, initial, close, save }: {
  title: string; initial: ProfileMetadata; close: () => void; save: (changes: Partial<Metadata>) => Promise<void>
}) {
  const action = useAction()
  return <Modal title={title} close={() => { if (!action.busy) close() }}>
    <form className="form" onSubmit={event => {
      event.preventDefault()
      const fields = readMetadata(new FormData(event.currentTarget))
      const changes: Partial<Metadata> = {}
      if (fields.title !== initial.title) changes.title = fields.title
      if (fields.description !== (initial.description ?? '')) changes.description = fields.description
      if (!Object.keys(changes).length) { close(); return }
      void action.run(async () => { await save(changes); close() })
    }}>
      <MetadataFields initial={initial} />
      <ErrorNotice text={action.error} />
      <div className="row">
        <button className="button primary" disabled={action.busy}>{action.busy ? 'Saving…' : 'Save changes'}</button>
        <button type="button" className="button" onClick={close} disabled={action.busy}>Cancel</button>
      </div>
    </form>
  </Modal>
}

export function UserProfile({ user, onSaved, onPasswordChanged }: {
  user: User; onSaved: (user: User) => void; onPasswordChanged: () => void
}) {
  const [username, setUsername] = useState(user.username)
  const [saved, setSaved] = useState(false)
  const [passwordMismatch, setPasswordMismatch] = useState(false)
  const profileAction = useAction()
  const passwordAction = useAction()

  function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void profileAction.run(async () => {
      const updated = await api.updateProfile(username.trim())
      setUsername(updated.username)
      onSaved(updated)
      setSaved(true)
    })
  }

  function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    void passwordAction.run(async () => {
      const next = String(data.get('newPassword'))
      if (next !== data.get('confirmPassword')) {
        setPasswordMismatch(true)
        return
      }
      await changePassword(String(data.get('currentPassword')), next)
      onPasswordChanged()
    })
  }

  return <>
    <header className="page-heading"><div><h1>My profile</h1><p>Manage your account and password</p></div></header>
    <div className="profile-grid">
      <section className="profile-panel">
        <div className="profile-identity">
          <span className="avatar profile-avatar" aria-hidden="true">{user.username.slice(0, 2).toUpperCase()}</span>
          <div><h2>{user.username}</h2><p>{user.email}</p></div>
        </div>
        <form className="form" onSubmit={saveProfile}>
          <label>Username<input name="username" value={username} minLength={3} maxLength={50} autoComplete="username" required
            onChange={event => { setUsername(event.target.value); setSaved(false) }} /></label>
          <ErrorNotice text={profileAction.error} />
          {saved && <p className="success-message" role="status">Username saved.</p>}
          <button className="button primary" disabled={profileAction.busy || username.trim() === user.username}>Save username</button>
        </form>
      </section>
      <section className="profile-panel">
        <h2>Change password</h2><p>You’ll need to sign in again after changing your password.</p>
        <form className="form" onSubmit={savePassword} onChange={() => setPasswordMismatch(false)}>
          <label>Current password<input name="currentPassword" type="password" autoComplete="current-password" required /></label>
          <label>New password<input name="newPassword" type="password" minLength={8} autoComplete="new-password" required /></label>
          <label>Confirm new password<input name="confirmPassword" type="password" minLength={8} autoComplete="new-password" required /></label>
          <ErrorNotice text={passwordMismatch ? 'The new passwords do not match.' : passwordAction.error} />
          <button className="button primary" disabled={passwordAction.busy}>Change password</button>
        </form>
      </section>
    </div>
  </>
}

export function OrganizationProfile({ organization, onSaved, onMembershipChanged, onLeft, openProjects }: {
  organization: Organization
  onSaved: (organization: Organization) => void
  onMembershipChanged: () => void
  onLeft: () => void
  openProjects: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [showInvite, setShowInvite] = useState(false)
  const [saved, setSaved] = useState(false)
  const [page, setPage] = useState(0)
  const { id, role, memberId } = organization
  const canEdit = role === 'OWNER' || role === 'ADMIN'
  const detail = useResource(useCallback(signal => api.organization(id, signal), [id]))
  const members = useResource(useCallback(signal => api.organizationMembers(id, page, signal), [id, page]))
  const invite = useResource(useCallback(signal => showInvite ? api.inviteCode(id, signal) : Promise.resolve(null), [id, showInvite]))
  const memberAction = useAction()
  const inviteAction = useAction()
  const accountAction = useAction()
  const current = detail.value ?? organization

  function canManage(member: OrganizationMember) {
    return canEdit && member.id !== memberId && member.role !== 'OWNER' && !(role === 'ADMIN' && member.role === 'ADMIN')
  }

  function changeRole(member: OrganizationMember, next: OrganizationRole) {
    void memberAction.run(async () => {
      const updated = await api.updateOrganizationMember(id, member.id, next)
      members.setValue(data => data && { ...data, content: data.content.map(item => item.id === updated.id ? updated : item) })
    })
  }

  function remove(member: OrganizationMember) {
    if (!window.confirm(`Remove ${member.username} from ${current.title}? They will lose access to all its projects.`)) return
    void memberAction.run(async () => {
      await api.removeOrganizationMember(id, member.id)
      if (page > 0 && members.value?.content.length === 1) setPage(page - 1)
      else members.reload()
    })
  }

  function transfer(member: OrganizationMember) {
    if (!window.confirm(`Make ${member.username} the owner of ${current.title}? You will become an administrator.`)) return
    void memberAction.run(async () => {
      await api.transferOwnership(id, member.id)
      members.reload()
      onMembershipChanged()
    })
  }

  function regenerate() {
    if (!window.confirm('Generate a new invite code? The current code will stop working immediately.')) return
    void inviteAction.run(async () => invite.setValue(await api.regenerateInviteCode(id)))
  }

  function leave() {
    if (!window.confirm(`Leave ${current.title}? You will need a new invite code to join again.`)) return
    void accountAction.run(async () => {
      await api.leaveOrganization(id)
      onLeft()
    })
  }

  function deleteOrganization() {
    const typed = window.prompt(`This permanently deletes ${current.title} with all its projects, issues and comments. Type the organization title to confirm.`)
    if (typed === null) return
    if (typed.trim() !== current.title) {
      window.alert('The title does not match. Nothing was deleted.')
      return
    }
    void accountAction.run(async () => {
      await api.deleteOrganization(id)
      onLeft()
    })
  }

  return <>
    <header className="page-heading"><div><h1>Organization profile</h1><p>{current.title}</p></div>
      <button className="button" onClick={openProjects}>View projects</button>
    </header>
    <ErrorNotice text={detail.error} retry={detail.retry} />
    <div className="profile-grid">
      <section className="profile-panel">
        <div className="profile-identity">
          <span className="record-icon"><Building2 size={20} aria-hidden="true" /></span>
          <div><h2>{current.title}</h2><span className="badge blue">{organizationRoles[role]}</span></div>
        </div>
        <p className="profile-description">{current.description || 'No description'}</p>
        {saved && <p className="success-message" role="status">Organization details saved.</p>}
        {canEdit && <div className="row">
          <button className="button primary" disabled={detail.loading} onClick={() => { setSaved(false); setEditing(true) }}><Pencil size={15} />Edit organization</button>
          <button className="button" aria-expanded={showInvite} onClick={() => setShowInvite(current => !current)}>{showInvite ? 'Hide invite code' : 'Show invite code'}</button>
        </div>}
        {canEdit && showInvite && <div className="invite-code">
          <ErrorNotice text={invite.error || inviteAction.error} retry={invite.error ? invite.retry : undefined} />
          {invite.loading && <Loading />}
          {invite.value && <div className="row">
            <span>Invite code <code>{invite.value.inviteCode}</code></span>
            <button className="button" disabled={inviteAction.busy} onClick={regenerate}><RefreshCw size={15} />Regenerate</button>
          </div>}
        </div>}
        <div className="danger-zone">
          <ErrorNotice text={accountAction.error} />
          {role === 'OWNER'
            ? <>
              <p className="muted">To leave this organization, first make another member the owner.</p>
              <button className="button danger" disabled={accountAction.busy} onClick={deleteOrganization}><Trash2 size={15} />Delete organization</button>
            </>
            : <button className="button danger" disabled={accountAction.busy} onClick={leave}><LogOut size={15} />Leave organization</button>}
        </div>
      </section>
      <section className="profile-panel">
        <h2>Members</h2>
        <ErrorNotice text={members.error} retry={members.retry} />
        <ErrorNotice text={memberAction.error} />
        {members.loading ? <Loading /> : <>
          {members.value?.content.length === 0 && <Empty title="No members yet" />}
          <ul className="profile-members">{members.value?.content.map(member => <li key={member.id}>
            <span>{member.username}{member.id === memberId && <small className="muted"> (you)</small>}</span>
            <span className="row">
              {canManage(member)
                ? <select aria-label={`Role of ${member.username}`} value={member.role} disabled={memberAction.busy}
                  onChange={event => changeRole(member, event.target.value as OrganizationRole)}>
                  <option value="ADMIN">{organizationRoles.ADMIN}</option>
                  <option value="USER">{organizationRoles.USER}</option>
                </select>
                : <span className="badge">{organizationRoles[member.role]}</span>}
              {role === 'OWNER' && member.id !== memberId && <button type="button" className="icon-button" title="Make owner"
                aria-label={`Make ${member.username} the owner`} disabled={memberAction.busy} onClick={() => transfer(member)}><Crown size={16} /></button>}
              {canManage(member) && <button type="button" className="icon-button" title="Remove from organization"
                aria-label={`Remove ${member.username}`} disabled={memberAction.busy} onClick={() => remove(member)}><Trash2 size={16} /></button>}
            </span>
          </li>)}</ul>
          <Pagination data={members.value} onChange={setPage} disabled={memberAction.busy} />
        </>}
      </section>
    </div>
    {canEdit && editing && <MetadataEditor title="Edit organization" initial={current} close={() => setEditing(false)} save={async changes => {
      const updated = await api.updateOrganization(id, changes)
      detail.setValue(updated)
      onSaved({ ...organization, ...updated })
      setSaved(true)
    }} />}
  </>
}
