package com.recruit.agent.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit/logs")
public class AuditController {
    private final AuditLogService service;
    public AuditController(AuditLogService service) { this.service = service; }

    @GetMapping
    public List<AuditLog> latest(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return service.latest(limit);
    }
}
