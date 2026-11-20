import { AlertCircle, ChevronLeft, ChevronRight, Layers3, LoaderCircle, X } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { ApiError, type Page } from './api'

export const message = (error: unknown) =>
  error instanceof ApiError ? error.message : 'Could not reach the server. Please try again.'

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
    load(operation.signal).then(data => {
      if (!operation.signal.aborted) setValue(data)
    }).catch(reason => {
      if (!operation.signal.aborted) {
        setValue(undefined)
        setError(message(reason))
        setRetryable(!(reason instanceof ApiError) || reason.status >= 500 || reason.status === 408 || reason.status === 429)
      }
    }).finally(() => {
      if (!operation.signal.aborted) setLoading(false)
    })
    return () => operation.abort()
  }, [load, revision])

  const reload = () => setRevision(version => version + 1)
  return { value, error, loading, reload, retry: retryable ? reload : undefined, setValue }
}

export function Brand() {
  return <div className="brand"><span className="brand-mark"><Layers3 size={19} /></span>TaskTracker</div>
}

export function ErrorNotice({ text, retry }: { text: string; retry?: () => void }) {
  return text && <div className="notice error" role="alert"><AlertCircle size={17} />
    <span>{text}</span>{retry && <button className="button" onClick={retry}>Try again</button>}
  </div>
}

export function Loading() {
  return <div className="loading" role="status"><LoaderCircle size={19} /> Loading…</div>
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return <div className="empty"><h2>{title}</h2>{children && <p>{children}</p>}</div>
}

export function Pagination({ data, onChange, disabled = false }: { data?: Page<unknown>; onChange: (page: number) => void; disabled?: boolean }) {
  if (!data || data.totalPages <= 1) return null
  return <nav className="pagination" aria-label="Pagination">
    <button className="icon-button" aria-label="Previous page" disabled={disabled || data.page === 0} onClick={() => onChange(data.page - 1)}><ChevronLeft size={17} /></button>
    <span>{data.page + 1} / {data.totalPages}</span>
    <button className="icon-button" aria-label="Next page" disabled={disabled || data.page + 1 >= data.totalPages} onClick={() => onChange(data.page + 1)}><ChevronRight size={17} /></button>
  </nav>
}

export function Modal({ title, close, children }: { title: string; close: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    element.showModal()
    return () => element.close()
  }, [])
  return <dialog ref={dialog} aria-label={title} onCancel={event => { event.preventDefault(); close() }}>
    <div className="modal-heading"><h2>{title}</h2><button className="icon-button" type="button" onClick={close} aria-label="Close"><X size={20} /></button></div>
    {children}
  </dialog>
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
