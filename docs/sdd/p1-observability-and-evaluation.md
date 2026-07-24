# P1 Observability, Privacy, And Evaluation SDD

## Objective

Make one recruitment-agent execution explainable from routing through
retrieval, tools, and model calls while preventing secrets and candidate
identifiers from entering traces or evaluation fixtures.

## Unified trace contract

All runtime events reuse `AgentTraceSink` and the existing `agent_trace` table.
Every event carries these normalized details:

- `component`: `AGENT`, `RETRIEVAL`, `TOOL`, or `MODEL`;
- `operation`: stable machine-readable operation name;
- `spanId`: identifier for one component operation;
- `parentSpanId`: optional caller span;
- `durationMillis`: present on terminal events;
- `errorCode`: present on failed events.

`traceId` identifies one end-to-end execution and `conversationId` identifies
the business conversation. Component-specific details may be added, but all
details must pass through the sensitive-data redactor before persistence.

The existing `GET /api/agent-traces` contract remains compatible and gains
optional component and status filters plus normalized fields in each response.

## Privacy boundary

Trace and imported evaluation payloads must redact:

- phone numbers;
- email addresses;
- Chinese resident identity numbers;
- bearer tokens and API keys.

Redaction is recursive for strings, maps, collections, and arrays. It preserves
field names and collection shape so failures remain diagnosable. Raw candidate
messages may be evaluated only after this boundary has accepted or sanitized
them.

## Reviewed conversation evaluation

Imported JSONL records use an explicit schema version and anonymous case ID.
The importer rejects direct identity fields and converts reviewed labels into
the existing `EvaluationCase` contract. It does not call a live model.

Baseline reports remain deterministic and include dataset version, case count,
classification, retrieval, task, handoff, latency, token, and error metrics.

## Acceptance

- A single trace query returns agent, retrieval, tool, and model events in time
  order when those components participate.
- MySQL and local profiles expose the same trace response shape.
- Tool events are no longer stored only in process memory.
- Trace details never contain configured sensitive patterns in plaintext.
- Reviewed conversation JSONL is validated and converted deterministically.
- Existing APIs remain backward compatible.
- All backend tests pass.
