import { Trash2, UserPlus } from 'lucide-react'
import { useCallback, useState, type FormEvent } from 'react'
import { api, type Project, type ProjectMember, type ProjectMemberRole } from './api'
import { confirmAction, toast } from './feedbackStore'
import { Avatar } from './bits'
import { plural, projectRoles, toOptions } from './labels'
import { Select } from './Select'
import { useAction, useResource } from './hooks'
import { Empty, ErrorNotice, Loading } from './ui'

export function ProjectMembers({ project, canManage, selfId }: { project: Project; canManage: boolean; selfId?: string }) {
  const roster = useResource(useCallback((signal: AbortSignal) => api.projectMembers(project, signal), [project]))
  const organizationMembers = useResource(
    useCallback((signal: AbortSignal) => api.allOrganizationMembers(project.organizationId, signal), [project.organizationId]),
  )
  const action = useAction()
  const [candidate, setCandidate] = useState('')
  const [role, setRole] = useState<ProjectMemberRole>('USER')
  const members = roster.value ?? []
  const inProject = new Set(members.map(member => member.organizationMemberId))
  const candidates = (organizationMembers.value ?? []).filter(member => !inProject.has(member.id))

  function replace(updated: ProjectMember) {
    roster.setValue(current => current?.map(member => (member.id === updated.id ? updated : member)))
  }

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!candidate) return
    void action.run(async () => {
      const added = await api.addProjectMember(project, candidate, role)
      roster.setValue(current => [...(current ?? []), added])
      toast(`${added.username} added to the project`)
      setCandidate('')
      setRole('USER')
    })
  }

  function changeRole(member: ProjectMember, next: ProjectMemberRole) {
    void action.run(async () => {
      replace(await api.updateProjectMember(project, member, next))
      toast(`${member.username} is now ${projectRoles[next].toLowerCase()}`)
    })
  }

  async function remove(member: ProjectMember) {
    if (
      !(await confirmAction({
        title: 'Remove from project?',
        confirmLabel: 'Remove',
        danger: true,
        message: `${member.username} will lose access to ${project.title}. Issues assigned to them will become unassigned.`,
      }))
    )
      return
    void action.run(async () => {
      await api.removeProjectMember(project, member)
      roster.setValue(current => current?.filter(item => item.id !== member.id))
      toast(`${member.username} removed from the project`)
    })
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>Project members</h2>
        {roster.value && <span className="muted">{plural(members.length, 'member')}</span>}
      </div>
      <div className="form">
        <ErrorNotice text={roster.error || organizationMembers.error} retry={roster.error ? roster.retry : organizationMembers.retry} />
        <ErrorNotice text={action.error} />
        {canManage && organizationMembers.value && (
          <form className="member-add" onSubmit={add}>
            <Select
              aria-label="Organization member"
              value={candidate}
              disabled={action.busy || candidates.length === 0}
              onChange={setCandidate}
              options={[
                { value: '', label: candidates.length ? 'Choose a member…' : 'Everyone is already in the project' },
                ...candidates.map(member => ({
                  value: member.id,
                  label: member.username,
                  hint: member.role === 'USER' ? member.email : `${member.email} · organization ${member.role.toLowerCase()}`,
                })),
              ]}
            />
            <Select
              aria-label="Role for the new member"
              value={role}
              disabled={action.busy}
              onChange={next => setRole(next as ProjectMemberRole)}
              options={toOptions(projectRoles)}
            />
            <button className="button primary" disabled={action.busy || !candidate}>
              <UserPlus size={16} />
              Add
            </button>
          </form>
        )}
        {roster.loading ? (
          <Loading />
        ) : (
          roster.value &&
          (members.length === 0 ? (
            <Empty title="No project members yet">
              Organization owners and admins can always open the project. Add other members to give them access.
            </Empty>
          ) : (
            <ul className="profile-members">
              {members.map(member => (
                <li key={member.id}>
                  <span className="member-identity">
                    <Avatar name={member.username} size="tiny" />
                    <span>
                      <strong>{member.username}</strong>
                      <small className="member-email">{member.email}</small>
                    </span>
                  </span>
                  <span className="member-controls">
                    {canManage && member.id !== selfId ? (
                      <>
                        <Select
                          aria-label={`Role of ${member.username}`}
                          value={member.role}
                          disabled={action.busy}
                          onChange={next => changeRole(member, next as ProjectMemberRole)}
                          options={toOptions(projectRoles)}
                        />
                        <button
                          type="button"
                          className="icon-button"
                          aria-label={`Remove ${member.username}`}
                          title="Remove from project"
                          disabled={action.busy}
                          onClick={() => void remove(member)}>
                          <Trash2 size={16} />
                        </button>
                      </>
                    ) : (
                      <span className="badge">{projectRoles[member.role]}</span>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          ))
        )}
      </div>
    </section>
  )
}
