# Backend Step 12 - 保存简历筛选评估

## 目标

在同步筛选接口返回 `ScreeningResult` 的同时，保存一条 `resume_evaluation` 记录，用于后续列表查询、人工复核和模型链路追踪。

## 范围

1. 新增 `Evaluation` 领域对象。
2. 新增 `EvaluationRepository` 抽象。
3. 提供本地内存实现，支持 `local` 环境和单元测试。
4. 提供 MyBatis-Plus 持久化实现，对应 MySQL 表 `resume_evaluation`。
5. `ScreeningApplicationService` 在筛选完成后保存评估记录。

## 评估记录字段映射

- `job_id`：岗位 ID。
- `candidate_id`：候选人 ID。
- `rule_score`：当前规则筛选得分，暂用 `finalScore`。
- `semantic_score`：语义得分，当前阶段还未接入向量检索，保持为空。
- `final_score`：最终分。
- `recommendation`：筛选建议。
- `confidence`：当前阶段按最终分归一化为 `finalScore / 100`。
- `advantages`：优势列表 JSON。
- `risks`：风险列表 JSON。
- `missing_information`：缺失信息列表 JSON。
- `model_name`、`prompt_version`、`human_result`：为后续 AI 和人工流程预留，当前为空。

## TDD 验证

1. `EvaluationPoConverterTest` 验证领域对象和 MySQL PO 的双向转换。
2. `ScreeningApplicationServiceTest` 验证筛选完成后保存一条评估记录。
3. 全量运行 `mvn -s .mvn/settings.xml test`。
