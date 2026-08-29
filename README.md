# Customer Management

A small full-stack application for creating and viewing customer records.

- **Backend** — Java 17, Spring Boot 3.3, Spring Data JPA, H2 (in-memory), Bean Validation
- **Frontend** — React 19, TypeScript, Vite
- **Tests** — JUnit 5, Mockito, AssertJ, MockMvc, `TestRestTemplate`; Vitest and Testing Library

---

## Running it

### Prerequisites

| Tool  | Version |
| ----- | ------- |
| JDK   | 17+     |
| Maven | 3.9+    |
| Node  | 20+     |

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

- OpenAPI UI — <http://localhost:8080/swagger-ui.html>
- H2 console — <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:customers`, user `sa`, no password)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts on <http://localhost:5173> and proxies `/api` to the backend, so both run on one
origin in development and there is no CORS preflight to debug.

### Tests

```bash
cd backend  && mvn test   # unit, slice and integration tests
cd frontend && npm test   # component and helper tests
cd frontend && npm run lint  # TypeScript, no emit
```

---

## API

Base path `/api/v1/customers`.

### `POST /api/v1/customers`

```json
{ "firstName": "Jane", "lastName": "O'Connor", "dateOfBirth": "1990-04-17" }
```

`201 Created` with a `Location` header and the created record:

```json
{
  "id": "6a3f0c4e-6f4a-4a6f-9c1e-2b3d4e5f6a7b",
  "firstName": "Jane",
  "lastName": "O'Connor",
  "dateOfBirth": "1990-04-17",
  "age": 35,
  "createdAt": "2025-06-15T09:00:00Z"
}
```

### `GET /api/v1/customers`

| Parameter | Default        | Notes                                                      |
| --------- | -------------- | ---------------------------------------------------------- |
| `q`       | —              | Case-insensitive substring match on either name             |
| `page`    | `0`            |                                                             |
| `size`    | `20`           | Capped at 100                                               |
| `sort`    | `createdAt,desc` | `firstName`, `lastName`, `dateOfBirth`, `createdAt` only  |

```json
{ "items": [], "page": 0, "size": 20, "totalItems": 0, "totalPages": 0 }
```

### `GET /api/v1/customers/{id}`

`200 OK`, or `404` if no record exists.

### Errors

Every failure is an RFC 9457 problem document (`application/problem+json`):

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "errors": [{ "field": "dateOfBirth", "message": "is required" }]
}
```

| Status | When                                                        |
| ------ | ----------------------------------------------------------- |
| 400    | Failed validation, unparseable body, bad `sort`, non-UUID id |
| 404    | No customer with that id                                     |
| 409    | A customer with the same name and date of birth exists       |
| 500    | Anything unhandled — logged in full, opaque to the caller    |

---

## Design decisions

The brief is deliberately thin in places. These are the calls I made and why.

**Duplicates are a conflict, not a silent second record.** Two people genuinely can share a name
and a birth date, but an identical payload is far more often a double-submitted form. A spurious
duplicate is much harder to find later than a rejected request is to work around now, so the API
returns `409`. The service checks first and the database enforces a unique constraint on
`(first_name, last_name, date_of_birth)` — the check alone loses to a concurrent request, so the
constraint is the real guarantee and the service translates the resulting integrity violation into
the same `409`. If genuine duplicates turn out to matter, the fix is a client-supplied override
flag rather than dropping the constraint.

**Names are compared case-insensitively, but stored as typed.** "jane doe" and "Jane Doe" are the
same person. What the user entered is what gets displayed back.

**Date of birth is a `LocalDate`, and age is derived.** A birth date is a calendar fact with no time
zone; storing an instant would make someone's birthday move depending on where the reader sits.
Age is computed per request from an injected `Clock` — a stored age is wrong the day after it is
written, and the injected clock is what makes the age tests deterministic.

**Validation rejects typos, not people.** Names allow any Unicode letter plus spaces, hyphens,
apostrophes and full stops, so "Zoë", "李", "O'Connor" and "van der Berg" are all valid; an
ASCII-only rule would have been a correctness bug wearing a validation costume. Birth dates must be
in the past and within 130 years, which catches `2099` and `1850` without excluding anyone real.
Whitespace is normalised in the DTO's compact constructor, so it is cleaned before validation and
before persistence rather than in three separate places.

**The API owns its pagination envelope.** Serialising Spring's `PageImpl` leaks framework internals
into the public contract and its JSON shape is explicitly unstable across versions, so
`PageResponse` is ours.

**Sort fields are whitelisted.** Passing an arbitrary property to Spring Data turns a query
parameter into a probe of the entity model, and an unknown property surfaces as a 500 rather than a
400.

**Errors never leak internals.** Stack traces and parser messages go to the log; the caller gets a
problem document with a field-addressable `errors` array, which is what lets the UI put each message
under the input that caused it instead of dumping one blob at the top of the form.

**`ddl-auto: create-drop`.** Defensible only because the database is ephemeral by design. Anything
durable would use Flyway with `ddl-auto: validate`; the entity already declares the constraints and
indexes that the first migration would contain.

**Scope held to the brief.** No update or delete endpoint, no authentication, no pagination controls
in the UI (the list requests up to 100 and the backend pages properly underneath). The layering makes
each a small addition, and inventing them would have spent time better put into tests and error
handling.

### Testing approach

Four levels, each answering a different question:

| Test                            | Question it answers                                     |
| ------------------------------- | ------------------------------------------------------- |
| `CreateCustomerRequestTest`     | Do the validation rules accept real names and reject typos? |
| `CustomerServiceTest`           | Are the business rules right, including age arithmetic across leap days and birthdays? |
| `CustomerRepositoryTest`        | Do the queries and the database constraint actually behave? |
| `CustomerControllerTest`        | Is the HTTP contract right — statuses, headers, error shape? |
| `CustomerApiIntegrationTest`    | Does it work end to end with nothing mocked?            |

The integration tests are the ones that would catch a wiring mistake every mocked layer above
happily agreed on. On the frontend, `format.test.ts` covers the validation mirrored from the server
and `CustomerForm.test.tsx` covers the four things a form has to get right: blocking an invalid
submit, sending the right payload, binding server field errors to inputs, and explaining a
duplicate or an unreachable API in words rather than status codes.

### Frontend notes

The UI is a two-pane records tool: entry form on the left, ledger on the right. Data columns are set
in a monospace face with tabular figures so a mis-keyed date is visible at a glance. Search is
debounced, and the list hook guards against out-of-order responses — without a sequence check a slow
early request can overwrite a fast later one and the list stops matching what is in the search box.
Keyboard focus moves to the first invalid field on a failed submit, `aria-invalid` and
`aria-describedby` tie messages to inputs, and the layout collapses to one column below 860px.

### What I would do next

- Flyway migrations and a real database
- Pagination controls in the UI once the list outgrows one screen
- Authentication and per-tenant scoping, which changes the duplicate rule from global to per-tenant
- An architecture test (ArchUnit) to keep the web layer out of the persistence layer as this grows
- Structured JSON logging with a correlation id
