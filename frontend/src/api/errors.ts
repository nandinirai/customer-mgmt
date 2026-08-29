import type { FieldError } from '../types'

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
