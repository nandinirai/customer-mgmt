import type { Customer, NewCustomer, Page } from '../types'
import { ApiError, NetworkError } from './errors'

const BASE = '/api/v1/customers'

interface ProblemDetail {
  title?: string
  detail?: string
  errors?: { field: string; message: string }[]
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(url, {
      ...init,
      headers: { Accept: 'application/json', ...(init?.headers ?? {}) },
    })
  } catch (cause) {
    // fetch only rejects when the request failed to complete at all. An
    // AbortError is a deliberate cancellation, not a failure to report.
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause
    }
    throw new NetworkError()
  }

  if (response.ok) {
    return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
  }

  const problem = await response.json().catch((): ProblemDetail => ({}))
  throw new ApiError(
    response.status,
    problem.title ?? 'Request failed',
    problem.detail ?? `The server responded with ${response.status}.`,
    problem.errors ?? [],
  )
}

export interface ListParams {
  search?: string
  page?: number
  size?: number
  sort?: string
  signal?: AbortSignal
}

export function listCustomers({
  search,
  page = 0,
  size = 20,
  sort = 'createdAt,desc',
  signal,
}: ListParams = {}): Promise<Page<Customer>> {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort })
  if (search?.trim()) {
    params.set('q', search.trim())
  }
  return request<Page<Customer>>(`${BASE}?${params.toString()}`, { signal })
}

export function createCustomer(customer: NewCustomer): Promise<Customer> {
  return request<Customer>(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(customer),
  })
}
