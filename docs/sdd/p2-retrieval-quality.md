# P2 Retrieval Quality SDD

## Objective

Upgrade lexical retrieval from PostgreSQL FTS ranking to a real BM25 engine,
then apply a bounded second-stage reranker without weakening dense-retrieval
fallback behavior.

## Source of truth

PostgreSQL with pgvector remains the durable source of knowledge chunks.
Lucene is a rebuildable in-process sparse index:

- rebuild all chunks when the vector profile becomes ready;
- update the index after a successful knowledge-chunk write;
- remove indexed chunks after a successful document deletion;
- never delete PostgreSQL data because a Lucene operation fails.

An index synchronization failure degrades to dense retrieval and is repaired by
the next rebuild. It must not make the durable knowledge write appear rolled
back when it was already committed.

## BM25

Use Apache Lucene 9.12.3 with `BM25Similarity`. Default tuning is `k1=1.2` and
`b=0.75`. SmartCN analyzes Chinese and mixed Chinese/English recruitment text.
Document ID and source filters must preserve the existing `RetrievalRequest`
contract.

## Reranking

RRF continues to combine dense and BM25 candidates. A second-stage reranker
receives only the bounded fused candidates and returns the requested limit.
The deterministic baseline reranker combines:

- normalized RRF score;
- normalized dense score;
- normalized BM25 score;
- exact query-term coverage;
- source-name term coverage.

Weights are configurable and sum-normalized. If reranking fails, the original
RRF order is returned and the trace is marked degraded.

## Evaluation

Retrieval tests must cover Chinese and English matching, metadata filters,
incremental update, deletion, restart rebuild, deterministic ordering, reranker
fallback, and Recall@K/MRR compatibility.

## Acceptance

- Sparse results are produced by Lucene `BM25Similarity`.
- Knowledge indexed before restart is searchable after index rebuild.
- New and deleted documents are reflected without restarting.
- Dense-only fallback remains available.
- Reranking never expands the candidate set.
- Existing API contracts remain compatible.
- Full backend tests pass.
