import { AlertTriangle, ChevronLeft, ChevronRight, Download, Gauge, Pencil } from 'lucide-react'
import { useCallback, useState, type FormEvent } from 'react'
import { api, type MemberWorkload, type Organization, type Project, type Workload } from './api'
import { Avatar } from './bits'
import { toast } from './feedbackStore'
import { useAction, usePeriod, useResource, type Period } from './hooks'
import { organizationRoles, plural, projectRoles } from './labels'
import { downloadCsv, formatDay, formatDuration, formatHours, formatRange, isWeekend, MINUTES_PER_WORK_DAY } from './time'
import { Empty, ErrorNotice, Loading, Modal } from './ui'

export function PeriodControls({ period, spans = true }: { period: Period; spans?: boolean }) {
  return (
    <div className="period-controls">
      {spans && (
        <div className="segmented" aria-label="Period length">
          <button aria-pressed={period.span === 'week'} onClick={() => period.setSpan('week')}>
            Week
          </button>
          <button aria-pressed={period.span === 'month'} onClick={() => period.setSpan('month')}>
            4 weeks
          </button>
        </div>
      )}
      <div className="row period-stepper">
        <button type="button" className="icon-button" aria-label="Previous period" onClick={period.previous}>
          <ChevronLeft size={16} />
        </button>
        <strong className="period-label">{formatRange(period.from, period.to)}</strong>
        <button type="button" className="icon-button" aria-label="Next period" disabled={period.isCurrent} onClick={period.next}>
          <ChevronRight size={16} />
        </button>
        {!period.isCurrent && (
          <button type="button" className="text-button" onClick={period.reset}>
            Today
          </button>
        )}
      </div>
    </div>
  )
}

function heatLevel(minutes: number) {
  if (!minutes) return 0
  if (minutes < 2 * 60) return 1
  if (minutes < 5 * 60) return 2
  if (minutes <= MINUTES_PER_WORK_DAY) return 3
  return 4
}

function utilization(member: Pick<MemberWorkload, 'loggedMinutes' | 'capacityMinutes'>) {
  return member.capacityMinutes ? Math.round((member.loggedMinutes / member.capacityMinutes) * 100) : 0
}

type RoleNames = Record<string, string>

function exportWorkload(name: string, data: Workload, roles: RoleNames) {
  const days = data.members[0]?.days.map(day => day.date) ?? []
  downloadCsv(`${name}-workload-${data.from}-${data.to}.csv`, [
    ['Member', 'Role', 'Logged hours', 'Capacity hours', 'Utilization %', 'Open issues', 'Unestimated', 'Remaining hours', ...days],
    ...data.members.map(member => [
      member.username,
      roles[member.role] ?? member.role,
      (member.loggedMinutes / 60).toFixed(2),
      (member.capacityMinutes / 60).toFixed(2),
      utilization(member),
      member.openIssues,
      member.unestimatedIssues,
      (member.remainingEstimateMinutes / 60).toFixed(2),
      ...member.days.map(day => (day.minutes / 60).toFixed(2)),
    ]),
  ])
}

