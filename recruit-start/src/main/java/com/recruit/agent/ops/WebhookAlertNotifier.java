package com.recruit.agent.ops;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class WebhookAlertNotifier implements AlertNotifier {

    private final RestClient restClient;
    private final String webhookUrl;

    public WebhookAlertNotifier(RestClient.Builder builder, String webhookUrl) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void notify(AlertEvent event) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "eventId", event.id(),
                        "fingerprint", event.fingerprint(),
                        "code", event.code(),
                        "severity", event.severity().name(),
                        "status", event.status().name(),
                        "title", event.title(),
                        "message", event.message(),
                        "details", event.details(),
                        "triggeredAt", event.lastTriggeredAt().toString()
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
