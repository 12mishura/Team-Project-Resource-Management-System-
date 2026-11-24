type ConfirmOptions = {
  title: string
  message: string
  confirmLabel: string
  danger?: boolean
  requireText?: string
}

export type PendingConfirm = ConfirmOptions & { resolve: (confirmed: boolean) => void }
type Toast = { id: number; text: string }

let pending: PendingConfirm | null = null
let toasts: Toast[] = []
let nextToastId = 1
const listeners = new Set<() => void>()
const emit = () => listeners.forEach(listener => listener())
export const subscribe = (listener: () => void) => {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function confirmAction(options: ConfirmOptions): Promise<boolean> {
  pending?.resolve(false)
  return new Promise(resolve => {
    pending = { ...options, resolve }
    emit()
  })
}

export function toast(text: string) {
  const id = nextToastId++
  toasts = [...toasts, { id, text }].slice(-3)
  emit()
  window.setTimeout(() => dismissToast(id), 3500)
}

export function dismissToast(id: number) {
  if (!toasts.some(item => item.id === id)) return
  toasts = toasts.filter(item => item.id !== id)
  emit()
}

export function settle(confirmed: boolean) {
  const current = pending
  pending = null
  emit()
  current?.resolve(confirmed)
}

export const currentConfirm = () => pending
export const currentToasts = () => toasts
