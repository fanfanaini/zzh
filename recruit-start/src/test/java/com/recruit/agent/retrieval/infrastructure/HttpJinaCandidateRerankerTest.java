package com.recruit.agent.retrieval.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpJinaCandidateRerankerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> response = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/rerank", this::respond);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldMapJinaIndexesBackToOriginalCandidates() throws Exception {
        response.set("{\"model\":\"jina-reranker-v2-base-multilingual\",\"results\":["
                + "{\"index\":1,\"relevance_score\":0.98},{\"index\":0,\"relevance_score\":0.25}]}");
        var reranker = new HttpJinaCandidateReranker(properties(50), objectMapper);

        var outcome = reranker.rerank("Java backend", List.of(
                result(1, "Java candidate"), result(2, "Spring Boot candidate")), 2);

        assertThat(outcome.results()).extracting(RankedRetrievalResult::chunkId).containsExactly(2L, 1L);
        assertThat(outcome.strategy()).isEqualTo("jina-cross-encoder:jina-reranker-v2-base-multilingual");
        assertThat(authorization.get()).isEqualTo("Bearer test-secret");
        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertThat(payload.path("query").asText()).isEqualTo("Java backend");
        assertThat(payload.path("top_n").asInt()).isEqualTo(2);
        assertThat(payload.path("return_documents").asBoolean()).isFalse();
        assertThat(payload.path("documents").get(0).asText()).isEqualTo("Java candidate");
    }

    @Test
    void shouldTruncateDocumentsAndRejectDuplicateIndexes() {
        response.set("{\"results\":[{\"index\":0,\"relevance_score\":0.8},{\"index\":0,\"relevance_score\":0.7}]}");
        var reranker = new HttpJinaCandidateReranker(properties(4), objectMapper);

        assertThatThrownBy(() -> reranker.rerank("query", List.of(result(1, "123456")), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid candidate scores");
    }

    @Test
    void shouldRejectNonSuccessfulHttpResponses() {
        response.set("status:500");
        var reranker = new HttpJinaCandidateReranker(properties(50), objectMapper);

        assertThatThrownBy(() -> reranker.rerank("query", List.of(result(1, "content")), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 500");
    }

    private void respond(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String body = response.get();
        boolean serverError = "status:500".equals(body);
        byte[] bytes = (serverError ? "{}" : body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(serverError ? 500 : 200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private RerankerProperties properties(int maxDocumentChars) {
        RerankerProperties properties = new RerankerProperties();
        properties.setEndpoint(java.net.URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/rerank"));
        properties.setApiKey("test-secret");
        properties.setMaxDocumentChars(maxDocumentChars);
        return properties;
    }

    private RankedRetrievalResult result(long chunkId, String content) {
        return new RankedRetrievalResult(1L, chunkId, "source", content,
                1, 80.0, null, null, 0.01);
    }
}
