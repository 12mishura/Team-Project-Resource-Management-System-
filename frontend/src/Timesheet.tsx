import { CalendarClock, Clock, Download, Plus } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { api, type TimesheetEntry } from './api'
import { statuses, types } from './labels'
import { LogTimeDialog, LogTimeForAnyIssue } from './TimeTracking'
import { datesBetween, downloadCsv, formatDay, formatDuration, formatHours, isWeekend, today } from './time'
import { PeriodControls } from './Workload'
import { usePeriod, useResource } from './hooks'
import { Empty, ErrorNotice, Loading } from './ui'

type Row = {
  key: string
  entry: TimesheetEntry
  byDay: Map<string, TimesheetEntry[]>
  total: number
}

export type TimesheetIssue = { organizationId: string; projectId: string; issueId: string }

function buildRows(entries: TimesheetEntry[]): Row[] {
  const rows = new Map<string, Row>()
  for (const entry of entries) {
    const row = rows.get(entry.issueId) ?? { key: entry.issueId, entry, byDay: new Map(), total: 0 }
    row.byDay.set(entry.workDate, [...(row.byDay.get(entry.workDate) ?? []), entry])
    row.total += entry.minutes
    rows.set(entry.issueId, row)
  }
  return [...rows.values()].sort(
    (a, b) =>
      a.entry.organizationTitle.localeCompare(b.entry.organizationTitle) ||
      a.entry.projectTitle.localeCompare(b.entry.projectTitle) ||
      b.total - a.total,
  )
}

function cellTitle(entries: TimesheetEntry[]) {
  return entries.map(entry => `${formatDuration(entry.minutes)}${entry.comment ? ` — ${entry.comment}` : ''}`).join('\n')
}

