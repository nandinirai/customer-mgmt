import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CustomerForm } from '../components/CustomerForm'

const fetchMock = vi.fn()

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  fetchMock.mockReset()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response
}

async function fillForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('First name'), 'Jane')
  await user.type(screen.getByLabelText('Last name'), 'Doe')
  fireEvent.change(screen.getByLabelText('Date of birth'), { target: { value: '1990-04-17' } })
}

describe('CustomerForm', () => {
  it('does not call the API when required fields are empty', async () => {
    const user = userEvent.setup()
    render(<CustomerForm onCreated={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Add customer' }))

    expect(await screen.findByText('First name is required')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('sends the trimmed values and confirms the addition', async () => {
    const user = userEvent.setup()
    const onCreated = vi.fn()
    fetchMock.mockResolvedValue(
      jsonResponse(201, {
        id: 'a3b1',
        firstName: 'Jane',
        lastName: 'Doe',
        dateOfBirth: '1990-04-17',
        age: 35,
        createdAt: '2025-06-15T09:00:00Z',
      }),
    )

    render(<CustomerForm onCreated={onCreated} />)
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Add customer' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1))
    const [, init] = fetchMock.mock.calls[0]!
    expect(JSON.parse(init.body)).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      dateOfBirth: '1990-04-17',
    })
    expect(await screen.findByText('Added Jane Doe.')).toBeInTheDocument()
  })

  it('explains a duplicate instead of showing a raw status code', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      jsonResponse(409, { title: 'Duplicate customer', detail: 'already exists' }),
    )

    render(<CustomerForm onCreated={vi.fn()} />)
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Add customer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('already on file')
  })

  it('places a server field error next to the field that caused it', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      jsonResponse(400, {
        title: 'Validation failed',
        detail: 'One or more fields are invalid.',
        errors: [{ field: 'lastName', message: 'is required' }],
      }),
    )

    render(<CustomerForm onCreated={vi.fn()} />)
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Add customer' }))

    const lastName = await screen.findByLabelText('Last name')
    await waitFor(() => expect(lastName).toHaveAttribute('aria-invalid', 'true'))
    expect(screen.getByText('Last name is required')).toBeInTheDocument()
  })

  it('reports an unreachable API in plain language', async () => {
    const user = userEvent.setup()
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    render(<CustomerForm onCreated={vi.fn()} />)
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Add customer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the server')
  })
})
