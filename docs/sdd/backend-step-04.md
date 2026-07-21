# SDD: 后端第 4 步

## 目标

实现候选人画像和简历元数据模块，为后续 PDF/DOCX 解析、AI 结构化抽取、筛选评分提供输入。

## 当前能力

```http
POST /api/candidates
GET  /api/candidates/{id}
GET  /api/candidates
POST /api/candidates/{id}/resumes
GET  /api/candidates/{id}/resumes
```

## 领域对象

| 对象 | 说明 |
| --- | --- |
| `Candidate` | 候选人画像，保存姓名、脱敏联系方式、学历、年限、期望城市、技能 |
| `ResumeDocument` | 简历文件元数据，保存文件名、类型、大小、存储对象名、解析状态、原始文本 |
| `ResumeParseStatus` | `UPLOADED`、`PARSING`、`PARSED`、`FAILED` |

## 设计决策

1. 这一阶段只记录简历元数据，不直接处理文件流。
2. `phoneHash`、`emailHash` 默认保存脱敏/哈希值，避免在业务库中直接扩散敏感信息。
3. 简历解析结果先预留 `rawText` 字段；结构化结果后续放入候选人教育、经历、项目、技能等明细表。
4. 默认仍使用内存仓储，MySQL 持久化会在下一步按同样端口适配器模式补齐。

## TDD 覆盖

当前轻量 Spec 覆盖：

1. 注册候选人后可以按 ID 查询。
2. 技能会在领域层标准化为小写。
3. 只有已存在候选人才能添加简历。
4. 新增简历的初始状态为 `UPLOADED`。

## 下一步

1. 为候选人和简历增加 MyBatis-Plus 持久化适配器。
2. 增加真实文件上传接口，先落 MinIO 或本地对象存储抽象。
3. 引入文档解析端口，支持 Tika/PDFBox/POI。
4. 将简历原文解析为 `CandidateProfile`，接入已有筛选服务。
