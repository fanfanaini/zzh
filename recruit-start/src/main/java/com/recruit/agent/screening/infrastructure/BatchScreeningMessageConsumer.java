package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.screening.application.BatchScreeningApplicationService;
import com.recruit.agent.screening.application.BatchScreeningJob;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("distributed")
public class BatchScreeningMessageConsumer {
    private final BatchScreeningApplicationService service;

    public BatchScreeningMessageConsumer(BatchScreeningApplicationService service) {
        this.service = service;
    }

    @RabbitListener(queues = BatchScreeningMessagingConfiguration.QUEUE)
    public void consume(BatchScreeningJob job) {
        service.process(job);
    }
}
