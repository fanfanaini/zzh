# RecruitAgent 后端实现状态

更新日期：2026-07-16

## 已完成的可运行主链路

- Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus。
- MySQL 业务数据持久化与 Flyway 初始化脚本。
- PostgreSQL + pgvector 岗位、项目经历和知识库向量检索。
- 岗位创建、修改、查询和 AI 结构化分析。
- 候选人登记、PDF/DOCX/文本简历上传、文本提取、AI 画像及教育/工作/项目等结构化详情解析。
- 单份/多文件上传，以及文本提取与画像解析的一体化 `parse` 接口。
- 规则评分、向量相似度、LLM 证据复核、人工审核和候选人排行榜。
- 后台批量筛选、任务进度、逐候选人步骤、失败计数、幂等键和批次重试。
- `distributed` profile 下 RabbitMQ 持久队列、死信队列与 Redis 任务参数/幂等键恢复。
- 多轮会话记录、意图识别、回复草稿、人工通过/拒绝。
- Prompt 版本保存/启用/查询、模型调用日志与 Token/延迟汇总。
- 知识文档上传、语义边界切片、阈值过滤、列表、删除、Top-K 向量检索，以及对话自动检索和片段引用。
- OpenAI 兼容真实 Embedding 接口，并保留无外部服务时的确定性向量回退。
- MinIO 对象存储适配器；图片与扫描 PDF 的 Tesseract OCR 回退。
- 基础 RBAC 权限矩阵、访问令牌/刷新令牌、修改密码、公开注册最小权限、修改型接口审计日志。
- Actuator、Prometheus、Grafana、前后端 Dockerfile 和 Nginx API 代理。
- 本地内存适配器与 MySQL/pgvector 生产适配器，可在无数据库时运行测试。

## 主要新增接口

| 接口 | 用途 |
| --- | --- |
| `PUT /api/jobs/{id}` | 修改岗位 |
| `POST /api/jobs/{id}/analyze` | AI 解析 JD 并更新岗位画像 |
| `POST /api/resumes/upload` | 上传单份简历 |
| `POST /api/resumes/batch-upload` | 上传多份简历 |
| `POST /api/resumes/{id}/parse` | 提取文本并生成候选人画像 |
| `POST /api/resumes/{id}/parse-task` | 提交后台简历解析任务并返回 `taskId` |
| `GET /api/resumes/{id}/detail` | 查询原文和结构化简历详情 |
| `PUT /api/resumes/{id}/structured-detail` | 人工修正结构化详情 |
| `POST /api/auth/refresh` | 使用 Refresh Token 续期登录 |
| `PUT /api/auth/password` | 修改当前用户密码 |
| `POST /api/jobs/{jobId}/batch-screen` | 创建后台批量筛选任务 |
| `GET /api/jobs/{jobId}/candidates` | 按最新评分获取候选人排行 |
| `POST /api/prompts/{code}/versions` | 创建 Prompt 版本 |
| `GET /api/model-calls/summary` | 模型调用、Token 与延迟汇总 |
| `POST /api/knowledge/documents` | 切片并索引知识文档 |
| `POST /api/knowledge/documents/upload` | 上传并索引 PDF/DOCX/文本知识文档 |
| `GET /api/knowledge/documents` | 查询知识文档 |
| `DELETE /api/knowledge/documents/{id}` | 删除文档及其全部向量 |
| `GET /api/knowledge/search` | 检索知识片段及引用来源 |
| `GET /api/audit/logs` | 管理员查询操作审计 |

批量筛选请求可携带 `Idempotency-Key` 请求头。接口返回 HTTP 202 和 `taskId`，进度通过
`GET /api/agent-tasks/{taskId}` 与 `GET /api/agent-tasks/{taskId}/steps` 查询。

## 本地数据库配置

默认配置与当前开发环境一致：

```text
MySQL:      localhost:3307/recruit_agent  root / 123456
PostgreSQL: localhost:5432/recruit_vector recruit / recruit
Redis:      localhost:6379
```

所有值都可以通过环境变量覆盖：`MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、
`VECTOR_DB_URL`、`VECTOR_DB_USERNAME`、`VECTOR_DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`。

真实 AI 使用 OpenAI 兼容接口，示例配置为 DeepSeek。为避免缺少密钥时阻塞应用启动，默认关闭；
设置 `AI_ENABLED=true` 和 `AI_API_KEY` 后启用，也可通过 `AI_BASE_URL` 和 `AI_MODEL` 覆盖配置。

## 启动方式

```powershell
docker compose -f deploy/docker-compose.yml up -d
mvn -s .mvn/settings.xml -pl recruit-start spring-boot:run -Dspring-boot.run.profiles=mysql,vector
```

如果旧 Docker volume 使用过其他账号密码，需要先自行迁移或删除旧 volume；修改 compose 环境变量不会重置已有数据库账号。

## 可选生产配置

完整批量筛选消息队列和对象存储模式使用 profile：`mysql,vector,distributed,minio`。本地开发不启用
`distributed` 和 `minio` 时，批量筛选和简历解析使用线程池、本地文件，便于单机调试。

DeepSeek 只提供聊天模型接口，不提供 Embedding API。真实向量需要额外配置
`EMBEDDING_ENABLED=true`、`EMBEDDING_BASE_URL`、`EMBEDDING_API_KEY` 和
`EMBEDDING_MODEL`；否则使用确定性向量，不能视为生产语义质量。

## 仍需按实际上线环境完成

- 企业/部门多租户数据隔离与管理员用户管理页面。
- Redis 接口限流、热点查询缓存和分布式锁。
- Rerank、自动评估数据集、Prompt A/B 与公平性实验。
- Grafana 业务 Dashboard 与 Alertmanager 实际告警接收渠道。
- OCR 准确率基准、超大文档分片处理和病毒扫描。
- 正式招聘平台授权接口、CI/CD 和云环境密钥管理。

这些是上线级增强，不应在外部依赖和验收标准未确定时用空壳实现冒充完成。
