package com.dossier.backend.ai;

import com.dossier.backend.ai.provider.GcpEnvironmentDetector;
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
 * Vector embedding service implementation.
 * Priority: Spring AI EmbeddingModel → Vertex AI REST (on GCP, ADC) → Google AI Studio REST (API key).
 */
@Slf4j
@Service
public class SpringAiEmbeddingService implements EmbeddingService {

    private static final String GOOGLE_EMBED_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=%s";

    // Vertex AI text-embedding-004: 768 dims, same as gemini-embedding-001
    private static final String VERTEX_EMBED_URL =
        "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/text-embedding-004:predict";

    private static final String GCP_METADATA_TOKEN_URL =
        "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Nullable
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.google.genai.api-key:}")
    private String googleApiKey;

    @Value("${GOOGLE_CLOUD_PROJECT:}")
    private String gcpProjectId;

    @Value("${VERTEX_AI_LOCATION:us-central1}")
    private String vertexLocation;

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
                log.warn("Spring AI embedding failed, falling back: {}", e.getMessage());
            }
        }

        if (GcpEnvironmentDetector.isRunningOnGcp()) {
            return embedViaVertexAi(text);
        }

        return embedViaGoogleRestApi(text);
    }

    private float[] embedViaVertexAi(String text) {
        try {
            String token = fetchAdcToken();
            if (token == null) {
                log.warn("Failed to fetch ADC token for Vertex AI embedding, falling back to Google REST API");
                return embedViaGoogleRestApi(text);
            }

            String url = String.format(VERTEX_EMBED_URL, vertexLocation, gcpProjectId, vertexLocation);
            String body = MAPPER.writeValueAsString(java.util.Map.of(
                "instances", java.util.List.of(java.util.Map.of("content", text)),
                "parameters", java.util.Map.of("outputDimensionality", 768)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Vertex AI embedding API returned {}: {}", response.statusCode(), response.body());
                return new float[0];
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode values = root.path("predictions").get(0).path("embeddings").path("values");
            if (values.isMissingNode() || values.isNull()) {
                log.warn("Vertex AI embedding response missing predictions[0].embeddings.values");
                return new float[0];
            }

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = (float) values.get(i).asDouble();
            }
            log.debug("Generated embedding via Vertex AI, dim={}", result.length);
            return result;

        } catch (Exception e) {
            log.warn("Vertex AI embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    private String fetchAdcToken() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GCP_METADATA_TOKEN_URL))
                .header("Metadata-Flavor", "Google")
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonNode node = MAPPER.readTree(response.body());
            return node.path("access_token").asText(null);
        } catch (Exception e) {
            log.warn("Failed to fetch ADC token: {}", e.getMessage());
            return null;
        }
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
