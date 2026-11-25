import { describe, expect, it } from 'vitest'
import {
  addDays,
  datesBetween,
  formatDuration,
  formatHours,
  isWeekend,
  parseDuration,
  progress,
  readDuration,
  startOfWeek,
  toCsv,
} from './time'

describe('parseDuration', () => {
  it.each([
    ['2h', 120],
    ['2h 30m', 150],
    ['2h30m', 150],
    ['45m', 45],
    ['1.5h', 90],
    ['1,5h', 90],
    ['1d', 480],
    ['1w', 2400],
    ['1d 2h', 600],
    ['1.5', 90],
    ['3', 180],
    ['1:30', 90],
    [' 2H 15M ', 135],
  ])('reads %s as %i minutes', (input, minutes) => {
    expect(parseDuration(input)).toBe(minutes)
  })

  it.each(['', 'abc', '2x', 'h', '0', '0h', '-1h', '2h abc', '1:75'])('rejects %j', input => {
    expect(parseDuration(input)).toBeNull()
  })

  it('reads a duration from form data', () => {
    const data = new FormData()
    data.set('duration', '1h 5m')
    expect(readDuration(data, 'duration')).toBe(65)
    expect(readDuration(data, 'missing')).toBeNull()
  })
})

describe('formatting', () => {
  it('formats durations compactly', () => {
    expect(formatDuration(0)).toBe('0h')
    expect(formatDuration(45)).toBe('45m')
    expect(formatDuration(120)).toBe('2h')
    expect(formatDuration(150)).toBe('2h 30m')
    expect(formatDuration(null)).toBe('—')
    expect(formatDuration(null, 'Not set')).toBe('Not set')
  })

  it('formats hours with one decimal when needed', () => {
    expect(formatHours(120)).toBe('2h')
    expect(formatHours(90)).toBe('1.5h')
    expect(formatHours(100)).toBe('1.7h')
  })

  it('caps progress at 100 percent', () => {
    expect(progress(30, 120)).toBe(25)
    expect(progress(300, 120)).toBe(100)
    expect(progress(30, null)).toBe(0)
  })
})

describe('dates', () => {
  it('finds the Monday of a week', () => {
    expect(startOfWeek('2026-11-25')).toBe('2026-11-23')
    expect(startOfWeek('2026-11-23')).toBe('2026-11-23')
    expect(startOfWeek('2026-11-29')).toBe('2026-11-23')
  })

  it('walks across months and lists days', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
    expect(datesBetween('2026-11-28', '2026-12-01')).toEqual(['2026-11-28', '2026-11-29', '2026-11-30', '2026-12-01'])
  })

  it('knows weekends', () => {
    expect(isWeekend('2026-11-28')).toBe(true)
    expect(isWeekend('2026-11-27')).toBe(false)
  })
})

describe('csv', () => {
  it('quotes cells that need it', () => {
    expect(
      toCsv([
        ['Issue', 'Comment'],
        ['Fix "login"', 'a, b'],
      ]),
    ).toBe('Issue,Comment\n"Fix ""login""","a, b"')
  })
})
