import { useEffect, useState } from 'react'
import { CustomerForm } from './components/CustomerForm'
import { CustomerTable } from './components/CustomerTable'
import { useCustomers } from './hooks/useCustomers'

export default function App() {
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const { customers, totalItems, status, error, reload } = useCustomers(search)

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchInput), 250)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  return (
    <div className="app">
      <header className="masthead">
        <h1 className="masthead__title">Customer records</h1>
        <p className="masthead__count" aria-live="polite">
          {status === 'ready' ? `${totalItems} on file` : '—'}
        </p>
      </header>

      <main className="layout">
        <CustomerForm onCreated={reload} />

        <section className="panel panel--wide" aria-labelledby="records-heading">
          <div className="panel__header">
            <h2 id="records-heading" className="panel__title">
              On file
            </h2>
            <label className="search">
              <span className="visually-hidden">Search by name</span>
              <input
                type="search"
                className="search__input"
                placeholder="Search by name"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
              />
            </label>
          </div>

          <CustomerTable
            customers={customers}
            status={status}
            error={error}
            search={search}
            onRetry={reload}
          />
        </section>
      </main>
    </div>
  )
}
