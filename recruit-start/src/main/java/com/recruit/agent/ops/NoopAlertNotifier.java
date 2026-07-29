package com.recruit.agent.ops;

public class NoopAlertNotifier implements AlertNotifier {

    @Override
    public void notify(AlertEvent event) {
        // Notification is intentionally disabled when no webhook is configured.
    }
}
