import { Building2, Check, ChevronDown, LayoutGrid, LogIn, Plus } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import type { Organization } from './api'
import { organizationRoles } from './labels'

export function OrganizationPicker({
  organizations,
  loading = false,
  selectedId,
  onSelect,
  onShowAll,
  onCreate,
  onJoin,
}: {
  organizations: Organization[]
  loading?: boolean
  selectedId: string
  onSelect: (id: string) => void
  onShowAll: () => void
  onCreate: () => void
  onJoin: () => void
}) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const optionsId = useId()
  const selected = organizations.find(item => item.id === selectedId)

  useEffect(() => {
    if (!open) return
    function closeOutside(event: PointerEvent) {
      if (event.target instanceof Node && !root.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
  }, [open])

  function choose(action: () => void) {
    setOpen(false)
    trigger.current?.focus()
    action()
  }

  return (
    <div
      className="org-picker"
      ref={root}
      onBlur={event => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}
      onKeyDown={event => {
        if (event.key === 'Escape' && open) {
          event.preventDefault()
          setOpen(false)
          trigger.current?.focus()
        }
      }}>
      <span className="org-picker-label">Organization</span>
      <button
        type="button"
        className="org-trigger"
        ref={trigger}
        aria-expanded={open}
        aria-controls={optionsId}
        aria-label={selected ? `Organization: ${selected.title}` : 'Select an organization'}
        onClick={() => setOpen(current => !current)}>
        <span className="org-icon">
          <Building2 size={15} aria-hidden="true" />
        </span>
        <span className="org-trigger-title">
          {selected?.title ?? (loading ? 'Loading…' : organizations.length ? 'Choose organization' : 'No organization')}
        </span>
        <ChevronDown size={16} className="org-chevron" aria-hidden="true" />
      </button>
      <div id={optionsId} className="popover org-options" role="group" aria-label="Organizations" hidden={!open}>
        {organizations.map(item => (
          <button
            type="button"
            key={item.id}
            className="popover-item org-option"
            aria-pressed={item.id === selectedId}
            onClick={() => choose(() => onSelect(item.id))}>
            <span className="org-option-label">
              <strong>{item.title}</strong>
              <small>{organizationRoles[item.role]}</small>
            </span>
            {item.id === selectedId && <Check size={16} aria-hidden="true" />}
          </button>
        ))}
        {organizations.length > 0 && <div className="popover-separator" role="separator" />}
        <button type="button" className="popover-item" onClick={() => choose(onShowAll)}>
          <LayoutGrid size={15} />
          All organizations
        </button>
        <button type="button" className="popover-item" onClick={() => choose(onCreate)}>
          <Plus size={15} />
          Create organization
        </button>
        <button type="button" className="popover-item" onClick={() => choose(onJoin)}>
          <LogIn size={15} />
          Join with a code
        </button>
      </div>
    </div>
  )
}
