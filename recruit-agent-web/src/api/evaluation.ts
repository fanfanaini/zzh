import request from './request'
import type {
  EvaluationReport,
  RetrievalComparisonReport,
  RetrievalRankingSample,
  RetrievalQualityReport,
} from '@/types/evaluation'

export const evaluationApi = {
  /** 运行核心基线评估（基于内置黄金数据集） */
  coreBaseline() {
    return request.get<EvaluationReport>('/evaluations/core-baseline')
  },

  /** 运行人工审核基线评估 */
  reviewedBaseline(jsonl: string) {
    return request.post<EvaluationReport>('/evaluations/reviewed-baseline', jsonl, {
      headers: { 'Content-Type': 'application/x-ndjson' },
    })
  },

  /** 比较检索方法（baseline vs reranked） */
  compareRetrieval(samples: RetrievalRankingSample[], k = 5) {
    return request.post<RetrievalComparisonReport>('/evaluations/retrieval-comparison', samples, {
      params: { k },
    })
  },

  /** 获取检索质量报告 */
  retrievalQualityReport(k = 5) {
    return request.get<RetrievalQualityReport>('/evaluations/retrieval-quality-report', {
      params: { k },
    })
  },
}
