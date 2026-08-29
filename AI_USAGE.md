# AI Usage
## Tools used

| Tool                  | Used for                                                                 |
| --------------------- | ------------------------------------------------------------------------ |
| Claude (chat)         | Scaffolding the project, drafting the backend and frontend, drafting docs |
| Copilot Github        | Reviewing                                                                 |

## What was delegated, and what was not

**Delegated.** The mechanical bulk: Maven and Vite configuration, entity/repository/service/controller
scaffolding, DTO boilerplate, the exception-handler wiring, the CSS, and the first draft of the test
suite. This is the part of a CRUD application.

**Not delegated.** Every decision the brief left open. The AI will produce a working CRUD app without
being asked a single question, which is exactly the risk: it makes the ambiguous calls silently and
they look deliberate afterwards. The following were decided first and then handed over as
constraints:

- Treating a duplicate name + date of birth as `409` rather than accepting it, and why (see README)
- Enforcing that with a database unique constraint rather than a service check alone, because the
  check loses to a concurrent request
- Deriving age at read time from an injected `Clock` rather than storing it
- Owning the pagination envelope instead of serialising Spring's `PageImpl`
- Whitelisting sortable fields
- Allowing Unicode letters in names — the default draft used an ASCII-only pattern, which silently
  rejects a large share of real customers
- Holding scope to the brief: no update/delete, no auth

## How the generated code was validated

1. **Compile and run.** `npm test` (22 passing), `npx tsc --noEmit` and `npm run build` were run
   against the real toolchain, and the typecheck caught a dependency mismatch the passing tests did
   not (see correction 9). `[Backend: record the result of `mvn test` and a manual pass through the
   UI creating, searching and duplicating a record.]`
2. **Read it against the framework docs, not against plausibility.** Generated Spring code is
   confidently wrong in ways that still compile; the annotations were checked against the actual
   Spring Boot 3.3 and Bean Validation behaviour rather than accepted because they looked idiomatic.
3. **Tests written to fail first where it mattered.** The age and duplicate tests were checked by
   temporarily breaking the production code to confirm they actually caught it, rather than trusting
   a green run on code and tests drafted together — a suite generated alongside its implementation
   tends to assert what the code does, not what it should do.
4. **Error paths exercised by hand.** Backend stopped mid-session to confirm the UI says
   "Could not reach the server" instead of throwing a JSON parse error.

## Corrections made to AI output

These are the mistakes that actually came up, not a representative sample:

| # | Problem                                                                                                                              | Fix                                                                        |
| - | ------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| 1 | Swagger `@Schema` annotations placed on record components. `@Schema` does not target `RECORD_COMPONENT`, so this does not compile.     | Removed; springdoc infers the types anyway.                                 |
| 2 | `@CreatedDate` with `AuditingEntityListener` but no `@EnableJpaAuditing`. Compiles, runs, and leaves `created_at` null.                | Replaced with a `@PrePersist` callback, which also works in every test slice.|
| 3 | CORS origins modelled as a `@ConfigurationProperties` record. `WebConfig` is a `WebMvcConfigurer`, so `@WebMvcTest` loads it — without the properties bean the slice fails to start. | Replaced with `@Value`, removing the coupling entirely.                     |
| 4 | `@Validated` on the controller alongside Spring 6.1's built-in method validation. Changes which exception parameter validation throws. | Dropped the annotation; the handler covers both exception types regardless. |
| 5 | The name regex constant declared inside the record it annotated.                                                                       | Moved to a `NameRules` class so the constant is unambiguous and shared with the frontend rule and the tests. |
| 6 | `userEvent.type` against `<input type="date">`. jsdom does not model the browser's date-segment behaviour, so this is flaky.           | `fireEvent.change` with the ISO value.                                      |
| 7 | `defineConfig` imported from `vite` while declaring a `test` block.                                                                     | Imported from `vitest/config`.                                              |
| 8 | `vite@^6` paired with `vitest@^2`. Tests pass; `tsc --noEmit` fails with a 30-line type error, because vitest 2 installs its own Vite 5 and the two `Plugin` types are distinct. | Upgraded to `vitest@^3`, which shares the top-level Vite. Caught only because the typecheck is a separate script from the test run. |

The pattern worth naming: none of these were syntax errors. They were plausible-looking code that
compiles and fails later — a null timestamp, a test slice that will not start, a flaky test. That is
what review has to be looking for, and it is why "it runs" is not validation.

## Time

| Phase                                    | With AI       | Estimated without |
| ---------------------------------------- | ------------- | ----------------- |
| Design decisions and API shape           | `[20 mins]`   | `[40 min]`        |
| Backend implementation                   | `[5 min]`     | `[20 min]`        |
| Frontend implementation                  | `[20 min]`    | `[60 min]`        |
| Tests                                    | `[30 min]`    | `[75 min]`        |


## Effect on the process

AI removed almost all of the typing and none of the judgement. The speedup was concentrated in
boilerplate — configuration, DTOs, CSS, the first pass at tests — and it was close to zero on the
parts that decide whether this is a good submission: what to do about duplicates, where the
concurrency hole is, which validation rules exclude real people.

Two things changed in how I worked. First, review became the expensive step rather than writing, and
it is a different kind of reading: the failure mode is confident, idiomatic-looking code with a
subtle behavioural bug, so skimming for style catches nothing. Second, the time saved went into
things that would otherwise have been cut for time - the concurrency handling on duplicate creation,
the out-of-order response guard in the list hook, the leap-day age cases. That is the honest benefit:
not a faster CRUD app, but a wider margin for the details.

The risk I would flag to a team adopting this: an AI-generated test suite written alongside its
implementation asserts what the code does. Reversing that — breaking the code to prove the test
fails — is the only cheap way I know to tell a real suite from a green one.

