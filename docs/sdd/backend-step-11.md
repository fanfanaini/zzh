# SDD: 后端第 11 步

## 目标

实现同步单候选人筛选 API，把岗位画像、候选人画像和评分领域服务串起来。

## API

```http
POST /api/jobs/{jobId}/screen
Content-Type: application/json

{
  "candidateId": 2001
}
```

响应：

```json
{
  "recommendation": "INTERVIEW",
  "score": {
    "skillScore": 100,
    "projectScore": 100,
    "experienceScore": 100,
    "educationScore": 100,
    "bonusScore": 50,
    "finalScore": 95
  },
  "advantages": [],
  "risks": [],
  "missingInformation": []
}
```

## 应用服务

`ScreeningApplicationService` 负责：

1. 根据 `jobId` 查询岗位。
2. 根据 `candidateId` 查询候选人。
3. 将 `Job` 转换为 `JobProfile`。
4. 将 `Candidate` 转换为 `CandidateProfile`。
5. 调用 `ResumeScreeningService` 得出结果。

## 当前边界

本阶段先不保存 `resume_evaluation`，只返回同步筛选结果。下一步再增加评估记录表对应的领域对象和 MyBatis 持久化。

## TDD 覆盖

1. `ScreeningApplicationServiceTest` 验证筛选和资源不存在。
2. `ScreeningControllerTest` 验证 API 成功响应、参数校验和 404。

## 下一步

1. 新增 `Evaluation` 领域对象。
2. 增加 `EvaluationRepository` 和 MyBatis 实现。
3. 筛选完成后保存评估结果。
