# Offline Agent Evaluation SDD

## Purpose

Provide deterministic, versioned evaluation for captured recruitment Agent runs. Metric calculation never calls a live model, tool, database, or retrieval service. Producers run the Agent separately and write one result per golden case; this module only validates, joins, and scores those files.

## Data contract

- Cases use `agent-evaluation-case/v1` and results use `agent-evaluation-result/v1`.
- Files are UTF-8 JSONL with one independent JSON object per non-blank line.
- `datasetVersion` must match across every case and result.
- `caseId` is unique and result coverage must exactly equal case coverage.
- `expectedTool` and `selectedTool` use `NONE` when no tool is expected or selected. This makes unwanted tool calls count as errors.
- Null latency and token fields mean unavailable and are excluded from sample counts. Zero is a measured value.
- `provenance` identifies how a case was obtained. The bundled dataset is curated and contains no production personal data.

JSON Schemas are in `docs/evaluation/schemas`. The Java parser also enforces schema versions, required semantic fields, duplicate IDs, and non-negative runtime measurements.

## Metrics

| Metric | Definition |
| --- | --- |
| Intent accuracy | Correct intent predictions divided by all cases. |
| Intent macro-F1 | Unweighted mean of per-label F1 over the union of expected and predicted labels. |
| Route exact match | Exact, case-sensitive equality of expected and selected routes. |
| Tool exact match | Exact, case-sensitive equality of expected and selected tools, including `NONE`. |
| Recall@K | Per query, unique relevant documents in the first K results divided by all relevant documents; averaged over cases with relevance labels. |
| MRR@K | Reciprocal rank of the first relevant document in the first K results; averaged over cases with relevance labels. |
| Task success rate | Captured successful runs divided by all runs. |
| Outcome match | Captured task success matching expected success, including intentional failure-injection cases. |
| Handoff precision/recall/F1 | Binary metrics against `expectedHandoff`; zero denominators report zero. |
| Runtime | Mean, p50, and p95 latency using linear interpolation over sorted samples. |
| Tokens | Prompt, completion, and combined totals over available samples. |
| Failures | Count grouped by `errorComponent:errorCode`. |

Retrieval cases without `relevantDocumentIds` are excluded from Recall@K and MRR. Duplicate retrieved IDs count once for recall; their first appearance determines reciprocal rank.

## Components

- `EvaluationJsonlParser` reads and validates versioned files.
- `AgentEvaluationService` joins in-memory records and calculates metrics.
- `OfflineEvaluationRunner` is the framework-neutral file entry point.
- `recruitment-golden-v1.jsonl` contains ten curated cases for job questions, salary risk, scheduling, status, complaints, out-of-domain requests, empty retrieval, timeout, invalid arguments, and conflicting memory.

`evaluation/recruitment-agent-core-v2.jsonl` is the executable core-routing
baseline. It uses the intent, route, tool, and handoff vocabulary implemented by
the current Java agent. `GET /api/evaluations/core-baseline` executes the current
classifier and routing policy instead of reading a captured result file. The
endpoint is administrator-only. Retrieval quality remains a separate seeded
integration baseline because the core suite intentionally has no database or
live model dependency.

The backend CI executes this suite with every backend change and uploads
`core-baseline-report.json` together with the Surefire reports. The current
core suite is a routing safety baseline, not evidence of retrieval or answer
quality.

The generic offline scorer has no persistence requirement. The core-baseline
controller is a deterministic adapter over the same metric contracts.

## Evolution rules

Add optional backward-compatible fields without changing the schema identifier. Any renamed field, changed meaning, or new required field creates a new schema and adapter. Golden cases are immutable within a dataset version; corrections produce a new dataset version so historical reports remain reproducible.

## Acceptance criteria

- Malformed, unsupported, duplicate, missing, unexpected, or cross-version records fail fast.
- Metric tests use hand-calculated expected values.
- The bundled golden dataset parses and has unique IDs and provenance.
- Focused tests run without Spring context or external infrastructure.
