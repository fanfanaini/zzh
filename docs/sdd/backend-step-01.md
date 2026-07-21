# SDD: 后端第 1 步

## 目标

建立 RecruitAgent 后端的最小工程骨架，并先实现简历筛选的核心领域规则。

## 技术选择

| 项 | 选择 |
| --- | --- |
| JDK | 17 |
| Web 框架 | Spring Boot 3 |
| 主业务数据库 | MySQL |
| 向量数据库 | PostgreSQL + pgvector |
| ORM | MyBatis-Plus |
| 数据库迁移 | Flyway |
| 测试 | TDD，优先覆盖领域规则 |

## 边界

第一步不接入大模型，不做简历文件解析，不做登录鉴权的完整实现。原因是这些能力依赖外部服务或基础设施，先把筛选规则沉淀为稳定领域模型。

## 当前领域规则

1. 学历不满足岗位要求，直接返回 `NOT_MATCH`。
2. 工作年限不满足岗位要求，直接返回 `NOT_MATCH`。
3. 必须技能缺失，直接返回 `NOT_MATCH`，并输出缺失技能。
4. 满足硬性条件后，按以下权重计算分数：

```text
Score = 0.4 * skill + 0.25 * project + 0.15 * experience + 0.1 * education + 0.1 * bonus
```

5. 最终分大于等于 80，推荐 `INTERVIEW`，否则进入 `MANUAL_REVIEW`。

## 数据库拆分

MySQL 保存岗位、候选人、简历评估、会话、审计日志等强事务业务数据。

PostgreSQL + pgvector 保存岗位描述、项目经历、知识库切片等 embedding 数据，用于 RAG 和语义匹配。

## 下一步

1. 安装 Maven 后迁移轻量 Spec 到 JUnit 5。
2. 增加岗位管理模块：建表、实体、Mapper、Service、Controller。
3. 增加 Docker Compose：MySQL、PostgreSQL + pgvector、Redis。
4. 为岗位 CRUD 写接口测试和持久化测试。
