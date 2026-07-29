package com.recruit.agent.ops;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class AgentOpsController {
    private final AgentOpsService service;
    private final AlertLifecycleService alertLifecycleService;

    public AgentOpsController(AgentOpsService service, AlertLifecycleService alertLifecycleService) {
        this.service = service;
        this.alertLifecycleService = alertLifecycleService;
    }

    @GetMapping("/dashboard")
    public AgentOpsDashboard dashboard() {
        return service.dashboard();
    }

    @GetMapping("/alerts")
    public List<AgentOpsAlert> alerts() {
        return service.alerts();
    }

    @PostMapping("/alert-events/evaluate")
    public List<AlertEvent> evaluateAlertEvents() {
        return alertLifecycleService.evaluate();
    }

    @GetMapping("/alert-events")
    public List<AlertEvent> alertEvents(@RequestParam(value = "status", required = false) AlertEventStatus status,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return alertLifecycleService.list(status, limit);
    }

    @PostMapping("/alert-events/{id}/ack")
    public AlertEvent acknowledge(@PathVariable("id") Long id,
                                  @RequestBody(required = false) AcknowledgeAlertRequest request) {
        return alertLifecycleService.acknowledge(id, request == null ? null : request.userId());
    }

    @PostMapping("/alert-events/{id}/retry-notification")
    public AlertEvent retryNotification(@PathVariable("id") Long id) {
        return alertLifecycleService.retryNotification(id);
    }
}
