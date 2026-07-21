# SDD: 后端第 5 步

## 目标

为候选人画像和简历元数据模块增加 MySQL 持久化适配器。

## 适配器

| 领域端口 | 默认实现 | MySQL 实现 |
| --- | --- | --- |
| `CandidateRepository` | `InMemoryCandidateRepository` | `MyBatisCandidateRepository` |
| `ResumeRepository` | `InMemoryResumeRepository` | `MyBatisResumeRepository` |

启用 `mysql` profile 后，Spring 会使用 MyBatis-Plus 适配器；默认 `local` 开发仍使用内存实现。

## 表映射

| 表 | PO | Mapper |
| --- | --- | --- |
| `candidate` | `CandidatePo` | `CandidateMapper` |
| `resume` | `ResumePo` | `ResumeMapper` |

候选人技能字段保存为 MySQL JSON，统一通过 `StringSetJsonCodec` 编解码。该工具也被岗位模块复用。

## ID 策略

当前使用 `TimestampIdGenerator` 生成时间戳型 ID，避免接入数据库自增后影响领域对象创建流程。后续可以替换为雪花 ID、Leaf 或数据库序列。

## TDD 覆盖

1. `CandidatePoConverterTest` 验证候选人领域对象和 PO 转换。
2. `ResumePoConverterTest` 验证简历领域对象和 PO 转换。
3. 原有 `CandidateApplicationServiceTest` 继续验证注册候选人和添加简历的用例行为。

## 下一步

1. 增加 API 集成测试，验证 Controller、参数校验和异常映射。
2. 增加简历文件存储端口，为 MinIO 接入做准备。
3. 增加文档解析端口，先支持从已提取文本生成候选人画像。
