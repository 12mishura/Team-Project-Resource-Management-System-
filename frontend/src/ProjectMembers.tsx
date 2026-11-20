import { Trash2, UserPlus } from 'lucide-react'
import { useCallback, useState, type FormEvent } from 'react'
import { api, type Project, type ProjectMember, type ProjectMemberRole } from './api'
import { Empty, ErrorNotice, Loading, Modal, useAction, useResource } from './ui'

export const projectRoles: Record<ProjectMemberRole, string> = { ADMIN: 'Admin', EXTENDED: 'Extended', USER: 'User' }

export function ProjectMembers({ project, close }: { project: Project; close: () => void }) {
  const roster = useResource(useCallback((signal: AbortSignal) => api.projectMembers(project, signal), [project]))
  const organizationMembers = useResource(useCallback((signal: AbortSignal) =>
    api.allOrganizationMembers(project.organizationId, signal), [project.organizationId]))
  const action = useAction()
  const [candidate, setCandidate] = useState('')
  const [role, setRole] = useState<ProjectMemberRole>('USER')
  const members = roster.value ?? []
  const inProject = new Set(members.map(member => member.organizationMemberId))
  const candidates = (organizationMembers.value ?? []).filter(member => member.role === 'USER' && !inProject.has(member.id))

  function replace(updated: ProjectMember) {
    roster.setValue(current => current?.map(member => member.id === updated.id ? updated : member))
  }

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!candidate) return
    void action.run(async () => {
      const added = await api.addProjectMember(project, candidate, role)
      roster.setValue(current => [...(current ?? []), added])
      setCandidate('')
      setRole('USER')
    })
  }

  function changeRole(member: ProjectMember, next: ProjectMemberRole) {
    void action.run(async () => replace(await api.updateProjectMember(project, member, next)))
  }

  function remove(member: ProjectMember) {
    if (!window.confirm(`Remove ${member.username} from ${project.title}? Issues assigned to them will become unassigned.`)) return
    void action.run(async () => {
      await api.removeProjectMember(project, member)
      roster.setValue(current => current?.filter(item => item.id !== member.id))
    })
  }

  return <Modal title="Project members" close={() => { if (!action.busy) close() }}>
    <div className="form">
      <ErrorNotice text={roster.error || organizationMembers.error} retry={roster.error ? roster.retry : organizationMembers.retry} />
      <ErrorNotice text={action.error} />
      {organizationMembers.value && <form className="member-add" onSubmit={add}>
        <select aria-label="Organization member" value={candidate} disabled={action.busy || candidates.length === 0}
          onChange={event => setCandidate(event.target.value)}>
          <option value="">{candidates.length ? 'Choose a member…' : 'Everyone is already in the project'}</option>
          {candidates.map(member => <option key={member.id} value={member.id}>{member.username} · {member.email}</option>)}
        </select>
        <select aria-label="Role for the new member" value={role} disabled={action.busy}
          onChange={event => setRole(event.target.value as ProjectMemberRole)}>
          {Object.entries(projectRoles).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
        </select>
        <button className="button primary" disabled={action.busy || !candidate}><UserPlus size={16} />Add</button>
      </form>}
      {roster.loading ? <Loading /> : roster.value && (members.length === 0 ? <Empty title="No project members yet">
        Organization owners and admins can always open the project. Add other members to give them access.
      </Empty> : <ul className="profile-members">{members.map(member => <li key={member.id}>
        <span>{member.username}<small className="muted"> {member.email}</small></span>
        <span className="row">
          <select aria-label={`Role of ${member.username}`} value={member.role} disabled={action.busy}
            onChange={event => changeRole(member, event.target.value as ProjectMemberRole)}>
            {Object.entries(projectRoles).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
          <button type="button" className="icon-button" aria-label={`Remove ${member.username}`} title="Remove from project"
            disabled={action.busy} onClick={() => remove(member)}><Trash2 size={16} /></button>
        </span>
      </li>)}</ul>)}
    </div>
  </Modal>
}
