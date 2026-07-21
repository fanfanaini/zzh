# SDD: 后端第 9 步

## 目标

接入 PDF/DOC/DOCX 文本提取适配器，并保持应用层只依赖统一的 `DocumentTextExtractor` 端口。

## 技术选择

本阶段使用 Apache Tika：

| 依赖 | 用途 |
| --- | --- |
| `tika-core` | Tika 基础 API |
| `tika-parsers-standard-package` | PDF、Office 等标准解析器集合 |

Tika 底层会使用 PDFBox、POI 等解析器，因此第一版不直接绑定具体文件格式库。

## 提取器结构

```text
DocumentTextExtractor
        ^
        |
CompositeDocumentTextExtractor
        |
        +-- PlainTextDocumentTextExtractor
        +-- TikaDocumentTextExtractor
```

`CompositeDocumentTextExtractor` 按顺序选择第一个支持当前文档的适配器。

## 当前支持

| 类型 | 处理方式 |
| --- | --- |
| `text/plain`、`.txt`、`.md` | 纯文本提取器 |
| `application/pdf`、`.pdf` | Tika |
| `application/msword`、`.doc` | Tika |
| `application/vnd.openxmlformats-officedocument.wordprocessingml.document`、`.docx` | Tika |

## TDD 覆盖

1. `CompositeDocumentTextExtractorTest` 验证路由和不支持格式。
2. `TikaDocumentTextExtractorTest` 验证 DOCX 提取和 PDF/DOCX 支持判断。
3. `ResumeControllerTest` 验证不支持格式会映射为 `400 Bad Request`。

## 下一步

1. 增加解析后的结构化抽取端口。
2. 先实现规则/正则版候选人画像抽取，后续再接大模型 Structured Output。
3. 将 `rawText` 转为 `CandidateProfile` 并进入筛选评分链路。
