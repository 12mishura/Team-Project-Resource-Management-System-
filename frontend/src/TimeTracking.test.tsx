import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, type Issue } from './api'
import { LogTimeDialog, TimeSummary } from './TimeTracking'
import { today } from './time'

const issue = (overrides: Partial<Issue>): Issue => ({
  id: 'issue',
  title: 'Build report',
  description: null,
  createdAt: '2026-11-20T10:00:00Z',
  updatedAt: '2026-11-20T10:00:00Z',
  organizationId: 'org',
  projectId: 'project',
  type: 'TASK',
  status: 'IN_PROGRESS',
  priority: 'MEDIUM',
  assigneeUsername: null,
  assigneeId: null,
  parentId: null,
  reporterId: 'user',
  reporterUsername: 'demo',
  severity: null,
  stepsToReproduce: null,
  storyPoints: null,
  estimateMinutes: 480,
  spentMinutes: 90,
  remainingMinutes: 390,
  ...overrides,
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('TimeSummary', () => {
  it('shows logged, estimate and remaining time', () => {
    render(<TimeSummary issue={issue({})} canLog onLog={() => {}} />)
    expect(screen.getByText('Logged').nextSibling).toHaveTextContent('1h 30m')
    expect(screen.getByText('Estimate').nextSibling).toHaveTextContent('8h')
    expect(screen.getByText('Remaining').nextSibling).toHaveTextContent('6h 30m')
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '19')
  })

  it('warns when the estimate is exceeded', () => {
    render(<TimeSummary issue={issue({ estimateMinutes: 60, spentMinutes: 100, remainingMinutes: 0 })} canLog={false} onLog={() => {}} />)
    expect(screen.getByText('Over estimate').nextSibling).toHaveTextContent('40m')
    expect(screen.queryByRole('button', { name: 'Log time' })).not.toBeInTheDocument()
  })

  it('shows when there is no estimate', () => {
    render(<TimeSummary issue={issue({ estimateMinutes: null, remainingMinutes: null })} canLog onLog={() => {}} />)
    expect(screen.getByText('Estimate').nextSibling).toHaveTextContent('Not set')
  })
})

describe('LogTimeDialog', () => {
  it('sends the parsed duration with today as the default date', async () => {
    const logWork = vi.spyOn(api, 'logWork').mockResolvedValue({} as never)
    const saved = vi.fn()
    render(
      <LogTimeDialog
        issue={{ organizationId: 'org', projectId: 'project', id: 'issue', title: 'Build report' }}
        close={() => {}}
        saved={saved}
      />,
    )

    await userEvent.type(screen.getByLabelText('Time spent'), '1h 15m')
    await userEvent.type(screen.getByLabelText('What did you work on?'), 'Charts')
    await userEvent.click(screen.getByRole('button', { name: 'Log time' }))

    expect(logWork).toHaveBeenCalledWith(expect.objectContaining({ id: 'issue' }), {
      minutes: 75,
      workDate: today(),
      comment: 'Charts',
    })
    expect(saved).toHaveBeenCalled()
  })

  it('blocks durations it cannot read', async () => {
    const logWork = vi.spyOn(api, 'logWork')
    render(
      <LogTimeDialog
        issue={{ organizationId: 'org', projectId: 'project', id: 'issue', title: 'Build report' }}
        close={() => {}}
        saved={() => {}}
      />,
    )

    const input = screen.getByLabelText('Time spent')
    await userEvent.type(input, 'soon')
    await userEvent.click(screen.getByRole('button', { name: 'Log time' }))

    expect(input).toBeInvalid()
    expect(logWork).not.toHaveBeenCalled()
  })
})

describe('LogTimeDialog without a fixed issue', () => {
  it('logs time on the issue picked from the list', async () => {
    const logWork = vi.spyOn(api, 'logWork').mockResolvedValue({} as never)
    render(
      <LogTimeDialog
        choices={[issue({ id: 'a', title: 'First' }), issue({ id: 'b', title: 'Second' })]}
        close={() => {}}
        saved={() => {}}
      />,
    )

    const submit = screen.getByRole('button', { name: 'Log time' })
    expect(submit).toBeDisabled()
    await userEvent.click(screen.getByRole('combobox', { name: 'Issue' }))
    await userEvent.click(screen.getByRole('option', { name: 'Task · Second' }))
    await userEvent.type(screen.getByLabelText('Time spent'), '30m')
    await userEvent.click(submit)

    expect(logWork).toHaveBeenCalledWith(expect.objectContaining({ id: 'b' }), expect.objectContaining({ minutes: 30 }))
  })

  it('explains when nothing is assigned', () => {
    render(<LogTimeDialog choices={[]} close={() => {}} saved={() => {}} />)
    expect(screen.getByRole('combobox', { name: 'Issue' })).toHaveTextContent('No open issues are assigned to you')
  })
})

describe('TimeSummary hints', () => {
  it('offers to join the project when the viewer manages it but is not a member', async () => {
    const onJoin = vi.fn()
    render(<TimeSummary issue={issue({})} canLog={false} onLog={() => {}} onJoin={onJoin} />)
    await userEvent.click(screen.getByRole('button', { name: 'Add yourself on Members' }))
    expect(onJoin).toHaveBeenCalled()
  })
})
