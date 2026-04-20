package com.dossier.backend.ai.provider;

import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for detecting whether the application is running on Google Cloud Platform (GCP).
 *
 * Detection strategy (two-tier, evaluated in order):
 * 1. Primary: GOOGLE_CLOUD_PROJECT environment variable, which is set automatically by all GCP
 *    runtimes (Cloud Run, GKE, GCE, App Engine). This is a near-zero-cost check.
 * 2. Secondary: HTTP probe to the GCP instance metadata server. A 500ms timeout prevents
 *    blocking startup on non-GCP environments where the endpoint is unreachable.
 *
 * The detection result is cached after the first call; GCP context is stable for the
 * process lifetime. This class is a static utility with no Spring dependencies so that it
 * can be called from @Bean factory methods during application context initialization.
 */
@Slf4j
public final class GcpEnvironmentDetector {

    private static final String GCP_PROJECT_ENV_VAR = "GOOGLE_CLOUD_PROJECT";
    private static final String METADATA_URL = "http://metadata.google.internal/computeMetadata/v1/";
    private static final String METADATA_FLAVOR_HEADER = "Metadata-Flavor";
    private static final String METADATA_FLAVOR_VALUE = "Google";
    private static final int METADATA_TIMEOUT_MS = 500;

    // volatile ensures the cached result is visible across threads on first write
    private static volatile Boolean cachedResult = null;

    private GcpEnvironmentDetector() {
        // Utility class; do not instantiate.
    }

    /**
     * Returns true if the application is running on GCP (Cloud Run, GKE, GCE, or App Engine).
     * The result is cached after the first invocation.
     */
    public static boolean isRunningOnGcp() {
        if (cachedResult != null) {
            return cachedResult;
        }
        cachedResult = detect();
        return cachedResult;
    }

    private static boolean detect() {
        String projectId = System.getenv(GCP_PROJECT_ENV_VAR);
        if (projectId != null && !projectId.isBlank()) {
            log.info("[GcpEnvironmentDetector] Detected GCP environment via {}='{}'",
                GCP_PROJECT_ENV_VAR, projectId);
            return true;
        }
        return probeMetadataServer();
    }

    private static boolean probeMetadataServer() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(METADATA_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty(METADATA_FLAVOR_HEADER, METADATA_FLAVOR_VALUE);
            conn.setConnectTimeout(METADATA_TIMEOUT_MS);
            conn.setReadTimeout(METADATA_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            int status = conn.getResponseCode();
            conn.disconnect();
            if (status == 200) {
                log.info("[GcpEnvironmentDetector] Detected GCP environment via metadata server probe (HTTP 200)");
                return true;
            }
            log.debug("[GcpEnvironmentDetector] Metadata server returned HTTP {}; not treating as GCP", status);
            return false;
        } catch (Exception e) {
            log.debug("[GcpEnvironmentDetector] Metadata server probe failed (expected on non-GCP): {}", e.getMessage());
            return false;
        }
    }
}
