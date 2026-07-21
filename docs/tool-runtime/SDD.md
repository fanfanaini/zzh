# Tool Runtime SDD

## Purpose

The tool runtime is a framework-neutral execution boundary between an agent orchestrator and recruitment business capabilities. It does not depend on Spring AI, LangChain4j, LangGraph4j, or MCP.

## Contracts

- `ToolDescriptor` defines a stable name, human-readable description, parameter contract, required permissions, timeout, retry limit, and idempotency behavior.
- `ToolParameterContract` validates required fields, basic JSON-compatible types, and unknown fields before business code runs.
- `ToolRegistry` owns descriptors and handlers and rejects duplicate names.
- `ToolInvocationContext` carries the invocation ID, principal, permissions, idempotency key, and orchestration attributes.
- `ToolResult` returns success data or a standardized `ToolError`; tool failures do not escape as runtime exceptions.
- `ToolTraceSink` receives one terminal trace for every invocation, including validation and permission failures.

## Execution Order

1. Resolve the registered tool.
2. Validate arguments.
3. Check all required permissions.
4. Validate and look up the idempotency key when required.
5. Execute with the descriptor timeout.
6. Retry only failures explicitly marked retryable, with an absolute maximum of three attempts.
7. Store successful idempotent results.
8. Emit the terminal trace.

This order prevents unauthorized calls from probing the idempotency store and prevents invalid calls from reaching business services.

## Error Codes

`TOOL_NOT_FOUND`, `INVALID_ARGUMENTS`, `PERMISSION_DENIED`, `IDEMPOTENCY_KEY_REQUIRED`, `TIMEOUT`, `EXECUTION_FAILED`, `RETRY_EXHAUSTED`, `INTERRUPTED`, and `DUPLICATE_TOOL` form the transport-neutral error vocabulary.

## Idempotency

The in-memory store is suitable for tests and a single-process development runtime. Production wiring should implement `ToolIdempotencyStore` with Redis or a durable database. The key scope is tool name, principal ID, and caller-provided idempotency key. Only successful results are cached, so a failed call can be retried later.

## Trace Safety

The current sink keeps traces in memory and is intended as a foundation. Tool adapters must not put secrets, resume files, access tokens, or unrestricted personal data in argument maps. A production sink should add redaction, retention, correlation IDs, and durable storage.

## MCP Adapter Boundary

No network MCP client or server is included in this change. A future adapter should:

1. Export registry descriptors as MCP tool schemas.
2. Convert MCP JSON arguments into the existing parameter contract.
3. Build `ToolInvocationContext` from authenticated transport metadata.
4. Invoke `ToolRuntime` without bypassing validation, permissions, timeout, retries, idempotency, or tracing.
5. Map `ToolResult` and `ToolErrorCode` back to MCP result/error payloads.

Business handlers must remain unaware of MCP. The same registry can therefore serve an internal LangGraph4j node, an MCP server, or a deterministic application service.

## Known Boundaries

- The basic parameter contract validates top-level types; nested JSON Schema constraints belong in a future schema adapter.
- The in-memory idempotency implementation prevents sequential duplicate effects. Cross-instance and concurrent exactly-once behavior requires a distributed atomic store.
- Timeout cancellation is cooperative. A handler that ignores interruption may continue running, so external clients must also enforce their own request deadlines.