function WorkloadView({
  name,
  description,
  load,
  showProjects,
  roles,
  editCapacity,
  version = 0,
  page = false,
}: {
  version?: number
  page?: boolean
  name: string
  description: string
  load: (from: string, to: string, signal: AbortSignal) => Promise<Workload>
  showProjects: boolean
  roles: RoleNames
  editCapacity?: (member: MemberWorkload) => void
}) {
  const period = usePeriod()
  const { from, to } = period
  const data = useResource(
    useCallback((signal: AbortSignal) => (version >= 0 ? load(from, to, signal) : Promise.reject()), [load, from, to, version]),
  )
  const workload = data.value
  const overloaded = workload?.members.filter(member => member.overloaded).length ?? 0
  const total = workload ? { loggedMinutes: workload.loggedMinutes, capacityMinutes: workload.capacityMinutes } : null

  return (
    <section className="workload" aria-label="Workload">
      <header className={page ? 'page-heading workload-heading' : 'section-heading workload-heading'}>
        <div>
          {page ? <h1>Team workload</h1> : <h2>Workload</h2>}
          <p className={page ? undefined : 'muted'}>{description}</p>
        </div>
        <div className="row">
          <PeriodControls period={period} />
          <button
            type="button"
            className="button"
            disabled={!workload?.members.length}
            onClick={() => workload && exportWorkload(name, workload, roles)}>
            <Download size={15} />
            Export CSV
          </button>
        </div>
      </header>
      <ErrorNotice text={data.error} retry={data.retry} />
      {data.loading && !workload && <Loading />}
      {workload && (
        <>
          <section className="time-stats" aria-label="Workload summary">
            <div>
              <strong>{formatHours(workload.loggedMinutes)}</strong>
              <span>Logged</span>
            </div>
            <div>
              <strong>{formatHours(workload.capacityMinutes)}</strong>
              <span>Capacity · {plural(workload.workingDays, 'work day')}</span>
            </div>
            <div>
              <strong>{total ? utilization(total) : 0}%</strong>
              <span>Utilization</span>
            </div>
            <div data-tone={overloaded ? 'warning' : undefined}>
              <strong>{formatHours(workload.remainingEstimateMinutes)}</strong>
              <span>{overloaded ? `Remaining · ${plural(overloaded, 'person')} overloaded` : 'Remaining estimate'}</span>
            </div>
          </section>
          {workload.members.length === 0 ? (
            <Empty title="No members yet" icon={<Gauge size={22} />}>
              Add people to see how their time is spent.
            </Empty>
          ) : (
            <div className="table-scroll">
              <table className="workload-table" data-span={period.span}>
                <thead>
                  <tr>
                    <th scope="col">Member</th>
                    <th scope="col">Logged / capacity</th>
                    <th scope="col" className="heat-head">
                      <span className="heat-row" aria-hidden="true">
                        {workload.members[0].days.map(day => (
                          <span key={day.date} className="heat-label" data-weekend={isWeekend(day.date) || undefined}>
                            {period.span === 'week' ? formatDay(day.date, { weekday: 'narrow' }) : ''}
                          </span>
                        ))}
                      </span>
                      <span className="sr-only">Hours per day</span>
                    </th>
                    <th scope="col" className="numeric">
                      Open
                    </th>
                    <th scope="col" className="numeric">
                      Remaining
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {workload.members.map(member => (
                    <MemberRow
                      key={member.organizationMemberId}
                      member={member}
                      role={roles[member.role] ?? member.role}
                      showProjects={showProjects}
                      editCapacity={editCapacity}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted small-text">
            Capacity counts working days in the period. Someone is overloaded when the remaining estimate of their open issues is more than
            one week of their capacity.
          </p>
        </>
      )}
    </section>
  )
}

function MemberRow({
  member,
  role,
  showProjects,
  editCapacity,
}: {
  member: MemberWorkload
  role: string
  showProjects: boolean
  editCapacity?: (member: MemberWorkload) => void
}) {
  const percent = utilization(member)
  return (
    <tr data-overloaded={member.overloaded || undefined}>
      <th scope="row">
        <span className="member-cell">
          <Avatar name={member.username} size="small" />
          <span>
            <strong>{member.username}</strong>
            <span className="muted small-text">{role}</span>
          </span>
        </span>
        {showProjects && member.projects.length > 0 && (
          <span className="project-split">
            {member.projects.map(project => (
              <span key={project.projectId} className="chip">
                {project.projectTitle} · {formatHours(project.minutes)}
              </span>
            ))}
          </span>
        )}
      </th>
      <td>
        <div className="capacity-cell">
          <span className="capacity-bar" data-level={percent > 100 ? 'over' : percent >= 80 ? 'full' : undefined}>
            <span style={{ width: `${Math.min(percent, 100)}%` }} />
          </span>
          <span className="small-text">
            {formatHours(member.loggedMinutes)} / {formatHours(member.capacityMinutes)} · {percent}%
          </span>
          {editCapacity ? (
            <button type="button" className="text-button small-text" title="Change weekly capacity" onClick={() => editCapacity(member)}>
              {formatHours(member.weeklyCapacityMinutes)}/wk
              <Pencil size={12} />
            </button>
          ) : (
            <span className="muted small-text">{formatHours(member.weeklyCapacityMinutes)}/wk</span>
          )}
        </div>
      </td>
      <td>
        <span className="heat-row">
          {member.days.map(day => (
            <span
              key={day.date}
              className="heat-cell"
              data-level={heatLevel(day.minutes)}
              data-weekend={isWeekend(day.date) || undefined}
              title={`${formatDay(day.date, { weekday: 'short', month: 'short', day: 'numeric' })}: ${formatDuration(day.minutes)}`}
            />
          ))}
        </span>
      </td>
      <td className="numeric">
        {member.openIssues}
        {member.unestimatedIssues > 0 && (
          <span className="muted small-text" title="Open issues without an estimate">
            {' '}
            ({member.unestimatedIssues} no est.)
          </span>
        )}
      </td>
      <td className="numeric">
        <span className="remaining-cell">
          {member.overloaded && <AlertTriangle size={14} aria-label="Overloaded" />}
          {formatHours(member.remainingEstimateMinutes)}
        </span>
      </td>
    </tr>
  )
}

export function ProjectWorkload({ project }: { project: Project }) {
  const load = useCallback((from: string, to: string, signal: AbortSignal) => api.projectWorkload(project, from, to, signal), [project])
  return (
    <WorkloadView
      name={project.title}
      description="Time logged in this project and the open work assigned to each member."
      load={load}
      roles={projectRoles}
      showProjects={false}
    />
  )
}

export function OrganizationWorkload({ organization }: { organization: Organization }) {
  const [revision, setRevision] = useState(0)
  const [editing, setEditing] = useState<MemberWorkload>()
  const load = useCallback(
    (from: string, to: string, signal: AbortSignal) => api.organizationWorkload(organization.id, from, to, signal),
    [organization.id],
  )
  if (organization.role === 'USER')
    return <Empty title="Only organization owners and admins can see the workload of the team" icon={<Gauge size={22} />} />
  return (
    <>
      <WorkloadView
        name={organization.title}
        description="Time logged across all projects and the open work assigned to each person."
        load={load}
        showProjects
        roles={organizationRoles}
        page
        version={revision}
        editCapacity={setEditing}
      />
      {editing && (
        <CapacityDialog
          organization={organization}
          member={editing}
          close={() => setEditing(undefined)}
          saved={() => {
            setEditing(undefined)
            setRevision(value => value + 1)
          }}
        />
      )}
    </>
  )
}

function CapacityDialog({
  organization,
  member,
  close,
  saved,
}: {
  organization: Organization
  member: MemberWorkload
  close: () => void
  saved: () => void
}) {
  const action = useAction()
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const hours = Number(new FormData(event.currentTarget).get('hours'))
    void action.run(async () => {
      await api.updateCapacity(organization.id, member.organizationMemberId, Math.round(hours * 60))
      toast(`Capacity of ${member.username} updated`)
      saved()
    })
  }
  return (
    <Modal
      title={`Weekly capacity of ${member.username}`}
      close={() => {
        if (!action.busy) close()
      }}>
      <form className="form" onSubmit={submit}>
        <label>
          Hours per week
          <input
            type="number"
            name="hours"
            min={0}
            max={168}
            step={0.5}
            required
            autoFocus
            defaultValue={member.weeklyCapacityMinutes / 60}
          />
        </label>
        <p className="muted small-text">Use 40 for full time, 20 for half time, or 0 for someone who does not take issues.</p>
        <ErrorNotice text={action.error} />
        <div className="row">
          <button className="button primary" disabled={action.busy}>
            {action.busy ? 'Saving…' : 'Save'}
          </button>
          <button type="button" className="button" disabled={action.busy} onClick={close}>
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  )
}
