package com.smartinsure.service;

import com.smartinsure.dto.customer.EstimateDamageResponse;
import com.smartinsure.dto.customer.EstimateDamageResponse.FileEstimateDto;
import com.smartinsure.ml.MlServiceClient;
import com.smartinsure.ml.dto.MlDamagePartsResponse;
import com.smartinsure.ml.dto.MlDamageSeverityResponse;
import com.smartinsure.ml.dto.MlPayoutEstimateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateService {

    private final MlServiceClient mlServiceClient;

    public EstimateDamageResponse estimateDamage(MultipartFile[] files) {
        List<FileEstimateDto> details = new ArrayList<>();
        Set<String> allDetectedParts = new HashSet<>();
        double totalScore = 0;
        int validFiles = 0;

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            Path tempFile = null;
            try {
                // Ensure correct extension for ML service to recognize PNG vs JPEG
                String ext = ".jpg";
                if (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".png")) {
                    ext = ".png";
                }
                tempFile = Files.createTempFile("est_", ext);
                file.transferTo(tempFile);

                MlDamageSeverityResponse mlResp = mlServiceClient.analyzeDamage(tempFile);
                MlDamagePartsResponse partsResp = mlServiceClient.detectPartsDamage(tempFile);
                
                List<String> detectedParts = partsResp.detectedParts() != null ? partsResp.detectedParts() : new ArrayList<>();
                allDetectedParts.addAll(detectedParts);

                details.add(FileEstimateDto.builder()
                        .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                        .severityScore(mlResp.severityScore())
                        .severityLabel(mlResp.severityLabel())
                        .detectedParts(detectedParts)
                        .build());
                        
                totalScore += mlResp.severityScore();
                validFiles++;
            } catch (Exception e) {
                log.error("Failed to estimate damage for file {}", file.getOriginalFilename(), e);
            } finally {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file {}", tempFile, e);
                    }
                }
            }
        }

        double averageScore = validFiles > 0 ? totalScore / validFiles : 0;

        // --- HEURISTIC OVERRIDE ---
        // If the MobileNet model underestimates severity, we use the YOLO parts detection as a multiplier. 
        // Each unique damaged part adds +15% to the severity.
        if (allDetectedParts.size() > 0) {
            double partBoost = allDetectedParts.size() * 0.15;
            averageScore = Math.min(averageScore + partBoost, 0.95); // Cap at 95% severity
        }

        String overallLabel = "MINOR";
        if (averageScore >= 0.60) {
            overallLabel = "SEVERE";
        } else if (averageScore >= 0.30) {
            overallLabel = "MODERATE";
        }

        BigDecimal estimatedPrice = BigDecimal.ZERO;
        String currency = "INR";

        if (validFiles > 0) {
            Map<String, Object> payload = Map.of(
                    "claimPublicId", "ESTIMATE_ONLY",
                    "severity", averageScore,
                    "sumInsured", new BigDecimal("500000")
            );
            MlPayoutEstimateResponse payoutResp = mlServiceClient.estimatePayout(payload);
            estimatedPrice = payoutResp.recommendedPayout();
            currency = payoutResp.currency();
        }

        return EstimateDamageResponse.builder()
                .averageSeverityScore(averageScore)
                .overallSeverityLabel(overallLabel)
                .estimatedPrice(estimatedPrice)
                .currency(currency)
                .overallDetectedParts(new ArrayList<>(allDetectedParts))
                .details(details)
                .build();
    }
}
