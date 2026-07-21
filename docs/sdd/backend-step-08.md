# SDD: 后端第 8 步

## 目标

增加简历文本提取端口，先支持纯文本简历，为后续 PDF/DOCX/OCR 接入建立稳定边界。

## 新增端口

| 类型 | 说明 |
| --- | --- |
| `DocumentTextExtractor` | 文档文本提取端口 |
| `DocumentContent` | 输入文档内容 |
| `ExtractedText` | 提取出的原始文本 |
| `PlainTextDocumentTextExtractor` | 纯文本适配器 |

## 应用服务

`CandidateResumeTextExtractionService` 负责：

1. 根据 `resumeId` 查询简历元数据。
2. 将状态更新为 `PARSING`。
3. 从 `FileStorage` 读取文件内容。
4. 调用 `DocumentTextExtractor` 提取文本。
5. 将 `rawText` 写回简历记录，并把状态更新为 `PARSED`。
6. 失败时把状态更新为 `FAILED`。

## API

```http
POST /api/resumes/{id}/extract-text
```

当前只支持 `text/plain`、`.txt`、`.md`。PDF、DOCX、OCR 在后续通过新适配器接入，不改变应用服务。

## TDD 覆盖

1. `PlainTextDocumentTextExtractorTest` 验证 UTF-8 文本提取和不支持格式拒绝。
2. `LocalFileStorageTest` 验证文件读取能力。
3. `CandidateResumeTextExtractionServiceTest` 验证解析状态和 `rawText` 写回。
4. `ResumeControllerTest` 验证解析接口成功和 404 行为。

## 下一步

1. 增加 Tika/PDFBox/POI 依赖。
2. 实现 PDF/DOCX 文本提取适配器。
3. 解析成功后触发候选人画像结构化抽取。
