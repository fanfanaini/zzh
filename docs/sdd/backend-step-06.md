# SDD: 后端第 6 步

## 目标

为现有 REST API 增加 Web 层集成测试，覆盖 Controller、参数校验、异常映射和临时安全配置。

## 测试策略

当前使用 `@WebMvcTest`，而不是完整 `@SpringBootTest`。

原因：

1. Web 层测试只关注 HTTP 映射、JSON 响应、状态码和参数校验。
2. 应用服务已由领域/应用层测试覆盖，这里用 mock 替身避免重复测试业务规则。
3. 不依赖 MySQL、Redis、Docker，能在 TDD 循环里快速运行。

## 覆盖范围

| 测试类 | 覆盖接口 |
| --- | --- |
| `JobControllerTest` | `POST /api/jobs`、`GET /api/jobs`、`GET /api/jobs/{id}` |
| `CandidateControllerTest` | `POST /api/candidates`、`POST /api/candidates/{id}/resumes`、`GET /api/candidates/{id}/resumes` |

## 已验证行为

1. 创建成功返回 `201 Created`。
2. 查询成功返回 `200 OK`。
3. 必填字段为空返回 `400 Bad Request`。
4. 资源不存在返回 `404 Not Found`。
5. 响应 JSON 字段符合当前 API 契约。

## 下一步

1. 增加简历文件存储端口，先定义接口，再实现本地/MinIO 适配器。
2. 增加文档文本提取端口，为 Tika/PDFBox/POI 接入做准备。
3. 接入真实 MySQL 后，再补 Testcontainers 持久化测试。
