import { ChevronDown, ChevronUp, ChevronsUp, Equal } from 'lucide-react'
import type { IssuePriority, IssueStatus } from './api'
import { avatarHue, initials, priorities, statuses } from './labels'

export function PriorityIcon({ priority }: { priority: IssuePriority }) {
  const Icon = { CRITICAL: ChevronsUp, HIGH: ChevronUp, MEDIUM: Equal, LOW: ChevronDown }[priority]
  return <Icon className="priority-icon" data-priority={priority} size={15} aria-label={`${priorities[priority]} priority`} />
}

export function Avatar({ name, size = 'small' }: { name: string | null; size?: 'small' | 'tiny' }) {
  if (!name)
    return (
      <span className={`avatar ${size} empty-avatar`} title="Unassigned" aria-label="Unassigned">
        ?
      </span>
    )
  const hue = avatarHue(name)
  return (
    <span
      className={`avatar ${size}`}
      title={name}
      aria-hidden="true"
      style={{ background: `hsl(${hue} 70% 90%)`, color: `hsl(${hue} 55% 32%)` }}>
      {initials(name)}
    </span>
  )
}

export function StatusBar({ counts, total }: { counts: Record<IssueStatus, number>; total: number }) {
  if (total === 0) return <div className="progress empty-progress" aria-hidden="true" />
  return (
    <div className="progress" role="img" aria-label={`${counts.DONE} of ${total} issues done`}>
      {(['DONE', 'IN_REVIEW', 'IN_PROGRESS'] as const).map(
        status =>
          counts[status] > 0 && (
            <span
              key={status}
              data-status={status}
              title={`${statuses[status]}: ${counts[status]}`}
              style={{ width: `${(counts[status] / total) * 100}%` }}
            />
          ),
      )}
    </div>
  )
}
