package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.application.ConversationApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replies")
public class ReplyController {

    private final ConversationApplicationService service;

    public ReplyController(ConversationApplicationService service) {
        this.service = service;
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public ReplySuggestionResponse approve(@PathVariable("id") Long id) {
        return ReplySuggestionResponse.from(service.approveReply(id));
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public ReplySuggestionResponse reject(@PathVariable("id") Long id) {
        return ReplySuggestionResponse.from(service.rejectReply(id));
    }
}
