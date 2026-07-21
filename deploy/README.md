# RecruitAgent 本地基础设施

启动：

```powershell
docker compose -f deploy/docker-compose.yml up -d
```

| 服务 | 地址 | 用户名 | 密码 |
| --- | --- | --- | --- |
| MySQL | `localhost:13307/recruit_agent` | `root` | `123456` |
| PostgreSQL + pgvector | `localhost:5432/recruit_vector` | `recruit` | `recruit` |
| Redis | `localhost:6379` | - | - |

注意：Docker 为避免占用本机 MySQL 的 `3306`，映射到了 `13307`。使用 Docker MySQL 时设置：

```powershell
$env:MYSQL_URL='jdbc:mysql://localhost:13307/recruit_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
```

数据库 volume 已存在时，修改 compose 密码不会重置旧账号密码。

后端在 IDEA 中启动：

```powershell
mvn -s .mvn/settings.xml -pl recruit-start spring-boot:run -Dspring-boot.run.profiles=mysql,vector
```

`application-mysql.yml` 默认连接 Docker 暴露的 `13307`，`application-vector.yml` 默认使用
`recruit/recruit`。若改用本机 MySQL `3306`，通过 `MYSQL_URL` 覆盖，不要同时连接两个同名数据库实例。

## 生成演示数据

项目提供了一套通过真实后端接口写入的演示数据，包含：

- 4 个岗位
- 6 名候选人
- 6 份可下载的文本简历
- 结构化教育、工作和项目经历
- 岗位与候选人筛选结果
- 招聘沟通会话和消息
- 岗位描述向量和简历项目向量
- 4 份招聘领域 RAG 知识文档

先启动 Docker Desktop 和基础设施：

```powershell
cd D:\文件\Java\recurit
docker compose -f deploy/docker-compose.yml up -d
```

然后在 IDEA 中使用 `mysql,vector` Profile 启动后端。后端使用 8080 时执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\seed\seed-demo-data.ps1
```

如果使用 Docker 后端，端口映射为 8081：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\seed\seed-demo-data.ps1 `
  -ApiBase http://localhost:8081/api
```

演示账号：

```text
用户名：demo_hr
密码：Demo@123
```

脚本会检查同名岗位、候选人、简历和知识文档，重复运行不会重复创建这些数据。若 PostgreSQL
向量卷曾被清空，但 MySQL 数据仍然存在，可以强制重新写入岗位和项目向量：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\seed\seed-demo-data.ps1 -ReindexVectors
```

RAG 原始测试资料位于：

```text
deploy/seed/knowledge
```

它们不是直接通过 SQL 塞入 PostgreSQL。脚本调用 `/api/knowledge/documents`，由后端完成文本切块、
Embedding 生成和 `knowledge_chunk_embedding` 写入。即使没有配置远程 Embedding API，项目也会使用
本地确定性向量实现，因此可以先验证完整 RAG 流程。

验证 MySQL 数据：

```powershell
docker exec recruit-mysql mysql -uroot -p123456 recruit_agent `
  -e "SELECT COUNT(*) AS jobs FROM job; SELECT COUNT(*) AS candidates FROM candidate; SELECT COUNT(*) AS resumes FROM resume;"
```

验证 pgvector 数据：

```powershell
docker exec recruit-postgres-vector psql -U recruit -d recruit_vector `
  -c "SELECT COUNT(*) AS knowledge_chunks FROM knowledge_chunk_embedding;"

docker exec recruit-postgres-vector psql -U recruit -d recruit_vector `
  -c "SELECT source_name, COUNT(*) AS chunks FROM knowledge_chunk_embedding GROUP BY source_name ORDER BY source_name;"
```
