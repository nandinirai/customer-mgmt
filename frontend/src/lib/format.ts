/** Renders an ISO date as a readable one without dragging in a date library. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  if (!year || !month || !day) {
    return isoDate
  }
  const date = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)))
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date)
}

export const MAX_NAME_LENGTH = 100

const NAME_PATTERN = /^\p{L}[\p{L}\p{M}'\u2019\-. ]*$/u

export function validateName(label: string, value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) {
    return `${label} is required`
  }
  if (trimmed.length > MAX_NAME_LENGTH) {
    return `${label} must be at most ${MAX_NAME_LENGTH} characters`
  }
  if (!NAME_PATTERN.test(trimmed)) {
    return `${label} may only contain letters, spaces, hyphens, apostrophes and full stops`
  }
  return null
}

export function validateDateOfBirth(value: string): string | null {
  if (!value) {
    return 'Date of birth is required'
  }
  const parsed = new Date(`${value}T00:00:00Z`)
  if (Number.isNaN(parsed.getTime())) {
    return 'Date of birth must be a real date'
  }
  const today = new Date()
  const todayUtc = Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate())
  if (parsed.getTime() >= todayUtc) {
    return 'Date of birth must be in the past'
  }
  const earliest = Date.UTC(today.getUTCFullYear() - 130, today.getUTCMonth(), today.getUTCDate())
  if (parsed.getTime() <= earliest) {
    return 'Date of birth must be within the last 130 years'
  }
  return null
}

/** The latest date the browser's date picker should offer. */
export function yesterdayIso(): string {
  const now = new Date()
  const yesterday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - 1))
  return yesterday.toISOString().slice(0, 10)
}
