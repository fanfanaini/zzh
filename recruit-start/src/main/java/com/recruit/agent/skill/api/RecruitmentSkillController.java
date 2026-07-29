package com.recruit.agent.skill.api;

import com.recruit.agent.skill.application.CreateRecruitmentSkillCommand;
import com.recruit.agent.skill.application.CreateRecruitmentSkillVersionCommand;
import com.recruit.agent.skill.application.RecruitmentSkillApplicationService;
import com.recruit.agent.skill.application.SkillMatch;
import com.recruit.agent.skill.domain.RecruitmentSkill;
import com.recruit.agent.skill.domain.RecruitmentSkillCard;
import com.recruit.agent.skill.domain.RecruitmentSkillVersion;
import com.recruit.agent.skill.infrastructure.MarkdownSkillImporter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class RecruitmentSkillController {
    private final RecruitmentSkillApplicationService service;
    private final MarkdownSkillImporter markdownImporter;

    public RecruitmentSkillController(RecruitmentSkillApplicationService service,
                                      MarkdownSkillImporter markdownImporter) {
        this.service = service;
        this.markdownImporter = markdownImporter;
    }

    @GetMapping
    public List<RecruitmentSkill> list() {
        return service.list();
    }

    @GetMapping("/cards")
    public List<RecruitmentSkillCard> cards() {
        return service.cards();
    }

    @GetMapping("/{skillId}")
    public RecruitmentSkill detail(@PathVariable Long skillId) {
        return service.detail(skillId);
    }

    @GetMapping("/{skillId}/versions")
    public List<RecruitmentSkillVersion> versions(@PathVariable Long skillId) {
        return service.versions(skillId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentSkill create(@Valid @RequestBody CreateSkillRequest request) {
        return service.create(toCommand(request));
    }

    @PostMapping("/{skillId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentSkillVersion addVersion(@PathVariable Long skillId,
                                              @Valid @RequestBody CreateSkillVersionRequest request) {
        return service.addVersion(skillId, toCommand(request));
    }

    @PostMapping("/{skillId}/versions/{versionId}/publish")
    public RecruitmentSkill publish(@PathVariable Long skillId, @PathVariable Long versionId) {
        return service.publish(skillId, versionId);
    }

    @PostMapping("/{skillId}/disable")
    public RecruitmentSkill disable(@PathVariable Long skillId) {
        return service.disable(skillId);
    }

    @PostMapping("/match")
    public List<SkillMatch> match(@Valid @RequestBody SkillMatchRequest request) {
        return service.match(request.query(), request.limit());
    }

    @PostMapping("/import-markdown")
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentSkill importMarkdown(@Valid @RequestBody ImportSkillMarkdownRequest request) {
        return service.create(markdownImporter.parse(request.markdown()));
    }

    private CreateRecruitmentSkillCommand toCommand(CreateSkillRequest request) {
        return new CreateRecruitmentSkillCommand(request.name(), request.category(), request.description(),
                request.scenario(), request.triggerKeywords(), request.priority(), request.version(),
                request.instruction(), request.promptFragment(), request.fewShots(), request.guardrails(),
                request.allowedTools(), request.riskTags(), request.handoffPolicy(),
                request.publish() == null || request.publish());
    }

    private CreateRecruitmentSkillVersionCommand toCommand(CreateSkillVersionRequest request) {
        return new CreateRecruitmentSkillVersionCommand(request.version(), request.instruction(),
                request.promptFragment(), request.fewShots(), request.guardrails(), request.allowedTools(),
                request.riskTags(), request.handoffPolicy(), request.publish() != null && request.publish());
    }
}
