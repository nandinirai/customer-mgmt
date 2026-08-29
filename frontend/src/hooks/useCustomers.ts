import { useCallback, useEffect, useRef, useState } from 'react'
import { listCustomers } from '../api/customers'
import { NetworkError } from '../api/errors'
import type { Customer } from '../types'

type Status = 'loading' | 'ready' | 'error'

interface State {
  customers: Customer[]
  totalItems: number
  status: Status
  error: string | null
}

const INITIAL: State = { customers: [], totalItems: 0, status: 'loading', error: null }

export function useCustomers(search: string) {
  const [state, setState] = useState<State>(INITIAL)
  // Typing in the search box fires overlapping requests; without a sequence
  // guard a slow early response can overwrite a fast later one and the list
  // stops matching the query on screen.
  const latestRequest = useRef(0)

  const load = useCallback(
    async (signal?: AbortSignal) => {
      const requestId = ++latestRequest.current
      setState((previous) => ({ ...previous, status: 'loading', error: null }))
      try {
        const page = await listCustomers({ search, size: 100, sort: 'createdAt,desc', signal })
        if (requestId !== latestRequest.current) return
        setState({ customers: page.items, totalItems: page.totalItems, status: 'ready', error: null })
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return
        if (requestId !== latestRequest.current) return
        setState({
          customers: [],
          totalItems: 0,
          status: 'error',
          error:
            error instanceof NetworkError
              ? error.message
              : 'The customer list could not be loaded. Try again.',
        })
      }
    },
    [search],
  )

  useEffect(() => {
    const controller = new AbortController()
    void load(controller.signal)
    return () => controller.abort()
  }, [load])

  return { ...state, reload: () => load() }
}