export function Timesheet({ openIssue }: { openIssue: (issue: TimesheetIssue) => void }) {
  const period = usePeriod('week')
  const { from, to } = period
  const data = useResource(useCallback((signal: AbortSignal) => api.timesheet(from, to, signal), [from, to]))
  const [logging, setLogging] = useState<TimesheetEntry>()
  const [loggingAny, setLoggingAny] = useState(false)
  const days = useMemo(() => datesBetween(from, to), [from, to])
  const entries = useMemo(() => data.value ?? [], [data.value])
  const rows = useMemo(() => buildRows(entries), [entries])
  const dayTotals = days.map(day => entries.filter(entry => entry.workDate === day).reduce((sum, entry) => sum + entry.minutes, 0))
  const total = dayTotals.reduce((sum, value) => sum + value, 0)
  const workDays = days.filter(day => !isWeekend(day) && day <= today()).length
  const loggedDays = dayTotals.filter(Boolean).length

  function exportEntries() {
    downloadCsv(`timesheet-${from}-${to}.csv`, [
      ['Date', 'Organization', 'Project', 'Issue', 'Type', 'Hours', 'Comment'],
      ...entries.map(entry => [
        entry.workDate,
        entry.organizationTitle,
        entry.projectTitle,
        entry.issueTitle,
        types[entry.issueType],
        (entry.minutes / 60).toFixed(2),
        entry.comment ?? '',
      ]),
    ])
  }

  return (
    <>
      <header className="page-heading">
        <div>
          <h1>Timesheet</h1>
          <p>The time you logged, issue by issue</p>
        </div>
        <div className="row">
          <PeriodControls period={period} spans={false} />
          <button type="button" className="button" disabled={!entries.length} onClick={exportEntries}>
            <Download size={15} />
            Export CSV
          </button>
          <button type="button" className="button primary" onClick={() => setLoggingAny(true)}>
            <Clock size={15} />
            Log time
          </button>
        </div>
      </header>
      <section className="time-stats" aria-label="Week summary">
        <div>
          <strong>{formatHours(total)}</strong>
          <span>Logged this {period.isCurrent ? 'week so far' : 'week'}</span>
        </div>
        <div>
          <strong>{workDays ? formatHours(Math.round(total / workDays)) : '0h'}</strong>
          <span>Per working day</span>
        </div>
        <div>
          <strong>
            {loggedDays} / {days.length}
          </strong>
          <span>Days with time</span>
        </div>
        <div>
          <strong>{rows.length}</strong>
          <span>{rows.length === 1 ? 'Issue' : 'Issues'}</span>
        </div>
      </section>
      <ErrorNotice text={data.error} retry={data.retry} />
      {data.loading && !data.value && <Loading />}
      {data.value && rows.length === 0 && (
        <Empty title="No time logged in this week" icon={<CalendarClock size={22} />}>
          Press Log time above and pick one of your issues, or open any issue in your project and press Log time there.
        </Empty>
      )}
      {rows.length > 0 && (
        <div className="table-scroll">
          <table className="timesheet">
            <thead>
              <tr>
                <th scope="col">Issue</th>
                {days.map(day => (
                  <th
                    key={day}
                    scope="col"
                    className="numeric"
                    data-weekend={isWeekend(day) || undefined}
                    data-today={day === today() || undefined}>
                    {formatDay(day)}
                  </th>
                ))}
                <th scope="col" className="numeric">
                  Total
                </th>
                <th scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map(row => (
                <tr key={row.key}>
                  <th scope="row">
                    <button
                      type="button"
                      className="row-link timesheet-issue"
                      onClick={() =>
                        openIssue({ organizationId: row.entry.organizationId, projectId: row.entry.projectId, issueId: row.entry.issueId })
                      }>
                      <span className="badge issue-type" data-type={row.entry.issueType}>
                        {types[row.entry.issueType]}
                      </span>
                      <span>
                        <strong className="issue-title">{row.entry.issueTitle}</strong>
                        <span className="muted small-text">
                          {row.entry.projectTitle} · {row.entry.organizationTitle} · {statuses[row.entry.issueStatus]}
                        </span>
                      </span>
                    </button>
                  </th>
                  {days.map(day => {
                    const items = row.byDay.get(day) ?? []
                    const minutes = items.reduce((sum, item) => sum + item.minutes, 0)
                    return (
                      <td
                        key={day}
                        className="numeric"
                        data-weekend={isWeekend(day) || undefined}
                        data-filled={minutes > 0 || undefined}
                        title={items.length ? cellTitle(items) : undefined}>
                        {minutes ? formatDuration(minutes) : ''}
                      </td>
                    )
                  })}
                  <td className="numeric">
                    <strong>{formatDuration(row.total)}</strong>
                  </td>
                  <td>
                    {row.entry.issueStatus !== 'DONE' && (
                      <button
                        type="button"
                        className="icon-button small"
                        aria-label={`Log time on ${row.entry.issueTitle}`}
                        title="Log time"
                        onClick={() => setLogging(row.entry)}>
                        <Plus size={14} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <th scope="row">Total</th>
                {dayTotals.map((minutes, index) => (
                  <td
                    key={days[index]}
                    className="numeric"
                    data-weekend={isWeekend(days[index]) || undefined}
                    data-over={minutes > 8 * 60 || undefined}>
                    {minutes ? formatDuration(minutes) : '—'}
                  </td>
                ))}
                <td className="numeric">
                  <strong>{formatDuration(total)}</strong>
                </td>
                <td />
              </tr>
            </tfoot>
          </table>
        </div>
      )}
      {loggingAny && (
        <LogTimeForAnyIssue
          close={() => setLoggingAny(false)}
          saved={() => {
            setLoggingAny(false)
            data.reload()
          }}
        />
      )}
      {logging && (
        <LogTimeDialog
          issue={{ organizationId: logging.organizationId, projectId: logging.projectId, id: logging.issueId, title: logging.issueTitle }}
          close={() => setLogging(undefined)}
          saved={() => {
            setLogging(undefined)
            data.reload()
          }}
        />
      )}
    </>
  )
}
