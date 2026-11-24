import { AlertCircle, ChevronDown, ChevronLeft, ChevronRight, Layers3, LoaderCircle, X } from 'lucide-react'
import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import type { Page } from './api'

export function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark">
        <Layers3 size={19} />
      </span>
      TaskTracker
    </div>
  )
}

export function ErrorNotice({ text, retry }: { text: string; retry?: () => void }) {
  return (
    text && (
      <div className="notice error" role="alert">
        <AlertCircle size={17} />
        <span>{text}</span>
        {retry && (
          <button className="button" onClick={retry}>
            Try again
          </button>
        )}
      </div>
    )
  )
}

export function Loading() {
  return (
    <div className="loading" role="status">
      <LoaderCircle size={19} /> Loading…
    </div>
  )
}

export function Empty({ title, children, icon, action }: { title: string; children?: ReactNode; icon?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty" data-rich={icon ? true : undefined}>
      {icon && (
        <span className="empty-icon" aria-hidden="true">
          {icon}
        </span>
      )}
      <h2>{title}</h2>
      {children && <p>{children}</p>}
      {action && <div className="empty-action">{action}</div>}
    </div>
  )
}

export type MenuEntry = 'separator' | { label: string; icon?: ReactNode; onSelect: () => void; danger?: boolean; disabled?: boolean }

export function Menu({
  label,
  text,
  icon,
  items,
  disabled = false,
}: {
  label: string
  text?: string
  icon?: ReactNode
  items: MenuEntry[]
  disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const menuId = useId()

  useEffect(() => {
    if (!open) return
    function closeOutside(event: PointerEvent) {
      if (event.target instanceof Node && !root.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
  }, [open])

  function focusItem(step: number) {
    const buttons = [...(root.current?.querySelectorAll<HTMLButtonElement>('[role=menuitem]:not(:disabled)') ?? [])]
    if (!buttons.length) return
    const index = buttons.indexOf(document.activeElement as HTMLButtonElement)
    buttons[(index + step + buttons.length) % buttons.length].focus()
  }

  return (
    <div
      className="menu"
      ref={root}
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}
      onKeyDown={event => {
        if (!open) return
        if (event.key === 'Escape') {
          event.preventDefault()
          setOpen(false)
          trigger.current?.focus()
        }
        if (event.key === 'ArrowDown') {
          event.preventDefault()
          focusItem(1)
        }
        if (event.key === 'ArrowUp') {
          event.preventDefault()
          focusItem(-1)
        }
      }}>
      <button
        type="button"
        className="button"
        ref={trigger}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label={text ? undefined : label}
        title={label}
        disabled={disabled}
        onClick={() => setOpen(current => !current)}>
        {icon}
        {text && <span>{text}</span>}
        <ChevronDown size={15} className="menu-chevron" aria-hidden="true" />
      </button>
      {open && (
        <div id={menuId} className="popover menu-list" role="menu" aria-label={label}>
          {items.map((item, index) =>
            item === 'separator' ? (
              <div key={index} className="popover-separator" role="separator" />
            ) : (
              <button
                key={item.label}
                type="button"
                role="menuitem"
                className="popover-item"
                data-danger={item.danger || undefined}
                disabled={item.disabled}
                onClick={() => {
                  setOpen(false)
                  trigger.current?.focus()
                  item.onSelect()
                }}>
                {item.icon ?? <span className="popover-icon-gap" aria-hidden="true" />}
                <span>{item.label}</span>
              </button>
            ),
          )}
        </div>
      )}
    </div>
  )
}

export function Pagination({
  data,
  onChange,
  disabled = false,
}: {
  data?: Page<unknown>
  onChange: (page: number) => void
  disabled?: boolean
}) {
  if (!data || data.totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        className="icon-button"
        aria-label="Previous page"
        disabled={disabled || data.page === 0}
        onClick={() => onChange(data.page - 1)}>
        <ChevronLeft size={17} />
      </button>
      <span>
        {data.page + 1} / {data.totalPages}
      </span>
      <button
        className="icon-button"
        aria-label="Next page"
        disabled={disabled || data.page + 1 >= data.totalPages}
        onClick={() => onChange(data.page + 1)}>
        <ChevronRight size={17} />
      </button>
    </nav>
  )
}

export function Modal({ title, close, children }: { title: string; close: () => void; children: ReactNode }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    element.showModal()
    return () => element.close()
  }, [])
  return (
    <dialog
      ref={dialog}
      aria-label={title}
      onCancel={event => {
        event.preventDefault()
        close()
      }}>
      <div className="modal-heading">
        <h2>{title}</h2>
        <button className="icon-button" type="button" onClick={close} aria-label="Close">
          <X size={20} />
        </button>
      </div>
      {children}
    </dialog>
  )
}
