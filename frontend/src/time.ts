const locale = 'en'
export const MINUTES_PER_WORK_DAY = 8 * 60
export const MAX_ESTIMATE_MINUTES = 60000
export const MAX_LOG_MINUTES = 24 * 60

const units: Record<string, number> = { w: 5 * MINUTES_PER_WORK_DAY, d: MINUTES_PER_WORK_DAY, h: 60, m: 1 }

export function parseDuration(input: string): number | null {
  const text = input.trim().toLowerCase().replace(',', '.')
  if (!text) return null
  if (/^\d+(\.\d+)?$/.test(text)) return Math.round(Number(text) * 60) || null
  const clock = /^(\d+):([0-5]\d)$/.exec(text)
  if (clock) return Number(clock[1]) * 60 + Number(clock[2]) || null
  const parts = text.replace(/\s+/g, '').match(/\d+(?:\.\d+)?[wdhm]/g)
  if (!parts || parts.join('') !== text.replace(/\s+/g, '')) return null
  const minutes = Math.round(parts.reduce((total, part) => total + Number(part.slice(0, -1)) * units[part.slice(-1)], 0))
  return minutes > 0 ? minutes : null
}

export function readDuration(data: FormData, name: string) {
  return parseDuration(String(data.get(name) ?? ''))
}

export function formatDuration(minutes: number | null | undefined, empty = '—'): string {
  if (minutes === null || minutes === undefined) return empty
  if (minutes === 0) return '0h'
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (!hours) return `${rest}m`
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}

export function formatHours(minutes: number): string {
  const hours = minutes / 60
  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)}h`
}

export function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function fromIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function today(): string {
  return toIsoDate(new Date())
}

export function addDays(value: string, days: number): string {
  const date = fromIsoDate(value)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

export function startOfWeek(value: string): string {
  const date = fromIsoDate(value)
  const shift = (date.getDay() + 6) % 7
  date.setDate(date.getDate() - shift)
  return toIsoDate(date)
}

export function datesBetween(from: string, to: string): string[] {
  const dates: string[] = []
  for (let day = from; day <= to; day = addDays(day, 1)) dates.push(day)
  return dates
}

export function isWeekend(value: string): boolean {
  const day = fromIsoDate(value).getDay()
  return day === 0 || day === 6
}

export function formatDay(value: string, options: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric' }): string {
  return fromIsoDate(value).toLocaleDateString(locale, options)
}

export function formatRange(from: string, to: string): string {
  const start = fromIsoDate(from)
  const end = fromIsoDate(to)
  const sameYear = start.getFullYear() === end.getFullYear()
  const startText = start.toLocaleDateString(locale, { month: 'short', day: 'numeric', ...(sameYear ? {} : { year: 'numeric' }) })
  const endText = end.toLocaleDateString(locale, { month: 'short', day: 'numeric', year: 'numeric' })
  return `${startText} – ${endText}`
}

export function progress(spent: number, estimate: number | null): number {
  if (!estimate) return 0
  return Math.min(100, Math.round((spent / estimate) * 100))
}

function csvCell(value: string | number): string {
  const text = String(value)
  return /[",\n;]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

export function toCsv(rows: (string | number)[][]): string {
  return rows.map(row => row.map(csvCell).join(',')).join('\n')
}

export function downloadCsv(fileName: string, rows: (string | number)[][]) {
  const blob = new Blob(['﻿' + toCsv(rows)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}
