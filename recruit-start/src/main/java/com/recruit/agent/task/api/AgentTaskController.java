package com.recruit.agent.task.api;

import com.recruit.agent.task.application.AgentTaskApplicationService;
import com.recruit.agent.task.application.CreateAgentTaskCommand;
import com.recruit.agent.candidate.application.CandidateProfileExtractionService;
import com.recruit.agent.candidate.application.CandidateResumeTextExtractionService;
import com.recruit.agent.candidate.application.ResumeParsingTaskService;
import com.recruit.agent.common.api.BusinessErrorCode;
import com.recruit.agent.common.api.BusinessException;
import com.recruit.agent.conversation.application.ConversationApplicationService;
import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskType;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-tasks")
public class AgentTaskController {

    private final AgentTaskApplicationService service;
    private final BatchScreeningApplicationService batchScreeningService;
    private final CandidateResumeTextExtractionService textExtractionService;
    private final CandidateProfileExtractionService profileExtractionService;
    private final ConversationApplicationService conversationService;
    private final ResumeParsingTaskService resumeParsingTaskService;

    public AgentTaskController(AgentTaskApplicationService service,
                               ObjectProvider<BatchScreeningApplicationService> batchScreeningService,
                               ObjectProvider<CandidateResumeTextExtractionService> textExtractionService,
                               ObjectProvider<CandidateProfileExtractionService> profileExtractionService,
                               ObjectProvider<ConversationApplicationService> conversationService,
                               ObjectProvider<ResumeParsingTaskService> resumeParsingTaskService) {
        this.service = service;
        this.batchScreeningService = batchScreeningService.getIfAvailable();
        this.textExtractionService = textExtractionService.getIfAvailable();
        this.profileExtractionService = profileExtractionService.getIfAvailable();
        this.conversationService = conversationService.getIfAvailable();
        this.resumeParsingTaskService = resumeParsingTaskService.getIfAvailable();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentTaskResponse create(@Valid @RequestBody CreateAgentTaskRequest request) {
        if (request.taskType() == AgentTaskType.SCREENING || request.taskType() == AgentTaskType.BATCH_SCREENING) {
            throw new BusinessException(BusinessErrorCode.TASK_TYPE_NOT_SUPPORTED,
                    "SCREENING/BATCH_SCREENING 请从匹配页面提交，手工任务缺少岗位和候选人参数");
        }
        AgentTask task = service.create(new CreateAgentTaskCommand(
                request.taskType(),
                request.targetId(),
                request.totalCount()
        ));
        if (task.taskType() == AgentTaskType.RESUME_PARSE && resumeParsingTaskService != null) {
            resumeParsingTaskService.schedule(task);
            return AgentTaskResponse.from(task);
        }
        return AgentTaskResponse.from(runManualTask(task));
    }

    @GetMapping("/{id}")
    public AgentTaskResponse get(@PathVariable("id") Long id) {
        return AgentTaskResponse.from(service.get(id));
    }

    @GetMapping
    public List<AgentTaskResponse> list() {
        return service.list().stream().map(AgentTaskResponse::from).toList();
    }

    @GetMapping("/{id}/steps")
    public List<AgentStepResponse> listSteps(@PathVariable("id") Long id) {
        return service.listSteps(id).stream()
                .map(AgentStepResponse::from)
                .toList();
    }

    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.OK)
    public AgentTaskResponse retry(@PathVariable("id") Long id) {
        if (service.get(id).taskType() == AgentTaskType.BATCH_SCREENING && batchScreeningService != null) {
            return AgentTaskResponse.from(batchScreeningService.retry(id));
        }
        if (service.get(id).taskType() == AgentTaskType.RESUME_PARSE && resumeParsingTaskService != null) {
            return AgentTaskResponse.from(resumeParsingTaskService.retry(id));
        }
        return AgentTaskResponse.from(runManualTask(service.retry(id)));
    }

    private AgentTask runManualTask(AgentTask task) {
        if (task.taskType() == AgentTaskType.RESUME_PARSE) {
            if (task.targetId() == null || textExtractionService == null || profileExtractionService == null) {
                service.recordFailure(task.id(), "参数校验", "RESUME_PARSE 需要 targetId=resumeId");
                return service.finish(task.id());
            }
            try {
                service.start(task.id());
                textExtractionService.extractText(task.targetId());
                profileExtractionService.extractProfile(task.targetId());
                service.recordSuccess(task.id(), "简历解析", "简历文本提取和候选人画像更新完成");
                return service.finish(task.id());
            } catch (RuntimeException exception) {
                service.recordFailure(task.id(), "简历解析", exception.getMessage());
                return service.finish(task.id());
            }
        }
        if (task.taskType() == AgentTaskType.REPLY_GENERATION) {
            if (task.targetId() == null || conversationService == null) {
                service.recordFailure(task.id(), "参数校验", "REPLY_GENERATION 需要 targetId=conversationId");
                return service.finish(task.id());
            }
            try {
                service.start(task.id());
                conversationService.generateReply(task.targetId());
                service.recordSuccess(task.id(), "回复生成", "AI 回复建议已生成");
                return service.finish(task.id());
            } catch (RuntimeException exception) {
                service.recordFailure(task.id(), "回复生成", exception.getMessage());
                return service.finish(task.id());
            }
        }
        service.recordFailure(task.id(), "任务创建", "SCREENING/BATCH_SCREENING 请从匹配页面提交，手工任务缺少岗位和候选人参数");
        return service.finish(task.id());
    }
}
