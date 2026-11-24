import { Check, ChevronDown } from 'lucide-react'
import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'

export type SelectOption = { value: string; label: string; hint?: string; icon?: ReactNode; disabled?: boolean }

export function Select({
  options,
  value,
  defaultValue,
  onChange,
  name,
  disabled = false,
  placeholder = 'Select…',
  className,
  ...aria
}: {
  options: SelectOption[]
  value?: string
  defaultValue?: string
  onChange?: (value: string) => void
  name?: string
  disabled?: boolean
  placeholder?: string
  className?: string
  'aria-label'?: string
}) {
  const [inner, setInner] = useState(defaultValue ?? options[0]?.value ?? '')
  const current = value ?? inner
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const [upward, setUpward] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const list = useRef<HTMLDivElement>(null)
  const typed = useRef({ text: '', at: 0 })
  const id = useId()
  const selected = options.find(option => option.value === current)

  useEffect(() => {
    if (!open) return
    function closeOutside(event: PointerEvent) {
      if (event.target instanceof Node && !root.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
  }, [open])

  useEffect(() => {
    if (!open || active < 0) return
    list.current?.querySelector<HTMLElement>(`[data-index="${active}"]`)?.scrollIntoView({ block: 'nearest' })
  }, [open, active])

  function show() {
    if (disabled) return
    const rect = trigger.current?.getBoundingClientRect()
    if (rect) {
      const below = window.innerHeight - rect.bottom
      setUpward(below < 260 && rect.top > below)
    }
    setActive(
      Math.max(
        0,
        options.findIndex(option => option.value === current),
      ),
    )
    setOpen(true)
  }

  function choose(option: SelectOption) {
    if (option.disabled) return
    setOpen(false)
    trigger.current?.focus()
    if (option.value === current) return
    if (value === undefined) setInner(option.value)
    onChange?.(option.value)
  }

  function move(step: number) {
    if (!options.length) return
    let next = active
    for (let i = 0; i < options.length; i++) {
      next = (next + step + options.length) % options.length
      if (!options[next].disabled) break
    }
    setActive(next)
  }

  function onKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) return
    if (!open) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) {
        event.preventDefault()
        show()
      }
      return
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
    } else if (event.key === 'Tab') setOpen(false)
    else if (event.key === 'ArrowDown') {
      event.preventDefault()
      move(1)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      move(-1)
    } else if (event.key === 'Home') {
      event.preventDefault()
      setActive(0)
    } else if (event.key === 'End') {
      event.preventDefault()
      setActive(options.length - 1)
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (options[active]) choose(options[active])
    } else if (event.key.length === 1) {
      const now = Date.now()
      typed.current = { text: (now - typed.current.at < 600 ? typed.current.text : '') + event.key.toLowerCase(), at: now }
      const match = options.findIndex(option => option.label.toLowerCase().startsWith(typed.current.text))
      if (match >= 0) setActive(match)
    }
  }

  return (
    <div
      className={`select ${className ?? ''}`}
      ref={root}
      data-open={open || undefined}
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}>
      {name && <input type="hidden" name={name} value={current} />}
      <button
        type="button"
        ref={trigger}
        className="select-trigger"
        disabled={disabled}
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={`${id}-list`}
        aria-activedescendant={open && active >= 0 ? `${id}-${active}` : undefined}
        aria-label={aria['aria-label']}
        onClick={() => (open ? setOpen(false) : show())}
        onKeyDown={onKeyDown}>
        {selected?.icon}
        <span className="select-value" data-placeholder={!selected || undefined}>
          {selected?.label ?? placeholder}
        </span>
        <ChevronDown size={16} className="select-chevron" aria-hidden="true" />
      </button>
      {open && (
        <div
          ref={list}
          id={`${id}-list`}
          role="listbox"
          className="popover select-list"
          data-upward={upward || undefined}
          aria-label={aria['aria-label']}>
          {options.length === 0 && <div className="select-empty">No options</div>}
          {options.map((option, index) => (
            <div
              key={option.value || '__empty'}
              id={`${id}-${index}`}
              data-index={index}
              role="option"
              aria-selected={option.value === current}
              aria-disabled={option.disabled || undefined}
              className="popover-item select-option"
              data-active={index === active || undefined}
              onPointerEnter={() => setActive(index)}
              onPointerDown={event => event.preventDefault()}
              tabIndex={-1}
              onClick={event => {
                event.preventDefault()
                choose(option)
              }}
              onKeyDown={event => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  choose(option)
                }
              }}>
              {option.icon}
              <span className="select-option-label">
                {option.label}
                {option.hint && <small>{option.hint}</small>}
              </span>
              {option.value === current && <Check size={15} className="select-check" aria-hidden="true" />}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
