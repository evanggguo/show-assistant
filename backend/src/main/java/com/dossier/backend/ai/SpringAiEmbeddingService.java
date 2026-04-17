package com.dossier.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 向量嵌入服务实现
 * 优先使用 Spring AI EmbeddingModel（如已配置），否则直接调用 Google text-embedding-004 REST API。
 */
@Slf4j
@Service
public class SpringAiEmbeddingService implements EmbeddingService {

    private static final String GOOGLE_EMBED_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=%s";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Nullable
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.google.genai.api-key:}")
    private String googleApiKey;

    @Autowired
    public SpringAiEmbeddingService(@Nullable EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        if (embeddingModel == null) {
            log.info("Spring AI EmbeddingModel not configured, will use Google REST API directly for embeddings.");
        }
    }

    @Override
    public float[] embed(String text) {
        if (embeddingModel != null) {
            try {
                float[] result = embeddingModel.embed(text);
                log.debug("Generated embedding via Spring AI, dim={}", result.length);
                return result;
            } catch (Exception e) {
                log.warn("Spring AI embedding failed, falling back to Google REST API: {}", e.getMessage());
            }
        }

        return embedViaGoogleRestApi(text);
    }

    private float[] embedViaGoogleRestApi(String text) {
        if (googleApiKey == null || googleApiKey.isBlank() || "placeholder".equals(googleApiKey)) {
            log.debug("Google API key not configured, skipping embedding");
            return new float[0];
        }
        try {
            String body = MAPPER.writeValueAsString(java.util.Map.of(
                "model", "models/gemini-embedding-001",
                "content", java.util.Map.of(
                    "parts", java.util.List.of(java.util.Map.of("text", text))
                ),
                "outputDimensionality", 768
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(GOOGLE_EMBED_URL, googleApiKey)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Google embedding API returned {}: {}", response.statusCode(), response.body());
                return new float[0];
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode values = root.path("embedding").path("values");
            if (values.isMissingNode()) {
                log.warn("Google embedding API response missing 'embedding.values'");
                return new float[0];
            }

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = (float) values.get(i).asDouble();
            }
            log.debug("Generated embedding via Google REST API, dim={}", result.length);
            return result;

        } catch (Exception e) {
            log.warn("Google REST API embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }
}
