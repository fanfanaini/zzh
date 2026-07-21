package com.recruit.agent.screening.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.screening.application.BatchScreeningJob;
import com.recruit.agent.screening.application.BatchScreeningPublisher;
import com.recruit.agent.screening.application.BatchScreeningStateStore;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@Profile("distributed")
@EnableRabbit
public class BatchScreeningMessagingConfiguration {
    public static final String EXCHANGE = "recruit.screening";
    public static final String QUEUE = "recruit.screening.batch";
    public static final String DEAD_LETTER_EXCHANGE = "recruit.screening.dlx";
    public static final String DEAD_LETTER_QUEUE = "recruit.screening.batch.dead";
    public static final String ROUTING_KEY = "batch";

    @Bean
    DirectExchange screeningExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean
    DirectExchange screeningDeadLetterExchange() { return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false); }
    @Bean
    Queue screeningQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_KEY).build();
    }
    @Bean
    Queue screeningDeadLetterQueue() { return QueueBuilder.durable(DEAD_LETTER_QUEUE).build(); }
    @Bean
    Binding screeningBinding(@Qualifier("screeningQueue") Queue screeningQueue,
                             @Qualifier("screeningExchange") DirectExchange screeningExchange) {
        return BindingBuilder.bind(screeningQueue).to(screeningExchange).with(ROUTING_KEY);
    }
    @Bean
    Binding screeningDeadLetterBinding(@Qualifier("screeningDeadLetterQueue") Queue screeningDeadLetterQueue,
                                       @Qualifier("screeningDeadLetterExchange") DirectExchange screeningDeadLetterExchange) {
        return BindingBuilder.bind(screeningDeadLetterQueue).to(screeningDeadLetterExchange).with(ROUTING_KEY);
    }
    @Bean
    Jackson2JsonMessageConverter rabbitJsonConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    @Bean
    BatchScreeningStateStore redisBatchScreeningStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        return new RedisBatchScreeningStateStore(redis, objectMapper);
    }
    @Bean
    BatchScreeningPublisher rabbitBatchScreeningPublisher(RabbitTemplate rabbitTemplate) {
        return job -> rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, job);
    }
}
