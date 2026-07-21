# SDD: 后端第 3 步

## 目标

补齐本地基础设施，并为岗位管理模块增加 MySQL 持久化适配器。

## 基础设施

本地通过 `deploy/docker-compose.yml` 编排：

| 服务 | 用途 |
| --- | --- |
| MySQL 8.4 | 业务主库，保存岗位、候选人、评估记录 |
| PostgreSQL + pgvector | 向量库，保存岗位描述、项目经历、知识库切片 embedding |
| Redis 7.4 | 缓存、任务进度、幂等控制、短期会话状态 |

启动命令：

```powershell
docker compose -f deploy/docker-compose.yml up -d
```

## Profile 策略

默认 `local` profile 使用 `InMemoryJobRepository`，便于 TDD 快速运行。

启用 MySQL 持久化时增加 `mysql` profile：

```powershell
mvn spring-boot:run -pl recruit-start -Dspring-boot.run.profiles=local,mysql
```

## 持久化设计

`JobRepository` 是领域端口。

当前有两个适配器：

| 适配器 | Profile | 用途 |
| --- | --- | --- |
| `InMemoryJobRepository` | `!mysql` | 快速开发和领域测试 |
| `MyBatisJobRepository` | `mysql` | 真实 MySQL 持久化 |

岗位技能暂存在 MySQL JSON 字段中，由 Jackson 做结构化序列化和反序列化。后续如果需要复杂技能查询，可以拆成 `job_skill` 表。

## 下一步

1. Maven 和 Docker 命令可用后运行 `mvn test` 与 `docker compose up -d`。
2. 增加岗位 API 集成测试。
3. 增加简历上传与候选人领域模型。
4. 增加 PostgreSQL 向量库访问端口，先实现岗位描述 embedding 的保存接口。
