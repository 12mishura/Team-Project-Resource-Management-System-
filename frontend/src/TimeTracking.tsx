import { Clock, Pencil, Plus, Trash2, UserPlus } from 'lucide-react'
import { useCallback, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { api, type Issue, type IssueRef, type WorkLog } from './api'
import { Avatar } from './bits'
import { confirmAction, toast } from './feedbackStore'
import { useAction, useResource } from './hooks'
import { formatDate, types } from './labels'
import { Select } from './Select'
import { MAX_ESTIMATE_MINUTES, MAX_LOG_MINUTES, formatDay, formatDuration, parseDuration, progress, readDuration, today } from './time'
import { ErrorNotice, Loading, Modal } from './ui'

const durationHint = 'Use a duration like 2h 30m, 1.5h, 45m or 1d'

export function DurationInput({
  name,
  defaultValue,
  required,
  max,
  autoFocus,
  placeholder = 'e.g. 2h 30m',
}: {
  name: string
  defaultValue?: string
  required?: boolean
  max: number
  autoFocus?: boolean
  placeholder?: string
}) {
  function validate(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget
    const text = input.value.trim()
    const minutes = parseDuration(text)
    if (!text) input.setCustomValidity('')
    else if (minutes === null) input.setCustomValidity(durationHint)
    else if (minutes > max) input.setCustomValidity(`The maximum is ${formatDuration(max)}`)
    else input.setCustomValidity('')
  }
  return (
    <input
      name={name}
      defaultValue={defaultValue}
      required={required}
      autoFocus={autoFocus}
      placeholder={placeholder}
      autoComplete="off"
      title={durationHint}
      onChange={validate}
    />
  )
}

export function EstimateField({ issue }: { issue?: Issue }) {
  return (
    <label>
      Estimate
      <DurationInput
        name="estimate"
        defaultValue={issue?.estimateMinutes ? formatDuration(issue.estimateMinutes) : ''}
        max={MAX_ESTIMATE_MINUTES}
      />
    </label>
  )
}

export function TimeSummary({ issue, canLog, onLog, onJoin }: { issue: Issue; canLog: boolean; onLog: () => void; onJoin?: () => void }) {
  const estimate = issue.estimateMinutes
  const over = estimate !== null && issue.spentMinutes > estimate
  const percent = progress(issue.spentMinutes, estimate)
  return (
    <section className="time-summary" aria-label="Time tracking" data-over={over || undefined}>
      <header>
        <h3>
          <Clock size={15} aria-hidden="true" />
          Time
        </h3>
        {canLog && (
          <button type="button" className="button small" onClick={onLog}>
            <Plus size={14} />
            Log time
          </button>
        )}
      </header>
      <div
        className="time-bar"
        role="progressbar"
        aria-label="Time spent against the estimate"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}>
        <span style={{ width: `${estimate ? percent : issue.spentMinutes ? 100 : 0}%` }} data-unestimated={!estimate || undefined} />
      </div>
      <dl className="time-numbers">
        <div>
          <dt>Logged</dt>
          <dd>{formatDuration(issue.spentMinutes)}</dd>
        </div>
        <div>
          <dt>{issue.type === 'EPIC' ? 'Estimate of issues' : 'Estimate'}</dt>
          <dd>{formatDuration(estimate, 'Not set')}</dd>
        </div>
        <div>
          <dt>{over ? 'Over estimate' : 'Remaining'}</dt>
          <dd>{over ? formatDuration(issue.spentMinutes - (estimate ?? 0)) : formatDuration(issue.remainingMinutes)}</dd>
        </div>
      </dl>
      {canLog && issue.spentMinutes === 0 && (
        <p className="time-note">Worked on this issue? Press Log time and enter how long, for example 1h 30m.</p>
      )}
      {!canLog && onJoin && (
        <p className="time-note">
          Only project members can log time, and you are not a member of this project yet.
          <button type="button" className="button small" onClick={onJoin}>
            <UserPlus size={14} />
            Add yourself on Members
          </button>
        </p>
      )}
      {issue.type === 'EPIC' && <p className="time-note">Time is logged on the issues of this epic and adds up here.</p>}
    </section>
  )
}

type LoggableIssue = IssueRef & { title: string }

