# TDD 开发约定

本项目按“先规格、再测试、再实现”的节奏推进。

## 循环

1. 写清楚当前小步需求。
2. 先写失败测试，描述业务规则。
3. 编写最小实现让测试通过。
4. 重构命名、边界和结构。
5. 补充 SDD 记录设计决策。

## 测试分层

| 层级 | 工具 | 目标 |
| --- | --- | --- |
| 领域规则测试 | JUnit 5 / 当前轻量 Spec | 验证岗位匹配、评分、状态流转等纯业务逻辑 |
| 应用服务测试 | JUnit 5 + Mockito | 验证用例编排、事务边界、异常处理 |
| 持久化测试 | Testcontainers | 验证 MySQL 表结构、Mapper、事务 |
| 向量库测试 | Testcontainers PostgreSQL + pgvector | 验证 embedding 存储与相似度检索 |
| 接口测试 | Spring Boot Test | 验证 REST API、参数校验、鉴权 |

当前 shell 暂未识别到 Maven/Gradle，所以项目同时保留两类测试：

1. `*Spec`：可用 `javac` 和 `java` 直接运行，保障核心领域规则不被环境阻塞。
2. `*Test`：标准 JUnit 5 测试，Maven 可用后通过 `mvn test` 运行。

## 当前轻量验证命令

```powershell
javac -encoding UTF-8 -d target/test-classes recruit-start/src/main/java/com/recruit/agent/screening/domain/*.java recruit-start/src/main/java/com/recruit/agent/job/domain/*.java recruit-start/src/main/java/com/recruit/agent/job/application/*.java recruit-start/src/main/java/com/recruit/agent/job/infrastructure/InMemoryJobRepository.java recruit-start/src/test/java/com/recruit/agent/screening/domain/ResumeScreeningServiceSpec.java recruit-start/src/test/java/com/recruit/agent/job/application/JobApplicationServiceSpec.java
java -cp target/test-classes com.recruit.agent.screening.domain.ResumeScreeningServiceSpec
java -cp target/test-classes com.recruit.agent.job.application.JobApplicationServiceSpec
```

Maven 可用后的目标命令：

```powershell
mvn test
```

## 本地基础设施

Docker 可用后启动 MySQL、PostgreSQL + pgvector、Redis：

```powershell
docker compose -f deploy/docker-compose.yml up -d
```

默认运行应用时使用内存仓储；需要验证 MySQL 持久化时启用 `mysql` profile：

```powershell
mvn spring-boot:run -pl recruit-start -Dspring-boot.run.profiles=local,mysql
```
