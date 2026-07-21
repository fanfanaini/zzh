# SDD: 后端第 2 步

## 目标

按 TDD 推进岗位管理模块的第一组能力：创建岗位、按 ID 查询岗位、查询岗位列表。

## 设计

岗位模块分为四层：

| 层 | 包 | 说明 |
| --- | --- | --- |
| API | `job.api` | REST 请求、响应 DTO |
| Application | `job.application` | 用例编排 |
| Domain | `job.domain` | 岗位实体和仓储接口 |
| Infrastructure | `job.infrastructure` | 当前使用内存仓储，后续替换 MySQL |

## 当前决策

1. 先使用 `InMemoryJobRepository` 跑通业务闭环，避免数据库基础设施阻塞 TDD。
2. `JobRepository` 作为端口，后续 MyBatis-Plus 实现只需要替换基础设施层。
3. 技能字段在领域层统一标准化为小写，减少后续匹配规则的重复处理。
4. 第一阶段临时放开 Spring Security 的所有请求，等用户与权限模块启动后再接入 JWT。

## API

```http
POST /api/jobs
GET  /api/jobs/{id}
GET  /api/jobs
```

## 下一步

1. 增加 Docker Compose：MySQL、PostgreSQL + pgvector、Redis。
2. 增加 MyBatis-Plus 版 `JobRepository`。
3. 给岗位 API 增加 Spring Boot 集成测试。
4. 增加 `PUT /api/jobs/{id}` 和 `PUT /api/jobs/{id}/rules`。
