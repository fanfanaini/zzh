# Learn Claude Code Adoption Review

## Conclusion

`D:/learn-claude-code` is best treated as an Agent harness design catalogue,
not as a runtime dependency. Its Python implementation is intentionally
educational and simplifies MCP transport, authorization, session lifecycle,
and concurrency. RecruitAgent should retain its Java 17, Spring Boot,
LangGraph4j, ToolRuntime, trace, and evaluation foundations.

## Ideas worth adopting

- Keep a stable execution loop and add permissions, tracing, memory, recovery,
  and context management through interceptors around graph nodes.
- Give every specialized agent an explicit input/output schema, isolated
  context, minimal tool set, maximum step count, and no uncontrolled recursion.
- Register MCP-discovered tools in the existing ToolRuntime instead of creating
  a parallel execution path. Namespace remote tools to prevent collisions.
- Preserve tool-call/tool-result pairs during context compression.
- Classify failures before retrying: rate limit, provider overload, timeout,
  context overflow, invalid arguments, permission denial, and business errors
  require different policies.
- Correlate asynchronous work with stable request, task, conversation, and trace
  identifiers and reject duplicate responses.

## P0 adoption plan

1. Add an `AgentExecutionContext` carrying tenant, user, roles, permissions,
   trace ID, task ID, conversation ID, prompt version, and model version.
2. Add graph-node interceptors that emit STARTED, SUCCEEDED, and FAILED events
   with duration, attempt, parent span, safe input/output summaries, and error code.
3. Replace character-only memory truncation with token budgets, durable summaries,
   structured facts, and compaction invariant tests.
4. Add a recovery matrix: bounded exponential backoff with jitter for transient
   provider failures, no retry for permission/argument errors, one compaction
   retry for context overflow, and an explicit fallback model policy.
5. Persist checkpoints for long-running resume parsing, screening, and reply
   workflows so execution can continue from the failed node after restart.
6. Extend evaluation from the deterministic core to the complete LangGraph
   workflow using reviewed, anonymized recruitment conversations.

Implemented in the current iteration:

- unified serializable `AgentExecutionContext` with tenant, identity, roles,
  permissions, trace, conversation, task, prompt, and model fields;
- Spring Security identity and role-to-permission mapping before graph execution;
- node lifecycle interception with STARTED/SUCCEEDED/FAILED, duration, attempt,
  execution identity, node details, and typed failure metadata;
- ToolRuntime authorization from the execution context and safe HR handoff when
  verified tool data is unavailable.
- configurable token-budgeted memory with Unicode-safe compaction;
- incremental durable conversation summaries with local and MySQL repositories;
- one context-overflow compaction retry followed by deterministic fallback;
- typed LLM failure classification with bounded exponential backoff and jitter;
- durable MySQL workflow checkpoints and startup recovery for resume parsing and
  distributed batch screening;
- Redis-backed atomic tool idempotency scoped by tool, tenant, principal, and
  caller key;
- correlated graph, retrieval, tool, and model traces with a mandatory
  sensitive-data boundary;
- strict import of reviewed anonymous conversations into the deterministic
  evaluation pipeline.

## P1 adoption plan

- Introduce typed subgraphs for job analysis, resume parsing, screening review,
  candidate communication, and safety review.
- Add durable business memory with source, confidence, scope, expiry, sensitivity,
  last confirmation time, audit history, and deletion support.
- Pilot a read-only MCP integration such as recruiting policy search or calendar
  availability. All calls must pass through ToolRuntime authorization and traces.
- Persist tool traces and export tool latency, timeout, retry, and error metrics.

## What not to copy

- Python global dictionaries, raw threads, or local Markdown files as runtime state.
- File mailboxes that read and delete messages without locking.
- String matching as a security boundary.
- Simulated MCP without transport authentication and lifecycle management.
- Passing every exception back to the model as an untyped string.
- Regex extraction of model-produced JSON.
- Autonomous coding-agent teams and worktree orchestration; they do not directly
  improve the recruitment product.

## Source pointers

- Stable harness loop: `D:/learn-claude-code/README.md`
- Permission model: `D:/learn-claude-code/s03_permission/code.py`
- Hooks: `D:/learn-claude-code/s04_hooks/code.py`
- Isolated subagents: `D:/learn-claude-code/s06_subagent/code.py`
- Context compaction: `D:/learn-claude-code/s08_context_compact/code.py`
- Memory selection: `D:/learn-claude-code/s09_memory/code.py`
- Error recovery: `D:/learn-claude-code/s11_error_recovery/code.py`
- MCP discovery: `D:/learn-claude-code/s19_mcp_plugin/code.py`
- Integrated harness: `D:/learn-claude-code/s20_comprehensive/code.py`
