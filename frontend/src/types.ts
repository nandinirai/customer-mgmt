export interface Customer {
  id: string
  firstName: string
  lastName: string
  /** ISO-8601 calendar date, e.g. 1990-04-17 */
  dateOfBirth: string
  age: number
  createdAt: string
}

export interface NewCustomer {
  firstName: string
  lastName: string
  dateOfBirth: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface FieldError {
  field: string
  message: string
}
