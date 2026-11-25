import { useEffect, useRef, useState } from 'react'
import { ApiError } from './api'
import { addDays, startOfWeek, today } from './time'

export const message = (error: unknown) => (error instanceof ApiError ? error.message : 'Could not reach the server. Please try again.')

export function useResource<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [value, setValue] = useState<T>()
  const [error, setError] = useState('')
  const [retryable, setRetryable] = useState(true)
  const [loading, setLoading] = useState(true)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const operation = new AbortController()
    setLoading(true)
    setError('')
    load(operation.signal)
      .then(data => {
        if (!operation.signal.aborted) setValue(data)
      })
      .catch(reason => {
        if (!operation.signal.aborted) {
          setValue(undefined)
          setError(message(reason))
          setRetryable(!(reason instanceof ApiError) || reason.status >= 500 || reason.status === 408 || reason.status === 429)
        }
      })
      .finally(() => {
        if (!operation.signal.aborted) setLoading(false)
      })
    return () => operation.abort()
  }, [load, revision])

  const reload = () => setRevision(version => version + 1)
  return { value, error, loading, reload, retry: retryable ? reload : undefined, setValue }
}

export function useAction() {
  const active = useRef(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function run(action: () => Promise<void>) {
    if (active.current) return
    active.current = true
    setBusy(true)
    setError('')
    try {
      await action()
    } catch (reason) {
      setError(message(reason))
    } finally {
      active.current = false
      setBusy(false)
    }
  }
  return { run, busy, error }
}

export type Span = 'week' | 'month'
const spanDays: Record<Span, number> = { week: 7, month: 28 }

export function usePeriod(initialSpan: Span = 'week') {
  const [span, setSpan] = useState<Span>(initialSpan)
  const [anchor, setAnchor] = useState(() => startOfWeek(today()))
  const length = spanDays[span]
  const from = span === 'week' ? anchor : addDays(anchor, -21)
  const to = addDays(from, length - 1)
  return {
    span,
    from,
    to,
    setSpan,
    previous: () => setAnchor(current => addDays(current, -length)),
    next: () =>
      setAnchor(current => {
        const latest = startOfWeek(today())
        const moved = addDays(current, length)
        return moved > latest ? latest : moved
      }),
    reset: () => setAnchor(startOfWeek(today())),
    isCurrent: anchor >= startOfWeek(today()),
  }
}

export type Period = ReturnType<typeof usePeriod>
