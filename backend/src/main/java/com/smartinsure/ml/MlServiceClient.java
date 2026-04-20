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
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Python ML microservice. Endpoints map to modules under {@code ml-service/app}.
 * When {@code smartinsure.ml-service.enabled=false}, deterministic placeholders are returned.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MlServiceClient {

    private final SmartInsureProperties properties;

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
            return new MlFraudResponse(0.11, new String[]{"placeholder"});
        }
        try {
            return post("/ml/fraud-detection", payload, MlFraudResponse.class);
        } catch (RestClientException ex) {
            log.warn("ML fraud detection fallback: {}", ex.getMessage());
            return new MlFraudResponse(0.22, new String[]{"Fallback – ML unreachable"});
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

    public MlDamageSeverityResponse analyzeDamage(java.nio.file.Path imagePath) {
        if (!properties.getMlService().isEnabled() || !java.nio.file.Files.exists(imagePath)) {
            return new MlDamageSeverityResponse(0.50, "MODERATE", "placeholder (file missing or service disabled)");
        }
        try {
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(imagePath));

            RestClient client = RestClient.builder()
                    .baseUrl(properties.getMlService().getBaseUrl())
                    .build();

            MlDamageSeverityResponse response = client.post()
                    .uri("/ml/analyze-damage")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlDamageSeverityResponse.class);
            
            log.info("ML Analysis Response for file {}: {}", imagePath.getFileName(), response);
            return response;
        } catch (Exception ex) {
            log.warn("ML real image analysis failed, falling back: {}", ex.getMessage());
            return new MlDamageSeverityResponse(0.50, "MODERATE", "fallback (error)");
        }
    }

    public MlDamagePartsResponse detectPartsDamage(java.nio.file.Path imagePath) {
        if (!properties.getMlService().isEnabled() || !java.nio.file.Files.exists(imagePath)) {
            return new MlDamagePartsResponse(List.of("BUMPER"), "MEDIUM", "placeholder (file missing or service disabled)");
        }
        try {
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            
            // Explicitly set content type and filename for the file part
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
            org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(imagePath);
            org.springframework.http.HttpEntity<org.springframework.core.io.FileSystemResource> entity = new org.springframework.http.HttpEntity<>(resource, headers);
            
            body.add("file", entity);

            RestClient client = RestClient.builder()
                    .baseUrl(properties.getMlService().getBaseUrl())
                    .build();

            return client.post()
                    .uri("/ml/part-damage-detection")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlDamagePartsResponse.class);
        } catch (Exception ex) {
            log.warn("ML part damage detection failure: {}", ex.getMessage());
            return new MlDamagePartsResponse(List.of("UNKNOWN"), "LOW", "fallback");
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

    private <T> T post(String path, Map<String, Object> body, Class<T> type) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getMlService().getBaseUrl())
                .build();
        return client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
    }
}
