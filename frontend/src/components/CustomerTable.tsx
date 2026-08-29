import { formatDate } from '../lib/format'
import type { Customer } from '../types'

interface Props {
  customers: Customer[]
  status: 'loading' | 'ready' | 'error'
  error: string | null
  search: string
  onRetry: () => void
}

export function CustomerTable({ customers, status, error, search, onRetry }: Props) {
  if (status === 'error') {
    return (
      <div className="state" role="alert">
        <p className="state__title">{error}</p>
        <button type="button" className="button button--quiet" onClick={onRetry}>
          Try again
        </button>
      </div>
    )
  }

  if (status === 'loading' && customers.length === 0) {
    return <p className="state state--muted">Loading records…</p>
  }

  if (customers.length === 0) {
    return (
      <div className="state">
        <p className="state__title">
          {search ? `No customer matches “${search}”.` : 'No customers on file yet.'}
        </p>
        <p className="state__hint">
          {search ? 'Try a shorter search term.' : 'Add the first one using the form.'}
        </p>
      </div>
    )
  }

  return (
    <table className="ledger">
      <caption className="visually-hidden">Customer records, newest first</caption>
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Date of birth</th>
          <th scope="col" className="ledger__numeric">
            Age
          </th>
        </tr>
      </thead>
      <tbody>
        {customers.map((customer) => (
          <tr key={customer.id}>
            <td>
              <span className="ledger__name">
                {customer.lastName}, {customer.firstName}
              </span>
            </td>
            <td>
              <time className="ledger__data" dateTime={customer.dateOfBirth}>
                {formatDate(customer.dateOfBirth)}
              </time>
            </td>
            <td className="ledger__numeric">
              <span className="ledger__data">{customer.age}</span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
