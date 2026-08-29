import { describe, expect, it } from 'vitest'
import { formatDate, validateDateOfBirth, validateName } from '../lib/format'

describe('formatDate', () => {
  it('renders an ISO date without shifting the day across time zones', () => {
    expect(formatDate('1990-04-17')).toContain('1990')
    expect(formatDate('1990-04-17')).toContain('17')
  })

  it('returns the input unchanged when it is not a date', () => {
    expect(formatDate('nonsense')).toBe('nonsense')
  })
})

describe('validateName', () => {
  it.each(['Jane', "O'Connor", 'Anne-Marie', 'Zoë', '李', 'van der Berg'])('accepts %s', (name) => {
    expect(validateName('First name', name)).toBeNull()
  })

  it.each(['', '   ', 'Jane123', '<script>'])('rejects %s', (name) => {
    expect(validateName('First name', name)).not.toBeNull()
  })

  it('rejects a name over 100 characters', () => {
    expect(validateName('First name', 'A'.repeat(101))).toContain('100')
  })
})

describe('validateDateOfBirth', () => {
  it('requires a value', () => {
    expect(validateDateOfBirth('')).toBe('Date of birth is required')
  })

  it('rejects today and the future', () => {
    const today = new Date().toISOString().slice(0, 10)
    expect(validateDateOfBirth(today)).toContain('past')
    expect(validateDateOfBirth('2099-01-01')).toContain('past')
  })

  it('rejects a date more than 130 years ago', () => {
    expect(validateDateOfBirth('1850-01-01')).toContain('130')
  })

  it('accepts a plausible birth date', () => {
    expect(validateDateOfBirth('1990-04-17')).toBeNull()
  })
})