export function LogTimeDialog({
  issue: fixedIssue,
  choices,
  workLog,
  close,
  saved,
}: {
  issue?: LoggableIssue
  choices?: Issue[]
  workLog?: WorkLog
  close: () => void
  saved: () => void
}) {
  const action = useAction()
  const [pickedId, setPickedId] = useState('')
  const issue: LoggableIssue | undefined = fixedIssue ?? choices?.find(choice => choice.id === pickedId)
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const minutes = readDuration(data, 'duration')
    if (!minutes || !issue) return
    const body = { minutes, workDate: String(data.get('workDate')), comment: String(data.get('comment') ?? '').trim() }
    void action.run(async () => {
      if (workLog) await api.updateWorkLog(issue, workLog, body)
      else await api.logWork(issue, { ...body, comment: body.comment || undefined })
      toast(workLog ? 'Work log updated' : `Logged ${formatDuration(minutes)}`)
      saved()
    })
  }
  return (
    <Modal
      title={workLog ? 'Edit work log' : 'Log time'}
      close={() => {
        if (!action.busy) close()
      }}>
      <form className="form" onSubmit={submit}>
        {fixedIssue ? (
          <p className="muted small-text">{fixedIssue.title}</p>
        ) : (
          <label>
            Issue
            <Select
              value={pickedId}
              onChange={setPickedId}
              disabled={!choices?.length}
              placeholder={choices?.length ? 'Choose an issue…' : 'No open issues are assigned to you'}
              options={(choices ?? []).map(choice => ({ value: choice.id, label: `${types[choice.type]} · ${choice.title}` }))}
            />
          </label>
        )}
        <div className="field-grid">
          <label>
            Time spent
            <DurationInput
              name="duration"
              required
              autoFocus
              max={MAX_LOG_MINUTES}
              defaultValue={workLog ? formatDuration(workLog.minutes) : ''}
            />
          </label>
          <label>
            Date
            <input type="date" name="workDate" required max={today()} defaultValue={workLog?.workDate ?? today()} />
          </label>
        </div>
        <label>
          What did you work on?
          <textarea name="comment" rows={3} maxLength={500} defaultValue={workLog?.comment ?? ''} placeholder="Optional" />
        </label>
        <p className="muted small-text">{durationHint}. A work day is 8 hours.</p>
        <ErrorNotice text={action.error} />
        <div className="row">
          <button className="button primary" disabled={action.busy || !issue}>
            {action.busy ? 'Saving…' : workLog ? 'Save changes' : 'Log time'}
          </button>
          <button type="button" className="button" disabled={action.busy} onClick={close}>
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  )
}

export function WorkLogList({
  issue,
  mutable,
  userId,
  admin,
  changed,
}: {
  issue: IssueRef & { title: string }
  mutable: boolean
  userId?: string
  admin: boolean
  changed: () => void
}) {
  const load = useCallback((signal: AbortSignal) => api.workLogs(issue, signal), [issue])
  const logs = useResource(load)
  const [editing, setEditing] = useState<WorkLog>()
  const action = useAction()

  async function remove(workLog: WorkLog) {
    const confirmed = await confirmAction({
      title: 'Delete work log?',
      message: `${formatDuration(workLog.minutes)} logged by ${workLog.authorUsername} on ${formatDay(workLog.workDate, { dateStyle: 'medium' })} will be removed.`,
      confirmLabel: 'Delete',
      danger: true,
    })
    if (!confirmed) return
    void action.run(async () => {
      await api.deleteWorkLog(issue, workLog)
      logs.reload()
      changed()
      toast('Work log deleted')
    })
  }

  const total = logs.value?.reduce((sum, item) => sum + item.minutes, 0) ?? 0
  return (
    <section className="activity work-logs">
      <ErrorNotice text={logs.error} retry={logs.retry} />
      <ErrorNotice text={action.error} />
      {logs.loading && !logs.value ? (
        <Loading />
      ) : logs.value?.length === 0 ? (
        <p className="muted">No time logged yet</p>
      ) : (
        <>
          <p className="muted small-text">
            {logs.value?.length} {logs.value?.length === 1 ? 'entry' : 'entries'} · {formatDuration(total)} in total
          </p>
          {logs.value?.map(item => {
            const own = item.authorId === userId
            return (
              <article key={item.id}>
                <header>
                  <span className="comment-author">
                    <Avatar name={item.authorUsername} size="tiny" />
                    <strong>{item.authorUsername}</strong>
                    <span className="chip work-minutes">{formatDuration(item.minutes)}</span>
                  </span>
                  <span className="row comment-meta">
                    <time dateTime={item.workDate} title={`Logged ${formatDate(item.createdAt)}`}>
                      {formatDay(item.workDate, { weekday: 'short', month: 'short', day: 'numeric' })}
                    </time>
                    {mutable && (own || admin) && (
                      <>
                        <button
                          type="button"
                          className="icon-button small"
                          aria-label="Edit work log"
                          title="Edit"
                          disabled={action.busy}
                          onClick={() => setEditing(item)}>
                          <Pencil size={14} />
                        </button>
                        <button
                          type="button"
                          className="icon-button small"
                          aria-label="Delete work log"
                          title="Delete"
                          disabled={action.busy}
                          onClick={() => void remove(item)}>
                          <Trash2 size={14} />
                        </button>
                      </>
                    )}
                  </span>
                </header>
                {item.comment && <p>{item.comment}</p>}
              </article>
            )
          })}
        </>
      )}
      {editing && (
        <LogTimeDialog
          issue={issue}
          workLog={editing}
          close={() => setEditing(undefined)}
          saved={() => {
            setEditing(undefined)
            logs.reload()
            changed()
          }}
        />
      )}
    </section>
  )
}

export function LogTimeForAnyIssue({ close, saved }: { close: () => void; saved: () => void }) {
  const mine = useResource(useCallback((signal: AbortSignal) => api.myIssues(signal), []))
  const choices = useMemo(() => (mine.value ?? []).filter(item => item.status !== 'DONE' && item.type !== 'EPIC'), [mine.value])
  if (mine.loading && !mine.value) return null
  return <LogTimeDialog choices={choices} close={close} saved={saved} />
}
