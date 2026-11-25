import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'
import { FeedbackHost } from './feedback'
import { confirmAction } from './feedbackStore'
import { Select } from './Select'

const options = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
]

function ControlledSelect({ onChange }: { onChange: (value: string) => void }) {
  const [value, setValue] = useState('MEDIUM')
  return (
    <label>
      Priority
      <Select
        value={value}
        options={options}
        onChange={next => {
          setValue(next)
          onChange(next)
        }}
      />
    </label>
  )
}

describe('Select', () => {
  it('opens on click, picks an option and closes', async () => {
    const onChange = vi.fn()
    render(<ControlledSelect onChange={onChange} />)
    const trigger = screen.getByRole('combobox', { name: 'Priority' })
    expect(trigger).toHaveTextContent('Medium')

    await userEvent.click(trigger)
    await userEvent.click(screen.getByRole('option', { name: 'High' }))

    expect(onChange).toHaveBeenCalledWith('HIGH')
    expect(trigger).toHaveTextContent('High')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('works with the keyboard', async () => {
    const onChange = vi.fn()
    render(<ControlledSelect onChange={onChange} />)
    const trigger = screen.getByRole('combobox', { name: 'Priority' })
    trigger.focus()

    await userEvent.keyboard('{ArrowDown}')
    expect(screen.getByRole('listbox')).toBeInTheDocument()
    await userEvent.keyboard('{ArrowUp}{Enter}')
    expect(onChange).toHaveBeenCalledWith('LOW')

    await userEvent.keyboard('{ArrowDown}{Escape}')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('submits its value through a hidden input', async () => {
    const submitted = vi.fn()
    render(
      <form
        onSubmit={event => {
          event.preventDefault()
          submitted(new FormData(event.currentTarget).get('priority'))
        }}>
        <label>
          Priority
          <Select name="priority" defaultValue="LOW" options={options} />
        </label>
        <button>Save</button>
      </form>,
    )
    await userEvent.click(screen.getByRole('combobox', { name: 'Priority' }))
    await userEvent.click(screen.getByRole('option', { name: 'High' }))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(submitted).toHaveBeenCalledWith('HIGH')
  })

  it('does not open when disabled', async () => {
    render(<Select aria-label="Status" value="LOW" options={options} disabled />)
    await userEvent.click(screen.getByRole('combobox', { name: 'Status' }))
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })
})

function Broken(): never {
  throw new Error('boom')
}

describe('ErrorBoundary', () => {
  it('shows a recoverable screen instead of crashing the page', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const onHome = vi.fn()
    render(
      <ErrorBoundary scope="page" onHome={onHome}>
        <Broken />
      </ErrorBoundary>,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('This page ran into a problem')
    await userEvent.click(screen.getByRole('button', { name: 'Go home' }))
    expect(onHome).toHaveBeenCalled()
  })

  it('renders children when nothing fails', () => {
    render(
      <ErrorBoundary scope="app">
        <p>All good</p>
      </ErrorBoundary>,
    )
    expect(screen.getByText('All good')).toBeInTheDocument()
  })
})

describe('confirm dialog', () => {
  it('resolves true only after the required text is typed', async () => {
    render(<FeedbackHost />)
    let answer: Promise<boolean> = Promise.resolve(false)
    act(() => {
      answer = confirmAction({
        title: 'Delete project?',
        message: 'Gone forever',
        confirmLabel: 'Delete',
        danger: true,
        requireText: 'Demo',
      })
    })
    const confirm = await screen.findByRole('button', { name: 'Delete' })
    expect(confirm).toBeDisabled()

    await userEvent.type(screen.getByRole('textbox'), 'Demo')
    expect(confirm).toBeEnabled()
    await userEvent.click(confirm)
    await expect(answer).resolves.toBe(true)
  })

  it('resolves false on cancel', async () => {
    render(<FeedbackHost />)
    let answer: Promise<boolean> = Promise.resolve(true)
    act(() => {
      answer = confirmAction({ title: 'Leave?', message: 'Sure?', confirmLabel: 'Leave' })
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }))
    await expect(answer).resolves.toBe(false)
  })
})
