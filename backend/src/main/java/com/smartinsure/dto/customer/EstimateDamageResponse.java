package com.smartinsure.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateDamageResponse {
    private double averageSeverityScore;
    private String overallSeverityLabel;
    private BigDecimal estimatedPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    private List<String> overallDetectedParts;
    private List<FileEstimateDto> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileEstimateDto {
        private String fileName;
        private double severityScore;
        private String severityLabel;
        private List<String> detectedParts;
    }
}
