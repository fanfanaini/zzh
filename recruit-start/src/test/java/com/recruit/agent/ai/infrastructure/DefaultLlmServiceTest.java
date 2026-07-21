package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.RawLlmClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLlmServiceTest {

    @Test
    void parsesStructuredResponseAndSavesSuccessLog() {
        InMemoryModelCallLogRepository logRepository = new InMemoryModelCallLogRepository();
        DefaultLlmService service = new DefaultLlmService(new StaticRawClient("""
                {"name":"张三","score":0.91}
                """), logRepository);

        SampleResult result = service.structuredChat(new LlmStructuredRequest<>(
                "TEST",
                "v1",
                "system",
                "user",
                SampleResult.class
        ));

        assertThat(result.name()).isEqualTo("张三");
        assertThat(result.score()).isEqualTo(0.91);
    }

    @Test
    void savesFailureLogWhenResponseCannotBeParsed() {
        InMemoryModelCallLogRepository logRepository = new InMemoryModelCallLogRepository();
        DefaultLlmService service = new DefaultLlmService(new StaticRawClient("not json"), logRepository);

        assertThatThrownBy(() -> service.structuredChat(new LlmStructuredRequest<>(
                "TEST",
                "v1",
                "system",
                "user",
                SampleResult.class
        ))).isInstanceOf(IllegalStateException.class);
    }

    private record SampleResult(String name, double score) {
    }

    private static class StaticRawClient implements RawLlmClient {
        private final String response;

        private StaticRawClient(String response) {
            this.response = response;
        }

        @Override
        public LlmCallResult structuredChat(String systemPrompt, String userPrompt) {
            return new LlmCallResult(modelName(), response, 10, 20, 3);
        }

        @Override
        public String modelName() {
            return "test-model";
        }
    }
}
