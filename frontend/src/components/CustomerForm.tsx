import { useId, useRef, useState } from 'react'
import { createCustomer } from '../api/customers'
import { ApiError, NetworkError } from '../api/errors'
import { MAX_NAME_LENGTH, validateDateOfBirth, validateName, yesterdayIso } from '../lib/format'
import type { Customer } from '../types'

type Field = 'firstName' | 'lastName' | 'dateOfBirth'
type Errors = Partial<Record<Field, string>>

const EMPTY = { firstName: '', lastName: '', dateOfBirth: '' }

interface Props {
  onCreated: (customer: Customer) => void
}

export function CustomerForm({ onCreated }: Props) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const firstFieldRef = useRef<HTMLInputElement>(null)
  const ids = { firstName: useId(), lastName: useId(), dateOfBirth: useId() }

  const validate = (): Errors => {
    const found: Errors = {}
    const firstName = validateName('First name', values.firstName)
    const lastName = validateName('Last name', values.lastName)
    const dateOfBirth = validateDateOfBirth(values.dateOfBirth)
    if (firstName) found.firstName = firstName
    if (lastName) found.lastName = lastName
    if (dateOfBirth) found.dateOfBirth = dateOfBirth
    return found
  }

  const update = (field: Field) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setValues((previous) => ({ ...previous, [field]: event.target.value }))
    // Clear the message as soon as the person starts fixing the field, rather
    // than leaving stale red text under an input they have already corrected.
    setErrors((previous) => ({ ...previous, [field]: undefined }))
    setFormError(null)
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setConfirmation(null)
    const found = validate()
    if (Object.keys(found).length > 0) {
      setErrors(found)
      // Send focus to the first thing that needs fixing, so keyboard and
      // screen-reader users are not left hunting for the message.
      const firstInvalid = (['firstName', 'lastName', 'dateOfBirth'] as Field[]).find((f) => found[f])
      if (firstInvalid) document.getElementById(ids[firstInvalid])?.focus()
      return
    }

    setSaving(true)
    setFormError(null)
    try {
      const created = await createCustomer({
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        dateOfBirth: values.dateOfBirth,
      })
      setValues(EMPTY)
      setErrors({})
      setConfirmation(`Added ${created.firstName} ${created.lastName}.`)
      onCreated(created)
      firstFieldRef.current?.focus()
    } catch (error) {
      handleFailure(error)
    } finally {
      setSaving(false)
    }
  }

  const handleFailure = (error: unknown) => {
    if (error instanceof ApiError && error.isValidation && error.fieldErrors.length > 0) {
      const mapped: Errors = {}
      for (const { field, message } of error.fieldErrors) {
        if (field === 'firstName' || field === 'lastName' || field === 'dateOfBirth') {
          mapped[field] = `${label(field)} ${message}`
        }
      }
      setErrors(mapped)
      setFormError(Object.keys(mapped).length ? null : error.message)
      return
    }
    if (error instanceof ApiError && error.isConflict) {
      setFormError('That customer is already on file. Check the list before adding them again.')
      return
    }
    setFormError(error instanceof NetworkError ? error.message : 'The customer could not be saved. Try again.')
  }

  return (
    <section className="panel" aria-labelledby="add-customer-heading">
      <h2 id="add-customer-heading" className="panel__title">
        Add a customer
      </h2>

      <form onSubmit={handleSubmit} noValidate>

      {formError && (
        <p className="alert alert--error" role="alert">
          {formError}
        </p>
      )}
      {confirmation && (
        <p className="alert alert--ok" role="status">
          {confirmation}
        </p>
      )}

      <div className="field">
        <label className="field__label" htmlFor={ids.firstName}>
          First name
        </label>
        <input
          id={ids.firstName}
          ref={firstFieldRef}
          className="field__input"
          value={values.firstName}
          onChange={update('firstName')}
          maxLength={MAX_NAME_LENGTH}
          autoComplete="given-name"
          aria-invalid={Boolean(errors.firstName)}
          aria-describedby={errors.firstName ? `${ids.firstName}-error` : undefined}
        />
        {errors.firstName && (
          <p className="field__error" id={`${ids.firstName}-error`}>
            {errors.firstName}
          </p>
        )}
      </div>

      <div className="field">
        <label className="field__label" htmlFor={ids.lastName}>
          Last name
        </label>
        <input
          id={ids.lastName}
          className="field__input"
          value={values.lastName}
          onChange={update('lastName')}
          maxLength={MAX_NAME_LENGTH}
          autoComplete="family-name"
          aria-invalid={Boolean(errors.lastName)}
          aria-describedby={errors.lastName ? `${ids.lastName}-error` : undefined}
        />
        {errors.lastName && (
          <p className="field__error" id={`${ids.lastName}-error`}>
            {errors.lastName}
          </p>
        )}
      </div>

      <div className="field">
        <label className="field__label" htmlFor={ids.dateOfBirth}>
          Date of birth
        </label>
        <input
          id={ids.dateOfBirth}
          className="field__input field__input--date"
          type="date"
          value={values.dateOfBirth}
          onChange={update('dateOfBirth')}
          max={yesterdayIso()}
          autoComplete="bday"
          aria-invalid={Boolean(errors.dateOfBirth)}
          aria-describedby={errors.dateOfBirth ? `${ids.dateOfBirth}-error` : undefined}
        />
        {errors.dateOfBirth && (
          <p className="field__error" id={`${ids.dateOfBirth}-error`}>
            {errors.dateOfBirth}
          </p>
        )}
      </div>

      <button type="submit" className="button" disabled={saving}>
        {saving ? 'Adding…' : 'Add customer'}
      </button>
      </form>
    </section>
  )
}

function label(field: Field): string {
  switch (field) {
    case 'firstName':
      return 'First name'
    case 'lastName':
      return 'Last name'
    case 'dateOfBirth':
      return 'Date of birth'
  }
}
