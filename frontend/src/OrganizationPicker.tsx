import { Building2, Check, ChevronsUpDown } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import type { Organization } from './api'

export const organizationRoles = { OWNER: 'Owner', ADMIN: 'Administrator', USER: 'Member' }

export function OrganizationPicker({ organizations, selectedId, onSelect }: {
  organizations: Organization[]
  selectedId: string
  onSelect: (id: string) => void
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

  return <div className="org-picker" ref={root}
    onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false) }}
    onKeyDown={event => {
      if (event.key === 'Escape' && open) {
        event.preventDefault()
        setOpen(false)
        trigger.current?.focus()
      }
    }}>
    <span className="org-picker-label">Organization</span>
    <button type="button" className="org-trigger" ref={trigger} aria-expanded={open} aria-controls={optionsId}
      aria-label={selected ? `Organization: ${selected.title}` : 'Select an organization'}
      onClick={() => setOpen(current => !current)}>
      <span className="org-icon"><Building2 size={15} aria-hidden="true" /></span>
      <span className="org-trigger-title">{selected?.title ?? 'Choose organization'}</span>
      <ChevronsUpDown size={15} className="org-chevron" aria-hidden="true" />
    </button>
    <div id={optionsId} className="org-options" role="group" aria-label="Organizations" hidden={!open}>
      {organizations.map(item => <button type="button" key={item.id} className="org-option"
        aria-pressed={item.id === selectedId} onClick={() => {
          setOpen(false)
          trigger.current?.focus()
          if (item.id !== selectedId) onSelect(item.id)
        }}>
        <span className="org-option-label"><strong>{item.title}</strong><small>{organizationRoles[item.role]}</small></span>
        {item.id === selectedId && <Check size={16} aria-hidden="true" />}
      </button>)}
    </div>
  </div>
}
