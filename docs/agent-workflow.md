# Recruitment Conversation Agent

## Implemented workflow

The reply suggestion use case now runs through a LangGraph4j state graph:

`memory -> classify -> route -> tool/handoff/safe reply -> generation`

The graph keeps the framework at the application boundary. Intent, memory,
routing, tool runtime, retrieval, and evaluation still expose framework-neutral
Java contracts and can be tested without LangGraph4j.

## Routes

- `JOB_INFO` calls `get_job_info` before model generation.
- `CANDIDATE_STATUS` calls `get_candidate_status` before model generation.
- `FAQ_RAG` calls `search_knowledge` and injects retrieved evidence.
- `INTERVIEW_SCHEDULING` records a handoff because no calendar write tool is configured.
- `HUMAN_HANDOFF` creates a draft but forces HR review for salary, employment decisions,
  complaints, legal issues, and sensitive personal data.
- `SAFE_REPLY` handles ordinary out-of-domain questions without calling the model.
- `GENERAL_REPLY` uses the configured reply generator with bounded memory.

## Tool safety

Tools are registered through `ToolRuntime`. The runtime validates arguments and
permissions, applies timeouts and bounded retries, supports idempotency, and emits
tool traces. The current recruitment tools are read-only and require
`recruit.read`.

## Retrieval

Knowledge search combines pgvector dense recall and PostgreSQL full-text lexical
recall with deterministic Reciprocal Rank Fusion. This is hybrid retrieval, but
PostgreSQL full-text ranking is not claimed to be a strict BM25 implementation.
Retrieval reports channel failures separately from valid empty results and only
fails the run when both channels fail.

## Observability

Administrators can inspect a graph run with:

`GET /api/agent-traces?conversationId={id}`

or:

`GET /api/agent-traces?traceId={traceId}`

With the `mysql` profile, agent traces are persisted in the `agent_trace` table
and survive application restarts. The local profile keeps an in-memory sink for
fast isolated development. Model and tool monitoring use their existing sinks.

## Evaluation

The versioned JSONL golden data set and offline evaluator report intent macro F1,
route and tool exact match, retrieval Recall@K/MRR, task success, handoff
precision/recall/F1, latency, token usage, and failures by component and code.
Scoring is deterministic and does not call a live model.

## Deliberate limitations

- There is no calendar write integration, so scheduling cannot be falsely reported
  as completed.
- MCP is not yet exposed as a transport; the tool runtime is ready for an MCP adapter.
- Lexical retrieval uses PostgreSQL FTS rather than a dedicated BM25 engine.
- Model, tool, and graph traces are still separate streams; a unified trace view
  is a later observability improvement.
