# SDD: 后端第 10 步

## 目标

增加“简历原文 -> 候选人画像”的结构化抽取端口，并先提供规则版实现。

## 端口

| 类型 | 说明 |
| --- | --- |
| `ResumeProfileExtractor` | 从 `rawText` 抽取候选人画像草稿 |
| `CandidateProfileDraft` | 抽取结果，包含学历、年限、城市、技能 |
| `RuleBasedResumeProfileExtractor` | 规则版 MVP 实现 |

## 应用服务

`CandidateProfileExtractionService` 负责：

1. 根据 `resumeId` 查询简历。
2. 确认简历已经有 `rawText`。
3. 查询候选人。
4. 调用 `ResumeProfileExtractor` 抽取画像草稿。
5. 将草稿写回候选人记录。

## API

```http
POST /api/resumes/{id}/extract-profile
```

如果简历尚未完成文本提取，返回：

```http
409 Conflict
```

## 规则版抽取范围

当前规则版只做 MVP 级识别：

| 字段 | 实现方式 |
| --- | --- |
| 技能 | 技能词典和别名匹配 |
| 工作年限 | 正则识别 `3年`、`2 years` 等表达 |
| 城市 | 常见城市词典匹配 |
| 学历 | 博士、硕士/研究生、本科、大专/专科关键词 |

该实现用于跑通链路，不作为最终 AI 抽取能力。后续接入大模型 Structured Output 时，只需要替换 `ResumeProfileExtractor` 适配器。

## TDD 覆盖

1. `RuleBasedResumeProfileExtractorTest` 验证技能、年限、城市、学历抽取。
2. `CandidateProfileExtractionServiceTest` 验证画像写回候选人。
3. `ResumeControllerTest` 验证画像抽取接口和 `409 Conflict`。

## 下一步

1. 将岗位画像和候选人画像接入筛选 API。
2. 实现 `POST /api/jobs/{jobId}/screen`。
3. 保存 `resume_evaluation` 评估结果。
