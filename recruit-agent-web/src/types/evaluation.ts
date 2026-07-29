export interface ClassificationMetrics {
  evaluatedCases: number
  accuracy: number
  macroF1: number
}

export interface ExactMatchMetrics {
  evaluatedCases: number
  matches: number
  exactMatchRate: number
}

export interface RetrievalMetrics {
  k: number
  evaluatedCases: number
  recallAtK: number
  meanReciprocalRank: number
}

export interface RankingMetrics {
  recallAtK: number
  mrrAtK: number
  ndcgAtK: number
  hitRateAtK: number
}

export interface BinaryClassificationMetrics {
  evaluatedCases: number
  truePositives: number
  falsePositives: number
  falseNegatives: number
  trueNegatives: number
  precision: number
  recall: number
  f1: number
}

export interface RuntimeMetrics {
  latencySamples: number
  averageLatencyMillis: number
  p50LatencyMillis: number
  p95LatencyMillis: number
  promptTokens: number
  completionTokens: number
  totalTokens: number
}

export interface EndToEndMetrics {
  evaluatedCases: number
  successfulCases: number
  successRate: number
  expectedOutcomeMatchRate: number
}

export interface EvaluationReport {
  datasetVersion: string
  intent: ClassificationMetrics
  routing: ExactMatchMetrics
  toolSelection: ExactMatchMetrics
  retrieval: RetrievalMetrics
  endToEnd: EndToEndMetrics
  handoff: BinaryClassificationMetrics
  runtime: RuntimeMetrics
  failureCounts: Record<string, number>
}

export interface RetrievalComparisonReport {
  cases: number
  k: number
  candidateRecall: number
  baseline: RankingMetrics
  reranked: RankingMetrics
  averageRerankLatencyMillis: number
  degradedRate: number
}

export interface RetrievalMetricDelta {
  recallAtK: number
  mrrAtK: number
  ndcgAtK: number
  hitRateAtK: number
}

export interface RetrievalWeakCase {
  caseId: string
  baselineReciprocalRank: number
  rerankedReciprocalRank: number
  baselineNdcg: number
  rerankedNdcg: number
  reason: string
}

export interface RetrievalQualityReport {
  datasetVersion: string
  generatedAt: string
  comparison: RetrievalComparisonReport
  delta: RetrievalMetricDelta
  verdict: string
  weakCases: RetrievalWeakCase[]
}

export interface RetrievalRankingSample {
  caseId: string
  relevantChunkIds: number[]
  relevanceGrades: Record<number, number>
  candidateChunkIds: number[]
  baselineChunkIds: number[]
  rerankedChunkIds: number[]
  rerankLatencyMillis: number
  degraded: boolean
}

export interface EvaluatedCase {
  caseId: string
  conversationId: string
  expectedIntent: string
  actualIntent: string | null
  expectedRoute: string
  actualRoute: string | null
  expectedTool: string | null
  actualTool: string | null
  relevantChunkIds: number[]
  retrievedChunkIds: number[]
  success: boolean
  latencyMillis: number
  error: string | null
}
