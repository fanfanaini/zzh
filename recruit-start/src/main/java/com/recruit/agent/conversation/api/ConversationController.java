package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.application.AddMessageCommand;
import com.recruit.agent.conversation.application.ConversationApplicationService;
import com.recruit.agent.conversation.application.CreateConversationCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationApplicationService service;

    public ConversationController(ConversationApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@Valid @RequestBody CreateConversationRequest request) {
        return ConversationResponse.from(service.create(new CreateConversationCommand(
                request.jobId(),
                request.candidateId(),
                request.stage()
        )));
    }

    @GetMapping("/{id}")
    public ConversationResponse get(@PathVariable("id") Long id) {
        return ConversationResponse.from(service.get(id));
    }

    @GetMapping
    public List<ConversationResponse> list(@RequestParam(value = "jobId", required = false) Long jobId,
                                           @RequestParam(value = "candidateId", required = false) Long candidateId,
                                           @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return service.list(jobId, candidateId, limit).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationMessageResponse addMessage(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddMessageRequest request
    ) {
        return ConversationMessageResponse.from(service.addMessage(new AddMessageCommand(
                id,
                request.senderType(),
                request.content()
        )));
    }

    @GetMapping("/{id}/messages")
    public List<ConversationMessageResponse> listMessages(@PathVariable("id") Long id) {
        return service.listMessages(id).stream()
                .map(ConversationMessageResponse::from)
                .toList();
    }

    @PostMapping("/{id}/generate-reply")
    @ResponseStatus(HttpStatus.CREATED)
    public ReplySuggestionResponse generateReply(@PathVariable("id") Long id) {
        return ReplySuggestionResponse.from(service.generateReply(id));
    }

    @GetMapping("/{id}/reply-suggestions")
    public List<ReplySuggestionResponse> listReplySuggestions(@PathVariable("id") Long id) {
        return service.listReplySuggestions(id).stream()
                .map(ReplySuggestionResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/stage")
    public ConversationResponse updateStage(@PathVariable("id") Long id,
                                            @Valid @RequestBody UpdateConversationStageRequest request) {
        return ConversationResponse.from(service.updateStage(id, request.stage()));
    }

    @PostMapping("/{id}/close")
    public ConversationResponse close(@PathVariable("id") Long id) {
        return ConversationResponse.from(service.close(id));
    }

    @PostMapping("/{id}/mark-candidate-replied")
    public ConversationResponse markCandidateReplied(@PathVariable("id") Long id) {
        return ConversationResponse.from(service.markCandidateReplied(id));
    }

    @PatchMapping("/{id}/follow-up")
    public ConversationResponse scheduleFollowUp(@PathVariable("id") Long id,
                                                 @Valid @RequestBody ScheduleFollowUpRequest request) {
        return ConversationResponse.from(service.scheduleFollowUp(id, request.nextFollowUpAt()));
    }
}
