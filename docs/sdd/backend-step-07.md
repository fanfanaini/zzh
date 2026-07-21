# SDD: 后端第 7 步

## 目标

增加简历文件存储端口，并提供本地文件系统实现，为后续 MinIO、PDF/DOCX 文本解析和 OCR 接入做准备。

## 新增端口

| 类型 | 说明 |
| --- | --- |
| `FileStorage` | 文件存储端口 |
| `FileUpload` | 待上传文件输入对象 |
| `StoredFile` | 存储后的文件元数据 |
| `LocalFileStorage` | 本地文件系统适配器 |

## API

新增 multipart 上传接口：

```http
POST /api/candidates/{id}/resumes/upload
Content-Type: multipart/form-data
```

表单字段：

```text
file
```

上传成功后，接口会：

1. 将文件保存到本地存储。
2. 得到 `objectName`。
3. 调用候选人应用服务保存简历元数据。
4. 返回 `ResumeResponse`。

## 本地存储策略

本地文件默认保存到：

```text
./data/resumes
```

对象名格式：

```text
yyyy/MM/dd/{uuid}-{safeFilename}
```

文件名会进行清洗，只保留字母、数字、点、下划线和短横线。存储实现会校验目标路径必须在根目录内，避免路径穿越。

## 配置

```yaml
recruit:
  storage:
    local-root: ./data/resumes
```

## TDD 覆盖

1. `LocalFileStorageTest` 验证文件落盘、日期目录和安全文件名。
2. `CandidateControllerTest` 验证 multipart 上传成功。
3. `CandidateControllerTest` 验证空文件返回 `400 Bad Request`。

## 下一步

1. 增加文档文本提取端口。
2. 先实现纯文本解析适配器，再接 Tika/PDFBox/POI。
3. 将解析出的原始文本写回 `ResumeDocument.rawText`。
