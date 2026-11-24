import { ArrowRight, Building2, CircleCheck, ClipboardList, Copy, Crown, LogOut, RefreshCw, Trash2 } from 'lucide-react'
import { useCallback, useState, type FormEvent } from 'react'
import { api, changePassword, type Metadata, type Organization, type OrganizationMember, type OrganizationRole, type User } from './api'
import { confirmAction, toast } from './feedbackStore'
import { Avatar } from './bits'
import { MetadataFields } from './forms'
import { Select } from './Select'
import { setThemePreference, useTheme, type ThemePreference } from './theme'
import { initials, noun, organizationRoles, plural, readMetadata } from './labels'
import { useAction, useResource } from './hooks'
import { Empty, ErrorNotice, Loading, Pagination } from './ui'

export function UserProfile({
  user,
  organizations,
  onSaved,
  onPasswordChanged,
  openOrganization,
}: {
  user: User
  organizations: Organization[]
  onSaved: (user: User) => void
  onPasswordChanged: () => void
  openOrganization: (id: string) => void
}) {
  const { preference: themePreference } = useTheme()
  const work = useResource(useCallback((signal: AbortSignal) => api.myIssues(signal), []))
  const issues = work.value ?? []
  const open = issues.filter(issue => issue.status !== 'DONE').length
  const done = issues.length - open
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

  return (
    <>
      <header className="page-heading">
        <div>
          <h1>Your profile</h1>
          <p>Manage your account and password</p>
        </div>
      </header>
      <section className="summary-strip" aria-label="My work">
        <span>
          <ClipboardList size={16} />
          <strong>{work.value ? open : '–'}</strong> open {noun(open, 'issue')}
        </span>
        <span>
          <CircleCheck size={16} />
          <strong>{work.value ? done : '–'}</strong> done
        </span>
        <span>
          <Building2 size={16} />
          <strong>{organizations.length}</strong> {noun(organizations.length, 'organization')}
        </span>
      </section>
      <div className="profile-grid">
        <section className="profile-panel">
          <div className="profile-identity">
            <span className="avatar profile-avatar" aria-hidden="true">
              {initials(user.username)}
            </span>
            <div>
              <h2>{user.username}</h2>
              <p>{user.email}</p>
            </div>
          </div>
          <form className="form" onSubmit={saveProfile}>
            <label>
              Username
              <input
                name="username"
                value={username}
                minLength={3}
                maxLength={50}
                autoComplete="username"
                required
                onChange={event => {
                  setUsername(event.target.value)
                  setSaved(false)
                }}
              />
            </label>
            <ErrorNotice text={profileAction.error} />
            {saved && (
              <p className="success-message" role="status">
                Username saved.
              </p>
            )}
            <button className="button primary" disabled={profileAction.busy || username.trim() === user.username}>
              Save username
            </button>
          </form>
        </section>
        <section className="profile-panel">
          <h2>Change password</h2>
          <p>You’ll need to sign in again after changing your password.</p>
          <form className="form" onSubmit={savePassword} onChange={() => setPasswordMismatch(false)}>
            <label>
              Current password
              <input name="currentPassword" type="password" autoComplete="current-password" required />
            </label>
            <label>
              New password
              <input name="newPassword" type="password" minLength={8} autoComplete="new-password" required />
            </label>
            <label>
              Confirm new password
              <input name="confirmPassword" type="password" minLength={8} autoComplete="new-password" required />
            </label>
            <ErrorNotice text={passwordMismatch ? 'The new passwords do not match.' : passwordAction.error} />
            <button className="button primary" disabled={passwordAction.busy}>
              Change password
            </button>
          </form>
        </section>
        <section className="profile-panel wide-panel">
          <div className="panel-heading">
            <h2>Appearance</h2>
            <span className="muted">Saved in this browser</span>
          </div>
          <label className="appearance-field">
            Theme
            <Select
              value={themePreference}
              onChange={next => setThemePreference(next as ThemePreference)}
              options={[
                { value: 'light', label: 'Light' },
                { value: 'dark', label: 'Dark' },
                { value: 'system', label: 'System', hint: 'Follow the operating system' },
              ]}
            />
          </label>
        </section>
        <section className="profile-panel wide-panel">
          <h2>My organizations</h2>
          {organizations.length === 0 ? (
            <p className="muted">You are not a member of any organization yet.</p>
          ) : (
            <ul className="link-list">
              {organizations.map(org => (
                <li key={org.id}>
                  <button type="button" className="focus-item" onClick={() => openOrganization(org.id)}>
                    <span className="record-icon small-icon">
                      <Building2 size={16} />
                    </span>
                    <span className="focus-text">
                      <strong>{org.title}</strong>
                      <small>{org.description || 'No description'}</small>
                    </span>
                    <span className="badge">{organizationRoles[org.role]}</span>
                    <ArrowRight size={16} className="muted" />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}

export function OrganizationMembers({
  organization,
  onMembershipChanged,
}: {
  organization: Organization
  onMembershipChanged: () => void
}) {
  const [showInvite, setShowInvite] = useState(false)
  const [page, setPage] = useState(0)
  const { id, role, memberId } = organization
  const canEdit = role === 'OWNER' || role === 'ADMIN'
  const members = useResource(useCallback(signal => api.organizationMembers(id, page, signal), [id, page]))
  const invite = useResource(useCallback(signal => (showInvite ? api.inviteCode(id, signal) : Promise.resolve(null)), [id, showInvite]))
  const memberAction = useAction()
  const inviteAction = useAction()

  function canManage(member: OrganizationMember) {
    return canEdit && member.id !== memberId && member.role !== 'OWNER' && !(role === 'ADMIN' && member.role === 'ADMIN')
  }

  async function copyInvite(code: string) {
    try {
      await navigator.clipboard.writeText(code)
      toast('Invite code copied')
    } catch {
      toast('Copy the code manually')
    }
  }

  function changeRole(member: OrganizationMember, next: OrganizationRole) {
    void memberAction.run(async () => {
      const updated = await api.updateOrganizationMember(id, member.id, next)
      members.setValue(data => data && { ...data, content: data.content.map(item => (item.id === updated.id ? updated : item)) })
      toast(`${updated.username} is now ${organizationRoles[updated.role].toLowerCase()}`)
    })
  }

  async function remove(member: OrganizationMember) {
    if (
      !(await confirmAction({
        title: 'Remove member?',
        confirmLabel: 'Remove',
        danger: true,
        message: `${member.username} will be removed from ${organization.title} and lose access to all its projects.`,
      }))
    )
      return
    void memberAction.run(async () => {
      await api.removeOrganizationMember(id, member.id)
      if (page > 0 && members.value?.content.length === 1) setPage(page - 1)
      else members.reload()
      toast(`${member.username} removed`)
    })
  }

  async function transfer(member: OrganizationMember) {
    if (
      !(await confirmAction({
        title: 'Transfer ownership?',
        confirmLabel: 'Transfer ownership',
        message: `${member.username} will become the owner of ${organization.title}. You will become an administrator.`,
      }))
    )
      return
    void memberAction.run(async () => {
      await api.transferOwnership(id, member.id)
      members.reload()
      onMembershipChanged()
      toast(`${member.username} is now the owner`)
    })
  }

  async function regenerate() {
    if (
      !(await confirmAction({
        title: 'Generate a new invite code?',
        confirmLabel: 'Generate',
        message: 'The current code will stop working immediately.',
      }))
    )
      return
    void inviteAction.run(async () => {
      invite.setValue(await api.regenerateInviteCode(id))
      toast('New invite code generated')
    })
  }

  return (
    <>
      <header className="page-heading">
        <div>
          <h1>Members</h1>
          <p>People in {organization.title}</p>
        </div>
      </header>
      <div className="page-stack">
        {canEdit && (
          <section className="panel">
            <div className="panel-heading">
              <h2>Invite people</h2>
              <span className="muted">Anyone with the code can join as a member</span>
            </div>
            {!showInvite ? (
              <div>
                <button className="button" onClick={() => setShowInvite(true)}>
                  Show invite code
                </button>
              </div>
            ) : (
              <>
                <ErrorNotice text={invite.error || inviteAction.error} retry={invite.error ? invite.retry : undefined} />
                {invite.loading && <Loading />}
                {invite.value && (
                  <div className="invite-row invite-code">
                    <span>
                      Invite code <code>{invite.value.inviteCode}</code>
                    </span>
                    <span className="row">
                      <button className="button" onClick={() => void copyInvite(invite.value!.inviteCode)}>
                        <Copy size={15} />
                        Copy
                      </button>
                      <button className="button" disabled={inviteAction.busy} onClick={() => void regenerate()}>
                        <RefreshCw size={15} />
                        Regenerate
                      </button>
                      <button className="button" onClick={() => setShowInvite(false)}>
                        Hide
                      </button>
                    </span>
                  </div>
                )}
              </>
            )}
          </section>
        )}
        <section className="panel">
          <div className="panel-heading">
            <h2>All members</h2>
            {members.value && <span className="muted">{plural(members.value.totalElements, 'member')}</span>}
          </div>
          <ErrorNotice text={members.error} retry={members.retry} />
          <ErrorNotice text={memberAction.error} />
          {members.loading && !members.value ? (
            <Loading />
          ) : (
            <>
              {members.value?.content.length === 0 && <Empty title="No members yet" />}
              <ul className="profile-members">
                {members.value?.content.map(member => (
                  <li key={member.id}>
                    <span className="member-identity">
                      <Avatar name={member.username} size="tiny" />
                      <span>
                        <strong>
                          {member.username}
                          {member.id === memberId && <small className="muted"> (you)</small>}
                        </strong>
                        <small className="member-email">{member.email}</small>
                      </span>
                    </span>
                    <span className="member-controls">
                      {canManage(member) ? (
                        <Select
                          aria-label={`Role of ${member.username}`}
                          value={member.role}
                          disabled={memberAction.busy}
                          onChange={next => changeRole(member, next as OrganizationRole)}
                          options={[
                            { value: 'ADMIN', label: organizationRoles.ADMIN },
                            { value: 'USER', label: organizationRoles.USER },
                          ]}
                        />
                      ) : (
                        <span className="badge">{organizationRoles[member.role]}</span>
                      )}
                      {role === 'OWNER' && member.id !== memberId && (
                        <button
                          type="button"
                          className="icon-button"
                          title="Make owner"
                          aria-label={`Make ${member.username} the owner`}
                          disabled={memberAction.busy}
                          onClick={() => void transfer(member)}>
                          <Crown size={16} />
                        </button>
                      )}
                      {canManage(member) && (
                        <button
                          type="button"
                          className="icon-button"
                          title="Remove from organization"
                          aria-label={`Remove ${member.username}`}
                          disabled={memberAction.busy}
                          onClick={() => void remove(member)}>
                          <Trash2 size={16} />
                        </button>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
              <Pagination data={members.value} onChange={setPage} disabled={memberAction.busy} />
            </>
          )}
        </section>
      </div>
    </>
  )
}

export function OrganizationSettings({
  organization,
  onSaved,
  onLeft,
}: {
  organization: Organization
  onSaved: (organization: Organization) => void
  onLeft: () => void
}) {
  const { id, role } = organization
  const canEdit = role === 'OWNER' || role === 'ADMIN'
  const detail = useResource(useCallback(signal => api.organization(id, signal), [id]))
  const saveAction = useAction()
  const accountAction = useAction()
  const current = detail.value ?? organization

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = readMetadata(new FormData(event.currentTarget))
    const changes: Partial<Metadata> = {}
    if (fields.title !== current.title) changes.title = fields.title
    if (fields.description !== (current.description ?? '')) changes.description = fields.description
    if (!Object.keys(changes).length) return
    void saveAction.run(async () => {
      const updated = await api.updateOrganization(id, changes)
      detail.setValue(updated)
      onSaved({ ...organization, ...updated })
      toast('Organization updated')
    })
  }

  async function leave() {
    if (
      !(await confirmAction({
        title: 'Leave organization?',
        confirmLabel: 'Leave',
        danger: true,
        message: `You will need a new invite code to join ${current.title} again.`,
      }))
    )
      return
    void accountAction.run(async () => {
      await api.leaveOrganization(id)
      toast(`You left ${current.title}`)
      onLeft()
    })
  }

  async function deleteOrganization() {
    if (
      !(await confirmAction({
        title: 'Delete organization?',
        confirmLabel: 'Delete organization',
        danger: true,
        requireText: current.title,
        message: `${current.title} will be permanently deleted with all its projects, issues and comments.`,
      }))
    )
      return
    void accountAction.run(async () => {
      await api.deleteOrganization(id)
      toast(`${current.title} deleted`)
      onLeft()
    })
  }

  return (
    <>
      <header className="page-heading">
        <div>
          <h1>Settings</h1>
          <p>{current.title}</p>
        </div>
      </header>
      <ErrorNotice text={detail.error} retry={detail.retry} />
      <div className="page-stack">
        <section className="panel">
          <div className="panel-heading">
            <h2>Details</h2>
            <span className="badge blue">{organizationRoles[role]}</span>
          </div>
          {canEdit ? (
            <form key={current.title + (current.description ?? '')} className="form settings-form" onSubmit={save}>
              <MetadataFields initial={current} />
              <ErrorNotice text={saveAction.error} />
              <button className="button primary" disabled={saveAction.busy || detail.loading}>
                {saveAction.busy ? 'Saving…' : 'Save changes'}
              </button>
            </form>
          ) : (
            <div className="settings-readonly">
              <div className="detail-field">
                Title<strong>{current.title}</strong>
              </div>
              <div className="detail-field">
                Description<p className="profile-description">{current.description || 'No description'}</p>
              </div>
              <p className="muted">Only owners and administrators can change these details.</p>
            </div>
          )}
        </section>
        <section className="panel danger-panel">
          <div className="panel-heading">
            <h2>Danger zone</h2>
          </div>
          <ErrorNotice text={accountAction.error} />
          {role === 'OWNER' ? (
            <div className="danger-row">
              <span>
                <strong>Delete organization</strong>
                <small>Deletes every project, issue and comment. To leave instead, make another member the owner first.</small>
              </span>
              <button className="button danger" disabled={accountAction.busy} onClick={() => void deleteOrganization()}>
                <Trash2 size={15} />
                Delete organization
              </button>
            </div>
          ) : (
            <div className="danger-row">
              <span>
                <strong>Leave organization</strong>
                <small>You will lose access to all its projects.</small>
              </span>
              <button className="button danger" disabled={accountAction.busy} onClick={() => void leave()}>
                <LogOut size={15} />
                Leave organization
              </button>
            </div>
          )}
        </section>
      </div>
    </>
  )
}
