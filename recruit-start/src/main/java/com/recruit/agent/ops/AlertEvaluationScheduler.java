package com.recruit.agent.ops;

import org.springframework.scheduling.annotation.Scheduled;

public class AlertEvaluationScheduler {

    private final AlertLifecycleService service;

    public AlertEvaluationScheduler(AlertLifecycleService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${recruit.ops.alert-evaluation-interval-ms:60000}")
    public void evaluate() {
        service.evaluate();
    }
}
