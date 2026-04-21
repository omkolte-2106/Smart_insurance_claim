package com.smartinsure.ml;

import com.smartinsure.config.SmartInsureProperties;
import com.smartinsure.ml.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Python ML microservice.
 *
 * FIX (v1.1):
 * - analyzeDamage / detectPartsDamage previously called Files.exists(imagePath)
 * on the
 * RAW path passed in from ClaimService (built with Paths.get(rootPath,
 * storedPath)).
 * On Windows the storedPath uses forward-slashes (stored by
 * LocalFileStorageService as
 * "claims/123/uuid-file.jpg") while rootPath may use back-slashes, causing
 * Paths.get().toAbsolutePath().normalize() to still not resolve correctly, so
 * Files.exists() returned false and the method silently returned the 0.50
 * placeholder.
 *
 * Fix: call toAbsolutePath().normalize() before the exists() check and log the
 * resolved
 * path so it is visible in backend logs. Also set correct Content-Type per file
 * extension (PNG vs JPEG) so FastAPI can read the multipart correctly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MlServiceClient {

    private final SmartInsureProperties properties;

    // ── simple JSON endpoints ────────────────────────────────────────────────

    public MlDocumentVerificationResponse verifyDocuments(Map<String, Object> payload) {
        if (!properties.getMlService().isEnabled()) {
            return new MlDocumentVerificationResponse(0.82, 0.79, 0.12, "ML service disabled – placeholder");
        }
        try {
            return post("/ml/document-verification", payload, MlDocumentVerificationResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML document verification fallback: {}", ex.getMessage());
            return new MlDocumentVerificationResponse(0.75, 0.74, 0.18, "Fallback – ML unreachable");
        }
    }

    public MlFraudResponse scoreFraud(Map<String, Object> payload) {
        if (!properties.getMlService().isEnabled()) {
            return new MlFraudResponse(0.11, new String[] { "placeholder" });
        }
        try {
            return post("/ml/fraud-detection", payload, MlFraudResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML fraud detection fallback: {}", ex.getMessage());
            return new MlFraudResponse(0.22, new String[] { "Fallback – ML unreachable" });
        }
    }

    public MlDamageSeverityResponse predictDamage(Map<String, Object> payload) {
        if (!properties.getMlService().isEnabled()) {
            return new MlDamageSeverityResponse(0.45, "MODERATE", "placeholder");
        }
        try {
            return post("/ml/damage-severity", payload, MlDamageSeverityResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML damage severity fallback: {}", ex.getMessage());
            return new MlDamageSeverityResponse(0.50, "MODERATE", "fallback");
        }
    }

    public MlPayoutEstimateResponse estimatePayout(Map<String, Object> payload) {
        if (!properties.getMlService().isEnabled()) {
            return new MlPayoutEstimateResponse(BigDecimal.valueOf(75000), "INR", "placeholder");
        }
        try {
            return post("/ml/payout-estimation", payload, MlPayoutEstimateResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML payout estimation fallback: {}", ex.getMessage());
            return new MlPayoutEstimateResponse(BigDecimal.valueOf(65000), "INR", "Fallback – ML unreachable");
        }
    }

    public MlCustomerRankingResponse rankCustomers(Map<String, Object> payload) {
        if (!properties.getMlService().isEnabled()) {
            return new MlCustomerRankingResponse(List.of());
        }
        try {
            return post("/ml/customer-ranking", payload, MlCustomerRankingResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML customer ranking fallback: {}", ex.getMessage());
            return new MlCustomerRankingResponse(List.of());
        }
    }

    // ── multipart / file endpoints ───────────────────────────────────────────

    /**
     * Send the vehicle damage photo to /ml/analyze-damage and return the real
     * severity score from the MobileNetV2 model.
     *
     * ROOT CAUSE FIX: the original code did Files.exists(imagePath) on the raw,
     * un-normalised Path. On Windows this returned false when the stored relative
     * path used forward-slashes, causing a silent 0.50 fallback every time.
     * Now we resolve + normalise before the check and log the result so you can
     * confirm the path is correct in the Spring Boot console.
     */
    public MlDamageSeverityResponse analyzeDamage(Path imagePath) {
        if (!properties.getMlService().isEnabled()) {
            log.warn("analyzeDamage: ML service disabled in config.");
            return new MlDamageSeverityResponse(0.50, "MODERATE", "placeholder (service disabled)");
        }

        // Normalise the path – fixes forward/back-slash mismatch on Windows
        Path resolved = imagePath.toAbsolutePath().normalize();
        boolean exists = Files.exists(resolved);
        log.info("analyzeDamage: resolved path='{}' exists={}", resolved, exists);

        if (!exists) {
            log.warn("analyzeDamage: file not found at '{}' – returning 0.50 fallback. " +
                    "Check that storage.root-path in application.yml matches the directory " +
                    "where uploaded files are actually saved.", resolved);
            return new MlDamageSeverityResponse(0.50, "MODERATE",
                    "fallback (file not found: " + resolved + ")");
        }

        try {
            MediaType partType = resolved.toString().toLowerCase().endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            org.springframework.http.HttpHeaders partHeaders = new org.springframework.http.HttpHeaders();
            partHeaders.setContentType(partType);
            body.add("file", new org.springframework.http.HttpEntity<>(
                    new org.springframework.core.io.FileSystemResource(resolved), partHeaders));

            MlDamageSeverityResponse response = RestClient.builder()
                    .baseUrl(properties.getMlService().getBaseUrl())
                    .build()
                    .post()
                    .uri("/ml/analyze-damage")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlDamageSeverityResponse.class);

            log.info("analyzeDamage: score={} label={}",
                    response != null ? response.severityScore() : "null",
                    response != null ? response.severityLabel() : "null");
            return response;

        } catch (Exception ex) {
            log.warn("analyzeDamage: ML call failed ({}). Returning fallback 0.50.", ex.getMessage());
            return new MlDamageSeverityResponse(0.50, "MODERATE",
                    "fallback (error: " + ex.getMessage() + ")");
        }
    }

    /**
     * Send the vehicle damage photo to /ml/part-damage-detection.
     * Same path-normalisation fix as analyzeDamage.
     */
    public MlDamagePartsResponse detectPartsDamage(Path imagePath) {
        if (!properties.getMlService().isEnabled()) {
            log.warn("detectPartsDamage: ML service disabled in config.");
            return new MlDamagePartsResponse(List.of("BUMPER"), "MEDIUM", "placeholder (service disabled)");
        }
        if (imagePath == null) {
            return new MlDamagePartsResponse(List.of("UNKNOWN"), "LOW", "fallback (no image path provided)");
        }

        Path resolved = imagePath.toAbsolutePath().normalize();
        boolean exists = Files.exists(resolved);
        log.info("detectPartsDamage: resolved path='{}' exists={}", resolved, exists);

        if (!exists) {
            log.warn("detectPartsDamage: file not found at '{}'.", resolved);
            return new MlDamagePartsResponse(List.of("UNKNOWN"), "LOW",
                    "fallback (file not found: " + resolved + ")");
        }

        try {
            MediaType partType = resolved.toString().toLowerCase().endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            org.springframework.http.HttpHeaders partHeaders = new org.springframework.http.HttpHeaders();
            partHeaders.setContentType(partType);
            body.add("file", new org.springframework.http.HttpEntity<>(
                    new org.springframework.core.io.FileSystemResource(resolved), partHeaders));

            return RestClient.builder()
                    .baseUrl(properties.getMlService().getBaseUrl())
                    .build()
                    .post()
                    .uri("/ml/part-damage-detection")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlDamagePartsResponse.class);

        } catch (Exception ex) {
            log.warn("detectPartsDamage: ML call failed ({}). Using fallback.", ex.getMessage());
            return new MlDamagePartsResponse(List.of("UNKNOWN"), "LOW",
                    "fallback (error: " + ex.getMessage() + ")");
        }
    }

    // ── private helper ───────────────────────────────────────────────────────

    private <T> T post(String path, Map<String, Object> body, Class<T> type) {
        return RestClient.builder()
                .baseUrl(properties.getMlService().getBaseUrl())
                .build()
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
    }
}