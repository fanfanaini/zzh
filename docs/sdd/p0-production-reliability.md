# P0 Production Reliability SDD

## Objective

Make long-running recruitment workflows recoverable and make external
dependencies fail predictably without duplicating side effects.

## Durable workflow checkpoints

A checkpoint identifies a task, a workflow node, and a business item. A
successful checkpoint is durable under the `mysql` profile and may be reused
after an application restart.

Required resume parsing checkpoints:

- `TEXT_EXTRACTED`
- `PROFILE_EXTRACTED`

Required batch screening checkpoint:

- `CANDIDATE_SCREENED`, scoped by candidate ID

Retrying a task must skip successful checkpoints. A failed checkpoint may be
attempted again. Task counters must describe the current execution outcome and
must not create duplicate evaluations.

## Model recovery matrix

Provider failures are classified before retry:

| Failure | Retry policy |
| --- | --- |
| `CONTEXT_OVERFLOW` | Compact context and retry once |
| `RATE_LIMIT` | Bounded exponential backoff with jitter |
| `PROVIDER_OVERLOADED` | Bounded exponential backoff with jitter |
| `TIMEOUT` | Bounded exponential backoff with jitter |
| `INVALID_REQUEST` | Do not retry |
| `AUTHENTICATION` | Do not retry |
| `UNKNOWN` | Do not retry |

Backoff and jitter must be injectable so tests never sleep. Each provider
attempt must emit a model-call log with the classified failure. Exhaustion must
produce an explicit deterministic fallback rather than an unbounded retry.

## Tool idempotency

The idempotency scope is:

`tool name + principal ID + tenant ID + caller idempotency key`

The production store must atomically claim work before the handler runs. A
concurrent caller must either replay a completed result or observe that the
same operation is in progress; it must not execute the handler again.

Only successful results are retained for replay. Failed claims are released so
the operation can be retried. Entries have a configurable TTL. Local
development keeps the in-memory implementation; the `distributed` profile
uses Redis.

## Acceptance

- Resume parsing resumes after text extraction without reading the file again.
- Batch screening skips candidates already completed by the same task.
- Restarting the application does not erase MySQL checkpoints or Redis
  idempotency results.
- Authentication and invalid-request model failures are attempted once.
- Transient model failures stop at the configured maximum attempt count.
- Concurrent identical idempotent tool calls execute the handler at most once.
- Existing HTTP contracts remain backward compatible.
- Focused tests and the full Maven test suite pass.
