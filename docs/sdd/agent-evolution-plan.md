# RecruitAgent Agent Evolution SDD

## 1. Objective

Turn the existing AI-assisted recruitment application into an observable and
measurable recruitment conversation agent. The first release focuses on one
end-to-end workflow instead of adding unrelated autonomous agents.

The target workflow is:

1. receive a candidate message;
2. classify intent and risk;
3. assemble bounded conversation memory;
4. route to knowledge retrieval, a business tool, general reply generation,
   or human handoff;
5. record retrieval, routing, tool, model, and handoff trace events;
6. evaluate the trajectory against a versioned golden data set.

## 2. Constraints

- JDK 17 and Spring Boot 3.3.x remain the application platform.
- Existing HTTP endpoints remain backward compatible in the first release.
- MySQL remains the source of truth for recruitment business data.
- PostgreSQL with pgvector remains the knowledge retrieval store.
- Existing `LlmService` and `EmbeddingService` ports remain valid.
- Agent frameworks must not leak into domain services.
- All model-generated actions with business impact require permission checks;
  salary promises, rejection, offer decisions, complaints, and low-confidence
  decisions require human handoff.

## 3. Capability Boundaries

### Retrieval

Retrieval owns dense and lexical recall, metadata filtering, rank fusion, and
retrieval diagnostics. It does not generate answers. Reciprocal Rank Fusion
must be deterministic and preserve source-level evidence.

### Conversation Core

Conversation core owns intent classification, risk signals, bounded memory,
and routing decisions. It does not execute external tools directly. A routing
decision must include the selected route, confidence, handoff flag, and reason.

### Tool Runtime

Tool runtime owns registration, validation, authorization, invocation,
timeout, retry, idempotency, and trace emission. Domain tools are adapters on
top of this runtime. MCP is an external protocol adapter, not the core runtime.

### Evaluation

Evaluation owns deterministic metric calculation and versioned golden cases.
It must not call a live model while calculating metrics. Live run outputs are
captured first and scored separately so results remain reproducible.

## 4. Integration Contract

Every end-to-end run uses a stable `traceId` and `conversationId`. Components
should expose enough information to produce these logical events:

- `intent.classified`
- `route.selected`
- `retrieval.started`
- `retrieval.completed`
- `tool.started`
- `tool.succeeded` or `tool.failed`
- `model.started`
- `model.succeeded` or `model.failed`
- `handoff.requested`
- `run.completed`

Failures must be explicit. Retrieval errors must not be indistinguishable from
valid empty retrieval, and tool timeouts must not be reported as model errors.

## 5. Golden Data

Golden cases are curated development fixtures, not production data. Each case
must declare its provenance and expected behavior. The initial set covers:

- job responsibility questions;
- salary and offer commitment risks;
- interview scheduling;
- candidate status questions;
- complaints and human handoff;
- out-of-domain messages;
- empty retrieval;
- tool timeout and retry;
- invalid tool arguments;
- conflicting conversation facts.

## 6. Acceptance Metrics

The first integrated release reports at least:

- intent accuracy and macro F1;
- route and tool-selection exact match;
- tool-argument validity rate;
- retrieval Recall@K and MRR;
- end-to-end task success rate;
- human-handoff precision, recall, and F1;
- average and percentile latency where samples are available;
- prompt and completion token totals;
- failure counts grouped by component and error code.

No fixed quality target is claimed before the first baseline evaluation. The
baseline result becomes the comparison point for prompt, model, retrieval, and
routing changes.

## 7. Delivery Order

1. land evaluation and trace foundations;
2. land hybrid retrieval;
3. land intent, routing, and bounded memory;
4. connect recruitment business tools to the tool runtime;
5. implement the graph workflow after the component contracts stabilize;
6. run the golden suite and publish the baseline report;
7. add MCP and LangGraph4j adapters only after the framework-neutral workflow
   passes the same contract tests.

## 8. Definition Of Done

- Existing backend tests continue to pass.
- New components have deterministic unit tests for success and failure paths.
- Database migrations are additive and idempotent under Flyway rules.
- A failed dependency produces a traceable error or human handoff, never a
  silently fabricated answer.
- The golden evaluation suite can compare two run result files without access
  to a live AI provider.
- Documentation distinguishes implemented capability from planned adapters.

## 9. Implementation Status

Implemented in the current backend:

- deterministic intent/risk classification, bounded memory, and routing;
- guarded tool runtime and read-only recruitment tools;
- pgvector plus PostgreSQL FTS hybrid retrieval with RRF and diagnostics;
- LangGraph4j recruitment conversation workflow;
- per-node agent traces with MySQL persistence and an administrator query API;
- versioned golden cases and deterministic offline evaluation metrics.

Still intentionally deferred:

- an MCP transport adapter;
- a real interview calendar write tool;
- a unified view across graph, tool, retrieval, and model trace streams;
- a dedicated BM25 engine and reranker;
- production baseline reports generated from reviewed real conversations.
