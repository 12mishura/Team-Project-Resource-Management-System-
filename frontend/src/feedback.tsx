import { CircleCheck, X } from 'lucide-react'
import { useState, useSyncExternalStore, type FormEvent } from 'react'
import { currentConfirm, currentToasts, dismissToast, settle, subscribe, type PendingConfirm } from './feedbackStore'
import { Modal } from './ui'

export function FeedbackHost() {
  const confirm = useSyncExternalStore(subscribe, currentConfirm)
  const visibleToasts = useSyncExternalStore(subscribe, currentToasts)
  return (
    <>
      {confirm && <ConfirmDialog key={confirm.title + confirm.message} options={confirm} />}
      <div className="toasts" role="status" aria-live="polite">
        {visibleToasts.map(item => (
          <div className="toast" key={item.id}>
            <CircleCheck size={17} aria-hidden="true" />
            <span>{item.text}</span>
            <button type="button" className="toast-close" aria-label="Dismiss" onClick={() => dismissToast(item.id)}>
              <X size={15} />
            </button>
          </div>
        ))}
      </div>
    </>
  )
}

function ConfirmDialog({ options }: { options: PendingConfirm }) {
  const [typed, setTyped] = useState('')
  const matches = !options.requireText || typed.trim() === options.requireText
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (matches) settle(true)
  }

  return (
    <Modal title={options.title} close={() => settle(false)}>
      <form className="form" onSubmit={submit}>
        <p className="confirm-message">{options.message}</p>
        {options.requireText && (
          <label>
            Type <strong className="confirm-target">{options.requireText}</strong> to confirm
            <input autoFocus value={typed} onChange={event => setTyped(event.target.value)} autoComplete="off" />
          </label>
        )}
        <div className="row">
          <button className={`button ${options.danger ? 'danger-solid' : 'primary'}`} disabled={!matches} autoFocus={!options.requireText}>
            {options.confirmLabel}
          </button>
          <button type="button" className="button" onClick={() => settle(false)}>
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  )
}
