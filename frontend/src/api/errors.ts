import type { FieldError } from '../types'

/**
 * A failure the server described in RFC 9457 terms. Keeping the field errors
 * structured is what lets the form put each message next to the input that
 * caused it instead of dumping one blob at the top.
 */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, title: string, detail: string, fieldErrors: FieldError[] = []) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.fieldErrors = fieldErrors
  }

  get isConflict(): boolean {
    return this.status === 409
  }

  get isValidation(): boolean {
    return this.status === 400
  }
}

/** Raised when the request never reached the server. */
export class NetworkError extends Error {
  constructor() {
    super('Could not reach the server. Check that the API is running on port 8080.')
    this.name = 'NetworkError'
  }
}
